//===- ELFMCLinker.cpp ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#include <mcld/Target/ELFMCLinker.h>
#include <mcld/LinkerConfig.h>
#include <mcld/Object/SectionMap.h>
#include <mcld/Support/MsgHandling.h>

using namespace mcld;

//===----------------------------------------------------------------------===//
// ELFMCLinker
//===----------------------------------------------------------------------===//
ELFMCLinker::ELFMCLinker(LinkerConfig& pConfig,
                         mcld::Module& pModule,
                         MemoryArea& pOutput)
  : MCLinker(pConfig, pModule, pOutput) {

}

ELFMCLinker::~ELFMCLinker()
{
  // MCLinker will delete m_pLDBackend and m_pLDDriver;
}

