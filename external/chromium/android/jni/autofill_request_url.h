// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef ANDROID_JNI_AUTOFILLREQUESTURL_H
#define ANDROID_JNI_AUTOFILLREQUESTURL_H

#include "base/basictypes.h"
#include <string>

namespace android {

class AutofillRequestUrl {
  public:
    static std::string GetQueryUrl();
  private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(AutofillRequestUrl);
};

} // namespace android

#endif
