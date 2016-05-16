/*
 * Copyright 2010-2012, The Android Open Source Project
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

#ifndef _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_BACKEND_H_  // NOLINT
#define _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_BACKEND_H_

#include "slang_backend.h"
#include "slang_pragma_recorder.h"
#include "slang_rs_check_ast.h"
#include "slang_rs_object_ref_count.h"

namespace llvm {
  class NamedMDNode;
}

namespace clang {
  class ASTConsumer;
  class DiagnosticsEngine;
  class TargetOptions;
  class CodeGenerator;
  class ASTContext;
  class DeclGroupRef;
  class FunctionDecl;
}

namespace slang {

class RSContext;

class RSBackend : public Backend {
 private:
  RSContext *mContext;

  clang::SourceManager &mSourceMgr;

  bool mAllowRSPrefix;

  bool mIsFilterscript;

  llvm::NamedMDNode *mExportVarMetadata;
  llvm::NamedMDNode *mExportFuncMetadata;
  llvm::NamedMDNode *mExportForEachNameMetadata;
  llvm::NamedMDNode *mExportForEachSignatureMetadata;
  llvm::NamedMDNode *mExportTypeMetadata;
  llvm::NamedMDNode *mExportElementMetadata;
  llvm::NamedMDNode *mRSObjectSlotsMetadata;

  RSObjectRefCount mRefCount;

  RSCheckAST mASTChecker;

  void AnnotateFunction(clang::FunctionDecl *FD);

  void dumpExportVarInfo(llvm::Module *M);
  void dumpExportFunctionInfo(llvm::Module *M);
  void dumpExportForEachInfo(llvm::Module *M);
  void dumpExportTypeInfo(llvm::Module *M);

 protected:
  virtual unsigned int getTargetAPI() const {
    return mContext->getTargetAPI();
  }

  virtual bool HandleTopLevelDecl(clang::DeclGroupRef D);

  virtual void HandleTranslationUnitPre(clang::ASTContext &C);

  virtual void HandleTranslationUnitPost(llvm::Module *M);

 public:
  RSBackend(RSContext *Context,
            clang::DiagnosticsEngine *DiagEngine,
            const clang::CodeGenOptions &CodeGenOpts,
            const clang::TargetOptions &TargetOpts,
            PragmaList *Pragmas,
            llvm::raw_ostream *OS,
            Slang::OutputType OT,
            clang::SourceManager &SourceMgr,
            bool AllowRSPrefix,
            bool IsFilterscript);

  virtual ~RSBackend();
};
}  // namespace slang

#endif  // _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_BACKEND_H_  NOLINT
