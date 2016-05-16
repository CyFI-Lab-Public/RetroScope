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

#ifndef _COMPILE_SLANG_SLANG_RS_TYPE_SPEC_H_  // NOLINT
#define _COMPILE_SLANG_SLANG_RS_TYPE_SPEC_H_

#define RS_DATA_TYPE_CLASS_ENUMS            \
    ENUM_RS_DATA_TYPE_CLASS(Primitive)      \
    ENUM_RS_DATA_TYPE_CLASS(Pointer)        \
    ENUM_RS_DATA_TYPE_CLASS(Vector)         \
    ENUM_RS_DATA_TYPE_CLASS(Matrix)         \
    ENUM_RS_DATA_TYPE_CLASS(ConstantArray)  \
    ENUM_RS_DATA_TYPE_CLASS(Record)

#define PRIMITIVE_DATA_TYPE_ENUMS                         \
    ENUM_PRIMITIVE_DATA_TYPE(Float16, NULL, 16)           \
    ENUM_PRIMITIVE_DATA_TYPE(Float32, "float", 32)        \
    ENUM_PRIMITIVE_DATA_TYPE(Float64, "double", 64)       \
    ENUM_PRIMITIVE_DATA_TYPE(Signed8, "char", 8)          \
    ENUM_PRIMITIVE_DATA_TYPE(Signed16, "short", 16)       \
    ENUM_PRIMITIVE_DATA_TYPE(Signed32, "int", 32)         \
    ENUM_PRIMITIVE_DATA_TYPE(Signed64, "long", 64)        \
    ENUM_PRIMITIVE_DATA_TYPE(Unsigned8, "uchar", 8)       \
    ENUM_PRIMITIVE_DATA_TYPE(Unsigned16, "ushort", 16)    \
    ENUM_PRIMITIVE_DATA_TYPE(Unsigned32, "uint", 32)      \
    ENUM_PRIMITIVE_DATA_TYPE(Unsigned64, "ulong", 64)     \
    ENUM_PRIMITIVE_DATA_TYPE(Boolean, "bool", 8)          \
    ENUM_PRIMITIVE_DATA_TYPE(Unsigned565, "u565", 16)     \
    ENUM_PRIMITIVE_DATA_TYPE(Unsigned5551, "u5551", 16)   \
    ENUM_PRIMITIVE_DATA_TYPE(Unsigned4444, "u4444", 16)   \
    PRIMITIVE_DATA_TYPE_RANGE(Float16, Unsigned4444)

#define RS_MATRIX_DATA_TYPE_ENUMS                             \
    ENUM_RS_MATRIX_DATA_TYPE(RSMatrix2x2, "rs_matrix2x2", 2)  \
    ENUM_RS_MATRIX_DATA_TYPE(RSMatrix3x3, "rs_matrix3x3", 3)  \
    ENUM_RS_MATRIX_DATA_TYPE(RSMatrix4x4, "rs_matrix4x4", 4)  \
    RS_MATRIX_DATA_TYPE_RANGE(RSMatrix2x2, RSMatrix4x4)

#define RS_OBJECT_DATA_TYPE_ENUMS                                       \
    ENUM_RS_OBJECT_DATA_TYPE(RSElement, "rs_element")                   \
    ENUM_RS_OBJECT_DATA_TYPE(RSType, "rs_type")                         \
    ENUM_RS_OBJECT_DATA_TYPE(RSAllocation, "rs_allocation")             \
    ENUM_RS_OBJECT_DATA_TYPE(RSSampler, "rs_sampler")                   \
    ENUM_RS_OBJECT_DATA_TYPE(RSScript, "rs_script")                     \
    ENUM_RS_OBJECT_DATA_TYPE(RSMesh, "rs_mesh")                         \
    ENUM_RS_OBJECT_DATA_TYPE(RSPath, "rs_path")                         \
    ENUM_RS_OBJECT_DATA_TYPE(RSProgramFragment, "rs_program_fragment")  \
    ENUM_RS_OBJECT_DATA_TYPE(RSProgramVertex, "rs_program_vertex")      \
    ENUM_RS_OBJECT_DATA_TYPE(RSProgramRaster, "rs_program_raster")      \
    ENUM_RS_OBJECT_DATA_TYPE(RSProgramStore, "rs_program_store")        \
    ENUM_RS_OBJECT_DATA_TYPE(RSFont, "rs_font")                         \
    RS_OBJECT_DATA_TYPE_RANGE(RSElement, RSFont)

enum RSTypeClass {
#define ENUM_RS_DATA_TYPE_CLASS(x)  RS_TC_ ## x,
  RS_DATA_TYPE_CLASS_ENUMS
#undef ENUM_RS_DATA_TYPE_CLASS
  RS_TC_Max
};

enum RSDataType {
#define ENUM_PRIMITIVE_DATA_TYPE(x, name, bits) RS_DT_ ## x,
#define PRIMITIVE_DATA_TYPE_RANGE(x, y) \
    RS_DT_FirstPrimitiveType = RS_DT_ ## x, \
    RS_DT_LastPrimitiveType = RS_DT_ ## y,
  PRIMITIVE_DATA_TYPE_ENUMS
#undef ENUM_PRIMITIVE_DATA_TYPE
#undef PRIMITIVE_DATA_TYPE_RANGE

#define ENUM_RS_MATRIX_DATA_TYPE(x, name, dim) RS_DT_ ## x,
#define RS_MATRIX_DATA_TYPE_RANGE(x, y) \
      RS_DT_FirstMatrixType = RS_DT_ ## x,  \
      RS_DT_LastMatrixType = RS_DT_ ## y,
  RS_MATRIX_DATA_TYPE_ENUMS
#undef ENUM_RS_MATRIX_DATA_TYPE
#undef RS_MATRIX_DATA_TYPE_RANGE

#define ENUM_RS_OBJECT_DATA_TYPE(x, name) RS_DT_ ## x,
#define RS_OBJECT_DATA_TYPE_RANGE(x, y) \
    RS_DT_FirstRSObjectType = RS_DT_ ## x,  \
    RS_DT_LastRSObjectType = RS_DT_ ## y,
  RS_OBJECT_DATA_TYPE_ENUMS
#undef ENUM_RS_OBJECT_DATA_TYPE
#undef RS_OBJECT_DATA_TYPE_RANGE

  RS_DT_USER_DEFINED
};

// Forward declaration
union RSType;

// NOTE: Current design need to keep struct RSTypeBase as a 4-byte integer for
//       efficient decoding process (see DecodeTypeMetadata).
struct RSTypeBase {
  /* enum RSTypeClass tc; */
  // tc is encoded in b[0].
  union {
    // FIXME: handle big-endianess case
    unsigned bits;  // NOTE: Little-endian is assumed.
    unsigned char b[4];
  };
};

struct RSPrimitiveType {
  struct RSTypeBase base;
  /* enum RSDataType dt; */
  // dt is encoded in base.b[1]
};

struct RSPointerType {
  struct RSTypeBase base;
  const union RSType *pointee;
};

struct RSVectorType {
  struct RSPrimitiveType base;  // base type of vec must be in primitive type
  /* unsigned char vsize; */
  // vsize is encoded in base.b[2]
};

// RSMatrixType is actually a specialize class of RSPrimitiveType whose value of
// dt (data type) can only be RS_DT_RSMatrix2x2, RS_DT_RSMatrix3x3 and
// RS_DT_RSMatrix4x4.
struct RSMatrixType {
  struct RSTypeBase base;
};

struct RSConstantArrayType {
  struct RSTypeBase base;
  const union RSType *element_type;
  /* unsigned esize; */
  // esize is encoded in base.bits{8-31} in little-endian way. This implicates
  // the number of elements in any constant array type should never exceed 2^24.
};

struct RSRecordField {
  const char *name;  // field name
  const union RSType *type;
};

struct RSRecordType {
  struct RSTypeBase base;
  const char *name;  // type name
  /* unsigned num_fields; */
  // num_fields is encoded in base.bits{16-31} in little-endian way. This
  // implicates the number of fields defined in any record type should never
  // exceed 2^16.

  struct RSRecordField field[1];
};

union RSType {
  struct RSTypeBase base;
  struct RSPrimitiveType prim;
  struct RSPointerType pointer;
  struct RSVectorType vec;
  struct RSConstantArrayType ca;
  struct RSRecordType rec;
};

#define RS_GET_TYPE_BASE(R)               (&((R)->base))
#define RS_CAST_TO_PRIMITIVE_TYPE(R)      (&((R)->prim))
#define RS_CAST_TO_POINTER_TYPE(R)        (&((R)->pointer))
#define RS_CAST_TO_VECTOR_TYPE(R)         (&((R)->vec))
#define RS_CAST_TO_CONSTANT_ARRAY_TYPE(R) (&((R)->ca))
#define RS_CAST_TO_RECORD_TYPE(R)         (&((R)->rec))

// RSType
#define RS_TYPE_GET_CLASS(R)  RS_GET_TYPE_BASE(R)->b[0]
#define RS_TYPE_SET_CLASS(R, V) RS_TYPE_GET_CLASS(R) = (V)

// RSPrimitiveType
#define RS_PRIMITIVE_TYPE_GET_DATA_TYPE(R)  \
    RS_CAST_TO_PRIMITIVE_TYPE(R)->base.b[1]
#define RS_PRIMITIVE_TYPE_SET_DATA_TYPE(R, V) \
    RS_PRIMITIVE_TYPE_GET_DATA_TYPE(R) = (V)

// RSPointerType
#define RS_POINTER_TYPE_GET_POINTEE_TYPE(R) \
    RS_CAST_TO_POINTER_TYPE(R)->pointee
#define RS_POINTER_TYPE_SET_POINTEE_TYPE(R, V) \
    RS_POINTER_TYPE_GET_POINTEE_TYPE(R) = (V)

// RSVectorType
#define RS_VECTOR_TYPE_GET_ELEMENT_TYPE(R) \
    RS_PRIMITIVE_TYPE_GET_DATA_TYPE(R)
#define RS_VECTOR_TYPE_SET_ELEMENT_TYPE(R, V) \
    RS_VECTOR_TYPE_GET_ELEMENT_TYPE(R) = (V)

#define RS_VECTOR_TYPE_GET_VECTOR_SIZE(R) \
    RS_CAST_TO_VECTOR_TYPE(R)->base.base.b[2]
#define RS_VECTOR_TYPE_SET_VECTOR_SIZE(R, V) \
    RS_VECTOR_TYPE_GET_VECTOR_SIZE(R) = (V)

// RSMatrixType
#define RS_MATRIX_TYPE_GET_DATA_TYPE(R) RS_PRIMITIVE_TYPE_GET_DATA_TYPE(R)
#define RS_MATRIX_TYPE_SET_DATA_TYPE(R, V)  \
    RS_MATRIX_TYPE_GET_DATA_TYPE(R) = (V)

// RSConstantArrayType
#define RS_CONSTANT_ARRAY_TYPE_GET_ELEMENT_TYPE(R) \
    RS_CAST_TO_CONSTANT_ARRAY_TYPE(R)->element_type
#define RS_CONSTANT_ARRAY_TYPE_SET_ELEMENT_TYPE(R, V) \
    RS_CONSTANT_ARRAY_TYPE_GET_ELEMENT_TYPE(R) = (V)

#define RS_CONSTANT_ARRAY_TYPE_GET_ELEMENT_SIZE(R)  \
    (RS_CAST_TO_CONSTANT_ARRAY_TYPE(R)->base.bits & 0x00ffffff)
#define RS_CONSTANT_ARRAY_TYPE_SET_ELEMENT_SIZE(R, V) \
    RS_CAST_TO_CONSTANT_ARRAY_TYPE(R)->base.bits =  \
    ((RS_CAST_TO_CONSTANT_ARRAY_TYPE(R)->base.bits & 0x000000ff) |  \
     ((V & 0xffffff) << 8))

// RSRecordType
#define RS_RECORD_TYPE_GET_NAME(R)  RS_CAST_TO_RECORD_TYPE(R)->name
#define RS_RECORD_TYPE_SET_NAME(R, V) RS_RECORD_TYPE_GET_NAME(R) = (V)

#define RS_RECORD_TYPE_GET_NUM_FIELDS(R)  \
    ((RS_CAST_TO_RECORD_TYPE(R)->base.bits & 0xffff0000) >> 16)
#define RS_RECORD_TYPE_SET_NUM_FIELDS(R, V) \
    RS_CAST_TO_RECORD_TYPE(R)->base.bits =  \
    ((RS_CAST_TO_RECORD_TYPE(R)->base.bits & 0x0000ffff) | ((V & 0xffff) << 16))

#define RS_RECORD_TYPE_GET_FIELD_NAME(R, I) \
    RS_CAST_TO_RECORD_TYPE(R)->field[(I)].name
#define RS_RECORD_TYPE_SET_FIELD_NAME(R, I, V) \
    RS_RECORD_TYPE_GET_FIELD_NAME(R, I) = (V)

#define RS_RECORD_TYPE_GET_FIELD_TYPE(R, I) \
    RS_CAST_TO_RECORD_TYPE(R)->field[(I)].type
#define RS_RECORD_TYPE_SET_FIELD_TYPE(R, I, V) \
    RS_RECORD_TYPE_GET_FIELD_TYPE(R, I) = (V)

#endif  // _COMPILE_SLANG_SLANG_RS_TYPE_SPEC_H_  NOLINT
