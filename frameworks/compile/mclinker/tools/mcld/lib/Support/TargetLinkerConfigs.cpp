//===- TargetLinkerConfigs.cpp --------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//

#include "alone/Config/Config.h"
#include "alone/Support/TargetLinkerConfigs.h"

#include <mcld/TargetOptions.h>
#include <mcld/MC/InputFactory.h>
#include <mcld/Fragment/Relocation.h>

using namespace alone;

#ifdef TARGET_BUILD
static const char* gDefaultDyld = "/system/bin/linker";
static const char* gDefaultSysroot = "/system";
#else
static const char* gDefaultDyld = "/usr/lib/ld.so.1";
static const char* gDefaultSysroot = "/";
#endif

//===----------------------------------------------------------------------===//
// ARM
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_ARM_CODEGEN)
ARMLinkerConfig::ARMLinkerConfig() : LinkerConfig(DEFAULT_ARM_TRIPLE_STRING) {

  // set up target-dependent options
  getLDConfig()->targets().setEndian(mcld::TargetOptions::Little);
  getLDConfig()->targets().setBitClass(32);

  // set up target-dependent constraints of attributes
  getLDConfig()->attribute().constraint().enableWholeArchive();
  getLDConfig()->attribute().constraint().disableAsNeeded();
  getLDConfig()->attribute().constraint().setSharedSystem();

  // set up the predefined attributes
  getLDConfig()->attribute().predefined().unsetWholeArchive();
  getLDConfig()->attribute().predefined().setDynamic();

  // set up target dependent options
  if (getLDScript()->sysroot().empty()) {
    getLDScript()->setSysroot(gDefaultSysroot);
  }

  if (!getLDConfig()->options().hasDyld()) {
    getLDConfig()->options().setDyld(gDefaultDyld);
  }

  // set up section map
  if (getLDConfig()->codeGenType() != mcld::LinkerConfig::Object) {
    bool exist = false;
    getLDScript()->sectionMap().append(".ARM.exidx", ".ARM.exidx", exist);
    getLDScript()->sectionMap().append(".ARM.extab", ".ARM.extab", exist);
    getLDScript()->sectionMap().append(".ARM.attributes", ".ARM.attributes", exist);
  }

  // set up relocation factory
  mcld::Relocation::SetUp(*getLDConfig());
}
#endif // defined(PROVIDE_ARM_CODEGEN)

//===----------------------------------------------------------------------===//
// Mips
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_MIPS_CODEGEN)
MipsLinkerConfig::MipsLinkerConfig()
  : LinkerConfig(DEFAULT_MIPS_TRIPLE_STRING) {

  // set up target-dependent options
  getLDConfig()->targets().setEndian(mcld::TargetOptions::Little);
  getLDConfig()->targets().setBitClass(32);

  // set up target-dependent constraints of attibutes
  getLDConfig()->attribute().constraint().enableWholeArchive();
  getLDConfig()->attribute().constraint().disableAsNeeded();
  getLDConfig()->attribute().constraint().setSharedSystem();

  // set up the predefined attributes
  getLDConfig()->attribute().predefined().unsetWholeArchive();
  getLDConfig()->attribute().predefined().setDynamic();

  // set up target dependent options
  if (getLDScript()->sysroot().empty()) {
    getLDScript()->setSysroot(gDefaultSysroot);
  }

  if (!getLDConfig()->options().hasDyld()) {
    getLDConfig()->options().setDyld(gDefaultDyld);
  }

  // set up relocation factory
  mcld::Relocation::SetUp(*getLDConfig());
}
#endif // defined(PROVIDE_MIPS_CODEGEN)

//===----------------------------------------------------------------------===//
// X86 and X86_64
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_X86_CODEGEN)
X86FamilyLinkerConfigBase::X86FamilyLinkerConfigBase(const std::string& pTriple)
  : LinkerConfig(pTriple) {
  // set up target-dependent options
  getLDConfig()->targets().setEndian(mcld::TargetOptions::Little);
  getLDConfig()->targets().setBitClass(32);

  // set up target-dependent constraints of attibutes
  getLDConfig()->attribute().constraint().enableWholeArchive();
  getLDConfig()->attribute().constraint().disableAsNeeded();
  getLDConfig()->attribute().constraint().setSharedSystem();

  // set up the predefined attributes
  getLDConfig()->attribute().predefined().unsetWholeArchive();
  getLDConfig()->attribute().predefined().setDynamic();

  // set up target dependent options
  if (getLDScript()->sysroot().empty()) {
    getLDScript()->setSysroot(gDefaultSysroot);
  }

  if (!getLDConfig()->options().hasDyld()) {
    getLDConfig()->options().setDyld(gDefaultDyld);
  }

  // set up relocation factory
  mcld::Relocation::SetUp(*getLDConfig());
}

X86_32LinkerConfig::X86_32LinkerConfig()
  : X86FamilyLinkerConfigBase(DEFAULT_X86_TRIPLE_STRING) {
}

X86_64LinkerConfig::X86_64LinkerConfig()
  : X86FamilyLinkerConfigBase(DEFAULT_X86_64_TRIPLE_STRING) {
}
#endif // defined(PROVIDE_X86_CODEGEN)

#if !defined(TARGET_BUILD)
//===----------------------------------------------------------------------===//
// General
//===----------------------------------------------------------------------===//
GeneralLinkerConfig::GeneralLinkerConfig(const std::string& pTriple)
  : LinkerConfig(pTriple) {

  // set up target-dependent options
  getLDConfig()->targets().setEndian(mcld::TargetOptions::Little);
  getLDConfig()->targets().setBitClass(32);

  // set up target-dependent constraints of attributes
  getLDConfig()->attribute().constraint().enableWholeArchive();
  getLDConfig()->attribute().constraint().disableAsNeeded();
  getLDConfig()->attribute().constraint().setSharedSystem();

  // set up the predefined attributes
  getLDConfig()->attribute().predefined().unsetWholeArchive();
  getLDConfig()->attribute().predefined().setDynamic();

  // set up section map
  if (llvm::Triple::arm == getLDConfig()->targets().triple().getArch() &&
      getLDConfig()->codeGenType() != mcld::LinkerConfig::Object) {
    bool exist = false;
    getLDScript()->sectionMap().append(".ARM.exidx", ".ARM.exidx", exist);
    getLDScript()->sectionMap().append(".ARM.extab", ".ARM.extab", exist);
    getLDScript()->sectionMap().append(".ARM.attributes", ".ARM.attributes", exist);
  }

  // set up relocation factory
  mcld::Relocation::SetUp(*getLDConfig());
}
#endif // defined(TARGET_BUILD)
