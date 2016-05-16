// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include <string>

#include "base/basictypes.h"

namespace android {

// JNI wrapper for libcore.net.MimeUtils
class MimeUtils {
 public:
  static bool GuessMimeTypeFromExtension(const std::string& extension,
      std::string* result);

  static bool GuessExtensionFromMimeType(const std::string& mimeType,
      std::string* result);

 private:
  DISALLOW_IMPLICIT_CONSTRUCTORS(MimeUtils);
};

} // namespace android

