//===- ARMEmulation.cpp ---------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "ARM.h"
#include <mcld/LinkerConfig.h>
#include <mcld/LinkerScript.h>
#include <mcld/Target/ELFEmulation.h>
#include <mcld/Support/TargetRegistry.h>

namespace mcld {

static bool MCLDEmulateARMELF(LinkerScript& pScript, LinkerConfig& pConfig)
{
  if (!MCLDEmulateELF(pScript, pConfig))
    return false;

  // set up bitclass and endian
  pConfig.targets().setEndian(TargetOptions::Little);
  pConfig.targets().setBitClass(32);

  // set up target-dependent constraints of attributes
  pConfig.attribute().constraint().enableWholeArchive();
  pConfig.attribute().constraint().enableAsNeeded();
  pConfig.attribute().constraint().setSharedSystem();

  // set up the predefined attributes
  pConfig.attribute().predefined().unsetWholeArchive();
  pConfig.attribute().predefined().unsetAsNeeded();
  pConfig.attribute().predefined().setDynamic();

  // set up section map
  if (pConfig.codeGenType() != LinkerConfig::Object) {
    bool exist = false;
    pScript.sectionMap().append(".ARM.exidx", ".ARM.exidx", exist);
    pScript.sectionMap().append(".ARM.extab", ".ARM.extab", exist);
    pScript.sectionMap().append(".ARM.attributes", ".ARM.attributes", exist);
  }
  return true;
}

//===----------------------------------------------------------------------===//
// emulateARMLD - the help function to emulate ARM ld
//===----------------------------------------------------------------------===//
bool emulateARMLD(LinkerScript& pScript, LinkerConfig& pConfig)
{
  if (pConfig.targets().triple().isOSDarwin()) {
    assert(0 && "MachO linker has not supported yet");
    return false;
  }
  if (pConfig.targets().triple().isOSWindows()) {
    assert(0 && "COFF linker has not supported yet");
    return false;
  }

  return MCLDEmulateARMELF(pScript, pConfig);
}

} // namespace of mcld

//===----------------------------------------------------------------------===//
// ARMEmulation
//===----------------------------------------------------------------------===//
extern "C" void MCLDInitializeARMEmulation() {
  // Register the emulation
  mcld::TargetRegistry::RegisterEmulation(mcld::TheARMTarget, mcld::emulateARMLD);
  mcld::TargetRegistry::RegisterEmulation(mcld::TheThumbTarget, mcld::emulateARMLD);
}

