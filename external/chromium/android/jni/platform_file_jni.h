// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "android/jni/jni_utils.h"
#include "base/platform_file.h"

#include <string>

namespace android {

uint64 contentUrlSize(const FilePath& name);

class JavaISWrapper {
public:
  JavaISWrapper(const FilePath& path);
  ~JavaISWrapper();

  int Read(char* out, int length);

private:
  jobject    m_inputStream;
  jmethodID  m_read;
  jmethodID  m_close;
};

}
