//===- TargetLinkerConfig.h -----------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//

#ifndef ALONE_SUPPORT_TARGET_LINKER_CONFIGS_H
#define ALONE_SUPPORT_TARGET_LINKER_CONFIGS_H

#include <string>

#include "alone/Config/Config.h"
#include "alone/Support/LinkerConfig.h"

namespace alone {

//===----------------------------------------------------------------------===//
// ARM
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_ARM_CODEGEN)
class ARMLinkerConfig : public LinkerConfig {
public:
  ARMLinkerConfig();
};
#endif // defined(PROVIDE_ARM_CODEGEN)

//===----------------------------------------------------------------------===//
// MIPS
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_MIPS_CODEGEN)
class MipsLinkerConfig : public LinkerConfig {
public:
  MipsLinkerConfig();
};
#endif // defined(PROVIDE_MIPS_CODEGEN)

//===----------------------------------------------------------------------===//
// X86 and X86_64
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_X86_CODEGEN)
class X86FamilyLinkerConfigBase : public LinkerConfig {
public:
  X86FamilyLinkerConfigBase(const std::string& pTriple);
};

class X86_32LinkerConfig : public X86FamilyLinkerConfigBase {
public:
  X86_32LinkerConfig();
};

class X86_64LinkerConfig : public X86FamilyLinkerConfigBase {
public:
  X86_64LinkerConfig();
};
#endif // defined(PROVIDE_X86_CODEGEN)

//===----------------------------------------------------------------------===//
// Default target
//===----------------------------------------------------------------------===//
class DefaultLinkerConfig : public
#if defined (DEFAULT_ARM_CODEGEN)
  ARMLinkerConfig
#elif defined (DEFAULT_MIPS_CODEGEN)
  MipsLinkerConfig
#elif defined (DEFAULT_X86_CODEGEN)
  X86_32LinkerConfig
#elif defined (DEFAULT_X86_64_CODEGEN)
  X86_64LinkerConfig
#else
#  error "Unsupported Default Target!"
#endif
{ };

#if !defined(TARGET_BUILD)
//===----------------------------------------------------------------------===//
// General target
//===----------------------------------------------------------------------===//
class GeneralLinkerConfig : public LinkerConfig {
public:
  GeneralLinkerConfig(const std::string& pTriple);
};
#endif // !defined(TARGET_BUILD)

} // end namespace alone

#endif // ALONE_SUPPORT_LINKER_CONFIG_H
