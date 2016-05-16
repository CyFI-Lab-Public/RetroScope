//===- X86MCLinker.cpp ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "X86.h"
#include "X86ELFMCLinker.h"
#include <mcld/Module.h>
#include <mcld/Support/TargetRegistry.h>
#include <llvm/ADT/Triple.h>

using namespace mcld;

namespace mcld {

//===----------------------------------------------------------------------===//
/// createX86MCLinker - the help funtion to create corresponding X86MCLinker
//===----------------------------------------------------------------------===//
MCLinker* createX86MCLinker(const std::string &pTriple,
                            LinkerConfig& pConfig,
                            mcld::Module& pModule,
                            MemoryArea& pOutput)
{
  Triple theTriple(pTriple);
  if (theTriple.isOSDarwin()) {
    assert(0 && "MachO linker has not supported yet");
    return NULL;
  }
  if (theTriple.isOSWindows()) {
    assert(0 && "COFF linker has not supported yet");
    return NULL;
  }

  return new X86ELFMCLinker(pConfig, pModule, pOutput);
}

} // namespace of mcld

//===----------------------------------------------------------------------===//
// X86MCLinker
//===----------------------------------------------------------------------===//
extern "C" void MCLDInitializeX86MCLinker() {
  // Register the linker frontend
  mcld::TargetRegistry::RegisterMCLinker(TheX86_32Target, createX86MCLinker);
  mcld::TargetRegistry::RegisterMCLinker(TheX86_64Target, createX86MCLinker);
}

