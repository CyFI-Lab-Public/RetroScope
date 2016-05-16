/*
 * Copyright 2011-2012, The Android Open Source Project
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

#include "librsloader.h"

#include "ELFObject.h"
#include "ELFSectionSymTab.h"
#include "ELFSymbol.h"

#include "utils/serialize.h"

#define LOG_TAG "bcc"
#include "cutils/log.h"

#include <llvm/ADT/OwningPtr.h>
#include <llvm/Support/ELF.h>

static inline RSExecRef wrap(ELFObject<32> *object) {
  return reinterpret_cast<RSExecRef>(object);
}

static inline ELFObject<32> *unwrap(RSExecRef object) {
  return reinterpret_cast<ELFObject<32> *>(object);
}

extern "C" RSExecRef rsloaderCreateExec(unsigned char const *buf,
                                        size_t buf_size,
                                        RSFindSymbolFn find_symbol,
                                        void *find_symbol_context) {
  RSExecRef object = rsloaderLoadExecutable(buf, buf_size);
  if (!object) {
    return NULL;
  }

  if (!rsloaderRelocateExecutable(object, find_symbol, find_symbol_context)) {
    rsloaderDisposeExec(object);
    return NULL;
  }

  return object;
}

extern "C" RSExecRef rsloaderLoadExecutable(unsigned char const *buf,
                                            size_t buf_size) {
  ArchiveReaderLE AR(buf, buf_size);

  llvm::OwningPtr<ELFObject<32> > object(ELFObject<32>::read(AR));
  if (!object) {
    ALOGE("Unable to load the ELF object.");
    return NULL;
  }

  return wrap(object.take());
}

extern "C" int rsloaderRelocateExecutable(RSExecRef object_,
                                          RSFindSymbolFn find_symbol,
                                          void *find_symbol_context) {
  ELFObject<32>* object = unwrap(object_);

  object->relocate(find_symbol, find_symbol_context);
  return (object->getMissingSymbols() == 0);
}

extern "C" void rsloaderUpdateSectionHeaders(RSExecRef object_,
                                             unsigned char *buf) {
  ELFObject<32> *object = unwrap(object_);

  // Remap the section header addresses to match the loaded code
  llvm::ELF::Elf32_Ehdr* header = reinterpret_cast<llvm::ELF::Elf32_Ehdr*>(buf);

  llvm::ELF::Elf32_Shdr* shtab =
      reinterpret_cast<llvm::ELF::Elf32_Shdr*>(buf + header->e_shoff);

  for (int i = 0; i < header->e_shnum; i++) {
    if (shtab[i].sh_flags & SHF_ALLOC) {
      ELFSectionBits<32>* bits =
          static_cast<ELFSectionBits<32>*>(object->getSectionByIndex(i));
      if (bits) {
        const unsigned char* addr = bits->getBuffer();
        shtab[i].sh_addr = reinterpret_cast<llvm::ELF::Elf32_Addr>(addr);
      }
    }
  }
}

extern "C" void rsloaderDisposeExec(RSExecRef object) {
  delete unwrap(object);
}

extern "C" void *rsloaderGetSymbolAddress(RSExecRef object_,
                                          char const *name) {
  ELFObject<32> *object = unwrap(object_);

  ELFSectionSymTab<32> *symtab =
    static_cast<ELFSectionSymTab<32> *>(object->getSectionByName(".symtab"));

  if (!symtab) {
    return NULL;
  }

  ELFSymbol<32> *symbol = symtab->getByName(name);

  if (!symbol) {
    ALOGV("Symbol not found: %s\n", name);
    return NULL;
  }

  int machine = object->getHeader()->getMachine();

  return symbol->getAddress(machine, false);
}

extern "C" size_t rsloaderGetSymbolSize(RSExecRef object_, char const *name) {
  ELFObject<32> *object = unwrap(object_);

  ELFSectionSymTab<32> *symtab =
    static_cast<ELFSectionSymTab<32> *>(object->getSectionByName(".symtab"));

  if (!symtab) {
    return 0;
  }

  ELFSymbol<32> *symbol = symtab->getByName(name);

  if (!symbol) {
    ALOGV("Symbol not found: %s\n", name);
    return 0;
  }

  return (size_t)symbol->getSize();
}

extern "C" size_t rsloaderGetFuncCount(RSExecRef object) {
  ELFSectionSymTab<32> *symtab = static_cast<ELFSectionSymTab<32> *>(
    unwrap(object)->getSectionByName(".symtab"));

  if (!symtab) {
    return 0;
  }

  return symtab->getFuncCount();
}

extern "C" void rsloaderGetFuncNameList(RSExecRef object,
                                        size_t size,
                                        char const **list) {
  ELFSectionSymTab<32> *symtab = static_cast<ELFSectionSymTab<32> *>(
    unwrap(object)->getSectionByName(".symtab"));

  if (symtab) {
    symtab->getFuncNameList(size, list);
  }
}
