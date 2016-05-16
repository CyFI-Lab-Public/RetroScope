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

#include "bcc/Support/InputFile.h"

#include "bcc/Support/Log.h"

using namespace bcc;

InputFile::InputFile(const std::string &pFilename, unsigned pFlags)
  : super(pFilename, pFlags) { }

ssize_t InputFile::read(void *pBuf, size_t count) {
  if ((mFD < 0) || hasError()) {
    return -1;
  }

  if ((count <= 0) || (pBuf == NULL)) {
    // Keep safe and issue a warning.
    ALOGW("InputFile::read: count = %zu, buffer = %p", count, pBuf);
    return 0;
  }

  while (count > 0) {
    ssize_t read_size = ::read(mFD, pBuf, count);

    if (read_size >= 0) {
      return read_size;
    } else if ((errno == EAGAIN) || (errno == EINTR)) {
      // If the errno is EAGAIN or EINTR, then we try to read again.
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
