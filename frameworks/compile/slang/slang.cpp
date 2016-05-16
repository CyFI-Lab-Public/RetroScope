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

#include "slang.h"

#include <stdlib.h>

#include <string>
#include <vector>

#include "clang/AST/ASTConsumer.h"
#include "clang/AST/ASTContext.h"

#include "clang/Basic/DiagnosticIDs.h"
#include "clang/Basic/DiagnosticOptions.h"
#include "clang/Basic/FileManager.h"
#include "clang/Basic/FileSystemOptions.h"
#include "clang/Basic/LangOptions.h"
#include "clang/Basic/SourceManager.h"
#include "clang/Basic/TargetInfo.h"
#include "clang/Basic/TargetOptions.h"

#include "clang/Frontend/CodeGenOptions.h"
#include "clang/Frontend/DependencyOutputOptions.h"
#include "clang/Frontend/FrontendDiagnostic.h"
#include "clang/Frontend/TextDiagnosticPrinter.h"
#include "clang/Frontend/Utils.h"

#include "clang/Lex/Preprocessor.h"
#include "clang/Lex/PreprocessorOptions.h"
#include "clang/Lex/HeaderSearch.h"
#include "clang/Lex/HeaderSearchOptions.h"

#include "clang/Parse/ParseAST.h"

#include "llvm/ADT/IntrusiveRefCntPtr.h"

#include "llvm/Bitcode/ReaderWriter.h"

// More force linking
#include "llvm/Linker.h"

// Force linking all passes/vmcore stuffs to libslang.so
#include "llvm/LinkAllIR.h"
#include "llvm/LinkAllPasses.h"

#include "llvm/Support/raw_ostream.h"
#include "llvm/Support/MemoryBuffer.h"
#include "llvm/Support/ErrorHandling.h"
#include "llvm/Support/ManagedStatic.h"
#include "llvm/Support/Path.h"
#include "llvm/Support/TargetSelect.h"
#include "llvm/Support/ToolOutputFile.h"

#include "slang_assert.h"
#include "slang_backend.h"
#include "slang_utils.h"

namespace {

struct ForceSlangLinking {
  ForceSlangLinking() {
    // We must reference the functions in such a way that compilers will not
    // delete it all as dead code, even with whole program optimization,
    // yet is effectively a NO-OP. As the compiler isn't smart enough
    // to know that getenv() never returns -1, this will do the job.
    if (std::getenv("bar") != reinterpret_cast<char*>(-1))
      return;

    // llvm-rs-link needs following functions existing in libslang.
    llvm::ParseBitcodeFile(NULL, llvm::getGlobalContext(), NULL);
    llvm::Linker::LinkModules(NULL, NULL, 0, NULL);

    // llvm-rs-cc need this.
    new clang::TextDiagnosticPrinter(llvm::errs(),
                                     new clang::DiagnosticOptions());
  }
} ForceSlangLinking;

}  // namespace

namespace slang {

#if defined(__arm__)
#   define DEFAULT_TARGET_TRIPLE_STRING "armv7-none-linux-gnueabi"
#elif defined(__x86_64__)
#   define DEFAULT_TARGET_TRIPLE_STRING "x86_64-unknown-linux"
#else
// let's use x86 as default target
#   define DEFAULT_TARGET_TRIPLE_STRING "i686-unknown-linux"
#endif

bool Slang::GlobalInitialized = false;

// Language option (define the language feature for compiler such as C99)
clang::LangOptions Slang::LangOpts;

// Code generation option for the compiler
clang::CodeGenOptions Slang::CodeGenOpts;

// The named of metadata node that pragma resides (should be synced with
// bcc.cpp)
const llvm::StringRef Slang::PragmaMetadataName = "#pragma";

static inline llvm::tool_output_file *
OpenOutputFile(const char *OutputFile,
               llvm::sys::fs::OpenFlags Flags,
               std::string* Error,
               clang::DiagnosticsEngine *DiagEngine) {
  slangAssert((OutputFile != NULL) && (Error != NULL) &&
              (DiagEngine != NULL) && "Invalid parameter!");

  if (SlangUtils::CreateDirectoryWithParents(
                        llvm::sys::path::parent_path(OutputFile), Error)) {
    llvm::tool_output_file *F =
          new llvm::tool_output_file(OutputFile, *Error, Flags);
    if (F != NULL)
      return F;
  }

  // Report error here.
  DiagEngine->Report(clang::diag::err_fe_error_opening)
    << OutputFile << *Error;

  return NULL;
}

void Slang::GlobalInitialization() {
  if (!GlobalInitialized) {
    // We only support x86, x64 and ARM target

    // For ARM
    LLVMInitializeARMTargetInfo();
    LLVMInitializeARMTarget();
    LLVMInitializeARMAsmPrinter();

    // For x86 and x64
    LLVMInitializeX86TargetInfo();
    LLVMInitializeX86Target();
    LLVMInitializeX86AsmPrinter();

    // Please refer to include/clang/Basic/LangOptions.h to setup
    // the options.
    LangOpts.RTTI = 0;  // Turn off the RTTI information support
    LangOpts.C99 = 1;
    LangOpts.Renderscript = 1;
    LangOpts.LaxVectorConversions = 0;  // Do not bitcast vectors!
    LangOpts.CharIsSigned = 1;  // Signed char is our default.

    CodeGenOpts.OptimizationLevel = 3;

    GlobalInitialized = true;
  }
}

void Slang::LLVMErrorHandler(void *UserData, const std::string &Message,
                             bool GenCrashDialog) {
  clang::DiagnosticsEngine* DiagEngine =
    static_cast<clang::DiagnosticsEngine *>(UserData);

  DiagEngine->Report(clang::diag::err_fe_error_backend) << Message;
  exit(1);
}

void Slang::createTarget(const std::string &Triple, const std::string &CPU,
                         const std::vector<std::string> &Features) {
  if (!Triple.empty())
    mTargetOpts->Triple = Triple;
  else
    mTargetOpts->Triple = DEFAULT_TARGET_TRIPLE_STRING;

  if (!CPU.empty())
    mTargetOpts->CPU = CPU;

  if (!Features.empty())
    mTargetOpts->FeaturesAsWritten = Features;

  mTarget.reset(clang::TargetInfo::CreateTargetInfo(*mDiagEngine,
                                                    mTargetOpts.getPtr()));
}

void Slang::createFileManager() {
  mFileSysOpt.reset(new clang::FileSystemOptions());
  mFileMgr.reset(new clang::FileManager(*mFileSysOpt));
}

void Slang::createSourceManager() {
  mSourceMgr.reset(new clang::SourceManager(*mDiagEngine, *mFileMgr));
}

void Slang::createPreprocessor() {
  // Default only search header file in current dir
  llvm::IntrusiveRefCntPtr<clang::HeaderSearchOptions> HSOpts =
      new clang::HeaderSearchOptions();
  clang::HeaderSearch *HeaderInfo = new clang::HeaderSearch(HSOpts,
                                                            *mFileMgr,
                                                            *mDiagEngine,
                                                            LangOpts,
                                                            mTarget.get());

  llvm::IntrusiveRefCntPtr<clang::PreprocessorOptions> PPOpts =
      new clang::PreprocessorOptions();
  mPP.reset(new clang::Preprocessor(PPOpts,
                                    *mDiagEngine,
                                    LangOpts,
                                    mTarget.get(),
                                    *mSourceMgr,
                                    *HeaderInfo,
                                    *this,
                                    NULL,
                                    /* OwnsHeaderSearch = */true));
  // Initialize the preprocessor
  mPragmas.clear();
  mPP->AddPragmaHandler(new PragmaRecorder(&mPragmas));

  std::vector<clang::DirectoryLookup> SearchList;
  for (unsigned i = 0, e = mIncludePaths.size(); i != e; i++) {
    if (const clang::DirectoryEntry *DE =
            mFileMgr->getDirectory(mIncludePaths[i])) {
      SearchList.push_back(clang::DirectoryLookup(DE,
                                                  clang::SrcMgr::C_System,
                                                  false));
    }
  }

  HeaderInfo->SetSearchPaths(SearchList,
                             /* angledDirIdx = */1,
                             /* systemDixIdx = */1,
                             /* noCurDirSearch = */false);

  initPreprocessor();
}

void Slang::createASTContext() {
  mASTContext.reset(new clang::ASTContext(LangOpts,
                                          *mSourceMgr,
                                          mTarget.get(),
                                          mPP->getIdentifierTable(),
                                          mPP->getSelectorTable(),
                                          mPP->getBuiltinInfo(),
                                          /* size_reserve = */0));
  initASTContext();
}

clang::ASTConsumer *
Slang::createBackend(const clang::CodeGenOptions& CodeGenOpts,
                     llvm::raw_ostream *OS, OutputType OT) {
  return new Backend(mDiagEngine, CodeGenOpts, getTargetOptions(),
                     &mPragmas, OS, OT);
}

Slang::Slang() : mInitialized(false), mDiagClient(NULL), mOT(OT_Default) {
  mTargetOpts = new clang::TargetOptions();
  GlobalInitialization();
}

void Slang::init(const std::string &Triple, const std::string &CPU,
                 const std::vector<std::string> &Features,
                 clang::DiagnosticsEngine *DiagEngine,
                 DiagnosticBuffer *DiagClient) {
  if (mInitialized)
    return;

  mDiagEngine = DiagEngine;
  mDiagClient = DiagClient;
  mDiag.reset(new clang::Diagnostic(mDiagEngine));
  initDiagnostic();
  llvm::install_fatal_error_handler(LLVMErrorHandler, mDiagEngine);

  createTarget(Triple, CPU, Features);
  createFileManager();
  createSourceManager();

  mInitialized = true;
}

clang::ModuleLoadResult Slang::loadModule(
    clang::SourceLocation ImportLoc,
    clang::ModuleIdPath Path,
    clang::Module::NameVisibilityKind Visibility,
    bool IsInclusionDirective) {
  slangAssert(0 && "Not implemented");
  return clang::ModuleLoadResult();
}

bool Slang::setInputSource(llvm::StringRef InputFile,
                           const char *Text,
                           size_t TextLength) {
  mInputFileName = InputFile.str();

  // Reset the ID tables if we are reusing the SourceManager
  mSourceMgr->clearIDTables();

  // Load the source
  llvm::MemoryBuffer *SB =
      llvm::MemoryBuffer::getMemBuffer(Text, Text + TextLength);
  mSourceMgr->createMainFileIDForMemBuffer(SB);

  if (mSourceMgr->getMainFileID().isInvalid()) {
    mDiagEngine->Report(clang::diag::err_fe_error_reading) << InputFile;
    return false;
  }
  return true;
}

bool Slang::setInputSource(llvm::StringRef InputFile) {
  mInputFileName = InputFile.str();

  mSourceMgr->clearIDTables();

  const clang::FileEntry *File = mFileMgr->getFile(InputFile);
  if (File)
    mSourceMgr->createMainFileID(File);

  if (mSourceMgr->getMainFileID().isInvalid()) {
    mDiagEngine->Report(clang::diag::err_fe_error_reading) << InputFile;
    return false;
  }

  return true;
}

bool Slang::setOutput(const char *OutputFile) {
  std::string Error;
  llvm::tool_output_file *OS = NULL;

  switch (mOT) {
    case OT_Dependency:
    case OT_Assembly:
    case OT_LLVMAssembly: {
      OS = OpenOutputFile(OutputFile, llvm::sys::fs::F_None, &Error,
          mDiagEngine);
      break;
    }
    case OT_Nothing: {
      break;
    }
    case OT_Object:
    case OT_Bitcode: {
      OS = OpenOutputFile(OutputFile, llvm::sys::fs::F_Binary,
                          &Error, mDiagEngine);
      break;
    }
    default: {
      llvm_unreachable("Unknown compiler output type");
    }
  }

  if (!Error.empty())
    return false;

  mOS.reset(OS);

  mOutputFileName = OutputFile;

  return true;
}

bool Slang::setDepOutput(const char *OutputFile) {
  std::string Error;

  mDOS.reset(
      OpenOutputFile(OutputFile, llvm::sys::fs::F_None, &Error, mDiagEngine));
  if (!Error.empty() || (mDOS.get() == NULL))
    return false;

  mDepOutputFileName = OutputFile;

  return true;
}

int Slang::generateDepFile() {
  if (mDiagEngine->hasErrorOccurred())
    return 1;
  if (mDOS.get() == NULL)
    return 1;

  // Initialize options for generating dependency file
  clang::DependencyOutputOptions DepOpts;
  DepOpts.IncludeSystemHeaders = 1;
  DepOpts.OutputFile = mDepOutputFileName;
  DepOpts.Targets = mAdditionalDepTargets;
  DepOpts.Targets.push_back(mDepTargetBCFileName);
  for (std::vector<std::string>::const_iterator
           I = mGeneratedFileNames.begin(), E = mGeneratedFileNames.end();
       I != E;
       I++) {
    DepOpts.Targets.push_back(*I);
  }
  mGeneratedFileNames.clear();

  // Per-compilation needed initialization
  createPreprocessor();
  AttachDependencyFileGen(*mPP.get(), DepOpts);

  // Inform the diagnostic client we are processing a source file
  mDiagClient->BeginSourceFile(LangOpts, mPP.get());

  // Go through the source file (no operations necessary)
  clang::Token Tok;
  mPP->EnterMainSourceFile();
  do {
    mPP->Lex(Tok);
  } while (Tok.isNot(clang::tok::eof));

  mPP->EndSourceFile();

  // Declare success if no error
  if (!mDiagEngine->hasErrorOccurred())
    mDOS->keep();

  // Clean up after compilation
  mPP.reset();
  mDOS.reset();

  return mDiagEngine->hasErrorOccurred() ? 1 : 0;
}

int Slang::compile() {
  if (mDiagEngine->hasErrorOccurred())
    return 1;
  if (mOS.get() == NULL)
    return 1;

  // Here is per-compilation needed initialization
  createPreprocessor();
  createASTContext();

  mBackend.reset(createBackend(CodeGenOpts, &mOS->os(), mOT));

  // Inform the diagnostic client we are processing a source file
  mDiagClient->BeginSourceFile(LangOpts, mPP.get());

  // The core of the slang compiler
  ParseAST(*mPP, mBackend.get(), *mASTContext);

  // Inform the diagnostic client we are done with previous source file
  mDiagClient->EndSourceFile();

  // Declare success if no error
  if (!mDiagEngine->hasErrorOccurred())
    mOS->keep();

  // The compilation ended, clear
  mBackend.reset();
  mASTContext.reset();
  mPP.reset();
  mOS.reset();

  return mDiagEngine->hasErrorOccurred() ? 1 : 0;
}

void Slang::setDebugMetadataEmission(bool EmitDebug) {
  if (EmitDebug)
    CodeGenOpts.setDebugInfo(clang::CodeGenOptions::FullDebugInfo);
  else
    CodeGenOpts.setDebugInfo(clang::CodeGenOptions::NoDebugInfo);
}

void Slang::setOptimizationLevel(llvm::CodeGenOpt::Level OptimizationLevel) {
  CodeGenOpts.OptimizationLevel = OptimizationLevel;
}

void Slang::reset() {
  llvm::errs() << mDiagClient->str();
  mDiagEngine->Reset();
  mDiagClient->reset();
}

Slang::~Slang() {
  llvm::llvm_shutdown();
}

}  // namespace slang
