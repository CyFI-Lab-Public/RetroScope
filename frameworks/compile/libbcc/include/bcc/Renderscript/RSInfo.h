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

#ifndef BCC_RS_INFO_H
#define BCC_RS_INFO_H

#include <stdint.h>

#include <utility>

#include "bcc/Support/Log.h"
#include "bcc/Support/Sha1Util.h"

#include <utils/String8.h>
#include <utils/Vector.h>

namespace llvm {
class Module;
}

namespace bcc {

// Forward declarations
class FileBase;
class InputFile;
class OutputFile;
class Source;
class RSScript;

typedef llvm::Module* (*RSLinkRuntimeCallback) (bcc::RSScript *, llvm::Module *, llvm::Module *);

namespace rsinfo {

/* RS info file magic */
#define RSINFO_MAGIC      "\0rsinfo\n"

/* RS info file version, encoded in 4 bytes of ASCII */
#define RSINFO_VERSION    "004\0"

struct __attribute__((packed)) ListHeader {
  // The offset from the beginning of the file of data
  uint32_t offset;
  // Number of item in the list
  uint32_t count;
  // Size of each item
  uint8_t itemSize;
};

/* RS info file header */
struct __attribute__((packed)) Header {
  // Magic versus version
  uint8_t magic[8];
  uint8_t version[4];

  uint8_t isThreadable;
  uint8_t hasDebugInformation;

  uint16_t headerSize;

  uint32_t strPoolSize;

  struct ListHeader dependencyTable;
  struct ListHeader pragmaList;
  struct ListHeader objectSlotList;
  struct ListHeader exportVarNameList;
  struct ListHeader exportFuncNameList;
  struct ListHeader exportForeachFuncList;
};

typedef uint32_t StringIndexTy;
// Use value -1 as an invalid string index marker. No need to declare with
// 'static' modifier since 'const' variable has internal linkage by default.
const StringIndexTy gInvalidStringIndex = static_cast<StringIndexTy>(-1);

struct __attribute__((packed)) DependencyTableItem {
  StringIndexTy id;
  // SHA-1 checksum is stored as a string in string pool (and has fixed-length
  // SHA1_DIGEST_LENGTH (=20) bytes)
  StringIndexTy sha1;
};

struct __attribute__((packed)) PragmaItem {
  // Pragma is a key-value pair.
  StringIndexTy key;
  StringIndexTy value;
};

struct __attribute__((packed)) ObjectSlotItem {
  uint32_t slot;
};

struct __attribute__((packed)) ExportVarNameItem {
  StringIndexTy name;
};

struct __attribute__((packed)) ExportFuncNameItem {
  StringIndexTy name;
};

struct __attribute__((packed)) ExportForeachFuncItem {
  StringIndexTy name;
  uint32_t signature;
};

// Return the human-readable name of the given rsinfo::*Item in the template
// parameter. This is for debugging and error message.
template<typename Item>
inline const char *GetItemTypeName();

template<>
inline const char *GetItemTypeName<DependencyTableItem>()
{ return "rs dependency info"; }

template<>
inline const char *GetItemTypeName<PragmaItem>()
{  return "rs pragma"; }

template<>
inline const char *GetItemTypeName<ObjectSlotItem>()
{  return "rs object slot"; }

template<>
inline const char *GetItemTypeName<ExportVarNameItem>()
{ return "rs export var"; }

template<>
inline const char *GetItemTypeName<ExportFuncNameItem>()
{  return "rs export func"; }

template<>
inline const char *GetItemTypeName<ExportForeachFuncItem>()
{ return "rs export foreach"; }

} // end namespace rsinfo

class RSInfo {
public:
  typedef android::Vector<std::pair<const char *,
                                    const uint8_t *> > DependencyTableTy;
  typedef android::Vector<std::pair<const char*, const char*> > PragmaListTy;
  typedef android::Vector<uint32_t> ObjectSlotListTy;
  typedef android::Vector<const char *> ExportVarNameListTy;
  typedef android::Vector<const char *> ExportFuncNameListTy;
  typedef android::Vector<std::pair<const char *,
                                    uint32_t> > ExportForeachFuncListTy;

public:
  // Calculate or load the SHA-1 information of the built-in dependencies.
  static bool LoadBuiltInSHA1Information();

  // Return the path of the RS info file corresponded to the given output
  // executable file.
  static android::String8 GetPath(const char *pFilename);

  static const char LibBCCPath[];
  static const char LibCompilerRTPath[];
  static const char LibRSPath[];
  static const char LibCLCorePath[];
  static const char LibCLCoreDebugPath[];
#if defined(ARCH_X86_HAVE_SSE2)
  static const char LibCLCoreX86Path[];
#endif
#if defined(ARCH_ARM_HAVE_NEON)
  static const char LibCLCoreNEONPath[];
#endif

private:
  // SHA-1 of the built-in dependencies. Will be initialized in
  // LoadBuiltInSHA1Information().
  static const uint8_t *LibBCCSHA1;
  static const uint8_t *LibCompilerRTSHA1;
  static const uint8_t *LibRSSHA1;
  static const uint8_t *LibCLCoreSHA1;
  static const uint8_t *LibCLCoreDebugSHA1;
#if defined(ARCH_ARM_HAVE_NEON)
  static const uint8_t *LibCLCoreNEONSHA1;
#endif

  static bool CheckDependency(const RSInfo &pInfo,
                              const char *pInputFilename,
                              const DependencyTableTy &pDeps);
  static bool AddBuiltInDependencies(RSInfo &pInfo);

  rsinfo::Header mHeader;

  char *mStringPool;

  // In most of the time, there're 4 source dependencies stored (libbcc.so,
  // libRS.so, libclcore and the input bitcode itself.)
  DependencyTableTy mDependencyTable;
  PragmaListTy mPragmas;
  ObjectSlotListTy mObjectSlots;
  ExportVarNameListTy mExportVarNames;
  ExportFuncNameListTy mExportFuncNames;
  ExportForeachFuncListTy mExportForeachFuncs;

  // Initialize an empty RSInfo with its size of string pool is pStringPoolSize.
  RSInfo(size_t pStringPoolSize);

  // layout() assigns value of offset in each ListHeader (i.e., it decides where
  // data should go in the file.) It also updates fields other than offset to
  // reflect the current RSInfo object states to mHeader.
  bool layout(off_t initial_offset);

public:
  ~RSInfo();

  // Implemented in RSInfoExtractor.cpp.
  static RSInfo *ExtractFromSource(const Source &pSource,
                                   const DependencyTableTy &pDeps);

  // Implemented in RSInfoReader.cpp.
  static RSInfo *ReadFromFile(InputFile &pInput,
                              const DependencyTableTy &pDeps);

  // Implemneted in RSInfoWriter.cpp
  bool write(OutputFile &pOutput);

  void dump() const;

  // const getter
  inline bool isThreadable() const
  { return mHeader.isThreadable; }
  inline bool hasDebugInformation() const
  { return mHeader.hasDebugInformation; }
  inline const DependencyTableTy &getDependencyTable() const
  { return mDependencyTable; }
  inline const PragmaListTy &getPragmas() const
  { return mPragmas; }
  inline const ObjectSlotListTy &getObjectSlots() const
  { return mObjectSlots; }
  inline const ExportVarNameListTy &getExportVarNames() const
  { return mExportVarNames; }
  inline const ExportFuncNameListTy &getExportFuncNames() const
  { return mExportFuncNames; }
  inline const ExportForeachFuncListTy &getExportForeachFuncs() const
  { return mExportForeachFuncs; }

  const char *getStringFromPool(rsinfo::StringIndexTy pStrIdx) const;
  rsinfo::StringIndexTy getStringIdxInPool(const char *pStr) const;

  // setter
  inline void setThreadable(bool pThreadable = true)
  { mHeader.isThreadable = pThreadable; }

public:
  enum FloatPrecision {
    FP_Full,
    FP_Relaxed,
    FP_Imprecise,
  };

  // Return the minimal floating point precision required for the associated
  // script.
  FloatPrecision getFloatPrecisionRequirement() const;
};

} // end namespace bcc

#endif  // BCC_RS_INFO_H
