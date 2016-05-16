//===- HexagonLDBackend.cpp -----------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "Hexagon.h"
#include "HexagonELFDynamic.h"
#include "HexagonLDBackend.h"
#include "HexagonRelocator.h"
#include "HexagonGNUInfo.h"
#include "HexagonAbsoluteStub.h"

#include <llvm/ADT/Triple.h>
#include <llvm/Support/Casting.h>

#include <mcld/LinkerConfig.h>
#include <mcld/IRBuilder.h>
#include <mcld/Fragment/AlignFragment.h>
#include <mcld/Fragment/FillFragment.h>
#include <mcld/Fragment/RegionFragment.h>
#include <mcld/Support/MemoryRegion.h>
#include <mcld/Support/MemoryArea.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/Support/TargetRegistry.h>
#include <mcld/Object/ObjectBuilder.h>
#include <mcld/Fragment/Stub.h>
#include <mcld/LD/BranchIslandFactory.h>
#include <mcld/LD/StubFactory.h>
#include <mcld/LD/LDContext.h>


#include <cstring>

using namespace mcld;

//===----------------------------------------------------------------------===//
// HexagonLDBackend
//===----------------------------------------------------------------------===//
HexagonLDBackend::HexagonLDBackend(const LinkerConfig& pConfig,
                                   HexagonGNUInfo* pInfo)
  : GNULDBackend(pConfig, pInfo),
    m_pRelocator(NULL),
    m_pGOT(NULL),
    m_pGOTPLT(NULL),
    m_pPLT(NULL),
    m_pRelaDyn(NULL),
    m_pRelaPLT(NULL),
    m_pDynamic(NULL),
    m_pGOTSymbol(NULL),
    m_CopyRel(llvm::ELF::R_HEX_COPY) {
}

HexagonLDBackend::~HexagonLDBackend()
{
  delete m_pRelocator;
  delete m_pGOT;
  delete m_pPLT;
  delete m_pRelaDyn;
  delete m_pRelaPLT;
  delete m_pDynamic;
}

bool HexagonLDBackend::initRelocator()
{
  if (NULL == m_pRelocator) {
    m_pRelocator = new HexagonRelocator(*this, config());
  }
  return true;
}

Relocator* HexagonLDBackend::getRelocator()
{
  assert(NULL != m_pRelocator);
  return m_pRelocator;
}

void HexagonLDBackend::doPreLayout(IRBuilder& pBuilder)
{
  // initialize .dynamic data
  if (!config().isCodeStatic() && NULL == m_pDynamic)
    m_pDynamic = new HexagonELFDynamic(*this, config());

  // set .got.plt and .got sizes
  // when building shared object, the .got section is must
  if ((LinkerConfig::Object != config().codeGenType()) &&
      (!config().isCodeStatic())) {
    setGOTSectionSize(pBuilder);

    // set .plt size
    if (m_pPLT->hasPLT1())
      m_pPLT->finalizeSectionSize();

    // set .rela.dyn size
    if (!m_pRelaDyn->empty()) {
      assert(!config().isCodeStatic() &&
            "static linkage should not result in a dynamic relocation section");
      setRelaDynSize();
    }
    // set .rela.plt size
    if (!m_pRelaPLT->empty()) {
      assert(!config().isCodeStatic() &&
            "static linkage should not result in a dynamic relocation section");
      setRelaPLTSize();
    }
  }
  // Shared libraries are compiled with -G0 so there is no need to set SData.
  if (LinkerConfig::Object == config().codeGenType())
    SetSDataSection();
}

void HexagonLDBackend::doPostLayout(Module& pModule, IRBuilder& pBuilder)
{
}

/// dynamic - the dynamic section of the target machine.
/// Use co-variant return type to return its own dynamic section.
HexagonELFDynamic& HexagonLDBackend::dynamic()
{
  assert(NULL != m_pDynamic);
  return *m_pDynamic;
}

/// dynamic - the dynamic section of the target machine.
/// Use co-variant return type to return its own dynamic section.
const HexagonELFDynamic& HexagonLDBackend::dynamic() const
{
  assert(NULL != m_pDynamic);
  return *m_pDynamic;
}

uint64_t HexagonLDBackend::emitSectionData(const LDSection& pSection,
                                          MemoryRegion& pRegion) const
{
  if (!pRegion.size())
    return 0;

  const ELFFileFormat* FileFormat = getOutputFormat();
  unsigned int EntrySize = 0;
  uint64_t RegionSize = 0;

  if ((LinkerConfig::Object != config().codeGenType()) &&
      (!config().isCodeStatic())) {
    if (&pSection == &(FileFormat->getPLT())) {
      assert(m_pPLT && "emitSectionData failed, m_pPLT is NULL!");

      unsigned char* buffer = pRegion.getBuffer();

      m_pPLT->applyPLT0();
      m_pPLT->applyPLT1();
      HexagonPLT::iterator it = m_pPLT->begin();
      unsigned int plt0_size = llvm::cast<PLTEntryBase>((*it)).size();

      memcpy(buffer, llvm::cast<PLTEntryBase>((*it)).getValue(), plt0_size);
      RegionSize += plt0_size;
      ++it;

      PLTEntryBase* plt1 = 0;
      HexagonPLT::iterator ie = m_pPLT->end();
      while (it != ie) {
        plt1 = &(llvm::cast<PLTEntryBase>(*it));
        EntrySize = plt1->size();
        memcpy(buffer + RegionSize, plt1->getValue(), EntrySize);
        RegionSize += EntrySize;
        ++it;
      }
      return RegionSize;
    }
    else if (&pSection == &(FileFormat->getGOT())) {
      RegionSize += emitGOTSectionData(pRegion);
      return RegionSize;
    }
    else if (&pSection == &(FileFormat->getGOTPLT())) {
      RegionSize += emitGOTPLTSectionData(pRegion, FileFormat);
      return RegionSize;
    }
  }

  const SectionData* sect_data = pSection.getSectionData();
  SectionData::const_iterator frag_iter, frag_end = sect_data->end();
  uint8_t* out_offset = pRegion.start();
  for (frag_iter = sect_data->begin(); frag_iter != frag_end; ++frag_iter) {
    size_t size = frag_iter->size();
    switch(frag_iter->getKind()) {
      case Fragment::Fillment: {
        const FillFragment& fill_frag =
          llvm::cast<FillFragment>(*frag_iter);
        if (0 == fill_frag.getValueSize()) {
          // virtual fillment, ignore it.
          break;
        }
        memset(out_offset, fill_frag.getValue(), fill_frag.size());
        break;
      }
      case Fragment::Region: {
        const RegionFragment& region_frag =
          llvm::cast<RegionFragment>(*frag_iter);
        const uint8_t* start = region_frag.getRegion().start();
        memcpy(out_offset, start, size);
        break;
      }
      case Fragment::Alignment: {
        const AlignFragment& align_frag = llvm::cast<AlignFragment>(*frag_iter);
        uint64_t count = size / align_frag.getValueSize();
        switch (align_frag.getValueSize()) {
          case 1u:
            std::memset(out_offset, align_frag.getValue(), count);
            break;
          default:
            llvm::report_fatal_error(
              "unsupported value size for align fragment emission yet.\n");
            break;
        } // end switch
        break;
      }
      case Fragment::Null: {
        assert(0x0 == size);
        break;
      }
      default:
        llvm::report_fatal_error("unsupported fragment type.\n");
        break;
    } // end switch
    out_offset += size;
  } // end for

  return pRegion.size();
}

HexagonGOT& HexagonLDBackend::getGOT()
{
  assert(NULL != m_pGOT);
  return *m_pGOT;
}

const HexagonGOT& HexagonLDBackend::getGOT() const
{
  assert(NULL != m_pGOT);
  return *m_pGOT;
}

HexagonPLT& HexagonLDBackend::getPLT()
{
  assert(NULL != m_pPLT && "PLT section not exist");
  return *m_pPLT;
}

const HexagonPLT& HexagonLDBackend::getPLT() const
{
  assert(NULL != m_pPLT && "PLT section not exist");
  return *m_pPLT;
}

OutputRelocSection& HexagonLDBackend::getRelaDyn()
{
  assert(NULL != m_pRelaDyn && ".rela.dyn section not exist");
  return *m_pRelaDyn;
}

const OutputRelocSection& HexagonLDBackend::getRelaDyn() const
{
  assert(NULL != m_pRelaDyn && ".rela.dyn section not exist");
  return *m_pRelaDyn;
}

OutputRelocSection& HexagonLDBackend::getRelaPLT()
{
  assert(NULL != m_pRelaPLT && ".rela.plt section not exist");
  return *m_pRelaPLT;
}

const OutputRelocSection& HexagonLDBackend::getRelaPLT() const
{
  assert(NULL != m_pRelaPLT && ".rela.plt section not exist");
  return *m_pRelaPLT;
}

HexagonGOTPLT& HexagonLDBackend::getGOTPLT()
{
  assert(NULL != m_pGOTPLT);
  return *m_pGOTPLT;
}

const HexagonGOTPLT& HexagonLDBackend::getGOTPLT() const
{
  assert(NULL != m_pGOTPLT);
  return *m_pGOTPLT;
}

void HexagonLDBackend::setRelaDynSize()
{
  ELFFileFormat* file_format = getOutputFormat();
  file_format->getRelaDyn().setSize
    (m_pRelaDyn->numOfRelocs() * getRelaEntrySize());
}

void HexagonLDBackend::setRelaPLTSize()
{
  ELFFileFormat* file_format = getOutputFormat();
  file_format->getRelaPlt().setSize
    (m_pRelaPLT->numOfRelocs() * getRelaEntrySize());
}

void HexagonLDBackend::setGOTSectionSize(IRBuilder& pBuilder)
{
  // set .got.plt size
  if (LinkerConfig::DynObj == config().codeGenType() ||
      m_pGOTPLT->hasGOT1() ||
      NULL != m_pGOTSymbol) {
    m_pGOTPLT->finalizeSectionSize();
    defineGOTSymbol(pBuilder, *(m_pGOTPLT->begin()));
  }

  // set .got size
  if (!m_pGOT->empty())
    m_pGOT->finalizeSectionSize();
}

uint64_t HexagonLDBackend::emitGOTSectionData(MemoryRegion& pRegion) const
{
  assert(m_pGOT && "emitGOTSectionData failed, m_pGOT is NULL!");

  uint32_t* buffer = reinterpret_cast<uint32_t*>(pRegion.getBuffer());

  HexagonGOTEntry* got = 0;
  unsigned int EntrySize = HexagonGOTEntry::EntrySize;
  uint64_t RegionSize = 0;

  for (HexagonGOT::iterator it = m_pGOT->begin(),
       ie = m_pGOT->end(); it != ie; ++it, ++buffer) {
    got = &(llvm::cast<HexagonGOTEntry>((*it)));
    *buffer = static_cast<uint32_t>(got->getValue());
    RegionSize += EntrySize;
  }

  return RegionSize;
}

void HexagonLDBackend::defineGOTSymbol(IRBuilder& pBuilder,
                                      Fragment& pFrag)
{
  // define symbol _GLOBAL_OFFSET_TABLE_
  if (m_pGOTSymbol != NULL) {
    pBuilder.AddSymbol<IRBuilder::Force, IRBuilder::Unresolve>(
                     "_GLOBAL_OFFSET_TABLE_",
                     ResolveInfo::Object,
                     ResolveInfo::Define,
                     ResolveInfo::Local,
                     0x0, // size
                     0x0, // value
                     FragmentRef::Create(pFrag, 0x0),
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
                     FragmentRef::Create(pFrag, 0x0),
                     ResolveInfo::Hidden);
  }
}

uint64_t HexagonLDBackend::emitGOTPLTSectionData(MemoryRegion& pRegion,
                                         const ELFFileFormat* FileFormat) const
{
  assert(m_pGOTPLT && "emitGOTPLTSectionData failed, m_pGOTPLT is NULL!");
  m_pGOTPLT->applyGOT0(FileFormat->getDynamic().addr());
  m_pGOTPLT->applyAllGOTPLT(*m_pPLT);

  uint32_t* buffer = reinterpret_cast<uint32_t*>(pRegion.getBuffer());

  HexagonGOTEntry* got = 0;
  unsigned int EntrySize = HexagonGOTEntry::EntrySize;
  uint64_t RegionSize = 0;

  for (HexagonGOTPLT::iterator it = m_pGOTPLT->begin(),
       ie = m_pGOTPLT->end(); it != ie; ++it, ++buffer) {
    got = &(llvm::cast<HexagonGOTEntry>((*it)));
    *buffer = static_cast<uint32_t>(got->getValue());
    RegionSize += EntrySize;
  }

  return RegionSize;
}

unsigned int
HexagonLDBackend::getTargetSectionOrder(const LDSection& pSectHdr) const
{
  const ELFFileFormat* file_format = getOutputFormat();

  if (LinkerConfig::Object != config().codeGenType()) {
    if (&pSectHdr == &file_format->getGOT()) {
      if (config().options().hasNow())
        return SHO_RELRO;
      return SHO_RELRO_LAST;
    }

    if (&pSectHdr == &file_format->getGOTPLT()) {
      if (config().options().hasNow())
        return SHO_RELRO;
      return SHO_NON_RELRO_FIRST;
    }

    if (&pSectHdr == &file_format->getPLT())
      return SHO_PLT;
  }

  if (&pSectHdr == m_pstart)
    return SHO_INIT;

  if (&pSectHdr == m_psdata)
    return SHO_SMALL_DATA;

  return SHO_UNDEFINED;
}

void HexagonLDBackend::initTargetSections(Module& pModule,
                                          ObjectBuilder& pBuilder)
{

  if ((LinkerConfig::Object != config().codeGenType()) &&
      (!config().isCodeStatic())) {
    ELFFileFormat* file_format = getOutputFormat();
    // initialize .got
    LDSection& got = file_format->getGOT();
    m_pGOT = new HexagonGOT(got);

    // initialize .got.plt
    LDSection& gotplt = file_format->getGOTPLT();
    m_pGOTPLT = new HexagonGOTPLT(gotplt);

    // initialize .plt
    LDSection& plt = file_format->getPLT();
    m_pPLT = new HexagonPLT(plt,
                        *m_pGOTPLT,
                        config());

    // initialize .rela.plt
    LDSection& relaplt = file_format->getRelaPlt();
    relaplt.setLink(&plt);
    m_pRelaPLT = new OutputRelocSection(pModule, relaplt);

    // initialize .rela.dyn
    LDSection& reladyn = file_format->getRelaDyn();
    m_pRelaDyn = new OutputRelocSection(pModule, reladyn);

  }
  m_psdata = pBuilder.CreateSection(".sdata",
                                    LDFileFormat::Target,
                                    llvm::ELF::SHT_PROGBITS,
                                    llvm::ELF::SHF_ALLOC | llvm::ELF::SHF_WRITE,
                                    4*1024);
  m_pscommon_1 = pBuilder.CreateSection(".scommon.1",
                                    LDFileFormat::Target,
                                    llvm::ELF::SHT_PROGBITS,
                                    llvm::ELF::SHF_ALLOC | llvm::ELF::SHF_WRITE,
                                    1);
  IRBuilder::CreateSectionData(*m_pscommon_1);

  m_pscommon_2 = pBuilder.CreateSection(".scommon.2",
                                    LDFileFormat::Target,
                                    llvm::ELF::SHT_PROGBITS,
                                    llvm::ELF::SHF_ALLOC | llvm::ELF::SHF_WRITE,
                                    2);
  IRBuilder::CreateSectionData(*m_pscommon_2);

  m_pscommon_4 = pBuilder.CreateSection(".scommon.4",
                                    LDFileFormat::Target,
                                    llvm::ELF::SHT_PROGBITS,
                                    llvm::ELF::SHF_ALLOC | llvm::ELF::SHF_WRITE,
                                    4);
  IRBuilder::CreateSectionData(*m_pscommon_4);

  m_pscommon_8 = pBuilder.CreateSection(".scommon.8",
                                    LDFileFormat::Target,
                                    llvm::ELF::SHT_PROGBITS,
                                    llvm::ELF::SHF_ALLOC | llvm::ELF::SHF_WRITE,
                                    8);
  IRBuilder::CreateSectionData(*m_pscommon_8);

  m_pstart = pBuilder.CreateSection(".start",
                                    LDFileFormat::Target,
                                    llvm::ELF::SHT_PROGBITS,
                                    llvm::ELF::SHF_ALLOC | llvm::ELF::SHF_WRITE,
                                    8);
  IRBuilder::CreateSectionData(*m_pstart);
}

void HexagonLDBackend::initTargetSymbols(IRBuilder& pBuilder, Module& pModule)
{
  if (config().codeGenType() == LinkerConfig::Object)
    return;

  // Define the symbol _GLOBAL_OFFSET_TABLE_ if there is a symbol with the
  // same name in input
  m_pGOTSymbol = pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                                  "_GLOBAL_OFFSET_TABLE_",
                                                  ResolveInfo::Object,
                                                  ResolveInfo::Define,
                                                  ResolveInfo::Local,
                                                  0x0,  // size
                                                  0x0,  // value
                                                  FragmentRef::Null(),
                                                  ResolveInfo::Hidden);
  m_psdabase =
    pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                                  "_SDA_BASE_",
                                                  ResolveInfo::Object,
                                                  ResolveInfo::Define,
                                                  ResolveInfo::Absolute,
                                                  0x0,  // size
                                                  0x0,  // value
                                                  FragmentRef::Null(),
                                                  ResolveInfo::Hidden);
  pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                                "__sbss_start",
                                                ResolveInfo::Object,
                                                ResolveInfo::Define,
                                                ResolveInfo::Absolute,
                                                0x0,  // size
                                                0x0,  // value
                                                FragmentRef::Null(),
                                                ResolveInfo::Hidden);
  pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                                "__sbss_end",
                                                ResolveInfo::Object,
                                                ResolveInfo::Define,
                                                ResolveInfo::Absolute,
                                                0x0,  // size
                                                0x0,  // value
                                                FragmentRef::Null(),
                                                ResolveInfo::Hidden);
}

bool HexagonLDBackend::initTargetStubs()
{
  if (NULL != getStubFactory()) {
    getStubFactory()->addPrototype
                        (new HexagonAbsoluteStub(config().isCodeIndep()));
    return true;
  }
  return false;
}

bool HexagonLDBackend::initBRIslandFactory()
{
  if (NULL == m_pBRIslandFactory) {
    m_pBRIslandFactory = new BranchIslandFactory(maxBranchOffset(), 0);
  }
  return true;
}

bool HexagonLDBackend::initStubFactory()
{
  if (NULL == m_pStubFactory) {
    m_pStubFactory = new StubFactory();
  }
  return true;
}

bool HexagonLDBackend::doRelax(Module& pModule, IRBuilder& pBuilder,
                               bool& pFinished)
{
  assert(NULL != getStubFactory() && NULL != getBRIslandFactory());
  bool isRelaxed = false;
  ELFFileFormat* file_format = getOutputFormat();
  // check branch relocs and create the related stubs if needed
  Module::obj_iterator input, inEnd = pModule.obj_end();
  for (input = pModule.obj_begin(); input != inEnd; ++input) {
    LDContext::sect_iterator rs, rsEnd = (*input)->context()->relocSectEnd();
    for (rs = (*input)->context()->relocSectBegin(); rs != rsEnd; ++rs) {
      if (LDFileFormat::Ignore == (*rs)->kind() || !(*rs)->hasRelocData())
        continue;
      RelocData::iterator reloc, rEnd = (*rs)->getRelocData()->end();
      for (reloc = (*rs)->getRelocData()->begin(); reloc != rEnd; ++reloc) {
        switch (reloc->type()) {
          case llvm::ELF::R_HEX_B22_PCREL:
          case llvm::ELF::R_HEX_B15_PCREL:
          case llvm::ELF::R_HEX_B7_PCREL:
          case llvm::ELF::R_HEX_B13_PCREL:
          case llvm::ELF::R_HEX_B9_PCREL: {
            Relocation* relocation = llvm::cast<Relocation>(reloc);
            uint64_t sym_value = 0x0;
            LDSymbol* symbol = relocation->symInfo()->outSymbol();
            if (symbol->hasFragRef()) {
              uint64_t value = symbol->fragRef()->getOutputOffset();
              uint64_t addr =
                symbol->fragRef()->frag()->getParent()->getSection().addr();
              sym_value = addr + value;
            }
            Stub* stub = getStubFactory()->create(*relocation, // relocation
                                                  sym_value, //symbol value
                                                  pBuilder,
                                                  *getBRIslandFactory());
            if (NULL != stub) {
              assert(NULL != stub->symInfo());
              // increase the size of .symtab and .strtab
              LDSection& symtab = file_format->getSymTab();
              LDSection& strtab = file_format->getStrTab();
              symtab.setSize(symtab.size() + sizeof(llvm::ELF::Elf32_Sym));
              strtab.setSize(strtab.size() + stub->symInfo()->nameSize() + 1);
              isRelaxed = true;
            }
          }
          break;

          default:
            break;
        }
      }
    }
  }

  // find the first fragment w/ invalid offset due to stub insertion
  Fragment* invalid = NULL;
  pFinished = true;
  for (BranchIslandFactory::iterator island = getBRIslandFactory()->begin(),
       island_end = getBRIslandFactory()->end(); island != island_end; ++island)
  {
    if ((*island).end() == file_format->getText().getSectionData()->end())
      break;

    Fragment* exit = (*island).end();
    if (((*island).offset() + (*island).size()) > exit->getOffset()) {
      invalid = exit;
      pFinished = false;
      break;
    }
  }

  // reset the offset of invalid fragments
  while (NULL != invalid) {
    invalid->setOffset(invalid->getPrevNode()->getOffset() +
                       invalid->getPrevNode()->size());
    invalid = invalid->getNextNode();
  }

  // reset the size of .text
  if (isRelaxed) {
    file_format->getText().setSize(
      file_format->getText().getSectionData()->back().getOffset() +
      file_format->getText().getSectionData()->back().size());
  }
  return isRelaxed;
}

/// finalizeSymbol - finalize the symbol value
bool HexagonLDBackend::finalizeTargetSymbols()
{
  if (config().codeGenType() == LinkerConfig::Object)
    return true;
  if (m_psdabase)
    m_psdabase->setValue(m_psdata->addr());

  ELFSegment *edata = m_ELFSegmentTable.find(llvm::ELF::PT_LOAD,
                                             llvm::ELF::PF_W, llvm::ELF::PF_X);
  if (NULL != edata) {
    if (NULL != f_pEData && ResolveInfo::ThreadLocal != f_pEData->type()) {
      f_pEData->setValue(edata->vaddr() + edata->filesz());
    }
    if (NULL != f_p_EData && ResolveInfo::ThreadLocal != f_p_EData->type()) {
      f_p_EData->setValue(edata->vaddr() + edata->filesz());
    }
    if (NULL != f_pBSSStart &&
        ResolveInfo::ThreadLocal != f_pBSSStart->type()) {
      f_pBSSStart->setValue(edata->vaddr() + edata->filesz());
    }
    if (NULL != f_pEnd && ResolveInfo::ThreadLocal != f_pEnd->type()) {
      f_pEnd->setValue(((edata->vaddr() +
                       edata->memsz()) + 7) & ~7);
    }
    if (NULL != f_p_End && ResolveInfo::ThreadLocal != f_p_End->type()) {
      f_p_End->setValue(((edata->vaddr() +
                       edata->memsz()) + 7) & ~7);
    }
  }
  return true;
}

/// merge Input Sections
bool HexagonLDBackend::mergeSection(Module& pModule, LDSection& pInputSection)
{
  if ((pInputSection.flag() & llvm::ELF::SHF_HEX_GPREL) ||
      (pInputSection.kind() == LDFileFormat::LinkOnce) ||
      (pInputSection.kind() == LDFileFormat::Target)) {
    SectionData *sd = NULL;
    if (!m_psdata->hasSectionData()) {
      sd = IRBuilder::CreateSectionData(*m_psdata);
      m_psdata->setSectionData(sd);
    }
    sd = m_psdata->getSectionData();
    MoveSectionDataAndSort(*pInputSection.getSectionData(), *sd);
  }
  else {
    ObjectBuilder builder(config(), pModule);
    return builder.MergeSection(pInputSection);
  }
  return true;
}

bool HexagonLDBackend::SetSDataSection() {
  SectionData *pTo = (m_psdata->getSectionData());

  if (pTo) {
    MoveCommonData(*m_pscommon_1->getSectionData(), *pTo);
    MoveCommonData(*m_pscommon_2->getSectionData(), *pTo);
    MoveCommonData(*m_pscommon_4->getSectionData(), *pTo);
    MoveCommonData(*m_pscommon_8->getSectionData(), *pTo);

    SectionData::FragmentListType& to_list = pTo->getFragmentList();
    SectionData::FragmentListType::iterator fragTo, fragToEnd = to_list.end();
    uint32_t offset = 0;
    for (fragTo = to_list.begin(); fragTo != fragToEnd; ++fragTo) {
      fragTo->setOffset(offset);
      offset += fragTo->size();
    }

    // set up pTo's header
    pTo->getSection().setSize(offset);

    SectionData::FragmentListType& newlist = pTo->getFragmentList();

    for (fragTo = newlist.begin(), fragToEnd = newlist.end();
         fragTo != fragToEnd; ++fragTo) {
      fragTo->setParent(pTo);
    }
  }

  return true;
}

/// allocateCommonSymbols - allocate common symbols in the corresponding
/// sections. This is called at pre-layout stage.
/// @refer Google gold linker: common.cc: 214
bool HexagonLDBackend::allocateCommonSymbols(Module& pModule)
{
  SymbolCategory& symbol_list = pModule.getSymbolTable();

  if (symbol_list.emptyCommons() && symbol_list.emptyLocals()) {
    SetSDataSection();
    return true;
  }

  int8_t maxGPSize = config().options().getGPSize();

  SymbolCategory::iterator com_sym, com_end;

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

      switch((*com_sym)->size())  {
      case 1:
        if (maxGPSize <= 0)
          break;
        ObjectBuilder::AppendFragment(*frag,
                                      *(m_pscommon_1->getSectionData()),
                                      (*com_sym)->value());
        continue;
      case 2:
        if (maxGPSize <= 1)
          break;
        ObjectBuilder::AppendFragment(*frag,
                                      *(m_pscommon_2->getSectionData()),
                                      (*com_sym)->value());
        continue;
      case 4:
        if (maxGPSize <= 3)
          break;
        ObjectBuilder::AppendFragment(*frag,
                                      *(m_pscommon_4->getSectionData()),
                                      (*com_sym)->value());
        continue;
      case 8:
        if (maxGPSize <= 7)
          break;
        ObjectBuilder::AppendFragment(*frag,
                                      *(m_pscommon_8->getSectionData()),
                                      (*com_sym)->value());
        continue;
      default:
        break;
      }

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

    switch((*com_sym)->size())  {
    case 1:
      if (maxGPSize <= 0)
        break;
      ObjectBuilder::AppendFragment(*frag,
                                    *(m_pscommon_1->getSectionData()),
                                    (*com_sym)->value());
      continue;
    case 2:
      if (maxGPSize <= 1)
        break;
      ObjectBuilder::AppendFragment(*frag,
                                    *(m_pscommon_2->getSectionData()),
                                    (*com_sym)->value());
      continue;
    case 4:
      if (maxGPSize <= 3)
        break;
      ObjectBuilder::AppendFragment(*frag,
                                    *(m_pscommon_4->getSectionData()),
                                    (*com_sym)->value());
      continue;
    case 8:
      if (maxGPSize <= 7)
        break;
      ObjectBuilder::AppendFragment(*frag,
                                    *(m_pscommon_8->getSectionData()),
                                    (*com_sym)->value());
      continue;
    default:
      break;
    }

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
  SetSDataSection();
  return true;
}

bool HexagonLDBackend::MoveCommonData(SectionData &pFrom, SectionData &pTo)
{
  SectionData::FragmentListType& to_list = pTo.getFragmentList();
  SectionData::FragmentListType::iterator frag, fragEnd = to_list.end();

  uint32_t pFromFlag = pFrom.getSection().align();
  bool found = false;

  SectionData::FragmentListType::iterator fragInsert;

  for (frag = to_list.begin(); frag != fragEnd; ++frag) {
    if (frag->getKind() == mcld::Fragment::Alignment) {
      fragInsert = frag;
      continue;
    }
    if ((frag->getKind() != mcld::Fragment::Region) &&
        (frag->getKind() != mcld::Fragment::Fillment)) {
      continue;
    }
    uint32_t flag = frag->getParent()->getSection().align();
    if (pFromFlag < flag) {
      found = true;
      break;
    }
  }
  AlignFragment* align = NULL;
  if (pFrom.getSection().align() > 1) {
    // if the align constraint is larger than 1, append an alignment
    align = new AlignFragment(pFrom.getSection().align(), // alignment
                              0x0, // the filled value
                              1u,  // the size of filled value
                              pFrom.getSection().align() - 1 // max bytes to emit
                              );
    pFrom.getFragmentList().push_front(align);
  }
  if (found)
    to_list.splice(fragInsert, pFrom.getFragmentList());
  else
    to_list.splice(frag, pFrom.getFragmentList());

  return true;
}

bool HexagonLDBackend::readSection(Input& pInput, SectionData& pSD)
{
  Fragment* frag = NULL;
  uint32_t offset = pInput.fileOffset() + pSD.getSection().offset();
  uint32_t size = pSD.getSection().size();

  if (pSD.getSection().type() == llvm::ELF::SHT_NOBITS) {
    frag = new FillFragment(0x0, 1, size);
  }
  else {
    MemoryRegion* region = pInput.memArea()->request(offset, size);
    if (NULL == region) {
      // If the input section's size is zero, we got a NULL region.
      // use a virtual fill fragment
      frag = new FillFragment(0x0, 0, 0);
    }
    else {
      frag = new RegionFragment(*region);
    }
  }

  ObjectBuilder::AppendFragment(*frag, pSD);
  return true;
}

/// MoveSectionData - move the fragments of pTO section data to pTo
bool HexagonLDBackend::MoveSectionDataAndSort(SectionData& pFrom, SectionData& pTo)
{
  assert(&pFrom != &pTo && "Cannot move section data to itself!");
  SectionData::FragmentListType& to_list = pTo.getFragmentList();
  SectionData::FragmentListType::iterator frag, fragEnd = to_list.end();

  uint32_t pFromFlag = pFrom.getSection().align();
  bool found = false;

  SectionData::FragmentListType::iterator fragInsert;

  for (frag = to_list.begin(); frag != fragEnd; ++frag) {
    if (frag->getKind() == mcld::Fragment::Alignment) {
      fragInsert = frag;
      continue;
    }
    if ((frag->getKind() != mcld::Fragment::Region) &&
        (frag->getKind() != mcld::Fragment::Fillment)) {
      continue;
    }
    uint32_t flag = frag->getParent()->getSection().align();
    if (pFromFlag < flag) {
      found = true;
      break;
    }
  }
  AlignFragment* align = NULL;
  if (pFrom.getSection().align() > 1) {
    // if the align constraint is larger than 1, append an alignment
    align = new AlignFragment(pFrom.getSection().align(), // alignment
                              0x0, // the filled value
                              1u,  // the size of filled value
                              pFrom.getSection().align() - 1 // max bytes to emit
                              );
    pFrom.getFragmentList().push_front(align);
  }
  if (found)
    to_list.splice(fragInsert, pFrom.getFragmentList());
  else
    to_list.splice(frag, pFrom.getFragmentList());

  uint32_t offset = 0;
  for (frag = to_list.begin(); frag != fragEnd; ++frag) {
    frag->setOffset(offset);
    offset += frag->size();
  }

  // set up pTo's header
  pTo.getSection().setSize(offset);

  if (pFrom.getSection().align() > pTo.getSection().align())
    pTo.getSection().setAlign(pFrom.getSection().align());

  if (pFrom.getSection().flag() > pTo.getSection().flag())
    pTo.getSection().setFlag(pFrom.getSection().flag());
  return true;
}

/// doCreateProgramHdrs - backend can implement this function to create the
/// target-dependent segments
void HexagonLDBackend::doCreateProgramHdrs(Module& pModule)
{
  // TODO
}

namespace mcld {

//===----------------------------------------------------------------------===//
/// createHexagonLDBackend - the help funtion to create corresponding
/// HexagonLDBackend
TargetLDBackend* createHexagonLDBackend(const llvm::Target& pTarget,
                                    const LinkerConfig& pConfig)
{
  if (pConfig.targets().triple().isOSDarwin()) {
    assert(0 && "MachO linker is not supported yet");
    /**
    return new HexagonMachOLDBackend(createHexagonMachOArchiveReader,
                               createHexagonMachOObjectReader,
                               createHexagonMachOObjectWriter);
    **/
  }
  if (pConfig.targets().triple().isOSWindows()) {
    assert(0 && "COFF linker is not supported yet");
    /**
    return new HexagonCOFFLDBackend(createHexagonCOFFArchiveReader,
                               createHexagonCOFFObjectReader,
                               createHexagonCOFFObjectWriter);
    **/
  }
  return new HexagonLDBackend(pConfig, new HexagonGNUInfo(pConfig.targets()));
}

} // namespace of mcld

//===----------------------------------------------------------------------===//
// Force static initialization.
//===----------------------------------------------------------------------===//
extern "C" void MCLDInitializeHexagonLDBackend() {
  // Register the linker backend
  mcld::TargetRegistry::RegisterTargetLDBackend(TheHexagonTarget,
                                                createHexagonLDBackend);
}
