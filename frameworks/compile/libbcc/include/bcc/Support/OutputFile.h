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

#ifndef BCC_SUPPORT_OUTPUT_FILE_H
#define BCC_SUPPORT_OUTPUT_FILE_H

#include "bcc/Support/File.h"
#include "bcc/Support/FileBase.h"

namespace llvm {
  class raw_fd_ostream;
}

namespace bcc {

class OutputFile : public File<FileBase::kWriteMode> {
  typedef File<FileBase::kWriteMode> super;
public:
  OutputFile(const std::string &pFilename, unsigned pFlags = 0);

  ssize_t write(const void *pBuf, size_t count);

  void truncate();

  // This is similar to the system call dup(). It creates a copy of the file
  // descriptor it contains and wrap it in llvm::raw_fd_ostream object. It
  // returns a non-NULL object if everything goes well and user should later
  // use delete operator to destroy it by itself.
  llvm::raw_fd_ostream *dup();
};

} // end namespace bcc

#endif  // BCC_SUPPORT_OUTPUT_FILE_H
