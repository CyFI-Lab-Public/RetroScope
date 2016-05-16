// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "net/base/platform_mime_util.h"

#include "android/jni/mime_utils.h"

using namespace android;

namespace net {

bool PlatformMimeUtil::GetPlatformMimeTypeFromExtension(
    const FilePath::StringType& ext, std::string* result) const {
  return MimeUtils::GuessMimeTypeFromExtension(ext, result);
}

bool PlatformMimeUtil::GetPreferredExtensionForMimeType(
    const std::string& mime_type, FilePath::StringType* ext) const {
  return MimeUtils::GuessExtensionFromMimeType(mime_type, ext);
}

} // namespace net
