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

#include "slang_rs_export_var.h"

#include "clang/AST/ASTContext.h"
#include "clang/AST/Type.h"

#include "llvm/ADT/APSInt.h"

#include "slang_rs_context.h"
#include "slang_rs_export_type.h"

namespace slang {

namespace {

static clang::DiagnosticBuilder ReportVarError(RSContext *Context,
                           const clang::SourceLocation Loc,
                           const char *Message) {
  clang::DiagnosticsEngine *DiagEngine = Context->getDiagnostics();
  const clang::SourceManager *SM = Context->getSourceManager();
  return DiagEngine->Report(clang::FullSourceLoc(Loc, *SM),
      DiagEngine->getCustomDiagID(clang::DiagnosticsEngine::Error, Message));
}

}  // namespace

RSExportVar::RSExportVar(RSContext *Context,
                         const clang::VarDecl *VD,
                         const RSExportType *ET)
    : RSExportable(Context, RSExportable::EX_VAR),
      mName(VD->getName().data(), VD->getName().size()),
      mET(ET),
      mIsConst(false),
      mIsUnsigned(false),
      mArraySize(0),
      mNumInits(0) {
  // mInit - Evaluate initializer expression
  const clang::Expr *Initializer = VD->getAnyInitializer();
  if (Initializer != NULL) {
    switch (ET->getClass()) {
      case RSExportType::ExportClassPrimitive:
      case RSExportType::ExportClassVector: {
        Initializer->EvaluateAsRValue(mInit, Context->getASTContext());
        break;
      }
      case RSExportType::ExportClassPointer: {
        if (Initializer->isNullPointerConstant(Context->getASTContext(),
                clang::Expr::NPC_ValueDependentIsNotNull)) {
          mInit.Val = clang::APValue(llvm::APSInt(1));
        } else {
          if (!Initializer->EvaluateAsRValue(mInit, Context->getASTContext())) {
            ReportVarError(Context, Initializer->getExprLoc(),
                           "initializer is not an R-value");
          }
        }
        break;
      }
      case RSExportType::ExportClassConstantArray: {
        const clang::InitListExpr *IList =
            static_cast<const clang::InitListExpr*>(Initializer);
        if (!IList) {
          ReportVarError(Context, VD->getLocation(),
                         "Unable to find initializer list");
          break;
        }
        const RSExportConstantArrayType *ECAT =
            static_cast<const RSExportConstantArrayType*>(ET);
        mArraySize = ECAT->getSize();
        mNumInits = IList->getNumInits();
        for (unsigned int i = 0; i < mNumInits; i++) {
          clang::Expr::EvalResult tempInit;
          if (!IList->getInit(i)->EvaluateAsRValue(tempInit,
                                                   Context->getASTContext())) {
            ReportVarError(Context, IList->getInit(i)->getExprLoc(),
                           "initializer is not an R-value");
          }
          mInitArray.push_back(tempInit);
        }
        break;
      }
      case RSExportType::ExportClassMatrix:
      case RSExportType::ExportClassRecord: {
        ReportVarError(Context, VD->getLocation(),
                       "Reflection of initializer to variable '%0' (of type "
                       "'%1') is unsupported currently.")
            << mName
            << ET->getName();
        break;
      }
      default: {
        slangAssert(false && "Unknown class of type");
      }
    }
  }

  clang::QualType QT = VD->getTypeSourceInfo()->getType();
  if (!QT.isNull()) {
    mIsConst = QT.isConstQualified();
    mIsUnsigned = QT->hasUnsignedIntegerRepresentation();
    if (QT == Context->getASTContext().BoolTy) {
      mIsUnsigned = false;
    }
  }

  return;
}

}  // namespace slang
