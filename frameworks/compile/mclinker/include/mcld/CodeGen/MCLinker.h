//===- MCLinker.h ---------------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
//
// MCLinker is a base class inherited by target specific linker.
// This class primarily handles common functionality used by all linkers.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_CODEGEN_MCLINKER_H
#define MCLD_CODEGEN_MCLINKER_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <llvm/CodeGen/MachineFunctionPass.h>

namespace llvm {

class Module;
class MachineFunction;

} // namespace of llvm

namespace mcld {

class Module;
class MemoryArea;
class IRBuilder;
class LinkerConfig;
class Linker;

/** \class MCLinker
*  \brief MCLinker provides a linking pass for standard compilation flow
*
*  MCLinker is responded for
*  - provide an interface for target-specific linker
*  - set up environment for ObjectLinker
*  - perform linking
*
*  @see MachineFunctionPass ObjectLinker
*/
class MCLinker : public llvm::MachineFunctionPass
{
protected:
  // Constructor. Although MCLinker has only two arguments,
  // TargetMCLinker should handle
  // - enabled attributes
  // - the default attribute
  // - the default link script
  // - the standard symbols
  MCLinker(LinkerConfig& pConfig,
           mcld::Module& pModule,
           MemoryArea& pOutput);

public:
  virtual ~MCLinker();

  virtual bool doInitialization(llvm::Module &pM);

  virtual bool doFinalization(llvm::Module &pM);

  virtual bool runOnMachineFunction(llvm::MachineFunction& pMFn);

protected:
  void initializeInputTree(IRBuilder& pBuilder);

protected:
  LinkerConfig& m_Config;
  mcld::Module& m_Module;
  MemoryArea& m_Output;
  IRBuilder* m_pBuilder;
  Linker* m_pLinker;

private:
  static char m_ID;
};

} // namespace of MC Linker

#endif

