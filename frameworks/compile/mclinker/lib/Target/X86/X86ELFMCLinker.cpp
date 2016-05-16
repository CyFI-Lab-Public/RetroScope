//===- X86ELFMCLinker.cpp -------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "X86ELFMCLinker.h"
#include <mcld/LinkerConfig.h>

using namespace mcld;

X86ELFMCLinker::X86ELFMCLinker(LinkerConfig& pConfig,
                               mcld::Module& pModule,
                               MemoryArea& pOutput)
  : ELFMCLinker(pConfig, pModule, pOutput) {
}

X86ELFMCLinker::~X86ELFMCLinker()
{
}

