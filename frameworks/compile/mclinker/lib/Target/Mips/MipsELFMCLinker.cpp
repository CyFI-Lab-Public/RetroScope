//===- MipsELFMCLinker.cpp ------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include "MipsELFMCLinker.h"
#include <mcld/LinkerConfig.h>

using namespace mcld;

MipsELFMCLinker::MipsELFMCLinker(LinkerConfig& pConfig,
                                 mcld::Module& pModule,
                                 MemoryArea& pOutput)
  : ELFMCLinker(pConfig, pModule, pOutput) {
}

MipsELFMCLinker::~MipsELFMCLinker()
{
}

