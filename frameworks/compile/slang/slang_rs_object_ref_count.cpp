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

#include "slang_rs_object_ref_count.h"

#include <list>

#include "clang/AST/DeclGroup.h"
#include "clang/AST/Expr.h"
#include "clang/AST/NestedNameSpecifier.h"
#include "clang/AST/OperationKinds.h"
#include "clang/AST/Stmt.h"
#include "clang/AST/StmtVisitor.h"

#include "slang_assert.h"
#include "slang_rs.h"
#include "slang_rs_ast_replace.h"
#include "slang_rs_export_type.h"

namespace slang {

clang::FunctionDecl *RSObjectRefCount::
    RSSetObjectFD[RSExportPrimitiveType::LastRSObjectType -
                  RSExportPrimitiveType::FirstRSObjectType + 1];
clang::FunctionDecl *RSObjectRefCount::
    RSClearObjectFD[RSExportPrimitiveType::LastRSObjectType -
                    RSExportPrimitiveType::FirstRSObjectType + 1];

void RSObjectRefCount::GetRSRefCountingFunctions(clang::ASTContext &C) {
  for (unsigned i = 0;
       i < (sizeof(RSClearObjectFD) / sizeof(clang::FunctionDecl*));
       i++) {
    RSSetObjectFD[i] = NULL;
    RSClearObjectFD[i] = NULL;
  }

  clang::TranslationUnitDecl *TUDecl = C.getTranslationUnitDecl();

  for (clang::DeclContext::decl_iterator I = TUDecl->decls_begin(),
          E = TUDecl->decls_end(); I != E; I++) {
    if ((I->getKind() >= clang::Decl::firstFunction) &&
        (I->getKind() <= clang::Decl::lastFunction)) {
      clang::FunctionDecl *FD = static_cast<clang::FunctionDecl*>(*I);

      // points to RSSetObjectFD or RSClearObjectFD
      clang::FunctionDecl **RSObjectFD;

      if (FD->getName() == "rsSetObject") {
        slangAssert((FD->getNumParams() == 2) &&
                    "Invalid rsSetObject function prototype (# params)");
        RSObjectFD = RSSetObjectFD;
      } else if (FD->getName() == "rsClearObject") {
        slangAssert((FD->getNumParams() == 1) &&
                    "Invalid rsClearObject function prototype (# params)");
        RSObjectFD = RSClearObjectFD;
      } else {
        continue;
      }

      const clang::ParmVarDecl *PVD = FD->getParamDecl(0);
      clang::QualType PVT = PVD->getOriginalType();
      // The first parameter must be a pointer like rs_allocation*
      slangAssert(PVT->isPointerType() &&
          "Invalid rs{Set,Clear}Object function prototype (pointer param)");

      // The rs object type passed to the FD
      clang::QualType RST = PVT->getPointeeType();
      RSExportPrimitiveType::DataType DT =
          RSExportPrimitiveType::GetRSSpecificType(RST.getTypePtr());
      slangAssert(RSExportPrimitiveType::IsRSObjectType(DT)
             && "must be RS object type");

      RSObjectFD[(DT - RSExportPrimitiveType::FirstRSObjectType)] = FD;
    }
  }
}

namespace {

// This function constructs a new CompoundStmt from the input StmtList.
static clang::CompoundStmt* BuildCompoundStmt(clang::ASTContext &C,
      std::list<clang::Stmt*> &StmtList, clang::SourceLocation Loc) {
  unsigned NewStmtCount = StmtList.size();
  unsigned CompoundStmtCount = 0;

  clang::Stmt **CompoundStmtList;
  CompoundStmtList = new clang::Stmt*[NewStmtCount];

  std::list<clang::Stmt*>::const_iterator I = StmtList.begin();
  std::list<clang::Stmt*>::const_iterator E = StmtList.end();
  for ( ; I != E; I++) {
    CompoundStmtList[CompoundStmtCount++] = *I;
  }
  slangAssert(CompoundStmtCount == NewStmtCount);

  clang::CompoundStmt *CS = new(C) clang::CompoundStmt(
      C, llvm::makeArrayRef(CompoundStmtList, CompoundStmtCount), Loc, Loc);

  delete [] CompoundStmtList;

  return CS;
}

static void AppendAfterStmt(clang::ASTContext &C,
                            clang::CompoundStmt *CS,
                            clang::Stmt *S,
                            std::list<clang::Stmt*> &StmtList) {
  slangAssert(CS);
  clang::CompoundStmt::body_iterator bI = CS->body_begin();
  clang::CompoundStmt::body_iterator bE = CS->body_end();
  clang::Stmt **UpdatedStmtList =
      new clang::Stmt*[CS->size() + StmtList.size()];

  unsigned UpdatedStmtCount = 0;
  unsigned Once = 0;
  for ( ; bI != bE; bI++) {
    if (!S && ((*bI)->getStmtClass() == clang::Stmt::ReturnStmtClass)) {
      // If we come across a return here, we don't have anything we can
      // reasonably replace. We should have already inserted our destructor
      // code in the proper spot, so we just clean up and return.
      delete [] UpdatedStmtList;

      return;
    }

    UpdatedStmtList[UpdatedStmtCount++] = *bI;

    if ((*bI == S) && !Once) {
      Once++;
      std::list<clang::Stmt*>::const_iterator I = StmtList.begin();
      std::list<clang::Stmt*>::const_iterator E = StmtList.end();
      for ( ; I != E; I++) {
        UpdatedStmtList[UpdatedStmtCount++] = *I;
      }
    }
  }
  slangAssert(Once <= 1);

  // When S is NULL, we are appending to the end of the CompoundStmt.
  if (!S) {
    slangAssert(Once == 0);
    std::list<clang::Stmt*>::const_iterator I = StmtList.begin();
    std::list<clang::Stmt*>::const_iterator E = StmtList.end();
    for ( ; I != E; I++) {
      UpdatedStmtList[UpdatedStmtCount++] = *I;
    }
  }

  CS->setStmts(C, UpdatedStmtList, UpdatedStmtCount);

  delete [] UpdatedStmtList;

  return;
}

// This class visits a compound statement and inserts DtorStmt
// in proper locations. This includes inserting it before any
// return statement in any sub-block, at the end of the logical enclosing
// scope (compound statement), and/or before any break/continue statement that
// would resume outside the declared scope. We will not handle the case for
// goto statements that leave a local scope.
//
// To accomplish these goals, it collects a list of sub-Stmt's that
// correspond to scope exit points. It then uses an RSASTReplace visitor to
// transform the AST, inserting appropriate destructors before each of those
// sub-Stmt's (and also before the exit of the outermost containing Stmt for
// the scope).
class DestructorVisitor : public clang::StmtVisitor<DestructorVisitor> {
 private:
  clang::ASTContext &mCtx;

  // The loop depth of the currently visited node.
  int mLoopDepth;

  // The switch statement depth of the currently visited node.
  // Note that this is tracked separately from the loop depth because
  // SwitchStmt-contained ContinueStmt's should have destructors for the
  // corresponding loop scope.
  int mSwitchDepth;

  // The outermost statement block that we are currently visiting.
  // This should always be a CompoundStmt.
  clang::Stmt *mOuterStmt;

  // The destructor to execute for this scope/variable.
  clang::Stmt* mDtorStmt;

  // The stack of statements which should be replaced by a compound statement
  // containing the new destructor call followed by the original Stmt.
  std::stack<clang::Stmt*> mReplaceStmtStack;

  // The source location for the variable declaration that we are trying to
  // insert destructors for. Note that InsertDestructors() will not generate
  // destructor calls for source locations that occur lexically before this
  // location.
  clang::SourceLocation mVarLoc;

 public:
  DestructorVisitor(clang::ASTContext &C,
                    clang::Stmt* OuterStmt,
                    clang::Stmt* DtorStmt,
                    clang::SourceLocation VarLoc);

  // This code walks the collected list of Stmts to replace and actually does
  // the replacement. It also finishes up by appending the destructor to the
  // current outermost CompoundStmt.
  void InsertDestructors() {
    clang::Stmt *S = NULL;
    clang::SourceManager &SM = mCtx.getSourceManager();
    std::list<clang::Stmt *> StmtList;
    StmtList.push_back(mDtorStmt);

    while (!mReplaceStmtStack.empty()) {
      S = mReplaceStmtStack.top();
      mReplaceStmtStack.pop();

      // Skip all source locations that occur before the variable's
      // declaration, since it won't have been initialized yet.
      if (SM.isBeforeInTranslationUnit(S->getLocStart(), mVarLoc)) {
        continue;
      }

      StmtList.push_back(S);
      clang::CompoundStmt *CS =
          BuildCompoundStmt(mCtx, StmtList, S->getLocEnd());
      StmtList.pop_back();

      RSASTReplace R(mCtx);
      R.ReplaceStmt(mOuterStmt, S, CS);
    }
    clang::CompoundStmt *CS =
      llvm::dyn_cast<clang::CompoundStmt>(mOuterStmt);
    slangAssert(CS);
    AppendAfterStmt(mCtx, CS, NULL, StmtList);
  }

  void VisitStmt(clang::Stmt *S);
  void VisitCompoundStmt(clang::CompoundStmt *CS);

  void VisitBreakStmt(clang::BreakStmt *BS);
  void VisitCaseStmt(clang::CaseStmt *CS);
  void VisitContinueStmt(clang::ContinueStmt *CS);
  void VisitDefaultStmt(clang::DefaultStmt *DS);
  void VisitDoStmt(clang::DoStmt *DS);
  void VisitForStmt(clang::ForStmt *FS);
  void VisitIfStmt(clang::IfStmt *IS);
  void VisitReturnStmt(clang::ReturnStmt *RS);
  void VisitSwitchCase(clang::SwitchCase *SC);
  void VisitSwitchStmt(clang::SwitchStmt *SS);
  void VisitWhileStmt(clang::WhileStmt *WS);
};

DestructorVisitor::DestructorVisitor(clang::ASTContext &C,
                         clang::Stmt *OuterStmt,
                         clang::Stmt *DtorStmt,
                         clang::SourceLocation VarLoc)
  : mCtx(C),
    mLoopDepth(0),
    mSwitchDepth(0),
    mOuterStmt(OuterStmt),
    mDtorStmt(DtorStmt),
    mVarLoc(VarLoc) {
  return;
}

void DestructorVisitor::VisitStmt(clang::Stmt *S) {
  for (clang::Stmt::child_iterator I = S->child_begin(), E = S->child_end();
       I != E;
       I++) {
    if (clang::Stmt *Child = *I) {
      Visit(Child);
    }
  }
  return;
}

void DestructorVisitor::VisitCompoundStmt(clang::CompoundStmt *CS) {
  VisitStmt(CS);
  return;
}

void DestructorVisitor::VisitBreakStmt(clang::BreakStmt *BS) {
  VisitStmt(BS);
  if ((mLoopDepth == 0) && (mSwitchDepth == 0)) {
    mReplaceStmtStack.push(BS);
  }
  return;
}

void DestructorVisitor::VisitCaseStmt(clang::CaseStmt *CS) {
  VisitStmt(CS);
  return;
}

void DestructorVisitor::VisitContinueStmt(clang::ContinueStmt *CS) {
  VisitStmt(CS);
  if (mLoopDepth == 0) {
    // Switch statements can have nested continues.
    mReplaceStmtStack.push(CS);
  }
  return;
}

void DestructorVisitor::VisitDefaultStmt(clang::DefaultStmt *DS) {
  VisitStmt(DS);
  return;
}

void DestructorVisitor::VisitDoStmt(clang::DoStmt *DS) {
  mLoopDepth++;
  VisitStmt(DS);
  mLoopDepth--;
  return;
}

void DestructorVisitor::VisitForStmt(clang::ForStmt *FS) {
  mLoopDepth++;
  VisitStmt(FS);
  mLoopDepth--;
  return;
}

void DestructorVisitor::VisitIfStmt(clang::IfStmt *IS) {
  VisitStmt(IS);
  return;
}

void DestructorVisitor::VisitReturnStmt(clang::ReturnStmt *RS) {
  mReplaceStmtStack.push(RS);
  return;
}

void DestructorVisitor::VisitSwitchCase(clang::SwitchCase *SC) {
  slangAssert(false && "Both case and default have specialized handlers");
  VisitStmt(SC);
  return;
}

void DestructorVisitor::VisitSwitchStmt(clang::SwitchStmt *SS) {
  mSwitchDepth++;
  VisitStmt(SS);
  mSwitchDepth--;
  return;
}

void DestructorVisitor::VisitWhileStmt(clang::WhileStmt *WS) {
  mLoopDepth++;
  VisitStmt(WS);
  mLoopDepth--;
  return;
}

clang::Expr *ClearSingleRSObject(clang::ASTContext &C,
                                 clang::Expr *RefRSVar,
                                 clang::SourceLocation Loc) {
  slangAssert(RefRSVar);
  const clang::Type *T = RefRSVar->getType().getTypePtr();
  slangAssert(!T->isArrayType() &&
              "Should not be destroying arrays with this function");

  clang::FunctionDecl *ClearObjectFD = RSObjectRefCount::GetRSClearObjectFD(T);
  slangAssert((ClearObjectFD != NULL) &&
              "rsClearObject doesn't cover all RS object types");

  clang::QualType ClearObjectFDType = ClearObjectFD->getType();
  clang::QualType ClearObjectFDArgType =
      ClearObjectFD->getParamDecl(0)->getOriginalType();

  // Example destructor for "rs_font localFont;"
  //
  // (CallExpr 'void'
  //   (ImplicitCastExpr 'void (*)(rs_font *)' <FunctionToPointerDecay>
  //     (DeclRefExpr 'void (rs_font *)' FunctionDecl='rsClearObject'))
  //   (UnaryOperator 'rs_font *' prefix '&'
  //     (DeclRefExpr 'rs_font':'rs_font' Var='localFont')))

  // Get address of targeted RS object
  clang::Expr *AddrRefRSVar =
      new(C) clang::UnaryOperator(RefRSVar,
                                  clang::UO_AddrOf,
                                  ClearObjectFDArgType,
                                  clang::VK_RValue,
                                  clang::OK_Ordinary,
                                  Loc);

  clang::Expr *RefRSClearObjectFD =
      clang::DeclRefExpr::Create(C,
                                 clang::NestedNameSpecifierLoc(),
                                 clang::SourceLocation(),
                                 ClearObjectFD,
                                 false,
                                 ClearObjectFD->getLocation(),
                                 ClearObjectFDType,
                                 clang::VK_RValue,
                                 NULL);

  clang::Expr *RSClearObjectFP =
      clang::ImplicitCastExpr::Create(C,
                                      C.getPointerType(ClearObjectFDType),
                                      clang::CK_FunctionToPointerDecay,
                                      RefRSClearObjectFD,
                                      NULL,
                                      clang::VK_RValue);

  llvm::SmallVector<clang::Expr*, 1> ArgList;
  ArgList.push_back(AddrRefRSVar);

  clang::CallExpr *RSClearObjectCall =
      new(C) clang::CallExpr(C,
                             RSClearObjectFP,
                             ArgList,
                             ClearObjectFD->getCallResultType(),
                             clang::VK_RValue,
                             Loc);

  return RSClearObjectCall;
}

static int ArrayDim(const clang::Type *T) {
  if (!T || !T->isArrayType()) {
    return 0;
  }

  const clang::ConstantArrayType *CAT =
    static_cast<const clang::ConstantArrayType *>(T);
  return static_cast<int>(CAT->getSize().getSExtValue());
}

static clang::Stmt *ClearStructRSObject(
    clang::ASTContext &C,
    clang::DeclContext *DC,
    clang::Expr *RefRSStruct,
    clang::SourceLocation StartLoc,
    clang::SourceLocation Loc);

static clang::Stmt *ClearArrayRSObject(
    clang::ASTContext &C,
    clang::DeclContext *DC,
    clang::Expr *RefRSArr,
    clang::SourceLocation StartLoc,
    clang::SourceLocation Loc) {
  const clang::Type *BaseType = RefRSArr->getType().getTypePtr();
  slangAssert(BaseType->isArrayType());

  int NumArrayElements = ArrayDim(BaseType);
  // Actually extract out the base RS object type for use later
  BaseType = BaseType->getArrayElementTypeNoTypeQual();

  clang::Stmt *StmtArray[2] = {NULL};
  int StmtCtr = 0;

  if (NumArrayElements <= 0) {
    return NULL;
  }

  // Example destructor loop for "rs_font fontArr[10];"
  //
  // (CompoundStmt
  //   (DeclStmt "int rsIntIter")
  //   (ForStmt
  //     (BinaryOperator 'int' '='
  //       (DeclRefExpr 'int' Var='rsIntIter')
  //       (IntegerLiteral 'int' 0))
  //     (BinaryOperator 'int' '<'
  //       (DeclRefExpr 'int' Var='rsIntIter')
  //       (IntegerLiteral 'int' 10)
  //     NULL << CondVar >>
  //     (UnaryOperator 'int' postfix '++'
  //       (DeclRefExpr 'int' Var='rsIntIter'))
  //     (CallExpr 'void'
  //       (ImplicitCastExpr 'void (*)(rs_font *)' <FunctionToPointerDecay>
  //         (DeclRefExpr 'void (rs_font *)' FunctionDecl='rsClearObject'))
  //       (UnaryOperator 'rs_font *' prefix '&'
  //         (ArraySubscriptExpr 'rs_font':'rs_font'
  //           (ImplicitCastExpr 'rs_font *' <ArrayToPointerDecay>
  //             (DeclRefExpr 'rs_font [10]' Var='fontArr'))
  //           (DeclRefExpr 'int' Var='rsIntIter')))))))

  // Create helper variable for iterating through elements
  clang::IdentifierInfo& II = C.Idents.get("rsIntIter");
  clang::VarDecl *IIVD =
      clang::VarDecl::Create(C,
                             DC,
                             StartLoc,
                             Loc,
                             &II,
                             C.IntTy,
                             C.getTrivialTypeSourceInfo(C.IntTy),
                             clang::SC_None);
  clang::Decl *IID = (clang::Decl *)IIVD;

  clang::DeclGroupRef DGR = clang::DeclGroupRef::Create(C, &IID, 1);
  StmtArray[StmtCtr++] = new(C) clang::DeclStmt(DGR, Loc, Loc);

  // Form the actual destructor loop
  // for (Init; Cond; Inc)
  //   RSClearObjectCall;

  // Init -> "rsIntIter = 0"
  clang::DeclRefExpr *RefrsIntIter =
      clang::DeclRefExpr::Create(C,
                                 clang::NestedNameSpecifierLoc(),
                                 clang::SourceLocation(),
                                 IIVD,
                                 false,
                                 Loc,
                                 C.IntTy,
                                 clang::VK_RValue,
                                 NULL);

  clang::Expr *Int0 = clang::IntegerLiteral::Create(C,
      llvm::APInt(C.getTypeSize(C.IntTy), 0), C.IntTy, Loc);

  clang::BinaryOperator *Init =
      new(C) clang::BinaryOperator(RefrsIntIter,
                                   Int0,
                                   clang::BO_Assign,
                                   C.IntTy,
                                   clang::VK_RValue,
                                   clang::OK_Ordinary,
                                   Loc,
                                   false);

  // Cond -> "rsIntIter < NumArrayElements"
  clang::Expr *NumArrayElementsExpr = clang::IntegerLiteral::Create(C,
      llvm::APInt(C.getTypeSize(C.IntTy), NumArrayElements), C.IntTy, Loc);

  clang::BinaryOperator *Cond =
      new(C) clang::BinaryOperator(RefrsIntIter,
                                   NumArrayElementsExpr,
                                   clang::BO_LT,
                                   C.IntTy,
                                   clang::VK_RValue,
                                   clang::OK_Ordinary,
                                   Loc,
                                   false);

  // Inc -> "rsIntIter++"
  clang::UnaryOperator *Inc =
      new(C) clang::UnaryOperator(RefrsIntIter,
                                  clang::UO_PostInc,
                                  C.IntTy,
                                  clang::VK_RValue,
                                  clang::OK_Ordinary,
                                  Loc);

  // Body -> "rsClearObject(&VD[rsIntIter]);"
  // Destructor loop operates on individual array elements

  clang::Expr *RefRSArrPtr =
      clang::ImplicitCastExpr::Create(C,
          C.getPointerType(BaseType->getCanonicalTypeInternal()),
          clang::CK_ArrayToPointerDecay,
          RefRSArr,
          NULL,
          clang::VK_RValue);

  clang::Expr *RefRSArrPtrSubscript =
      new(C) clang::ArraySubscriptExpr(RefRSArrPtr,
                                       RefrsIntIter,
                                       BaseType->getCanonicalTypeInternal(),
                                       clang::VK_RValue,
                                       clang::OK_Ordinary,
                                       Loc);

  RSExportPrimitiveType::DataType DT =
      RSExportPrimitiveType::GetRSSpecificType(BaseType);

  clang::Stmt *RSClearObjectCall = NULL;
  if (BaseType->isArrayType()) {
    RSClearObjectCall =
        ClearArrayRSObject(C, DC, RefRSArrPtrSubscript, StartLoc, Loc);
  } else if (DT == RSExportPrimitiveType::DataTypeUnknown) {
    RSClearObjectCall =
        ClearStructRSObject(C, DC, RefRSArrPtrSubscript, StartLoc, Loc);
  } else {
    RSClearObjectCall = ClearSingleRSObject(C, RefRSArrPtrSubscript, Loc);
  }

  clang::ForStmt *DestructorLoop =
      new(C) clang::ForStmt(C,
                            Init,
                            Cond,
                            NULL,  // no condVar
                            Inc,
                            RSClearObjectCall,
                            Loc,
                            Loc,
                            Loc);

  StmtArray[StmtCtr++] = DestructorLoop;
  slangAssert(StmtCtr == 2);

  clang::CompoundStmt *CS = new(C) clang::CompoundStmt(
      C, llvm::makeArrayRef(StmtArray, StmtCtr), Loc, Loc);

  return CS;
}

static unsigned CountRSObjectTypes(clang::ASTContext &C,
                                   const clang::Type *T,
                                   clang::SourceLocation Loc) {
  slangAssert(T);
  unsigned RSObjectCount = 0;

  if (T->isArrayType()) {
    return CountRSObjectTypes(C, T->getArrayElementTypeNoTypeQual(), Loc);
  }

  RSExportPrimitiveType::DataType DT =
      RSExportPrimitiveType::GetRSSpecificType(T);
  if (DT != RSExportPrimitiveType::DataTypeUnknown) {
    return (RSExportPrimitiveType::IsRSObjectType(DT) ? 1 : 0);
  }

  if (T->isUnionType()) {
    clang::RecordDecl *RD = T->getAsUnionType()->getDecl();
    RD = RD->getDefinition();
    for (clang::RecordDecl::field_iterator FI = RD->field_begin(),
           FE = RD->field_end();
         FI != FE;
         FI++) {
      const clang::FieldDecl *FD = *FI;
      const clang::Type *FT = RSExportType::GetTypeOfDecl(FD);
      if (CountRSObjectTypes(C, FT, Loc)) {
        slangAssert(false && "can't have unions with RS object types!");
        return 0;
      }
    }
  }

  if (!T->isStructureType()) {
    return 0;
  }

  clang::RecordDecl *RD = T->getAsStructureType()->getDecl();
  RD = RD->getDefinition();
  for (clang::RecordDecl::field_iterator FI = RD->field_begin(),
         FE = RD->field_end();
       FI != FE;
       FI++) {
    const clang::FieldDecl *FD = *FI;
    const clang::Type *FT = RSExportType::GetTypeOfDecl(FD);
    if (CountRSObjectTypes(C, FT, Loc)) {
      // Sub-structs should only count once (as should arrays, etc.)
      RSObjectCount++;
    }
  }

  return RSObjectCount;
}

static clang::Stmt *ClearStructRSObject(
    clang::ASTContext &C,
    clang::DeclContext *DC,
    clang::Expr *RefRSStruct,
    clang::SourceLocation StartLoc,
    clang::SourceLocation Loc) {
  const clang::Type *BaseType = RefRSStruct->getType().getTypePtr();

  slangAssert(!BaseType->isArrayType());

  // Structs should show up as unknown primitive types
  slangAssert(RSExportPrimitiveType::GetRSSpecificType(BaseType) ==
              RSExportPrimitiveType::DataTypeUnknown);

  unsigned FieldsToDestroy = CountRSObjectTypes(C, BaseType, Loc);
  slangAssert(FieldsToDestroy != 0);

  unsigned StmtCount = 0;
  clang::Stmt **StmtArray = new clang::Stmt*[FieldsToDestroy];
  for (unsigned i = 0; i < FieldsToDestroy; i++) {
    StmtArray[i] = NULL;
  }

  // Populate StmtArray by creating a destructor for each RS object field
  clang::RecordDecl *RD = BaseType->getAsStructureType()->getDecl();
  RD = RD->getDefinition();
  for (clang::RecordDecl::field_iterator FI = RD->field_begin(),
         FE = RD->field_end();
       FI != FE;
       FI++) {
    // We just look through all field declarations to see if we find a
    // declaration for an RS object type (or an array of one).
    bool IsArrayType = false;
    clang::FieldDecl *FD = *FI;
    const clang::Type *FT = RSExportType::GetTypeOfDecl(FD);
    const clang::Type *OrigType = FT;
    while (FT && FT->isArrayType()) {
      FT = FT->getArrayElementTypeNoTypeQual();
      IsArrayType = true;
    }

    if (RSExportPrimitiveType::IsRSObjectType(FT)) {
      clang::DeclAccessPair FoundDecl =
          clang::DeclAccessPair::make(FD, clang::AS_none);
      clang::MemberExpr *RSObjectMember =
          clang::MemberExpr::Create(C,
                                    RefRSStruct,
                                    false,
                                    clang::NestedNameSpecifierLoc(),
                                    clang::SourceLocation(),
                                    FD,
                                    FoundDecl,
                                    clang::DeclarationNameInfo(),
                                    NULL,
                                    OrigType->getCanonicalTypeInternal(),
                                    clang::VK_RValue,
                                    clang::OK_Ordinary);

      slangAssert(StmtCount < FieldsToDestroy);

      if (IsArrayType) {
        StmtArray[StmtCount++] = ClearArrayRSObject(C,
                                                    DC,
                                                    RSObjectMember,
                                                    StartLoc,
                                                    Loc);
      } else {
        StmtArray[StmtCount++] = ClearSingleRSObject(C,
                                                     RSObjectMember,
                                                     Loc);
      }
    } else if (FT->isStructureType() && CountRSObjectTypes(C, FT, Loc)) {
      // In this case, we have a nested struct. We may not end up filling all
      // of the spaces in StmtArray (sub-structs should handle themselves
      // with separate compound statements).
      clang::DeclAccessPair FoundDecl =
          clang::DeclAccessPair::make(FD, clang::AS_none);
      clang::MemberExpr *RSObjectMember =
          clang::MemberExpr::Create(C,
                                    RefRSStruct,
                                    false,
                                    clang::NestedNameSpecifierLoc(),
                                    clang::SourceLocation(),
                                    FD,
                                    FoundDecl,
                                    clang::DeclarationNameInfo(),
                                    NULL,
                                    OrigType->getCanonicalTypeInternal(),
                                    clang::VK_RValue,
                                    clang::OK_Ordinary);

      if (IsArrayType) {
        StmtArray[StmtCount++] = ClearArrayRSObject(C,
                                                    DC,
                                                    RSObjectMember,
                                                    StartLoc,
                                                    Loc);
      } else {
        StmtArray[StmtCount++] = ClearStructRSObject(C,
                                                     DC,
                                                     RSObjectMember,
                                                     StartLoc,
                                                     Loc);
      }
    }
  }

  slangAssert(StmtCount > 0);
  clang::CompoundStmt *CS = new(C) clang::CompoundStmt(
      C, llvm::makeArrayRef(StmtArray, StmtCount), Loc, Loc);

  delete [] StmtArray;

  return CS;
}

static clang::Stmt *CreateSingleRSSetObject(clang::ASTContext &C,
                                            clang::Expr *DstExpr,
                                            clang::Expr *SrcExpr,
                                            clang::SourceLocation StartLoc,
                                            clang::SourceLocation Loc) {
  const clang::Type *T = DstExpr->getType().getTypePtr();
  clang::FunctionDecl *SetObjectFD = RSObjectRefCount::GetRSSetObjectFD(T);
  slangAssert((SetObjectFD != NULL) &&
              "rsSetObject doesn't cover all RS object types");

  clang::QualType SetObjectFDType = SetObjectFD->getType();
  clang::QualType SetObjectFDArgType[2];
  SetObjectFDArgType[0] = SetObjectFD->getParamDecl(0)->getOriginalType();
  SetObjectFDArgType[1] = SetObjectFD->getParamDecl(1)->getOriginalType();

  clang::Expr *RefRSSetObjectFD =
      clang::DeclRefExpr::Create(C,
                                 clang::NestedNameSpecifierLoc(),
                                 clang::SourceLocation(),
                                 SetObjectFD,
                                 false,
                                 Loc,
                                 SetObjectFDType,
                                 clang::VK_RValue,
                                 NULL);

  clang::Expr *RSSetObjectFP =
      clang::ImplicitCastExpr::Create(C,
                                      C.getPointerType(SetObjectFDType),
                                      clang::CK_FunctionToPointerDecay,
                                      RefRSSetObjectFD,
                                      NULL,
                                      clang::VK_RValue);

  llvm::SmallVector<clang::Expr*, 2> ArgList;
  ArgList.push_back(new(C) clang::UnaryOperator(DstExpr,
                                                clang::UO_AddrOf,
                                                SetObjectFDArgType[0],
                                                clang::VK_RValue,
                                                clang::OK_Ordinary,
                                                Loc));
  ArgList.push_back(SrcExpr);

  clang::CallExpr *RSSetObjectCall =
      new(C) clang::CallExpr(C,
                             RSSetObjectFP,
                             ArgList,
                             SetObjectFD->getCallResultType(),
                             clang::VK_RValue,
                             Loc);

  return RSSetObjectCall;
}

static clang::Stmt *CreateStructRSSetObject(clang::ASTContext &C,
                                            clang::Expr *LHS,
                                            clang::Expr *RHS,
                                            clang::SourceLocation StartLoc,
                                            clang::SourceLocation Loc);

/*static clang::Stmt *CreateArrayRSSetObject(clang::ASTContext &C,
                                           clang::Expr *DstArr,
                                           clang::Expr *SrcArr,
                                           clang::SourceLocation StartLoc,
                                           clang::SourceLocation Loc) {
  clang::DeclContext *DC = NULL;
  const clang::Type *BaseType = DstArr->getType().getTypePtr();
  slangAssert(BaseType->isArrayType());

  int NumArrayElements = ArrayDim(BaseType);
  // Actually extract out the base RS object type for use later
  BaseType = BaseType->getArrayElementTypeNoTypeQual();

  clang::Stmt *StmtArray[2] = {NULL};
  int StmtCtr = 0;

  if (NumArrayElements <= 0) {
    return NULL;
  }

  // Create helper variable for iterating through elements
  clang::IdentifierInfo& II = C.Idents.get("rsIntIter");
  clang::VarDecl *IIVD =
      clang::VarDecl::Create(C,
                             DC,
                             StartLoc,
                             Loc,
                             &II,
                             C.IntTy,
                             C.getTrivialTypeSourceInfo(C.IntTy),
                             clang::SC_None,
                             clang::SC_None);
  clang::Decl *IID = (clang::Decl *)IIVD;

  clang::DeclGroupRef DGR = clang::DeclGroupRef::Create(C, &IID, 1);
  StmtArray[StmtCtr++] = new(C) clang::DeclStmt(DGR, Loc, Loc);

  // Form the actual loop
  // for (Init; Cond; Inc)
  //   RSSetObjectCall;

  // Init -> "rsIntIter = 0"
  clang::DeclRefExpr *RefrsIntIter =
      clang::DeclRefExpr::Create(C,
                                 clang::NestedNameSpecifierLoc(),
                                 IIVD,
                                 Loc,
                                 C.IntTy,
                                 clang::VK_RValue,
                                 NULL);

  clang::Expr *Int0 = clang::IntegerLiteral::Create(C,
      llvm::APInt(C.getTypeSize(C.IntTy), 0), C.IntTy, Loc);

  clang::BinaryOperator *Init =
      new(C) clang::BinaryOperator(RefrsIntIter,
                                   Int0,
                                   clang::BO_Assign,
                                   C.IntTy,
                                   clang::VK_RValue,
                                   clang::OK_Ordinary,
                                   Loc);

  // Cond -> "rsIntIter < NumArrayElements"
  clang::Expr *NumArrayElementsExpr = clang::IntegerLiteral::Create(C,
      llvm::APInt(C.getTypeSize(C.IntTy), NumArrayElements), C.IntTy, Loc);

  clang::BinaryOperator *Cond =
      new(C) clang::BinaryOperator(RefrsIntIter,
                                   NumArrayElementsExpr,
                                   clang::BO_LT,
                                   C.IntTy,
                                   clang::VK_RValue,
                                   clang::OK_Ordinary,
                                   Loc);

  // Inc -> "rsIntIter++"
  clang::UnaryOperator *Inc =
      new(C) clang::UnaryOperator(RefrsIntIter,
                                  clang::UO_PostInc,
                                  C.IntTy,
                                  clang::VK_RValue,
                                  clang::OK_Ordinary,
                                  Loc);

  // Body -> "rsSetObject(&Dst[rsIntIter], Src[rsIntIter]);"
  // Loop operates on individual array elements

  clang::Expr *DstArrPtr =
      clang::ImplicitCastExpr::Create(C,
          C.getPointerType(BaseType->getCanonicalTypeInternal()),
          clang::CK_ArrayToPointerDecay,
          DstArr,
          NULL,
          clang::VK_RValue);

  clang::Expr *DstArrPtrSubscript =
      new(C) clang::ArraySubscriptExpr(DstArrPtr,
                                       RefrsIntIter,
                                       BaseType->getCanonicalTypeInternal(),
                                       clang::VK_RValue,
                                       clang::OK_Ordinary,
                                       Loc);

  clang::Expr *SrcArrPtr =
      clang::ImplicitCastExpr::Create(C,
          C.getPointerType(BaseType->getCanonicalTypeInternal()),
          clang::CK_ArrayToPointerDecay,
          SrcArr,
          NULL,
          clang::VK_RValue);

  clang::Expr *SrcArrPtrSubscript =
      new(C) clang::ArraySubscriptExpr(SrcArrPtr,
                                       RefrsIntIter,
                                       BaseType->getCanonicalTypeInternal(),
                                       clang::VK_RValue,
                                       clang::OK_Ordinary,
                                       Loc);

  RSExportPrimitiveType::DataType DT =
      RSExportPrimitiveType::GetRSSpecificType(BaseType);

  clang::Stmt *RSSetObjectCall = NULL;
  if (BaseType->isArrayType()) {
    RSSetObjectCall = CreateArrayRSSetObject(C, DstArrPtrSubscript,
                                             SrcArrPtrSubscript,
                                             StartLoc, Loc);
  } else if (DT == RSExportPrimitiveType::DataTypeUnknown) {
    RSSetObjectCall = CreateStructRSSetObject(C, DstArrPtrSubscript,
                                              SrcArrPtrSubscript,
                                              StartLoc, Loc);
  } else {
    RSSetObjectCall = CreateSingleRSSetObject(C, DstArrPtrSubscript,
                                              SrcArrPtrSubscript,
                                              StartLoc, Loc);
  }

  clang::ForStmt *DestructorLoop =
      new(C) clang::ForStmt(C,
                            Init,
                            Cond,
                            NULL,  // no condVar
                            Inc,
                            RSSetObjectCall,
                            Loc,
                            Loc,
                            Loc);

  StmtArray[StmtCtr++] = DestructorLoop;
  slangAssert(StmtCtr == 2);

  clang::CompoundStmt *CS =
      new(C) clang::CompoundStmt(C, StmtArray, StmtCtr, Loc, Loc);

  return CS;
} */

static clang::Stmt *CreateStructRSSetObject(clang::ASTContext &C,
                                            clang::Expr *LHS,
                                            clang::Expr *RHS,
                                            clang::SourceLocation StartLoc,
                                            clang::SourceLocation Loc) {
  clang::QualType QT = LHS->getType();
  const clang::Type *T = QT.getTypePtr();
  slangAssert(T->isStructureType());
  slangAssert(!RSExportPrimitiveType::IsRSObjectType(T));

  // Keep an extra slot for the original copy (memcpy)
  unsigned FieldsToSet = CountRSObjectTypes(C, T, Loc) + 1;

  unsigned StmtCount = 0;
  clang::Stmt **StmtArray = new clang::Stmt*[FieldsToSet];
  for (unsigned i = 0; i < FieldsToSet; i++) {
    StmtArray[i] = NULL;
  }

  clang::RecordDecl *RD = T->getAsStructureType()->getDecl();
  RD = RD->getDefinition();
  for (clang::RecordDecl::field_iterator FI = RD->field_begin(),
         FE = RD->field_end();
       FI != FE;
       FI++) {
    bool IsArrayType = false;
    clang::FieldDecl *FD = *FI;
    const clang::Type *FT = RSExportType::GetTypeOfDecl(FD);
    const clang::Type *OrigType = FT;

    if (!CountRSObjectTypes(C, FT, Loc)) {
      // Skip to next if we don't have any viable RS object types
      continue;
    }

    clang::DeclAccessPair FoundDecl =
        clang::DeclAccessPair::make(FD, clang::AS_none);
    clang::MemberExpr *DstMember =
        clang::MemberExpr::Create(C,
                                  LHS,
                                  false,
                                  clang::NestedNameSpecifierLoc(),
                                  clang::SourceLocation(),
                                  FD,
                                  FoundDecl,
                                  clang::DeclarationNameInfo(),
                                  NULL,
                                  OrigType->getCanonicalTypeInternal(),
                                  clang::VK_RValue,
                                  clang::OK_Ordinary);

    clang::MemberExpr *SrcMember =
        clang::MemberExpr::Create(C,
                                  RHS,
                                  false,
                                  clang::NestedNameSpecifierLoc(),
                                  clang::SourceLocation(),
                                  FD,
                                  FoundDecl,
                                  clang::DeclarationNameInfo(),
                                  NULL,
                                  OrigType->getCanonicalTypeInternal(),
                                  clang::VK_RValue,
                                  clang::OK_Ordinary);

    if (FT->isArrayType()) {
      FT = FT->getArrayElementTypeNoTypeQual();
      IsArrayType = true;
    }

    RSExportPrimitiveType::DataType DT =
        RSExportPrimitiveType::GetRSSpecificType(FT);

    if (IsArrayType) {
      clang::DiagnosticsEngine &DiagEngine = C.getDiagnostics();
      DiagEngine.Report(
        clang::FullSourceLoc(Loc, C.getSourceManager()),
        DiagEngine.getCustomDiagID(
          clang::DiagnosticsEngine::Error,
          "Arrays of RS object types within structures cannot be copied"));
      // TODO(srhines): Support setting arrays of RS objects
      // StmtArray[StmtCount++] =
      //    CreateArrayRSSetObject(C, DstMember, SrcMember, StartLoc, Loc);
    } else if (DT == RSExportPrimitiveType::DataTypeUnknown) {
      StmtArray[StmtCount++] =
          CreateStructRSSetObject(C, DstMember, SrcMember, StartLoc, Loc);
    } else if (RSExportPrimitiveType::IsRSObjectType(DT)) {
      StmtArray[StmtCount++] =
          CreateSingleRSSetObject(C, DstMember, SrcMember, StartLoc, Loc);
    } else {
      slangAssert(false);
    }
  }

  slangAssert(StmtCount < FieldsToSet);

  // We still need to actually do the overall struct copy. For simplicity,
  // we just do a straight-up assignment (which will still preserve all
  // the proper RS object reference counts).
  clang::BinaryOperator *CopyStruct =
      new(C) clang::BinaryOperator(LHS, RHS, clang::BO_Assign, QT,
                                   clang::VK_RValue, clang::OK_Ordinary, Loc,
                                   false);
  StmtArray[StmtCount++] = CopyStruct;

  clang::CompoundStmt *CS = new(C) clang::CompoundStmt(
      C, llvm::makeArrayRef(StmtArray, StmtCount), Loc, Loc);

  delete [] StmtArray;

  return CS;
}

}  // namespace

void RSObjectRefCount::Scope::ReplaceRSObjectAssignment(
    clang::BinaryOperator *AS) {

  clang::QualType QT = AS->getType();

  clang::ASTContext &C = RSObjectRefCount::GetRSSetObjectFD(
      RSExportPrimitiveType::DataTypeRSFont)->getASTContext();

  clang::SourceLocation Loc = AS->getExprLoc();
  clang::SourceLocation StartLoc = AS->getLHS()->getExprLoc();
  clang::Stmt *UpdatedStmt = NULL;

  if (!RSExportPrimitiveType::IsRSObjectType(QT.getTypePtr())) {
    // By definition, this is a struct assignment if we get here
    UpdatedStmt =
        CreateStructRSSetObject(C, AS->getLHS(), AS->getRHS(), StartLoc, Loc);
  } else {
    UpdatedStmt =
        CreateSingleRSSetObject(C, AS->getLHS(), AS->getRHS(), StartLoc, Loc);
  }

  RSASTReplace R(C);
  R.ReplaceStmt(mCS, AS, UpdatedStmt);
  return;
}

void RSObjectRefCount::Scope::AppendRSObjectInit(
    clang::VarDecl *VD,
    clang::DeclStmt *DS,
    RSExportPrimitiveType::DataType DT,
    clang::Expr *InitExpr) {
  slangAssert(VD);

  if (!InitExpr) {
    return;
  }

  clang::ASTContext &C = RSObjectRefCount::GetRSSetObjectFD(
      RSExportPrimitiveType::DataTypeRSFont)->getASTContext();
  clang::SourceLocation Loc = RSObjectRefCount::GetRSSetObjectFD(
      RSExportPrimitiveType::DataTypeRSFont)->getLocation();
  clang::SourceLocation StartLoc = RSObjectRefCount::GetRSSetObjectFD(
      RSExportPrimitiveType::DataTypeRSFont)->getInnerLocStart();

  if (DT == RSExportPrimitiveType::DataTypeIsStruct) {
    const clang::Type *T = RSExportType::GetTypeOfDecl(VD);
    clang::DeclRefExpr *RefRSVar =
        clang::DeclRefExpr::Create(C,
                                   clang::NestedNameSpecifierLoc(),
                                   clang::SourceLocation(),
                                   VD,
                                   false,
                                   Loc,
                                   T->getCanonicalTypeInternal(),
                                   clang::VK_RValue,
                                   NULL);

    clang::Stmt *RSSetObjectOps =
        CreateStructRSSetObject(C, RefRSVar, InitExpr, StartLoc, Loc);

    std::list<clang::Stmt*> StmtList;
    StmtList.push_back(RSSetObjectOps);
    AppendAfterStmt(C, mCS, DS, StmtList);
    return;
  }

  clang::FunctionDecl *SetObjectFD = RSObjectRefCount::GetRSSetObjectFD(DT);
  slangAssert((SetObjectFD != NULL) &&
              "rsSetObject doesn't cover all RS object types");

  clang::QualType SetObjectFDType = SetObjectFD->getType();
  clang::QualType SetObjectFDArgType[2];
  SetObjectFDArgType[0] = SetObjectFD->getParamDecl(0)->getOriginalType();
  SetObjectFDArgType[1] = SetObjectFD->getParamDecl(1)->getOriginalType();

  clang::Expr *RefRSSetObjectFD =
      clang::DeclRefExpr::Create(C,
                                 clang::NestedNameSpecifierLoc(),
                                 clang::SourceLocation(),
                                 SetObjectFD,
                                 false,
                                 Loc,
                                 SetObjectFDType,
                                 clang::VK_RValue,
                                 NULL);

  clang::Expr *RSSetObjectFP =
      clang::ImplicitCastExpr::Create(C,
                                      C.getPointerType(SetObjectFDType),
                                      clang::CK_FunctionToPointerDecay,
                                      RefRSSetObjectFD,
                                      NULL,
                                      clang::VK_RValue);

  const clang::Type *T = RSExportType::GetTypeOfDecl(VD);
  clang::DeclRefExpr *RefRSVar =
      clang::DeclRefExpr::Create(C,
                                 clang::NestedNameSpecifierLoc(),
                                 clang::SourceLocation(),
                                 VD,
                                 false,
                                 Loc,
                                 T->getCanonicalTypeInternal(),
                                 clang::VK_RValue,
                                 NULL);

  llvm::SmallVector<clang::Expr*, 2> ArgList;
  ArgList.push_back(new(C) clang::UnaryOperator(RefRSVar,
                                                clang::UO_AddrOf,
                                                SetObjectFDArgType[0],
                                                clang::VK_RValue,
                                                clang::OK_Ordinary,
                                                Loc));
  ArgList.push_back(InitExpr);

  clang::CallExpr *RSSetObjectCall =
      new(C) clang::CallExpr(C,
                             RSSetObjectFP,
                             ArgList,
                             SetObjectFD->getCallResultType(),
                             clang::VK_RValue,
                             Loc);

  std::list<clang::Stmt*> StmtList;
  StmtList.push_back(RSSetObjectCall);
  AppendAfterStmt(C, mCS, DS, StmtList);

  return;
}

void RSObjectRefCount::Scope::InsertLocalVarDestructors() {
  for (std::list<clang::VarDecl*>::const_iterator I = mRSO.begin(),
          E = mRSO.end();
        I != E;
        I++) {
    clang::VarDecl *VD = *I;
    clang::Stmt *RSClearObjectCall = ClearRSObject(VD, VD->getDeclContext());
    if (RSClearObjectCall) {
      DestructorVisitor DV((*mRSO.begin())->getASTContext(),
                           mCS,
                           RSClearObjectCall,
                           VD->getSourceRange().getBegin());
      DV.Visit(mCS);
      DV.InsertDestructors();
    }
  }
  return;
}

clang::Stmt *RSObjectRefCount::Scope::ClearRSObject(
    clang::VarDecl *VD,
    clang::DeclContext *DC) {
  slangAssert(VD);
  clang::ASTContext &C = VD->getASTContext();
  clang::SourceLocation Loc = VD->getLocation();
  clang::SourceLocation StartLoc = VD->getInnerLocStart();
  const clang::Type *T = RSExportType::GetTypeOfDecl(VD);

  // Reference expr to target RS object variable
  clang::DeclRefExpr *RefRSVar =
      clang::DeclRefExpr::Create(C,
                                 clang::NestedNameSpecifierLoc(),
                                 clang::SourceLocation(),
                                 VD,
                                 false,
                                 Loc,
                                 T->getCanonicalTypeInternal(),
                                 clang::VK_RValue,
                                 NULL);

  if (T->isArrayType()) {
    return ClearArrayRSObject(C, DC, RefRSVar, StartLoc, Loc);
  }

  RSExportPrimitiveType::DataType DT =
      RSExportPrimitiveType::GetRSSpecificType(T);

  if (DT == RSExportPrimitiveType::DataTypeUnknown ||
      DT == RSExportPrimitiveType::DataTypeIsStruct) {
    return ClearStructRSObject(C, DC, RefRSVar, StartLoc, Loc);
  }

  slangAssert((RSExportPrimitiveType::IsRSObjectType(DT)) &&
              "Should be RS object");

  return ClearSingleRSObject(C, RefRSVar, Loc);
}

bool RSObjectRefCount::InitializeRSObject(clang::VarDecl *VD,
                                          RSExportPrimitiveType::DataType *DT,
                                          clang::Expr **InitExpr) {
  slangAssert(VD && DT && InitExpr);
  const clang::Type *T = RSExportType::GetTypeOfDecl(VD);

  // Loop through array types to get to base type
  while (T && T->isArrayType()) {
    T = T->getArrayElementTypeNoTypeQual();
  }

  bool DataTypeIsStructWithRSObject = false;
  *DT = RSExportPrimitiveType::GetRSSpecificType(T);

  if (*DT == RSExportPrimitiveType::DataTypeUnknown) {
    if (RSExportPrimitiveType::IsStructureTypeWithRSObject(T)) {
      *DT = RSExportPrimitiveType::DataTypeIsStruct;
      DataTypeIsStructWithRSObject = true;
    } else {
      return false;
    }
  }

  bool DataTypeIsRSObject = false;
  if (DataTypeIsStructWithRSObject) {
    DataTypeIsRSObject = true;
  } else {
    DataTypeIsRSObject = RSExportPrimitiveType::IsRSObjectType(*DT);
  }
  *InitExpr = VD->getInit();

  if (!DataTypeIsRSObject && *InitExpr) {
    // If we already have an initializer for a matrix type, we are done.
    return DataTypeIsRSObject;
  }

  clang::Expr *ZeroInitializer =
      CreateZeroInitializerForRSSpecificType(*DT,
                                             VD->getASTContext(),
                                             VD->getLocation());

  if (ZeroInitializer) {
    ZeroInitializer->setType(T->getCanonicalTypeInternal());
    VD->setInit(ZeroInitializer);
  }

  return DataTypeIsRSObject;
}

clang::Expr *RSObjectRefCount::CreateZeroInitializerForRSSpecificType(
    RSExportPrimitiveType::DataType DT,
    clang::ASTContext &C,
    const clang::SourceLocation &Loc) {
  clang::Expr *Res = NULL;
  switch (DT) {
    case RSExportPrimitiveType::DataTypeIsStruct:
    case RSExportPrimitiveType::DataTypeRSElement:
    case RSExportPrimitiveType::DataTypeRSType:
    case RSExportPrimitiveType::DataTypeRSAllocation:
    case RSExportPrimitiveType::DataTypeRSSampler:
    case RSExportPrimitiveType::DataTypeRSScript:
    case RSExportPrimitiveType::DataTypeRSMesh:
    case RSExportPrimitiveType::DataTypeRSPath:
    case RSExportPrimitiveType::DataTypeRSProgramFragment:
    case RSExportPrimitiveType::DataTypeRSProgramVertex:
    case RSExportPrimitiveType::DataTypeRSProgramRaster:
    case RSExportPrimitiveType::DataTypeRSProgramStore:
    case RSExportPrimitiveType::DataTypeRSFont: {
      //    (ImplicitCastExpr 'nullptr_t'
      //      (IntegerLiteral 0)))
      llvm::APInt Zero(C.getTypeSize(C.IntTy), 0);
      clang::Expr *Int0 = clang::IntegerLiteral::Create(C, Zero, C.IntTy, Loc);
      clang::Expr *CastToNull =
          clang::ImplicitCastExpr::Create(C,
                                          C.NullPtrTy,
                                          clang::CK_IntegralToPointer,
                                          Int0,
                                          NULL,
                                          clang::VK_RValue);

      llvm::SmallVector<clang::Expr*, 1>InitList;
      InitList.push_back(CastToNull);

      Res = new(C) clang::InitListExpr(C, Loc, InitList, Loc);
      break;
    }
    case RSExportPrimitiveType::DataTypeRSMatrix2x2:
    case RSExportPrimitiveType::DataTypeRSMatrix3x3:
    case RSExportPrimitiveType::DataTypeRSMatrix4x4: {
      // RS matrix is not completely an RS object. They hold data by themselves.
      // (InitListExpr rs_matrix2x2
      //   (InitListExpr float[4]
      //     (FloatingLiteral 0)
      //     (FloatingLiteral 0)
      //     (FloatingLiteral 0)
      //     (FloatingLiteral 0)))
      clang::QualType FloatTy = C.FloatTy;
      // Constructor sets value to 0.0f by default
      llvm::APFloat Val(C.getFloatTypeSemantics(FloatTy));
      clang::FloatingLiteral *Float0Val =
          clang::FloatingLiteral::Create(C,
                                         Val,
                                         /* isExact = */true,
                                         FloatTy,
                                         Loc);

      unsigned N = 0;
      if (DT == RSExportPrimitiveType::DataTypeRSMatrix2x2)
        N = 2;
      else if (DT == RSExportPrimitiveType::DataTypeRSMatrix3x3)
        N = 3;
      else if (DT == RSExportPrimitiveType::DataTypeRSMatrix4x4)
        N = 4;
      unsigned N_2 = N * N;

      // Assume we are going to be allocating 16 elements, since 4x4 is max.
      llvm::SmallVector<clang::Expr*, 16> InitVals;
      for (unsigned i = 0; i < N_2; i++)
        InitVals.push_back(Float0Val);
      clang::Expr *InitExpr =
          new(C) clang::InitListExpr(C, Loc, InitVals, Loc);
      InitExpr->setType(C.getConstantArrayType(FloatTy,
                                               llvm::APInt(32, N_2),
                                               clang::ArrayType::Normal,
                                               /* EltTypeQuals = */0));
      llvm::SmallVector<clang::Expr*, 1> InitExprVec;
      InitExprVec.push_back(InitExpr);

      Res = new(C) clang::InitListExpr(C, Loc, InitExprVec, Loc);
      break;
    }
    case RSExportPrimitiveType::DataTypeUnknown:
    case RSExportPrimitiveType::DataTypeFloat16:
    case RSExportPrimitiveType::DataTypeFloat32:
    case RSExportPrimitiveType::DataTypeFloat64:
    case RSExportPrimitiveType::DataTypeSigned8:
    case RSExportPrimitiveType::DataTypeSigned16:
    case RSExportPrimitiveType::DataTypeSigned32:
    case RSExportPrimitiveType::DataTypeSigned64:
    case RSExportPrimitiveType::DataTypeUnsigned8:
    case RSExportPrimitiveType::DataTypeUnsigned16:
    case RSExportPrimitiveType::DataTypeUnsigned32:
    case RSExportPrimitiveType::DataTypeUnsigned64:
    case RSExportPrimitiveType::DataTypeBoolean:
    case RSExportPrimitiveType::DataTypeUnsigned565:
    case RSExportPrimitiveType::DataTypeUnsigned5551:
    case RSExportPrimitiveType::DataTypeUnsigned4444:
    case RSExportPrimitiveType::DataTypeMax: {
      slangAssert(false && "Not RS object type!");
    }
    // No default case will enable compiler detecting the missing cases
  }

  return Res;
}

void RSObjectRefCount::VisitDeclStmt(clang::DeclStmt *DS) {
  for (clang::DeclStmt::decl_iterator I = DS->decl_begin(), E = DS->decl_end();
       I != E;
       I++) {
    clang::Decl *D = *I;
    if (D->getKind() == clang::Decl::Var) {
      clang::VarDecl *VD = static_cast<clang::VarDecl*>(D);
      RSExportPrimitiveType::DataType DT =
          RSExportPrimitiveType::DataTypeUnknown;
      clang::Expr *InitExpr = NULL;
      if (InitializeRSObject(VD, &DT, &InitExpr)) {
        // We need to zero-init all RS object types (including matrices), ...
        getCurrentScope()->AppendRSObjectInit(VD, DS, DT, InitExpr);
        // ... but, only add to the list of RS objects if we have some
        // non-matrix RS object fields.
        if (CountRSObjectTypes(mCtx, VD->getType().getTypePtr(),
                               VD->getLocation())) {
          getCurrentScope()->addRSObject(VD);
        }
      }
    }
  }
  return;
}

void RSObjectRefCount::VisitCompoundStmt(clang::CompoundStmt *CS) {
  if (!CS->body_empty()) {
    // Push a new scope
    Scope *S = new Scope(CS);
    mScopeStack.push(S);

    VisitStmt(CS);

    // Destroy the scope
    slangAssert((getCurrentScope() == S) && "Corrupted scope stack!");
    S->InsertLocalVarDestructors();
    mScopeStack.pop();
    delete S;
  }
  return;
}

void RSObjectRefCount::VisitBinAssign(clang::BinaryOperator *AS) {
  clang::QualType QT = AS->getType();

  if (CountRSObjectTypes(mCtx, QT.getTypePtr(), AS->getExprLoc())) {
    getCurrentScope()->ReplaceRSObjectAssignment(AS);
  }

  return;
}

void RSObjectRefCount::VisitStmt(clang::Stmt *S) {
  for (clang::Stmt::child_iterator I = S->child_begin(), E = S->child_end();
       I != E;
       I++) {
    if (clang::Stmt *Child = *I) {
      Visit(Child);
    }
  }
  return;
}

// This function walks the list of global variables and (potentially) creates
// a single global static destructor function that properly decrements
// reference counts on the contained RS object types.
clang::FunctionDecl *RSObjectRefCount::CreateStaticGlobalDtor() {
  Init();

  clang::DeclContext *DC = mCtx.getTranslationUnitDecl();
  clang::SourceLocation loc;

  llvm::StringRef SR(".rs.dtor");
  clang::IdentifierInfo &II = mCtx.Idents.get(SR);
  clang::DeclarationName N(&II);
  clang::FunctionProtoType::ExtProtoInfo EPI;
  clang::QualType T = mCtx.getFunctionType(mCtx.VoidTy,
      llvm::ArrayRef<clang::QualType>(), EPI);
  clang::FunctionDecl *FD = NULL;

  // Generate rsClearObject() call chains for every global variable
  // (whether static or extern).
  std::list<clang::Stmt *> StmtList;
  for (clang::DeclContext::decl_iterator I = DC->decls_begin(),
          E = DC->decls_end(); I != E; I++) {
    clang::VarDecl *VD = llvm::dyn_cast<clang::VarDecl>(*I);
    if (VD) {
      if (CountRSObjectTypes(mCtx, VD->getType().getTypePtr(), loc)) {
        if (!FD) {
          // Only create FD if we are going to use it.
          FD = clang::FunctionDecl::Create(mCtx, DC, loc, loc, N, T, NULL,
                                           clang::SC_None);
        }
        // Make sure to create any helpers within the function's DeclContext,
        // not the one associated with the global translation unit.
        clang::Stmt *RSClearObjectCall = Scope::ClearRSObject(VD, FD);
        StmtList.push_back(RSClearObjectCall);
      }
    }
  }

  // Nothing needs to be destroyed, so don't emit a dtor.
  if (StmtList.empty()) {
    return NULL;
  }

  clang::CompoundStmt *CS = BuildCompoundStmt(mCtx, StmtList, loc);

  FD->setBody(CS);

  return FD;
}

}  // namespace slang
