//===- TargetSelect.h -----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_TARGET_SELECT_H
#define MCLD_TARGET_SELECT_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

extern "C" {
  // Declare all of the target-initialization functions that are available.
#define MCLD_TARGET(TargetName) void MCLDInitialize##TargetName##LDTargetInfo();
#include "mcld/Config/Targets.def"

  // Declare all of the target-dependent functions that are available.
#define MCLD_TARGET(TargetName) void MCLDInitialize##TargetName##LDTarget();
#include "mcld/Config/Targets.def"

  // Declare all of the target-depedent linker information
#define MCLD_LINKER(TargetName) void MCLDInitialize##TargetName##LDInfo();
#include "mcld/Config/Linkers.def"

  // Declare all of the available linker environment.
#define MCLD_LINKER(TargetName) void MCLDInitialize##TargetName##MCLinker();
#include "mcld/Config/Linkers.def"

  // Declare all of the available emulators.
#define MCLD_TARGET(TargetName) void MCLDInitialize##TargetName##Emulation();
#include "mcld/Config/Targets.def"

  // Declare all of the available target-specific linker
#define MCLD_LINKER(TargetName) void MCLDInitialize##TargetName##LDBackend();
#include "mcld/Config/Linkers.def"

  // Declare all of the available target-specific diagnostic line infomation
#define MCLD_LINKER(TargetName) void MCLDInitialize##TargetName##DiagnosticLineInfo();
#include "mcld/Config/Linkers.def"

} // extern "C"

namespace mcld
{
  /// InitializeAllTargetInfos - The main program should call this function if
  /// it wants access to all available targets that MCLD is configured to
  /// support, to make them available via the TargetRegistry.
  ///
  /// It is legal for a client to make multiple calls to this function.
  inline void InitializeAllTargetInfos() {
#define MCLD_TARGET(TargetName) MCLDInitialize##TargetName##LDTargetInfo();
#include "mcld/Config/Targets.def"
  }

  /// InitializeAllTargets - The main program should call this function if it
  /// wants access to all available target machines that MCLD is configured to
  /// support, to make them available via the TargetRegistry.
  ///
  /// It is legal for a client to make multiple calls to this function.
  inline void InitializeAllTargets() {
    mcld::InitializeAllTargetInfos();

#define MCLD_TARGET(TargetName) MCLDInitialize##TargetName##LDBackend();
#include "mcld/Config/Targets.def"
  }

  /// InitializeAllEmulations - The main program should call this function if
  /// it wants all emulations to be configured to support. This function makes
  /// all emulations available via the TargetRegistry.
  inline void InitializeAllEmulations() {
#define MCLD_TARGET(TargetName) MCLDInitialize##TargetName##Emulation();
#include "mcld/Config/Targets.def"
  }

  /// InitializeAllLinkers - The main program should call this function if it
  /// wants all linkers that is configured to support, to make them
  /// available via the TargetRegistry.
  ///
  /// It is legal for a client to make multiple calls to this function.
  inline void InitializeAllLinkers() {
#define MCLD_TARGET(TargetName) MCLDInitialize##TargetName##LDTarget();
#include "mcld/Config/Targets.def"

#define MCLD_LINKER(TargetName) MCLDInitialize##TargetName##MCLinker();
#include "mcld/Config/Linkers.def"
  }

  /// InitializeMsgHandler - The main program should call this function if it
  /// wants to print linker-specific messages. To make them available via the
  /// TargetRegistry.
  inline void InitializeAllDiagnostics() {
#define MCLD_LINKER(TargetName)  MCLDInitialize##TargetName##DiagnosticLineInfo();
#include "mcld/Config/Linkers.def"
  }

} // namespace of mcld

#endif

