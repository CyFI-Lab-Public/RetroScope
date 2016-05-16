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

#include <cstdio>
#include <cstring>

#include <string>

#include "slang_rs_type_spec.h"

enum {
#define ENUM_PRIMITIVE_DATA_TYPE(x, name, bits) x,
#define PRIMITIVE_DATA_TYPE_RANGE(x, y) \
    FirstPrimitiveType = x, \
    LastPrimitiveType = y,
  PRIMITIVE_DATA_TYPE_ENUMS
#undef ENUM_PRIMITIVE_DATA_TYPE
#undef PRIMITIVE_DATA_TYPE_RANGE

#define ENUM_RS_MATRIX_DATA_TYPE(x, name, dim) x,
#define RS_MATRIX_DATA_TYPE_RANGE(x, y)  \
    FirstRSMatrixType = x,  \
    LastRSMatrixType = y,
  RS_MATRIX_DATA_TYPE_ENUMS
#undef ENUM_RS_MATRIX_DATA_TYPE
#undef RS_MATRIX_DATA_TYPE_RANGE

#define ENUM_RS_OBJECT_DATA_TYPE(x, name) x,
#define RS_OBJECT_DATA_TYPE_RANGE(x, y) \
    FirstRSObjectType = x,  \
    LastRSObjectType = y,
  RS_OBJECT_DATA_TYPE_ENUMS
#undef ENUM_RS_OBJECT_DATA_TYPE
#undef RS_OBJECT_DATA_TYPE_RANGE
};

class RSDataTypeSpec {
 private:
  const char *mTypeName;        // e.g. Float32
  // FIXME: better name
  const char *mTypePragmaName;  // e.g. float
  size_t mBits;

 protected:
  enum {
    DT_PrimitiveClass,
    DT_RSMatrixClass,
    DT_RSObjectClass
  } mClass;

 public:
  RSDataTypeSpec(const char *TypeName,
                 const char *TypePragmaName,
                 size_t Bits)
      : mTypeName(TypeName),
        mTypePragmaName(TypePragmaName),
        mBits(Bits),
        mClass(DT_PrimitiveClass) {
    return;
  }

  inline const char *getTypeName() const { return mTypeName; }
  inline const char *getTypePragmaName() const { return mTypePragmaName; }
  inline size_t getSizeInBit() const { return mBits; }
  inline bool isRSMatrix() const { return (mClass == DT_RSMatrixClass); }
  inline bool isRSObject() const { return (mClass == DT_RSObjectClass); }
};

class RSMatrixDataTypeSpec : public RSDataTypeSpec {
 private:
  unsigned mDim;
  static float ignore;

 public:
  RSMatrixDataTypeSpec(const char *TypeName,
                       const char *TypePragmaName,
                       unsigned Dim)
      : RSDataTypeSpec(TypeName, TypePragmaName, Dim * Dim * sizeof(ignore)),
        mDim(Dim) {
    mClass = DT_RSMatrixClass;
    return;
  }

  inline unsigned getDim() const { return mDim; }
};

class RSObjectDataTypeSpec : public RSDataTypeSpec {
 public:
  RSObjectDataTypeSpec(const char *TypeName,
                       const char *TypePragmaName)
      : RSDataTypeSpec(TypeName, TypePragmaName, 32 /* opaque pointer */) {
    mClass = DT_RSObjectClass;
    return;
  }
};

/////////////////////////////////////////////////////////////////////////////

// clang::BuiltinType::Kind -> RSDataTypeSpec
class ClangBuiltinTypeMap {
  const char *mBuiltinTypeKind;
  const RSDataTypeSpec *mDataType;

 public:
  ClangBuiltinTypeMap(const char *BuiltinTypeKind,
                      const RSDataTypeSpec *DataType)
      : mBuiltinTypeKind(BuiltinTypeKind),
        mDataType(DataType) {
    return;
  }

  inline const char *getBuiltinTypeKind() const { return mBuiltinTypeKind; }
  inline const RSDataTypeSpec *getDataType() const { return mDataType; }
};

/////////////////////////////////////////////////////////////////////////////

class RSDataElementSpec {
 private:
  const char *mElementName;
  const RSDataTypeSpec *mDataType;
  bool mIsNormal;
  unsigned mVectorSize;

 public:
  RSDataElementSpec(const char *ElementName,
                    const RSDataTypeSpec *DataType,
                    bool IsNormal,
                    unsigned VectorSize)
      : mElementName(ElementName),
        mDataType(DataType),
        mIsNormal(IsNormal),
        mVectorSize(VectorSize) {
    return;
  }

  inline const char *getElementName() const { return mElementName; }
  inline const RSDataTypeSpec *getDataType() const { return mDataType; }
  inline bool isNormal() const { return mIsNormal; }
  inline unsigned getVectorSize() const { return mVectorSize; }
};

/////////////////////////////////////////////////////////////////////////////

// -gen-rs-data-type-enums
//
// ENUM_PRIMITIVE_DATA_TYPE(type, cname, bits)
// ENUM_PRIMITIVE_DATA_TYPE_RANGE(begin_type, end_type)
// ENUM_RS_MATRIX_DATA_TYPE(type, cname, bits)
// ENUM_RS_MATRIX_DATA_TYPE_RANGE(begin_type, end_type)
// ENUM_RS_OBJECT_DATA_TYPE(type, cname, bits)
// ENUM_RS_OBJECT_DATA_TYPE_RANGE(begin_type, end_type)
//
// ENUM_RS_DATA_TYPE(type, cname, bits)
// e.g., ENUM_RS_DATA_TYPE(Float32, "float", 256)
static int GenRSDataTypeEnums(const RSDataTypeSpec *const DataTypes[],
                              unsigned NumDataTypes) {
  // Alias missing #define
#define ALIAS_DEF(x, y) \
  printf("#ifndef " #x "\n");  \
  printf("#define " #x "(type, cname, bits) " #y "(type, cname, bits)\n");  \
  printf("#endif\n\n")
  ALIAS_DEF(ENUM_PRIMITIVE_DATA_TYPE, ENUM_RS_DATA_TYPE);
  ALIAS_DEF(ENUM_RS_MATRIX_DATA_TYPE, ENUM_RS_DATA_TYPE);
  ALIAS_DEF(ENUM_RS_OBJECT_DATA_TYPE, ENUM_RS_DATA_TYPE);
#undef ALIAS_DEF

#define ALIAS_DEF(x) \
  printf("#ifndef " #x "\n");  \
  printf("#define " #x "(begin_type, end_type)\n");  \
  printf("#endif\n\n")
  ALIAS_DEF(ENUM_PRIMITIVE_DATA_TYPE_RANGE);
  ALIAS_DEF(ENUM_RS_MATRIX_DATA_TYPE_RANGE);
  ALIAS_DEF(ENUM_RS_OBJECT_DATA_TYPE_RANGE);
#undef ALIAS_DEF

#define DEF(x) \
  printf(#x "(%s, \"%s\", %lu)\n",  \
         DataTypes[i]->getTypeName(), \
         DataTypes[i]->getTypePragmaName(), \
         (unsigned long) DataTypes[i]->getSizeInBit());  // NOLINT(runtime/int)
#define DEF_RANGE(x, begin, end)  \
  printf(#x "(%s, %s)\n\n", \
         DataTypes[begin]->getTypeName(), \
         DataTypes[end]->getTypeName())
  for (unsigned i = FirstPrimitiveType; i <= LastPrimitiveType; i++)
    DEF(ENUM_PRIMITIVE_DATA_TYPE);
  DEF_RANGE(ENUM_PRIMITIVE_DATA_TYPE_RANGE,
            FirstPrimitiveType, LastPrimitiveType);
  for (unsigned i = FirstRSMatrixType; i <= LastRSMatrixType; i++)
    DEF(ENUM_RS_MATRIX_DATA_TYPE)
  DEF_RANGE(ENUM_RS_MATRIX_DATA_TYPE_RANGE,
            FirstRSMatrixType, LastRSMatrixType);
  for (unsigned i = FirstRSObjectType; i <= LastRSObjectType; i++)
    DEF(ENUM_RS_OBJECT_DATA_TYPE)
  DEF_RANGE(ENUM_RS_OBJECT_DATA_TYPE_RANGE,
            FirstRSObjectType, LastRSObjectType);
#undef DEF
#undef DEF_RANGE

#define UNDEF(x)  \
  printf("#undef " #x "\n")
  UNDEF(ENUM_PRIMITIVE_DATA_TYPE);
  UNDEF(ENUM_RS_MATRIX_DATA_TYPE);
  UNDEF(ENUM_RS_OBJECT_DATA_TYPE);
  UNDEF(ENUM_PRIMITIVE_DATA_TYPE_RANGE);
  UNDEF(ENUM_RS_MATRIX_DATA_TYPE_RANGE);
  UNDEF(ENUM_RS_OBJECT_DATA_TYPE_RANGE);
  UNDEF(ENUM_RS_DATA_TYPE);
  return 0;
}

// -gen-clang-builtin-cnames
//
// ENUM_SUPPORT_BUILTIN_TYPE(builtin_type, type, cname)
// e.g., ENUM_SUPPORT_BUILTIN_TYPE(clang::BuiltinType::Float, Float32, "float")
static int GenClangBuiltinEnum(
    const ClangBuiltinTypeMap *const ClangBuilitinsMap[],
    unsigned NumClangBuilitins) {
  for (unsigned i = 0; i < NumClangBuilitins; i++)
    printf("ENUM_SUPPORT_BUILTIN_TYPE(%s, %s, \"%s\")\n",
           ClangBuilitinsMap[i]->getBuiltinTypeKind(),
           ClangBuilitinsMap[i]->getDataType()->getTypeName(),
           ClangBuilitinsMap[i]->getDataType()->getTypePragmaName());
  printf("#undef ENUM_SUPPORT_BUILTIN_TYPE\n");
  return 0;
}

// -gen-rs-matrix-type-enums
//
// ENUM_RS_MATRIX_TYPE(type, cname, dim)
// e.g., ENUM_RS_MATRIX_TYPE(RSMatrix2x2, "rs_matrix2x2", 2)
static int GenRSMatrixTypeEnums(const RSDataTypeSpec *const DataTypes[],
                                unsigned NumDataTypes) {
  for (unsigned i = 0; i < NumDataTypes; i++)
    if (DataTypes[i]->isRSMatrix()) {
      const RSMatrixDataTypeSpec *const MatrixDataType =
          static_cast<const RSMatrixDataTypeSpec *const>(DataTypes[i]);
      printf("ENUM_RS_MATRIX_TYPE(%s, \"%s\", %u)\n",
             MatrixDataType->getTypeName(),
             MatrixDataType->getTypePragmaName(),
             MatrixDataType->getDim());
    }
  printf("#undef ENUM_RS_MATRIX_TYPE\n");
  return 0;
}

// -gen-rs-object-type-enums
//
// ENUM_RS_OBJECT_TYPE(type, cname)
// e.g., ENUM_RS_OBJECT_TYPE(RSElement, "rs_element")
static int GenRSObjectTypeEnums(const RSDataTypeSpec *const DataTypes[],
                                unsigned NumDataTypes) {
  for (unsigned i = 0; i < NumDataTypes; i++)
    if (DataTypes[i]->isRSObject())
      printf("ENUM_RS_OBJECT_TYPE(%s, \"%s\")\n",
             DataTypes[i]->getTypeName(),
             DataTypes[i]->getTypePragmaName());
  printf("#undef ENUM_RS_OBJECT_TYPE\n");
  return 0;
}

// -gen-rs-data-element-enums
//
// ENUM_RS_DATA_ELEMENT(name, dt, dk, normailized, vsize)
// e.g., ENUM_RS_DATA_ELEMENT("rs_pixel_rgba", PixelRGB, Unsigned8, true, 4)
int GenRSDataElementEnums(const RSDataElementSpec *const DataElements[],
                          unsigned NumDataElements) {
  for (unsigned i = 0; i < NumDataElements; i++)
    printf("ENUM_RS_DATA_ELEMENT(\"%s\", %s, %s, %d)\n",
           DataElements[i]->getElementName(),
           DataElements[i]->getDataType()->getTypeName(),
           ((DataElements[i]->isNormal()) ? "true" : "false"),
           DataElements[i]->getVectorSize());
  printf("#undef ENUM_RS_DATA_ELEMENT\n");
  return 0;
}

int main(int argc, char **argv) {
  if (argc < 2) {
    fprintf(stderr, "usage: %s [gen type]\n", argv[0]);
    return 1;
  }

  RSDataTypeSpec *DataTypes[] = {
#define ENUM_PRIMITIVE_DATA_TYPE(x, name, bits) \
  new RSDataTypeSpec(#x , name, bits),
#define PRIMITIVE_DATA_TYPE_RANGE(x, y)
  PRIMITIVE_DATA_TYPE_ENUMS
#undef ENUM_PRIMITIVE_DATA_TYPE
#undef PRIMITIVE_DATA_TYPE_RANGE

#define ENUM_RS_MATRIX_DATA_TYPE(x, name, dim) \
  new RSMatrixDataTypeSpec(#x , name, dim),
#define RS_MATRIX_DATA_TYPE_RANGE(x, y)
  RS_MATRIX_DATA_TYPE_ENUMS
#undef ENUM_RS_MATRIX_DATA_TYPE
#undef RS_MATRIX_DATA_TYPE_RANGE

#define ENUM_RS_OBJECT_DATA_TYPE(x, name)  \
  new RSObjectDataTypeSpec(#x, name),
#define RS_OBJECT_DATA_TYPE_RANGE(x, y)
  RS_OBJECT_DATA_TYPE_ENUMS
#undef ENUM_RS_OBJECT_DATA_TYPE
#undef RS_OBJECT_DATA_TYPE_RANGE
  };

  unsigned NumDataTypes = sizeof(DataTypes) / sizeof(DataTypes[0]);
  /////////////////////////////////////////////////////////////////////////////

  ClangBuiltinTypeMap *ClangBuilitinsMap[] = {
    new ClangBuiltinTypeMap("clang::BuiltinType::Bool",   DataTypes[Boolean]),
    new ClangBuiltinTypeMap("clang::BuiltinType::Char_U", DataTypes[Unsigned8]),
    new ClangBuiltinTypeMap("clang::BuiltinType::UChar",  DataTypes[Unsigned8]),
    new ClangBuiltinTypeMap("clang::BuiltinType::Char16", DataTypes[Signed16]),
    new ClangBuiltinTypeMap("clang::BuiltinType::Char32", DataTypes[Signed32]),
    new ClangBuiltinTypeMap(
      "clang::BuiltinType::UShort", DataTypes[Unsigned16]),
    new ClangBuiltinTypeMap(
      "clang::BuiltinType::UInt", DataTypes[Unsigned32]),
    new ClangBuiltinTypeMap(
      "clang::BuiltinType::ULong",  DataTypes[Unsigned32]),
    new ClangBuiltinTypeMap(
      "clang::BuiltinType::ULongLong", DataTypes[Unsigned64]),

    new ClangBuiltinTypeMap("clang::BuiltinType::Char_S", DataTypes[Signed8]),
    new ClangBuiltinTypeMap("clang::BuiltinType::SChar",  DataTypes[Signed8]),
    new ClangBuiltinTypeMap("clang::BuiltinType::Short",  DataTypes[Signed16]),
    new ClangBuiltinTypeMap("clang::BuiltinType::Int",    DataTypes[Signed32]),
    new ClangBuiltinTypeMap("clang::BuiltinType::Long",   DataTypes[Signed64]),
    new ClangBuiltinTypeMap(
      "clang::BuiltinType::LongLong", DataTypes[Signed64]),

    new ClangBuiltinTypeMap("clang::BuiltinType::Float",  DataTypes[Float32]),
    new ClangBuiltinTypeMap("clang::BuiltinType::Double", DataTypes[Float64])
  };

  unsigned NumClangBuilitins =
      sizeof(ClangBuilitinsMap) / sizeof(ClangBuilitinsMap[0]);

  /////////////////////////////////////////////////////////////////////////////

  RSDataElementSpec *DataElements[] = {
    new RSDataElementSpec("rs_pixel_l",
                          DataTypes[Unsigned8],
                          /* IsNormal = */true, /* VectorSize = */1),
    new RSDataElementSpec("rs_pixel_a",
                          DataTypes[Unsigned8],
                          true, 1),
    new RSDataElementSpec("rs_pixel_la",
                          DataTypes[Unsigned8],
                          true, 2),
    new RSDataElementSpec("rs_pixel_rgb",
                          DataTypes[Unsigned8],
                          true, 3),
    new RSDataElementSpec("rs_pixel_rgba",
                          DataTypes[Unsigned8],
                          true, 4),
    new RSDataElementSpec("rs_pixel_rgb565",
                          DataTypes[Unsigned8],
                          true, 3),
    new RSDataElementSpec("rs_pixel_rgb5551",
                          DataTypes[Unsigned8],
                          true, 4),
    new RSDataElementSpec("rs_pixel_rgb4444",
                          DataTypes[Unsigned8],
                          true, 4),
  };

  unsigned NumDataElements = sizeof(DataElements) / sizeof(DataElements[0]);
  /////////////////////////////////////////////////////////////////////////////
  int Result = 1;

  if (::strcmp(argv[1], "-gen-rs-data-type-enums") == 0)
    Result = GenRSDataTypeEnums(DataTypes, NumDataTypes);
  else if (::strcmp(argv[1], "-gen-clang-builtin-enums") == 0)
    Result = GenClangBuiltinEnum(ClangBuilitinsMap, NumClangBuilitins);
  else if (::strcmp(argv[1], "-gen-rs-matrix-type-enums") == 0)
    Result = GenRSMatrixTypeEnums(DataTypes, NumDataTypes);
  else if (::strcmp(argv[1], "-gen-rs-object-type-enums") == 0)
    Result = GenRSObjectTypeEnums(DataTypes, NumDataTypes);
  else if (::strcmp(argv[1], "-gen-rs-data-element-enums") == 0)
    Result = GenRSDataElementEnums(DataElements, NumDataElements);
  else
    fprintf(stderr, "%s: Unknown table generation type '%s'\n",
                    argv[0], argv[1]);


  /////////////////////////////////////////////////////////////////////////////
  for (unsigned i = 0; i < NumDataTypes; i++)
    delete DataTypes[i];
  for (unsigned i = 0; i < NumClangBuilitins; i++)
    delete ClangBuilitinsMap[i];
  for (unsigned i = 0; i < NumDataElements; i++)
    delete DataElements[i];

  return Result;
}
