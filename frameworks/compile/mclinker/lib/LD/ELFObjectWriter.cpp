//===- ELFObjectWriter.cpp ------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/LD/ELFObjectWriter.h>

#include <mcld/Module.h>
#include <mcld/LinkerConfig.h>
#include <mcld/Target/GNULDBackend.h>
#include <mcld/Support/MemoryArea.h>
#include <mcld/Support/MemoryRegion.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/ADT/SizeTraits.h>
#include <mcld/Fragment/FragmentLinker.h>
#include <mcld/Fragment/AlignFragment.h>
#include <mcld/Fragment/FillFragment.h>
#include <mcld/Fragment/RegionFragment.h>
#include <mcld/Fragment/Stub.h>
#include <mcld/Fragment/NullFragment.h>
#include <mcld/LD/LDSection.h>
#include <mcld/LD/SectionData.h>
#include <mcld/LD/ELFSegment.h>
#include <mcld/LD/ELFSegmentFactory.h>
#include <mcld/LD/RelocData.h>
#include <mcld/LD/EhFrame.h>

#include <llvm/Support/ErrorHandling.h>
#include <llvm/Support/system_error.h>
#include <llvm/Support/ELF.h>
#include <llvm/Support/Casting.h>

using namespace llvm;
using namespace llvm::ELF;
using namespace mcld;

//===----------------------------------------------------------------------===//
// ELFObjectWriter
//===----------------------------------------------------------------------===//
ELFObjectWriter::ELFObjectWriter(GNULDBackend& pBackend,
                                 const LinkerConfig& pConfig)
  : ObjectWriter(), m_Backend(pBackend), m_Config(pConfig)
{
}

ELFObjectWriter::~ELFObjectWriter()
{
}

void ELFObjectWriter::writeSection(MemoryArea& pOutput, LDSection *section)
{
  MemoryRegion* region;
  // Request output region
  switch (section->kind()) {
  case LDFileFormat::Note:
    if (section->getSectionData() == NULL)
      return;
    // Fall through
  case LDFileFormat::Regular:
  case LDFileFormat::Relocation:
  case LDFileFormat::Target:
  case LDFileFormat::Debug:
  case LDFileFormat::GCCExceptTable:
  case LDFileFormat::EhFrame: {
    region = pOutput.request(section->offset(), section->size());
    if (NULL == region) {
      llvm::report_fatal_error(llvm::Twine("cannot get enough memory region for output section `") +
                               llvm::Twine(section->name()) +
                               llvm::Twine("'.\n"));
    }
    break;
  }
  case LDFileFormat::Null:
  case LDFileFormat::NamePool:
  case LDFileFormat::BSS:
  case LDFileFormat::MetaData:
  case LDFileFormat::Version:
  case LDFileFormat::EhFrameHdr:
  case LDFileFormat::StackNote:
    // Ignore these sections
    return;
  default:
    llvm::errs() << "WARNING: unsupported section kind: "
                 << section->kind()
                 << " of section "
                 << section->name()
                 << ".\n";
    return;
  }

  // Write out sections with data
  switch(section->kind()) {
  case LDFileFormat::GCCExceptTable:
  case LDFileFormat::EhFrame:
  case LDFileFormat::Regular:
  case LDFileFormat::Debug:
  case LDFileFormat::Note:
    // FIXME: if optimization of exception handling sections is enabled,
    // then we should emit these sections by the other way.
    emitSectionData(*section, *region);
    break;
  case LDFileFormat::Relocation:
    emitRelocation(m_Config, *section, *region);
    break;
  case LDFileFormat::Target:
    target().emitSectionData(*section, *region);
    break;
  default:
    llvm_unreachable("invalid section kind");
  }
}

llvm::error_code ELFObjectWriter::writeObject(Module& pModule,
                                              MemoryArea& pOutput)
{
  bool is_dynobj = m_Config.codeGenType() == LinkerConfig::DynObj;
  bool is_exec = m_Config.codeGenType() == LinkerConfig::Exec;
  bool is_binary = m_Config.codeGenType() == LinkerConfig::Binary;
  bool is_object = m_Config.codeGenType() == LinkerConfig::Object;

  assert(is_dynobj || is_exec || is_binary || is_object);

  if (is_dynobj || is_exec) {
    // Allow backend to sort symbols before emitting
    target().orderSymbolTable(pModule);

    // Write out the interpreter section: .interp
    target().emitInterp(pOutput);

    // Write out name pool sections: .dynsym, .dynstr, .hash
    target().emitDynNamePools(pModule, pOutput);
  }

  if (is_object || is_dynobj || is_exec) {
    // Write out name pool sections: .symtab, .strtab
    target().emitRegNamePools(pModule, pOutput);
  }

  if (is_binary) {
    // Iterate over the loadable segments and write the corresponding sections
    ELFSegmentFactory::iterator seg, segEnd = target().elfSegmentTable().end();

    for (seg = target().elfSegmentTable().begin(); seg != segEnd; ++seg) {
      if (llvm::ELF::PT_LOAD == (*seg).type()) {
        ELFSegment::sect_iterator sect, sectEnd = (*seg).end();
        for (sect = (*seg).begin(); sect != sectEnd; ++sect)
          writeSection(pOutput, *sect);
      }
    }
  } else {
    // Write out regular ELF sections
    Module::iterator sect, sectEnd = pModule.end();
    for (sect = pModule.begin(); sect != sectEnd; ++sect)
      writeSection(pOutput, *sect);

    emitShStrTab(target().getOutputFormat()->getShStrTab(), pModule, pOutput);

    if (m_Config.targets().is32Bits()) {
      // Write out ELF header
      // Write out section header table
      writeELFHeader<32>(m_Config, pModule, pOutput);
      if (is_dynobj || is_exec)
        emitProgramHeader<32>(pOutput);

      emitSectionHeader<32>(pModule, m_Config, pOutput);
    }
    else if (m_Config.targets().is64Bits()) {
      // Write out ELF header
      // Write out section header table
      writeELFHeader<64>(m_Config, pModule, pOutput);
      if (is_dynobj || is_exec)
        emitProgramHeader<64>(pOutput);

      emitSectionHeader<64>(pModule, m_Config, pOutput);
    }
    else
      return make_error_code(errc::not_supported);
  }

  pOutput.clear();
  return llvm::make_error_code(llvm::errc::success);
}

// writeELFHeader - emit ElfXX_Ehdr
template<size_t SIZE>
void ELFObjectWriter::writeELFHeader(const LinkerConfig& pConfig,
                                     const Module& pModule,
                                     MemoryArea& pOutput) const
{
  typedef typename ELFSizeTraits<SIZE>::Ehdr ElfXX_Ehdr;
  typedef typename ELFSizeTraits<SIZE>::Shdr ElfXX_Shdr;
  typedef typename ELFSizeTraits<SIZE>::Phdr ElfXX_Phdr;

  // ELF header must start from 0x0
  MemoryRegion *region = pOutput.request(0, sizeof(ElfXX_Ehdr));
  ElfXX_Ehdr* header = (ElfXX_Ehdr*)region->start();

  memcpy(header->e_ident, ElfMagic, EI_MAG3+1);

  header->e_ident[EI_CLASS]      = (SIZE == 32) ? ELFCLASS32 : ELFCLASS64;
  header->e_ident[EI_DATA]       = pConfig.targets().isLittleEndian()?
                                       ELFDATA2LSB : ELFDATA2MSB;
  header->e_ident[EI_VERSION]    = target().getInfo().ELFVersion();
  header->e_ident[EI_OSABI]      = target().getInfo().OSABI();
  header->e_ident[EI_ABIVERSION] = target().getInfo().ABIVersion();

  // FIXME: add processor-specific and core file types.
  switch(pConfig.codeGenType()) {
    case LinkerConfig::Object:
      header->e_type = ET_REL;
      break;
    case LinkerConfig::DynObj:
      header->e_type = ET_DYN;
      break;
    case LinkerConfig::Exec:
      header->e_type = ET_EXEC;
      break;
    default:
      llvm::errs() << "unspported output file type: " << pConfig.codeGenType() << ".\n";
      header->e_type = ET_NONE;
  }
  header->e_machine   = target().getInfo().machine();
  header->e_version   = header->e_ident[EI_VERSION];
  header->e_entry     = getEntryPoint(pConfig, pModule);

  if (LinkerConfig::Object != pConfig.codeGenType())
    header->e_phoff   = sizeof(ElfXX_Ehdr);
  else
    header->e_phoff   = 0x0;

  header->e_shoff     = getLastStartOffset<SIZE>(pModule);
  header->e_flags     = target().getInfo().flags();
  header->e_ehsize    = sizeof(ElfXX_Ehdr);
  header->e_phentsize = sizeof(ElfXX_Phdr);
  header->e_phnum     = target().numOfSegments();
  header->e_shentsize = sizeof(ElfXX_Shdr);
  header->e_shnum     = pModule.size();
  header->e_shstrndx  = pModule.getSection(".shstrtab")->index();
}

/// getEntryPoint
uint64_t ELFObjectWriter::getEntryPoint(const LinkerConfig& pConfig,
                                        const Module& pModule) const
{
  llvm::StringRef entry_name;
  if (pConfig.options().hasEntry())
    entry_name = pConfig.options().entry();
  else
    entry_name = target().getInfo().entry();

  uint64_t result = 0x0;

  bool issue_warning = (pConfig.options().hasEntry() &&
                        LinkerConfig::Object != pConfig.codeGenType() &&
                        LinkerConfig::DynObj != pConfig.codeGenType());

  const LDSymbol* entry_symbol = pModule.getNamePool().findSymbol(entry_name);

  // found the symbol
  if (NULL != entry_symbol) {
    if (entry_symbol->desc() != ResolveInfo::Define && issue_warning) {
      llvm::errs() << "WARNING: entry symbol '"
                   << entry_symbol->name()
                   << "' exists but is not defined.\n";
    }
    result = entry_symbol->value();
  }
  // not in the symbol pool
  else {
    // We should parse entry as a number.
    // @ref GNU ld manual, Options -e. e.g., -e 0x1000.
    char* endptr;
    result = strtoull(entry_name.data(), &endptr, 0);
    if (*endptr != '\0') {
      if (issue_warning) {
        llvm::errs() << "cannot find entry symbol '"
                     << entry_name.data()
                     << "'.\n";
      }
      result = 0x0;
    }
  }
  return result;
}

// emitSectionHeader - emit ElfXX_Shdr
template<size_t SIZE>
void ELFObjectWriter::emitSectionHeader(const Module& pModule,
                                        const LinkerConfig& pConfig,
                                        MemoryArea& pOutput) const
{
  typedef typename ELFSizeTraits<SIZE>::Shdr ElfXX_Shdr;

  // emit section header
  unsigned int sectNum = pModule.size();
  unsigned int header_size = sizeof(ElfXX_Shdr) * sectNum;
  MemoryRegion* region = pOutput.request(getLastStartOffset<SIZE>(pModule),
                                         header_size);
  ElfXX_Shdr* shdr = (ElfXX_Shdr*)region->start();

  // Iterate the SectionTable in LDContext
  unsigned int sectIdx = 0;
  unsigned int shstridx = 0; // NULL section has empty name
  for (; sectIdx < sectNum; ++sectIdx) {
    const LDSection *ld_sect   = pModule.getSectionTable().at(sectIdx);
    shdr[sectIdx].sh_name      = shstridx;
    shdr[sectIdx].sh_type      = ld_sect->type();
    shdr[sectIdx].sh_flags     = ld_sect->flag();
    shdr[sectIdx].sh_addr      = ld_sect->addr();
    shdr[sectIdx].sh_offset    = ld_sect->offset();
    shdr[sectIdx].sh_size      = ld_sect->size();
    shdr[sectIdx].sh_addralign = ld_sect->align();
    shdr[sectIdx].sh_entsize   = getSectEntrySize<SIZE>(*ld_sect);
    shdr[sectIdx].sh_link      = getSectLink(*ld_sect, pConfig);
    shdr[sectIdx].sh_info      = getSectInfo(*ld_sect);

    // adjust strshidx
    shstridx += ld_sect->name().size() + 1;
  }
}

// emitProgramHeader - emit ElfXX_Phdr
template<size_t SIZE>
void ELFObjectWriter::emitProgramHeader(MemoryArea& pOutput) const
{
  typedef typename ELFSizeTraits<SIZE>::Ehdr ElfXX_Ehdr;
  typedef typename ELFSizeTraits<SIZE>::Phdr ElfXX_Phdr;

  uint64_t start_offset, phdr_size;

  start_offset = sizeof(ElfXX_Ehdr);
  phdr_size = sizeof(ElfXX_Phdr);
  // Program header must start directly after ELF header
  MemoryRegion *region = pOutput.request(start_offset,
                                         target().numOfSegments() * phdr_size);

  ElfXX_Phdr* phdr = (ElfXX_Phdr*)region->start();

  // Iterate the elf segment table in GNULDBackend
  size_t index = 0;
  ELFSegmentFactory::const_iterator seg = target().elfSegmentTable().begin(),
                                 segEnd = target().elfSegmentTable().end();
  for (; seg != segEnd; ++seg, ++index) {
    phdr[index].p_type   = (*seg).type();
    phdr[index].p_flags  = (*seg).flag();
    phdr[index].p_offset = (*seg).offset();
    phdr[index].p_vaddr  = (*seg).vaddr();
    phdr[index].p_paddr  = (*seg).paddr();
    phdr[index].p_filesz = (*seg).filesz();
    phdr[index].p_memsz  = (*seg).memsz();
    phdr[index].p_align  = (*seg).align();
  }
}

/// emitShStrTab - emit section string table
void
ELFObjectWriter::emitShStrTab(const LDSection& pShStrTab,
                              const Module& pModule,
                              MemoryArea& pOutput)
{
  // write out data
  MemoryRegion* region = pOutput.request(pShStrTab.offset(), pShStrTab.size());
  unsigned char* data = region->start();
  size_t shstrsize = 0;
  Module::const_iterator section, sectEnd = pModule.end();
  for (section = pModule.begin(); section != sectEnd; ++section) {
    strcpy((char*)(data + shstrsize), (*section)->name().data());
    shstrsize += (*section)->name().size() + 1;
  }
}

/// emitSectionData
void
ELFObjectWriter::emitSectionData(const LDSection& pSection,
                                 MemoryRegion& pRegion) const
{
  const SectionData* sd = NULL;
  switch (pSection.kind()) {
    case LDFileFormat::Relocation:
      assert(pSection.hasRelocData());
      return;
    case LDFileFormat::EhFrame:
      assert(pSection.hasEhFrame());
      sd = pSection.getEhFrame()->getSectionData();
      break;
    default:
      assert(pSection.hasSectionData());
      sd = pSection.getSectionData();
      break;
  }
  emitSectionData(*sd, pRegion);
}

/// emitRelocation
void ELFObjectWriter::emitRelocation(const LinkerConfig& pConfig,
                                     const LDSection& pSection,
                                     MemoryRegion& pRegion) const
{
  const RelocData* sect_data = pSection.getRelocData();
  assert(NULL != sect_data && "SectionData is NULL in emitRelocation!");

  if (pSection.type() == SHT_REL) {
    if (pConfig.targets().is32Bits())
      emitRel<32>(pConfig, *sect_data, pRegion);
    else if (pConfig.targets().is64Bits())
      emitRel<64>(pConfig, *sect_data, pRegion);
    else {
      fatal(diag::unsupported_bitclass) << pConfig.targets().triple().str()
                                        << pConfig.targets().bitclass();
    }
  } else if (pSection.type() == SHT_RELA) {
    if (pConfig.targets().is32Bits())
      emitRela<32>(pConfig, *sect_data, pRegion);
    else if (pConfig.targets().is64Bits())
      emitRela<64>(pConfig, *sect_data, pRegion);
    else {
      fatal(diag::unsupported_bitclass) << pConfig.targets().triple().str()
                                        << pConfig.targets().bitclass();
    }
  } else
    llvm::report_fatal_error("unsupported relocation section type!");
}


// emitRel - emit ElfXX_Rel
template<size_t SIZE>
void ELFObjectWriter::emitRel(const LinkerConfig& pConfig,
                              const RelocData& pRelocData,
                              MemoryRegion& pRegion) const
{
  typedef typename ELFSizeTraits<SIZE>::Rel  ElfXX_Rel;
  typedef typename ELFSizeTraits<SIZE>::Addr ElfXX_Addr;
  typedef typename ELFSizeTraits<SIZE>::Word ElfXX_Word;

  ElfXX_Rel* rel = reinterpret_cast<ElfXX_Rel*>(pRegion.start());

  const Relocation* relocation = 0;
  const FragmentRef* frag_ref = 0;

  for (RelocData::const_iterator it = pRelocData.begin(),
       ie = pRelocData.end(); it != ie; ++it, ++rel) {

    relocation = &(llvm::cast<Relocation>(*it));
    frag_ref = &(relocation->targetRef());

    if(LinkerConfig::DynObj == pConfig.codeGenType() ||
       LinkerConfig::Exec == pConfig.codeGenType()) {
      rel->r_offset = static_cast<ElfXX_Addr>(
                      frag_ref->frag()->getParent()->getSection().addr() +
                      frag_ref->getOutputOffset());
    }
    else {
      rel->r_offset = static_cast<ElfXX_Addr>(frag_ref->getOutputOffset());
    }
    ElfXX_Word Index;
    if( relocation->symInfo() == NULL )
      Index = 0;
    else
      Index = static_cast<ElfXX_Word>(
              target().getSymbolIdx(relocation->symInfo()->outSymbol()));

    rel->setSymbolAndType(Index, relocation->type());
  }
}

// emitRela - emit ElfXX_Rela
template<size_t SIZE>
void ELFObjectWriter::emitRela(const LinkerConfig& pConfig,
                               const RelocData& pRelocData,
                               MemoryRegion& pRegion) const
{
  typedef typename ELFSizeTraits<SIZE>::Rela ElfXX_Rela;
  typedef typename ELFSizeTraits<SIZE>::Addr ElfXX_Addr;
  typedef typename ELFSizeTraits<SIZE>::Word ElfXX_Word;

  ElfXX_Rela* rel = reinterpret_cast<ElfXX_Rela*>(pRegion.start());

  const Relocation* relocation = 0;
  const FragmentRef* frag_ref = 0;

  for (RelocData::const_iterator it = pRelocData.begin(),
       ie = pRelocData.end(); it != ie; ++it, ++rel) {

    relocation = &(llvm::cast<Relocation>(*it));
    frag_ref = &(relocation->targetRef());

    if(LinkerConfig::DynObj == pConfig.codeGenType() ||
       LinkerConfig::Exec == pConfig.codeGenType()) {
      rel->r_offset = static_cast<ElfXX_Addr>(
                      frag_ref->frag()->getParent()->getSection().addr() +
                      frag_ref->getOutputOffset());
    }
    else {
      rel->r_offset = static_cast<ElfXX_Addr>(frag_ref->getOutputOffset());
    }

    ElfXX_Word Index;
    if( relocation->symInfo() == NULL )
      Index = 0;
    else
      Index = static_cast<ElfXX_Word>(
              target().getSymbolIdx(relocation->symInfo()->outSymbol()));

    rel->setSymbolAndType(Index, relocation->type());
    rel->r_addend = relocation->addend();
  }
}


/// getSectEntrySize - compute ElfXX_Shdr::sh_entsize
template<size_t SIZE>
uint64_t ELFObjectWriter::getSectEntrySize(const LDSection& pSection) const
{
  typedef typename ELFSizeTraits<SIZE>::Word ElfXX_Word;
  typedef typename ELFSizeTraits<SIZE>::Sym  ElfXX_Sym;
  typedef typename ELFSizeTraits<SIZE>::Rel  ElfXX_Rel;
  typedef typename ELFSizeTraits<SIZE>::Rela ElfXX_Rela;
  typedef typename ELFSizeTraits<SIZE>::Dyn  ElfXX_Dyn;

  if (llvm::ELF::SHT_DYNSYM == pSection.type() ||
      llvm::ELF::SHT_SYMTAB == pSection.type())
    return sizeof(ElfXX_Sym);
  if (llvm::ELF::SHT_REL == pSection.type())
    return sizeof(ElfXX_Rel);
  if (llvm::ELF::SHT_RELA == pSection.type())
    return sizeof(ElfXX_Rela);
  if (llvm::ELF::SHT_HASH     == pSection.type() ||
      llvm::ELF::SHT_GNU_HASH == pSection.type())
    return sizeof(ElfXX_Word);
  if (llvm::ELF::SHT_DYNAMIC == pSection.type())
    return sizeof(ElfXX_Dyn);
  return 0x0;
}

/// getSectLink - compute ElfXX_Shdr::sh_link
uint64_t ELFObjectWriter::getSectLink(const LDSection& pSection,
                                      const LinkerConfig& pConfig) const
{
  if (llvm::ELF::SHT_SYMTAB == pSection.type())
    return target().getOutputFormat()->getStrTab().index();
  if (llvm::ELF::SHT_DYNSYM == pSection.type())
    return target().getOutputFormat()->getDynStrTab().index();
  if (llvm::ELF::SHT_DYNAMIC == pSection.type())
    return target().getOutputFormat()->getDynStrTab().index();
  if (llvm::ELF::SHT_HASH     == pSection.type() ||
      llvm::ELF::SHT_GNU_HASH == pSection.type())
    return target().getOutputFormat()->getDynSymTab().index();
  if (llvm::ELF::SHT_REL == pSection.type() ||
      llvm::ELF::SHT_RELA == pSection.type()) {
    if (LinkerConfig::Object == pConfig.codeGenType())
      return target().getOutputFormat()->getSymTab().index();
    else
      return target().getOutputFormat()->getDynSymTab().index();
  }
  // FIXME: currently we link ARM_EXIDX section to output text section here
  if (llvm::ELF::SHT_ARM_EXIDX == pSection.type())
    return target().getOutputFormat()->getText().index();
  return llvm::ELF::SHN_UNDEF;
}

/// getSectInfo - compute ElfXX_Shdr::sh_info
uint64_t ELFObjectWriter::getSectInfo(const LDSection& pSection) const
{
  if (llvm::ELF::SHT_SYMTAB == pSection.type() ||
      llvm::ELF::SHT_DYNSYM == pSection.type())
    return pSection.getInfo();

  if (llvm::ELF::SHT_REL == pSection.type() ||
      llvm::ELF::SHT_RELA == pSection.type()) {
    const LDSection* info_link = pSection.getLink();
    if (NULL != info_link)
      return info_link->index();
  }

  return 0x0;
}

/// getLastStartOffset
template<>
uint64_t ELFObjectWriter::getLastStartOffset<32>(const Module& pModule) const
{
  const LDSection* lastSect = pModule.back();
  assert(lastSect != NULL);
  return Align<32>(lastSect->offset() + lastSect->size());
}

/// getLastStartOffset
template<>
uint64_t ELFObjectWriter::getLastStartOffset<64>(const Module& pModule) const
{
  const LDSection* lastSect = pModule.back();
  assert(lastSect != NULL);
  return Align<64>(lastSect->offset() + lastSect->size());
}

/// emitSectionData
void ELFObjectWriter::emitSectionData(const SectionData& pSD,
                                      MemoryRegion& pRegion) const
{
  SectionData::const_iterator fragIter, fragEnd = pSD.end();
  size_t cur_offset = 0;
  for (fragIter = pSD.begin(); fragIter != fragEnd; ++fragIter) {
    size_t size = fragIter->size();
    switch(fragIter->getKind()) {
      case Fragment::Region: {
        const RegionFragment& region_frag = llvm::cast<RegionFragment>(*fragIter);
        const uint8_t* from = region_frag.getRegion().start();
        memcpy(pRegion.getBuffer(cur_offset), from, size);
        break;
      }
      case Fragment::Alignment: {
        // TODO: emit values with different sizes (> 1 byte), and emit nops
        const AlignFragment& align_frag = llvm::cast<AlignFragment>(*fragIter);
        uint64_t count = size / align_frag.getValueSize();
        switch (align_frag.getValueSize()) {
          case 1u:
            std::memset(pRegion.getBuffer(cur_offset),
                        align_frag.getValue(),
                        count);
            break;
          default:
            llvm::report_fatal_error("unsupported value size for align fragment emission yet.\n");
            break;
        }
        break;
      }
      case Fragment::Fillment: {
        const FillFragment& fill_frag = llvm::cast<FillFragment>(*fragIter);
        if (0 == size ||
            0 == fill_frag.getValueSize() ||
            0 == fill_frag.size()) {
          // ignore virtual fillment
          break;
        }

        uint64_t num_tiles = fill_frag.size() / fill_frag.getValueSize();
        for (uint64_t i = 0; i != num_tiles; ++i) {
          std::memset(pRegion.getBuffer(cur_offset),
                      fill_frag.getValue(),
                      fill_frag.getValueSize());
        }
        break;
      }
      case Fragment::Stub: {
        const Stub& stub_frag = llvm::cast<Stub>(*fragIter);
        memcpy(pRegion.getBuffer(cur_offset), stub_frag.getContent(), size);
        break;
      }
      case Fragment::Null: {
        assert(0x0 == size);
        break;
      }
      case Fragment::Target:
        llvm::report_fatal_error("Target fragment should not be in a regular section.\n");
        break;
      default:
        llvm::report_fatal_error("invalid fragment should not be in a regular section.\n");
        break;
    }
    cur_offset += size;
  }
}

