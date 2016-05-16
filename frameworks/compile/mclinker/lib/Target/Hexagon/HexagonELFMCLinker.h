//===- HexagonELFMCLinker.h -----------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef HEXAGON_ELFSECTLINKER_H
#define HEXAGON_ELFSECTLINKER_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <mcld/Target/ELFMCLinker.h>

namespace mcld {

class Module;
class MemoryArea;

/** \class HexagonELFMCLinker
 *  \brief HexagonELFMCLinker sets up the environment for linking.
 *
 *  \see
 */
class HexagonELFMCLinker : public ELFMCLinker
{
public:
  HexagonELFMCLinker(LinkerConfig& pConfig,
                     mcld::Module& pModule,
                     MemoryArea& pOutput);

  ~HexagonELFMCLinker();
};

} // namespace of mcld

#endif
