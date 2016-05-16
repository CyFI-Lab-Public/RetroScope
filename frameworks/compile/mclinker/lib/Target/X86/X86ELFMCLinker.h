//===- X86ELFMCLinker.h ---------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef X86_ELFSECTLINKER_H
#define X86_ELFSECTLINKER_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <mcld/Target/ELFMCLinker.h>

namespace mcld {

class Module;
class MemoryArea;

/** \class X86ELFMCLinker
 *  \brief X86ELFMCLinker sets up the environment for linking.
 *
 *  \see
 */
class X86ELFMCLinker : public ELFMCLinker
{
public:
  X86ELFMCLinker(LinkerConfig& pConfig,
                 mcld::Module& pModule,
                 MemoryArea& pOutput);

  ~X86ELFMCLinker();
};

} // namespace of mcld

#endif

