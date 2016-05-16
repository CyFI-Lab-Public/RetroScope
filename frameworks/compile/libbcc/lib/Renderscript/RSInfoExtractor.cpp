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

//===----------------------------------------------------------------------===//
// This file implements RSInfo::ExtractFromSource()
//===----------------------------------------------------------------------===//
#include "bcc/Renderscript/RSInfo.h"

#include <llvm/IR/Constants.h>
#include <llvm/IR/Metadata.h>
#include <llvm/IR/Module.h>

#include "bcc/Source.h"
#include "bcc/Support/Log.h"

using namespace bcc;

namespace {

// Name of metadata node where pragma info resides (should be synced with
// slang.cpp)
const llvm::StringRef pragma_metadata_name("#pragma");

/*
 * The following names should be synced with the one appeared in
 * slang_rs_metadata.h.
 */
// Name of metadata node where exported variable names reside
const llvm::StringRef export_var_metadata_name("#rs_export_var");

// Name of metadata node where exported function names reside
const llvm::StringRef export_func_metadata_name("#rs_export_func");

// Name of metadata node where exported ForEach name information resides
const llvm::StringRef export_foreach_name_metadata_name("#rs_export_foreach_name");

// Name of metadata node where exported ForEach signature information resides
const llvm::StringRef export_foreach_metadata_name("#rs_export_foreach");

// Name of metadata node where RS object slot info resides (should be
const llvm::StringRef object_slot_metadata_name("#rs_object_slots");

inline llvm::StringRef getStringFromOperand(const llvm::Value *pString) {
  if ((pString != NULL) && (pString->getValueID() == llvm::Value::MDStringVal)) {
    return static_cast<const llvm::MDString *>(pString)->getString();
  }
  return llvm::StringRef();
}

template<size_t NumOperands>
inline size_t getMetadataStringLength(const llvm::NamedMDNode *pMetadata) {
  if (pMetadata == NULL) {
    return 0;
  }

  size_t string_size = 0;
  for (unsigned i = 0, e = pMetadata->getNumOperands(); i < e; i++) {
    llvm::MDNode *node = pMetadata->getOperand(i);
    if ((node != NULL) && (node->getNumOperands() >= NumOperands)) {
      // Compiler try its best to unroll this loop since NumOperands is a
      // template parameter (therefore the number of iteration can be determined
      // at compile-time and it's usually small.)
      for (unsigned j = 0; j < NumOperands; j++) {
        llvm::StringRef s = getStringFromOperand(node->getOperand(j));
        if (s.size() > 0) {
          // +1 is for the null-terminator at the end of string.
          string_size += (s.size() + 1);
        }
      }
    }
  }

  return string_size;
}

// Write a string pString to the string pool pStringPool at offset pWriteStart.
// Return the pointer the pString resides within the string pool.
const char *writeString(const llvm::StringRef &pString, char *pStringPool,
                        off_t *pWriteStart) {
  if (pString.empty()) {
    return pStringPool;
  }

  char *pStringWriteStart = pStringPool + *pWriteStart;
  // Copy the string.
  ::memcpy(pStringWriteStart, pString.data(), pString.size());
  // Write null-terminator at the end of the string.
  pStringWriteStart[ pString.size() ] = '\0';
  // Update pWriteStart.
  *pWriteStart += (pString.size() + 1);

  return pStringWriteStart;
}

bool writeDependency(const std::string &pSourceName, const uint8_t *pSHA1,
                     char *pStringPool, off_t *pWriteStart,
                     RSInfo::DependencyTableTy &pDepTable) {
  const char *source_name = writeString(pSourceName, pStringPool, pWriteStart);

  uint8_t *sha1 = reinterpret_cast<uint8_t *>(pStringPool + *pWriteStart);

  // SHA-1 is special. It's size of SHA1_DIGEST_LENGTH (=20) bytes long without
  // null-terminator.
  ::memcpy(sha1, pSHA1, SHA1_DIGEST_LENGTH);
  // Record in the result RSInfo object.
  pDepTable.push(std::make_pair(source_name, sha1));
  // Update the string pool pointer.
  *pWriteStart += SHA1_DIGEST_LENGTH;

  return true;
}

} // end anonymous namespace

RSInfo *RSInfo::ExtractFromSource(const Source &pSource,
                                  const DependencyTableTy &pDeps)
{
  const llvm::Module &module = pSource.getModule();
  const char *module_name = module.getModuleIdentifier().c_str();

  const llvm::NamedMDNode *pragma =
      module.getNamedMetadata(pragma_metadata_name);
  const llvm::NamedMDNode *export_var =
      module.getNamedMetadata(export_var_metadata_name);
  const llvm::NamedMDNode *export_func =
      module.getNamedMetadata(export_func_metadata_name);
  const llvm::NamedMDNode *export_foreach_name =
      module.getNamedMetadata(export_foreach_name_metadata_name);
  const llvm::NamedMDNode *export_foreach_signature =
      module.getNamedMetadata(export_foreach_metadata_name);
  const llvm::NamedMDNode *object_slots =
      module.getNamedMetadata(object_slot_metadata_name);

  // Always write a byte 0x0 at the beginning of the string pool.
  size_t string_pool_size = 1;
  off_t cur_string_pool_offset = 0;

  RSInfo *result = NULL;

  // Handle legacy case for pre-ICS bitcode that doesn't contain a metadata
  // section for ForEach. We generate a full signature for a "root" function.
  if ((export_foreach_name == NULL) || (export_foreach_signature == NULL)) {
    export_foreach_name = NULL;
    export_foreach_signature = NULL;
    string_pool_size += 5;  // insert "root\0" for #rs_export_foreach_name
  }

  string_pool_size += getMetadataStringLength<2>(pragma);
  string_pool_size += getMetadataStringLength<1>(export_var);
  string_pool_size += getMetadataStringLength<1>(export_func);
  string_pool_size += getMetadataStringLength<1>(export_foreach_name);

  // Don't forget to reserve the space for the dependency informationin string
  // pool.
  string_pool_size += ::strlen(LibBCCPath) + 1 + SHA1_DIGEST_LENGTH;
  string_pool_size += ::strlen(LibCompilerRTPath) + 1 + SHA1_DIGEST_LENGTH;
  string_pool_size += ::strlen(LibRSPath) + 1 + SHA1_DIGEST_LENGTH;
  string_pool_size += ::strlen(LibCLCorePath) + 1 + SHA1_DIGEST_LENGTH;
  string_pool_size += ::strlen(LibCLCoreDebugPath) + 1 + SHA1_DIGEST_LENGTH;
#if defined(ARCH_ARM_HAVE_NEON)
  string_pool_size += ::strlen(LibCLCoreNEONPath) + 1 + SHA1_DIGEST_LENGTH;
#endif
  for (unsigned i = 0, e = pDeps.size(); i != e; i++) {
    // +1 for null-terminator
    string_pool_size += ::strlen(/* name */pDeps[i].first) + 1;
    // +SHA1_DIGEST_LENGTH for SHA-1 checksum
    string_pool_size += SHA1_DIGEST_LENGTH;
  }

  // Allocate result object
  result = new (std::nothrow) RSInfo(string_pool_size);
  if (result == NULL) {
    ALOGE("Out of memory when create RSInfo object for %s!", module_name);
    goto bail;
  }

  // Check string pool.
  if (result->mStringPool == NULL) {
    ALOGE("Out of memory when allocate string pool in RSInfo object for %s!",
          module_name);
    goto bail;
  }

  // First byte of string pool should be an empty string
  result->mStringPool[ cur_string_pool_offset++ ] = '\0';

  // Populate all the strings and data.
#define FOR_EACH_NODE_IN(_metadata, _node)  \
  for (unsigned i = 0, e = (_metadata)->getNumOperands(); i != e; i++)  \
    if (((_node) = (_metadata)->getOperand(i)) != NULL)
  //===--------------------------------------------------------------------===//
  // #pragma
  //===--------------------------------------------------------------------===//
  // Pragma is actually a key-value pair. The value can be an empty string while
  // the key cannot.
  if (pragma != NULL) {
    llvm::MDNode *node;
    FOR_EACH_NODE_IN(pragma, node) {
        llvm::StringRef key = getStringFromOperand(node->getOperand(0));
        llvm::StringRef val = getStringFromOperand(node->getOperand(1));
        if (key.empty()) {
          ALOGW("%s contains pragma metadata with empty key (skip)!",
                module_name);
        } else {
          result->mPragmas.push(std::make_pair(
              writeString(key, result->mStringPool, &cur_string_pool_offset),
              writeString(val, result->mStringPool, &cur_string_pool_offset)));
        } // key.empty()
    } // FOR_EACH_NODE_IN
  } // pragma != NULL

  //===--------------------------------------------------------------------===//
  // #rs_export_var
  //===--------------------------------------------------------------------===//
  if (export_var != NULL) {
    llvm::MDNode *node;
    FOR_EACH_NODE_IN(export_var, node) {
      llvm::StringRef name = getStringFromOperand(node->getOperand(0));
      if (name.empty()) {
        ALOGW("%s contains empty entry in #rs_export_var metadata (skip)!",
              module_name);
      } else {
          result->mExportVarNames.push(
              writeString(name, result->mStringPool, &cur_string_pool_offset));
      }
    }
  }

  //===--------------------------------------------------------------------===//
  // #rs_export_func
  //===--------------------------------------------------------------------===//
  if (export_func != NULL) {
    llvm::MDNode *node;
    FOR_EACH_NODE_IN(export_func, node) {
      llvm::StringRef name = getStringFromOperand(node->getOperand(0));
      if (name.empty()) {
        ALOGW("%s contains empty entry in #rs_export_func metadata (skip)!",
              module_name);
      } else {
        result->mExportFuncNames.push(
            writeString(name, result->mStringPool, &cur_string_pool_offset));
      }
    }
  }

  //===--------------------------------------------------------------------===//
  // #rs_export_foreach and #rs_export_foreach_name
  //===--------------------------------------------------------------------===//
  // It's a little bit complicated to deal with #rs_export_foreach (the
  // signature of foreach-able function) and #rs_export_foreach_name (the name
  // of function which is foreach-able). We have to maintain a legacy case:
  //
  //  In pre-ICS bitcode, forEach feature only supports non-graphic root()
  //  function and only one signature corresponded to that non-graphic root()
  //  was written to the #rs_export_foreach metadata section. There's no
  //  #rs_export_foreach_name metadata section.
  //
  // Currently, not only non-graphic root() is supported but also other
  // functions that are exportable. Therefore, a new metadata section
  // #rs_export_foreach_name is added to specify which functions are
  // for-eachable. In this case, #rs_export_foreach (the function name) and
  // #rs_export_foreach metadata (the signature) is one-to-one mapping among
  // their entries.
  if ((export_foreach_name != NULL) && (export_foreach_signature != NULL)) {
    unsigned num_foreach_function;

    // Should be one-to-one mapping.
    if (export_foreach_name->getNumOperands() !=
        export_foreach_signature->getNumOperands()) {
      ALOGE("Mismatch number of foreach-able function names (%u) in "
            "#rs_export_foreach_name and number of signatures (%u) "
            "in %s!", export_foreach_name->getNumOperands(),
            export_foreach_signature->getNumOperands(), module_name);
      goto bail;
    }

    num_foreach_function = export_foreach_name->getNumOperands();
    for (unsigned i = 0; i < num_foreach_function; i++) {
      llvm::MDNode *name_node = export_foreach_name->getOperand(i);
      llvm::MDNode *signature_node = export_foreach_signature->getOperand(i);

      llvm::StringRef name, signature_string;
      if (name_node != NULL) {
        name = getStringFromOperand(name_node->getOperand(0));
      }
      if (signature_node != NULL) {
        signature_string = getStringFromOperand(signature_node->getOperand(0));
      }

      if (!name.empty() && !signature_string.empty()) {
        // Both name_node and signature_node are not NULL nodes.
        uint32_t signature;
        if (signature_string.getAsInteger(10, signature)) {
          ALOGE("Non-integer signature value '%s' for function %s found in %s!",
                signature_string.str().c_str(), name.str().c_str(), module_name);
          goto bail;
        }
        result->mExportForeachFuncs.push(std::make_pair(
              writeString(name, result->mStringPool, &cur_string_pool_offset),
              signature));
      } else {
        // One or both of the name and signature value are empty. It's safe only
        // if both of them are empty.
        if (name.empty() && signature_string.empty()) {
          ALOGW("Entries #%u at #rs_export_foreach_name and #rs_export_foreach"
                " are both NULL in %s! (skip)", i, module_name);
          continue;
        } else {
          ALOGE("Entries #%u at %s is NULL in %s! (skip)", i,
                (name.empty() ? "#rs_export_foreach_name" :
                                "#rs_export_foreach"), module_name);
          goto bail;
        }
      }
    } // end for
  } else {
    // To handle the legacy case, we generate a full signature for a "root"
    // function which means that we need to set the bottom 5 bits (0x1f) in the
    // mask.
    result->mExportForeachFuncs.push(std::make_pair(
          writeString(llvm::StringRef("root"), result->mStringPool,
                      &cur_string_pool_offset), 0x1f));
  }

  //===--------------------------------------------------------------------===//
  // #rs_object_slots
  //===--------------------------------------------------------------------===//
  if (object_slots != NULL) {
    llvm::MDNode *node;
    for (unsigned int i = 0; i <= export_var->getNumOperands(); i++) {
      result->mObjectSlots.push(0);
    }
    FOR_EACH_NODE_IN(object_slots, node) {
      llvm::StringRef val = getStringFromOperand(node->getOperand(0));
      if (val.empty()) {
        ALOGW("%s contains empty entry in #rs_object_slots (skip)!",
              module.getModuleIdentifier().c_str());
      } else {
        uint32_t slot;
        if (val.getAsInteger(10, slot)) {
          ALOGE("Non-integer object slot value '%s' in %s!", val.str().c_str(),
                module.getModuleIdentifier().c_str());
          goto bail;
        } else {
          result->mObjectSlots.editItemAt(slot) = 1;
        }
      }
    }
  }
#undef FOR_EACH_NODE_IN

  if (LoadBuiltInSHA1Information()) {
    //===------------------------------------------------------------------===//
    // Record built-in dependency information.
    //===------------------------------------------------------------------===//
    if (!writeDependency(LibBCCPath, LibBCCSHA1,
                         result->mStringPool, &cur_string_pool_offset,
                         result->mDependencyTable)) {
      goto bail;
    }

    if (!writeDependency(LibCompilerRTPath, LibCompilerRTSHA1,
                         result->mStringPool, &cur_string_pool_offset,
                         result->mDependencyTable)) {
      goto bail;
    }

    if (!writeDependency(LibRSPath, LibRSSHA1,
                         result->mStringPool, &cur_string_pool_offset,
                         result->mDependencyTable)) {
      goto bail;
    }

    if (!writeDependency(LibCLCorePath, LibCLCoreSHA1,
                         result->mStringPool, &cur_string_pool_offset,
                         result->mDependencyTable)) {
      goto bail;
    }

    if (!writeDependency(LibCLCoreDebugPath, LibCLCoreDebugSHA1,
                         result->mStringPool, &cur_string_pool_offset,
                         result->mDependencyTable)) {
      goto bail;
    }

#if defined(ARCH_ARM_HAVE_NEON)
    if (!writeDependency(LibCLCoreNEONPath, LibCLCoreNEONSHA1,
                         result->mStringPool, &cur_string_pool_offset,
                         result->mDependencyTable)) {
      goto bail;
    }
#endif

    //===------------------------------------------------------------------===//
    // Record dependency information.
    //===------------------------------------------------------------------===//
    for (unsigned i = 0, e = pDeps.size(); i != e; i++) {
      if (!writeDependency(/* name */pDeps[i].first, /* SHA-1 */pDeps[i].second,
                           result->mStringPool, &cur_string_pool_offset,
                           result->mDependencyTable)) {
        goto bail;
      }
    }
  }

  //===--------------------------------------------------------------------===//
  // Determine whether the bitcode contains debug information
  //===--------------------------------------------------------------------===//
  // The root context of the debug information in the bitcode is put under
  // the metadata named "llvm.dbg.cu".
  result->mHeader.hasDebugInformation =
      static_cast<uint8_t>(module.getNamedMetadata("llvm.dbg.cu") != NULL);

  assert((cur_string_pool_offset == string_pool_size) &&
            "Unexpected string pool size!");

  return result;

bail:
  delete result;
  return NULL;
}
