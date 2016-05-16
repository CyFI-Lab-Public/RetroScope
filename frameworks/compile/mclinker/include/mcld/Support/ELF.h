//===- ELF.h --------------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_SUPPORT_ELF_H
#define MCLD_SUPPORT_ELF_H

namespace mcld {
namespace ELF {

// Section flags
enum SHF {
  // Indicates this section requires ordering in relation to
  // other sections of the same type.
  SHF_ORDERED = 0x40000000,

  // This section is excluded from input of an executable or shared object.
  // Ignore this flag if SHF_ALLOC is also set or if a relocation refers to
  // the section
  SHF_EXCLUDE = 0x80000000,

  // Section with data that is GP relative addressable.
  SHF_MIPS_GPREL = 0x10000000
}; // enum SHF

} // namespace of ELF
} // namespace of mcld

#endif

