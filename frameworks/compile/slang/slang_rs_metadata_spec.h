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

#ifndef _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_METADATA_SPEC_H_  // NOLINT
#define _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_METADATA_SPEC_H_

// Design Philosophy:
//  Expensive encoding but cheap decoding process.
//
// 1. A string table concatenates ALL strings (including '\0' in t)
// 2. A string index table which is an integer array containg the offset
//    to access each string in the string table.
// 3. All ->name field in RSType/RSVar/RSFunction now refer to an index in the
//    string index table for offset lookup.
// 4. RSType information is encoded as a byte stream like:
//
//                    [RSType #1][RSType #2] ... [RSType #N]
//
//    All ->type or ->base_type in RS*Type now becomes an index to RSType array.
//
// 5. RSVar => an string table index plus RSType array index
// 6. RSFunction => an string table index

namespace llvm {
  class Module;
}

// Forward declaration
union RSType;

struct RSVar {
  const char *name;  // variable name
  const union RSType *type;
};

struct RSFunction {
  const char *name;  // function name
};

// Opaque pointer
typedef int RSMetadataEncoder;

// Create a context associated with M for encoding metadata.
RSMetadataEncoder *CreateRSMetadataEncoder(llvm::Module *M);

// Encode V as a metadata in M. Return 0 if every thing goes well.
int RSEncodeVarMetadata(RSMetadataEncoder *E, const RSVar *V);
// Encode F as a metadata in M. Return 0 if every thing goes well.
int RSEncodeFunctionMetadata(RSMetadataEncoder *E, const RSFunction *F);

// Release the memory allocation of Encoder without flushing things.
void DestroyRSMetadataEncoder(RSMetadataEncoder *E);

// Flush metadata in E to its associated module and Destroy it. Return 0 if
// every thing goes well. This will also call the DestroyRSMetadataEncoder().
int FinalizeRSMetadataEncoder(RSMetadataEncoder *E);

// TODO(slang): Decoder
struct RSMetadata {
  unsigned num_vars;
  unsigned num_funcs;

  RSVar *vars;
  RSFunction *funcs;

  void *context;
};

// struct RSMetadata *RSDecodeMetadata(llvm::Module *M);
// void RSReleaseMetadata(struct RSMetadata *MD);

#endif  // _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_METADATA_SPEC_H_  NOLINT
