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

#include "bcc/Support/OutputFile.h"

#include <cstdlib>

#include <llvm/Support/raw_ostream.h>

#include "bcc/Support/Log.h"

using namespace bcc;

OutputFile::OutputFile(const std::string &pFilename, unsigned pFlags)
  : super(pFilename, pFlags) { }

ssize_t OutputFile::write(const void *pBuf, size_t count) {
  if ((mFD < 0) || hasError()) {
    return -1;
  }

  if ((count <= 0) || (pBuf == NULL)) {
    // Keep safe and issue a warning.
    ALOGW("OutputFile::write: count = %zu, buffer = %p", count, pBuf);
    return 0;
  }

  while (count > 0) {
    ssize_t write_size = ::write(mFD, pBuf, count);

    if (write_size > 0) {
      return write_size;
    } else if ((errno == EAGAIN) || (errno == EINTR)) {
      // If the errno is EAGAIN or EINTR, then we try to write again.
      //
      // Fall-through
    } else {
      detectError();
      return -1;
    }
  }
  // unreachable
  return 0;
}

void OutputFile::truncate() {
  if (mFD < 0) {
    return;
  }

  do {
    if (::ftruncate(mFD, 0) == 0) {
      return;
    }
  } while (errno == EINTR);
  detectError();

  return;
}

llvm::raw_fd_ostream *OutputFile::dup() {
  int newfd;

  do {
    newfd = ::dup(mFD);
    if (newfd < 0) {
      if (errno != EINTR) {
        detectError();
        return NULL;
      }
      // EINTR
      continue;
    }
    // dup() returns ok.
    break;
  } while (true);

  llvm::raw_fd_ostream *result =
      new (std::nothrow) llvm::raw_fd_ostream(newfd, /* shouldClose */true);

  if (result == NULL) {
    mError.assign(llvm::errc::not_enough_memory, llvm::system_category());
  }

  return result;
}
