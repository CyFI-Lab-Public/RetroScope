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

#include "slang_rs_context.h"

#include <string>

#include "clang/AST/ASTContext.h"
#include "clang/AST/Decl.h"
#include "clang/AST/DeclBase.h"
#include "clang/AST/Mangle.h"
#include "clang/AST/Type.h"

#include "clang/Basic/Linkage.h"
#include "clang/Basic/TargetInfo.h"

#include "llvm/IR/LLVMContext.h"
#include "llvm/IR/DataLayout.h"

#include "slang.h"
#include "slang_assert.h"
#include "slang_rs_export_foreach.h"
#include "slang_rs_export_func.h"
#include "slang_rs_export_type.h"
#include "slang_rs_export_var.h"
#include "slang_rs_exportable.h"
#include "slang_rs_pragma_handler.h"
#include "slang_rs_reflection.h"

namespace slang {

RSContext::RSContext(clang::Preprocessor &PP,
                     clang::ASTContext &Ctx,
                     const clang::TargetInfo &Target,
                     PragmaList *Pragmas,
                     unsigned int TargetAPI,
                     std::vector<std::string> *GeneratedFileNames)
    : mPP(PP),
      mCtx(Ctx),
      mTarget(Target),
      mPragmas(Pragmas),
      mTargetAPI(TargetAPI),
      mGeneratedFileNames(GeneratedFileNames),
      mDataLayout(NULL),
      mLLVMContext(llvm::getGlobalContext()),
      mLicenseNote(NULL),
      mRSPackageName("android.renderscript"),
      version(0),
      mIsCompatLib(false),
      mMangleCtx(Ctx.createMangleContext()) {
  slangAssert(mGeneratedFileNames && "Must supply GeneratedFileNames");

  // For #pragma rs export_type
  PP.AddPragmaHandler(
      "rs", RSPragmaHandler::CreatePragmaExportTypeHandler(this));

  // For #pragma rs java_package_name
  PP.AddPragmaHandler(
      "rs", RSPragmaHandler::CreatePragmaJavaPackageNameHandler(this));

  // For #pragma rs set_reflect_license
  PP.AddPragmaHandler(
      "rs", RSPragmaHandler::CreatePragmaReflectLicenseHandler(this));

  // For #pragma version
  PP.AddPragmaHandler(RSPragmaHandler::CreatePragmaVersionHandler(this));

  // Prepare target data
  mDataLayout = new llvm::DataLayout(Target.getTargetDescription());

  return;
}

bool RSContext::processExportVar(const clang::VarDecl *VD) {
  slangAssert(!VD->getName().empty() && "Variable name should not be empty");

  // TODO(zonr): some check on variable

  RSExportType *ET = RSExportType::CreateFromDecl(this, VD);
  if (!ET)
    return false;

  RSExportVar *EV = new RSExportVar(this, VD, ET);
  if (EV == NULL)
    return false;
  else
    mExportVars.push_back(EV);

  return true;
}

bool RSContext::processExportFunc(const clang::FunctionDecl *FD) {
  slangAssert(!FD->getName().empty() && "Function name should not be empty");

  if (!FD->isThisDeclarationADefinition()) {
    return true;
  }

  if (FD->getStorageClass() != clang::SC_None) {
    fprintf(stderr, "RSContext::processExportFunc : cannot export extern or "
                    "static function '%s'\n", FD->getName().str().c_str());
    return false;
  }

  clang::DiagnosticsEngine *DiagEngine = getDiagnostics();
  if (RSExportForEach::isSpecialRSFunc(mTargetAPI, FD)) {
    // Do not reflect specialized functions like init, dtor, or graphics root.
    return RSExportForEach::validateSpecialFuncDecl(mTargetAPI,
                                                    DiagEngine, FD);
  } else if (RSExportForEach::isRSForEachFunc(mTargetAPI, DiagEngine, FD)) {
    RSExportForEach *EFE = RSExportForEach::Create(this, FD);
    if (EFE == NULL)
      return false;
    else
      mExportForEach.push_back(EFE);
    return true;
  }

  RSExportFunc *EF = RSExportFunc::Create(this, FD);
  if (EF == NULL)
    return false;
  else
    mExportFuncs.push_back(EF);

  return true;
}


bool RSContext::processExportType(const llvm::StringRef &Name) {
  clang::TranslationUnitDecl *TUDecl = mCtx.getTranslationUnitDecl();

  slangAssert(TUDecl != NULL && "Translation unit declaration (top-level "
                                "declaration) is null object");

  const clang::IdentifierInfo *II = mPP.getIdentifierInfo(Name);
  if (II == NULL)
    // TODO(zonr): alert identifier @Name mark as an exportable type cannot be
    //             found
    return false;

  clang::DeclContext::lookup_const_result R = TUDecl->lookup(II);
  RSExportType *ET = NULL;

  for (clang::DeclContext::lookup_const_iterator I = R.begin(), E = R.end();
       I != E;
       I++) {
    clang::NamedDecl *const ND = *I;
    const clang::Type *T = NULL;

    switch (ND->getKind()) {
      case clang::Decl::Typedef: {
        T = static_cast<const clang::TypedefDecl*>(
            ND)->getCanonicalDecl()->getUnderlyingType().getTypePtr();
        break;
      }
      case clang::Decl::Record: {
        T = static_cast<const clang::RecordDecl*>(ND)->getTypeForDecl();
        break;
      }
      default: {
        // unsupported, skip
        break;
      }
    }

    if (T != NULL)
      ET = RSExportType::Create(this, T);
  }

  return (ET != NULL);
}


// Possibly re-order ForEach exports (maybe generating a dummy "root" function).
// We require "root" to be listed as slot 0 of our exported compute kernels,
// so this only needs to be created if we have other non-root kernels.
void RSContext::cleanupForEach() {
  bool foundNonRoot = false;
  ExportForEachList::iterator begin = mExportForEach.begin();

  for (ExportForEachList::iterator I = begin, E = mExportForEach.end();
       I != E;
       I++) {
    RSExportForEach *EFE = *I;
    if (!EFE->getName().compare("root")) {
      if (I == begin) {
        // Nothing to do, since it is the first function
        return;
      }

      mExportForEach.erase(I);
      mExportForEach.push_front(EFE);
      return;
    } else {
      foundNonRoot = true;
    }
  }

  // If we found a non-root kernel, but no root() function, we need to add a
  // dummy version (so that script->script calls of rsForEach don't behave
  // erratically).
  if (foundNonRoot) {
    RSExportForEach *DummyRoot = RSExportForEach::CreateDummyRoot(this);
    mExportForEach.push_front(DummyRoot);
  }
}


bool RSContext::processExport() {
  bool valid = true;

  if (getDiagnostics()->hasErrorOccurred()) {
    return false;
  }

  // Export variable
  clang::TranslationUnitDecl *TUDecl = mCtx.getTranslationUnitDecl();
  for (clang::DeclContext::decl_iterator DI = TUDecl->decls_begin(),
           DE = TUDecl->decls_end();
       DI != DE;
       DI++) {
    if (DI->getKind() == clang::Decl::Var) {
      clang::VarDecl *VD = (clang::VarDecl*) (*DI);
      if (VD->getFormalLinkage() == clang::ExternalLinkage) {
        if (!processExportVar(VD)) {
          valid = false;
        }
      }
    } else if (DI->getKind() == clang::Decl::Function) {
      // Export functions
      clang::FunctionDecl *FD = (clang::FunctionDecl*) (*DI);
      if (FD->getFormalLinkage() == clang::ExternalLinkage) {
        if (!processExportFunc(FD)) {
          valid = false;
        }
      }
    }
  }

  if (valid) {
    cleanupForEach();
  }

  // Finally, export type forcely set to be exported by user
  for (NeedExportTypeSet::const_iterator EI = mNeedExportTypes.begin(),
           EE = mNeedExportTypes.end();
       EI != EE;
       EI++) {
    if (!processExportType(EI->getKey())) {
      valid = false;
    }
  }

  return valid;
}

bool RSContext::insertExportType(const llvm::StringRef &TypeName,
                                 RSExportType *ET) {
  ExportTypeMap::value_type *NewItem =
      ExportTypeMap::value_type::Create(TypeName.begin(),
                                        TypeName.end(),
                                        mExportTypes.getAllocator(),
                                        ET);

  if (mExportTypes.insert(NewItem)) {
    return true;
  } else {
    free(NewItem);
    return false;
  }
}

bool RSContext::reflectToJava(const std::string &OutputPathBase,
                              const std::string &RSPackageName,
                              const std::string &InputFileName,
                              const std::string &OutputBCFileName) {
  if (!RSPackageName.empty()) {
    mRSPackageName = RSPackageName;
  }

  // If we are not targeting the actual Android Renderscript classes,
  // we should reflect code that works with the compatibility library.
  if (mRSPackageName.compare("android.renderscript") != 0) {
    mIsCompatLib = true;
  }

  RSReflection *R = new RSReflection(this, mGeneratedFileNames);
  bool ret = R->reflect(OutputPathBase, mReflectJavaPackageName, mRSPackageName,
                        InputFileName, OutputBCFileName);
  if (!ret)
    fprintf(stderr, "RSContext::reflectToJava : failed to do reflection "
                    "(%s)\n", R->getLastError());
  delete R;
  return ret;
}

RSContext::~RSContext() {
  delete mLicenseNote;
  delete mDataLayout;
  for (ExportableList::iterator I = mExportables.begin(),
          E = mExportables.end();
       I != E;
       I++) {
    if (!(*I)->isKeep())
      delete *I;
  }
}

}  // namespace slang
