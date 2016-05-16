/*
 * Copyright 2010, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _FRAMEWORKS_COMPILE_SLANG_SLANG_H_  // NOLINT
#define _FRAMEWORKS_COMPILE_SLANG_SLANG_H_

#include <cstdio>
#include <string>
#include <vector>

// Terrible workaround for TargetOptions.h not using llvm::RefCountedBase!
#include "llvm/ADT/IntrusiveRefCntPtr.h"
using llvm::RefCountedBase;

#include "clang/Basic/TargetOptions.h"
#include "clang/Lex/ModuleLoader.h"

#include "llvm/ADT/OwningPtr.h"
#include "llvm/ADT/StringRef.h"

#include "llvm/Target/TargetMachine.h"

#include "slang_diagnostic_buffer.h"
#include "slang_pragma_recorder.h"

namespace llvm {
  class tool_output_file;
}

namespace clang {
  class ASTConsumer;
  class ASTContext;
  class Backend;
  class CodeGenOptions;
  class Diagnostic;
  class DiagnosticsEngine;
  class FileManager;
  class FileSystemOptions;
  class LangOptions;
  class Preprocessor;
  class SourceManager;
  class TargetInfo;
}  // namespace clang

namespace slang {

class Slang : public clang::ModuleLoader {
  static clang::LangOptions LangOpts;
  static clang::CodeGenOptions CodeGenOpts;

  static bool GlobalInitialized;

  static void LLVMErrorHandler(void *UserData, const std::string &Message,
                               bool GenCrashDialog);

 public:
  enum OutputType {
    OT_Dependency,
    OT_Assembly,
    OT_LLVMAssembly,
    OT_Bitcode,
    OT_Nothing,
    OT_Object,

    OT_Default = OT_Bitcode
  };

 private:
  bool mInitialized;

  // Diagnostics Mediator (An interface for both Producer and Consumer)
  llvm::OwningPtr<clang::Diagnostic> mDiag;

  // Diagnostics Engine (Producer and Diagnostics Reporter)
  clang::DiagnosticsEngine *mDiagEngine;

  // Diagnostics Consumer
  // NOTE: The ownership is taken by mDiagEngine after creation.
  DiagnosticBuffer *mDiagClient;

  // The target being compiled for
  llvm::IntrusiveRefCntPtr<clang::TargetOptions> mTargetOpts;
  llvm::OwningPtr<clang::TargetInfo> mTarget;
  void createTarget(std::string const &Triple, std::string const &CPU,
                    std::vector<std::string> const &Features);


  // File manager (for prepocessor doing the job such as header file search)
  llvm::OwningPtr<clang::FileManager> mFileMgr;
  llvm::OwningPtr<clang::FileSystemOptions> mFileSysOpt;
  void createFileManager();


  // Source manager (responsible for the source code handling)
  llvm::OwningPtr<clang::SourceManager> mSourceMgr;
  void createSourceManager();


  // Preprocessor (source code preprocessor)
  llvm::OwningPtr<clang::Preprocessor> mPP;
  void createPreprocessor();


  // AST context (the context to hold long-lived AST nodes)
  llvm::OwningPtr<clang::ASTContext> mASTContext;
  void createASTContext();


  // AST consumer, responsible for code generation
  llvm::OwningPtr<clang::ASTConsumer> mBackend;


  // File names
  std::string mInputFileName;
  std::string mOutputFileName;

  std::string mDepOutputFileName;
  std::string mDepTargetBCFileName;
  std::vector<std::string> mAdditionalDepTargets;
  std::vector<std::string> mGeneratedFileNames;

  OutputType mOT;

  // Output stream
  llvm::OwningPtr<llvm::tool_output_file> mOS;

  // Dependency output stream
  llvm::OwningPtr<llvm::tool_output_file> mDOS;

  std::vector<std::string> mIncludePaths;

 protected:
  PragmaList mPragmas;

  clang::DiagnosticsEngine &getDiagnostics() { return *mDiagEngine; }
  clang::TargetInfo const &getTargetInfo() const { return *mTarget; }
  clang::FileManager &getFileManager() { return *mFileMgr; }
  clang::SourceManager &getSourceManager() { return *mSourceMgr; }
  clang::Preprocessor &getPreprocessor() { return *mPP; }
  clang::ASTContext &getASTContext() { return *mASTContext; }

  inline clang::TargetOptions const &getTargetOptions() const
    { return *mTargetOpts.getPtr(); }

  virtual void initDiagnostic() {}
  virtual void initPreprocessor() {}
  virtual void initASTContext() {}

  virtual clang::ASTConsumer *
    createBackend(const clang::CodeGenOptions& CodeGenOpts,
                  llvm::raw_ostream *OS,
                  OutputType OT);

 public:
  static const llvm::StringRef PragmaMetadataName;

  static void GlobalInitialization();

  Slang();

  void init(const std::string &Triple, const std::string &CPU,
            const std::vector<std::string> &Features,
            clang::DiagnosticsEngine *DiagEngine,
            DiagnosticBuffer *DiagClient);

  virtual clang::ModuleLoadResult loadModule(
      clang::SourceLocation ImportLoc,
      clang::ModuleIdPath Path,
      clang::Module::NameVisibilityKind VK,
      bool IsInclusionDirective);

  bool setInputSource(llvm::StringRef InputFile, const char *Text,
                      size_t TextLength);

  bool setInputSource(llvm::StringRef InputFile);

  std::string const &getInputFileName() const { return mInputFileName; }

  void setIncludePaths(const std::vector<std::string> &IncludePaths) {
    mIncludePaths = IncludePaths;
  }

  void setOutputType(OutputType OT) { mOT = OT; }

  bool setOutput(const char *OutputFile);

  std::string const &getOutputFileName() const {
    return mOutputFileName;
  }

  bool setDepOutput(const char *OutputFile);

  void setDepTargetBC(const char *TargetBCFile) {
    mDepTargetBCFileName = TargetBCFile;
  }

  void setAdditionalDepTargets(
      std::vector<std::string> const &AdditionalDepTargets) {
    mAdditionalDepTargets = AdditionalDepTargets;
  }

  void appendGeneratedFileName(std::string const &GeneratedFileName) {
    mGeneratedFileNames.push_back(GeneratedFileName);
  }

  int generateDepFile();

  int compile();

  char const *getErrorMessage() { return mDiagClient->str().c_str(); }

  void setDebugMetadataEmission(bool EmitDebug);

  void setOptimizationLevel(llvm::CodeGenOpt::Level OptimizationLevel);

  // Reset the slang compiler state such that it can be reused to compile
  // another file
  virtual void reset();

  virtual ~Slang();
};

}  // namespace slang

#endif  // _FRAMEWORKS_COMPILE_SLANG_SLANG_H_  NOLINT
