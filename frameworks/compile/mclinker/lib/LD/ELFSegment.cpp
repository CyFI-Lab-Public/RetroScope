//===- ELFSegment.cpp -----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/LD/ELFSegment.h>

using namespace mcld;

//==========================
// ELFSegment
ELFSegment::ELFSegment(uint32_t pType,
                       uint32_t pFlag,
                       uint64_t pOffset,
                       uint64_t pVaddr,
                       uint64_t pPaddr,
                       uint64_t pFilesz,
                       uint64_t pMemsz,
                       uint64_t pAlign,
                       uint64_t pMaxSectAlign)
  : m_Type(pType),
    m_Flag(pFlag),
    m_Offset(pOffset),
    m_Vaddr(pVaddr),
    m_Paddr(pPaddr),
    m_Filesz(pFilesz),
    m_Memsz(pMemsz),
    m_Align(pAlign),
    m_MaxSectionAlign(pMaxSectAlign) {
}

ELFSegment::~ELFSegment()
{
}

bool ELFSegment::isDataSegment() const
{
  bool result = false;
  if ((type() == llvm::ELF::PT_LOAD) && (flag() & llvm::ELF::PF_W) != 0x0) {
    for (const_sect_iterator it = begin(), ie = end(); it != ie; ++it) {
      if ((*it)->kind() != LDFileFormat::BSS) {
        result = true;
        break;
      }
    }
  }
  return result;
}

bool ELFSegment::isBssSegment() const
{
  bool result = false;
  if ((type() == llvm::ELF::PT_LOAD) && (flag() & llvm::ELF::PF_W) != 0x0) {
    const_sect_iterator it = begin(), ie = end();
    for (; it != ie; ++it) {
      if ((*it)->kind() != LDFileFormat::BSS)
        break;
    }
    if (it == ie)
      result = true;
  }
  return result;
}

