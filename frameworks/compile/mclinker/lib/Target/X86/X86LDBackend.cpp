//===- X86LDBackend.cpp ---------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "X86.h"
#include "X86ELFDynamic.h"
#include "X86LDBackend.h"
#include "X86Relocator.h"
#include "X86GNUInfo.h"

#include <llvm/ADT/Triple.h>
#include <llvm/Support/Casting.h>

#include <mcld/LinkerConfig.h>
#include <mcld/IRBuilder.h>
#include <mcld/Fragment/FillFragment.h>
#include <mcld/Fragment/RegionFragment.h>
#include <mcld/Fragment/FragmentLinker.h>
#include <mcld/Support/MemoryRegion.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/Support/TargetRegistry.h>
#include <mcld/Object/ObjectBuilder.h>

#include <cstring>

using namespace mcld;

//===----------------------------------------------------------------------===//
// X86GNULDBackend
//===----------------------------------------------------------------------===//
X86GNULDBackend::X86GNULDBackend(const LinkerConfig& pConfig,
				 GNUInfo* pInfo,
				 Relocation::Type pCopyRel)
  : GNULDBackend(pConfig, pInfo),
    m_pRelocator(NULL),
    m_pPLT(NULL),
    m_pRelDyn(NULL),
    m_pRelPLT(NULL),
    m_pDynamic(NULL),
    m_pGOTSymbol(NULL),
    m_CopyRel(pCopyRel)
{
  Triple::ArchType arch = pConfig.targets().triple().getArch();
  assert (arch == Triple::x86 || arch == Triple::x86_64);
  if (arch == Triple::x86 ||
      pConfig.targets().triple().getEnvironment() == Triple::GNUX32) {
    m_RelEntrySize = 8;
    m_RelaEntrySize = 12;
    if (arch == Triple::x86)
      m_PointerRel = llvm::ELF::R_386_32;
    else
      m_PointerRel = llvm::ELF::R_X86_64_32;
  }
  else {
    m_RelEntrySize = 16;
    m_RelaEntrySize = 24;
    m_PointerRel = llvm::ELF::R_X86_64_64;
  }
}

X86GNULDBackend::~X86GNULDBackend()
{
  delete m_pRelocator;
  delete m_pPLT;
  delete m_pRelDyn;
  delete m_pRelPLT;
  delete m_pDynamic;
}

Relocator* X86GNULDBackend::getRelocator()
{
  assert(NULL != m_pRelocator);
  return m_pRelocator;
}

void X86GNULDBackend::doPreLayout(IRBuilder& pBuilder)
{
  // initialize .dynamic data
  if (!config().isCodeStatic() && NULL == m_pDynamic)
    m_pDynamic = new X86ELFDynamic(*this, config());

  // set .got.plt and .got sizes
  // when building shared object, the .got section is must
  if (LinkerConfig::Object != config().codeGenType()) {
    setGOTSectionSize(pBuilder);

    // set .plt size
    if (m_pPLT->hasPLT1())
      m_pPLT->finalizeSectionSize();

    // set .rel.dyn/.rela.dyn size
    if (!m_pRelDyn->empty()) {
      assert(!config().isCodeStatic() &&
            "static linkage should not result in a dynamic relocation section");
      setRelDynSize();
    }
    // set .rel.plt/.rela.plt size
    if (!m_pRelPLT->empty()) {
      assert(!config().isCodeStatic() &&
            "static linkage should not result in a dynamic relocation section");
      setRelPLTSize();
    }
  }
}

void X86GNULDBackend::doPostLayout(Module& pModule,
                                   IRBuilder& pBuilder)
{
}

/// dynamic - the dynamic section of the target machine.
/// Use co-variant return type to return its own dynamic section.
X86ELFDynamic& X86GNULDBackend::dynamic()
{
  assert(NULL != m_pDynamic);
  return *m_pDynamic;
}

/// dynamic - the dynamic section of the target machine.
/// Use co-variant return type to return its own dynamic section.
const X86ELFDynamic& X86GNULDBackend::dynamic() const
{
  assert(NULL != m_pDynamic);
  return *m_pDynamic;
}

void X86GNULDBackend::defineGOTSymbol(IRBuilder& pBuilder,
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

uint64_t X86GNULDBackend::emitSectionData(const LDSection& pSection,
                                          MemoryRegion& pRegion) const
{
  assert(pRegion.size() && "Size of MemoryRegion is zero!");

  const ELFFileFormat* FileFormat = getOutputFormat();
  assert(FileFormat &&
         "ELFFileFormat is NULL in X86GNULDBackend::emitSectionData!");

  unsigned int EntrySize = 0;
  uint64_t RegionSize = 0;

  if (&pSection == &(FileFormat->getPLT())) {
    assert(m_pPLT && "emitSectionData failed, m_pPLT is NULL!");

    unsigned char* buffer = pRegion.getBuffer();

    m_pPLT->applyPLT0();
    m_pPLT->applyPLT1();
    X86PLT::iterator it = m_pPLT->begin();
    unsigned int plt0_size = llvm::cast<PLTEntryBase>((*it)).size();

    memcpy(buffer, llvm::cast<PLTEntryBase>((*it)).getValue(), plt0_size);
    RegionSize += plt0_size;
    ++it;

    PLTEntryBase* plt1 = 0;
    X86PLT::iterator ie = m_pPLT->end();
    while (it != ie) {
      plt1 = &(llvm::cast<PLTEntryBase>(*it));
      EntrySize = plt1->size();
      memcpy(buffer + RegionSize, plt1->getValue(), EntrySize);
      RegionSize += EntrySize;
      ++it;
    }
  }

  else if (&pSection == &(FileFormat->getGOT())) {
    RegionSize += emitGOTSectionData(pRegion);
  }

  else if (&pSection == &(FileFormat->getGOTPLT())) {
    RegionSize += emitGOTPLTSectionData(pRegion, FileFormat);
  }

  else {
    fatal(diag::unrecognized_output_sectoin)
            << pSection.name()
            << "mclinker@googlegroups.com";
  }
  return RegionSize;
}

X86PLT& X86GNULDBackend::getPLT()
{
  assert(NULL != m_pPLT && "PLT section not exist");
  return *m_pPLT;
}

const X86PLT& X86GNULDBackend::getPLT() const
{
  assert(NULL != m_pPLT && "PLT section not exist");
  return *m_pPLT;
}

OutputRelocSection& X86GNULDBackend::getRelDyn()
{
  assert(NULL != m_pRelDyn && ".rel.dyn/.rela.dyn section not exist");
  return *m_pRelDyn;
}

const OutputRelocSection& X86GNULDBackend::getRelDyn() const
{
  assert(NULL != m_pRelDyn && ".rel.dyn/.rela.dyn section not exist");
  return *m_pRelDyn;
}

OutputRelocSection& X86GNULDBackend::getRelPLT()
{
  assert(NULL != m_pRelPLT && ".rel.plt/.rela.plt section not exist");
  return *m_pRelPLT;
}

const OutputRelocSection& X86GNULDBackend::getRelPLT() const
{
  assert(NULL != m_pRelPLT && ".rel.plt/.rela.plt section not exist");
  return *m_pRelPLT;
}

unsigned int
X86GNULDBackend::getTargetSectionOrder(const LDSection& pSectHdr) const
{
  const ELFFileFormat* file_format = getOutputFormat();

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

  return SHO_UNDEFINED;
}

void X86GNULDBackend::initTargetSymbols(IRBuilder& pBuilder, Module& pModule)
{
  if (LinkerConfig::Object != config().codeGenType()) {
    // Define the symbol _GLOBAL_OFFSET_TABLE_ if there is a symbol with the
    // same name in input
    m_pGOTSymbol =
      pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                                "_GLOBAL_OFFSET_TABLE_",
                                                ResolveInfo::Object,
                                                ResolveInfo::Define,
                                                ResolveInfo::Local,
                                                0x0,  // size
                                                0x0,  // value
                                                FragmentRef::Null(), // FragRef
                                                ResolveInfo::Hidden);
  }
}

/// finalizeSymbol - finalize the symbol value
bool X86GNULDBackend::finalizeTargetSymbols()
{
  return true;
}

/// doCreateProgramHdrs - backend can implement this function to create the
/// target-dependent segments
void X86GNULDBackend::doCreateProgramHdrs(Module& pModule)
{
  // TODO
}

X86_32GNULDBackend::X86_32GNULDBackend(const LinkerConfig& pConfig,
				       GNUInfo* pInfo)
  : X86GNULDBackend(pConfig, pInfo, llvm::ELF::R_386_COPY),
    m_pGOT (NULL),
    m_pGOTPLT (NULL) {
}

X86_32GNULDBackend::~X86_32GNULDBackend()
{
  delete m_pGOT;
  delete m_pGOTPLT;
}

bool X86_32GNULDBackend::initRelocator()
{
  if (NULL == m_pRelocator) {
    m_pRelocator = new X86_32Relocator(*this, config());
  }
  return true;
}

void X86_32GNULDBackend::initTargetSections(Module& pModule,
					    ObjectBuilder& pBuilder)
{
  if (LinkerConfig::Object != config().codeGenType()) {
    ELFFileFormat* file_format = getOutputFormat();
    // initialize .got
    LDSection& got = file_format->getGOT();
    m_pGOT = new X86_32GOT(got);

    // initialize .got.plt
    LDSection& gotplt = file_format->getGOTPLT();
    m_pGOTPLT = new X86_32GOTPLT(gotplt);

    // initialize .plt
    LDSection& plt = file_format->getPLT();
    m_pPLT = new X86_32PLT(plt,
			   *m_pGOTPLT,
			   config());

    // initialize .rel.plt
    LDSection& relplt = file_format->getRelPlt();
    relplt.setLink(&plt);
    m_pRelPLT = new OutputRelocSection(pModule, relplt);

    // initialize .rel.dyn
    LDSection& reldyn = file_format->getRelDyn();
    m_pRelDyn = new OutputRelocSection(pModule, reldyn);

  }
}

X86_32GOT& X86_32GNULDBackend::getGOT()
{
  assert(NULL != m_pGOT);
  return *m_pGOT;
}

const X86_32GOT& X86_32GNULDBackend::getGOT() const
{
  assert(NULL != m_pGOT);
  return *m_pGOT;
}

X86_32GOTPLT& X86_32GNULDBackend::getGOTPLT()
{
  assert(NULL != m_pGOTPLT);
  return *m_pGOTPLT;
}

const X86_32GOTPLT& X86_32GNULDBackend::getGOTPLT() const
{
  assert(NULL != m_pGOTPLT);
  return *m_pGOTPLT;
}

void X86_32GNULDBackend::setRelDynSize()
{
  ELFFileFormat* file_format = getOutputFormat();
  file_format->getRelDyn().setSize
    (m_pRelDyn->numOfRelocs() * getRelEntrySize());
}

void X86_32GNULDBackend::setRelPLTSize()
{
  ELFFileFormat* file_format = getOutputFormat();
  file_format->getRelPlt().setSize
    (m_pRelPLT->numOfRelocs() * getRelEntrySize());
}

void X86_32GNULDBackend::setGOTSectionSize(IRBuilder& pBuilder)
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

uint64_t X86_32GNULDBackend::emitGOTSectionData(MemoryRegion& pRegion) const
{
  assert(m_pGOT && "emitGOTSectionData failed, m_pGOT is NULL!");

  uint32_t* buffer = reinterpret_cast<uint32_t*>(pRegion.getBuffer());

  X86_32GOTEntry* got = 0;
  unsigned int EntrySize = X86_32GOTEntry::EntrySize;
  uint64_t RegionSize = 0;

  for (X86_32GOT::iterator it = m_pGOT->begin(),
       ie = m_pGOT->end(); it != ie; ++it, ++buffer) {
    got = &(llvm::cast<X86_32GOTEntry>((*it)));
    *buffer = static_cast<uint32_t>(got->getValue());
    RegionSize += EntrySize;
  }

  return RegionSize;
}

uint64_t X86_32GNULDBackend::emitGOTPLTSectionData(MemoryRegion& pRegion,
						   const ELFFileFormat* FileFormat) const
{
  assert(m_pGOTPLT && "emitGOTPLTSectionData failed, m_pGOTPLT is NULL!");
  m_pGOTPLT->applyGOT0(FileFormat->getDynamic().addr());
  m_pGOTPLT->applyAllGOTPLT(*m_pPLT);

  uint32_t* buffer = reinterpret_cast<uint32_t*>(pRegion.getBuffer());

  X86_32GOTEntry* got = 0;
  unsigned int EntrySize = X86_32GOTEntry::EntrySize;
  uint64_t RegionSize = 0;

  for (X86_32GOTPLT::iterator it = m_pGOTPLT->begin(),
       ie = m_pGOTPLT->end(); it != ie; ++it, ++buffer) {
    got = &(llvm::cast<X86_32GOTEntry>((*it)));
    *buffer = static_cast<uint32_t>(got->getValue());
    RegionSize += EntrySize;
  }

  return RegionSize;
}

X86_64GNULDBackend::X86_64GNULDBackend(const LinkerConfig& pConfig,
				       GNUInfo* pInfo)
  : X86GNULDBackend(pConfig, pInfo, llvm::ELF::R_X86_64_COPY),
    m_pGOT (NULL),
    m_pGOTPLT (NULL) {
}

X86_64GNULDBackend::~X86_64GNULDBackend()
{
  delete m_pGOT;
  delete m_pGOTPLT;
}

bool X86_64GNULDBackend::initRelocator()
{
  if (NULL == m_pRelocator) {
    m_pRelocator = new X86_64Relocator(*this, config());
  }
  return true;
}

X86_64GOT& X86_64GNULDBackend::getGOT()
{
  assert(NULL != m_pGOT);
  return *m_pGOT;
}

const X86_64GOT& X86_64GNULDBackend::getGOT() const
{
  assert(NULL != m_pGOT);
  return *m_pGOT;
}

X86_64GOTPLT& X86_64GNULDBackend::getGOTPLT()
{
  assert(NULL != m_pGOTPLT);
  return *m_pGOTPLT;
}

const X86_64GOTPLT& X86_64GNULDBackend::getGOTPLT() const
{
  assert(NULL != m_pGOTPLT);
  return *m_pGOTPLT;
}

void X86_64GNULDBackend::setRelDynSize()
{
  ELFFileFormat* file_format = getOutputFormat();
  file_format->getRelaDyn().setSize
    (m_pRelDyn->numOfRelocs() * getRelaEntrySize());
}

void X86_64GNULDBackend::setRelPLTSize()
{
  ELFFileFormat* file_format = getOutputFormat();
  file_format->getRelaPlt().setSize
    (m_pRelPLT->numOfRelocs() * getRelaEntrySize());
}

void X86_64GNULDBackend::initTargetSections(Module& pModule,
					    ObjectBuilder& pBuilder)
{
  if (LinkerConfig::Object != config().codeGenType()) {
    ELFFileFormat* file_format = getOutputFormat();
    // initialize .got
    LDSection& got = file_format->getGOT();
    m_pGOT = new X86_64GOT(got);

    // initialize .got.plt
    LDSection& gotplt = file_format->getGOTPLT();
    m_pGOTPLT = new X86_64GOTPLT(gotplt);

    // initialize .plt
    LDSection& plt = file_format->getPLT();
    m_pPLT = new X86_64PLT(plt,
			   *m_pGOTPLT,
			   config());

    // initialize .rela.plt
    LDSection& relplt = file_format->getRelaPlt();
    relplt.setLink(&plt);
    m_pRelPLT = new OutputRelocSection(pModule, relplt);

    // initialize .rela.dyn
    LDSection& reldyn = file_format->getRelaDyn();
    m_pRelDyn = new OutputRelocSection(pModule, reldyn);

  }
}

void X86_64GNULDBackend::setGOTSectionSize(IRBuilder& pBuilder)
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

uint64_t X86_64GNULDBackend::emitGOTSectionData(MemoryRegion& pRegion) const
{
  assert(m_pGOT && "emitGOTSectionData failed, m_pGOT is NULL!");

  uint64_t* buffer = reinterpret_cast<uint64_t*>(pRegion.getBuffer());

  X86_64GOTEntry* got = 0;
  unsigned int EntrySize = X86_64GOTEntry::EntrySize;
  uint64_t RegionSize = 0;

  for (X86_64GOT::iterator it = m_pGOT->begin(),
       ie = m_pGOT->end(); it != ie; ++it, ++buffer) {
    got = &(llvm::cast<X86_64GOTEntry>((*it)));
    *buffer = static_cast<uint64_t>(got->getValue());
    RegionSize += EntrySize;
  }

  return RegionSize;
}

uint64_t X86_64GNULDBackend::emitGOTPLTSectionData(MemoryRegion& pRegion,
						   const ELFFileFormat* FileFormat) const
{
  assert(m_pGOTPLT && "emitGOTPLTSectionData failed, m_pGOTPLT is NULL!");
  m_pGOTPLT->applyGOT0(FileFormat->getDynamic().addr());
  m_pGOTPLT->applyAllGOTPLT(*m_pPLT);

  uint64_t* buffer = reinterpret_cast<uint64_t*>(pRegion.getBuffer());

  X86_64GOTEntry* got = 0;
  unsigned int EntrySize = X86_64GOTEntry::EntrySize;
  uint64_t RegionSize = 0;

  for (X86_64GOTPLT::iterator it = m_pGOTPLT->begin(),
       ie = m_pGOTPLT->end(); it != ie; ++it, ++buffer) {
    got = &(llvm::cast<X86_64GOTEntry>((*it)));
    *buffer = static_cast<uint64_t>(got->getValue());
    RegionSize += EntrySize;
  }

  return RegionSize;
}

namespace mcld {

//===----------------------------------------------------------------------===//
/// createX86LDBackend - the help funtion to create corresponding X86LDBackend
///
TargetLDBackend* createX86LDBackend(const llvm::Target& pTarget,
                                    const LinkerConfig& pConfig)
{
  if (pConfig.targets().triple().isOSDarwin()) {
    assert(0 && "MachO linker is not supported yet");
    /**
    return new X86MachOLDBackend(createX86MachOArchiveReader,
                               createX86MachOObjectReader,
                               createX86MachOObjectWriter);
    **/
  }
  if (pConfig.targets().triple().isOSWindows()) {
    assert(0 && "COFF linker is not supported yet");
    /**
    return new X86COFFLDBackend(createX86COFFArchiveReader,
                               createX86COFFObjectReader,
                               createX86COFFObjectWriter);
    **/
  }
  Triple::ArchType arch = pConfig.targets().triple().getArch();
  if (arch == Triple::x86)
    return new X86_32GNULDBackend(pConfig,
				  new X86_32GNUInfo(pConfig.targets().triple()));
  assert (arch == Triple::x86_64);
  return new X86_64GNULDBackend(pConfig,
				new X86_64GNUInfo(pConfig.targets().triple()));
}

} // namespace of mcld

//===----------------------------------------------------------------------===//
// Force static initialization.
//===----------------------------------------------------------------------===//
extern "C" void MCLDInitializeX86LDBackend() {
  // Register the linker backend
  mcld::TargetRegistry::RegisterTargetLDBackend(TheX86_32Target, createX86LDBackend);
  mcld::TargetRegistry::RegisterTargetLDBackend(TheX86_64Target, createX86LDBackend);
}
