//===- HexagonMCLinker.cpp ------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "Hexagon.h"
#include "HexagonELFMCLinker.h"
#include <mcld/Module.h>
#include <mcld/Support/TargetRegistry.h>
#include <llvm/ADT/Triple.h>

using namespace mcld;

namespace mcld {

/// createHexagonMCLinker - the help funtion to create corresponding
/// HexagonMCLinker
MCLinker* createHexagonMCLinker(const std::string &pTriple,
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

  if (theTriple.isArch32Bit())
    return new HexagonELFMCLinker(pConfig, pModule, pOutput);

  assert(0 && "Hexagon_64 has not supported yet");
  return NULL;
}

} // namespace of mcld

//===----------------------------------------------------------------------===//
// HexagonMCLinker
//===----------------------------------------------------------------------===//
extern "C" void MCLDInitializeHexagonMCLinker() {
  // Register the linker frontend
  mcld::TargetRegistry::RegisterMCLinker(TheHexagonTarget,
                                         createHexagonMCLinker);
}

