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

#ifndef _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_REFLECTION_H_  // NOLINT
#define _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_REFLECTION_H_

#include <fstream>
#include <iostream>
#include <map>
#include <set>
#include <string>
#include <vector>

#include "llvm/ADT/StringExtras.h"

#include "slang_assert.h"
#include "slang_rs_export_type.h"

namespace slang {

  class RSContext;
  class RSExportVar;
  class RSExportFunc;
  class RSExportForEach;

class RSReflection {
 private:
  const RSContext *mRSContext;

  std::string mLastError;
  std::vector<std::string> *mGeneratedFileNames;

  inline void setError(const std::string &Error) { mLastError = Error; }

  class Context {
   private:
    static const char *const ApacheLicenseNote;

    bool mVerbose;

    std::string mOutputPathBase;

    std::string mInputRSFile;

    std::string mPackageName;
    std::string mRSPackageName;
    std::string mResourceId;
    std::string mPaddingPrefix;

    std::string mClassName;

    std::string mLicenseNote;

    std::string mIndent;

    int mPaddingFieldIndex;

    int mNextExportVarSlot;
    int mNextExportFuncSlot;
    int mNextExportForEachSlot;

    // A mapping from a field in a record type to its index in the rsType
    // instance. Only used when generates TypeClass (ScriptField_*).
    typedef std::map<const RSExportRecordType::Field*, unsigned>
        FieldIndexMapTy;
    FieldIndexMapTy mFieldIndexMap;
    // Field index of current processing TypeClass.
    unsigned mFieldIndex;

    inline void clear() {
      mClassName = "";
      mIndent = "";
      mPaddingFieldIndex = 1;
      mNextExportVarSlot = 0;
      mNextExportFuncSlot = 0;
      mNextExportForEachSlot = 0;
      return;
    }

    bool openClassFile(const std::string &ClassName,
                       std::string &ErrorMsg);

   public:
    typedef enum {
      AM_Public,
      AM_Protected,
      AM_Private,
      AM_PublicSynchronized
    } AccessModifier;

    bool mUseStdout;
    mutable std::ofstream mOF;

    // Generated RS Elements for type-checking code.
    std::set<std::string> mTypesToCheck;

    // Generated FieldPackers for unsigned setters/validation.
    std::set<std::string> mFieldPackerTypes;

    bool addTypeNameForElement(const std::string &TypeName);
    bool addTypeNameForFieldPacker(const std::string &TypeName);

    static const char *AccessModifierStr(AccessModifier AM);

    Context(const std::string &OutputPathBase,
            const std::string &InputRSFile,
            const std::string &PackageName,
            const std::string &RSPackageName,
            const std::string &ResourceId,
            const std::string &PaddingPrefix,
            bool UseStdout)
        : mVerbose(true),
          mOutputPathBase(OutputPathBase),
          mInputRSFile(InputRSFile),
          mPackageName(PackageName),
          mRSPackageName(RSPackageName),
          mResourceId(ResourceId),
          mPaddingPrefix(PaddingPrefix),
          mLicenseNote(ApacheLicenseNote),
          mUseStdout(UseStdout) {
      clear();
      resetFieldIndex();
      clearFieldIndexMap();
      return;
    }

    inline std::ostream &out() const {
      return ((mUseStdout) ? std::cout : mOF);
    }
    inline std::ostream &indent() const {
      out() << mIndent;
      return out();
    }

    inline void incIndentLevel() {
      mIndent.append(4, ' ');
      return;
    }

    inline void decIndentLevel() {
      slangAssert(getIndentLevel() > 0 && "No indent");
      mIndent.erase(0, 4);
      return;
    }

    inline int getIndentLevel() { return (mIndent.length() >> 2); }

    inline int getNextExportVarSlot() { return mNextExportVarSlot++; }

    inline int getNextExportFuncSlot() { return mNextExportFuncSlot++; }
    inline int getNextExportForEachSlot() { return mNextExportForEachSlot++; }

    // Will remove later due to field name information is not necessary for
    // C-reflect-to-Java
    inline std::string createPaddingField() {
      return mPaddingPrefix + llvm::itostr(mPaddingFieldIndex++);
    }

    inline void setLicenseNote(const std::string &LicenseNote) {
      mLicenseNote = LicenseNote;
    }

    bool startClass(AccessModifier AM,
                    bool IsStatic,
                    const std::string &ClassName,
                    const char *SuperClassName,
                    std::string &ErrorMsg);
    void endClass();

    void startFunction(AccessModifier AM,
                       bool IsStatic,
                       const char *ReturnType,
                       const std::string &FunctionName,
                       int Argc, ...);

    typedef std::vector<std::pair<std::string, std::string> > ArgTy;
    void startFunction(AccessModifier AM,
                       bool IsStatic,
                       const char *ReturnType,
                       const std::string &FunctionName,
                       const ArgTy &Args);
    void endFunction();

    void startBlock(bool ShouldIndent = false);
    void endBlock();

    inline const std::string &getPackageName() const { return mPackageName; }
    inline const std::string &getRSPackageName() const {
      return mRSPackageName;
    }
    inline const std::string &getClassName() const { return mClassName; }
    inline const std::string &getResourceId() const { return mResourceId; }

    void startTypeClass(const std::string &ClassName);
    void endTypeClass();

    inline void incFieldIndex() { mFieldIndex++; }

    inline void resetFieldIndex() { mFieldIndex = 0; }

    inline void addFieldIndexMapping(const RSExportRecordType::Field *F) {
      slangAssert((mFieldIndexMap.find(F) == mFieldIndexMap.end()) &&
                  "Nested structure never occurs in C language.");
      mFieldIndexMap.insert(std::make_pair(F, mFieldIndex));
    }

    inline unsigned getFieldIndex(const RSExportRecordType::Field *F) const {
      FieldIndexMapTy::const_iterator I = mFieldIndexMap.find(F);
      slangAssert((I != mFieldIndexMap.end()) &&
                  "Requesting field is out of scope.");
      return I->second;
    }

    inline void clearFieldIndexMap() { mFieldIndexMap.clear(); }
  };

  bool genScriptClass(Context &C,
                      const std::string &ClassName,
                      std::string &ErrorMsg);
  void genScriptClassConstructor(Context &C);

  static void genInitBoolExportVariable(Context &C,
                                        const std::string &VarName,
                                        const clang::APValue &Val);
  static void genInitPrimitiveExportVariable(Context &C,
                                             const std::string &VarName,
                                             const clang::APValue &Val);
  static void genInitExportVariable(Context &C,
                                    const RSExportType *ET,
                                    const std::string &VarName,
                                    const clang::APValue &Val);
  void genExportVariable(Context &C, const RSExportVar *EV);
  void genPrimitiveTypeExportVariable(Context &C, const RSExportVar *EV);
  void genPointerTypeExportVariable(Context &C, const RSExportVar *EV);
  void genVectorTypeExportVariable(Context &C, const RSExportVar *EV);
  void genMatrixTypeExportVariable(Context &C, const RSExportVar *EV);
  void genConstantArrayTypeExportVariable(Context &C, const RSExportVar *EV);
  void genRecordTypeExportVariable(Context &C, const RSExportVar *EV);
  void genPrivateExportVariable(Context &C,
                                const std::string &TypeName,
                                const std::string &VarName);
  void genSetExportVariable(Context &C,
                            const std::string &TypeName,
                            const RSExportVar *EV);
  void genGetExportVariable(Context &C,
                            const std::string &TypeName,
                            const std::string &VarName);
  void genGetFieldID(Context &C,
                     const std::string &VarName);

  void genExportFunction(Context &C,
                         const RSExportFunc *EF);

  void genExportForEach(Context &C,
                        const RSExportForEach *EF);

  static void genTypeCheck(Context &C,
                           const RSExportType *ET,
                           const char *VarName);

  static void genTypeInstanceFromPointer(Context &C,
                                         const RSExportType *ET);

  static void genTypeInstance(Context &C,
                              const RSExportType *ET);

  static void genFieldPackerInstance(Context &C,
                                     const RSExportType *ET);

  bool genTypeClass(Context &C,
                    const RSExportRecordType *ERT,
                    std::string &ErrorMsg);
  void genTypeItemClass(Context &C, const RSExportRecordType *ERT);
  void genTypeClassConstructor(Context &C, const RSExportRecordType *ERT);
  void genTypeClassCopyToArray(Context &C, const RSExportRecordType *ERT);
  void genTypeClassCopyToArrayLocal(Context &C, const RSExportRecordType *ERT);
  void genTypeClassItemSetter(Context &C, const RSExportRecordType *ERT);
  void genTypeClassItemGetter(Context &C, const RSExportRecordType *ERT);
  void genTypeClassComponentSetter(Context &C, const RSExportRecordType *ERT);
  void genTypeClassComponentGetter(Context &C, const RSExportRecordType *ERT);
  void genTypeClassCopyAll(Context &C, const RSExportRecordType *ERT);
  void genTypeClassResize(Context &C);

  void genBuildElement(Context &C,
                       const char *ElementBuilderName,
                       const RSExportRecordType *ERT,
                       const char *RenderScriptVar,
                       bool IsInline);
  void genAddElementToElementBuilder(Context &C,
                                     const RSExportType *ERT,
                                     const std::string &VarName,
                                     const char *ElementBuilderName,
                                     const char *RenderScriptVar,
                                     unsigned ArraySize);
  void genAddPaddingToElementBuiler(Context &C,
                                    int PaddingSize,
                                    const char *ElementBuilderName,
                                    const char *RenderScriptVar);

  bool genCreateFieldPacker(Context &C,
                            const RSExportType *T,
                            const char *FieldPackerName);
  void genPackVarOfType(Context &C,
                        const RSExportType *T,
                        const char *VarName,
                        const char *FieldPackerName);
  void genAllocateVarOfType(Context &C,
                            const RSExportType *T,
                            const std::string &VarName);
  void genNewItemBufferIfNull(Context &C, const char *Index);
  void genNewItemBufferPackerIfNull(Context &C);

 public:
  explicit RSReflection(const RSContext *Context,
      std::vector<std::string> *GeneratedFileNames)
      : mRSContext(Context),
        mLastError(""),
        mGeneratedFileNames(GeneratedFileNames) {
    slangAssert(mGeneratedFileNames && "Must supply GeneratedFileNames");
    return;
  }

  bool reflect(const std::string &OutputPathBase,
               const std::string &OutputPackageName,
               const std::string &RSPackageName,
               const std::string &InputFileName,
               const std::string &OutputBCFileName);

  inline const char *getLastError() const {
    if (mLastError.empty())
      return NULL;
    else
      return mLastError.c_str();
  }
};  // class RSReflection

}   // namespace slang

#endif  // _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_REFLECTION_H_  NOLINT
