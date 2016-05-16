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

#include "slang_rs_metadata_spec.h"

#include <cstdlib>
#include <list>
#include <map>
#include <string>

#include "llvm/ADT/SmallVector.h"
#include "llvm/ADT/StringRef.h"

#include "llvm/Metadata.h"
#include "llvm/Module.h"

#include "slang_assert.h"
#include "slang_rs_type_spec.h"

#define RS_METADATA_STRTAB_MN   "#rs_metadata_strtab"
#define RS_TYPE_INFO_MN         "#rs_type_info"
#define RS_EXPORT_VAR_MN        "#rs_export_var"
#define RS_EXPORT_FUNC_MN       "#rs_export_func"
#define RS_EXPORT_RECORD_TYPE_NAME_MN_PREFIX  "%"

///////////////////////////////////////////////////////////////////////////////
// Useful utility functions
///////////////////////////////////////////////////////////////////////////////
static bool EncodeInteger(llvm::LLVMContext &C,
                          unsigned I,
                          llvm::SmallVectorImpl<llvm::Value*> &Op) {
  llvm::StringRef S(reinterpret_cast<const char*>(&I), sizeof(I));
  llvm::MDString *MDS = llvm::MDString::get(C, S);

  if (MDS == NULL)
    return false;
  Op.push_back(MDS);
  return true;
}

///////////////////////////////////////////////////////////////////////////////
// class RSMetadataEncoderInternal
///////////////////////////////////////////////////////////////////////////////
namespace {

class RSMetadataEncoderInternal {
 private:
  llvm::Module *mModule;

  typedef std::map</* key */unsigned, unsigned/* index */> TypesMapTy;
  TypesMapTy mTypes;
  std::list<unsigned> mEncodedRSTypeInfo;  // simply a sequece of integers
  unsigned mCurTypeIndex;

  // A special type for lookup created record type. It uses record name as key.
  typedef std::map</* name */std::string, unsigned/* index */> RecordTypesMapTy;
  RecordTypesMapTy mRecordTypes;

  typedef std::map<std::string, unsigned/* index */> StringsMapTy;
  StringsMapTy mStrings;
  std::list<const char*> mEncodedStrings;
  unsigned mCurStringIndex;

  llvm::NamedMDNode *mVarInfoMetadata;
  llvm::NamedMDNode *mFuncInfoMetadata;

  // This function check the return value of function:
  //   joinString, encodeTypeBase, encode*Type(), encodeRSType, encodeRSVar,
  //   and encodeRSFunc. Return false if the value of Index indicates failure.
  inline bool checkReturnIndex(unsigned *Index) {
    if (*Index == 0)
      return false;
    else
      (*Index)--;
    return true;
  }

  unsigned joinString(const std::string &S);

  unsigned encodeTypeBase(const struct RSTypeBase *Base);
  unsigned encodeTypeBaseAsKey(const struct RSTypeBase *Base);
#define ENUM_RS_DATA_TYPE_CLASS(x)  \
  unsigned encode ## x ## Type(const union RSType *T);
RS_DATA_TYPE_CLASS_ENUMS
#undef ENUM_RS_DATA_TYPE_CLASS

  unsigned encodeRSType(const union RSType *T);

  int flushStringTable();
  int flushTypeInfo();

 public:
  explicit RSMetadataEncoderInternal(llvm::Module *M);

  int encodeRSVar(const RSVar *V);
  int encodeRSFunc(const RSFunction *F);

  int finalize();
};

}  // namespace

RSMetadataEncoderInternal::RSMetadataEncoderInternal(llvm::Module *M)
    : mModule(M),
      mCurTypeIndex(0),
      mCurStringIndex(0),
      mVarInfoMetadata(NULL),
      mFuncInfoMetadata(NULL) {
  mTypes.clear();
  mEncodedRSTypeInfo.clear();
  mRecordTypes.clear();
  mStrings.clear();

  return;
}

// Return (StringIndex + 1) when successfully join the string and 0 if there's
// any error.
unsigned RSMetadataEncoderInternal::joinString(const std::string &S) {
  StringsMapTy::const_iterator I = mStrings.find(S);

  if (I != mStrings.end())
    return (I->second + 1);

  // Add S into mStrings
  std::pair<StringsMapTy::iterator, bool> Res =
      mStrings.insert(std::make_pair(S, mCurStringIndex));
  // Insertion failed
  if (!Res.second)
    return 0;

  // Add S into mEncodedStrings
  mEncodedStrings.push_back(Res.first->first.c_str());
  mCurStringIndex++;

  // Return (StringIndex + 1)
  return (Res.first->second + 1);
}

unsigned
RSMetadataEncoderInternal::encodeTypeBase(const struct RSTypeBase *Base) {
  mEncodedRSTypeInfo.push_back(Base->bits);
  return ++mCurTypeIndex;
}

unsigned RSMetadataEncoderInternal::encodeTypeBaseAsKey(
    const struct RSTypeBase *Base) {
  TypesMapTy::const_iterator I = mTypes.find(Base->bits);
  if (I != mTypes.end())
    return (I->second + 1);

  // Add Base into mTypes
  std::pair<TypesMapTy::iterator, bool> Res =
      mTypes.insert(std::make_pair(Base->bits, mCurTypeIndex));
  // Insertion failed
  if (!Res.second)
    return 0;

  // Push to mEncodedRSTypeInfo. This will also update mCurTypeIndex.
  return encodeTypeBase(Base);
}

unsigned RSMetadataEncoderInternal::encodePrimitiveType(const union RSType *T) {
  return encodeTypeBaseAsKey(RS_GET_TYPE_BASE(T));
}

unsigned RSMetadataEncoderInternal::encodePointerType(const union RSType *T) {
  // Encode pointee type first
  unsigned PointeeType = encodeRSType(RS_POINTER_TYPE_GET_POINTEE_TYPE(T));
  if (!checkReturnIndex(&PointeeType))
    return 0;

  unsigned Res = encodeTypeBaseAsKey(RS_GET_TYPE_BASE(T));
  // Push PointeeType after the base type
  mEncodedRSTypeInfo.push_back(PointeeType);
  return Res;
}

unsigned RSMetadataEncoderInternal::encodeVectorType(const union RSType *T) {
  return encodeTypeBaseAsKey(RS_GET_TYPE_BASE(T));
}

unsigned RSMetadataEncoderInternal::encodeMatrixType(const union RSType *T) {
  return encodeTypeBaseAsKey(RS_GET_TYPE_BASE(T));
}

unsigned
RSMetadataEncoderInternal::encodeConstantArrayType(const union RSType *T) {
  // Encode element type
  unsigned ElementType =
      encodeRSType(RS_CONSTANT_ARRAY_TYPE_GET_ELEMENT_TYPE(T));
  if (!checkReturnIndex(&ElementType))
    return 0;

  unsigned Res = encodeTypeBase(RS_GET_TYPE_BASE(T));
  // Push the ElementType after the type base
  mEncodedRSTypeInfo.push_back(ElementType);
  return Res;
}

unsigned RSMetadataEncoderInternal::encodeRecordType(const union RSType *T) {
  // Construct record name
  std::string RecordInfoMetadataName(RS_EXPORT_RECORD_TYPE_NAME_MN_PREFIX);
  RecordInfoMetadataName.append(RS_RECORD_TYPE_GET_NAME(T));

  // Try to find it in mRecordTypes
  RecordTypesMapTy::const_iterator I =
      mRecordTypes.find(RecordInfoMetadataName);

  // This record type has been encoded before. Fast return its index here.
  if (I != mRecordTypes.end())
    return (I->second + 1);

  // Encode this record type into mTypes. Encode record name string first.
  unsigned RecordName = joinString(RecordInfoMetadataName);
  if (!checkReturnIndex(&RecordName))
    return 0;

  unsigned Base = encodeTypeBase(RS_GET_TYPE_BASE(T));
  if (!checkReturnIndex(&Base))
    return 0;

  // Push record name after encoding the type base
  mEncodedRSTypeInfo.push_back(RecordName);

  // Add this record type into the map
  std::pair<StringsMapTy::iterator, bool> Res =
      mRecordTypes.insert(std::make_pair(RecordInfoMetadataName, Base));
  // Insertion failed
  if (!Res.second)
    return 0;

  // Create a named MDNode for this record type. We cannot create this before
  // encoding type base into Types and updating mRecordTypes. This is because
  // we may have structure like:
  //
  //            struct foo {
  //              ...
  //              struct foo *bar;  // self type reference
  //              ...
  //            }
  llvm::NamedMDNode *RecordInfoMetadata =
      mModule->getOrInsertNamedMetadata(RecordInfoMetadataName);

  slangAssert((RecordInfoMetadata->getNumOperands() == 0) &&
              "Record created before!");

  // Encode field info into this named MDNode
  llvm::SmallVector<llvm::Value*, 3> FieldInfo;

  for (unsigned i = 0; i < RS_RECORD_TYPE_GET_NUM_FIELDS(T); i++) {
    // 1. field name
    unsigned FieldName = joinString(RS_RECORD_TYPE_GET_FIELD_NAME(T, i));
    if (!checkReturnIndex(&FieldName))
      return 0;
    if (!EncodeInteger(mModule->getContext(),
                       FieldName,
                       FieldInfo)) {
      return 0;
    }

    // 2. field type
    unsigned FieldType = encodeRSType(RS_RECORD_TYPE_GET_FIELD_TYPE(T, i));
    if (!checkReturnIndex(&FieldType))
      return 0;
    if (!EncodeInteger(mModule->getContext(),
                       FieldType,
                       FieldInfo)) {
      return 0;
    }

    RecordInfoMetadata->addOperand(llvm::MDNode::get(mModule->getContext(),
                                                     FieldInfo));
    FieldInfo.clear();
  }

  return (Res.first->second + 1);
}

unsigned RSMetadataEncoderInternal::encodeRSType(const union RSType *T) {
  switch (static_cast<enum RSTypeClass>(RS_TYPE_GET_CLASS(T))) {
#define ENUM_RS_DATA_TYPE_CLASS(x)  \
    case RS_TC_ ## x: return encode ## x ## Type(T);
    RS_DATA_TYPE_CLASS_ENUMS
#undef ENUM_RS_DATA_TYPE_CLASS
    default: return 0;
  }
  return 0;
}

int RSMetadataEncoderInternal::encodeRSVar(const RSVar *V) {
  // check parameter
  if ((V == NULL) || (V->name == NULL) || (V->type == NULL))
    return -1;

  // 1. var name
  unsigned VarName = joinString(V->name);
  if (!checkReturnIndex(&VarName)) {
    return -2;
  }

  // 2. type
  unsigned Type = encodeRSType(V->type);

  llvm::SmallVector<llvm::Value*, 1> VarInfo;

  if (!EncodeInteger(mModule->getContext(), VarName, VarInfo)) {
    return -3;
  }
  if (!EncodeInteger(mModule->getContext(), Type, VarInfo)) {
    return -4;
  }

  if (mVarInfoMetadata == NULL)
    mVarInfoMetadata = mModule->getOrInsertNamedMetadata(RS_EXPORT_VAR_MN);

  mVarInfoMetadata->addOperand(llvm::MDNode::get(mModule->getContext(),
                                                 VarInfo));

  return 0;
}

int RSMetadataEncoderInternal::encodeRSFunc(const RSFunction *F) {
  // check parameter
  if ((F == NULL) || (F->name == NULL)) {
    return -1;
  }

  // 1. var name
  unsigned FuncName = joinString(F->name);
  if (!checkReturnIndex(&FuncName)) {
    return -2;
  }

  llvm::SmallVector<llvm::Value*, 1> FuncInfo;
  if (!EncodeInteger(mModule->getContext(), FuncName, FuncInfo)) {
    return -3;
  }

  if (mFuncInfoMetadata == NULL)
    mFuncInfoMetadata = mModule->getOrInsertNamedMetadata(RS_EXPORT_FUNC_MN);

  mFuncInfoMetadata->addOperand(llvm::MDNode::get(mModule->getContext(),
                                                  FuncInfo));

  return 0;
}

// Write string table and string index table
int RSMetadataEncoderInternal::flushStringTable() {
  slangAssert((mCurStringIndex == mEncodedStrings.size()));
  slangAssert((mCurStringIndex == mStrings.size()));

  if (mCurStringIndex == 0)
    return 0;

  // Prepare named MDNode for string table and string index table.
  llvm::NamedMDNode *RSMetadataStrTab =
      mModule->getOrInsertNamedMetadata(RS_METADATA_STRTAB_MN);
  RSMetadataStrTab->dropAllReferences();

  unsigned StrTabSize = 0;
  unsigned *StrIdx = reinterpret_cast<unsigned*>(
                        ::malloc((mStrings.size() + 1) * sizeof(unsigned)));

  if (StrIdx == NULL)
    return -1;

  unsigned StrIdxI = 0;  // iterator for array StrIdx

  // count StrTabSize and fill StrIdx by the way
  for (std::list<const char*>::const_iterator I = mEncodedStrings.begin(),
          E = mEncodedStrings.end();
       I != E;
       I++) {
    StrIdx[StrIdxI++] = StrTabSize;
    StrTabSize += ::strlen(*I) + 1 /* for '\0' */;
  }
  StrIdx[StrIdxI] = StrTabSize;

  // Allocate
  char *StrTab = reinterpret_cast<char*>(::malloc(StrTabSize));
  if (StrTab == NULL) {
    free(StrIdx);
    return -1;
  }

  llvm::StringRef StrTabData(StrTab, StrTabSize);
  llvm::StringRef StrIdxData(reinterpret_cast<const char*>(StrIdx),
                             mStrings.size() * sizeof(unsigned));

  // Copy
  StrIdxI = 1;
  for (std::list<const char*>::const_iterator I = mEncodedStrings.begin(),
          E = mEncodedStrings.end();
       I != E;
       I++) {
    // Get string length from StrIdx (O(1)) instead of call strlen again (O(n)).
    unsigned CurStrLength = StrIdx[StrIdxI] - StrIdx[StrIdxI - 1];
    ::memcpy(StrTab, *I, CurStrLength);
    // Move forward the pointer
    StrTab += CurStrLength;
    StrIdxI++;
  }

  // Flush to metadata
  llvm::Value *StrTabMDS =
      llvm::MDString::get(mModule->getContext(), StrTabData);
  llvm::Value *StrIdxMDS =
      llvm::MDString::get(mModule->getContext(), StrIdxData);

  if ((StrTabMDS == NULL) || (StrIdxMDS == NULL)) {
    free(StrIdx);
    free(StrTab);
    return -1;
  }

  llvm::SmallVector<llvm::Value*, 2> StrTabVal;
  StrTabVal.push_back(StrTabMDS);
  StrTabVal.push_back(StrIdxMDS);
  RSMetadataStrTab->addOperand(llvm::MDNode::get(mModule->getContext(),
                                                 StrTabVal));

  return 0;
}

// Write RS type stream
int RSMetadataEncoderInternal::flushTypeInfo() {
  unsigned TypeInfoCount = mEncodedRSTypeInfo.size();
  if (TypeInfoCount <= 0) {
    return 0;
  }

  llvm::NamedMDNode *RSTypeInfo =
      mModule->getOrInsertNamedMetadata(RS_TYPE_INFO_MN);
  RSTypeInfo->dropAllReferences();

  unsigned *TypeInfos =
      reinterpret_cast<unsigned*>(::malloc(TypeInfoCount * sizeof(unsigned)));
  unsigned TypeInfosIdx = 0;  // iterator for array TypeInfos

  if (TypeInfos == NULL)
    return -1;

  for (std::list<unsigned>::const_iterator I = mEncodedRSTypeInfo.begin(),
          E = mEncodedRSTypeInfo.end();
       I != E;
       I++)
    TypeInfos[TypeInfosIdx++] = *I;

  llvm::StringRef TypeInfoData(reinterpret_cast<const char*>(TypeInfos),
                               TypeInfoCount * sizeof(unsigned));
  llvm::Value *TypeInfoMDS =
      llvm::MDString::get(mModule->getContext(), TypeInfoData);
  if (TypeInfoMDS == NULL) {
    free(TypeInfos);
    return -1;
  }

  llvm::SmallVector<llvm::Value*, 1> TypeInfo;
  TypeInfo.push_back(TypeInfoMDS);

  RSTypeInfo->addOperand(llvm::MDNode::get(mModule->getContext(),
                                           TypeInfo));
  free(TypeInfos);

  return 0;
}

int RSMetadataEncoderInternal::finalize() {
  int Res = flushStringTable();
  if (Res != 0)
    return Res;

  Res = flushTypeInfo();
  if (Res != 0)
    return Res;

  return 0;
}

///////////////////////////////////////////////////////////////////////////////
// APIs
///////////////////////////////////////////////////////////////////////////////
RSMetadataEncoder *CreateRSMetadataEncoder(llvm::Module *M) {
  return reinterpret_cast<RSMetadataEncoder*>(new RSMetadataEncoderInternal(M));
}

int RSEncodeVarMetadata(RSMetadataEncoder *E, const RSVar *V) {
  return reinterpret_cast<RSMetadataEncoderInternal*>(E)->encodeRSVar(V);
}

int RSEncodeFunctionMetadata(RSMetadataEncoder *E, const RSFunction *F) {
  return reinterpret_cast<RSMetadataEncoderInternal*>(E)->encodeRSFunc(F);
}

void DestroyRSMetadataEncoder(RSMetadataEncoder *E) {
  RSMetadataEncoderInternal *C =
      reinterpret_cast<RSMetadataEncoderInternal*>(E);
  delete C;
  return;
}

int FinalizeRSMetadataEncoder(RSMetadataEncoder *E) {
  RSMetadataEncoderInternal *C =
      reinterpret_cast<RSMetadataEncoderInternal*>(E);
  int Res = C->finalize();
  DestroyRSMetadataEncoder(E);
  return Res;
}
