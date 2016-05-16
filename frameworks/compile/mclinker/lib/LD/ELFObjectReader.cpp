//===- ELFObjectReader.cpp ------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/LD/ELFObjectReader.h>

#include <string>
#include <cassert>

#include <llvm/Support/ELF.h>
#include <llvm/ADT/Twine.h>

#include <mcld/IRBuilder.h>
#include <mcld/MC/MCLDInput.h>
#include <mcld/LD/ELFReader.h>
#include <mcld/LD/EhFrameReader.h>
#include <mcld/LD/EhFrame.h>
#include <mcld/Target/GNULDBackend.h>
#include <mcld/Support/MsgHandling.h>
#include <mcld/Object/ObjectBuilder.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// ELFObjectReader
//===----------------------------------------------------------------------===//
/// constructor
ELFObjectReader::ELFObjectReader(GNULDBackend& pBackend,
                                 IRBuilder& pBuilder,
                                 const LinkerConfig& pConfig)
  : ObjectReader(),
    m_pELFReader(NULL),
    m_pEhFrameReader(NULL),
    m_Builder(pBuilder),
    m_ReadFlag(ParseEhFrame),
    m_Backend(pBackend),
    m_Config(pConfig) {
  if (pConfig.targets().is32Bits() && pConfig.targets().isLittleEndian()) {
    m_pELFReader = new ELFReader<32, true>(pBackend);
  }
  else if (pConfig.targets().is64Bits() && pConfig.targets().isLittleEndian()) {
    m_pELFReader = new ELFReader<64, true>(pBackend);
  }

  m_pEhFrameReader = new EhFrameReader();
}

/// destructor
ELFObjectReader::~ELFObjectReader()
{
  delete m_pELFReader;
  delete m_pEhFrameReader;
}

/// isMyFormat
bool ELFObjectReader::isMyFormat(Input &pInput) const
{
  assert(pInput.hasMemArea());

  // Don't warning about the frequently requests.
  // MemoryArea has a list of cache to handle this.
  size_t hdr_size = m_pELFReader->getELFHeaderSize();
  MemoryRegion* region = pInput.memArea()->request(pInput.fileOffset(),
                                                     hdr_size);

  uint8_t* ELF_hdr = region->start();
  bool result = true;
  if (!m_pELFReader->isELF(ELF_hdr))
    result = false;
  else if (!m_pELFReader->isMyEndian(ELF_hdr))
    result = false;
  else if (!m_pELFReader->isMyMachine(ELF_hdr))
    result = false;
  else if (Input::Object != m_pELFReader->fileType(ELF_hdr))
    result = false;
  pInput.memArea()->release(region);
  return result;
}

/// readHeader - read section header and create LDSections.
bool ELFObjectReader::readHeader(Input& pInput)
{
  assert(pInput.hasMemArea());

  size_t hdr_size = m_pELFReader->getELFHeaderSize();
  MemoryRegion* region = pInput.memArea()->request(pInput.fileOffset(),
                                                     hdr_size);
  uint8_t* ELF_hdr = region->start();
  bool result = m_pELFReader->readSectionHeaders(pInput, ELF_hdr);
  pInput.memArea()->release(region);
  return result;
}

/// readSections - read all regular sections.
bool ELFObjectReader::readSections(Input& pInput)
{
  // handle sections
  LDContext::sect_iterator section, sectEnd = pInput.context()->sectEnd();
  for (section = pInput.context()->sectBegin(); section != sectEnd; ++section) {
    // ignore the section if the LDSection* in input context is NULL
    if (NULL == *section)
        continue;

    switch((*section)->kind()) {
      /** group sections **/
      case LDFileFormat::Group: {
        assert(NULL != (*section)->getLink());
        ResolveInfo* signature =
              m_pELFReader->readSignature(pInput,
                                          *(*section)->getLink(),
                                          (*section)->getInfo());

        bool exist = false;
        if (0 == signature->nameSize() &&
            ResolveInfo::Section == signature->type()) {
          // if the signature is a section symbol in input object, we use the
          // section name as group signature.
          signatures().insert((*section)->name(), exist);
        } else {
          signatures().insert(signature->name(), exist);
        }

        if (exist) {
          // if this is not the first time we see this group signature, then
          // ignore all the members in this group (set Ignore)
          MemoryRegion* region = pInput.memArea()->request(
               pInput.fileOffset() + (*section)->offset(), (*section)->size());
          llvm::ELF::Elf32_Word* value =
                     reinterpret_cast<llvm::ELF::Elf32_Word*>(region->start());

          size_t size = region->size() / sizeof(llvm::ELF::Elf32_Word);
          if (llvm::ELF::GRP_COMDAT == *value) {
            for (size_t index = 1; index < size; ++index) {
              pInput.context()->getSection(value[index])->setKind(LDFileFormat::Ignore);
            }
          }
          pInput.memArea()->release(region);
        }
        ResolveInfo::Destroy(signature);
        break;
      }
      /** linkonce sections **/
      case LDFileFormat::LinkOnce: {
        bool exist = false;
        // .gnu.linkonce + "." + type + "." + name
        llvm::StringRef name(llvm::StringRef((*section)->name()).drop_front(14));
        signatures().insert(name.split(".").second, exist);
        if (!exist) {
          if (name.startswith("wi")) {
            (*section)->setKind(LDFileFormat::Debug);
            if (m_Config.options().stripDebug())
              (*section)->setKind(LDFileFormat::Ignore);
            else {
              SectionData* sd = IRBuilder::CreateSectionData(**section);
              if (!m_pELFReader->readRegularSection(pInput, *sd))
                fatal(diag::err_cannot_read_section) << (*section)->name();
            }
          } else {
            (*section)->setKind(LDFileFormat::Regular);
            SectionData* sd = IRBuilder::CreateSectionData(**section);
            if (!m_pELFReader->readRegularSection(pInput, *sd))
              fatal(diag::err_cannot_read_section) << (*section)->name();
          }
        } else {
          (*section)->setKind(LDFileFormat::Ignore);
        }
        break;
      }
      /** relocation sections **/
      case LDFileFormat::Relocation: {
        assert(NULL != (*section)->getLink());
        size_t link_index = (*section)->getLink()->index();
        LDSection* link_sect = pInput.context()->getSection(link_index);
        if (NULL == link_sect || LDFileFormat::Ignore == link_sect->kind()) {
          // Relocation sections of group members should also be part of the
          // group. Thus, if the associated member sections are ignored, the
          // related relocations should be also ignored.
          (*section)->setKind(LDFileFormat::Ignore);
        }
        break;
      }
      /** normal sections **/
      // FIXME: support Version Kind
      case LDFileFormat::Version:
      // FIXME: support GCCExceptTable Kind
      case LDFileFormat::GCCExceptTable:
      /** Fall through **/
      case LDFileFormat::Regular:
      case LDFileFormat::Note:
      case LDFileFormat::MetaData: {
        SectionData* sd = IRBuilder::CreateSectionData(**section);
        if (!m_pELFReader->readRegularSection(pInput, *sd))
          fatal(diag::err_cannot_read_section) << (*section)->name();
        break;
      }
      case LDFileFormat::Debug: {
        if (m_Config.options().stripDebug()) {
          (*section)->setKind(LDFileFormat::Ignore);
        }
        else {
          SectionData* sd = IRBuilder::CreateSectionData(**section);
          if (!m_pELFReader->readRegularSection(pInput, *sd)) {
            fatal(diag::err_cannot_read_section) << (*section)->name();
          }
        }
        break;
      }
      case LDFileFormat::EhFrame: {
        EhFrame* eh_frame = IRBuilder::CreateEhFrame(**section);

        if (m_Config.options().hasEhFrameHdr() &&
            (m_ReadFlag & ParseEhFrame)) {

          // if --eh-frame-hdr option is given, parse .eh_frame.
          if (!m_pEhFrameReader->read<32, true>(pInput, *eh_frame)) {
            // if we failed to parse a .eh_frame, we should not parse the rest
            // .eh_frame.
            m_ReadFlag ^= ParseEhFrame;
          }
        }
        else {
          if (!m_pELFReader->readRegularSection(pInput,
                                                *eh_frame->getSectionData())) {
            fatal(diag::err_cannot_read_section) << (*section)->name();
          }
        }
        break;
      }
      /** target dependent sections **/
      case LDFileFormat::Target: {
        SectionData* sd = IRBuilder::CreateSectionData(**section);
        if (!m_Backend.readSection(pInput, *sd)) {
          fatal(diag::err_cannot_read_target_section) << (*section)->name();
        }
        break;
      }
      /** BSS sections **/
      case LDFileFormat::BSS: {
        IRBuilder::CreateBSS(**section);
        break;
      }
      // ignore
      case LDFileFormat::Null:
      case LDFileFormat::NamePool:
      case LDFileFormat::Ignore:
      case LDFileFormat::StackNote:
        continue;
      // warning
      case LDFileFormat::EhFrameHdr:
      default: {
        warning(diag::warn_illegal_input_section) << (*section)->name()
                                                  << pInput.name()
                                                  << pInput.path();
        break;
      }
    }
  } // end of for all sections

  return true;
}

/// readSymbols - read symbols from the input relocatable object.
bool ELFObjectReader::readSymbols(Input& pInput)
{
  assert(pInput.hasMemArea());

  LDSection* symtab_shdr = pInput.context()->getSection(".symtab");
  if (NULL == symtab_shdr) {
    note(diag::note_has_no_symtab) << pInput.name()
                                   << pInput.path()
                                   << ".symtab";
    return true;
  }

  LDSection* strtab_shdr = symtab_shdr->getLink();
  if (NULL == strtab_shdr) {
    fatal(diag::fatal_cannot_read_strtab) << pInput.name()
                                          << pInput.path()
                                          << ".symtab";
    return false;
  }

  MemoryRegion* symtab_region = pInput.memArea()->request(
             pInput.fileOffset() + symtab_shdr->offset(), symtab_shdr->size());
  MemoryRegion* strtab_region = pInput.memArea()->request(
             pInput.fileOffset() + strtab_shdr->offset(), strtab_shdr->size());
  char* strtab = reinterpret_cast<char*>(strtab_region->start());
  bool result = m_pELFReader->readSymbols(pInput,
                                          m_Builder,
                                          *symtab_region,
                                          strtab);
  pInput.memArea()->release(symtab_region);
  pInput.memArea()->release(strtab_region);
  return result;
}

bool ELFObjectReader::readRelocations(Input& pInput)
{
  assert(pInput.hasMemArea());

  MemoryArea* mem = pInput.memArea();
  LDContext::sect_iterator rs, rsEnd = pInput.context()->relocSectEnd();
  for (rs = pInput.context()->relocSectBegin(); rs != rsEnd; ++rs) {
    if (LDFileFormat::Ignore == (*rs)->kind())
      continue;

    uint32_t offset = pInput.fileOffset() + (*rs)->offset();
    uint32_t size = (*rs)->size();
    MemoryRegion* region = mem->request(offset, size);
    IRBuilder::CreateRelocData(**rs); ///< create relocation data for the header
    switch ((*rs)->type()) {
      case llvm::ELF::SHT_RELA: {
        if (!m_pELFReader->readRela(pInput, **rs, *region)) {
          mem->release(region);
          return false;
        }
        break;
      }
      case llvm::ELF::SHT_REL: {
        if (!m_pELFReader->readRel(pInput, **rs, *region)) {
          mem->release(region);
          return false;
        }
        break;
      }
      default: { ///< should not enter
        mem->release(region);
        return false;
      }
    } // end of switch

    mem->release(region);
  } // end of for all relocation data

  return true;
}

