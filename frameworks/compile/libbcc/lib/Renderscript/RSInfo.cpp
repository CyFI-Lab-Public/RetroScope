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

//#define LOG_NDEBUG 0
#include "bcc/Renderscript/RSInfo.h"

#if !defined(_WIN32)  /* TODO create a HAVE_DLFCN_H */
#include <dlfcn.h>
#endif

#include <cstring>
#include <new>
#include <string>

#include "bcc/Support/FileBase.h"
#include "bcc/Support/Log.h"

#ifdef HAVE_ANDROID_OS
#include <cutils/properties.h>
#endif

using namespace bcc;

const char RSInfo::LibBCCPath[] = "/system/lib/libbcc.so";
const char RSInfo::LibCompilerRTPath[] = "/system/lib/libcompiler_rt.so";
const char RSInfo::LibRSPath[] = "/system/lib/libRS.so";
const char RSInfo::LibCLCorePath[] = "/system/lib/libclcore.bc";
const char RSInfo::LibCLCoreDebugPath[] = "/system/lib/libclcore_debug.bc";
#if defined(ARCH_X86_HAVE_SSE2)
const char RSInfo::LibCLCoreX86Path[] = "/system/lib/libclcore_x86.bc";
#endif
#if defined(ARCH_ARM_HAVE_NEON)
const char RSInfo::LibCLCoreNEONPath[] = "/system/lib/libclcore_neon.bc";
#endif

const uint8_t *RSInfo::LibBCCSHA1 = NULL;
const uint8_t *RSInfo::LibCompilerRTSHA1 = NULL;
const uint8_t *RSInfo::LibRSSHA1 = NULL;
const uint8_t *RSInfo::LibCLCoreSHA1 = NULL;
const uint8_t *RSInfo::LibCLCoreDebugSHA1 = NULL;
#if defined(ARCH_ARM_HAVE_NEON)
const uint8_t *RSInfo::LibCLCoreNEONSHA1 = NULL;
#endif

bool RSInfo::LoadBuiltInSHA1Information() {
#ifdef TARGET_BUILD
  if (LibBCCSHA1 != NULL) {
    // Loaded before.
    return true;
  }

  void *h = ::dlopen("/system/lib/libbcc.sha1.so", RTLD_LAZY | RTLD_NOW);
  if (h == NULL) {
    ALOGE("Failed to load SHA-1 information from shared library '"
          "/system/lib/libbcc.sha1.so'! (%s)", ::dlerror());
    return false;
  }

  LibBCCSHA1 = reinterpret_cast<const uint8_t *>(::dlsym(h, "libbcc_so_SHA1"));
  LibCompilerRTSHA1 =
      reinterpret_cast<const uint8_t *>(::dlsym(h, "libcompiler_rt_so_SHA1"));
  LibRSSHA1 = reinterpret_cast<const uint8_t *>(::dlsym(h, "libRS_so_SHA1"));
  LibCLCoreSHA1 =
      reinterpret_cast<const uint8_t *>(::dlsym(h, "libclcore_bc_SHA1"));
  LibCLCoreDebugSHA1 =
      reinterpret_cast<const uint8_t *>(::dlsym(h, "libclcore_debug_bc_SHA1"));
#if defined(ARCH_ARM_HAVE_NEON)
  LibCLCoreNEONSHA1 =
      reinterpret_cast<const uint8_t *>(::dlsym(h, "libclcore_neon_bc_SHA1"));
#endif

  return true;
#else  // TARGET_BUILD
  return false;
#endif  // TARGET_BUILD
}

android::String8 RSInfo::GetPath(const char *pFilename) {
  android::String8 result(pFilename);
  result.append(".info");
  return result;
}

#define PRINT_DEPENDENCY(PREFIX, N, X) \
        ALOGV("\t" PREFIX "Source name: %s, "                                 \
                          "SHA-1: %02x%02x%02x%02x%02x%02x%02x%02x%02x%02x"   \
                                 "%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x",  \
              (N), (X)[ 0], (X)[ 1], (X)[ 2], (X)[ 3], (X)[ 4], (X)[ 5],      \
                   (X)[ 6], (X)[ 7], (X)[ 8], (X)[ 9], (X)[10], (X)[11],      \
                   (X)[12], (X)[13], (X)[14], (X)[15], (X)[16], (X)[17],      \
                   (X)[18], (X)[19]);

bool RSInfo::CheckDependency(const RSInfo &pInfo,
                             const char *pInputFilename,
                             const DependencyTableTy &pDeps) {
  // Built-in dependencies are libbcc.so, libRS.so and libclcore.bc plus
  // libclcore_neon.bc if NEON is available on the target device.
#if !defined(ARCH_ARM_HAVE_NEON)
  static const unsigned NumBuiltInDependencies = 5;
#else
  static const unsigned NumBuiltInDependencies = 6;
#endif

  LoadBuiltInSHA1Information();

  if (pInfo.mDependencyTable.size() != (pDeps.size() + NumBuiltInDependencies)) {
    ALOGD("Number of dependencies recorded mismatch (%lu v.s. %lu) in %s!",
          static_cast<unsigned long>(pInfo.mDependencyTable.size()),
          static_cast<unsigned long>(pDeps.size()), pInputFilename);
    return false;
  } else {
    // Built-in dependencies always go first.
    const std::pair<const char *, const uint8_t *> &cache_libbcc_dep =
        pInfo.mDependencyTable[0];
    const std::pair<const char *, const uint8_t *> &cache_libcompiler_rt_dep =
        pInfo.mDependencyTable[1];
    const std::pair<const char *, const uint8_t *> &cache_libRS_dep =
        pInfo.mDependencyTable[2];
    const std::pair<const char *, const uint8_t *> &cache_libclcore_dep =
        pInfo.mDependencyTable[3];
    const std::pair<const char *, const uint8_t *> &cache_libclcore_debug_dep =
        pInfo.mDependencyTable[4];
#if defined(ARCH_ARM_HAVE_NEON)
    const std::pair<const char *, const uint8_t *> &cache_libclcore_neon_dep =
        pInfo.mDependencyTable[5];
#endif

    // Check libbcc.so.
    if (::memcmp(cache_libbcc_dep.second, LibBCCSHA1, SHA1_DIGEST_LENGTH) != 0) {
        ALOGD("Cache %s is dirty due to %s has been updated.", pInputFilename,
              LibBCCPath);
        PRINT_DEPENDENCY("current - ", LibBCCPath, LibBCCSHA1);
        PRINT_DEPENDENCY("cache - ", cache_libbcc_dep.first,
                                     cache_libbcc_dep.second);
        return false;
    }

    // Check libcompiler_rt.so.
    if (::memcmp(cache_libcompiler_rt_dep.second, LibCompilerRTSHA1,
                 SHA1_DIGEST_LENGTH) != 0) {
        ALOGD("Cache %s is dirty due to %s has been updated.", pInputFilename,
              LibCompilerRTPath);
        PRINT_DEPENDENCY("current - ", LibCompilerRTPath, LibCompilerRTSHA1);
        PRINT_DEPENDENCY("cache - ", cache_libcompiler_rt_dep.first,
                                     cache_libcompiler_rt_dep.second);
        return false;
    }

    // Check libRS.so.
    if (::memcmp(cache_libRS_dep.second, LibRSSHA1, SHA1_DIGEST_LENGTH) != 0) {
        ALOGD("Cache %s is dirty due to %s has been updated.", pInputFilename,
              LibRSPath);
        PRINT_DEPENDENCY("current - ", LibRSPath, LibRSSHA1);
        PRINT_DEPENDENCY("cache - ", cache_libRS_dep.first,
                                     cache_libRS_dep.second);
        return false;
    }

    // Check libclcore.bc.
    if (::memcmp(cache_libclcore_dep.second, LibCLCoreSHA1,
                 SHA1_DIGEST_LENGTH) != 0) {
        ALOGD("Cache %s is dirty due to %s has been updated.", pInputFilename,
              LibCLCorePath);
        PRINT_DEPENDENCY("current - ", LibCLCorePath, LibCLCoreSHA1);
        PRINT_DEPENDENCY("cache - ", cache_libclcore_dep.first,
                                     cache_libclcore_dep.second);
        return false;
    }

    // Check libclcore_debug.bc.
    if (::memcmp(cache_libclcore_debug_dep.second, LibCLCoreDebugSHA1,
                 SHA1_DIGEST_LENGTH) != 0) {
        ALOGD("Cache %s is dirty due to %s has been updated.", pInputFilename,
              LibCLCoreDebugPath);
        PRINT_DEPENDENCY("current - ", LibCLCoreDebugPath, LibCLCoreDebugSHA1);
        PRINT_DEPENDENCY("cache - ", cache_libclcore_debug_dep.first,
                                     cache_libclcore_debug_dep.second);
        return false;
    }

#if defined(ARCH_ARM_HAVE_NEON)
    // Check libclcore_neon.bc if NEON is available.
    if (::memcmp(cache_libclcore_neon_dep.second, LibCLCoreNEONSHA1,
                 SHA1_DIGEST_LENGTH) != 0) {
        ALOGD("Cache %s is dirty due to %s has been updated.", pInputFilename,
              LibCLCoreNEONPath);
        PRINT_DEPENDENCY("current - ", LibCLCoreNEONPath, LibCLCoreNEONSHA1);
        PRINT_DEPENDENCY("cache - ", cache_libclcore_neon_dep.first,
                                     cache_libclcore_neon_dep.second);
        return false;
    }
#endif

    for (unsigned i = 0; i < pDeps.size(); i++) {
      const std::pair<const char *, const uint8_t *> &cache_dep =
          pInfo.mDependencyTable[i + NumBuiltInDependencies];

      if ((::strcmp(pDeps[i].first, cache_dep.first) != 0) ||
          (::memcmp(pDeps[i].second, cache_dep.second,
                    SHA1_DIGEST_LENGTH) != 0)) {
        ALOGD("Cache %s is dirty due to the source it dependends on has been "
              "changed:", pInputFilename);
        PRINT_DEPENDENCY("given - ", pDeps[i].first, pDeps[i].second);
        PRINT_DEPENDENCY("cache - ", cache_dep.first, cache_dep.second);
        return false;
      }
    }
  }

  return true;
}

RSInfo::RSInfo(size_t pStringPoolSize) : mStringPool(NULL) {
  ::memset(&mHeader, 0, sizeof(mHeader));

  ::memcpy(mHeader.magic, RSINFO_MAGIC, sizeof(mHeader.magic));
  ::memcpy(mHeader.version, RSINFO_VERSION, sizeof(mHeader.version));

  mHeader.headerSize = sizeof(mHeader);

  mHeader.dependencyTable.itemSize = sizeof(rsinfo::DependencyTableItem);
  mHeader.pragmaList.itemSize = sizeof(rsinfo::PragmaItem);
  mHeader.objectSlotList.itemSize = sizeof(rsinfo::ObjectSlotItem);
  mHeader.exportVarNameList.itemSize = sizeof(rsinfo::ExportVarNameItem);
  mHeader.exportFuncNameList.itemSize = sizeof(rsinfo::ExportFuncNameItem);
  mHeader.exportForeachFuncList.itemSize = sizeof(rsinfo::ExportForeachFuncItem);

  if (pStringPoolSize > 0) {
    mHeader.strPoolSize = pStringPoolSize;
    mStringPool = new (std::nothrow) char [ mHeader.strPoolSize ];
    if (mStringPool == NULL) {
      ALOGE("Out of memory when allocate memory for string pool in RSInfo "
            "constructor (size: %u)!", mHeader.strPoolSize);
    }
  }
}

RSInfo::~RSInfo() {
  delete [] mStringPool;
}

bool RSInfo::layout(off_t initial_offset) {
  mHeader.dependencyTable.offset = initial_offset +
                                   mHeader.headerSize +
                                   mHeader.strPoolSize;
  mHeader.dependencyTable.count = mDependencyTable.size();

#define AFTER(_list) ((_list).offset + (_list).itemSize * (_list).count)
  mHeader.pragmaList.offset = AFTER(mHeader.dependencyTable);
  mHeader.pragmaList.count = mPragmas.size();

  mHeader.objectSlotList.offset = AFTER(mHeader.pragmaList);
  mHeader.objectSlotList.count = mObjectSlots.size();

  mHeader.exportVarNameList.offset = AFTER(mHeader.objectSlotList);
  mHeader.exportVarNameList.count = mExportVarNames.size();

  mHeader.exportFuncNameList.offset = AFTER(mHeader.exportVarNameList);
  mHeader.exportFuncNameList.count = mExportFuncNames.size();

  mHeader.exportForeachFuncList.offset = AFTER(mHeader.exportFuncNameList);
  mHeader.exportForeachFuncList.count = mExportForeachFuncs.size();
#undef AFTER

  return true;
}

void RSInfo::dump() const {
  // Hide the codes to save the code size when debugging is disabled.
#if !LOG_NDEBUG

  // Dump header
  ALOGV("RSInfo Header:");
  ALOGV("\tIs threadable: %s", ((mHeader.isThreadable) ? "true" : "false"));
  ALOGV("\tHeader size: %u", mHeader.headerSize);
  ALOGV("\tString pool size: %u", mHeader.strPoolSize);

#define DUMP_LIST_HEADER(_name, _header) do { \
  ALOGV(_name ":"); \
  ALOGV("\toffset: %u", (_header).offset);  \
  ALOGV("\t# of item: %u", (_header).count);  \
  ALOGV("\tsize of each item: %u", (_header).itemSize); \
} while (false)
  DUMP_LIST_HEADER("Dependency table", mHeader.dependencyTable);
  for (DependencyTableTy::const_iterator dep_iter = mDependencyTable.begin(),
          dep_end = mDependencyTable.end(); dep_iter != dep_end; dep_iter++) {
    PRINT_DEPENDENCY("", dep_iter->first, dep_iter->second);
  }

  DUMP_LIST_HEADER("Pragma list", mHeader.pragmaList);
  for (PragmaListTy::const_iterator pragma_iter = mPragmas.begin(),
        pragma_end = mPragmas.end(); pragma_iter != pragma_end; pragma_iter++) {
    ALOGV("\tkey: %s, value: %s", pragma_iter->first, pragma_iter->second);
  }

  DUMP_LIST_HEADER("RS object slots", mHeader.objectSlotList);
  for (ObjectSlotListTy::const_iterator slot_iter = mObjectSlots.begin(),
          slot_end = mObjectSlots.end(); slot_iter != slot_end; slot_iter++) {
    ALOGV("slot: %u", *slot_iter);
  }

  DUMP_LIST_HEADER("RS export variables", mHeader.exportVarNameList);
  for (ExportVarNameListTy::const_iterator var_iter = mExportVarNames.begin(),
          var_end = mExportVarNames.end(); var_iter != var_end; var_iter++) {
    ALOGV("name: %s", *var_iter);
  }

  DUMP_LIST_HEADER("RS export functions", mHeader.exportFuncNameList);
  for (ExportFuncNameListTy::const_iterator func_iter = mExportFuncNames.begin(),
        func_end = mExportFuncNames.end(); func_iter != func_end; func_iter++) {
    ALOGV("name: %s", *func_iter);
  }

  DUMP_LIST_HEADER("RS foreach list", mHeader.exportForeachFuncList);
  for (ExportForeachFuncListTy::const_iterator
          foreach_iter = mExportForeachFuncs.begin(),
          foreach_end = mExportForeachFuncs.end(); foreach_iter != foreach_end;
          foreach_iter++) {
    ALOGV("name: %s, signature: %05x", foreach_iter->first,
                                       foreach_iter->second);
  }
#undef DUMP_LIST_HEADER

#endif // LOG_NDEBUG
  return;
}

const char *RSInfo::getStringFromPool(rsinfo::StringIndexTy pStrIdx) const {
  // String pool uses direct indexing. Ensure that the pStrIdx is within the
  // range.
  if (pStrIdx >= mHeader.strPoolSize) {
    ALOGE("String index #%u is out of range in string pool (size: %u)!",
          pStrIdx, mHeader.strPoolSize);
    return NULL;
  }
  return &mStringPool[ pStrIdx ];
}

rsinfo::StringIndexTy RSInfo::getStringIdxInPool(const char *pStr) const {
  // Assume we are on the flat memory architecture (i.e., the memory space is
  // continuous.)
  if ((mStringPool + mHeader.strPoolSize) < pStr) {
    ALOGE("String %s does not in the string pool!", pStr);
    return rsinfo::gInvalidStringIndex;
  }
  return (pStr - mStringPool);
}

RSInfo::FloatPrecision RSInfo::getFloatPrecisionRequirement() const {
  // Check to see if we have any FP precision-related pragmas.
  std::string relaxed_pragma("rs_fp_relaxed");
  std::string imprecise_pragma("rs_fp_imprecise");
  std::string full_pragma("rs_fp_full");
  bool relaxed_pragma_seen = false;
  bool imprecise_pragma_seen = false;
  RSInfo::FloatPrecision result = FP_Full;

  for (PragmaListTy::const_iterator pragma_iter = mPragmas.begin(),
           pragma_end = mPragmas.end(); pragma_iter != pragma_end;
       pragma_iter++) {
    const char *pragma_key = pragma_iter->first;
    if (!relaxed_pragma.compare(pragma_key)) {
      if (relaxed_pragma_seen || imprecise_pragma_seen) {
        ALOGE("Multiple float precision pragmas specified!");
      }
      relaxed_pragma_seen = true;
    } else if (!imprecise_pragma.compare(pragma_key)) {
      if (relaxed_pragma_seen || imprecise_pragma_seen) {
        ALOGE("Multiple float precision pragmas specified!");
      }
      imprecise_pragma_seen = true;
    }
  }

  // Imprecise is selected over Relaxed precision.
  // In the absence of both, we stick to the default Full precision.
  if (imprecise_pragma_seen) {
    result = FP_Imprecise;
  } else if (relaxed_pragma_seen) {
    result = FP_Relaxed;
  }

#ifdef HAVE_ANDROID_OS
  // Provide an override for precsion via adb shell setprop
  // adb shell setprop debug.rs.precision rs_fp_full
  // adb shell setprop debug.rs.precision rs_fp_relaxed
  // adb shell setprop debug.rs.precision rs_fp_imprecise
  char precision_prop_buf[PROPERTY_VALUE_MAX];
  property_get("debug.rs.precision", precision_prop_buf, "");

  if (precision_prop_buf[0]) {
    if (!relaxed_pragma.compare(precision_prop_buf)) {
      ALOGI("Switching to RS FP relaxed mode via setprop");
      result = FP_Relaxed;
    } else if (!imprecise_pragma.compare(precision_prop_buf)) {
      ALOGI("Switching to RS FP imprecise mode via setprop");
      result = FP_Imprecise;
    } else if (!full_pragma.compare(precision_prop_buf)) {
      ALOGI("Switching to RS FP full mode via setprop");
      result = FP_Full;
    }
  }
#endif

  return result;
}
