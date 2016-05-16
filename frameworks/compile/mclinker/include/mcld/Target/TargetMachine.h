//===- TargetMachine.h ----------------------------------------------------===//
//
//                     The MCLinker Project
//
// This file is distributed under the University of Illinois Open Source
// License. See LICENSE.TXT for details.
//
//===----------------------------------------------------------------------===//
#ifndef MCLD_TARGET_TARGET_MACHINE_H
#define MCLD_TARGET_TARGET_MACHINE_H
#ifdef ENABLE_UNITTEST
#include <gtest.h>
#endif
#include <llvm/Target/TargetMachine.h>
#include <string>

namespace llvm {

class Target;
class TargetData;
class TargetMachine;
class PassManagerBase;

} // namespace of llvm

namespace mcld {

class Module;
class Target;
class MemoryArea;
class LinkerConfig;
class ToolOutputFile;

using namespace llvm;

enum CodeGenFileType {
  CGFT_ASMFile,
  CGFT_OBJFile,
  CGFT_DSOFile,
  CGFT_EXEFile,
  CGFT_PARTIAL,
  CGFT_BINARY,
  CGFT_NULLFile
};


/** \class mcld::MCLDTargetMachine
 *  \brief mcld::MCLDTargetMachine is a object adapter of LLVMTargetMachine.
 */
class MCLDTargetMachine
{
public:
  /// Adapter of llvm::TargetMachine
  ///
  MCLDTargetMachine(llvm::TargetMachine &pTM,
                    const mcld::Target &pTarget,
                    const std::string &pTriple);

  virtual ~MCLDTargetMachine();

  /// getTarget - adapt llvm::TargetMachine::getTarget
  const mcld::Target& getTarget() const;

  /// getTM - return adapted the llvm::TargetMachine.
  const llvm::TargetMachine& getTM() const { return m_TM; }
  llvm::TargetMachine&       getTM()       { return m_TM; }

  /// appPassesToEmitFile - The target function which we has to modify as
  /// upstreaming.
  bool addPassesToEmitFile(PassManagerBase &,
                           mcld::ToolOutputFile& pOutput,
                           mcld::CodeGenFileType,
                           CodeGenOpt::Level,
                           mcld::Module& pModule,
                           mcld::LinkerConfig& pConfig,
                           bool DisableVerify = true);

  /// getDataLayout
  const DataLayout *getDataLayout() const { return m_TM.getDataLayout(); }

  /// setAsmVerbosityDefault
  static void setAsmVerbosityDefault(bool pAsmVerbose) {
    llvm::TargetMachine::setAsmVerbosityDefault(pAsmVerbose);
  }

private:
  /// addCommonCodeGenPasses - Add standard LLVM codegen passes used for
  /// both emitting to assembly files or machine code output.
  bool addCommonCodeGenPasses(PassManagerBase &,
                              mcld::CodeGenFileType,
                              CodeGenOpt::Level,
                              bool DisableVerify,
                              llvm::MCContext *&OutCtx);

  bool addCompilerPasses(PassManagerBase &pPM,
                         llvm::formatted_raw_ostream &pOutput,
                         llvm::MCContext *&OutCtx);

  bool addAssemblerPasses(PassManagerBase &pPM,
                          llvm::raw_ostream &pOutput,
                          llvm::MCContext *&OutCtx);

  bool addLinkerPasses(PassManagerBase &pPM,
                       LinkerConfig& pConfig,
                       Module& pModule,
                       mcld::MemoryArea& pOutput,
                       llvm::MCContext *&OutCtx);

private:
  llvm::TargetMachine &m_TM;
  const mcld::Target *m_pTarget;
  const std::string& m_Triple;
};

} // namespace of mcld

#endif

