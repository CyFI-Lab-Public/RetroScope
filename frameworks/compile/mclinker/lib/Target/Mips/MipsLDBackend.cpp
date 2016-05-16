//===- MipsLDBackend.cpp --------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "Mips.h"
#include "MipsGNUInfo.h"
#include "MipsELFDynamic.h"
#include "MipsLDBackend.h"
#include "MipsRelocator.h"

#include <llvm/ADT/Triple.h>
#include <llvm/Support/ELF.h>

#include <mcld/Module.h>
#include <mcld/LinkerConfig.h>
#include <mcld/IRBuilder.h>
#include <mcld/MC/Attribute.h>
#include <mcld/Fragment/FillFragment.h>
#include <mcld/Support/MemoryRegion.h>
#include <mcld/Support/MemoryArea.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/Support/TargetRegistry.h>
#include <mcld/Target/OutputRelocSection.h>
#include <mcld/Object/ObjectBuilder.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// MipsGNULDBackend
//===----------------------------------------------------------------------===//
MipsGNULDBackend::MipsGNULDBackend(const LinkerConfig& pConfig,
                                   MipsGNUInfo* pInfo)
  : GNULDBackend(pConfig, pInfo),
    m_pRelocator(NULL),
    m_pGOT(NULL),
    m_pRelDyn(NULL),
    m_pDynamic(NULL),
    m_pGOTSymbol(NULL),
    m_pGpDispSymbol(NULL)
{
}

MipsGNULDBackend::~MipsGNULDBackend()
{
  delete m_pRelocator;
  delete m_pGOT;
  delete m_pRelDyn;
  delete m_pDynamic;
}

void MipsGNULDBackend::initTargetSections(Module& pModule, ObjectBuilder& pBuilder)
{
  if (LinkerConfig::Object != config().codeGenType()) {
    ELFFileFormat* file_format = getOutputFormat();

    // initialize .got
    LDSection& got = file_format->getGOT();
    m_pGOT = new MipsGOT(got);

    // initialize .rel.dyn
    LDSection& reldyn = file_format->getRelDyn();
    m_pRelDyn = new OutputRelocSection(pModule, reldyn);
  }
}

void MipsGNULDBackend::initTargetSymbols(IRBuilder& pBuilder, Module& pModule)
{
  // Define the symbol _GLOBAL_OFFSET_TABLE_ if there is a symbol with the
  // same name in input
  m_pGOTSymbol = pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                   "_GLOBAL_OFFSET_TABLE_",
                   ResolveInfo::Object,
                   ResolveInfo::Define,
                   ResolveInfo::Local,
                   0x0,  // size
                   0x0,  // value
                   FragmentRef::Null(), // FragRef
                   ResolveInfo::Hidden);

  m_pGpDispSymbol = pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                   "_gp_disp",
                   ResolveInfo::Section,
                   ResolveInfo::Define,
                   ResolveInfo::Absolute,
                   0x0,  // size
                   0x0,  // value
                   FragmentRef::Null(), // FragRef
                   ResolveInfo::Default);

  if (NULL != m_pGpDispSymbol) {
    m_pGpDispSymbol->resolveInfo()->setReserved(MipsRelocator::ReserveGpDisp);
  }
}

bool MipsGNULDBackend::initRelocator()
{
  if (NULL == m_pRelocator) {
    m_pRelocator = new MipsRelocator(*this, config());
  }
  return true;
}

Relocator* MipsGNULDBackend::getRelocator()
{
  assert(NULL != m_pRelocator);
  return m_pRelocator;
}

void MipsGNULDBackend::doPreLayout(IRBuilder& pBuilder)
{
  // initialize .dynamic data
  if (!config().isCodeStatic() && NULL == m_pDynamic)
    m_pDynamic = new MipsELFDynamic(*this, config());

  // set .got size
  // when building shared object, the .got section is must.
  if (LinkerConfig::Object != config().codeGenType()) {
    if (LinkerConfig::DynObj == config().codeGenType() ||
        m_pGOT->hasGOT1() ||
        NULL != m_pGOTSymbol) {
      m_pGOT->finalizeScanning(*m_pRelDyn);
      m_pGOT->finalizeSectionSize();

      defineGOTSymbol(pBuilder);
    }

    ELFFileFormat* file_format = getOutputFormat();
    // set .rel.dyn size
    if (!m_pRelDyn->empty()) {
      assert(!config().isCodeStatic() &&
            "static linkage should not result in a dynamic relocation section");
      file_format->getRelDyn().setSize(
                                  m_pRelDyn->numOfRelocs() * getRelEntrySize());
    }
  }
}

void MipsGNULDBackend::doPostLayout(Module& pModule, IRBuilder& pBuilder)
{
}

/// dynamic - the dynamic section of the target machine.
/// Use co-variant return type to return its own dynamic section.
MipsELFDynamic& MipsGNULDBackend::dynamic()
{
  assert(NULL != m_pDynamic);
  return *m_pDynamic;
}

/// dynamic - the dynamic section of the target machine.
/// Use co-variant return type to return its own dynamic section.
const MipsELFDynamic& MipsGNULDBackend::dynamic() const
{
  assert(NULL != m_pDynamic);
  return *m_pDynamic;
}

uint64_t MipsGNULDBackend::emitSectionData(const LDSection& pSection,
                                           MemoryRegion& pRegion) const
{
  assert(pRegion.size() && "Size of MemoryRegion is zero!");

  const ELFFileFormat* file_format = getOutputFormat();

  if (&pSection == &(file_format->getGOT())) {
    assert(NULL != m_pGOT && "emitSectionData failed, m_pGOT is NULL!");
    uint64_t result = m_pGOT->emit(pRegion);
    return result;
  }

  fatal(diag::unrecognized_output_sectoin)
          << pSection.name()
          << "mclinker@googlegroups.com";
  return 0;
}

bool MipsGNULDBackend::hasEntryInStrTab(const LDSymbol& pSym) const
{
  return ResolveInfo::Section != pSym.type() ||
         m_pGpDispSymbol == &pSym;
}

namespace {
  struct DynsymGOTCompare
  {
    const MipsGOT& m_pGOT;

    DynsymGOTCompare(const MipsGOT& pGOT)
      : m_pGOT(pGOT)
    {
    }

    bool operator()(const LDSymbol* X, const LDSymbol* Y) const
    {
      return m_pGOT.dynSymOrderCompare(X, Y);
    }
  };
}

void MipsGNULDBackend::orderSymbolTable(Module& pModule)
{
  if (GeneralOptions::GNU  == config().options().getHashStyle() ||
      GeneralOptions::Both == config().options().getHashStyle()) {
    // The MIPS ABI and .gnu.hash require .dynsym to be sorted
    // in different ways. The MIPS ABI requires a mapping between
    // the GOT and the symbol table. At the same time .gnu.hash
    // needs symbols to be grouped by hash code.
    llvm::errs() << ".gnu.hash is incompatible with the MIPS ABI\n";
  }

  Module::SymbolTable& symbols = pModule.getSymbolTable();

  std::stable_sort(symbols.dynamicBegin(), symbols.dynamicEnd(),
                   DynsymGOTCompare(*m_pGOT));
}

MipsGOT& MipsGNULDBackend::getGOT()
{
  assert(NULL != m_pGOT);
  return *m_pGOT;
}

const MipsGOT& MipsGNULDBackend::getGOT() const
{
  assert(NULL != m_pGOT);
  return *m_pGOT;
}

OutputRelocSection& MipsGNULDBackend::getRelDyn()
{
  assert(NULL != m_pRelDyn);
  return *m_pRelDyn;
}

const OutputRelocSection& MipsGNULDBackend::getRelDyn() const
{
  assert(NULL != m_pRelDyn);
  return *m_pRelDyn;
}

unsigned int
MipsGNULDBackend::getTargetSectionOrder(const LDSection& pSectHdr) const
{
  const ELFFileFormat* file_format = getOutputFormat();

  if (&pSectHdr == &file_format->getGOT())
    return SHO_DATA;

  return SHO_UNDEFINED;
}

/// finalizeSymbol - finalize the symbol value
bool MipsGNULDBackend::finalizeTargetSymbols()
{
  if (NULL != m_pGpDispSymbol)
    m_pGpDispSymbol->setValue(m_pGOT->getGPDispAddress());

  return true;
}

/// allocateCommonSymbols - allocate common symbols in the corresponding
/// sections. This is called at pre-layout stage.
/// @refer Google gold linker: common.cc: 214
/// FIXME: Mips needs to allocate small common symbol
bool MipsGNULDBackend::allocateCommonSymbols(Module& pModule)
{
  SymbolCategory& symbol_list = pModule.getSymbolTable();

  if (symbol_list.emptyCommons() && symbol_list.emptyFiles() &&
      symbol_list.emptyLocals() && symbol_list.emptyLocalDyns())
    return true;

  SymbolCategory::iterator com_sym, com_end;

  // FIXME: If the order of common symbols is defined, then sort common symbols
  // std::sort(com_sym, com_end, some kind of order);

  // get corresponding BSS LDSection
  ELFFileFormat* file_format = getOutputFormat();
  LDSection& bss_sect = file_format->getBSS();
  LDSection& tbss_sect = file_format->getTBSS();

  // get or create corresponding BSS SectionData
  SectionData* bss_sect_data = NULL;
  if (bss_sect.hasSectionData())
    bss_sect_data = bss_sect.getSectionData();
  else
    bss_sect_data = IRBuilder::CreateSectionData(bss_sect);

  SectionData* tbss_sect_data = NULL;
  if (tbss_sect.hasSectionData())
    tbss_sect_data = tbss_sect.getSectionData();
  else
    tbss_sect_data = IRBuilder::CreateSectionData(tbss_sect);

  // remember original BSS size
  uint64_t bss_offset  = bss_sect.size();
  uint64_t tbss_offset = tbss_sect.size();

  // allocate all local common symbols
  com_end = symbol_list.localEnd();

  for (com_sym = symbol_list.localBegin(); com_sym != com_end; ++com_sym) {
    if (ResolveInfo::Common == (*com_sym)->desc()) {
      // We have to reset the description of the symbol here. When doing
      // incremental linking, the output relocatable object may have common
      // symbols. Therefore, we can not treat common symbols as normal symbols
      // when emitting the regular name pools. We must change the symbols'
      // description here.
      (*com_sym)->resolveInfo()->setDesc(ResolveInfo::Define);
      Fragment* frag = new FillFragment(0x0, 1, (*com_sym)->size());
      (*com_sym)->setFragmentRef(FragmentRef::Create(*frag, 0));

      if (ResolveInfo::ThreadLocal == (*com_sym)->type()) {
        // allocate TLS common symbol in tbss section
        tbss_offset += ObjectBuilder::AppendFragment(*frag,
                                                     *tbss_sect_data,
                                                     (*com_sym)->value());
      }
      // FIXME: how to identify small and large common symbols?
      else {
        bss_offset += ObjectBuilder::AppendFragment(*frag,
                                                    *bss_sect_data,
                                                    (*com_sym)->value());
      }
    }
  }

  // allocate all global common symbols
  com_end = symbol_list.commonEnd();
  for (com_sym = symbol_list.commonBegin(); com_sym != com_end; ++com_sym) {
    // We have to reset the description of the symbol here. When doing
    // incremental linking, the output relocatable object may have common
    // symbols. Therefore, we can not treat common symbols as normal symbols
    // when emitting the regular name pools. We must change the symbols'
    // description here.
    (*com_sym)->resolveInfo()->setDesc(ResolveInfo::Define);
    Fragment* frag = new FillFragment(0x0, 1, (*com_sym)->size());
    (*com_sym)->setFragmentRef(FragmentRef::Create(*frag, 0));

    if (ResolveInfo::ThreadLocal == (*com_sym)->type()) {
      // allocate TLS common symbol in tbss section
      tbss_offset += ObjectBuilder::AppendFragment(*frag,
                                                   *tbss_sect_data,
                                                   (*com_sym)->value());
    }
    // FIXME: how to identify small and large common symbols?
    else {
      bss_offset += ObjectBuilder::AppendFragment(*frag,
                                                  *bss_sect_data,
                                                  (*com_sym)->value());
    }
  }

  bss_sect.setSize(bss_offset);
  tbss_sect.setSize(tbss_offset);
  symbol_list.changeCommonsToGlobal();
  return true;
}

void MipsGNULDBackend::defineGOTSymbol(IRBuilder& pBuilder)
{
  // If we do not reserve any GOT entries, we do not need to re-define GOT
  // symbol.
  if (!m_pGOT->hasGOT1())
    return;

  // define symbol _GLOBAL_OFFSET_TABLE_
  if ( m_pGOTSymbol != NULL ) {
    pBuilder.AddSymbol<IRBuilder::Force, IRBuilder::Unresolve>(
                     "_GLOBAL_OFFSET_TABLE_",
                     ResolveInfo::Object,
                     ResolveInfo::Define,
                     ResolveInfo::Local,
                     0x0, // size
                     0x0, // value
                     FragmentRef::Create(*(m_pGOT->begin()), 0x0),
                     ResolveInfo::Hidden);
  }
  else {
    m_pGOTSymbol = pBuilder.AddSymbol<IRBuilder::Force, IRBuilder::Resolve>(
                     "_GLOBAL_OFFSET_TABLE_",
                     ResolveInfo::Object,
                     ResolveInfo::Define,
                     ResolveInfo::Local,
                     0x0, // size
                     0x0, // value
                     FragmentRef::Create(*(m_pGOT->begin()), 0x0),
                     ResolveInfo::Hidden);
  }
}

/// doCreateProgramHdrs - backend can implement this function to create the
/// target-dependent segments
void MipsGNULDBackend::doCreateProgramHdrs(Module& pModule)
{
  // TODO
}

//===----------------------------------------------------------------------===//
/// createMipsLDBackend - the help funtion to create corresponding MipsLDBackend
///
static TargetLDBackend* createMipsLDBackend(const llvm::Target& pTarget,
                                            const LinkerConfig& pConfig)
{
  if (pConfig.targets().triple().isOSDarwin()) {
    assert(0 && "MachO linker is not supported yet");
  }
  if (pConfig.targets().triple().isOSWindows()) {
    assert(0 && "COFF linker is not supported yet");
  }
  return new MipsGNULDBackend(pConfig, new MipsGNUInfo(pConfig.targets().triple()));
}

//===----------------------------------------------------------------------===//
// Force static initialization.
//===----------------------------------------------------------------------===//
extern "C" void MCLDInitializeMipsLDBackend() {
  // Register the linker backend
  mcld::TargetRegistry::RegisterTargetLDBackend(mcld::TheMipselTarget,
                                                createMipsLDBackend);
}

