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

#ifndef ELF_HEADER_HXX
#define ELF_HEADER_HXX

#include "utils/raw_ostream.h"

#include <llvm/Support/raw_ostream.h>
#include <llvm/Support/Format.h>

template <unsigned Bitwidth>
void ELFHeader<Bitwidth>::print() {
  using namespace llvm;

  out() << fillformat('=', 79) << '\n';
  out().changeColor(raw_ostream::WHITE, true);
  out() << "ELF Header\n";
  out().resetColor();
  out() << fillformat('-', 79) << '\n';

#define PRINT_LINT(title, value) \
  out() << format("  %-32s : ", (char const *)(title)) << (value) << '\n'
  PRINT_LINT("Class",                 getClassStr(getClass()));
  PRINT_LINT("Endianness",            getEndiannessStr(getEndianness()));
  PRINT_LINT("Header Version",        (unsigned)getVersion());
  PRINT_LINT("OS ABI",                getOSABIStr(getOSABI()));
  PRINT_LINT("ABI Version",           (unsigned)getABIVersion());
  PRINT_LINT("Object Type",           getObjectTypeStr(getObjectType()));
  PRINT_LINT("Machine",               getMachineStr(getMachine()));
  PRINT_LINT("Version",               getVersionStr(getVersion()));
  PRINT_LINT("Entry Address",         getEntryAddress());
  PRINT_LINT("Program Header Offset", getProgramHeaderTableOffset());
  PRINT_LINT("Section Header Offset", getSectionHeaderTableOffset());
  PRINT_LINT("Flags",                 getFlags());
  PRINT_LINT("ELF Header Size",       getELFHeaderSize());
  PRINT_LINT("Program Header Size",   getProgramHeaderEntrySize());
  PRINT_LINT("Program Header Num",    getProgramHeaderNum());
  PRINT_LINT("Section Header Size",   getSectionHeaderEntrySize());
  PRINT_LINT("Section Header Num",    getSectionHeaderNum());
  PRINT_LINT("String Section Index",  getStringSectionIndex());
#undef PRINT_LINT

  out() << fillformat('=', 79) << "\n\n";
}

#endif // ELF_HEADER_HXX
