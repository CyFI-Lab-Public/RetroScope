/*
 * Copyright 2011-2012, The Android Open Source Project
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

#include "slang_rs_export_foreach.h"

#include <string>

#include "clang/AST/ASTContext.h"
#include "clang/AST/Attr.h"
#include "clang/AST/Decl.h"
#include "clang/AST/TypeLoc.h"

#include "llvm/IR/DerivedTypes.h"

#include "slang_assert.h"
#include "slang_rs_context.h"
#include "slang_rs_export_type.h"
#include "slang_version.h"

namespace slang {

namespace {

static void ReportNameError(clang::DiagnosticsEngine *DiagEngine,
                            clang::ParmVarDecl const *PVD) {
  slangAssert(DiagEngine && PVD);
  const clang::SourceManager &SM = DiagEngine->getSourceManager();

  DiagEngine->Report(
    clang::FullSourceLoc(PVD->getLocation(), SM),
    DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                "Duplicate parameter entry "
                                "(by position/name): '%0'"))
    << PVD->getName();
  return;
}

}  // namespace


// This function takes care of additional validation and construction of
// parameters related to forEach_* reflection.
bool RSExportForEach::validateAndConstructParams(
    RSContext *Context, const clang::FunctionDecl *FD) {
  slangAssert(Context && FD);
  bool valid = true;
  clang::ASTContext &C = Context->getASTContext();
  clang::DiagnosticsEngine *DiagEngine = Context->getDiagnostics();

  numParams = FD->getNumParams();

  if (Context->getTargetAPI() < SLANG_JB_TARGET_API) {
    if (!isRootRSFunc(FD)) {
      DiagEngine->Report(
        clang::FullSourceLoc(FD->getLocation(), DiagEngine->getSourceManager()),
        DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                    "Non-root compute kernel %0() is "
                                    "not supported in SDK levels %1-%2"))
        << FD->getName()
        << SLANG_MINIMUM_TARGET_API
        << (SLANG_JB_TARGET_API - 1);
      return false;
    }
  }

  mResultType = FD->getResultType().getCanonicalType();
  // Compute kernel functions are required to return a void type or
  // be marked explicitly as a kernel. In the case of
  // "__attribute__((kernel))", we handle validation differently.
  if (FD->hasAttr<clang::KernelAttr>()) {
    return validateAndConstructKernelParams(Context, FD);
  }

  // If numParams is 0, we already marked this as a graphics root().
  slangAssert(numParams > 0);

  // Compute kernel functions of this type are required to return a void type.
  if (mResultType != C.VoidTy) {
    DiagEngine->Report(
      clang::FullSourceLoc(FD->getLocation(), DiagEngine->getSourceManager()),
      DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                  "Compute kernel %0() is required to return a "
                                  "void type")) << FD->getName();
    valid = false;
  }

  // Validate remaining parameter types
  // TODO(all): Add support for LOD/face when we have them

  size_t i = 0;
  const clang::ParmVarDecl *PVD = FD->getParamDecl(i);
  clang::QualType QT = PVD->getType().getCanonicalType();

  // Check for const T1 *in
  if (QT->isPointerType() && QT->getPointeeType().isConstQualified()) {
    mIn = PVD;
    i++;  // advance parameter pointer
  }

  // Check for T2 *out
  if (i < numParams) {
    PVD = FD->getParamDecl(i);
    QT = PVD->getType().getCanonicalType();
    if (QT->isPointerType() && !QT->getPointeeType().isConstQualified()) {
      mOut = PVD;
      i++;  // advance parameter pointer
    }
  }

  if (!mIn && !mOut) {
    DiagEngine->Report(
      clang::FullSourceLoc(FD->getLocation(),
                           DiagEngine->getSourceManager()),
      DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                  "Compute kernel %0() must have at least one "
                                  "parameter for in or out")) << FD->getName();
    valid = false;
  }

  // Check for T3 *usrData
  if (i < numParams) {
    PVD = FD->getParamDecl(i);
    QT = PVD->getType().getCanonicalType();
    if (QT->isPointerType() && QT->getPointeeType().isConstQualified()) {
      mUsrData = PVD;
      i++;  // advance parameter pointer
    }
  }

  while (i < numParams) {
    PVD = FD->getParamDecl(i);
    QT = PVD->getType().getCanonicalType();

    if (QT.getUnqualifiedType() != C.UnsignedIntTy) {
      DiagEngine->Report(
        clang::FullSourceLoc(PVD->getLocation(),
                             DiagEngine->getSourceManager()),
        DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                    "Unexpected kernel %0() parameter '%1' "
                                    "of type '%2'"))
        << FD->getName() << PVD->getName() << PVD->getType().getAsString();
      valid = false;
    } else {
      llvm::StringRef ParamName = PVD->getName();
      if (ParamName.equals("x")) {
        if (mX) {
          ReportNameError(DiagEngine, PVD);
          valid = false;
        } else if (mY) {
          // Can't go back to X after skipping Y
          ReportNameError(DiagEngine, PVD);
          valid = false;
        } else {
          mX = PVD;
        }
      } else if (ParamName.equals("y")) {
        if (mY) {
          ReportNameError(DiagEngine, PVD);
          valid = false;
        } else {
          mY = PVD;
        }
      } else {
        if (!mX && !mY) {
          mX = PVD;
        } else if (!mY) {
          mY = PVD;
        } else {
          DiagEngine->Report(
            clang::FullSourceLoc(PVD->getLocation(),
                                 DiagEngine->getSourceManager()),
            DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                        "Unexpected kernel %0() parameter '%1' "
                                        "of type '%2'"))
            << FD->getName() << PVD->getName() << PVD->getType().getAsString();
          valid = false;
        }
      }
    }

    i++;
  }

  mSignatureMetadata = 0;
  if (valid) {
    // Set up the bitwise metadata encoding for runtime argument passing.
    mSignatureMetadata |= (mIn ?       0x01 : 0);
    mSignatureMetadata |= (mOut ?      0x02 : 0);
    mSignatureMetadata |= (mUsrData ?  0x04 : 0);
    mSignatureMetadata |= (mX ?        0x08 : 0);
    mSignatureMetadata |= (mY ?        0x10 : 0);
  }

  if (Context->getTargetAPI() < SLANG_ICS_TARGET_API) {
    // APIs before ICS cannot skip between parameters. It is ok, however, for
    // them to omit further parameters (i.e. skipping X is ok if you skip Y).
    if (mSignatureMetadata != 0x1f &&  // In, Out, UsrData, X, Y
        mSignatureMetadata != 0x0f &&  // In, Out, UsrData, X
        mSignatureMetadata != 0x07 &&  // In, Out, UsrData
        mSignatureMetadata != 0x03 &&  // In, Out
        mSignatureMetadata != 0x01) {  // In
      DiagEngine->Report(
        clang::FullSourceLoc(FD->getLocation(),
                             DiagEngine->getSourceManager()),
        DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                    "Compute kernel %0() targeting SDK levels "
                                    "%1-%2 may not skip parameters"))
        << FD->getName() << SLANG_MINIMUM_TARGET_API
        << (SLANG_ICS_TARGET_API - 1);
      valid = false;
    }
  }

  return valid;
}


bool RSExportForEach::validateAndConstructKernelParams(RSContext *Context,
    const clang::FunctionDecl *FD) {
  slangAssert(Context && FD);
  bool valid = true;
  clang::ASTContext &C = Context->getASTContext();
  clang::DiagnosticsEngine *DiagEngine = Context->getDiagnostics();

  if (Context->getTargetAPI() < SLANG_JB_MR1_TARGET_API) {
    DiagEngine->Report(
      clang::FullSourceLoc(FD->getLocation(),
                           DiagEngine->getSourceManager()),
      DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                  "Compute kernel %0() targeting SDK levels "
                                  "%1-%2 may not use pass-by-value with "
                                  "__attribute__((kernel))"))
      << FD->getName() << SLANG_MINIMUM_TARGET_API
      << (SLANG_JB_MR1_TARGET_API - 1);
    return false;
  }

  // Denote that we are indeed a pass-by-value kernel.
  mKernel = true;

  if (mResultType != C.VoidTy) {
    mReturn = true;
  }

  if (mResultType->isPointerType()) {
    DiagEngine->Report(
      clang::FullSourceLoc(FD->getTypeSpecStartLoc(),
                           DiagEngine->getSourceManager()),
      DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                  "Compute kernel %0() cannot return a "
                                  "pointer type: '%1'"))
      << FD->getName() << mResultType.getAsString();
    valid = false;
  }

  // Validate remaining parameter types
  // TODO(all): Add support for LOD/face when we have them

  size_t i = 0;
  const clang::ParmVarDecl *PVD = NULL;
  clang::QualType QT;

  if (i < numParams) {
    PVD = FD->getParamDecl(i);
    QT = PVD->getType().getCanonicalType();

    if (QT->isPointerType()) {
      DiagEngine->Report(
        clang::FullSourceLoc(PVD->getLocation(),
                             DiagEngine->getSourceManager()),
        DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                    "Compute kernel %0() cannot have "
                                    "parameter '%1' of pointer type: '%2'"))
        << FD->getName() << PVD->getName() << PVD->getType().getAsString();
      valid = false;
    } else if (QT.getUnqualifiedType() == C.UnsignedIntTy) {
      // First parameter is either input or x, y (iff it is uint32_t).
      llvm::StringRef ParamName = PVD->getName();
      if (ParamName.equals("x")) {
        mX = PVD;
      } else if (ParamName.equals("y")) {
        mY = PVD;
      } else {
        mIn = PVD;
      }
    } else {
      mIn = PVD;
    }

    i++;  // advance parameter pointer
  }

  // Check that we have at least one allocation to use for dimensions.
  if (valid && !mIn && !mReturn) {
    DiagEngine->Report(
      clang::FullSourceLoc(FD->getLocation(),
                           DiagEngine->getSourceManager()),
      DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                  "Compute kernel %0() must have at least one "
                                  "input parameter or a non-void return "
                                  "type")) << FD->getName();
    valid = false;
  }

  // TODO: Abstract this block away, since it is duplicate code.
  while (i < numParams) {
    PVD = FD->getParamDecl(i);
    QT = PVD->getType().getCanonicalType();

    if (QT.getUnqualifiedType() != C.UnsignedIntTy) {
      DiagEngine->Report(
        clang::FullSourceLoc(PVD->getLocation(),
                             DiagEngine->getSourceManager()),
        DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                    "Unexpected kernel %0() parameter '%1' "
                                    "of type '%2'"))
        << FD->getName() << PVD->getName() << PVD->getType().getAsString();
      valid = false;
    } else {
      llvm::StringRef ParamName = PVD->getName();
      if (ParamName.equals("x")) {
        if (mX) {
          ReportNameError(DiagEngine, PVD);
          valid = false;
        } else if (mY) {
          // Can't go back to X after skipping Y
          ReportNameError(DiagEngine, PVD);
          valid = false;
        } else {
          mX = PVD;
        }
      } else if (ParamName.equals("y")) {
        if (mY) {
          ReportNameError(DiagEngine, PVD);
          valid = false;
        } else {
          mY = PVD;
        }
      } else {
        if (!mX && !mY) {
          mX = PVD;
        } else if (!mY) {
          mY = PVD;
        } else {
          DiagEngine->Report(
            clang::FullSourceLoc(PVD->getLocation(),
                                 DiagEngine->getSourceManager()),
            DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                        "Unexpected kernel %0() parameter '%1' "
                                        "of type '%2'"))
            << FD->getName() << PVD->getName() << PVD->getType().getAsString();
          valid = false;
        }
      }
    }

    i++;  // advance parameter pointer
  }

  mSignatureMetadata = 0;
  if (valid) {
    // Set up the bitwise metadata encoding for runtime argument passing.
    mSignatureMetadata |= (mIn ?       0x01 : 0);
    slangAssert(mOut == NULL);
    mSignatureMetadata |= (mReturn ?   0x02 : 0);
    slangAssert(mUsrData == NULL);
    mSignatureMetadata |= (mUsrData ?  0x04 : 0);
    mSignatureMetadata |= (mX ?        0x08 : 0);
    mSignatureMetadata |= (mY ?        0x10 : 0);
    mSignatureMetadata |= (mKernel ?   0x20 : 0);  // pass-by-value
  }

  return valid;
}


RSExportForEach *RSExportForEach::Create(RSContext *Context,
                                         const clang::FunctionDecl *FD) {
  slangAssert(Context && FD);
  llvm::StringRef Name = FD->getName();
  RSExportForEach *FE;

  slangAssert(!Name.empty() && "Function must have a name");

  FE = new RSExportForEach(Context, Name);

  if (!FE->validateAndConstructParams(Context, FD)) {
    return NULL;
  }

  clang::ASTContext &Ctx = Context->getASTContext();

  std::string Id(DUMMY_RS_TYPE_NAME_PREFIX"helper_foreach_param:");
  Id.append(FE->getName()).append(DUMMY_RS_TYPE_NAME_POSTFIX);

  // Extract the usrData parameter (if we have one)
  if (FE->mUsrData) {
    const clang::ParmVarDecl *PVD = FE->mUsrData;
    clang::QualType QT = PVD->getType().getCanonicalType();
    slangAssert(QT->isPointerType() &&
                QT->getPointeeType().isConstQualified());

    const clang::ASTContext &C = Context->getASTContext();
    if (QT->getPointeeType().getCanonicalType().getUnqualifiedType() ==
        C.VoidTy) {
      // In the case of using const void*, we can't reflect an appopriate
      // Java type, so we fall back to just reflecting the ain/aout parameters
      FE->mUsrData = NULL;
    } else {
      clang::RecordDecl *RD =
          clang::RecordDecl::Create(Ctx, clang::TTK_Struct,
                                    Ctx.getTranslationUnitDecl(),
                                    clang::SourceLocation(),
                                    clang::SourceLocation(),
                                    &Ctx.Idents.get(Id));

      clang::FieldDecl *FD =
          clang::FieldDecl::Create(Ctx,
                                   RD,
                                   clang::SourceLocation(),
                                   clang::SourceLocation(),
                                   PVD->getIdentifier(),
                                   QT->getPointeeType(),
                                   NULL,
                                   /* BitWidth = */ NULL,
                                   /* Mutable = */ false,
                                   /* HasInit = */ clang::ICIS_NoInit);
      RD->addDecl(FD);
      RD->completeDefinition();

      // Create an export type iff we have a valid usrData type
      clang::QualType T = Ctx.getTagDeclType(RD);
      slangAssert(!T.isNull());

      RSExportType *ET = RSExportType::Create(Context, T.getTypePtr());

      if (ET == NULL) {
        fprintf(stderr, "Failed to export the function %s. There's at least "
                        "one parameter whose type is not supported by the "
                        "reflection\n", FE->getName().c_str());
        return NULL;
      }

      slangAssert((ET->getClass() == RSExportType::ExportClassRecord) &&
                  "Parameter packet must be a record");

      FE->mParamPacketType = static_cast<RSExportRecordType *>(ET);
    }
  }

  if (FE->mIn) {
    const clang::Type *T = FE->mIn->getType().getCanonicalType().getTypePtr();
    FE->mInType = RSExportType::Create(Context, T);
    if (FE->mKernel) {
      slangAssert(FE->mInType);
    }
  }

  if (FE->mKernel && FE->mReturn) {
    const clang::Type *T = FE->mResultType.getTypePtr();
    FE->mOutType = RSExportType::Create(Context, T);
    slangAssert(FE->mOutType);
  } else if (FE->mOut) {
    const clang::Type *T = FE->mOut->getType().getCanonicalType().getTypePtr();
    FE->mOutType = RSExportType::Create(Context, T);
  }

  return FE;
}

RSExportForEach *RSExportForEach::CreateDummyRoot(RSContext *Context) {
  slangAssert(Context);
  llvm::StringRef Name = "root";
  RSExportForEach *FE = new RSExportForEach(Context, Name);
  FE->mDummyRoot = true;
  return FE;
}

bool RSExportForEach::isGraphicsRootRSFunc(int targetAPI,
                                           const clang::FunctionDecl *FD) {
  if (FD->hasAttr<clang::KernelAttr>()) {
    return false;
  }

  if (!isRootRSFunc(FD)) {
    return false;
  }

  if (FD->getNumParams() == 0) {
    // Graphics root function
    return true;
  }

  // Check for legacy graphics root function (with single parameter).
  if ((targetAPI < SLANG_ICS_TARGET_API) && (FD->getNumParams() == 1)) {
    const clang::QualType &IntType = FD->getASTContext().IntTy;
    if (FD->getResultType().getCanonicalType() == IntType) {
      return true;
    }
  }

  return false;
}

bool RSExportForEach::isRSForEachFunc(int targetAPI,
    clang::DiagnosticsEngine *DiagEngine,
    const clang::FunctionDecl *FD) {
  slangAssert(DiagEngine && FD);
  bool hasKernelAttr = FD->hasAttr<clang::KernelAttr>();

  if (FD->getStorageClass() == clang::SC_Static) {
    if (hasKernelAttr) {
      DiagEngine->Report(
        clang::FullSourceLoc(FD->getLocation(),
                             DiagEngine->getSourceManager()),
        DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                    "Invalid use of attribute kernel with "
                                    "static function declaration: %0"))
        << FD->getName();
    }
    return false;
  }

  // Anything tagged as a kernel is definitely used with ForEach.
  if (hasKernelAttr) {
    return true;
  }

  if (isGraphicsRootRSFunc(targetAPI, FD)) {
    return false;
  }

  // Check if first parameter is a pointer (which is required for ForEach).
  unsigned int numParams = FD->getNumParams();

  if (numParams > 0) {
    const clang::ParmVarDecl *PVD = FD->getParamDecl(0);
    clang::QualType QT = PVD->getType().getCanonicalType();

    if (QT->isPointerType()) {
      return true;
    }

    // Any non-graphics root() is automatically a ForEach candidate.
    // At this point, however, we know that it is not going to be a valid
    // compute root() function (due to not having a pointer parameter). We
    // still want to return true here, so that we can issue appropriate
    // diagnostics.
    if (isRootRSFunc(FD)) {
      return true;
    }
  }

  return false;
}

bool
RSExportForEach::validateSpecialFuncDecl(int targetAPI,
                                         clang::DiagnosticsEngine *DiagEngine,
                                         clang::FunctionDecl const *FD) {
  slangAssert(DiagEngine && FD);
  bool valid = true;
  const clang::ASTContext &C = FD->getASTContext();
  const clang::QualType &IntType = FD->getASTContext().IntTy;

  if (isGraphicsRootRSFunc(targetAPI, FD)) {
    if ((targetAPI < SLANG_ICS_TARGET_API) && (FD->getNumParams() == 1)) {
      // Legacy graphics root function
      const clang::ParmVarDecl *PVD = FD->getParamDecl(0);
      clang::QualType QT = PVD->getType().getCanonicalType();
      if (QT != IntType) {
        DiagEngine->Report(
          clang::FullSourceLoc(PVD->getLocation(),
                               DiagEngine->getSourceManager()),
          DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                      "invalid parameter type for legacy "
                                      "graphics root() function: %0"))
          << PVD->getType();
        valid = false;
      }
    }

    // Graphics root function, so verify that it returns an int
    if (FD->getResultType().getCanonicalType() != IntType) {
      DiagEngine->Report(
        clang::FullSourceLoc(FD->getLocation(),
                             DiagEngine->getSourceManager()),
        DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                    "root() is required to return "
                                    "an int for graphics usage"));
      valid = false;
    }
  } else if (isInitRSFunc(FD) || isDtorRSFunc(FD)) {
    if (FD->getNumParams() != 0) {
      DiagEngine->Report(
          clang::FullSourceLoc(FD->getLocation(),
                               DiagEngine->getSourceManager()),
          DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                      "%0(void) is required to have no "
                                      "parameters")) << FD->getName();
      valid = false;
    }

    if (FD->getResultType().getCanonicalType() != C.VoidTy) {
      DiagEngine->Report(
          clang::FullSourceLoc(FD->getLocation(),
                               DiagEngine->getSourceManager()),
          DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error,
                                      "%0(void) is required to have a void "
                                      "return type")) << FD->getName();
      valid = false;
    }
  } else {
    slangAssert(false && "must be called on root, init or .rs.dtor function!");
  }

  return valid;
}

}  // namespace slang
