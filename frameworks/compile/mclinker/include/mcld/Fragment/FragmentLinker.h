//===- FragmentLinker.h ---------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
//
// This file provides a number of APIs used by SectLinker.
// These APIs do the things which a linker should do.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_FRAGMENT_FRAGMENT_LINKER_H
#define MCLD_FRAGMENT_FRAGMENT_LINKER_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif

#include <string>

#include <mcld/LinkerConfig.h>
#include <mcld/LD/LDFileFormat.h>
#include <mcld/LD/LDSymbol.h>
#include <mcld/Fragment/Relocation.h>
#include <mcld/MC/MCLDInput.h>

namespace mcld {

class Module;
class TargetLDBackend;
class LinkerConfig;
class MemoryArea;

/** \class FragmentLinker
 *  \brief FragmentLinker provides a pass to link object files.
 */
class FragmentLinker
{
public:
  FragmentLinker(const LinkerConfig& pConfig,
                 Module& pModule,
                 TargetLDBackend& pBackend);

  ~FragmentLinker();

  bool finalizeSymbols();

  /// applyRelocations - apply all relocation enties.
  bool applyRelocations();

  /// syncRelocationResult - After applying relocation, write back relocation target
  /// data to output file.
  void syncRelocationResult(MemoryArea& pOutput);

private:
  /// normalSyncRelocationResult - sync relocation result when producing shared
  /// objects or executables
  void normalSyncRelocationResult(MemoryArea& pOutput);

  /// partialSyncRelocationResult - sync relocation result when doing partial
  /// link
  void partialSyncRelocationResult(MemoryArea& pOutput);

  /// writeRelocationResult - helper function of syncRelocationResult, write
  /// relocation target data to output
  void writeRelocationResult(Relocation& pReloc, uint8_t* pOutput);

private:
  const LinkerConfig& m_Config;
  Module& m_Module;
  TargetLDBackend& m_Backend;
};

} // namespace of mcld

#endif

