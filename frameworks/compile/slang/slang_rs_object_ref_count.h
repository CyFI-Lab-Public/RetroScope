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

#ifndef _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_OBJECT_REF_COUNT_H_  // NOLINT
#define _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_OBJECT_REF_COUNT_H_

#include <list>
#include <stack>

#include "clang/AST/StmtVisitor.h"

#include "slang_assert.h"
#include "slang_rs_export_type.h"

namespace clang {
  class Expr;
  class Stmt;
}

namespace slang {

// This class provides the overall reference counting mechanism for handling
// local variables of RS object types (rs_font, rs_allocation, ...). This
// class ensures that appropriate functions (rsSetObject, rsClearObject) are
// called at proper points in the object's lifetime.
// 1) Each local object of appropriate type must be zero-initialized (to
// prevent corruption) during subsequent rsSetObject()/rsClearObject() calls.
// 2) Assignments using these types must also be converted into the
// appropriate (possibly a series of) rsSetObject() calls.
// 3) Finally, each local object must call rsClearObject() when it goes out
// of scope.
class RSObjectRefCount : public clang::StmtVisitor<RSObjectRefCount> {
 private:
  class Scope {
   private:
    clang::CompoundStmt *mCS;      // Associated compound statement ({ ... })
    std::list<clang::VarDecl*> mRSO;  // Declared RS objects in this scope

   public:
    explicit Scope(clang::CompoundStmt *CS) : mCS(CS) {
      return;
    }

    inline void addRSObject(clang::VarDecl* VD) {
      mRSO.push_back(VD);
      return;
    }

    void ReplaceRSObjectAssignment(clang::BinaryOperator *AS);

    void AppendRSObjectInit(clang::VarDecl *VD,
                            clang::DeclStmt *DS,
                            RSExportPrimitiveType::DataType DT,
                            clang::Expr *InitExpr);

    void InsertLocalVarDestructors();

    static clang::Stmt *ClearRSObject(clang::VarDecl *VD,
                                      clang::DeclContext *DC);
  };

  clang::ASTContext &mCtx;
  std::stack<Scope*> mScopeStack;
  bool RSInitFD;

  // RSSetObjectFD and RSClearObjectFD holds FunctionDecl of rsSetObject()
  // and rsClearObject() in the current ASTContext.
  static clang::FunctionDecl *RSSetObjectFD[];
  static clang::FunctionDecl *RSClearObjectFD[];

  inline Scope *getCurrentScope() {
    return mScopeStack.top();
  }

  // Initialize RSSetObjectFD and RSClearObjectFD.
  static void GetRSRefCountingFunctions(clang::ASTContext &C);

  // Return false if the type of variable declared in VD does not contain
  // an RS object type.
  static bool InitializeRSObject(clang::VarDecl *VD,
                                 RSExportPrimitiveType::DataType *DT,
                                 clang::Expr **InitExpr);

  // Return a zero-initializer expr of the type DT. This processes both
  // RS matrix type and RS object type.
  static clang::Expr *CreateZeroInitializerForRSSpecificType(
      RSExportPrimitiveType::DataType DT,
      clang::ASTContext &C,
      const clang::SourceLocation &Loc);

 public:
  explicit RSObjectRefCount(clang::ASTContext &C)
      : mCtx(C),
        RSInitFD(false) {
    return;
  }

  void Init() {
    if (!RSInitFD) {
      GetRSRefCountingFunctions(mCtx);
      RSInitFD = true;
    }
    return;
  }

  static clang::FunctionDecl *GetRSSetObjectFD(
      RSExportPrimitiveType::DataType DT) {
    slangAssert(RSExportPrimitiveType::IsRSObjectType(DT));
    return RSSetObjectFD[(DT - RSExportPrimitiveType::FirstRSObjectType)];
  }

  static clang::FunctionDecl *GetRSSetObjectFD(const clang::Type *T) {
    return GetRSSetObjectFD(RSExportPrimitiveType::GetRSSpecificType(T));
  }

  static clang::FunctionDecl *GetRSClearObjectFD(
      RSExportPrimitiveType::DataType DT) {
    slangAssert(RSExportPrimitiveType::IsRSObjectType(DT));
    return RSClearObjectFD[(DT - RSExportPrimitiveType::FirstRSObjectType)];
  }

  static clang::FunctionDecl *GetRSClearObjectFD(const clang::Type *T) {
    return GetRSClearObjectFD(RSExportPrimitiveType::GetRSSpecificType(T));
  }

  void VisitStmt(clang::Stmt *S);
  void VisitDeclStmt(clang::DeclStmt *DS);
  void VisitCompoundStmt(clang::CompoundStmt *CS);
  void VisitBinAssign(clang::BinaryOperator *AS);
  // We believe that RS objects are never involved in CompoundAssignOperator.
  // I.e., rs_allocation foo; foo += bar;

  // Emit a global destructor to clean up RS objects.
  clang::FunctionDecl *CreateStaticGlobalDtor();
};

}  // namespace slang

#endif  // _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_OBJECT_REF_COUNT_H_  NOLINT
