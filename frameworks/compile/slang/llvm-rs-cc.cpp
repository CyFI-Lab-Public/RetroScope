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

#include <cstdlib>
#include <list>
#include <set>
#include <string>
#include <utility>
#include <vector>

#include "clang/Driver/DriverDiagnostic.h"
#include "clang/Driver/Options.h"

#include "clang/Basic/DiagnosticOptions.h"
#include "clang/Frontend/TextDiagnosticPrinter.h"
#include "clang/Frontend/Utils.h"

#include "llvm/ADT/SmallVector.h"
#include "llvm/ADT/IntrusiveRefCntPtr.h"
#include "llvm/ADT/OwningPtr.h"

#include "llvm/Option/Arg.h"
#include "llvm/Option/ArgList.h"
#include "llvm/Option/Option.h"
#include "llvm/Option/OptTable.h"
#include "llvm/Support/CommandLine.h"
#include "llvm/Support/ManagedStatic.h"
#include "llvm/Support/MemoryBuffer.h"
#include "llvm/Support/Path.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Support/system_error.h"
#include "llvm/Target/TargetMachine.h"

#include "slang.h"
#include "slang_assert.h"
#include "slang_diagnostic_buffer.h"
#include "slang_rs.h"
#include "slang_rs_reflect_utils.h"

using clang::driver::options::DriverOption;
using llvm::opt::arg_iterator;
using llvm::opt::Arg;
using llvm::opt::ArgList;
using llvm::opt::InputArgList;
using llvm::opt::Option;
using llvm::opt::OptTable;

// SaveStringInSet, ExpandArgsFromBuf and ExpandArgv are all copied from
// $(CLANG_ROOT)/tools/driver/driver.cpp for processing argc/argv passed in
// main().
static inline const char *SaveStringInSet(std::set<std::string> &SavedStrings,
                                          llvm::StringRef S) {
  return SavedStrings.insert(S).first->c_str();
}
static void ExpandArgsFromBuf(const char *Arg,
                              llvm::SmallVectorImpl<const char*> &ArgVector,
                              std::set<std::string> &SavedStrings);
static void ExpandArgv(int argc, const char **argv,
                       llvm::SmallVectorImpl<const char*> &ArgVector,
                       std::set<std::string> &SavedStrings);

enum {
  OPT_INVALID = 0,  // This is not an option ID.
#define PREFIX(NAME, VALUE)
#define OPTION(PREFIX, NAME, ID, KIND, GROUP, ALIAS, ALIASARGS, FLAGS, PARAM, \
               HELPTEXT, METAVAR) OPT_##ID,
#include "RSCCOptions.inc"
  LastOption
#undef OPTION
#undef PREFIX
};

#define PREFIX(NAME, VALUE) const char *const NAME[] = VALUE;
#define OPTION(PREFIX, NAME, ID, KIND, GROUP, ALIAS, ALIASARGS, FLAGS, PARAM, \
               HELPTEXT, METAVAR)
#include "RSCCOptions.inc"
#undef OPTION
#undef PREFIX

static const OptTable::Info RSCCInfoTable[] = {
#define PREFIX(NAME, VALUE)
#define OPTION(PREFIX, NAME, ID, KIND, GROUP, ALIAS, ALIASARGS, FLAGS, PARAM, \
               HELPTEXT, METAVAR)   \
  { PREFIX, NAME, HELPTEXT, METAVAR, OPT_##ID, Option::KIND##Class, PARAM, \
    FLAGS, OPT_##GROUP, OPT_##ALIAS, ALIASARGS },
#include "RSCCOptions.inc"
#undef OPTION
#undef PREFIX
};

class RSCCOptTable : public OptTable {
 public:
  RSCCOptTable()
      : OptTable(RSCCInfoTable,
                 sizeof(RSCCInfoTable) / sizeof(RSCCInfoTable[0])) {
  }
};

OptTable *createRSCCOptTable() {
  return new RSCCOptTable();
}

///////////////////////////////////////////////////////////////////////////////

class RSCCOptions {
 public:
  // The include search paths
  std::vector<std::string> mIncludePaths;

  // The output directory, if any.
  std::string mOutputDir;

  // The output type
  slang::Slang::OutputType mOutputType;

  unsigned mAllowRSPrefix : 1;

  // The name of the target triple to compile for.
  std::string mTriple;

  // The name of the target CPU to generate code for.
  std::string mCPU;

  // The list of target specific features to enable or disable -- this should
  // be a list of strings starting with by '+' or '-'.
  std::vector<std::string> mFeatures;

  std::string mJavaReflectionPathBase;

  std::string mJavaReflectionPackageName;

  std::string mRSPackageName;

  slang::BitCodeStorageType mBitcodeStorage;

  unsigned mOutputDep : 1;

  std::string mOutputDepDir;

  std::vector<std::string> mAdditionalDepTargets;

  unsigned mShowHelp : 1;  // Show the -help text.
  unsigned mShowVersion : 1;  // Show the -version text.

  unsigned int mTargetAPI;

  // Enable emission of debugging symbols
  unsigned mDebugEmission : 1;

  // The optimization level used in CodeGen, and encoded in emitted bitcode
  llvm::CodeGenOpt::Level mOptimizationLevel;

  RSCCOptions() {
    mOutputType = slang::Slang::OT_Bitcode;
    // Triple/CPU/Features must be hard-coded to our chosen portable ABI.
    mTriple = "armv7-none-linux-gnueabi";
    mCPU = "";
    slangAssert(mFeatures.empty());
    mFeatures.push_back("+long64");
    mBitcodeStorage = slang::BCST_APK_RESOURCE;
    mOutputDep = 0;
    mShowHelp = 0;
    mShowVersion = 0;
    mTargetAPI = RS_VERSION;
    mDebugEmission = 0;
    mOptimizationLevel = llvm::CodeGenOpt::Aggressive;
  }
};

// ParseArguments -
static void ParseArguments(llvm::SmallVectorImpl<const char*> &ArgVector,
                           llvm::SmallVectorImpl<const char*> &Inputs,
                           RSCCOptions &Opts,
                           clang::DiagnosticsEngine &DiagEngine) {
  if (ArgVector.size() > 1) {
    const char **ArgBegin = ArgVector.data() + 1;
    const char **ArgEnd = ArgVector.data() + ArgVector.size();
    unsigned MissingArgIndex, MissingArgCount;
    llvm::OwningPtr<OptTable> OptParser(createRSCCOptTable());
    llvm::OwningPtr<InputArgList> Args(
      OptParser->ParseArgs(ArgBegin, ArgEnd, MissingArgIndex, MissingArgCount));

    // Check for missing argument error.
    if (MissingArgCount)
      DiagEngine.Report(clang::diag::err_drv_missing_argument)
        << Args->getArgString(MissingArgIndex) << MissingArgCount;

    clang::DiagnosticOptions DiagOpts;
    DiagOpts.IgnoreWarnings = Args->hasArg(OPT_w);
    DiagOpts.Warnings = Args->getAllArgValues(OPT_W);
    clang::ProcessWarningOptions(DiagEngine, DiagOpts);

    // Issue errors on unknown arguments.
    for (arg_iterator it = Args->filtered_begin(OPT_UNKNOWN),
        ie = Args->filtered_end(); it != ie; ++it)
      DiagEngine.Report(clang::diag::err_drv_unknown_argument)
        << (*it)->getAsString(*Args);

    for (ArgList::const_iterator it = Args->begin(), ie = Args->end();
        it != ie; ++it) {
      const Arg *A = *it;
      if (A->getOption().getKind() == Option::InputClass)
        Inputs.push_back(A->getValue());
    }

    Opts.mIncludePaths = Args->getAllArgValues(OPT_I);

    Opts.mOutputDir = Args->getLastArgValue(OPT_o);

    if (const Arg *A = Args->getLastArg(OPT_M_Group)) {
      switch (A->getOption().getID()) {
        case OPT_M: {
          Opts.mOutputDep = 1;
          Opts.mOutputType = slang::Slang::OT_Dependency;
          break;
        }
        case OPT_MD: {
          Opts.mOutputDep = 1;
          Opts.mOutputType = slang::Slang::OT_Bitcode;
          break;
        }
        default: {
          slangAssert(false && "Invalid option in M group!");
        }
      }
    }

    if (const Arg *A = Args->getLastArg(OPT_Output_Type_Group)) {
      switch (A->getOption().getID()) {
        case OPT_emit_asm: {
          Opts.mOutputType = slang::Slang::OT_Assembly;
          break;
        }
        case OPT_emit_llvm: {
          Opts.mOutputType = slang::Slang::OT_LLVMAssembly;
          break;
        }
        case OPT_emit_bc: {
          Opts.mOutputType = slang::Slang::OT_Bitcode;
          break;
        }
        case OPT_emit_nothing: {
          Opts.mOutputType = slang::Slang::OT_Nothing;
          break;
        }
        default: {
          slangAssert(false && "Invalid option in output type group!");
        }
      }
    }

    if (Opts.mOutputDep &&
        ((Opts.mOutputType != slang::Slang::OT_Bitcode) &&
         (Opts.mOutputType != slang::Slang::OT_Dependency)))
      DiagEngine.Report(clang::diag::err_drv_argument_not_allowed_with)
          << Args->getLastArg(OPT_M_Group)->getAsString(*Args)
          << Args->getLastArg(OPT_Output_Type_Group)->getAsString(*Args);

    Opts.mAllowRSPrefix = Args->hasArg(OPT_allow_rs_prefix);

    Opts.mJavaReflectionPathBase =
        Args->getLastArgValue(OPT_java_reflection_path_base);
    Opts.mJavaReflectionPackageName =
        Args->getLastArgValue(OPT_java_reflection_package_name);

    Opts.mRSPackageName = Args->getLastArgValue(OPT_rs_package_name);

    llvm::StringRef BitcodeStorageValue =
        Args->getLastArgValue(OPT_bitcode_storage);
    if (BitcodeStorageValue == "ar")
      Opts.mBitcodeStorage = slang::BCST_APK_RESOURCE;
    else if (BitcodeStorageValue == "jc")
      Opts.mBitcodeStorage = slang::BCST_JAVA_CODE;
    else if (!BitcodeStorageValue.empty())
      DiagEngine.Report(clang::diag::err_drv_invalid_value)
          << OptParser->getOptionName(OPT_bitcode_storage)
          << BitcodeStorageValue;

    if (Args->hasArg(OPT_reflect_cpp)) {
      Opts.mBitcodeStorage = slang::BCST_CPP_CODE;
      // mJavaReflectionPathBase isn't set for C++ reflected builds
      // set it to mOutputDir so we can use the path sanely from
      // RSReflectionBase later on
      Opts.mJavaReflectionPathBase = Opts.mOutputDir;
    }

    Opts.mOutputDepDir =
        Args->getLastArgValue(OPT_output_dep_dir, Opts.mOutputDir);
    Opts.mAdditionalDepTargets =
        Args->getAllArgValues(OPT_additional_dep_target);

    Opts.mShowHelp = Args->hasArg(OPT_help);
    Opts.mShowVersion = Args->hasArg(OPT_version);
    Opts.mDebugEmission = Args->hasArg(OPT_emit_g);

    size_t OptLevel = clang::getLastArgIntValue(*Args,
                                                OPT_optimization_level,
                                                3,
                                                DiagEngine);

    Opts.mOptimizationLevel = OptLevel == 0 ? llvm::CodeGenOpt::None
                                            : llvm::CodeGenOpt::Aggressive;

    Opts.mTargetAPI = clang::getLastArgIntValue(*Args,
                                                OPT_target_api,
                                                RS_VERSION,
                                                DiagEngine);
  }

  return;
}

static const char *DetermineOutputFile(const std::string &OutputDir,
                                       const char *InputFile,
                                       slang::Slang::OutputType OutputType,
                                       std::set<std::string> &SavedStrings) {
  if (OutputType == slang::Slang::OT_Nothing)
    return "/dev/null";

  std::string OutputFile(OutputDir);

  // Append '/' to Opts.mOutputDir if not presents
  if (!OutputFile.empty() &&
      (OutputFile[OutputFile.size() - 1]) != OS_PATH_SEPARATOR)
    OutputFile.append(1, OS_PATH_SEPARATOR);

  if (OutputType == slang::Slang::OT_Dependency) {
    // The build system wants the .d file name stem to be exactly the same as
    // the source .rs file, instead of the .bc file.
    OutputFile.append(slang::RSSlangReflectUtils::GetFileNameStem(InputFile));
  } else {
    OutputFile.append(
        slang::RSSlangReflectUtils::BCFileNameFromRSFileName(InputFile));
  }

  switch (OutputType) {
    case slang::Slang::OT_Dependency: {
      OutputFile.append(".d");
      break;
    }
    case slang::Slang::OT_Assembly: {
      OutputFile.append(".S");
      break;
    }
    case slang::Slang::OT_LLVMAssembly: {
      OutputFile.append(".ll");
      break;
    }
    case slang::Slang::OT_Object: {
      OutputFile.append(".o");
      break;
    }
    case slang::Slang::OT_Bitcode: {
      OutputFile.append(".bc");
      break;
    }
    case slang::Slang::OT_Nothing:
    default: {
      slangAssert(false && "Invalid output type!");
    }
  }

  return SaveStringInSet(SavedStrings, OutputFile);
}

#define str(s) #s
#define wrap_str(s) str(s)
static void llvm_rs_cc_VersionPrinter() {
  llvm::raw_ostream &OS = llvm::outs();
  OS << "llvm-rs-cc: Renderscript compiler\n"
     << "  (http://developer.android.com/guide/topics/renderscript)\n"
     << "  based on LLVM (http://llvm.org):\n";
  OS << "  Built " << __DATE__ << " (" << __TIME__ ").\n";
  OS << "  Target APIs: " << SLANG_MINIMUM_TARGET_API << " - "
     << SLANG_MAXIMUM_TARGET_API;
  OS << "\n  Build type: " << wrap_str(TARGET_BUILD_VARIANT);
#ifndef __DISABLE_ASSERTS
  OS << " with assertions";
#endif
  OS << ".\n";
  return;
}
#undef wrap_str
#undef str

int main(int argc, const char **argv) {
  std::set<std::string> SavedStrings;
  llvm::SmallVector<const char*, 256> ArgVector;
  RSCCOptions Opts;
  llvm::SmallVector<const char*, 16> Inputs;
  std::string Argv0;

  atexit(llvm::llvm_shutdown);

  ExpandArgv(argc, argv, ArgVector, SavedStrings);

  // Argv0
  Argv0 = llvm::sys::path::stem(ArgVector[0]);

  // Setup diagnostic engine
  slang::DiagnosticBuffer *DiagClient = new slang::DiagnosticBuffer();

  llvm::IntrusiveRefCntPtr<clang::DiagnosticIDs> DiagIDs(
    new clang::DiagnosticIDs());

  llvm::IntrusiveRefCntPtr<clang::DiagnosticOptions> DiagOpts(
    new clang::DiagnosticOptions());
  clang::DiagnosticsEngine DiagEngine(DiagIDs, &*DiagOpts, DiagClient, true);

  slang::Slang::GlobalInitialization();

  ParseArguments(ArgVector, Inputs, Opts, DiagEngine);

  // Exits when there's any error occurred during parsing the arguments
  if (DiagEngine.hasErrorOccurred()) {
    llvm::errs() << DiagClient->str();
    return 1;
  }

  if (Opts.mShowHelp) {
    llvm::OwningPtr<OptTable> OptTbl(createRSCCOptTable());
    OptTbl->PrintHelp(llvm::outs(), Argv0.c_str(),
                      "Renderscript source compiler");
    return 0;
  }

  if (Opts.mShowVersion) {
    llvm_rs_cc_VersionPrinter();
    return 0;
  }

  // No input file
  if (Inputs.empty()) {
    DiagEngine.Report(clang::diag::err_drv_no_input_files);
    llvm::errs() << DiagClient->str();
    return 1;
  }

  // Prepare input data for RS compiler.
  std::list<std::pair<const char*, const char*> > IOFiles;
  std::list<std::pair<const char*, const char*> > DepFiles;

  llvm::OwningPtr<slang::SlangRS> Compiler(new slang::SlangRS());

  Compiler->init(Opts.mTriple, Opts.mCPU, Opts.mFeatures, &DiagEngine,
                 DiagClient);

  for (int i = 0, e = Inputs.size(); i != e; i++) {
    const char *InputFile = Inputs[i];
    const char *OutputFile =
        DetermineOutputFile(Opts.mOutputDir, InputFile,
                            Opts.mOutputType, SavedStrings);

    if (Opts.mOutputDep) {
      const char *BCOutputFile, *DepOutputFile;

      if (Opts.mOutputType == slang::Slang::OT_Bitcode)
        BCOutputFile = OutputFile;
      else
        BCOutputFile = DetermineOutputFile(Opts.mOutputDepDir,
                                           InputFile,
                                           slang::Slang::OT_Bitcode,
                                           SavedStrings);

      if (Opts.mOutputType == slang::Slang::OT_Dependency)
        DepOutputFile = OutputFile;
      else
        DepOutputFile = DetermineOutputFile(Opts.mOutputDepDir,
                                            InputFile,
                                            slang::Slang::OT_Dependency,
                                            SavedStrings);

      DepFiles.push_back(std::make_pair(BCOutputFile, DepOutputFile));
    }

    IOFiles.push_back(std::make_pair(InputFile, OutputFile));
  }

  // Let's rock!
  int CompileFailed = !Compiler->compile(IOFiles,
                                         DepFiles,
                                         Opts.mIncludePaths,
                                         Opts.mAdditionalDepTargets,
                                         Opts.mOutputType,
                                         Opts.mBitcodeStorage,
                                         Opts.mAllowRSPrefix,
                                         Opts.mOutputDep,
                                         Opts.mTargetAPI,
                                         Opts.mDebugEmission,
                                         Opts.mOptimizationLevel,
                                         Opts.mJavaReflectionPathBase,
                                         Opts.mJavaReflectionPackageName,
                                         Opts.mRSPackageName);

  Compiler->reset();

  return CompileFailed;
}

///////////////////////////////////////////////////////////////////////////////

// ExpandArgsFromBuf -
static void ExpandArgsFromBuf(const char *Arg,
                              llvm::SmallVectorImpl<const char*> &ArgVector,
                              std::set<std::string> &SavedStrings) {
  const char *FName = Arg + 1;
  llvm::OwningPtr<llvm::MemoryBuffer> MemBuf;
  if (llvm::MemoryBuffer::getFile(FName, MemBuf)) {
    // Unable to open the file
    ArgVector.push_back(SaveStringInSet(SavedStrings, Arg));
    return;
  }

  const char *Buf = MemBuf->getBufferStart();
  char InQuote = ' ';
  std::string CurArg;

  for (const char *P = Buf; ; ++P) {
    if (*P == '\0' || (isspace(*P) && InQuote == ' ')) {
      if (!CurArg.empty()) {
        if (CurArg[0] != '@') {
          ArgVector.push_back(SaveStringInSet(SavedStrings, CurArg));
        } else {
          ExpandArgsFromBuf(CurArg.c_str(), ArgVector, SavedStrings);
        }

        CurArg = "";
      }
      if (*P == '\0')
        break;
      else
        continue;
    }

    if (isspace(*P)) {
      if (InQuote != ' ')
        CurArg.push_back(*P);
      continue;
    }

    if (*P == '"' || *P == '\'') {
      if (InQuote == *P)
        InQuote = ' ';
      else if (InQuote == ' ')
        InQuote = *P;
      else
        CurArg.push_back(*P);
      continue;
    }

    if (*P == '\\') {
      ++P;
      if (*P != '\0')
        CurArg.push_back(*P);
      continue;
    }
    CurArg.push_back(*P);
  }
}

// ExpandArgsFromBuf -
static void ExpandArgv(int argc, const char **argv,
                       llvm::SmallVectorImpl<const char*> &ArgVector,
                       std::set<std::string> &SavedStrings) {
  for (int i = 0; i < argc; ++i) {
    const char *Arg = argv[i];
    if (Arg[0] != '@') {
      ArgVector.push_back(SaveStringInSet(SavedStrings, std::string(Arg)));
      continue;
    }

    ExpandArgsFromBuf(Arg, ArgVector, SavedStrings);
  }
}
