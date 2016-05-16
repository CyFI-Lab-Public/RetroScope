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

#ifndef _FRAMEWORKS_COMPILE_SLANG_SLANG_BACKEND_H_  // NOLINT
#define _FRAMEWORKS_COMPILE_SLANG_SLANG_BACKEND_H_

#include "clang/AST/ASTConsumer.h"

#include "llvm/PassManager.h"

#include "llvm/Support/FormattedStream.h"

#include "slang.h"
#include "slang_pragma_recorder.h"
#include "slang_version.h"

namespace llvm {
  class formatted_raw_ostream;
  class LLVMContext;
  class NamedMDNode;
  class Module;
  class PassManager;
  class FunctionPassManager;
}

namespace clang {
  class CodeGenOptions;
  class CodeGenerator;
  class DeclGroupRef;
  class TagDecl;
  class VarDecl;
}

namespace slang {

class Backend : public clang::ASTConsumer {
 private:
  const clang::TargetOptions &mTargetOpts;

  llvm::Module *mpModule;

  // Output stream
  llvm::raw_ostream *mpOS;
  Slang::OutputType mOT;

  // This helps us translate Clang AST using into LLVM IR
  clang::CodeGenerator *mGen;

  // Passes

  // Passes apply on function scope in a translation unit
  llvm::FunctionPassManager *mPerFunctionPasses;
  // Passes apply on module scope
  llvm::PassManager *mPerModulePasses;
  // Passes for code emission
  llvm::FunctionPassManager *mCodeGenPasses;

  llvm::formatted_raw_ostream FormattedOutStream;

  void CreateFunctionPasses();
  void CreateModulePasses();
  bool CreateCodeGenPasses();

  void WrapBitcode(llvm::raw_string_ostream &Bitcode);

 protected:
  llvm::LLVMContext &mLLVMContext;
  clang::DiagnosticsEngine &mDiagEngine;
  const clang::CodeGenOptions &mCodeGenOpts;

  PragmaList *mPragmas;

  virtual unsigned int getTargetAPI() const {
    return SLANG_MAXIMUM_TARGET_API;
  }

  // This handler will be invoked before Clang translates @Ctx to LLVM IR. This
  // give you an opportunity to modified the IR in AST level (scope information,
  // unoptimized IR, etc.). After the return from this method, slang will start
  // translate @Ctx into LLVM IR. One should not operate on @Ctx afterwards
  // since the changes applied on that never reflects to the LLVM module used
  // in the final codegen.
  virtual void HandleTranslationUnitPre(clang::ASTContext &Ctx) { return; }

  // This handler will be invoked when Clang have converted AST tree to LLVM IR.
  // The @M contains the resulting LLVM IR tree. After the return from this
  // method, slang will start doing optimization and code generation for @M.
  virtual void HandleTranslationUnitPost(llvm::Module *M) { return; }

 public:
  Backend(clang::DiagnosticsEngine *DiagEngine,
          const clang::CodeGenOptions &CodeGenOpts,
          const clang::TargetOptions &TargetOpts,
          PragmaList *Pragmas,
          llvm::raw_ostream *OS,
          Slang::OutputType OT);

  // Initialize - This is called to initialize the consumer, providing the
  // ASTContext.
  virtual void Initialize(clang::ASTContext &Ctx);

  // HandleTopLevelDecl - Handle the specified top-level declaration.  This is
  // called by the parser to process every top-level Decl*. Note that D can be
  // the head of a chain of Decls (e.g. for `int a, b` the chain will have two
  // elements). Use Decl::getNextDeclarator() to walk the chain.
  virtual bool HandleTopLevelDecl(clang::DeclGroupRef D);

  // HandleTranslationUnit - This method is called when the ASTs for entire
  // translation unit have been parsed.
  virtual void HandleTranslationUnit(clang::ASTContext &Ctx);

  // HandleTagDeclDefinition - This callback is invoked each time a TagDecl
  // (e.g. struct, union, enum, class) is completed.  This allows the client to
  // hack on the type, which can occur at any point in the file (because these
  // can be defined in declspecs).
  virtual void HandleTagDeclDefinition(clang::TagDecl *D);

  // CompleteTentativeDefinition - Callback invoked at the end of a translation
  // unit to notify the consumer that the given tentative definition should be
  // completed.
  virtual void CompleteTentativeDefinition(clang::VarDecl *D);

  virtual ~Backend();
};

}   // namespace slang

#endif  // _FRAMEWORKS_COMPILE_SLANG_SLANG_BACKEND_H_  NOLINT
