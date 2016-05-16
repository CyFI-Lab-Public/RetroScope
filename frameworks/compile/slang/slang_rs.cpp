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

#include "slang_rs.h"

#include <cstring>
#include <list>
#include <sstream>
#include <string>
#include <utility>
#include <vector>

#include "clang/Basic/SourceLocation.h"

#include "clang/Frontend/FrontendDiagnostic.h"

#include "clang/Sema/SemaDiagnostic.h"

#include "llvm/Support/Path.h"

#include "os_sep.h"
#include "slang_rs_backend.h"
#include "slang_rs_context.h"
#include "slang_rs_export_type.h"

#include "slang_rs_reflection_cpp.h"

namespace slang {

#define FS_SUFFIX  "fs"

#define RS_HEADER_SUFFIX  "rsh"

/* RS_HEADER_ENTRY(name) */
#define ENUM_RS_HEADER()  \
  RS_HEADER_ENTRY(rs_allocation) \
  RS_HEADER_ENTRY(rs_atomic) \
  RS_HEADER_ENTRY(rs_cl) \
  RS_HEADER_ENTRY(rs_core) \
  RS_HEADER_ENTRY(rs_debug) \
  RS_HEADER_ENTRY(rs_element) \
  RS_HEADER_ENTRY(rs_graphics) \
  RS_HEADER_ENTRY(rs_math) \
  RS_HEADER_ENTRY(rs_mesh) \
  RS_HEADER_ENTRY(rs_matrix) \
  RS_HEADER_ENTRY(rs_object) \
  RS_HEADER_ENTRY(rs_program) \
  RS_HEADER_ENTRY(rs_quaternion) \
  RS_HEADER_ENTRY(rs_sampler) \
  RS_HEADER_ENTRY(rs_time) \
  RS_HEADER_ENTRY(rs_types) \

// Returns true if \p Filename ends in ".fs".
bool SlangRS::isFilterscript(const char *Filename) {
  const char *c = strrchr(Filename, '.');
  if (c && !strncmp(FS_SUFFIX, c + 1, strlen(FS_SUFFIX) + 1)) {
    return true;
  } else {
    return false;
  }
}

bool SlangRS::reflectToJava(const std::string &OutputPathBase,
                            const std::string &RSPackageName) {
  return mRSContext->reflectToJava(OutputPathBase,
                                   RSPackageName,
                                   getInputFileName(),
                                   getOutputFileName());
}

bool SlangRS::generateBitcodeAccessor(const std::string &OutputPathBase,
                                      const std::string &PackageName) {
  RSSlangReflectUtils::BitCodeAccessorContext BCAccessorContext;

  BCAccessorContext.rsFileName = getInputFileName().c_str();
  BCAccessorContext.bcFileName = getOutputFileName().c_str();
  BCAccessorContext.reflectPath = OutputPathBase.c_str();
  BCAccessorContext.packageName = PackageName.c_str();
  BCAccessorContext.bcStorage = BCST_JAVA_CODE;   // Must be BCST_JAVA_CODE

  return RSSlangReflectUtils::GenerateBitCodeAccessor(BCAccessorContext);
}

bool SlangRS::checkODR(const char *CurInputFile) {
  for (RSContext::ExportableList::iterator I = mRSContext->exportable_begin(),
          E = mRSContext->exportable_end();
       I != E;
       I++) {
    RSExportable *RSE = *I;
    if (RSE->getKind() != RSExportable::EX_TYPE)
      continue;

    RSExportType *ET = static_cast<RSExportType *>(RSE);
    if (ET->getClass() != RSExportType::ExportClassRecord)
      continue;

    RSExportRecordType *ERT = static_cast<RSExportRecordType *>(ET);

    // Artificial record types (create by us not by user in the source) always
    // conforms the ODR.
    if (ERT->isArtificial())
      continue;

    // Key to lookup ERT in ReflectedDefinitions
    llvm::StringRef RDKey(ERT->getName());
    ReflectedDefinitionListTy::const_iterator RD =
        ReflectedDefinitions.find(RDKey);

    if (RD != ReflectedDefinitions.end()) {
      const RSExportRecordType *Reflected = RD->getValue().first;
      // There's a record (struct) with the same name reflected before. Enforce
      // ODR checking - the Reflected must hold *exactly* the same "definition"
      // as the one defined previously. We say two record types A and B have the
      // same definition iff:
      //
      //  struct A {              struct B {
      //    Type(a1) a1,            Type(b1) b1,
      //    Type(a2) a2,            Type(b1) b2,
      //    ...                     ...
      //    Type(aN) aN             Type(b3) b3,
      //  };                      }
      //  Cond. #1. They have same number of fields, i.e., N = M;
      //  Cond. #2. for (i := 1 to N)
      //              Type(ai) = Type(bi) must hold;
      //  Cond. #3. for (i := 1 to N)
      //              Name(ai) = Name(bi) must hold;
      //
      // where,
      //  Type(F) = the type of field F and
      //  Name(F) = the field name.

      bool PassODR = false;
      // Cond. #1 and Cond. #2
      if (Reflected->equals(ERT)) {
        // Cond #3.
        RSExportRecordType::const_field_iterator AI = Reflected->fields_begin(),
                                                 BI = ERT->fields_begin();

        for (unsigned i = 0, e = Reflected->getFields().size(); i != e; i++) {
          if ((*AI)->getName() != (*BI)->getName())
            break;
          AI++;
          BI++;
        }
        PassODR = (AI == (Reflected->fields_end()));
      }

      if (!PassODR) {
        getDiagnostics().Report(mDiagErrorODR) << Reflected->getName()
                                               << getInputFileName()
                                               << RD->getValue().second;
        return false;
      }
    } else {
      llvm::StringMapEntry<ReflectedDefinitionTy> *ME =
          llvm::StringMapEntry<ReflectedDefinitionTy>::Create(RDKey.begin(),
                                                              RDKey.end());
      ME->setValue(std::make_pair(ERT, CurInputFile));

      if (!ReflectedDefinitions.insert(ME))
        delete ME;

      // Take the ownership of ERT such that it won't be freed in ~RSContext().
      ERT->keep();
    }
  }
  return true;
}

void SlangRS::initDiagnostic() {
  clang::DiagnosticsEngine &DiagEngine = getDiagnostics();

  if (DiagEngine.setDiagnosticGroupMapping("implicit-function-declaration",
                                           clang::diag::MAP_ERROR))
    DiagEngine.Report(clang::diag::warn_unknown_warning_option)
      << "implicit-function-declaration";

  DiagEngine.setDiagnosticMapping(
    clang::diag::ext_typecheck_convert_discards_qualifiers,
    clang::diag::MAP_ERROR,
    clang::SourceLocation());

  mDiagErrorInvalidOutputDepParameter =
    DiagEngine.getCustomDiagID(
      clang::DiagnosticsEngine::Error,
      "invalid parameter for output dependencies files.");

  mDiagErrorODR =
    DiagEngine.getCustomDiagID(
      clang::DiagnosticsEngine::Error,
      "type '%0' in different translation unit (%1 v.s. %2) "
      "has incompatible type definition");

  mDiagErrorTargetAPIRange =
    DiagEngine.getCustomDiagID(
      clang::DiagnosticsEngine::Error,
      "target API level '%0' is out of range ('%1' - '%2')");
}

void SlangRS::initPreprocessor() {
  clang::Preprocessor &PP = getPreprocessor();

  std::stringstream RSH;
  RSH << "#define RS_VERSION " << mTargetAPI << std::endl;
  RSH << "#include \"rs_core." RS_HEADER_SUFFIX "\"" << std::endl;
  PP.setPredefines(RSH.str());
}

void SlangRS::initASTContext() {
  mRSContext = new RSContext(getPreprocessor(),
                             getASTContext(),
                             getTargetInfo(),
                             &mPragmas,
                             mTargetAPI,
                             &mGeneratedFileNames);
}

clang::ASTConsumer
*SlangRS::createBackend(const clang::CodeGenOptions& CodeGenOpts,
                        llvm::raw_ostream *OS,
                        Slang::OutputType OT) {
    return new RSBackend(mRSContext,
                         &getDiagnostics(),
                         CodeGenOpts,
                         getTargetOptions(),
                         &mPragmas,
                         OS,
                         OT,
                         getSourceManager(),
                         mAllowRSPrefix,
                         mIsFilterscript);
}

bool SlangRS::IsRSHeaderFile(const char *File) {
#define RS_HEADER_ENTRY(name)  \
  if (::strcmp(File, #name "."RS_HEADER_SUFFIX) == 0)  \
    return true;
ENUM_RS_HEADER()
#undef RS_HEADER_ENTRY
  return false;
}

bool SlangRS::IsLocInRSHeaderFile(const clang::SourceLocation &Loc,
                                  const clang::SourceManager &SourceMgr) {
  clang::FullSourceLoc FSL(Loc, SourceMgr);
  clang::PresumedLoc PLoc = SourceMgr.getPresumedLoc(FSL);

  const char *Filename = PLoc.getFilename();
  if (!Filename) {
    return false;
  } else {
    return IsRSHeaderFile(llvm::sys::path::filename(Filename).data());
  }
}

SlangRS::SlangRS()
  : Slang(), mRSContext(NULL), mAllowRSPrefix(false), mTargetAPI(0),
    mIsFilterscript(false) {
}

bool SlangRS::compile(
    const std::list<std::pair<const char*, const char*> > &IOFiles,
    const std::list<std::pair<const char*, const char*> > &DepFiles,
    const std::vector<std::string> &IncludePaths,
    const std::vector<std::string> &AdditionalDepTargets,
    Slang::OutputType OutputType, BitCodeStorageType BitcodeStorage,
    bool AllowRSPrefix, bool OutputDep,
    unsigned int TargetAPI, bool EmitDebug,
    llvm::CodeGenOpt::Level OptimizationLevel,
    const std::string &JavaReflectionPathBase,
    const std::string &JavaReflectionPackageName,
    const std::string &RSPackageName) {
  if (IOFiles.empty())
    return true;

  if (OutputDep && (DepFiles.size() != IOFiles.size())) {
    getDiagnostics().Report(mDiagErrorInvalidOutputDepParameter);
    return false;
  }

  std::string RealPackageName;

  const char *InputFile, *OutputFile, *BCOutputFile, *DepOutputFile;
  std::list<std::pair<const char*, const char*> >::const_iterator
      IOFileIter = IOFiles.begin(), DepFileIter = DepFiles.begin();

  setIncludePaths(IncludePaths);
  setOutputType(OutputType);
  if (OutputDep) {
    setAdditionalDepTargets(AdditionalDepTargets);
  }

  setDebugMetadataEmission(EmitDebug);

  setOptimizationLevel(OptimizationLevel);

  mAllowRSPrefix = AllowRSPrefix;

  mTargetAPI = TargetAPI;
  if (mTargetAPI < SLANG_MINIMUM_TARGET_API ||
      mTargetAPI > SLANG_MAXIMUM_TARGET_API) {
    getDiagnostics().Report(mDiagErrorTargetAPIRange) << mTargetAPI
        << SLANG_MINIMUM_TARGET_API << SLANG_MAXIMUM_TARGET_API;
    return false;
  }

  // Skip generation of warnings a second time if we are doing more than just
  // a single pass over the input file.
  bool SuppressAllWarnings = (OutputType != Slang::OT_Dependency);

  for (unsigned i = 0, e = IOFiles.size(); i != e; i++) {
    InputFile = IOFileIter->first;
    OutputFile = IOFileIter->second;

    reset();

    if (!setInputSource(InputFile))
      return false;

    if (!setOutput(OutputFile))
      return false;

    mIsFilterscript = isFilterscript(InputFile);

    if (Slang::compile() > 0)
      return false;

    if (!JavaReflectionPackageName.empty()) {
      mRSContext->setReflectJavaPackageName(JavaReflectionPackageName);
    }
    const std::string &RealPackageName =
        mRSContext->getReflectJavaPackageName();

    if (OutputType != Slang::OT_Dependency) {

      if (BitcodeStorage == BCST_CPP_CODE) {
          RSReflectionCpp R(mRSContext);
          bool ret = R.reflect(JavaReflectionPathBase, getInputFileName(), getOutputFileName());
          if (!ret) {
            return false;
          }
      } else {

        if (!reflectToJava(JavaReflectionPathBase, RSPackageName)) {
          return false;
        }

        for (std::vector<std::string>::const_iterator
                 I = mGeneratedFileNames.begin(), E = mGeneratedFileNames.end();
             I != E;
             I++) {
          std::string ReflectedName = RSSlangReflectUtils::ComputePackagedPath(
              JavaReflectionPathBase.c_str(),
              (RealPackageName + OS_PATH_SEPARATOR_STR + *I).c_str());
          appendGeneratedFileName(ReflectedName + ".java");
        }

        if ((OutputType == Slang::OT_Bitcode) &&
            (BitcodeStorage == BCST_JAVA_CODE) &&
            !generateBitcodeAccessor(JavaReflectionPathBase,
                                     RealPackageName.c_str())) {
          return false;
        }
      }
    }

    if (OutputDep) {
      BCOutputFile = DepFileIter->first;
      DepOutputFile = DepFileIter->second;

      setDepTargetBC(BCOutputFile);

      if (!setDepOutput(DepOutputFile))
        return false;

      if (SuppressAllWarnings) {
        getDiagnostics().setSuppressAllDiagnostics(true);
      }
      if (generateDepFile() > 0)
        return false;
      if (SuppressAllWarnings) {
        getDiagnostics().setSuppressAllDiagnostics(false);
      }

      DepFileIter++;
    }

    if (!checkODR(InputFile))
      return false;

    IOFileIter++;
  }

  return true;
}

void SlangRS::reset() {
  delete mRSContext;
  mRSContext = NULL;
  mGeneratedFileNames.clear();
  Slang::reset();
  return;
}

SlangRS::~SlangRS() {
  delete mRSContext;
  for (ReflectedDefinitionListTy::iterator I = ReflectedDefinitions.begin(),
          E = ReflectedDefinitions.end();
       I != E;
       I++) {
    delete I->getValue().first;
  }
  return;
}

}  // namespace slang
