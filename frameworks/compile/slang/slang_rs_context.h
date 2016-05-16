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

#ifndef _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_CONTEXT_H_  // NOLINT
#define _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_CONTEXT_H_

#include <cstdio>
#include <list>
#include <map>
#include <string>

#include "clang/Lex/Preprocessor.h"
#include "clang/AST/Mangle.h"

#include "llvm/ADT/OwningPtr.h"
#include "llvm/ADT/StringSet.h"
#include "llvm/ADT/StringMap.h"

#include "slang_pragma_recorder.h"

namespace llvm {
  class LLVMContext;
  class DataLayout;
}   // namespace llvm

namespace clang {
  class VarDecl;
  class ASTContext;
  class TargetInfo;
  class FunctionDecl;
  class SourceManager;
}   // namespace clang

namespace slang {
  class RSExportable;
  class RSExportVar;
  class RSExportFunc;
  class RSExportForEach;
  class RSExportType;

class RSContext {
  typedef llvm::StringSet<> NeedExportVarSet;
  typedef llvm::StringSet<> NeedExportFuncSet;
  typedef llvm::StringSet<> NeedExportTypeSet;

 public:
  typedef std::list<RSExportable*> ExportableList;
  typedef std::list<RSExportVar*> ExportVarList;
  typedef std::list<RSExportFunc*> ExportFuncList;
  typedef std::list<RSExportForEach*> ExportForEachList;
  typedef llvm::StringMap<RSExportType*> ExportTypeMap;

 private:
  clang::Preprocessor &mPP;
  clang::ASTContext &mCtx;
  const clang::TargetInfo &mTarget;
  PragmaList *mPragmas;
  unsigned int mTargetAPI;
  std::vector<std::string> *mGeneratedFileNames;

  llvm::DataLayout *mDataLayout;
  llvm::LLVMContext &mLLVMContext;

  ExportableList mExportables;

  NeedExportTypeSet mNeedExportTypes;

  std::string *mLicenseNote;
  std::string mReflectJavaPackageName;
  std::string mReflectJavaPathName;

  std::string mRSPackageName;

  int version;

  bool mIsCompatLib;

  llvm::OwningPtr<clang::MangleContext> mMangleCtx;

  bool processExportVar(const clang::VarDecl *VD);
  bool processExportFunc(const clang::FunctionDecl *FD);
  bool processExportType(const llvm::StringRef &Name);

  void cleanupForEach();

  ExportVarList mExportVars;
  ExportFuncList mExportFuncs;
  ExportForEachList mExportForEach;
  ExportTypeMap mExportTypes;

 public:
  RSContext(clang::Preprocessor &PP,
            clang::ASTContext &Ctx,
            const clang::TargetInfo &Target,
            PragmaList *Pragmas,
            unsigned int TargetAPI,
            std::vector<std::string> *GeneratedFileNames);

  inline clang::Preprocessor &getPreprocessor() const { return mPP; }
  inline clang::ASTContext &getASTContext() const { return mCtx; }
  inline clang::MangleContext &getMangleContext() const {
    return *mMangleCtx;
  }
  inline const llvm::DataLayout *getDataLayout() const { return mDataLayout; }
  inline llvm::LLVMContext &getLLVMContext() const { return mLLVMContext; }
  inline const clang::SourceManager *getSourceManager() const {
    return &mPP.getSourceManager();
  }
  inline clang::DiagnosticsEngine *getDiagnostics() const {
    return &mPP.getDiagnostics();
  }
  inline unsigned int getTargetAPI() const {
    return mTargetAPI;
  }

  inline void setLicenseNote(const std::string &S) {
    mLicenseNote = new std::string(S);
  }
  inline const std::string *getLicenseNote() const { return mLicenseNote; }

  inline void addExportType(const std::string &S) {
    mNeedExportTypes.insert(S);
    return;
  }

  inline void setReflectJavaPackageName(const std::string &S) {
    mReflectJavaPackageName = S;
    return;
  }
  inline const std::string &getReflectJavaPackageName() const {
    return mReflectJavaPackageName;
  }

  inline void setRSPackageName(const std::string &S) {
    mRSPackageName = S;
    return;
  }
  inline const std::string &getRSPackageName() const {
    return mRSPackageName;
  }

  bool processExport();
  inline void newExportable(RSExportable *E) {
    if (E != NULL)
      mExportables.push_back(E);
  }
  typedef ExportableList::iterator exportable_iterator;
  exportable_iterator exportable_begin() {
    return mExportables.begin();
  }
  exportable_iterator exportable_end() {
    return mExportables.end();
  }

  typedef ExportVarList::const_iterator const_export_var_iterator;
  const_export_var_iterator export_vars_begin() const {
    return mExportVars.begin();
  }
  const_export_var_iterator export_vars_end() const {
    return mExportVars.end();
  }
  inline bool hasExportVar() const {
    return !mExportVars.empty();
  }

  typedef ExportFuncList::const_iterator const_export_func_iterator;
  const_export_func_iterator export_funcs_begin() const {
    return mExportFuncs.begin();
  }
  const_export_func_iterator export_funcs_end() const {
    return mExportFuncs.end();
  }
  inline bool hasExportFunc() const { return !mExportFuncs.empty(); }

  typedef ExportForEachList::const_iterator const_export_foreach_iterator;
  const_export_foreach_iterator export_foreach_begin() const {
    return mExportForEach.begin();
  }
  const_export_foreach_iterator export_foreach_end() const {
    return mExportForEach.end();
  }
  inline bool hasExportForEach() const { return !mExportForEach.empty(); }

  typedef ExportTypeMap::iterator export_type_iterator;
  typedef ExportTypeMap::const_iterator const_export_type_iterator;
  export_type_iterator export_types_begin() { return mExportTypes.begin(); }
  export_type_iterator export_types_end() { return mExportTypes.end(); }
  const_export_type_iterator export_types_begin() const {
    return mExportTypes.begin();
  }
  const_export_type_iterator export_types_end() const {
    return mExportTypes.end();
  }
  inline bool hasExportType() const { return !mExportTypes.empty(); }
  export_type_iterator findExportType(const llvm::StringRef &TypeName) {
    return mExportTypes.find(TypeName);
  }
  const_export_type_iterator findExportType(const llvm::StringRef &TypeName)
      const {
    return mExportTypes.find(TypeName);
  }

  // Insert the specified Typename/Type pair into the map. If the key already
  // exists in the map, return false and ignore the request, otherwise insert it
  // and return true.
  bool insertExportType(const llvm::StringRef &TypeName, RSExportType *Type);

  bool reflectToJava(const std::string &OutputPathBase,
                     const std::string &RSPackageName,
                     const std::string &InputFileName,
                     const std::string &OutputBCFileName);

  int getVersion() const { return version; }
  void setVersion(int v) {
    version = v;
    return;
  }

  bool isCompatLib() const { return mIsCompatLib; }

  void addPragma(const std::string &T, const std::string &V) {
    mPragmas->push_back(make_pair(T, V));
  }

  ~RSContext();
};

}   // namespace slang

#endif  // _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_CONTEXT_H_  NOLINT
