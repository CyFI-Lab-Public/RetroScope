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

#ifndef _FRAMEWORKS_COMPILE_SLANG_SLANG_REFLECT_UTILS_H_  // NOLINT
#define _FRAMEWORKS_COMPILE_SLANG_SLANG_REFLECT_UTILS_H_

#include <string>

namespace slang {

// BitCode storage type
enum BitCodeStorageType {
  BCST_APK_RESOURCE,
  BCST_JAVA_CODE,
  BCST_CPP_CODE
};

class RSSlangReflectUtils {
 public:
  // Encode a binary bitcode file into a Java source file.
  // rsFileName: the original .rs file name (with or without path).
  // bcFileName: where is the bit code file
  // reflectPath: where to output the generated Java file, no package name in
  // it.
  // packageName: the package of the output Java file.
  struct BitCodeAccessorContext {
    const char *rsFileName;
    const char *bcFileName;
    const char *reflectPath;
    const char *packageName;

    BitCodeStorageType bcStorage;
  };

  // Return the stem of the file name, i.e., remove the dir and the extension.
  // Eg, foo.ext -> foo
  //     foo.bar.ext -> foo.bar
  //     ./path/foo.ext -> foo
  static std::string GetFileNameStem(const char* fileName);

  // Compuate a Java source file path from a given prefixPath and its package.
  // Eg, given prefixPath=./foo/bar and packageName=com.x.y, then it returns
  // ./foo/bar/com/x/y
  static std::string ComputePackagedPath(const char *prefixPath,
                                         const char *packageName);

  // Compute Java class name from a .rs file name.
  // Any non-alnum, non-underscore characters will be discarded.
  // E.g. with rsFileName=./foo/bar/my-Renderscript_file.rs it returns
  // "myRenderscript_file".
  // rsFileName: the input .rs file name (with or without path).
  static std::string JavaClassNameFromRSFileName(const char *rsFileName);

  // Compute a bitcode file name (no extension) from a .rs file name.
  // Because the bitcode file name may be used as Resource ID in the generated
  // class (something like R.raw.<bitcode_filename>), Any non-alnum,
  // non-underscore character will be discarded.
  // The difference from JavaClassNameFromRSFileName() is that the result is
  // converted to lowercase.
  // E.g. with rsFileName=./foo/bar/my-Renderscript_file.rs it returns
  // "myrenderscript_file"
  // rsFileName: the input .rs file name (with or without path).
  static std::string BCFileNameFromRSFileName(const char *rsFileName);

  // Generate the bit code accessor Java source file.
  static bool GenerateBitCodeAccessor(const BitCodeAccessorContext &context);
};
}  // namespace slang

#endif  // _FRAMEWORKS_COMPILE_SLANG_SLANG_REFLECT_UTILS_H_  NOLINT
