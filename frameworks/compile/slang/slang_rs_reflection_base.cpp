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

#include <sys/stat.h>

#include <cstdarg>
#include <cctype>

#include <algorithm>
#include <limits>
#include <sstream>
#include <string>
#include <utility>

#include "os_sep.h"
#include "slang_rs_context.h"
#include "slang_rs_export_var.h"
#include "slang_rs_export_foreach.h"
#include "slang_rs_export_func.h"
#include "slang_rs_reflect_utils.h"
#include "slang_version.h"
#include "slang_utils.h"

#include "slang_rs_reflection_base.h"



using namespace std;

namespace slang {

static const char *const gApacheLicenseNote =
"/*\n"
" * Copyright (C) 2012 The Android Open Source Project\n"
" *\n"
" * Licensed under the Apache License, Version 2.0 (the \"License\");\n"
" * you may not use this file except in compliance with the License.\n"
" * You may obtain a copy of the License at\n"
" *\n"
" *      http://www.apache.org/licenses/LICENSE-2.0\n"
" *\n"
" * Unless required by applicable law or agreed to in writing, software\n"
" * distributed under the License is distributed on an \"AS IS\" BASIS,\n"
" * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
" * See the License for the specific language governing permissions and\n"
" * limitations under the License.\n"
" */\n"
"\n";


RSReflectionBase::RSReflectionBase(const RSContext *con)
  : mVerbose(true) {
  mRSContext = con;
  mLicenseNote = gApacheLicenseNote;

}

RSReflectionBase::~RSReflectionBase() {

}

/*
bool RSReflectionBase::openFile(const string &name, string &errorMsg) {
    if(!mUseStdout) {
        mOF.clear();
        if(!SlangUtils::CreateDirectoryWithParents(mOutputPath, &errorMsg)) {
            return false;
        }

        string cf(mOutputPath + OS_PATH_SEPARATOR_STR + name);
        mOF.open(cf.c_str());
        if(!mOF.good()) {
            errorMsg = "failed to open file '" + cf + "' for write";
            return false;
        }
    }
    return true;
}
*/

void RSReflectionBase::startFile(const string &filename) {
  if(mVerbose) {
    printf("Generating %s\n", filename.c_str());
  }

  // License
  write(mLicenseNote);

  // Notice of generated file
  write("/*");
  write(" * This file is auto-generated. DO NOT MODIFY!");
  write(" * The source Renderscript file: " + mInputFileName);
  write(" */");
  write("");
}

// remove path plus .rs from filename to generate class name
string RSReflectionBase::stripRS(const string &s) const {
  string tmp(s);
  size_t pos = tmp.rfind(".rs");
  if(pos != string::npos) {
    tmp.erase(pos);
  }
  pos = tmp.rfind("/");
  if (pos != string::npos) {
    tmp.erase(0, pos+1);
  }
  return tmp;
}

void RSReflectionBase::write(const std::string &t) {
  //printf("%s%s\n", mIndent.c_str(), t.c_str());
  mText.push_back(mIndent + t);
}

void RSReflectionBase::write(const std::stringstream &t) {
  mText.push_back(mIndent + t.str());
}


void RSReflectionBase::incIndent() {
  mIndent.append("    ");
}

void RSReflectionBase::decIndent() {
  mIndent.erase(0, 4);
}

bool RSReflectionBase::writeFile(const string &filename, const vector< string > &txt) {
  FILE *pfin = fopen((mOutputPath + filename).c_str(), "wt");
  if (pfin == NULL) {
    fprintf(stderr, "Error: could not write file %s\n", filename.c_str());
    return false;
  }

  for(size_t ct=0; ct < txt.size(); ct++) {
    fprintf(pfin, "%s\n", txt[ct].c_str());
  }
  fclose(pfin);
  return true;
}


string RSReflectionBase::genInitValue(const clang::APValue &Val, bool asBool) {
  stringstream tmp;
  switch (Val.getKind()) {
    case clang::APValue::Int: {
      llvm::APInt api = Val.getInt();
      if(asBool) {
        tmp << ((api.getSExtValue() == 0) ? "false" : "true");
      } else {
        // TODO: Handle unsigned possibly for C++ API.
        tmp << api.getSExtValue();
        if (api.getBitWidth() > 32) {
          tmp << "L";
        }
      }
      break;
    }

    case clang::APValue::Float: {
      llvm::APFloat apf = Val.getFloat();
      llvm::SmallString<30> s;
      apf.toString(s);
      tmp << s.c_str();
      if (&apf.getSemantics() == &llvm::APFloat::IEEEsingle) {
        if (s.count('.') == 0) {
          tmp << ".f";
        } else {
          tmp << "f";
        }
      }
      break;
    }

    case clang::APValue::ComplexInt:
    case clang::APValue::ComplexFloat:
    case clang::APValue::LValue:
    case clang::APValue::Vector: {
      slangAssert(false && "Primitive type cannot have such kind of initializer");
      break;
    }

    default: {
      slangAssert(false && "Unknown kind of initializer");
    }
  }
  return tmp.str();
}

bool RSReflectionBase::addTypeNameForElement(
    const std::string &TypeName) {
  if (mTypesToCheck.find(TypeName) == mTypesToCheck.end()) {
    mTypesToCheck.insert(TypeName);
    return true;
  } else {
    return false;
  }
}

const char *RSReflectionBase::getVectorAccessor(unsigned Index) {
  static const char *VectorAccessorMap[] = {
    /* 0 */ "x",
    /* 1 */ "y",
    /* 2 */ "z",
    /* 3 */ "w",
  };

  slangAssert((Index < (sizeof(VectorAccessorMap) / sizeof(const char*))) &&
              "Out-of-bound index to access vector member");

  return VectorAccessorMap[Index];
}

}
