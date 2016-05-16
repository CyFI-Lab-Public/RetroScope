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

#ifndef BCC_SUPPORT_FILE_H
#define BCC_SUPPORT_FILE_H

#include "bcc/Support/FileBase.h"

namespace bcc {

template<enum FileBase::OpenModeEnum OpenMode>
struct FileAttribute {
  // The flags to the 2nd argument in ::open().
  enum { kOpenFlags };

  // Default value of LockMode.
  enum { kDefaultLockMode };
};

// FileAttribute for accessing read-only file
template<>
struct FileAttribute<FileBase::kReadMode> {
  enum { kOpenFlags       = O_RDONLY };
  enum { kDefaultLockMode = FileBase::kReadLock };
};

// FileAttribute for accessing writable file
template<>
struct FileAttribute<FileBase::kWriteMode> {
  enum { kOpenFlags       = O_RDWR | O_CREAT };
  enum { kDefaultLockMode = FileBase::kWriteLock };
};

template<enum FileBase::OpenModeEnum OpenMode>
class File : public FileBase {
public:
  File(const std::string &pFilename, unsigned pFlags)
    : FileBase(pFilename, FileAttribute<OpenMode>::kOpenFlags, pFlags) { }

  inline bool lock(enum LockModeEnum pMode = static_cast<enum LockModeEnum>(
                      FileAttribute<OpenMode>::kDefaultLockMode),
                   bool pNonblocking = true,
                   unsigned pMaxRetry = FileBase::kDefaultMaxRetryLock,
                   useconds_t pRetryInterval =
                      FileBase::kDefaultRetryLockInterval) {
    return FileBase::lock(pMode, pNonblocking, pMaxRetry, pRetryInterval);
  }

  inline android::FileMap *createMap(off_t pOffset, size_t pLength,
                                     bool pIsReadOnly =
                                        (OpenMode == FileBase::kReadMode)) {
    return FileBase::createMap(pOffset, pLength, pIsReadOnly);
  }
};


} // end namespace bcc

#endif  // BCC_SUPPORT_FILE_H
