/*
 * Copyright 2012, The Android Open Source Project
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

#ifndef _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_REFLECTION_CPP_H_  // NOLINT
#define _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_REFLECTION_CPP_H_

#include "slang_rs_reflection_base.h"

#include <set>
#include <string>

#define RS_EXPORT_VAR_PREFIX             "mExportVar_"

namespace slang {

class RSReflectionCpp : public RSReflectionBase {
 public:
  explicit RSReflectionCpp(const RSContext *);
  virtual ~RSReflectionCpp();

  bool reflect(const std::string &OutputPathBase,
               const std::string &InputFileName,
               const std::string &OutputBCFileName);


 private:
  unsigned int mNextExportVarSlot;
  unsigned int mNextExportFuncSlot;
  unsigned int mNextExportForEachSlot;

  inline void clear() {
    mNextExportVarSlot = 0;
    mNextExportFuncSlot = 0;
    mNextExportForEachSlot = 0;
    mTypesToCheck.clear();
  }

  inline unsigned int getNextExportVarSlot() {
    return mNextExportVarSlot++;
  }

  inline unsigned int getNextExportFuncSlot() {
    return mNextExportFuncSlot++;
  }

  inline unsigned int getNextExportForEachSlot() {
    return mNextExportForEachSlot++;
  }

  bool makeHeader(const std::string &baseClass);
  bool makeImpl(const std::string &baseClass);
  void makeFunctionSignature(std::stringstream &ss, bool isDefinition,
                             const RSExportFunc *ef);
  bool writeBC();

  bool startScriptHeader();


  // Write out code for an export variable initialization.
  void genInitExportVariable(const RSExportType *ET,
                             const std::string &VarName,
                             const clang::APValue &Val);
  void genZeroInitExportVariable(const std::string &VarName);
  void genInitBoolExportVariable(const std::string &VarName,
                                 const clang::APValue &Val);
  void genInitPrimitiveExportVariable(const std::string &VarName,
                                      const clang::APValue &Val);

  // Produce an argument string of the form "T1 t, T2 u, T3 v".
  void makeArgs(std::stringstream &ss, const ArgTy& Args);

  // Write out code for an export variable.
  void genExportVariable(const RSExportVar *EV);

  void genPrimitiveTypeExportVariable(const RSExportVar *EV);
  void genPointerTypeExportVariable(const RSExportVar *EV);
  void genVectorTypeExportVariable(const RSExportVar *EV);
  void genMatrixTypeExportVariable(const RSExportVar *EV);
  void genConstantArrayTypeExportVariable(const RSExportVar *EV);
  void genRecordTypeExportVariable(const RSExportVar *EV);

  // Write out a local FieldPacker (if necessary).
  bool genCreateFieldPacker(const RSExportType *T,
                            const char *FieldPackerName);

  // Populate (write) the FieldPacker with add() operations.
  void genPackVarOfType(const RSExportType *ET,
                        const char *VarName,
                        const char *FieldPackerName);

  // Generate a runtime type check for VarName.
  void genTypeCheck(const RSExportType *ET, const char *VarName);

  // Generate a type instance for a given forEach argument type.
  void genTypeInstanceFromPointer(const RSExportType *ET);
  void genTypeInstance(const RSExportType *ET);

};  // class RSReflectionCpp

}   // namespace slang

#endif  // _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_REFLECTION_CPP_H_  NOLINT
