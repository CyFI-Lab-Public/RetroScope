//===- Linker.cpp ---------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Linker.h>
#include <mcld/LinkerConfig.h>
#include <mcld/Module.h>
#include <mcld/IRBuilder.h>

#include <mcld/Support/MsgHandling.h>
#include <mcld/Support/TargetRegistry.h>
#include <mcld/Support/FileHandle.h>
#include <mcld/Support/MemoryArea.h>
#include <mcld/Support/raw_ostream.h>

#include <mcld/Object/ObjectLinker.h>
#include <mcld/MC/InputBuilder.h>
#include <mcld/Target/TargetLDBackend.h>
#include <mcld/LD/LDSection.h>
#include <mcld/LD/LDSymbol.h>
#include <mcld/LD/SectionData.h>
#include <mcld/LD/RelocData.h>
#include <mcld/Fragment/Relocation.h>
#include <mcld/Fragment/FragmentRef.h>

#include <cassert>

using namespace mcld;

Linker::Linker()
  : m_pConfig(NULL), m_pIRBuilder(NULL),
    m_pTarget(NULL), m_pBackend(NULL), m_pObjLinker(NULL) {
}

Linker::~Linker()
{
  reset();
}

/// emulate - To set up target-dependent options and default linker script.
/// Follow GNU ld quirks.
bool Linker::emulate(LinkerScript& pScript, LinkerConfig& pConfig)
{
  m_pConfig = &pConfig;

  if (!initTarget())
    return false;

  if (!initBackend())
    return false;

  if (!initOStream())
    return false;

  if (!initEmulator(pScript))
    return false;

  return true;
}

bool Linker::link(Module& pModule, IRBuilder& pBuilder)
{
  if (!normalize(pModule, pBuilder))
    return false;

  if (!resolve())
    return false;

  return layout();
}

/// normalize - to convert the command line language to the input tree.
bool Linker::normalize(Module& pModule, IRBuilder& pBuilder)
{
  assert(NULL != m_pConfig);

  m_pIRBuilder = &pBuilder;

  m_pObjLinker = new ObjectLinker(*m_pConfig, *m_pBackend);

  m_pObjLinker->setup(pModule, pBuilder);

  // 2. - initialize FragmentLinker
  if (!m_pObjLinker->initFragmentLinker())
    return false;

  // 3. - initialize output's standard sections
  if (!m_pObjLinker->initStdSections())
    return false;

  if (!Diagnose())
    return false;

  // 4. - normalize the input tree
  //   read out sections and symbol/string tables (from the files) and
  //   set them in Module. When reading out the symbol, resolve their symbols
  //   immediately and set their ResolveInfo (i.e., Symbol Resolution).
  m_pObjLinker->normalize();

  if (m_pConfig->options().trace()) {
    static int counter = 0;
    mcld::outs() << "** name\ttype\tpath\tsize (" << pModule.getInputTree().size() << ")\n";
    InputTree::const_dfs_iterator input, inEnd = pModule.getInputTree().dfs_end();
    for (input=pModule.getInputTree().dfs_begin(); input!=inEnd; ++input) {
      mcld::outs() << counter++ << " *  " << (*input)->name();
      switch((*input)->type()) {
      case Input::Archive:
        mcld::outs() << "\tarchive\t(";
        break;
      case Input::Object:
        mcld::outs() << "\tobject\t(";
        break;
      case Input::DynObj:
        mcld::outs() << "\tshared\t(";
        break;
      case Input::Script:
        mcld::outs() << "\tscript\t(";
        break;
      case Input::External:
        mcld::outs() << "\textern\t(";
        break;
      default:
        unreachable(diag::err_cannot_trace_file) << (*input)->type()
                                                 << (*input)->name()
                                                 << (*input)->path();
      }
      mcld::outs() << (*input)->path() << ")\n";
    }
  }

  // 5. - set up code position
  if (LinkerConfig::DynObj == m_pConfig->codeGenType() ||
      m_pConfig->options().isPIE()) {
    m_pConfig->setCodePosition(LinkerConfig::Independent);
  }
  else if (pModule.getLibraryList().empty()) {
    // If the output is dependent on its loaded address, and it does not need
    // to call outside functions, then we can treat the output static dependent
    // and perform better optimizations.
    m_pConfig->setCodePosition(LinkerConfig::StaticDependent);

    if (LinkerConfig::Exec == m_pConfig->codeGenType()) {
      // Since the output is static dependent, there should not have any undefined
      // references in the output module.
      m_pConfig->options().setNoUndefined();
    }
  }
  else {
    m_pConfig->setCodePosition(LinkerConfig::DynamicDependent);
  }

  if (!m_pObjLinker->linkable())
    return Diagnose();

  return true;
}

bool Linker::resolve()
{
  assert(NULL != m_pConfig);
  assert(m_pObjLinker != NULL);

  // 6. - read all relocation entries from input files
  //   For all relocation sections of each input file (in the tree),
  //   read out reloc entry info from the object file and accordingly
  //   initiate their reloc entries in SectOrRelocData of LDSection.
  //
  //   To collect all edges in the reference graph.
  m_pObjLinker->readRelocations();

  // 7. - merge all sections
  //   Push sections into Module's SectionTable.
  //   Merge sections that have the same name.
  //   Maintain them as fragments in the section.
  //
  //   To merge nodes of the reference graph.
  if (!m_pObjLinker->mergeSections())
    return false;

  // 8. - allocateCommonSymbols
  //   Allocate fragments for common symbols to the corresponding sections.
  if (!m_pObjLinker->allocateCommonSymbols())
    return false;
  return true;
}

bool Linker::layout()
{
  assert(NULL != m_pConfig && NULL != m_pObjLinker);

  // 9. - add standard symbols, target-dependent symbols and script symbols
  // m_pObjLinker->addUndefSymbols();
  if (!m_pObjLinker->addStandardSymbols() ||
      !m_pObjLinker->addTargetSymbols() ||
      !m_pObjLinker->addScriptSymbols())
    return false;

  // 10. - scan all relocation entries by output symbols.
  //   reserve GOT space for layout.
  //   the space info is needed by pre-layout to compute the section size
  m_pObjLinker->scanRelocations();

  // 11.a - init relaxation stuff.
  m_pObjLinker->initStubs();

  // 11.b - pre-layout
  m_pObjLinker->prelayout();

  // 11.c - linear layout
  //   Decide which sections will be left in. Sort the sections according to
  //   a given order. Then, create program header accordingly.
  //   Finally, set the offset for sections (@ref LDSection)
  //   according to the new order.
  m_pObjLinker->layout();

  // 11.d - post-layout (create segment, instruction relaxing)
  m_pObjLinker->postlayout();

  // 12. - finalize symbol value
  m_pObjLinker->finalizeSymbolValue();

  // 13. - apply relocations
  m_pObjLinker->relocation();

  if (!Diagnose())
    return false;
  return true;
}

bool Linker::emit(MemoryArea& pOutput)
{
  // 13. - write out output
  m_pObjLinker->emitOutput(pOutput);

  // 14. - post processing
  m_pObjLinker->postProcessing(pOutput);

  if (!Diagnose())
    return false;

  return true;
}

bool Linker::emit(const std::string& pPath)
{
  FileHandle file;
  FileHandle::Permission perm = FileHandle::Permission(0x755);
  if (!file.open(pPath,
            FileHandle::ReadWrite | FileHandle::Truncate | FileHandle::Create,
            perm)) {
    error(diag::err_cannot_open_output_file) << "Linker::emit()" << pPath;
    return false;
  }

  MemoryArea* output = new MemoryArea(file);

  bool result = emit(*output);

  delete output;
  file.close();
  return result;
}

bool Linker::emit(int pFileDescriptor)
{
  FileHandle file;
  file.delegate(pFileDescriptor);
  MemoryArea* output = new MemoryArea(file);

  bool result = emit(*output);

  delete output;
  file.close();
  return result;
}

bool Linker::reset()
{
  m_pConfig = NULL;
  m_pIRBuilder = NULL;
  m_pTarget = NULL;

  // Because llvm::iplist will touch the removed node, we must clear
  // RelocData before deleting target backend.
  RelocData::Clear();
  SectionData::Clear();
  EhFrame::Clear();

  delete m_pBackend;
  m_pBackend = NULL;

  delete m_pObjLinker;
  m_pObjLinker = NULL;

  LDSection::Clear();
  LDSymbol::Clear();
  FragmentRef::Clear();
  Relocation::Clear();
  return true;
}

bool Linker::initTarget()
{
  assert(NULL != m_pConfig);

  std::string error;
  m_pTarget = mcld::TargetRegistry::lookupTarget(m_pConfig->targets().triple().str(), error);
  if (NULL == m_pTarget) {
    fatal(diag::fatal_cannot_init_target) << m_pConfig->targets().triple().str() << error;
    return false;
  }
  return true;
}

bool Linker::initBackend()
{
  assert(NULL != m_pTarget);
  m_pBackend = m_pTarget->createLDBackend(*m_pConfig);
  if (NULL == m_pBackend) {
    fatal(diag::fatal_cannot_init_backend) << m_pConfig->targets().triple().str();
    return false;
  }
  return true;
}

bool Linker::initOStream()
{
  assert(NULL != m_pConfig);

  mcld::outs().setColor(m_pConfig->options().color());
  mcld::errs().setColor(m_pConfig->options().color());

  return true;
}

bool Linker::initEmulator(LinkerScript& pScript)
{
  assert(NULL != m_pTarget && NULL != m_pConfig);
  return m_pTarget->emulate(pScript, *m_pConfig);
}

