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

#ifndef _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_REFLECTION_BASE_H_  // NOLINT
#define _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_REFLECTION_BASE_H_

#include <fstream>
#include <iostream>
#include <map>
#include <set>
#include <string>
#include <vector>

#include "llvm/ADT/StringExtras.h"

#include "slang_assert.h"
#include "slang_rs_export_type.h"

namespace slang {
    class RSContext;
    class RSExportVar;
    class RSExportFunc;
    class RSExportForEach;

class RSReflectionBase {
protected:
    const RSContext *mRSContext;

    // Generated RS Elements for type-checking code.
    std::set<std::string> mTypesToCheck;

    RSReflectionBase(const RSContext *);


    bool mVerbose;

    std::string mLicenseNote;
    std::string mInputFileName;

    std::string mClassName;
    std::string mOutputPath;
    std::string mOutputBCFileName;

    std::vector< std::string > mText;
    std::string mIndent;

    bool openFile(const std::string &name, std::string &errorMsg);
    void startFile(const std::string &filename);
    void incIndent();
    void decIndent();
    void write(const std::string &t);
    void write(const std::stringstream &t);

    std::string stripRS(const std::string &s) const;

    bool writeFile(const std::string &filename, const std::vector< std::string > &txt);

    bool addTypeNameForElement(const std::string &TypeName);

    static const char *getVectorAccessor(unsigned index);

private:

public:
    typedef std::vector<std::pair<std::string, std::string> > ArgTy;

    virtual ~RSReflectionBase();

    static std::string genInitValue(const clang::APValue &Val, bool asBool=false);

};  // class RSReflection

}   // namespace slang

#endif  // _FRAMEWORKS_COMPILE_SLANG_SLANG_RS_REFLECTION_BASE_H_  NOLINT
