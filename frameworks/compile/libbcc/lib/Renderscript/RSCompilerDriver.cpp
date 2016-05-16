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

#include "bcc/Renderscript/RSCompilerDriver.h"

#include <llvm/IR/Module.h>
#include <llvm/Support/CommandLine.h>
#include <llvm/Support/Path.h>
#include <llvm/Support/raw_ostream.h>

#include "bcinfo/BitcodeWrapper.h"

#include "bcc/Compiler.h"
#include "bcc/Renderscript/RSExecutable.h"
#include "bcc/Renderscript/RSScript.h"
#include "bcc/Support/CompilerConfig.h"
#include "bcc/Support/TargetCompilerConfigs.h"
#include "bcc/Source.h"
#include "bcc/Support/FileMutex.h"
#include "bcc/Support/Log.h"
#include "bcc/Support/InputFile.h"
#include "bcc/Support/Initialization.h"
#include "bcc/Support/Sha1Util.h"
#include "bcc/Support/OutputFile.h"

#ifdef HAVE_ANDROID_OS
#include <cutils/properties.h>
#endif
#include <utils/String8.h>
#include <utils/StopWatch.h>

using namespace bcc;

RSCompilerDriver::RSCompilerDriver(bool pUseCompilerRT) :
    mConfig(NULL), mCompiler(), mCompilerRuntime(NULL), mDebugContext(false),
    mEnableGlobalMerge(true) {
  init::Initialize();
  // Chain the symbol resolvers for compiler_rt and RS runtimes.
  if (pUseCompilerRT) {
    mCompilerRuntime = new CompilerRTSymbolResolver();
    mResolver.chainResolver(*mCompilerRuntime);
  }
  mResolver.chainResolver(mRSRuntime);
}

RSCompilerDriver::~RSCompilerDriver() {
  delete mCompilerRuntime;
  delete mConfig;
}

RSExecutable *
RSCompilerDriver::loadScript(const char *pCacheDir, const char *pResName,
                             const char *pBitcode, size_t pBitcodeSize) {
  //android::StopWatch load_time("bcc: RSCompilerDriver::loadScript time");
  if ((pCacheDir == NULL) || (pResName == NULL)) {
    ALOGE("Missing pCacheDir and/or pResName");
    return NULL;
  }

  if ((pBitcode == NULL) || (pBitcodeSize <= 0)) {
    ALOGE("No bitcode supplied! (bitcode: %p, size of bitcode: %zu)",
          pBitcode, pBitcodeSize);
    return NULL;
  }

  RSInfo::DependencyTableTy dep_info;
  uint8_t bitcode_sha1[20];
  Sha1Util::GetSHA1DigestFromBuffer(bitcode_sha1, pBitcode, pBitcodeSize);

  // {pCacheDir}/{pResName}.o
  llvm::SmallString<80> output_path(pCacheDir);
  llvm::sys::path::append(output_path, pResName);
  llvm::sys::path::replace_extension(output_path, ".o");

  dep_info.push(std::make_pair(output_path.c_str(), bitcode_sha1));

  //===--------------------------------------------------------------------===//
  // Acquire the read lock for reading the Script object file.
  //===--------------------------------------------------------------------===//
  FileMutex<FileBase::kReadLock> read_output_mutex(output_path.c_str());

  if (read_output_mutex.hasError() || !read_output_mutex.lock()) {
    ALOGE("Unable to acquire the read lock for %s! (%s)", output_path.c_str(),
          read_output_mutex.getErrorMessage().c_str());
    return NULL;
  }

  //===--------------------------------------------------------------------===//
  // Read the output object file.
  //===--------------------------------------------------------------------===//
  InputFile *object_file = new (std::nothrow) InputFile(output_path.c_str());

  if ((object_file == NULL) || object_file->hasError()) {
      //      ALOGE("Unable to open the %s for read! (%s)", output_path.c_str(),
      //            object_file->getErrorMessage().c_str());
    delete object_file;
    return NULL;
  }

  //===--------------------------------------------------------------------===//
  // Acquire the read lock on object_file for reading its RS info file.
  //===--------------------------------------------------------------------===//
  android::String8 info_path = RSInfo::GetPath(output_path.c_str());

  if (!object_file->lock()) {
    ALOGE("Unable to acquire the read lock on %s for reading %s! (%s)",
          output_path.c_str(), info_path.string(),
          object_file->getErrorMessage().c_str());
    delete object_file;
    return NULL;
  }

 //===---------------------------------------------------------------------===//
  // Open and load the RS info file.
  //===--------------------------------------------------------------------===//
  InputFile info_file(info_path.string());
  RSInfo *info = RSInfo::ReadFromFile(info_file, dep_info);

  // Release the lock on object_file.
  object_file->unlock();

  if (info == NULL) {
    delete object_file;
    return NULL;
  }

  //===--------------------------------------------------------------------===//
  // Create the RSExecutable.
  //===--------------------------------------------------------------------===//
  RSExecutable *result = RSExecutable::Create(*info, *object_file, mResolver);
  if (result == NULL) {
    delete object_file;
    delete info;
    return NULL;
  }

  return result;
}

#if defined(DEFAULT_ARM_CODEGEN)
extern llvm::cl::opt<bool> EnableGlobalMerge;
#endif

bool RSCompilerDriver::setupConfig(const RSScript &pScript) {
  bool changed = false;

  const llvm::CodeGenOpt::Level script_opt_level =
      static_cast<llvm::CodeGenOpt::Level>(pScript.getOptimizationLevel());

  if (mConfig != NULL) {
    // Renderscript bitcode may have their optimization flag configuration
    // different than the previous run of RS compilation.
    if (mConfig->getOptimizationLevel() != script_opt_level) {
      mConfig->setOptimizationLevel(script_opt_level);
      changed = true;
    }
  } else {
    // Haven't run the compiler ever.
    mConfig = new (std::nothrow) DefaultCompilerConfig();
    if (mConfig == NULL) {
      // Return false since mConfig remains NULL and out-of-memory.
      return false;
    }
    mConfig->setOptimizationLevel(script_opt_level);
#if defined(DEFAULT_ARM_CODEGEN)
    EnableGlobalMerge = mEnableGlobalMerge;
#endif
    changed = true;
  }

#if defined(DEFAULT_ARM_CODEGEN)
  // NEON should be disable when full-precision floating point is required.
  assert((pScript.getInfo() != NULL) && "NULL RS info!");
  if (pScript.getInfo()->getFloatPrecisionRequirement() == RSInfo::FP_Full) {
    // Must be ARMCompilerConfig.
    ARMCompilerConfig *arm_config = static_cast<ARMCompilerConfig *>(mConfig);
    changed |= arm_config->enableNEON(/* pEnable */false);
  }
#endif

  return changed;
}

Compiler::ErrorCode
RSCompilerDriver::compileScript(RSScript &pScript,
                                const char* pScriptName,
                                const char *pOutputPath,
                                const char *pRuntimePath,
                                const RSInfo::DependencyTableTy &pDeps,
                                bool pSkipLoad, bool pDumpIR) {
  //android::StopWatch compile_time("bcc: RSCompilerDriver::compileScript time");
  RSInfo *info = NULL;

  //===--------------------------------------------------------------------===//
  // Extract RS-specific information from source bitcode.
  //===--------------------------------------------------------------------===//
  // RS info may contains configuration (such as #optimization_level) to the
  // compiler therefore it should be extracted before compilation.
  info = RSInfo::ExtractFromSource(pScript.getSource(), pDeps);
  if (info == NULL) {
    return Compiler::kErrInvalidSource;
  }

  //===--------------------------------------------------------------------===//
  // Associate script with its info
  //===--------------------------------------------------------------------===//
  // This is required since RS compiler may need information in the info file
  // to do some transformation (e.g., expand foreach-able function.)
  pScript.setInfo(info);

  //===--------------------------------------------------------------------===//
  // Link RS script with Renderscript runtime.
  //===--------------------------------------------------------------------===//
  if (!RSScript::LinkRuntime(pScript, pRuntimePath)) {
    ALOGE("Failed to link script '%s' with Renderscript runtime!", pScriptName);
    return Compiler::kErrInvalidSource;
  }

  {
    // FIXME(srhines): Windows compilation can't use locking like this, but
    // we also don't need to worry about concurrent writers of the same file.
#ifndef USE_MINGW
    //===------------------------------------------------------------------===//
    // Acquire the write lock for writing output object file.
    //===------------------------------------------------------------------===//
    FileMutex<FileBase::kWriteLock> write_output_mutex(pOutputPath);

    if (write_output_mutex.hasError() || !write_output_mutex.lock()) {
      ALOGE("Unable to acquire the lock for writing %s! (%s)",
            pOutputPath, write_output_mutex.getErrorMessage().c_str());
      return Compiler::kErrInvalidSource;
    }
#endif

    // Open the output file for write.
    OutputFile output_file(pOutputPath,
                           FileBase::kTruncate | FileBase::kBinary);

    if (output_file.hasError()) {
        ALOGE("Unable to open %s for write! (%s)", pOutputPath,
              output_file.getErrorMessage().c_str());
      return Compiler::kErrInvalidSource;
    }

    // Setup the config to the compiler.
    bool compiler_need_reconfigure = setupConfig(pScript);

    if (mConfig == NULL) {
      ALOGE("Failed to setup config for RS compiler to compile %s!",
            pOutputPath);
      return Compiler::kErrInvalidSource;
    }

    if (compiler_need_reconfigure) {
      Compiler::ErrorCode err = mCompiler.config(*mConfig);
      if (err != Compiler::kSuccess) {
        ALOGE("Failed to config the RS compiler for %s! (%s)",pOutputPath,
              Compiler::GetErrorString(err));
        return Compiler::kErrInvalidSource;
      }
    }

    OutputFile *ir_file = NULL;
    llvm::raw_fd_ostream *IRStream = NULL;
    if (pDumpIR) {
      android::String8 path(pOutputPath);
      path.append(".ll");
      ir_file = new OutputFile(path.string(), FileBase::kTruncate);
      IRStream = ir_file->dup();
    }

    // Run the compiler.
    Compiler::ErrorCode compile_result = mCompiler.compile(pScript,
                                                           output_file, IRStream);

    if (ir_file) {
      ir_file->close();
      delete ir_file;
    }

    if (compile_result != Compiler::kSuccess) {
      ALOGE("Unable to compile the source to file %s! (%s)", pOutputPath,
            Compiler::GetErrorString(compile_result));
      return Compiler::kErrInvalidSource;
    }
  }

  // No need to produce an RSExecutable in this case.
  // TODO: Error handling in this case is nonexistent.
  if (pSkipLoad) {
    return Compiler::kSuccess;
  }

  {
    android::String8 info_path = RSInfo::GetPath(pOutputPath);
    OutputFile info_file(info_path.string(), FileBase::kTruncate);

    if (info_file.hasError()) {
      ALOGE("Failed to open the info file %s for write! (%s)",
            info_path.string(), info_file.getErrorMessage().c_str());
      return Compiler::kErrInvalidSource;
    }

    FileMutex<FileBase::kWriteLock> write_info_mutex(info_path.string());
    if (write_info_mutex.hasError() || !write_info_mutex.lock()) {
      ALOGE("Unable to acquire the lock for writing %s! (%s)",
            info_path.string(), write_info_mutex.getErrorMessage().c_str());
      return Compiler::kErrInvalidSource;
    }

    // Perform the write.
    if (!info->write(info_file)) {
      ALOGE("Failed to sync the RS info file %s!", info_path.string());
      return Compiler::kErrInvalidSource;
    }
  }

  return Compiler::kSuccess;
}

bool RSCompilerDriver::build(BCCContext &pContext,
                             const char *pCacheDir,
                             const char *pResName,
                             const char *pBitcode,
                             size_t pBitcodeSize,
                             const char *pRuntimePath,
                             RSLinkRuntimeCallback pLinkRuntimeCallback,
                             bool pDumpIR) {
    //  android::StopWatch build_time("bcc: RSCompilerDriver::build time");
  //===--------------------------------------------------------------------===//
  // Check parameters.
  //===--------------------------------------------------------------------===//
  if ((pCacheDir == NULL) || (pResName == NULL)) {
    ALOGE("Invalid parameter passed to RSCompilerDriver::build()! (cache dir: "
          "%s, resource name: %s)", ((pCacheDir) ? pCacheDir : "(null)"),
                                    ((pResName) ? pResName : "(null)"));
    return false;
  }

  if ((pBitcode == NULL) || (pBitcodeSize <= 0)) {
    ALOGE("No bitcode supplied! (bitcode: %p, size of bitcode: %u)",
          pBitcode, static_cast<unsigned>(pBitcodeSize));
    return false;
  }

  //===--------------------------------------------------------------------===//
  // Prepare dependency information.
  //===--------------------------------------------------------------------===//
  RSInfo::DependencyTableTy dep_info;
  uint8_t bitcode_sha1[20];
  Sha1Util::GetSHA1DigestFromBuffer(bitcode_sha1, pBitcode, pBitcodeSize);

  //===--------------------------------------------------------------------===//
  // Construct output path.
  // {pCacheDir}/{pResName}.o
  //===--------------------------------------------------------------------===//
  llvm::SmallString<80> output_path(pCacheDir);
  llvm::sys::path::append(output_path, pResName);
  llvm::sys::path::replace_extension(output_path, ".o");

  dep_info.push(std::make_pair(output_path.c_str(), bitcode_sha1));

  //===--------------------------------------------------------------------===//
  // Load the bitcode and create script.
  //===--------------------------------------------------------------------===//
  Source *source = Source::CreateFromBuffer(pContext, pResName,
                                            pBitcode, pBitcodeSize);
  if (source == NULL) {
    return false;
  }

  RSScript *script = new (std::nothrow) RSScript(*source);
  if (script == NULL) {
    ALOGE("Out of memory when create Script object for '%s'! (output: %s)",
          pResName, output_path.c_str());
    delete source;
    return false;
  }

  script->setLinkRuntimeCallback(pLinkRuntimeCallback);

  // Read information from bitcode wrapper.
  bcinfo::BitcodeWrapper wrapper(pBitcode, pBitcodeSize);
  script->setCompilerVersion(wrapper.getCompilerVersion());
  script->setOptimizationLevel(static_cast<RSScript::OptimizationLevel>(
                                   wrapper.getOptimizationLevel()));

  //===--------------------------------------------------------------------===//
  // Compile the script
  //===--------------------------------------------------------------------===//
  Compiler::ErrorCode status = compileScript(*script, pResName,
                                             output_path.c_str(),
                                             pRuntimePath, dep_info, false,
                                             pDumpIR);

  // Script is no longer used. Free it to get more memory.
  delete script;

  if (status != Compiler::kSuccess) {
    return false;
  }

  return true;
}


bool RSCompilerDriver::build(RSScript &pScript, const char *pOut,
                             const char *pRuntimePath) {
  RSInfo::DependencyTableTy dep_info;
  RSInfo *info = RSInfo::ExtractFromSource(pScript.getSource(), dep_info);
  if (info == NULL) {
    return false;
  }
  pScript.setInfo(info);

  // Embed the info string directly in the ELF, since this path is for an
  // offline (host) compilation.
  pScript.setEmbedInfo(true);

  Compiler::ErrorCode status = compileScript(pScript, pOut, pOut, pRuntimePath,
                                             dep_info, true);
  if (status != Compiler::kSuccess) {
    return false;
  }

  return true;
}

