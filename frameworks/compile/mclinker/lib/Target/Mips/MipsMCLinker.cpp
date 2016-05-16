//===- MipsMCLinker.cpp ---------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "MipsELFMCLinker.h"

#include "Mips.h"
#include <llvm/ADT/Triple.h>
#include <mcld/Module.h>
#include <mcld/Support/TargetRegistry.h>

using namespace mcld;

namespace mcld {
//===----------------------------------------------------------------------===//
/// createMipsMCLinker - the help funtion to create
//===----------------------------------------------------------------------===//
/// corresponding MipsMCLinker
///
MCLinker* createMipsMCLinker(const std::string &pTriple,
                             LinkerConfig& pConfig,
                             mcld::Module& pModule,
                             MemoryArea& pOutput)
{
  llvm::Triple theTriple(pTriple);
  if (theTriple.isOSDarwin()) {
    assert(0 && "MachO linker has not supported yet");
    return NULL;
  }
  if (theTriple.isOSWindows()) {
    assert(0 && "COFF linker has not supported yet");
    return NULL;
  }

  return new MipsELFMCLinker(pConfig, pModule, pOutput);
}

} // namespace of mcld

//===----------------------------------------------------------------------===//
// MipsMCLinker
//===----------------------------------------------------------------------===//
extern "C" void MCLDInitializeMipsMCLinker() {
  // Register the linker frontend
  mcld::TargetRegistry::RegisterMCLinker(TheMipselTarget, createMipsMCLinker);
}
