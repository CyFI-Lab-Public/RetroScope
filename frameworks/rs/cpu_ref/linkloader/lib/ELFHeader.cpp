/*
 * Copyright 2011, The Android Open Source Project
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

#include "ELFHeader.h"
#include "ELF.h"

char const *ELFHeaderHelperMixin::getClassStr(int clazz) {
  switch (clazz) {
  default:
#define CASE_PAIR(A, B) case A: return B;
  CASE_PAIR(ELFCLASSNONE, "Invalid class")
  CASE_PAIR(ELFCLASS32, "32bit")
  CASE_PAIR(ELFCLASS64, "64bit")
#undef CASE_PAIR
  }
}

char const *ELFHeaderHelperMixin::getEndiannessStr(int endianness) {
  switch (endianness) {
  default:
#define CASE_PAIR(A, B) case A: return B;
  CASE_PAIR(ELFDATANONE, "Invalid endianness")
  CASE_PAIR(ELFDATA2LSB, "Little endian")
  CASE_PAIR(ELFDATA2MSB, "Big endian")
#undef CASE_PAIR
  }
}

char const *ELFHeaderHelperMixin::getOSABIStr(int abi) {
  if (abi >= 64 && abi <= 255) {
    return "Architecture specific";
  }

  switch (abi) {
  default: return "Unknown OS ABI";
#define CASE_PAIR(A, B) case A: return B;
  CASE_PAIR(ELFOSABI_NONE, "No extensions or not specified")
  CASE_PAIR(ELFOSABI_HPUX, "HP-UX")
  CASE_PAIR(ELFOSABI_NETBSD, "NetBSD")
  CASE_PAIR(ELFOSABI_LINUX, "Linux")
  CASE_PAIR(ELFOSABI_SOLARIS, "Solaris")
  CASE_PAIR(ELFOSABI_AIX, "AIX")
  CASE_PAIR(ELFOSABI_FREEBSD, "FreeBSD")
  CASE_PAIR(ELFOSABI_TRU64, "Tru64")
  CASE_PAIR(ELFOSABI_MODESTO, "Modesto")
  CASE_PAIR(ELFOSABI_OPENBSD, "OpenBSD")
#undef CASE_PAIR
  }
}

char const *ELFHeaderHelperMixin::getObjectTypeStr(uint16_t type) {
  switch (type) {
  default: return "No file type";

  case ET_REL:  return "Relocatable file";
  case ET_EXEC: return "Executable file";
  case ET_DYN:  return "Shared object file";
  case ET_CORE: return "Core file";

  case ET_LOOS: case ET_HIOS:
    return "Operating system-specific";

  case ET_LOPROC: case ET_HIPROC:
    return "Processor-specific";
  }
}

char const *ELFHeaderHelperMixin::getMachineStr(uint16_t machine) {
  switch (machine) {
    default: return "No machine or unknown";
    case EM_386: return "Intel 80386 (X86)";
    case EM_X86_64: return "AMD x86-64 architecture";
    case EM_ARM: return "Advanced RISC Machine (ARM)";
    case EM_MIPS: return "MIPS";
  }
}

char const *ELFHeaderHelperMixin::getVersionStr(uint32_t version) {
  switch (version) {
    default: return "Invalid version";
    case EV_CURRENT: return "Current version";
  }
}
