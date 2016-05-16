//===- GNULDBackend.cpp ---------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Target/GNULDBackend.h>

#include <string>
#include <cstring>
#include <cassert>
#include <vector>
#include <algorithm>
#include <map>

#include <mcld/Module.h>
#include <mcld/LinkerConfig.h>
#include <mcld/IRBuilder.h>
#include <mcld/InputTree.h>
#include <mcld/Config/Config.h>
#include <mcld/ADT/SizeTraits.h>
#include <mcld/LD/LDSymbol.h>
#include <mcld/LD/LDContext.h>
#include <mcld/Fragment/FillFragment.h>
#include <mcld/LD/EhFrame.h>
#include <mcld/LD/EhFrameHdr.h>
#include <mcld/LD/RelocData.h>
#include <mcld/LD/RelocationFactory.h>
#include <mcld/MC/Attribute.h>
#include <mcld/Support/MemoryArea.h>
#include <mcld/Support/MemoryRegion.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/Support/MemoryAreaFactory.h>
#include <mcld/LD/BranchIslandFactory.h>
#include <mcld/LD/StubFactory.h>
#include <mcld/Object/ObjectBuilder.h>

namespace {

//===--------------------------------------------------------------------===//
// non-member functions
//===----------------------------------------------------------------------===//
static const std::string simple_c_identifier_allowed_chars =
  "0123456789"
  "ABCDEFGHIJKLMNOPWRSTUVWXYZ"
  "abcdefghijklmnopqrstuvwxyz"
  "_";

/// isCIdentifier - return if the pName is a valid C identifier
static bool isCIdentifier(const std::string& pName)
{
  return (pName.find_first_not_of(simple_c_identifier_allowed_chars)
              == std::string::npos);
}

} // anonymous namespace

using namespace mcld;

//===----------------------------------------------------------------------===//
// GNULDBackend
//===----------------------------------------------------------------------===//
GNULDBackend::GNULDBackend(const LinkerConfig& pConfig, GNUInfo* pInfo)
  : TargetLDBackend(pConfig),
    m_pObjectReader(NULL),
    m_pDynObjFileFormat(NULL),
    m_pExecFileFormat(NULL),
    m_pObjectFileFormat(NULL),
    m_pInfo(pInfo),
    m_ELFSegmentTable(9), // magic number
    m_pBRIslandFactory(NULL),
    m_pStubFactory(NULL),
    m_pEhFrameHdr(NULL),
    m_bHasTextRel(false),
    m_bHasStaticTLS(false),
    f_pPreInitArrayStart(NULL),
    f_pPreInitArrayEnd(NULL),
    f_pInitArrayStart(NULL),
    f_pInitArrayEnd(NULL),
    f_pFiniArrayStart(NULL),
    f_pFiniArrayEnd(NULL),
    f_pStack(NULL),
    f_pDynamic(NULL),
    f_pTDATA(NULL),
    f_pTBSS(NULL),
    f_pExecutableStart(NULL),
    f_pEText(NULL),
    f_p_EText(NULL),
    f_p__EText(NULL),
    f_pEData(NULL),
    f_p_EData(NULL),
    f_pBSSStart(NULL),
    f_pEnd(NULL),
    f_p_End(NULL) {
  m_pSymIndexMap = new HashTableType(1024);
}

GNULDBackend::~GNULDBackend()
{
  delete m_pInfo;
  delete m_pDynObjFileFormat;
  delete m_pExecFileFormat;
  delete m_pObjectFileFormat;
  delete m_pSymIndexMap;
  delete m_pEhFrameHdr;
  delete m_pBRIslandFactory;
  delete m_pStubFactory;
}

size_t GNULDBackend::sectionStartOffset() const
{
  if (LinkerConfig::Binary == config().codeGenType())
    return 0x0;

  switch (config().targets().bitclass()) {
    case 32u:
      return sizeof(llvm::ELF::Elf32_Ehdr) +
             numOfSegments() * sizeof(llvm::ELF::Elf32_Phdr);
    case 64u:
      return sizeof(llvm::ELF::Elf64_Ehdr) +
             numOfSegments() * sizeof(llvm::ELF::Elf64_Phdr);
    default:
      fatal(diag::unsupported_bitclass) << config().targets().triple().str()
                                        << config().targets().bitclass();
      return 0;
  }
}

uint64_t GNULDBackend::getSegmentStartAddr(const LinkerScript& pScript) const
{
  LinkerScript::AddressMap::const_iterator mapping =
    pScript.addressMap().find(".text");
  if (pScript.addressMap().end() != mapping)
    return mapping.getEntry()->value();
  else if (config().isCodeIndep())
    return 0x0;
  else
    return m_pInfo->defaultTextSegmentAddr();
}

GNUArchiveReader*
GNULDBackend::createArchiveReader(Module& pModule)
{
  assert(NULL != m_pObjectReader);
  return new GNUArchiveReader(pModule, *m_pObjectReader);
}

ELFObjectReader* GNULDBackend::createObjectReader(IRBuilder& pBuilder)
{
  m_pObjectReader = new ELFObjectReader(*this, pBuilder, config());
  return m_pObjectReader;
}

ELFDynObjReader* GNULDBackend::createDynObjReader(IRBuilder& pBuilder)
{
  return new ELFDynObjReader(*this, pBuilder, config());
}

ELFBinaryReader* GNULDBackend::createBinaryReader(IRBuilder& pBuilder)
{
  return new ELFBinaryReader(*this, pBuilder, config());
}

ELFObjectWriter* GNULDBackend::createWriter()
{
  return new ELFObjectWriter(*this, config());
}

bool GNULDBackend::initStdSections(ObjectBuilder& pBuilder)
{
  switch (config().codeGenType()) {
    case LinkerConfig::DynObj: {
      if (NULL == m_pDynObjFileFormat)
        m_pDynObjFileFormat = new ELFDynObjFileFormat();
      m_pDynObjFileFormat->initStdSections(pBuilder,
                                           config().targets().bitclass());
      return true;
    }
    case LinkerConfig::Exec:
    case LinkerConfig::Binary: {
      if (NULL == m_pExecFileFormat)
        m_pExecFileFormat = new ELFExecFileFormat();
      m_pExecFileFormat->initStdSections(pBuilder,
                                         config().targets().bitclass());
      return true;
    }
    case LinkerConfig::Object: {
      if (NULL == m_pObjectFileFormat)
        m_pObjectFileFormat = new ELFObjectFileFormat();
      m_pObjectFileFormat->initStdSections(pBuilder,
                                           config().targets().bitclass());
      return true;
    }
    default:
      fatal(diag::unrecognized_output_file) << config().codeGenType();
      return false;
  }
}

/// initStandardSymbols - define and initialize standard symbols.
/// This function is called after section merging but before read relocations.
bool GNULDBackend::initStandardSymbols(IRBuilder& pBuilder,
                                       Module& pModule)
{
  if (LinkerConfig::Object == config().codeGenType())
    return true;

  // GNU extension: define __start and __stop symbols for the sections whose
  // name can be presented as C symbol
  // ref: GNU gold, Layout::define_section_symbols
  Module::iterator iter, iterEnd = pModule.end();
  for (iter = pModule.begin(); iter != iterEnd; ++iter) {
    LDSection* section = *iter;

    switch (section->kind()) {
      case LDFileFormat::Relocation:
        continue;
      case LDFileFormat::EhFrame:
        if (!section->hasEhFrame())
          continue;
        break;
      default:
        if (!section->hasSectionData())
          continue;
        break;
    } // end of switch

    if (isCIdentifier(section->name())) {
      std::string start_name = "__start_" + section->name();
      FragmentRef* start_fragref = FragmentRef::Create(
                                       section->getSectionData()->front(), 0x0);
      pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                                    start_name,
                                                    ResolveInfo::NoType,
                                                    ResolveInfo::Define,
                                                    ResolveInfo::Global,
                                                    0x0, // size
                                                    0x0, // value
                                                    start_fragref, // FragRef
                                                    ResolveInfo::Default);

      std::string stop_name = "__stop_" + section->name();
      FragmentRef* stop_fragref = FragmentRef::Create(
                           section->getSectionData()->front(), section->size());
      pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                                    stop_name,
                                                    ResolveInfo::NoType,
                                                    ResolveInfo::Define,
                                                    ResolveInfo::Global,
                                                    0x0, // size
                                                    0x0, // value
                                                    stop_fragref, // FragRef
                                                    ResolveInfo::Default);
    }
  }

  ELFFileFormat* file_format = getOutputFormat();

  // -----  section symbols  ----- //
  // .preinit_array
  FragmentRef* preinit_array = NULL;
  if (file_format->hasPreInitArray()) {
    preinit_array = FragmentRef::Create(
                   file_format->getPreInitArray().getSectionData()->front(),
                   0x0);
  }
  else {
    preinit_array = FragmentRef::Null();
  }
  f_pPreInitArrayStart =
     pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                             "__preinit_array_start",
                                             ResolveInfo::NoType,
                                             ResolveInfo::Define,
                                             ResolveInfo::Global,
                                             0x0, // size
                                             0x0, // value
                                             preinit_array, // FragRef
                                             ResolveInfo::Hidden);
  f_pPreInitArrayEnd =
     pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                             "__preinit_array_end",
                                             ResolveInfo::NoType,
                                             ResolveInfo::Define,
                                             ResolveInfo::Global,
                                             0x0, // size
                                             0x0, // value
                                             FragmentRef::Null(), // FragRef
                                             ResolveInfo::Hidden);

  // .init_array
  FragmentRef* init_array = NULL;
  if (file_format->hasInitArray()) {
    init_array = FragmentRef::Create(
                      file_format->getInitArray().getSectionData()->front(),
                      0x0);
  }
  else {
    init_array = FragmentRef::Null();
  }

  f_pInitArrayStart =
     pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                             "__init_array_start",
                                             ResolveInfo::NoType,
                                             ResolveInfo::Define,
                                             ResolveInfo::Global,
                                             0x0, // size
                                             0x0, // value
                                             init_array, // FragRef
                                             ResolveInfo::Hidden);
  f_pInitArrayEnd =
     pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                             "__init_array_end",
                                             ResolveInfo::NoType,
                                             ResolveInfo::Define,
                                             ResolveInfo::Global,
                                             0x0, // size
                                             0x0, // value
                                             init_array, // FragRef
                                             ResolveInfo::Hidden);

  // .fini_array
  FragmentRef* fini_array = NULL;
  if (file_format->hasFiniArray()) {
    fini_array = FragmentRef::Create(
                     file_format->getFiniArray().getSectionData()->front(),
                     0x0);
  }
  else {
    fini_array = FragmentRef::Null();
  }

  f_pFiniArrayStart =
     pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                             "__fini_array_start",
                                             ResolveInfo::NoType,
                                             ResolveInfo::Define,
                                             ResolveInfo::Global,
                                             0x0, // size
                                             0x0, // value
                                             fini_array, // FragRef
                                             ResolveInfo::Hidden);
  f_pFiniArrayEnd =
     pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                             "__fini_array_end",
                                             ResolveInfo::NoType,
                                             ResolveInfo::Define,
                                             ResolveInfo::Global,
                                             0x0, // size
                                             0x0, // value
                                             fini_array, // FragRef
                                             ResolveInfo::Hidden);

  // .stack
  FragmentRef* stack = NULL;
  if (file_format->hasStack()) {
    stack = FragmentRef::Create(
                          file_format->getStack().getSectionData()->front(),
                          0x0);
  }
  else {
    stack = FragmentRef::Null();
  }

  f_pStack =
     pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                             "__stack",
                                             ResolveInfo::NoType,
                                             ResolveInfo::Define,
                                             ResolveInfo::Global,
                                             0x0, // size
                                             0x0, // value
                                             stack, // FragRef
                                             ResolveInfo::Hidden);

  // _DYNAMIC
  // TODO: add SectionData for .dynamic section, and then we can get the correct
  // symbol section index for _DYNAMIC. Now it will be ABS.
  f_pDynamic =
     pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                                   "_DYNAMIC",
                                                   ResolveInfo::Object,
                                                   ResolveInfo::Define,
                                                   ResolveInfo::Local,
                                                   0x0, // size
                                                   0x0, // value
                                                   FragmentRef::Null(), // FragRef
                                                   ResolveInfo::Hidden);

  // -----  segment symbols  ----- //
  f_pExecutableStart =
     pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                             "__executable_start",
                                             ResolveInfo::NoType,
                                             ResolveInfo::Define,
                                             ResolveInfo::Absolute,
                                             0x0, // size
                                             0x0, // value
                                             FragmentRef::Null(), // FragRef
                                             ResolveInfo::Default);
  f_pEText =
     pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                             "etext",
                                             ResolveInfo::NoType,
                                             ResolveInfo::Define,
                                             ResolveInfo::Absolute,
                                             0x0, // size
                                             0x0, // value
                                             FragmentRef::Null(), // FragRef
                                             ResolveInfo::Default);
  f_p_EText =
     pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                             "_etext",
                                             ResolveInfo::NoType,
                                             ResolveInfo::Define,
                                             ResolveInfo::Absolute,
                                             0x0, // size
                                             0x0, // value
                                             FragmentRef::Null(), // FragRef
                                             ResolveInfo::Default);
  f_p__EText =
     pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                             "__etext",
                                             ResolveInfo::NoType,
                                             ResolveInfo::Define,
                                             ResolveInfo::Absolute,
                                             0x0, // size
                                             0x0, // value
                                             FragmentRef::Null(), // FragRef
                                             ResolveInfo::Default);
  f_pEData =
     pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                             "edata",
                                             ResolveInfo::NoType,
                                             ResolveInfo::Define,
                                             ResolveInfo::Absolute,
                                             0x0, // size
                                             0x0, // value
                                             FragmentRef::Null(), // FragRef
                                             ResolveInfo::Default);

  f_pEnd =
     pBuilder.AddSymbol<IRBuilder::AsReferred, IRBuilder::Resolve>(
                                             "end",
                                             ResolveInfo::NoType,
                                             ResolveInfo::Define,
                                             ResolveInfo::Absolute,
                                             0x0, // size
                                             0x0, // value
                                             FragmentRef::Null(), // FragRef
                                             ResolveInfo::Default);

  // _edata is defined forcefully.
  // @ref Google gold linker: defstd.cc: 186
  f_p_EData =
     pBuilder.AddSymbol<IRBuilder::Force, IRBuilder::Resolve>(
                                             "_edata",
                                             ResolveInfo::NoType,
                                             ResolveInfo::Define,
                                             ResolveInfo::Absolute,
                                             0x0, // size
                                             0x0, // value
                                             FragmentRef::Null(), // FragRef
                                             ResolveInfo::Default);

  // __bss_start is defined forcefully.
  // @ref Google gold linker: defstd.cc: 214
  f_pBSSStart =
     pBuilder.AddSymbol<IRBuilder::Force, IRBuilder::Resolve>(
                                             "__bss_start",
                                             ResolveInfo::NoType,
                                             ResolveInfo::Define,
                                             ResolveInfo::Absolute,
                                             0x0, // size
                                             0x0, // value
                                             FragmentRef::Null(), // FragRef
                                             ResolveInfo::Default);

  // _end is defined forcefully.
  // @ref Google gold linker: defstd.cc: 228
  f_p_End =
     pBuilder.AddSymbol<IRBuilder::Force, IRBuilder::Resolve>(
                                             "_end",
                                             ResolveInfo::NoType,
                                             ResolveInfo::Define,
                                             ResolveInfo::Absolute,
                                             0x0, // size
                                             0x0, // value
                                             FragmentRef::Null(), // FragRef
                                             ResolveInfo::Default);

  return true;
}

bool GNULDBackend::finalizeStandardSymbols()
{
  if (LinkerConfig::Object == config().codeGenType())
    return true;

  ELFFileFormat* file_format = getOutputFormat();

  // -----  section symbols  ----- //
  if (NULL != f_pPreInitArrayStart) {
    if (!f_pPreInitArrayStart->hasFragRef()) {
      f_pPreInitArrayStart->resolveInfo()->setBinding(ResolveInfo::Absolute);
      f_pPreInitArrayStart->setValue(0x0);
    }
  }

  if (NULL != f_pPreInitArrayEnd) {
    if (f_pPreInitArrayEnd->hasFragRef()) {
      f_pPreInitArrayEnd->setValue(f_pPreInitArrayEnd->value() +
                                   file_format->getPreInitArray().size());
    }
    else {
      f_pPreInitArrayEnd->resolveInfo()->setBinding(ResolveInfo::Absolute);
      f_pPreInitArrayEnd->setValue(0x0);
    }
  }

  if (NULL != f_pInitArrayStart) {
    if (!f_pInitArrayStart->hasFragRef()) {
      f_pInitArrayStart->resolveInfo()->setBinding(ResolveInfo::Absolute);
      f_pInitArrayStart->setValue(0x0);
    }
  }

  if (NULL != f_pInitArrayEnd) {
    if (f_pInitArrayEnd->hasFragRef()) {
      f_pInitArrayEnd->setValue(f_pInitArrayEnd->value() +
                                file_format->getInitArray().size());
    }
    else {
      f_pInitArrayEnd->resolveInfo()->setBinding(ResolveInfo::Absolute);
      f_pInitArrayEnd->setValue(0x0);
    }
  }

  if (NULL != f_pFiniArrayStart) {
    if (!f_pFiniArrayStart->hasFragRef()) {
      f_pFiniArrayStart->resolveInfo()->setBinding(ResolveInfo::Absolute);
      f_pFiniArrayStart->setValue(0x0);
    }
  }

  if (NULL != f_pFiniArrayEnd) {
    if (f_pFiniArrayEnd->hasFragRef()) {
      f_pFiniArrayEnd->setValue(f_pFiniArrayEnd->value() +
                                file_format->getFiniArray().size());
    }
    else {
      f_pFiniArrayEnd->resolveInfo()->setBinding(ResolveInfo::Absolute);
      f_pFiniArrayEnd->setValue(0x0);
    }
  }

  if (NULL != f_pStack) {
    if (!f_pStack->hasFragRef()) {
      f_pStack->resolveInfo()->setBinding(ResolveInfo::Absolute);
      f_pStack->setValue(0x0);
    }
  }

  if (NULL != f_pDynamic) {
    f_pDynamic->resolveInfo()->setBinding(ResolveInfo::Local);
    f_pDynamic->setValue(file_format->getDynamic().addr());
    f_pDynamic->setSize(file_format->getDynamic().size());
  }

  // -----  segment symbols  ----- //
  if (NULL != f_pExecutableStart) {
    ELFSegment* exec_start = m_ELFSegmentTable.find(llvm::ELF::PT_LOAD, 0x0, 0x0);
    if (NULL != exec_start) {
      if (ResolveInfo::ThreadLocal != f_pExecutableStart->type()) {
        f_pExecutableStart->setValue(f_pExecutableStart->value() +
                                     exec_start->vaddr());
      }
    }
    else
      f_pExecutableStart->setValue(0x0);
  }

  if (NULL != f_pEText || NULL != f_p_EText || NULL !=f_p__EText) {
    ELFSegment* etext = m_ELFSegmentTable.find(llvm::ELF::PT_LOAD,
                                               llvm::ELF::PF_X,
                                               llvm::ELF::PF_W);
    if (NULL != etext) {
      if (NULL != f_pEText && ResolveInfo::ThreadLocal != f_pEText->type()) {
        f_pEText->setValue(f_pEText->value() +
                           etext->vaddr() +
                           etext->memsz());
      }
      if (NULL != f_p_EText && ResolveInfo::ThreadLocal != f_p_EText->type()) {
        f_p_EText->setValue(f_p_EText->value() +
                            etext->vaddr() +
                            etext->memsz());
      }
      if (NULL != f_p__EText && ResolveInfo::ThreadLocal != f_p__EText->type()) {
        f_p__EText->setValue(f_p__EText->value() +
                            etext->vaddr() +
                            etext->memsz());
      }
    }
    else {
      if (NULL != f_pEText)
        f_pEText->setValue(0x0);
      if (NULL != f_p_EText)
        f_p_EText->setValue(0x0);
      if (NULL != f_p__EText)
        f_p__EText->setValue(0x0);
    }
  }

  if (NULL != f_pEData || NULL != f_p_EData || NULL != f_pBSSStart ||
      NULL != f_pEnd || NULL != f_p_End) {
    ELFSegment* edata = m_ELFSegmentTable.find(llvm::ELF::PT_LOAD,
                                               llvm::ELF::PF_W,
                                               0x0);
    if (NULL != edata) {
      if (NULL != f_pEData && ResolveInfo::ThreadLocal != f_pEData->type()) {
        f_pEData->setValue(f_pEData->value() +
                            edata->vaddr() +
                            edata->filesz());
      }
      if (NULL != f_p_EData && ResolveInfo::ThreadLocal != f_p_EData->type()) {
        f_p_EData->setValue(f_p_EData->value() +
                            edata->vaddr() +
                            edata->filesz());
      }
      if (NULL != f_pBSSStart && ResolveInfo::ThreadLocal != f_pBSSStart->type()) {
        f_pBSSStart->setValue(f_pBSSStart->value() +
                              edata->vaddr() +
                              edata->filesz());
      }

      if (NULL != f_pEnd && ResolveInfo::ThreadLocal != f_pEnd->type()) {
        f_pEnd->setValue(f_pEnd->value() +
                         edata->vaddr() +
                         edata->memsz());
      }
      if (NULL != f_p_End && ResolveInfo::ThreadLocal != f_p_End->type()) {
        f_p_End->setValue(f_p_End->value() +
                          edata->vaddr() +
                          edata->memsz());
      }
    }
    else {
      if (NULL != f_pEData)
        f_pEData->setValue(0x0);
      if (NULL != f_p_EData)
        f_p_EData->setValue(0x0);
      if (NULL != f_pBSSStart)
        f_pBSSStart->setValue(0x0);

      if (NULL != f_pEnd)
        f_pEnd->setValue(0x0);
      if (NULL != f_p_End)
        f_p_End->setValue(0x0);
    }
  }

  return true;
}

bool GNULDBackend::finalizeTLSSymbol(LDSymbol& pSymbol)
{
  // ignore if symbol has no fragRef
  if (!pSymbol.hasFragRef())
    return true;

  // the value of a TLS symbol is the offset to the TLS segment
  ELFSegment* tls_seg = m_ELFSegmentTable.find(llvm::ELF::PT_TLS,
                                               llvm::ELF::PF_R, 0x0);
  uint64_t value = pSymbol.fragRef()->getOutputOffset();
  uint64_t addr  = pSymbol.fragRef()->frag()->getParent()->getSection().addr();
  pSymbol.setValue(value + addr - tls_seg->vaddr());
  return true;
}

ELFFileFormat* GNULDBackend::getOutputFormat()
{
  switch (config().codeGenType()) {
    case LinkerConfig::DynObj:
      assert(NULL != m_pDynObjFileFormat);
      return m_pDynObjFileFormat;
    case LinkerConfig::Exec:
    case LinkerConfig::Binary:
      assert(NULL != m_pExecFileFormat);
      return m_pExecFileFormat;
    case LinkerConfig::Object:
      assert(NULL != m_pObjectFileFormat);
      return m_pObjectFileFormat;
    default:
      fatal(diag::unrecognized_output_file) << config().codeGenType();
      return NULL;
  }
}

const ELFFileFormat* GNULDBackend::getOutputFormat() const
{
  switch (config().codeGenType()) {
    case LinkerConfig::DynObj:
      assert(NULL != m_pDynObjFileFormat);
      return m_pDynObjFileFormat;
    case LinkerConfig::Exec:
    case LinkerConfig::Binary:
      assert(NULL != m_pExecFileFormat);
      return m_pExecFileFormat;
    case LinkerConfig::Object:
      assert(NULL != m_pObjectFileFormat);
      return m_pObjectFileFormat;
    default:
      fatal(diag::unrecognized_output_file) << config().codeGenType();
      return NULL;
  }
}

/// sizeNamePools - compute the size of regular name pools
/// In ELF executable files, regular name pools are .symtab, .strtab,
/// .dynsym, .dynstr, .hash and .shstrtab.
void GNULDBackend::sizeNamePools(Module& pModule)
{
  assert(LinkerConfig::Unset != config().codePosition());

  // number of entries in symbol tables starts from 1 to hold the special entry
  // at index 0 (STN_UNDEF). See ELF Spec Book I, p1-21.
  size_t symtab = 1;
  size_t dynsym = config().isCodeStatic()? 0 : 1;

  // size of string tables starts from 1 to hold the null character in their
  // first byte
  size_t strtab   = 1;
  size_t dynstr   = config().isCodeStatic()? 0 : 1;
  size_t shstrtab = 1;
  size_t hash     = 0;
  size_t gnuhash  = 0;

  // number of local symbol in the .symtab and .dynsym
  size_t symtab_local_cnt = 0;
  size_t dynsym_local_cnt = 0;

  Module::SymbolTable& symbols = pModule.getSymbolTable();
  Module::const_sym_iterator symbol, symEnd;
  /// Compute the size of .symtab, .strtab, and symtab_local_cnt
  /// @{
  symEnd = symbols.end();
  for (symbol = symbols.begin(); symbol != symEnd; ++symbol) {
    ++symtab;
    if (hasEntryInStrTab(**symbol))
      strtab += (*symbol)->nameSize() + 1;
  }
  symtab_local_cnt = 1 + symbols.numOfFiles() + symbols.numOfLocals() +
                     symbols.numOfLocalDyns();

  ELFFileFormat* file_format = getOutputFormat();

  switch(config().codeGenType()) {
    case LinkerConfig::DynObj: {
      // soname
      dynstr += pModule.name().size() + 1;
    }
    /** fall through **/
    case LinkerConfig::Exec:
    case LinkerConfig::Binary: {
      if (!config().isCodeStatic()) {
        /// Compute the size of .dynsym, .dynstr, and dynsym_local_cnt
        symEnd = symbols.dynamicEnd();
        for (symbol = symbols.localDynBegin(); symbol != symEnd; ++symbol) {
          ++dynsym;
          if (hasEntryInStrTab(**symbol))
            dynstr += (*symbol)->nameSize() + 1;
        }
        dynsym_local_cnt = 1 + symbols.numOfLocalDyns();

        // compute .gnu.hash
        if (GeneralOptions::GNU  == config().options().getHashStyle() ||
            GeneralOptions::Both == config().options().getHashStyle()) {
          // count the number of dynsym to hash
          size_t hashed_sym_cnt = 0;
          symEnd = symbols.dynamicEnd();
          for (symbol = symbols.dynamicBegin(); symbol != symEnd; ++symbol) {
            if (DynsymCompare().needGNUHash(**symbol))
              ++hashed_sym_cnt;
          }
          // Special case for empty .dynsym
          if (hashed_sym_cnt == 0)
            gnuhash = 5 * 4 + config().targets().bitclass() / 8;
          else {
            size_t nbucket = getHashBucketCount(hashed_sym_cnt, true);
            gnuhash = (4 + nbucket + hashed_sym_cnt) * 4;
            gnuhash += (1U << getGNUHashMaskbitslog2(hashed_sym_cnt)) / 8;
          }
        }

        // compute .hash
        if (GeneralOptions::SystemV == config().options().getHashStyle() ||
            GeneralOptions::Both == config().options().getHashStyle()) {
          // Both Elf32_Word and Elf64_Word are 4 bytes
          hash = (2 + getHashBucketCount(dynsym, false) + dynsym) *
                 sizeof(llvm::ELF::Elf32_Word);
        }

        // add DT_NEEDED
        Module::const_lib_iterator lib, libEnd = pModule.lib_end();
        for (lib = pModule.lib_begin(); lib != libEnd; ++lib) {
          if (!(*lib)->attribute()->isAsNeeded() || (*lib)->isNeeded()) {
            dynstr += (*lib)->name().size() + 1;
            dynamic().reserveNeedEntry();
          }
        }

        // add DT_RPATH
        if (!config().options().getRpathList().empty()) {
          dynamic().reserveNeedEntry();
          GeneralOptions::const_rpath_iterator rpath,
            rpathEnd = config().options().rpath_end();
          for (rpath = config().options().rpath_begin();
               rpath != rpathEnd; ++rpath)
            dynstr += (*rpath).size() + 1;
        }

        // set size
        if (config().targets().is32Bits()) {
          file_format->getDynSymTab().setSize(dynsym *
                                              sizeof(llvm::ELF::Elf32_Sym));
        } else {
          file_format->getDynSymTab().setSize(dynsym *
                                              sizeof(llvm::ELF::Elf64_Sym));
        }
        file_format->getDynStrTab().setSize(dynstr);
        file_format->getHashTab().setSize(hash);
        file_format->getGNUHashTab().setSize(gnuhash);

        // set .dynsym sh_info to one greater than the symbol table
        // index of the last local symbol
        file_format->getDynSymTab().setInfo(dynsym_local_cnt);

        // Because some entries in .dynamic section need information of .dynsym,
        // .dynstr, .symtab, .strtab and .hash, we can not reserve non-DT_NEEDED
        // entries until we get the size of the sections mentioned above
        dynamic().reserveEntries(*file_format);
        file_format->getDynamic().setSize(dynamic().numOfBytes());
      }
    }
    /* fall through */
    case LinkerConfig::Object: {
      if (config().targets().is32Bits())
        file_format->getSymTab().setSize(symtab*sizeof(llvm::ELF::Elf32_Sym));
      else
        file_format->getSymTab().setSize(symtab*sizeof(llvm::ELF::Elf64_Sym));
      file_format->getStrTab().setSize(strtab);

      // set .symtab sh_info to one greater than the symbol table
      // index of the last local symbol
      file_format->getSymTab().setInfo(symtab_local_cnt);

      // compute the size of .shstrtab section.
      Module::const_iterator sect, sectEnd = pModule.end();
      for (sect = pModule.begin(); sect != sectEnd; ++sect) {
        switch ((*sect)->kind()) {
        case LDFileFormat::Null:
          break;
        // take StackNote directly
        case LDFileFormat::StackNote:
          shstrtab += ((*sect)->name().size() + 1);
          break;
        case LDFileFormat::EhFrame:
          if (((*sect)->size() != 0) ||
              ((*sect)->hasEhFrame() &&
               config().codeGenType() == LinkerConfig::Object))
            shstrtab += ((*sect)->name().size() + 1);
          break;
        case LDFileFormat::Relocation:
          if (((*sect)->size() != 0) ||
              ((*sect)->hasRelocData() &&
               config().codeGenType() == LinkerConfig::Object))
            shstrtab += ((*sect)->name().size() + 1);
          break;
        default:
          if (((*sect)->size() != 0) ||
              ((*sect)->hasSectionData() &&
               config().codeGenType() == LinkerConfig::Object))
            shstrtab += ((*sect)->name().size() + 1);
          break;
        } // end of switch
      } // end of for
      shstrtab += (strlen(".shstrtab") + 1);
      file_format->getShStrTab().setSize(shstrtab);
      break;
    }
    default:
      fatal(diag::fatal_illegal_codegen_type) << pModule.name();
      break;
  } // end of switch
}

/// emitSymbol32 - emit an ELF32 symbol
void GNULDBackend::emitSymbol32(llvm::ELF::Elf32_Sym& pSym,
                                LDSymbol& pSymbol,
                                char* pStrtab,
                                size_t pStrtabsize,
                                size_t pSymtabIdx)
{
   // FIXME: check the endian between host and target
   // write out symbol
   if (hasEntryInStrTab(pSymbol)) {
     pSym.st_name  = pStrtabsize;
     strcpy((pStrtab + pStrtabsize), pSymbol.name());
   }
   else {
     pSym.st_name  = 0;
   }
   pSym.st_value = pSymbol.value();
   pSym.st_size  = getSymbolSize(pSymbol);
   pSym.st_info  = getSymbolInfo(pSymbol);
   pSym.st_other = pSymbol.visibility();
   pSym.st_shndx = getSymbolShndx(pSymbol);
}

/// emitSymbol64 - emit an ELF64 symbol
void GNULDBackend::emitSymbol64(llvm::ELF::Elf64_Sym& pSym,
                                LDSymbol& pSymbol,
                                char* pStrtab,
                                size_t pStrtabsize,
                                size_t pSymtabIdx)
{
   // FIXME: check the endian between host and target
   // write out symbol
   if (hasEntryInStrTab(pSymbol)) {
     pSym.st_name  = pStrtabsize;
     strcpy((pStrtab + pStrtabsize), pSymbol.name());
   }
   else {
     pSym.st_name  = 0;
   }
   pSym.st_value = pSymbol.value();
   pSym.st_size  = getSymbolSize(pSymbol);
   pSym.st_info  = getSymbolInfo(pSymbol);
   pSym.st_other = pSymbol.visibility();
   pSym.st_shndx = getSymbolShndx(pSymbol);
}

/// emitRegNamePools - emit regular name pools - .symtab, .strtab
///
/// the size of these tables should be computed before layout
/// layout should computes the start offset of these tables
void GNULDBackend::emitRegNamePools(const Module& pModule,
                                    MemoryArea& pOutput)
{
  ELFFileFormat* file_format = getOutputFormat();

  LDSection& symtab_sect = file_format->getSymTab();
  LDSection& strtab_sect = file_format->getStrTab();

  MemoryRegion* symtab_region = pOutput.request(symtab_sect.offset(),
                                                symtab_sect.size());
  MemoryRegion* strtab_region = pOutput.request(strtab_sect.offset(),
                                                strtab_sect.size());

  // set up symtab_region
  llvm::ELF::Elf32_Sym* symtab32 = NULL;
  llvm::ELF::Elf64_Sym* symtab64 = NULL;
  if (config().targets().is32Bits())
    symtab32 = (llvm::ELF::Elf32_Sym*)symtab_region->start();
  else if (config().targets().is64Bits())
    symtab64 = (llvm::ELF::Elf64_Sym*)symtab_region->start();
  else {
    fatal(diag::unsupported_bitclass) << config().targets().triple().str()
                                      << config().targets().bitclass();
  }

  // set up strtab_region
  char* strtab = (char*)strtab_region->start();

  // emit the first ELF symbol
  if (config().targets().is32Bits())
    emitSymbol32(symtab32[0], *LDSymbol::Null(), strtab, 0, 0);
  else
    emitSymbol64(symtab64[0], *LDSymbol::Null(), strtab, 0, 0);

  bool sym_exist = false;
  HashTableType::entry_type* entry = NULL;
  if (LinkerConfig::Object == config().codeGenType()) {
    entry = m_pSymIndexMap->insert(LDSymbol::Null(), sym_exist);
    entry->setValue(0);
  }

  size_t symIdx = 1;
  size_t strtabsize = 1;

  const Module::SymbolTable& symbols = pModule.getSymbolTable();
  Module::const_sym_iterator symbol, symEnd;

  symEnd = symbols.end();
  for (symbol = symbols.begin(); symbol != symEnd; ++symbol) {
    if (LinkerConfig::Object == config().codeGenType()) {
      entry = m_pSymIndexMap->insert(*symbol, sym_exist);
      entry->setValue(symIdx);
    }
    if (config().targets().is32Bits())
      emitSymbol32(symtab32[symIdx], **symbol, strtab, strtabsize, symIdx);
    else
      emitSymbol64(symtab64[symIdx], **symbol, strtab, strtabsize, symIdx);
    ++symIdx;
    if (hasEntryInStrTab(**symbol))
      strtabsize += (*symbol)->nameSize() + 1;
  }
}

/// emitDynNamePools - emit dynamic name pools - .dyntab, .dynstr, .hash
///
/// the size of these tables should be computed before layout
/// layout should computes the start offset of these tables
void GNULDBackend::emitDynNamePools(Module& pModule, MemoryArea& pOutput)
{
  ELFFileFormat* file_format = getOutputFormat();
  if (!file_format->hasDynSymTab() ||
      !file_format->hasDynStrTab() ||
      !file_format->hasDynamic())
    return;

  bool sym_exist = false;
  HashTableType::entry_type* entry = 0;

  LDSection& symtab_sect = file_format->getDynSymTab();
  LDSection& strtab_sect = file_format->getDynStrTab();
  LDSection& dyn_sect    = file_format->getDynamic();

  MemoryRegion* symtab_region = pOutput.request(symtab_sect.offset(),
                                                symtab_sect.size());
  MemoryRegion* strtab_region = pOutput.request(strtab_sect.offset(),
                                                strtab_sect.size());
  MemoryRegion* dyn_region    = pOutput.request(dyn_sect.offset(),
                                                dyn_sect.size());
  // set up symtab_region
  llvm::ELF::Elf32_Sym* symtab32 = NULL;
  llvm::ELF::Elf64_Sym* symtab64 = NULL;
  if (config().targets().is32Bits())
    symtab32 = (llvm::ELF::Elf32_Sym*)symtab_region->start();
  else if (config().targets().is64Bits())
    symtab64 = (llvm::ELF::Elf64_Sym*)symtab_region->start();
  else {
    fatal(diag::unsupported_bitclass) << config().targets().triple().str()
                                      << config().targets().bitclass();
  }

  // set up strtab_region
  char* strtab = (char*)strtab_region->start();

  // emit the first ELF symbol
  if (config().targets().is32Bits())
    emitSymbol32(symtab32[0], *LDSymbol::Null(), strtab, 0, 0);
  else
    emitSymbol64(symtab64[0], *LDSymbol::Null(), strtab, 0, 0);

  size_t symIdx = 1;
  size_t strtabsize = 1;

  Module::SymbolTable& symbols = pModule.getSymbolTable();
  // emit .gnu.hash
  if (GeneralOptions::GNU  == config().options().getHashStyle() ||
      GeneralOptions::Both == config().options().getHashStyle())
    emitGNUHashTab(symbols, pOutput);

  // emit .hash
  if (GeneralOptions::SystemV == config().options().getHashStyle() ||
      GeneralOptions::Both == config().options().getHashStyle())
    emitELFHashTab(symbols, pOutput);

  // emit .dynsym, and .dynstr (emit LocalDyn and Dynamic category)
  Module::const_sym_iterator symbol, symEnd = symbols.dynamicEnd();
  for (symbol = symbols.localDynBegin(); symbol != symEnd; ++symbol) {
    if (config().targets().is32Bits())
      emitSymbol32(symtab32[symIdx], **symbol, strtab, strtabsize, symIdx);
    else
      emitSymbol64(symtab64[symIdx], **symbol, strtab, strtabsize, symIdx);
    // maintain output's symbol and index map
    entry = m_pSymIndexMap->insert(*symbol, sym_exist);
    entry->setValue(symIdx);
    // sum up counters
    ++symIdx;
    if (hasEntryInStrTab(**symbol))
      strtabsize += (*symbol)->nameSize() + 1;
  }

  // emit DT_NEED
  // add DT_NEED strings into .dynstr
  ELFDynamic::iterator dt_need = dynamic().needBegin();
  Module::const_lib_iterator lib, libEnd = pModule.lib_end();
  for (lib = pModule.lib_begin(); lib != libEnd; ++lib) {
    if (!(*lib)->attribute()->isAsNeeded() || (*lib)->isNeeded()) {
      strcpy((strtab + strtabsize), (*lib)->name().c_str());
      (*dt_need)->setValue(llvm::ELF::DT_NEEDED, strtabsize);
      strtabsize += (*lib)->name().size() + 1;
      ++dt_need;
    }
  }

  if (!config().options().getRpathList().empty()) {
    if (!config().options().hasNewDTags())
      (*dt_need)->setValue(llvm::ELF::DT_RPATH, strtabsize);
    else
      (*dt_need)->setValue(llvm::ELF::DT_RUNPATH, strtabsize);
    ++dt_need;

    GeneralOptions::const_rpath_iterator rpath,
      rpathEnd = config().options().rpath_end();
    for (rpath = config().options().rpath_begin(); rpath != rpathEnd; ++rpath) {
      memcpy((strtab + strtabsize), (*rpath).data(), (*rpath).size());
      strtabsize += (*rpath).size();
      strtab[strtabsize++] = (rpath + 1 == rpathEnd ? '\0' : ':');
    }
  }

  // initialize value of ELF .dynamic section
  if (LinkerConfig::DynObj == config().codeGenType()) {
    // set pointer to SONAME entry in dynamic string table.
    dynamic().applySoname(strtabsize);
  }
  dynamic().applyEntries(*file_format);
  dynamic().emit(dyn_sect, *dyn_region);

  // emit soname
  if (LinkerConfig::DynObj == config().codeGenType()) {
    strcpy((strtab + strtabsize), pModule.name().c_str());
    strtabsize += pModule.name().size() + 1;
  }
}

/// emitELFHashTab - emit .hash
void GNULDBackend::emitELFHashTab(const Module::SymbolTable& pSymtab,
                                  MemoryArea& pOutput)
{
  ELFFileFormat* file_format = getOutputFormat();
  if (!file_format->hasHashTab())
    return;
  LDSection& hash_sect = file_format->getHashTab();
  MemoryRegion* hash_region = pOutput.request(hash_sect.offset(),
                                              hash_sect.size());
  // both 32 and 64 bits hash table use 32-bit entry
  // set up hash_region
  uint32_t* word_array = (uint32_t*)hash_region->start();
  uint32_t& nbucket = word_array[0];
  uint32_t& nchain  = word_array[1];

  size_t dynsymSize = 1 + pSymtab.numOfLocalDyns() + pSymtab.numOfDynamics();
  nbucket = getHashBucketCount(dynsymSize, false);
  nchain  = dynsymSize;

  uint32_t* bucket = (word_array + 2);
  uint32_t* chain  = (bucket + nbucket);

  // initialize bucket
  memset((void*)bucket, 0, nbucket);

  hash::StringHash<hash::ELF> hash_func;

  size_t idx = 1;
  Module::const_sym_iterator symbol, symEnd = pSymtab.dynamicEnd();
  for (symbol = pSymtab.localDynBegin(); symbol != symEnd; ++symbol) {
    llvm::StringRef name((*symbol)->name());
    size_t bucket_pos = hash_func(name) % nbucket;
    chain[idx] = bucket[bucket_pos];
    bucket[bucket_pos] = idx;
    ++idx;
  }
}

/// emitGNUHashTab - emit .gnu.hash
void GNULDBackend::emitGNUHashTab(Module::SymbolTable& pSymtab,
                                  MemoryArea& pOutput)
{
  ELFFileFormat* file_format = getOutputFormat();
  if (!file_format->hasGNUHashTab())
    return;

  MemoryRegion* gnuhash_region =
    pOutput.request(file_format->getGNUHashTab().offset(),
                    file_format->getGNUHashTab().size());

  uint32_t* word_array = (uint32_t*)gnuhash_region->start();
  // fixed-length fields
  uint32_t& nbucket   = word_array[0];
  uint32_t& symidx    = word_array[1];
  uint32_t& maskwords = word_array[2];
  uint32_t& shift2    = word_array[3];
  // variable-length fields
  uint8_t*  bitmask = (uint8_t*)(word_array + 4);
  uint32_t* bucket  = NULL;
  uint32_t* chain   = NULL;

  // count the number of dynsym to hash
  size_t unhashed_sym_cnt = pSymtab.numOfLocalDyns();
  size_t hashed_sym_cnt   = pSymtab.numOfDynamics();
  Module::const_sym_iterator symbol, symEnd = pSymtab.dynamicEnd();
  for (symbol = pSymtab.dynamicBegin(); symbol != symEnd; ++symbol) {
    if (DynsymCompare().needGNUHash(**symbol))
      break;
      ++unhashed_sym_cnt;
      --hashed_sym_cnt;
  }

  // special case for the empty hash table
  if (hashed_sym_cnt == 0) {
    nbucket   = 1; // one empty bucket
    symidx    = 1 + unhashed_sym_cnt; // symidx above unhashed symbols
    maskwords = 1; // bitmask length
    shift2    = 0; // bloom filter

    if (config().targets().is32Bits()) {
      uint32_t* maskval = (uint32_t*)bitmask;
      *maskval = 0; // no valid hashes
    } else {
      // must be 64
      uint64_t* maskval = (uint64_t*)bitmask;
      *maskval = 0; // no valid hashes
    }
    bucket  = (uint32_t*)(bitmask + config().targets().bitclass() / 8);
    *bucket = 0; // no hash in the only bucket
    return;
  }

  uint32_t maskbitslog2 = getGNUHashMaskbitslog2(hashed_sym_cnt);
  uint32_t maskbits = 1u << maskbitslog2;
  uint32_t shift1 = config().targets().is32Bits() ? 5 : 6;
  uint32_t mask = (1u << shift1) - 1;

  nbucket   = getHashBucketCount(hashed_sym_cnt, true);
  symidx    = 1 + unhashed_sym_cnt;
  maskwords = 1 << (maskbitslog2 - shift1);
  shift2    = maskbitslog2;

  // setup bucket and chain
  bucket = (uint32_t*)(bitmask + maskbits / 8);
  chain  = (bucket + nbucket);

  // build the gnu style hash table
  typedef std::multimap<uint32_t,
                        std::pair<LDSymbol*, uint32_t> > SymMapType;
  SymMapType symmap;
  symEnd = pSymtab.dynamicEnd();
  for (symbol = pSymtab.localDynBegin() + symidx - 1; symbol != symEnd;
    ++symbol) {
    hash::StringHash<hash::DJB> hasher;
    uint32_t djbhash = hasher((*symbol)->name());
    uint32_t hash = djbhash % nbucket;
    symmap.insert(std::make_pair(hash, std::make_pair(*symbol, djbhash)));
  }

  // compute bucket, chain, and bitmask
  std::vector<uint64_t> bitmasks(maskwords);
  size_t hashedidx = symidx;
  for (size_t idx = 0; idx < nbucket; ++idx) {
    size_t count = 0;
    std::pair<SymMapType::iterator, SymMapType::iterator> ret;
    ret = symmap.equal_range(idx);
    for (SymMapType::iterator it = ret.first; it != ret.second; ) {
      // rearrange the hashed symbol ordering
      *(pSymtab.localDynBegin() + hashedidx - 1) = it->second.first;
      uint32_t djbhash = it->second.second;
      uint32_t val = ((djbhash >> shift1) & ((maskbits >> shift1) - 1));
      bitmasks[val] |= 1u << (djbhash & mask);
      bitmasks[val] |= 1u << ((djbhash >> shift2) & mask);
      val = djbhash & ~1u;
      // advance the iterator and check if we're dealing w/ the last elment
      if (++it == ret.second) {
        // last element terminates the chain
        val |= 1;
      }
      chain[hashedidx - symidx] = val;

      ++hashedidx;
      ++count;
    }

    if (count == 0)
      bucket[idx] = 0;
    else
      bucket[idx] = hashedidx - count;
  }

  // write the bitmasks
  if (config().targets().is32Bits()) {
    uint32_t* maskval = (uint32_t*)bitmask;
    for (size_t i = 0; i < maskwords; ++i)
      std::memcpy(maskval + i, &bitmasks[i], 4);
  } else {
    // must be 64
    uint64_t* maskval = (uint64_t*)bitmask;
    for (size_t i = 0; i < maskwords; ++i)
      std::memcpy(maskval + i, &bitmasks[i], 8);
  }
}

/// sizeInterp - compute the size of the .interp section
void GNULDBackend::sizeInterp()
{
  const char* dyld_name;
  if (config().options().hasDyld())
    dyld_name = config().options().dyld().c_str();
  else
    dyld_name = m_pInfo->dyld();

  LDSection& interp = getOutputFormat()->getInterp();
  interp.setSize(std::strlen(dyld_name) + 1);
}

/// emitInterp - emit the .interp
void GNULDBackend::emitInterp(MemoryArea& pOutput)
{
  if (getOutputFormat()->hasInterp()) {
    const LDSection& interp = getOutputFormat()->getInterp();
    MemoryRegion *region = pOutput.request(interp.offset(), interp.size());
    const char* dyld_name;
    if (config().options().hasDyld())
      dyld_name = config().options().dyld().c_str();
    else
      dyld_name = m_pInfo->dyld();

    std::memcpy(region->start(), dyld_name, interp.size());
  }
}

bool GNULDBackend::hasEntryInStrTab(const LDSymbol& pSym) const
{
  return ResolveInfo::Section != pSym.type();
}

void GNULDBackend::orderSymbolTable(Module& pModule)
{
  Module::SymbolTable& symbols = pModule.getSymbolTable();

  if (GeneralOptions::GNU  == config().options().getHashStyle() ||
      GeneralOptions::Both == config().options().getHashStyle())
    // Currently we may add output symbols after sizeNamePools(), and a
    // non-stable sort is used in SymbolCategory::arrange(), so we just
    // sort .dynsym right before emitting .gnu.hash
    std::stable_sort(symbols.dynamicBegin(), symbols.dynamicEnd(),
                     DynsymCompare());
}

/// getSectionOrder
unsigned int GNULDBackend::getSectionOrder(const LDSection& pSectHdr) const
{
  const ELFFileFormat* file_format = getOutputFormat();

  // NULL section should be the "1st" section
  if (LDFileFormat::Null == pSectHdr.kind())
    return 0;

  if (&pSectHdr == &file_format->getStrTab())
    return SHO_STRTAB;

  // if the section is not ALLOC, lay it out until the last possible moment
  if (0 == (pSectHdr.flag() & llvm::ELF::SHF_ALLOC))
    return SHO_UNDEFINED;

  bool is_write = (pSectHdr.flag() & llvm::ELF::SHF_WRITE) != 0;
  bool is_exec = (pSectHdr.flag() & llvm::ELF::SHF_EXECINSTR) != 0;
  // TODO: need to take care other possible output sections
  switch (pSectHdr.kind()) {
    case LDFileFormat::Regular:
      if (is_exec) {
        if (&pSectHdr == &file_format->getInit())
          return SHO_INIT;
        if (&pSectHdr == &file_format->getFini())
          return SHO_FINI;
        return SHO_TEXT;
      } else if (!is_write) {
        return SHO_RO;
      } else {
        if (config().options().hasRelro()) {
          if (&pSectHdr == &file_format->getPreInitArray() ||
              &pSectHdr == &file_format->getInitArray() ||
              &pSectHdr == &file_format->getFiniArray() ||
              &pSectHdr == &file_format->getCtors() ||
              &pSectHdr == &file_format->getDtors() ||
              &pSectHdr == &file_format->getJCR() ||
              &pSectHdr == &file_format->getDataRelRo())
            return SHO_RELRO;
          if (&pSectHdr == &file_format->getDataRelRoLocal())
            return SHO_RELRO_LOCAL;
        }
        if ((pSectHdr.flag() & llvm::ELF::SHF_TLS) != 0x0) {
          return SHO_TLS_DATA;
        }
        return SHO_DATA;
      }

    case LDFileFormat::BSS:
      if ((pSectHdr.flag() & llvm::ELF::SHF_TLS) != 0x0)
        return SHO_TLS_BSS;
      return SHO_BSS;

    case LDFileFormat::NamePool: {
      if (&pSectHdr == &file_format->getDynamic())
        return SHO_RELRO;
      return SHO_NAMEPOOL;
    }
    case LDFileFormat::Relocation:
      if (&pSectHdr == &file_format->getRelPlt() ||
          &pSectHdr == &file_format->getRelaPlt())
        return SHO_REL_PLT;
      return SHO_RELOCATION;

    // get the order from target for target specific sections
    case LDFileFormat::Target:
      return getTargetSectionOrder(pSectHdr);

    // handle .interp and .note.* sections
    case LDFileFormat::Note:
      if (file_format->hasInterp() && (&pSectHdr == &file_format->getInterp()))
        return SHO_INTERP;
      else if (is_write)
        return SHO_RW_NOTE;
      else
        return SHO_RO_NOTE;

    case LDFileFormat::EhFrame:
      // set writable .eh_frame as relro
      if (is_write)
        return SHO_RELRO;
    case LDFileFormat::EhFrameHdr:
    case LDFileFormat::GCCExceptTable:
      return SHO_EXCEPTION;

    case LDFileFormat::MetaData:
    case LDFileFormat::Debug:
    default:
      return SHO_UNDEFINED;
  }
}

/// getSymbolSize
uint64_t GNULDBackend::getSymbolSize(const LDSymbol& pSymbol) const
{
  // @ref Google gold linker: symtab.cc: 2780
  // undefined and dynamic symbols should have zero size.
  if (pSymbol.isDyn() || pSymbol.desc() == ResolveInfo::Undefined)
    return 0x0;
  return pSymbol.resolveInfo()->size();
}

/// getSymbolInfo
uint64_t GNULDBackend::getSymbolInfo(const LDSymbol& pSymbol) const
{
  // set binding
  uint8_t bind = 0x0;
  if (pSymbol.resolveInfo()->isLocal())
    bind = llvm::ELF::STB_LOCAL;
  else if (pSymbol.resolveInfo()->isGlobal())
    bind = llvm::ELF::STB_GLOBAL;
  else if (pSymbol.resolveInfo()->isWeak())
    bind = llvm::ELF::STB_WEAK;
  else if (pSymbol.resolveInfo()->isAbsolute()) {
    // (Luba) Is a absolute but not global (weak or local) symbol meaningful?
    bind = llvm::ELF::STB_GLOBAL;
  }

  if (config().codeGenType() != LinkerConfig::Object &&
      (pSymbol.visibility() == llvm::ELF::STV_INTERNAL ||
      pSymbol.visibility() == llvm::ELF::STV_HIDDEN))
    bind = llvm::ELF::STB_LOCAL;

  uint32_t type = pSymbol.resolveInfo()->type();
  // if the IndirectFunc symbol (i.e., STT_GNU_IFUNC) is from dynobj, change
  // its type to Function
  if (type == ResolveInfo::IndirectFunc && pSymbol.isDyn())
    type = ResolveInfo::Function;
  return (type | (bind << 4));
}

/// getSymbolValue - this function is called after layout()
uint64_t GNULDBackend::getSymbolValue(const LDSymbol& pSymbol) const
{
  if (pSymbol.isDyn())
    return 0x0;

  return pSymbol.value();
}

/// getSymbolShndx - this function is called after layout()
uint64_t
GNULDBackend::getSymbolShndx(const LDSymbol& pSymbol) const
{
  if (pSymbol.resolveInfo()->isAbsolute())
    return llvm::ELF::SHN_ABS;
  if (pSymbol.resolveInfo()->isCommon())
    return llvm::ELF::SHN_COMMON;
  if (pSymbol.resolveInfo()->isUndef() || pSymbol.isDyn())
    return llvm::ELF::SHN_UNDEF;

  if (pSymbol.resolveInfo()->isLocal() &&
      LinkerConfig::Object != config().codeGenType()) {
    switch (pSymbol.type()) {
      case ResolveInfo::NoType:
      case ResolveInfo::File:
        return llvm::ELF::SHN_ABS;
    }
  }

  if (pSymbol.resolveInfo()->isDefine() && !pSymbol.hasFragRef())
    return llvm::ELF::SHN_ABS;

  assert(pSymbol.hasFragRef() && "symbols must have fragment reference to get its index");
  return pSymbol.fragRef()->frag()->getParent()->getSection().index();
}

/// getSymbolIdx - called by emitRelocation to get the ouput symbol table index
size_t GNULDBackend::getSymbolIdx(const LDSymbol* pSymbol) const
{
   HashTableType::iterator entry = m_pSymIndexMap->find(const_cast<LDSymbol *>(pSymbol));
   assert(entry != m_pSymIndexMap->end() && "symbol not found in the symbol table");
   return entry.getEntry()->value();
}

/// isTemporary - Whether pSymbol is a local label.
bool GNULDBackend::isTemporary(const LDSymbol& pSymbol) const
{
  if (ResolveInfo::Local != pSymbol.binding())
    return false;

  if (pSymbol.nameSize() < 2)
    return false;

  const char* name = pSymbol.name();
  if ('.' == name[0] && 'L' == name[1])
    return true;

  // UnixWare 2.1 cc generate DWARF debugging symbols with `..' prefix.
  // @ref Google gold linker, target.cc:39 @@ Target::do_is_local_label_name()
  if (name[0] == '.' && name[1] == '.')
    return true;

  // Work arround for gcc's bug
  // gcc sometimes generate symbols with '_.L_' prefix.
  // @ref Google gold linker, target.cc:39 @@ Target::do_is_local_label_name()
  if (pSymbol.nameSize() < 4)
    return false;

  if (name[0] == '_' && name[1] == '.' && name[2] == 'L' && name[3] == '_')
    return true;

  return false;
}

/// allocateCommonSymbols - allocate common symbols in the corresponding
/// sections. This is executed at pre-layout stage.
/// @refer Google gold linker: common.cc: 214
bool
GNULDBackend::allocateCommonSymbols(Module& pModule)
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

/// updateSectionFlags - update pTo's flags when merging pFrom
/// update the output section flags based on input section flags.
/// @ref The Google gold linker:
///      output.cc: 2809: Output_section::update_flags_for_input_section
bool GNULDBackend::updateSectionFlags(LDSection& pTo, const LDSection& pFrom)
{
  // union the flags from input
  uint32_t flags = pTo.flag();
  flags |= (pFrom.flag() &
              (llvm::ELF::SHF_WRITE |
               llvm::ELF::SHF_ALLOC |
               llvm::ELF::SHF_EXECINSTR));

  // if there is an input section is not SHF_MERGE, clean this flag
  if (0 == (pFrom.flag() & llvm::ELF::SHF_MERGE))
    flags &= ~llvm::ELF::SHF_MERGE;

  // if there is an input section is not SHF_STRINGS, clean this flag
  if (0 == (pFrom.flag() & llvm::ELF::SHF_STRINGS))
    flags &= ~llvm::ELF::SHF_STRINGS;

  pTo.setFlag(flags);
  return true;
}

/// createProgramHdrs - base on output sections to create the program headers
void GNULDBackend::createProgramHdrs(Module& pModule)
{
  ELFFileFormat *file_format = getOutputFormat();

  // make PT_PHDR
  m_ELFSegmentTable.produce(llvm::ELF::PT_PHDR);

  // make PT_INTERP
  if (file_format->hasInterp()) {
    ELFSegment* interp_seg = m_ELFSegmentTable.produce(llvm::ELF::PT_INTERP);
    interp_seg->addSection(&file_format->getInterp());
  }

  uint32_t cur_flag, prev_flag = getSegmentFlag(0);
  ELFSegment* load_seg = NULL;
  // make possible PT_LOAD segments
  LinkerScript::AddressMap::iterator addrEnd
                                      = pModule.getScript().addressMap().end();
  Module::iterator sect, sect_end = pModule.end();
  for (sect = pModule.begin(); sect != sect_end; ++sect) {

    if (0 == ((*sect)->flag() & llvm::ELF::SHF_ALLOC) &&
        LDFileFormat::Null != (*sect)->kind())
      continue;

    cur_flag = getSegmentFlag((*sect)->flag());
    bool createPT_LOAD = false;
    if (LDFileFormat::Null == (*sect)->kind()) {
      // 1. create text segment
      createPT_LOAD = true;
    }
    else if (!config().options().omagic() &&
             (prev_flag & llvm::ELF::PF_W) ^ (cur_flag & llvm::ELF::PF_W)) {
      // 2. create data segment if w/o omagic set
      createPT_LOAD = true;
    }
    else if ((*sect)->kind() == LDFileFormat::BSS &&
             load_seg->isDataSegment() &&
             addrEnd != pModule.getScript().addressMap().find(".bss")) {
      // 3. create bss segment if w/ -Tbss and there is a data segment
      createPT_LOAD = true;
    }
    else {
      if ((*sect != &(file_format->getText())) &&
          (*sect != &(file_format->getData())) &&
          (*sect != &(file_format->getBSS())) &&
          (addrEnd != pModule.getScript().addressMap().find((*sect)->name())))
        // 4. create PT_LOAD for sections in address map except for text, data,
        // and bss
        createPT_LOAD = true;
    }

    if (createPT_LOAD) {
      // create new PT_LOAD segment
      load_seg = m_ELFSegmentTable.produce(llvm::ELF::PT_LOAD, cur_flag);
      if (!config().options().nmagic() && !config().options().omagic())
        load_seg->setAlign(abiPageSize());
    }

    assert(NULL != load_seg);
    load_seg->addSection((*sect));
    if (cur_flag != prev_flag)
      load_seg->updateFlag(cur_flag);

    prev_flag = cur_flag;
  }

  // make PT_DYNAMIC
  if (file_format->hasDynamic()) {
    ELFSegment* dyn_seg = m_ELFSegmentTable.produce(llvm::ELF::PT_DYNAMIC,
                                                    llvm::ELF::PF_R |
                                                    llvm::ELF::PF_W);
    dyn_seg->addSection(&file_format->getDynamic());
  }

  if (config().options().hasRelro()) {
    // make PT_GNU_RELRO
    ELFSegment* relro_seg = m_ELFSegmentTable.produce(llvm::ELF::PT_GNU_RELRO);
    for (ELFSegmentFactory::iterator seg = elfSegmentTable().begin(),
         segEnd = elfSegmentTable().end(); seg != segEnd; ++seg) {
      if (llvm::ELF::PT_LOAD != (*seg).type())
        continue;

      for (ELFSegment::sect_iterator sect = (*seg).begin(),
             sectEnd = (*seg).end(); sect != sectEnd; ++sect) {
        unsigned int order = getSectionOrder(**sect);
        if (SHO_RELRO_LOCAL == order ||
            SHO_RELRO == order ||
            SHO_RELRO_LAST == order) {
          relro_seg->addSection(*sect);
        }
      }
    }
  }

  // make PT_GNU_EH_FRAME
  if (file_format->hasEhFrameHdr()) {
    ELFSegment* eh_seg = m_ELFSegmentTable.produce(llvm::ELF::PT_GNU_EH_FRAME);
    eh_seg->addSection(&file_format->getEhFrameHdr());
  }

  // make PT_TLS
  if (file_format->hasTData() || file_format->hasTBSS()) {
    ELFSegment* tls_seg = m_ELFSegmentTable.produce(llvm::ELF::PT_TLS);
    if (file_format->hasTData())
      tls_seg->addSection(&file_format->getTData());
    if (file_format->hasTBSS())
      tls_seg->addSection(&file_format->getTBSS());
  }

  // make PT_GNU_STACK
  if (file_format->hasStackNote()) {
    m_ELFSegmentTable.produce(llvm::ELF::PT_GNU_STACK,
                              llvm::ELF::PF_R |
                              llvm::ELF::PF_W |
                              getSegmentFlag(file_format->getStackNote().flag()));
  }

  // make PT_NOTE
  ELFSegment *note_seg = NULL;
  prev_flag = getSegmentFlag(0);
  for (sect = pModule.begin(); sect != sect_end; ++sect) {
    if ((*sect)->kind() != LDFileFormat::Note ||
        ((*sect)->flag() & llvm::ELF::SHF_ALLOC) == 0)
      continue;

    cur_flag = getSegmentFlag((*sect)->flag());
    // we have different section orders for read-only and writable notes, so
    // create 2 segments if needed.
    if (note_seg == NULL ||
        (cur_flag & llvm::ELF::PF_W) != (prev_flag & llvm::ELF::PF_W))
      note_seg = m_ELFSegmentTable.produce(llvm::ELF::PT_NOTE, cur_flag);

    note_seg->addSection(*sect);
    prev_flag = cur_flag;
  }

  // create target dependent segments
  doCreateProgramHdrs(pModule);
}

/// setupProgramHdrs - set up the attributes of segments
void GNULDBackend::setupProgramHdrs(const LinkerScript& pScript)
{
  // update segment info
  uint64_t seg_start_addr = getSegmentStartAddr(pScript);
  ELFSegmentFactory::iterator seg, seg_end = m_ELFSegmentTable.end();
  for (seg = m_ELFSegmentTable.begin(); seg != seg_end; ++seg) {
    ELFSegment& segment = *seg;

    // update PT_PHDR
    if (llvm::ELF::PT_PHDR == segment.type()) {
      uint64_t offset = 0, phdr_size = 0;
      if (config().targets().is32Bits()) {
        offset = sizeof(llvm::ELF::Elf32_Ehdr);
        phdr_size = sizeof(llvm::ELF::Elf32_Phdr);
      }
      else {
        offset = sizeof(llvm::ELF::Elf64_Ehdr);
        phdr_size = sizeof(llvm::ELF::Elf64_Phdr);
      }
      segment.setOffset(offset);
      segment.setVaddr(seg_start_addr + offset);
      segment.setPaddr(segment.vaddr());
      segment.setFilesz(numOfSegments() * phdr_size);
      segment.setMemsz(numOfSegments() * phdr_size);
      segment.setAlign(config().targets().bitclass() / 8);
      continue;
    }

    // bypass if there is no section in this segment (e.g., PT_GNU_STACK)
    if (segment.numOfSections() == 0)
      continue;

    segment.setOffset(segment.front()->offset());
    if (llvm::ELF::PT_LOAD == segment.type() &&
        LDFileFormat::Null == segment.front()->kind())
      segment.setVaddr(seg_start_addr);
    else
      segment.setVaddr(segment.front()->addr());
    segment.setPaddr(segment.vaddr());

    const LDSection* last_sect = segment.back();
    assert(NULL != last_sect);
    uint64_t file_size = last_sect->offset() - segment.offset();
    if (LDFileFormat::BSS != last_sect->kind())
      file_size += last_sect->size();
    segment.setFilesz(file_size);

    segment.setMemsz(last_sect->addr() - segment.vaddr() + last_sect->size());
  }
}

/// setupGNUStackInfo - setup the section flag of .note.GNU-stack in output
/// @ref gold linker: layout.cc:2608
void GNULDBackend::setupGNUStackInfo(Module& pModule)
{
  uint32_t flag = 0x0;
  if (config().options().hasStackSet()) {
    // 1. check the command line option (-z execstack or -z noexecstack)
    if (config().options().hasExecStack())
      flag = llvm::ELF::SHF_EXECINSTR;
  }
  else {
    // 2. check the stack info from the input objects
    // FIXME: since we alway emit .note.GNU-stack in output now, we may be able
    // to check this from the output .note.GNU-stack directly after section
    // merging is done
    size_t object_count = 0, stack_note_count = 0;
    Module::const_obj_iterator obj, objEnd = pModule.obj_end();
    for (obj = pModule.obj_begin(); obj != objEnd; ++obj) {
      ++object_count;
      const LDSection* sect = (*obj)->context()->getSection(".note.GNU-stack");
      if (NULL != sect) {
        ++stack_note_count;
        // 2.1 found a stack note that is set as executable
        if (0 != (llvm::ELF::SHF_EXECINSTR & sect->flag())) {
          flag = llvm::ELF::SHF_EXECINSTR;
          break;
        }
      }
    }

    // 2.2 there are no stack note sections in all input objects
    if (0 == stack_note_count)
      return;

    // 2.3 a special case. Use the target default to decide if the stack should
    //     be executable
    if (llvm::ELF::SHF_EXECINSTR != flag && object_count != stack_note_count)
      if (m_pInfo->isDefaultExecStack())
        flag = llvm::ELF::SHF_EXECINSTR;
  }

  if (getOutputFormat()->hasStackNote()) {
    getOutputFormat()->getStackNote().setFlag(flag);
  }
}

/// setupRelro - setup the offset constraint of PT_RELRO
void GNULDBackend::setupRelro(Module& pModule)
{
  assert(config().options().hasRelro());
  // if -z relro is given, we need to adjust sections' offset again, and let
  // PT_GNU_RELRO end on a common page boundary

  Module::iterator sect = pModule.begin();
  for (Module::iterator sect_end = pModule.end(); sect != sect_end; ++sect) {
    // find the first non-relro section
    if (getSectionOrder(**sect) > SHO_RELRO_LAST)
      break;
  }

  // align the first non-relro section to page boundary
  uint64_t offset = (*sect)->offset();
  alignAddress(offset, commonPageSize());
  (*sect)->setOffset(offset);

  // It seems that compiler think .got and .got.plt are continuous (w/o any
  // padding between). If .got is the last section in PT_RELRO and it's not
  // continuous to its next section (i.e. .got.plt), we need to add padding
  // in front of .got instead.
  // FIXME: Maybe we can handle this in a more general way.
  LDSection& got = getOutputFormat()->getGOT();
  if ((getSectionOrder(got) == SHO_RELRO_LAST) &&
      (got.offset() + got.size() != offset)) {
    got.setOffset(offset - got.size());
  }

  // set up remaining section's offset
  setOutputSectionOffset(pModule, ++sect, pModule.end());
}

/// setOutputSectionOffset - helper function to set a group of output sections'
/// offset, and set pSectBegin to pStartOffset if pStartOffset is not -1U.
void GNULDBackend::setOutputSectionOffset(Module& pModule,
                                          Module::iterator pSectBegin,
                                          Module::iterator pSectEnd,
                                          uint64_t pStartOffset)
{
  if (pSectBegin == pModule.end())
    return;

  assert(pSectEnd == pModule.end() ||
         (pSectEnd != pModule.end() &&
          (*pSectBegin)->index() <= (*pSectEnd)->index()));

  if (pStartOffset != -1U) {
    (*pSectBegin)->setOffset(pStartOffset);
    ++pSectBegin;
  }

  // set up the "cur" and "prev" iterator
  Module::iterator cur = pSectBegin;
  Module::iterator prev = pSectBegin;
  if (cur != pModule.begin())
    --prev;
  else
    ++cur;

  for (; cur != pSectEnd; ++cur, ++prev) {
    uint64_t offset = 0x0;
    switch ((*prev)->kind()) {
      case LDFileFormat::Null:
        offset = sectionStartOffset();
        break;
      case LDFileFormat::BSS:
        offset = (*prev)->offset();
        break;
      default:
        offset = (*prev)->offset() + (*prev)->size();
        break;
    }

    alignAddress(offset, (*cur)->align());
    (*cur)->setOffset(offset);
  }
}

/// setOutputSectionOffset - helper function to set output sections' address
void GNULDBackend::setOutputSectionAddress(Module& pModule,
                                           Module::iterator pSectBegin,
                                           Module::iterator pSectEnd)
{
  if (pSectBegin == pModule.end())
    return;

  assert(pSectEnd == pModule.end() ||
         (pSectEnd != pModule.end() &&
          (*pSectBegin)->index() <= (*pSectEnd)->index()));

  const LinkerScript& script = pModule.getScript();
  uint64_t seg_start_addr = getSegmentStartAddr(script);
  for (ELFSegmentFactory::iterator seg = elfSegmentTable().begin(),
         segEnd = elfSegmentTable().end(), prev = elfSegmentTable().end();
       seg != segEnd; prev = seg, ++seg) {
    if (llvm::ELF::PT_LOAD != (*seg).type())
      continue;

    uint64_t start_addr = 0x0;
    LinkerScript::AddressMap::const_iterator mapping;
    if ((*seg).front()->kind() == LDFileFormat::Null)
      mapping = script.addressMap().find(".text");
    else if ((*seg).isDataSegment())
      mapping = script.addressMap().find(".data");
    else if ((*seg).isBssSegment())
      mapping = script.addressMap().find(".bss");
    else
      mapping = script.addressMap().find((*seg).front()->name());

    if (mapping != script.addressMap().end()) {
      // use address mapping in script options
      start_addr = mapping.getEntry()->value();
    }
    else {
      if ((*seg).front()->kind() == LDFileFormat::Null) {
        // 1st PT_LOAD
        start_addr = seg_start_addr;
      }
      else if ((*prev).front()->kind() == LDFileFormat::Null) {
        // prev segment is 1st PT_LOAD
        start_addr = seg_start_addr + (*seg).front()->offset();
      }
      else {
        // Others
        start_addr = (*prev).front()->addr() + (*seg).front()->offset();
      }
      // Try to align p_vaddr at page boundary if not in script options.
      // To do so will add more padding in file, but can save one page
      // at runtime.
      alignAddress(start_addr, (*seg).align());
    }

    // in p75, http://www.sco.com/developers/devspecs/gabi41.pdf
    // p_align: As "Program Loading" describes in this chapter of the
    // processor supplement, loadable process segments must have congruent
    // values for p_vaddr and p_offset, modulo the page size.
    if ((start_addr & ((*seg).align() - 1)) !=
        ((*seg).front()->offset() & ((*seg).align() - 1))) {
      uint64_t padding = (*seg).align() +
                         (start_addr & ((*seg).align() - 1)) -
                         ((*seg).front()->offset() & ((*seg).align() - 1));
      setOutputSectionOffset(pModule,
                             pModule.begin() + (*seg).front()->index(),
                             pModule.end(),
                             (*seg).front()->offset() + padding);
      if (config().options().hasRelro())
        setupRelro(pModule);
    }

    for (ELFSegment::sect_iterator sect = (*seg).begin(),
           sectEnd = (*seg).end(); sect != sectEnd; ++sect) {
      if ((*sect)->index() < (*pSectBegin)->index())
        continue;

      if (LDFileFormat::Null == (*sect)->kind())
        continue;

      if (sect == pSectEnd)
        return;

      if (sect != (*seg).begin())
        (*sect)->setAddr(start_addr + (*sect)->offset() -
                         (*seg).front()->offset());
      else
        (*sect)->setAddr(start_addr);
    }
  }
}

/// layout - layout method
void GNULDBackend::layout(Module& pModule)
{
  std::vector<SHOEntry> output_list;
  // 1. determine what sections will go into final output, and push the needed
  // sections into output_list for later processing
  for (Module::iterator it = pModule.begin(), ie = pModule.end(); it != ie;
       ++it) {
    switch ((*it)->kind()) {
    // take NULL and StackNote directly
    case LDFileFormat::Null:
    case LDFileFormat::StackNote:
      output_list.push_back(std::make_pair(*it, getSectionOrder(**it)));
      break;
    // ignore if section size is 0
    case LDFileFormat::EhFrame:
      if (((*it)->size() != 0) ||
          ((*it)->hasEhFrame() &&
           config().codeGenType() == LinkerConfig::Object))
        output_list.push_back(std::make_pair(*it, getSectionOrder(**it)));
      break;
    case LDFileFormat::Relocation:
      if (((*it)->size() != 0) ||
          ((*it)->hasRelocData() &&
           config().codeGenType() == LinkerConfig::Object))
        output_list.push_back(std::make_pair(*it, getSectionOrder(**it)));
      break;
    case LDFileFormat::Regular:
    case LDFileFormat::Target:
    case LDFileFormat::MetaData:
    case LDFileFormat::BSS:
    case LDFileFormat::Debug:
    case LDFileFormat::GCCExceptTable:
    case LDFileFormat::Note:
    case LDFileFormat::NamePool:
    case LDFileFormat::EhFrameHdr:
      if (((*it)->size() != 0) ||
          ((*it)->hasSectionData() &&
           config().codeGenType() == LinkerConfig::Object))
        output_list.push_back(std::make_pair(*it, getSectionOrder(**it)));
      break;
    case LDFileFormat::Group:
      if (LinkerConfig::Object == config().codeGenType()) {
        //TODO: support incremental linking
        ;
      }
      break;
    case LDFileFormat::Version:
      if (0 != (*it)->size()) {
        output_list.push_back(std::make_pair(*it, getSectionOrder(**it)));
        warning(diag::warn_unsupported_symbolic_versioning) << (*it)->name();
      }
      break;
    default:
      if (0 != (*it)->size()) {
        error(diag::err_unsupported_section) << (*it)->name() << (*it)->kind();
      }
      break;
    }
  } // end of for

  // 2. sort output section orders
  std::stable_sort(output_list.begin(), output_list.end(), SHOCompare());

  // 3. update output sections in Module
  pModule.getSectionTable().clear();
  for(size_t index = 0; index < output_list.size(); ++index) {
    (output_list[index].first)->setIndex(index);
    pModule.getSectionTable().push_back(output_list[index].first);
  }

  // 4. create program headers
  if (LinkerConfig::Object != config().codeGenType()) {
    createProgramHdrs(pModule);
  }

  // 5. set output section offset
  setOutputSectionOffset(pModule, pModule.begin(), pModule.end(), 0x0);
}

/// preLayout - Backend can do any needed modification before layout
void GNULDBackend::preLayout(Module& pModule, IRBuilder& pBuilder)
{
  // prelayout target first
  doPreLayout(pBuilder);

  if (LinkerConfig::Object != config().codeGenType() &&
      config().options().hasEhFrameHdr() && getOutputFormat()->hasEhFrame()) {
    // init EhFrameHdr and size the output section
    ELFFileFormat* format = getOutputFormat();
    m_pEhFrameHdr = new EhFrameHdr(format->getEhFrameHdr(),
                                   format->getEhFrame());
    m_pEhFrameHdr->sizeOutput();
  }

  // change .tbss and .tdata section symbol from Local to LocalDyn category
  if (NULL != f_pTDATA)
    pModule.getSymbolTable().changeToDynamic(*f_pTDATA);

  if (NULL != f_pTBSS)
    pModule.getSymbolTable().changeToDynamic(*f_pTBSS);

  // To merge input's relocation sections into output's relocation sections.
  //
  // If we are generating relocatables (-r), move input relocation sections
  // to corresponding output relocation sections.
  if (LinkerConfig::Object == config().codeGenType()) {
    Module::obj_iterator input, inEnd = pModule.obj_end();
    for (input = pModule.obj_begin(); input != inEnd; ++input) {
      LDContext::sect_iterator rs, rsEnd = (*input)->context()->relocSectEnd();
      for (rs = (*input)->context()->relocSectBegin(); rs != rsEnd; ++rs) {

        // get the output relocation LDSection with identical name.
        LDSection* output_sect = pModule.getSection((*rs)->name());
        if (NULL == output_sect) {
          output_sect = LDSection::Create((*rs)->name(),
                                          (*rs)->kind(),
                                          (*rs)->type(),
                                          (*rs)->flag());

          output_sect->setAlign((*rs)->align());
          pModule.getSectionTable().push_back(output_sect);
        }

        // set output relocation section link
        const LDSection* input_link = (*rs)->getLink();
        assert(NULL != input_link && "Illegal input relocation section.");

        // get the linked output section
        LDSection* output_link = pModule.getSection(input_link->name());
        assert(NULL != output_link);

        output_sect->setLink(output_link);

        // get output relcoationData, create one if not exist
        if (!output_sect->hasRelocData())
          IRBuilder::CreateRelocData(*output_sect);

        RelocData* out_reloc_data = output_sect->getRelocData();

        // move relocations from input's to output's RelcoationData
        RelocData::RelocationListType& out_list =
                                             out_reloc_data->getRelocationList();
        RelocData::RelocationListType& in_list =
                                      (*rs)->getRelocData()->getRelocationList();
        out_list.splice(out_list.end(), in_list);

        // size output
        if (llvm::ELF::SHT_REL == output_sect->type())
          output_sect->setSize(out_reloc_data->size() * getRelEntrySize());
        else if (llvm::ELF::SHT_RELA == output_sect->type())
          output_sect->setSize(out_reloc_data->size() * getRelaEntrySize());
        else {
          fatal(diag::unknown_reloc_section_type) << output_sect->type()
                                                  << output_sect->name();
        }
      } // end of for each relocation section
    } // end of for each input
  } // end of if

  // set up the section flag of .note.GNU-stack section
  setupGNUStackInfo(pModule);
}

/// postLayout - Backend can do any needed modification after layout
void GNULDBackend::postLayout(Module& pModule, IRBuilder& pBuilder)
{
  // 1. set up section address and segment attributes
  if (LinkerConfig::Object != config().codeGenType()) {
    if (config().options().hasRelro()) {
      // 1.1 set up the offset constraint of PT_RELRO
      setupRelro(pModule);
    }

    // 1.2 set up the output sections' address
    setOutputSectionAddress(pModule, pModule.begin(), pModule.end());

    // 1.3 do relaxation
    relax(pModule, pBuilder);

    // 1.4 set up the attributes of program headers
    setupProgramHdrs(pModule.getScript());
  }

  // 2. target specific post layout
  doPostLayout(pModule, pBuilder);
}

void GNULDBackend::postProcessing(MemoryArea& pOutput)
{
  if (LinkerConfig::Object != config().codeGenType() &&
      config().options().hasEhFrameHdr() && getOutputFormat()->hasEhFrame()) {
    // emit eh_frame_hdr
    m_pEhFrameHdr->emitOutput<32>(pOutput);
  }
}

/// getHashBucketCount - calculate hash bucket count.
/// @ref Google gold linker, dynobj.cc:791
unsigned GNULDBackend::getHashBucketCount(unsigned pNumOfSymbols,
                                          bool pIsGNUStyle)
{
  // @ref Google gold, dynobj.cc:loc 791
  static const unsigned int buckets[] =
  {
    1, 3, 17, 37, 67, 97, 131, 197, 263, 521, 1031, 2053, 4099, 8209,
    16411, 32771, 65537, 131101, 262147
  };
  const unsigned buckets_count = sizeof buckets / sizeof buckets[0];

  unsigned int result = 1;
  for (unsigned i = 0; i < buckets_count; ++i) {
    if (pNumOfSymbols < buckets[i])
      break;
    result = buckets[i];
  }

  if (pIsGNUStyle && result < 2)
    result = 2;

  return result;
}

/// getGNUHashMaskbitslog2 - calculate the number of mask bits in log2
/// @ref binutils gold, dynobj.cc:1165
unsigned GNULDBackend::getGNUHashMaskbitslog2(unsigned pNumOfSymbols) const
{
  uint32_t maskbitslog2 = 1;
  for (uint32_t x = pNumOfSymbols >> 1; x != 0; x >>=1)
    ++maskbitslog2;

  if (maskbitslog2 < 3)
    maskbitslog2 = 5;
  else if (((1U << (maskbitslog2 - 2)) & pNumOfSymbols) != 0)
    maskbitslog2 += 3;
  else
    maskbitslog2 += 2;

  if (config().targets().bitclass() == 64 && maskbitslog2 == 5)
    maskbitslog2 = 6;

  return maskbitslog2;
}

/// isDynamicSymbol
/// @ref Google gold linker: symtab.cc:311
bool GNULDBackend::isDynamicSymbol(const LDSymbol& pSymbol)
{
  // If a local symbol is in the LDContext's symbol table, it's a real local
  // symbol. We should not add it
  if (pSymbol.binding() == ResolveInfo::Local)
    return false;

  // If we are building shared object, and the visibility is external, we
  // need to add it.
  if (LinkerConfig::DynObj == config().codeGenType() ||
      LinkerConfig::Exec   == config().codeGenType() ||
      LinkerConfig::Binary == config().codeGenType()) {
    if (pSymbol.resolveInfo()->visibility() == ResolveInfo::Default ||
        pSymbol.resolveInfo()->visibility() == ResolveInfo::Protected)
      return true;
  }
  return false;
}

/// isDynamicSymbol
/// @ref Google gold linker: symtab.cc:311
bool GNULDBackend::isDynamicSymbol(const ResolveInfo& pResolveInfo)
{
  // If a local symbol is in the LDContext's symbol table, it's a real local
  // symbol. We should not add it
  if (pResolveInfo.binding() == ResolveInfo::Local)
    return false;

  // If we are building shared object, and the visibility is external, we
  // need to add it.
  if (LinkerConfig::DynObj == config().codeGenType() ||
      LinkerConfig::Exec   == config().codeGenType() ||
      LinkerConfig::Binary == config().codeGenType()) {
    if (pResolveInfo.visibility() == ResolveInfo::Default ||
        pResolveInfo.visibility() == ResolveInfo::Protected)
      return true;
  }
  return false;
}

/// commonPageSize - the common page size of the target machine.
/// @ref gold linker: target.h:135
uint64_t GNULDBackend::commonPageSize() const
{
  if (config().options().commPageSize() > 0)
    return std::min(config().options().commPageSize(), abiPageSize());
  else
    return std::min(m_pInfo->commonPageSize(), abiPageSize());
}

/// abiPageSize - the abi page size of the target machine.
/// @ref gold linker: target.h:125
uint64_t GNULDBackend::abiPageSize() const
{
  if (config().options().maxPageSize() > 0)
    return config().options().maxPageSize();
  else
    return m_pInfo->abiPageSize();
}

/// isSymbolPreemtible - whether the symbol can be preemted by other
/// link unit
/// @ref Google gold linker, symtab.h:551
bool GNULDBackend::isSymbolPreemptible(const ResolveInfo& pSym) const
{
  if (pSym.other() != ResolveInfo::Default)
    return false;

  // This is because the codeGenType of pie is DynObj. And gold linker check
  // the "shared" option instead.
  if (config().options().isPIE())
    return false;

  if (LinkerConfig::DynObj != config().codeGenType())
    return false;

  if (config().options().Bsymbolic())
    return false;

  // A local defined symbol should be non-preemptible.
  // This issue is found when linking libstdc++ on freebsd. A R_386_GOT32
  // relocation refers to a local defined symbol, and we should generate a
  // relative dynamic relocation when applying the relocation.
  if (pSym.isDefine() && pSym.binding() == ResolveInfo::Local)
    return false;

  return true;
}

/// symbolNeedsDynRel - return whether the symbol needs a dynamic relocation
/// @ref Google gold linker, symtab.h:645
bool GNULDBackend::symbolNeedsDynRel(const ResolveInfo& pSym,
                                     bool pSymHasPLT,
                                     bool isAbsReloc) const
{
  // an undefined reference in the executables should be statically
  // resolved to 0 and no need a dynamic relocation
  if (pSym.isUndef() &&
      !pSym.isDyn() &&
      (LinkerConfig::Exec   == config().codeGenType() ||
       LinkerConfig::Binary == config().codeGenType()))
    return false;

  // An absolute symbol can be resolved directly if it is either local
  // or we are linking statically. Otherwise it can still be overridden
  // at runtime.
  if (pSym.isAbsolute() &&
      (pSym.binding() == ResolveInfo::Local || config().isCodeStatic()))
    return false;
  if (config().isCodeIndep() && isAbsReloc)
    return true;
  if (pSymHasPLT && ResolveInfo::Function == pSym.type())
    return false;
  if (!config().isCodeIndep() && pSymHasPLT)
    return false;
  if (pSym.isDyn() || pSym.isUndef() ||
      isSymbolPreemptible(pSym))
    return true;

  return false;
}

/// symbolNeedsPLT - return whether the symbol needs a PLT entry
/// @ref Google gold linker, symtab.h:596
bool GNULDBackend::symbolNeedsPLT(const ResolveInfo& pSym) const
{
  if (pSym.isUndef() &&
      !pSym.isDyn() &&
      LinkerConfig::DynObj != config().codeGenType())
    return false;

  // An IndirectFunc symbol (i.e., STT_GNU_IFUNC) always needs a plt entry
  if (pSym.type() == ResolveInfo::IndirectFunc)
    return true;

  if (pSym.type() != ResolveInfo::Function)
    return false;

  if (config().isCodeStatic())
    return false;

  if (config().options().isPIE())
    return false;

  return (pSym.isDyn() ||
          pSym.isUndef() ||
          isSymbolPreemptible(pSym));
}

/// symbolHasFinalValue - return true if the symbol's value can be decided at
/// link time
/// @ref Google gold linker, Symbol::final_value_is_known
bool GNULDBackend::symbolFinalValueIsKnown(const ResolveInfo& pSym) const
{
  // if the output is pic code or if not executables, symbols' value may change
  // at runtime
  // FIXME: CodeIndep() || LinkerConfig::Relocatable == CodeGenType
  if (config().isCodeIndep() ||
      (LinkerConfig::Exec != config().codeGenType() &&
       LinkerConfig::Binary != config().codeGenType()))
    return false;

  // if the symbol is from dynamic object, then its value is unknown
  if (pSym.isDyn())
    return false;

  // if the symbol is not in dynamic object and is not undefined, then its value
  // is known
  if (!pSym.isUndef())
    return true;

  // if the symbol is undefined and not in dynamic objects, for example, a weak
  // undefined symbol, then whether the symbol's final value can be known
  // depends on whrther we're doing static link
  return config().isCodeStatic();
}

/// symbolNeedsCopyReloc - return whether the symbol needs a copy relocation
bool GNULDBackend::symbolNeedsCopyReloc(const Relocation& pReloc,
                                        const ResolveInfo& pSym) const
{
  // only the reference from dynamic executable to non-function symbol in
  // the dynamic objects may need copy relocation
  if (config().isCodeIndep() ||
      !pSym.isDyn() ||
      pSym.type() == ResolveInfo::Function ||
      pSym.size() == 0)
    return false;

  // check if the option -z nocopyreloc is given
  if (config().options().hasNoCopyReloc())
    return false;

  // TODO: Is this check necessary?
  // if relocation target place is readonly, a copy relocation is needed
  uint32_t flag = pReloc.targetRef().frag()->getParent()->getSection().flag();
  if (0 == (flag & llvm::ELF::SHF_WRITE))
    return true;

  return false;
}

LDSymbol& GNULDBackend::getTDATASymbol()
{
  assert(NULL != f_pTDATA);
  return *f_pTDATA;
}

const LDSymbol& GNULDBackend::getTDATASymbol() const
{
  assert(NULL != f_pTDATA);
  return *f_pTDATA;
}

LDSymbol& GNULDBackend::getTBSSSymbol()
{
  assert(NULL != f_pTBSS);
  return *f_pTBSS;
}

const LDSymbol& GNULDBackend::getTBSSSymbol() const
{
  assert(NULL != f_pTBSS);
  return *f_pTBSS;
}

void GNULDBackend::checkAndSetHasTextRel(const LDSection& pSection)
{
  if (m_bHasTextRel)
    return;

  // if the target section of the dynamic relocation is ALLOCATE but is not
  // writable, than we should set DF_TEXTREL
  const uint32_t flag = pSection.flag();
  if (0 == (flag & llvm::ELF::SHF_WRITE) && (flag & llvm::ELF::SHF_ALLOC))
    m_bHasTextRel = true;

  return;
}

/// initBRIslandFactory - initialize the branch island factory for relaxation
bool GNULDBackend::initBRIslandFactory()
{
  if (NULL == m_pBRIslandFactory) {
    m_pBRIslandFactory = new BranchIslandFactory(maxBranchOffset());
  }
  return true;
}

/// initStubFactory - initialize the stub factory for relaxation
bool GNULDBackend::initStubFactory()
{
  if (NULL == m_pStubFactory) {
    m_pStubFactory = new StubFactory();
  }
  return true;
}

bool GNULDBackend::relax(Module& pModule, IRBuilder& pBuilder)
{
  if (!mayRelax())
    return true;

  bool finished = true;
  do {
    if (doRelax(pModule, pBuilder, finished)) {
      // If the sections (e.g., .text) are relaxed, the layout is also changed
      // We need to do the following:

      // 1. set up the offset
      setOutputSectionOffset(pModule, pModule.begin(), pModule.end());

      // 2. set up the offset constraint of PT_RELRO
      if (config().options().hasRelro())
        setupRelro(pModule);

      // 3. set up the output sections' address
      setOutputSectionAddress(pModule, pModule.begin(), pModule.end());
    }
  } while (!finished);

  return true;
}

bool GNULDBackend::DynsymCompare::needGNUHash(const LDSymbol& X) const
{
  // FIXME: in bfd and gold linker, an undefined symbol might be hashed
  // when the ouput is not PIC, if the symbol is referred by a non pc-relative
  // reloc, and its value is set to the addr of the plt entry.
  return !X.resolveInfo()->isUndef() && !X.isDyn();
}

bool GNULDBackend::DynsymCompare::operator()(const LDSymbol* X,
                                             const LDSymbol* Y) const
{
  return !needGNUHash(*X) && needGNUHash(*Y);
}

