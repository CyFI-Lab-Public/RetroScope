/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "ELFObjectLoaderImpl.h"

#include <llvm/Support/ELF.h>

// The following files are included from librsloader.
#include "ELFObject.h"
#include "ELFSectionSymTab.h"
#include "ELFSymbol.h"
#include "utils/serialize.h"

#include "bcc/ExecutionEngine/SymbolResolverInterface.h"
#include "bcc/Support/Log.h"

using namespace bcc;

bool ELFObjectLoaderImpl::load(const void *pMem, size_t pMemSize) {
  ArchiveReaderLE reader(reinterpret_cast<const unsigned char *>(pMem),
                         pMemSize);

  mObject = ELFObject<32>::read(reader);
  if (mObject == NULL) {
    ALOGE("Unable to load the ELF object!");
    return false;
  }

  // Retrive the pointer to the symbol table.
  mSymTab = static_cast<ELFSectionSymTab<32> *>(
                mObject->getSectionByName(".symtab"));
  if (mSymTab == NULL) {
    ALOGW("Object doesn't contain any symbol table.");
  }

  return true;
}

bool ELFObjectLoaderImpl::relocate(SymbolResolverInterface &pResolver) {
  mObject->relocate(SymbolResolverInterface::LookupFunction, &pResolver);

  if (mObject->getMissingSymbols()) {
    ALOGE("Some symbols are found to be undefined during relocation!");
    return false;
  }

  return true;
}

bool ELFObjectLoaderImpl::prepareDebugImage(void *pDebugImg,
                                            size_t pDebugImgSize) {
  // Update the value of sh_addr in pDebugImg to its corresponding section in
  // the mObject.
  llvm::ELF::Elf32_Ehdr *elf_header =
      reinterpret_cast<llvm::ELF::Elf32_Ehdr *>(pDebugImg);

  if (elf_header->e_shoff > pDebugImgSize) {
    ALOGE("Invalid section header table offset found! (e_shoff = %d)",
          elf_header->e_shoff);
    return false;
  }

  if ((elf_header->e_shoff +
       sizeof(llvm::ELF::Elf32_Shdr) * elf_header->e_shnum) > pDebugImgSize) {
    ALOGE("Invalid image supplied (debug image doesn't contain all the section"
          "header or corrupted image)! (e_shoff = %d, e_shnum = %d)",
          elf_header->e_shoff, elf_header->e_shnum);
    return false;
  }

  llvm::ELF::Elf32_Shdr *section_header_table =
      reinterpret_cast<llvm::ELF::Elf32_Shdr *>(
          reinterpret_cast<uint8_t*>(pDebugImg) + elf_header->e_shoff);

  for (unsigned i = 0; i < elf_header->e_shnum; i++) {
    if (section_header_table[i].sh_flags & llvm::ELF::SHF_ALLOC) {
      ELFSectionBits<32> *section =
          static_cast<ELFSectionBits<32> *>(mObject->getSectionByIndex(i));
      if (section != NULL) {
        section_header_table[i].sh_addr =
            reinterpret_cast<llvm::ELF::Elf32_Addr>(section->getBuffer());
      }
    }
  }

  return true;
}

void *ELFObjectLoaderImpl::getSymbolAddress(const char *pName) const {
  if (mSymTab == NULL) {
    return NULL;
  }

  const ELFSymbol<32> *symbol = mSymTab->getByName(pName);
  if (symbol == NULL) {
    ALOGV("Request symbol '%s' is not found in the object!", pName);
    return NULL;
  }

  return symbol->getAddress(mObject->getHeader()->getMachine(),
                            /* autoAlloc */false);
}

size_t ELFObjectLoaderImpl::getSymbolSize(const char *pName) const {
  if (mSymTab == NULL) {
    return 0;
  }

  const ELFSymbol<32> *symbol = mSymTab->getByName(pName);

  if (symbol == NULL) {
    ALOGV("Request symbol '%s' is not found in the object!", pName);
    return 0;
  }

  return static_cast<size_t>(symbol->getSize());

}

bool
ELFObjectLoaderImpl::getSymbolNameList(android::Vector<const char *>& pNameList,
                                       ObjectLoader::SymbolType pType) const {
  if (mSymTab == NULL) {
    return false;
  }

  unsigned elf_type;
  switch (pType) {
    case ObjectLoader::kFunctionType: {
      elf_type = llvm::ELF::STT_FUNC;
      break;
    }
    case ObjectLoader::kUnknownType: {
      break;
    }
    default: {
      assert(false && "Invalid symbol type given!");
      return false;
    }
  }

  for (size_t i = 0, e = mSymTab->size(); i != e; i++) {
    ELFSymbol<32> *symbol = (*mSymTab)[i];
    if (symbol == NULL) {
      continue;
    }

    if ((pType == ObjectLoader::kUnknownType) ||
        (symbol->getType() == elf_type)) {
      const char *symbol_name = symbol->getName();
      if (symbol_name != NULL) {
        pNameList.push_back(symbol_name);
      }
    }
  }

  return true;
}

ELFObjectLoaderImpl::~ELFObjectLoaderImpl() {
  delete mObject;
  return;
}
