//===- ObjectLinker.cpp ---------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Object/ObjectLinker.h>

#include <mcld/LinkerConfig.h>
#include <mcld/Module.h>
#include <mcld/InputTree.h>
#include <mcld/IRBuilder.h>
#include <mcld/LD/LDSection.h>
#include <mcld/LD/LDContext.h>
#include <mcld/LD/Archive.h>
#include <mcld/LD/ArchiveReader.h>
#include <mcld/LD/ObjectReader.h>
#include <mcld/LD/DynObjReader.h>
#include <mcld/LD/GroupReader.h>
#include <mcld/LD/BinaryReader.h>
#include <mcld/LD/ObjectWriter.h>
#include <mcld/LD/ResolveInfo.h>
#include <mcld/LD/RelocData.h>
#include <mcld/LD/Relocator.h>
#include <mcld/Support/RealPath.h>
#include <mcld/Support/MemoryArea.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/Support/DefSymParser.h>
#include <mcld/Target/TargetLDBackend.h>
#include <mcld/Fragment/FragmentLinker.h>
#include <mcld/Object/ObjectBuilder.h>

#include <llvm/Support/Casting.h>


using namespace llvm;
using namespace mcld;
ObjectLinker::ObjectLinker(const LinkerConfig& pConfig,
                           TargetLDBackend& pLDBackend)
  : m_Config(pConfig),
    m_pLinker(NULL),
    m_pModule(NULL),
    m_pBuilder(NULL),
    m_LDBackend(pLDBackend),
    m_pObjectReader(NULL),
    m_pDynObjReader(NULL),
    m_pArchiveReader(NULL),
    m_pGroupReader(NULL),
    m_pBinaryReader(NULL),
    m_pWriter(NULL) {
}

ObjectLinker::~ObjectLinker()
{
  delete m_pLinker;
  delete m_pObjectReader;
  delete m_pDynObjReader;
  delete m_pArchiveReader;
  delete m_pGroupReader;
  delete m_pBinaryReader;
  delete m_pWriter;
}

void ObjectLinker::setup(Module& pModule, IRBuilder& pBuilder)
{
  m_pModule = &pModule;
  m_pBuilder = &pBuilder;

  // set up soname
  if (!m_Config.options().soname().empty()) {
    m_pModule->setName(m_Config.options().soname());
  }
}

/// initFragmentLinker - initialize FragmentLinker
///  Connect all components with FragmentLinker
bool ObjectLinker::initFragmentLinker()
{
  if (NULL == m_pLinker) {
    m_pLinker = new FragmentLinker(m_Config,
                                   *m_pModule,
                                   m_LDBackend);
  }

  // initialize the readers and writers
  // Because constructor can not be failed, we initalize all readers and
  // writers outside the FragmentLinker constructors.
  m_pObjectReader  = m_LDBackend.createObjectReader(*m_pBuilder);
  m_pArchiveReader = m_LDBackend.createArchiveReader(*m_pModule);
  m_pDynObjReader  = m_LDBackend.createDynObjReader(*m_pBuilder);
  m_pBinaryReader  = m_LDBackend.createBinaryReader(*m_pBuilder);
  m_pGroupReader   = new GroupReader(*m_pModule, *m_pObjectReader,
                         *m_pDynObjReader, *m_pArchiveReader, *m_pBinaryReader);
  m_pWriter        = m_LDBackend.createWriter();

  // initialize Relocator
  m_LDBackend.initRelocator();
  return true;
}

/// initStdSections - initialize standard sections
bool ObjectLinker::initStdSections()
{
  ObjectBuilder builder(m_Config, *m_pModule);

  // initialize standard sections
  if (!m_LDBackend.initStdSections(builder))
    return false;

  // initialize target-dependent sections
  m_LDBackend.initTargetSections(*m_pModule, builder);

  return true;
}

void ObjectLinker::normalize()
{
  // -----  set up inputs  ----- //
  Module::input_iterator input, inEnd = m_pModule->input_end();
  for (input = m_pModule->input_begin(); input!=inEnd; ++input) {
    // is a group node
    if (isGroup(input)) {
      getGroupReader()->readGroup(input, m_pBuilder->getInputBuilder(), m_Config);
      continue;
    }

    // already got type - for example, bitcode or external OIR (object
    // intermediate representation)
    if ((*input)->type() == Input::Script ||
        (*input)->type() == Input::Archive ||
        (*input)->type() == Input::External)
      continue;

    if (Input::Object == (*input)->type()) {
      m_pModule->getObjectList().push_back(*input);
      continue;
    }

    if (Input::DynObj == (*input)->type()) {
      m_pModule->getLibraryList().push_back(*input);
      continue;
    }

    // read input as a binary file
    if (m_Config.options().isBinaryInput()) {
      (*input)->setType(Input::Object);
      getBinaryReader()->readBinary(**input);
      m_pModule->getObjectList().push_back(*input);
    }
    // is a relocatable object file
    else if (getObjectReader()->isMyFormat(**input)) {
      (*input)->setType(Input::Object);
      getObjectReader()->readHeader(**input);
      getObjectReader()->readSections(**input);
      getObjectReader()->readSymbols(**input);
      m_pModule->getObjectList().push_back(*input);
    }
    // is a shared object file
    else if (getDynObjReader()->isMyFormat(**input)) {
      (*input)->setType(Input::DynObj);
      getDynObjReader()->readHeader(**input);
      getDynObjReader()->readSymbols(**input);
      m_pModule->getLibraryList().push_back(*input);
    }
    // is an archive
    else if (getArchiveReader()->isMyFormat(**input)) {
      (*input)->setType(Input::Archive);
      Archive archive(**input, m_pBuilder->getInputBuilder());
      getArchiveReader()->readArchive(archive);
      if(archive.numOfObjectMember() > 0) {
        m_pModule->getInputTree().merge<InputTree::Inclusive>(input,
                                                            archive.inputs());
      }
    }
    else {
      fatal(diag::err_unrecognized_input_file) << (*input)->path()
                                          << m_Config.targets().triple().str();
    }
  } // end of for
}

bool ObjectLinker::linkable() const
{
  // check we have input and output files
  if (m_pModule->getInputTree().empty()) {
    error(diag::err_no_inputs);
    return false;
  }

  // can not mix -static with shared objects
  Module::const_lib_iterator lib, libEnd = m_pModule->lib_end();
  for (lib = m_pModule->lib_begin(); lib != libEnd; ++lib) {
    if((*lib)->attribute()->isStatic()) {
      error(diag::err_mixed_shared_static_objects)
                                      << (*lib)->name() << (*lib)->path();
      return false;
    }
  }

  // --nmagic and --omagic options lead to static executable program.
  // These options turn off page alignment of sections. Because the
  // sections are not aligned to pages, these sections can not contain any
  // exported functions. Also, because the two options disable linking
  // against shared libraries, the output absolutely does not call outside
  // functions.
  if (m_Config.options().nmagic() && !m_Config.isCodeStatic()) {
    error(diag::err_nmagic_not_static);
    return false;
  }
  if (m_Config.options().omagic() && !m_Config.isCodeStatic()) {
    error(diag::err_omagic_not_static);
    return false;
  }

  return true;
}

/// readRelocations - read all relocation entries
///
/// All symbols should be read and resolved before this function.
bool ObjectLinker::readRelocations()
{
  // Bitcode is read by the other path. This function reads relocation sections
  // in object files.
  mcld::InputTree::bfs_iterator input, inEnd = m_pModule->getInputTree().bfs_end();
  for (input=m_pModule->getInputTree().bfs_begin(); input!=inEnd; ++input) {
    if ((*input)->type() == Input::Object && (*input)->hasMemArea()) {
      if (!getObjectReader()->readRelocations(**input))
        return false;
    }
    // ignore the other kinds of files.
  }
  return true;
}

/// mergeSections - put allinput sections into output sections
bool ObjectLinker::mergeSections()
{
  ObjectBuilder builder(m_Config, *m_pModule);
  Module::obj_iterator obj, objEnd = m_pModule->obj_end();
  for (obj = m_pModule->obj_begin(); obj != objEnd; ++obj) {
    LDContext::sect_iterator sect, sectEnd = (*obj)->context()->sectEnd();
    for (sect = (*obj)->context()->sectBegin(); sect != sectEnd; ++sect) {
      switch ((*sect)->kind()) {
        // Some *INPUT sections should not be merged.
        case LDFileFormat::Ignore:
        case LDFileFormat::Null:
        case LDFileFormat::Relocation:
        case LDFileFormat::NamePool:
        case LDFileFormat::Group:
        case LDFileFormat::StackNote:
          // skip
          continue;
        case LDFileFormat::Target:
          if (!m_LDBackend.mergeSection(*m_pModule, **sect)) {
            error(diag::err_cannot_merge_section) << (*sect)->name()
                                                  << (*obj)->name();
            return false;
          }
          break;
        case LDFileFormat::EhFrame: {
          if (!(*sect)->hasEhFrame())
            continue; // skip

          LDSection* out_sect = NULL;
          if (NULL == (out_sect = builder.MergeSection(**sect))) {
            error(diag::err_cannot_merge_section) << (*sect)->name()
                                                  << (*obj)->name();
            return false;
          }

          if (!m_LDBackend.updateSectionFlags(*out_sect, **sect)) {
            error(diag::err_cannot_merge_section) << (*sect)->name()
                                                  << (*obj)->name();
            return false;
          }
          break;
        }
        default: {
          if (!(*sect)->hasSectionData())
            continue; // skip

          LDSection* out_sect = NULL;
          if (NULL == (out_sect = builder.MergeSection(**sect))) {
            error(diag::err_cannot_merge_section) << (*sect)->name()
                                                  << (*obj)->name();
            return false;
          }

          if (!m_LDBackend.updateSectionFlags(*out_sect, **sect)) {
            error(diag::err_cannot_merge_section) << (*sect)->name()
                                                  << (*obj)->name();
            return false;
          }
          break;
        }
      } // end of switch
    } // for each section
  } // for each obj
  return true;
}

/// addStandardSymbols - shared object and executable files need some
/// standard symbols
///   @return if there are some input symbols with the same name to the
///   standard symbols, return false
bool ObjectLinker::addStandardSymbols()
{
  // create and add section symbols for each output section
  Module::iterator iter, iterEnd = m_pModule->end();
  for (iter = m_pModule->begin(); iter != iterEnd; ++iter) {
    m_pModule->getSectionSymbolSet().add(**iter, m_pModule->getNamePool());
  }

  return m_LDBackend.initStandardSymbols(*m_pBuilder, *m_pModule);
}

/// addTargetSymbols - some targets, such as MIPS and ARM, need some
/// target-dependent symbols
///   @return if there are some input symbols with the same name to the
///   target symbols, return false
bool ObjectLinker::addTargetSymbols()
{
  m_LDBackend.initTargetSymbols(*m_pBuilder, *m_pModule);
  return true;
}

/// addScriptSymbols - define symbols from the command line option or linker
/// scripts.
bool ObjectLinker::addScriptSymbols()
{
  const LinkerScript& script = m_pModule->getScript();
  LinkerScript::DefSymMap::const_entry_iterator it;
  LinkerScript::DefSymMap::const_entry_iterator ie = script.defSymMap().end();
  // go through the entire defSymMap
  for (it = script.defSymMap().begin(); it != ie; ++it) {
    const llvm::StringRef sym =  it.getEntry()->key();
    ResolveInfo* old_info = m_pModule->getNamePool().findInfo(sym);
    // if the symbol does not exist, we can set type to NOTYPE
    // else we retain its type, same goes for size - 0 or retain old value
    // and visibility - Default or retain
    if (old_info != NULL) {
      if(!m_pBuilder->AddSymbol<IRBuilder::Force, IRBuilder::Unresolve>(
                             sym,
                             static_cast<ResolveInfo::Type>(old_info->type()),
                             ResolveInfo::Define,
                             ResolveInfo::Absolute,
                             old_info->size(),
                             0x0,
                             FragmentRef::Null(),
                             old_info->visibility()))
        return false;
    }
    else {
      if (!m_pBuilder->AddSymbol<IRBuilder::Force, IRBuilder::Unresolve>(
                             sym,
                             ResolveInfo::NoType,
                             ResolveInfo::Define,
                             ResolveInfo::Absolute,
                             0x0,
                             0x0,
                             FragmentRef::Null(),
                             ResolveInfo::Default))
        return false;
    }
  }
  return true;
}

bool ObjectLinker::scanRelocations()
{
  // apply all relocations of all inputs
  Module::obj_iterator input, inEnd = m_pModule->obj_end();
  for (input = m_pModule->obj_begin(); input != inEnd; ++input) {
    m_LDBackend.getRelocator()->initializeScan(**input);
    LDContext::sect_iterator rs, rsEnd = (*input)->context()->relocSectEnd();
    for (rs = (*input)->context()->relocSectBegin(); rs != rsEnd; ++rs) {
      // bypass the reloc section if
      // 1. its section kind is changed to Ignore. (The target section is a
      // discarded group section.)
      // 2. it has no reloc data. (All symbols in the input relocs are in the
      // discarded group sections)
      if (LDFileFormat::Ignore == (*rs)->kind() || !(*rs)->hasRelocData())
        continue;
      RelocData::iterator reloc, rEnd = (*rs)->getRelocData()->end();
      for (reloc = (*rs)->getRelocData()->begin(); reloc != rEnd; ++reloc) {
        Relocation* relocation = llvm::cast<Relocation>(reloc);
        // scan relocation
        if (LinkerConfig::Object != m_Config.codeGenType())
          m_LDBackend.getRelocator()->scanRelocation(
                                    *relocation, *m_pBuilder, *m_pModule, **rs);
        else
          m_LDBackend.getRelocator()->partialScanRelocation(
                                                 *relocation, *m_pModule, **rs);
      } // for all relocations
    } // for all relocation section
    m_LDBackend.getRelocator()->finalizeScan(**input);
  } // for all inputs
  return true;
}

/// initStubs - initialize stub-related stuff.
bool ObjectLinker::initStubs()
{
  // initialize BranchIslandFactory
  m_LDBackend.initBRIslandFactory();

  // initialize StubFactory
  m_LDBackend.initStubFactory();

  // initialize target stubs
  m_LDBackend.initTargetStubs();
  return true;
}

/// allocateCommonSymobols - allocate fragments for common symbols to the
/// corresponding sections
bool ObjectLinker::allocateCommonSymbols()
{
  if (LinkerConfig::Object != m_Config.codeGenType() ||
      m_Config.options().isDefineCommon())
    return m_LDBackend.allocateCommonSymbols(*m_pModule);
  return true;
}

/// prelayout - help backend to do some modification before layout
bool ObjectLinker::prelayout()
{
  // finalize the section symbols, set their fragment reference and push them
  // into output symbol table
  Module::iterator sect, sEnd = m_pModule->end();
  for (sect = m_pModule->begin(); sect != sEnd; ++sect) {
    m_pModule->getSectionSymbolSet().finalize(**sect,
        m_pModule->getSymbolTable(),
        m_Config.codeGenType() == LinkerConfig::Object);
  }

  m_LDBackend.preLayout(*m_pModule, *m_pBuilder);

  /// check program interpreter - computer the name size of the runtime dyld
  if (!m_Config.isCodeStatic() &&
      (LinkerConfig::Exec == m_Config.codeGenType() ||
       m_Config.options().isPIE() ||
       m_Config.options().hasDyld()))
    m_LDBackend.sizeInterp();

  /// measure NamePools - compute the size of name pool sections
  /// In ELF, will compute  the size of.symtab, .strtab, .dynsym, .dynstr,
  /// .hash and .shstrtab sections.
  ///
  /// dump all symbols and strings from FragmentLinker and build the format-dependent
  /// hash table.
  /// @note sizeNamePools replies on LinkerConfig::CodePosition. Must determine
  /// code position model before calling GNULDBackend::sizeNamePools()
  m_LDBackend.sizeNamePools(*m_pModule);

  return true;
}

/// layout - linearly layout all output sections and reserve some space
/// for GOT/PLT
///   Because we do not support instruction relaxing in this early version,
///   if there is a branch can not jump to its target, we return false
///   directly
bool ObjectLinker::layout()
{
  m_LDBackend.layout(*m_pModule);
  return true;
}

/// prelayout - help backend to do some modification after layout
bool ObjectLinker::postlayout()
{
  m_LDBackend.postLayout(*m_pModule, *m_pBuilder);
  return true;
}

/// finalizeSymbolValue - finalize the resolved symbol value.
///   Before relocate(), after layout(), FragmentLinker should correct value of all
///   symbol.
bool ObjectLinker::finalizeSymbolValue()
{
  bool finalized = m_pLinker->finalizeSymbols() && m_LDBackend.finalizeSymbols();
  bool scriptSymsAdded = true;
  uint64_t symVal;
  const LinkerScript& script = m_pModule->getScript();
  LinkerScript::DefSymMap::const_entry_iterator it;
  LinkerScript::DefSymMap::const_entry_iterator ie = script.defSymMap().end();

  DefSymParser parser(*m_pModule);
  for (it = script.defSymMap().begin(); it != ie; ++it) {
    llvm::StringRef symName =  it.getEntry()->key();
    llvm::StringRef expr =  it.getEntry()->value();

    LDSymbol* symbol = m_pModule->getNamePool().findSymbol(symName);
    assert(NULL != symbol && "--defsym symbol should be in the name pool");
    scriptSymsAdded &= parser.parse(expr, symVal);
    if (!scriptSymsAdded)
      break;
    symbol->setValue(symVal);
  }
  return finalized && scriptSymsAdded ;
}

/// relocate - applying relocation entries and create relocation
/// section in the output files
/// Create relocation section, asking TargetLDBackend to
/// read the relocation information into RelocationEntry
/// and push_back into the relocation section
bool ObjectLinker::relocation()
{
  return m_pLinker->applyRelocations();
}

/// emitOutput - emit the output file.
bool ObjectLinker::emitOutput(MemoryArea& pOutput)
{
  return llvm::errc::success == getWriter()->writeObject(*m_pModule, pOutput);
}


/// postProcessing - do modification after all processes
bool ObjectLinker::postProcessing(MemoryArea& pOutput)
{
  m_pLinker->syncRelocationResult(pOutput);

  // emit .eh_frame_hdr
  // eh_frame_hdr should be emitted after syncRelocation, because eh_frame_hdr
  // needs FDE PC value, which will be corrected at syncRelocation
  m_LDBackend.postProcessing(pOutput);
  return true;
}

