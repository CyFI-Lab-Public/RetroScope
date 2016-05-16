//===- ELFEmulation.h -----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_ELF_EMULATION_H
#define MCLD_ELF_EMULATION_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

namespace mcld {

class LinkerConfig;
class LinkerScript;

bool MCLDEmulateELF(LinkerScript& pScript, LinkerConfig& pConfig);

} // namespace of mcld

#endif

