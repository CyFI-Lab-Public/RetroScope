// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef ANDROID_CONTENT_BROWSER_TAB_CONTENTS_TAB_CONTENTS_H_
#define ANDROID_CONTENT_BROWSER_TAB_CONTENTS_TAB_CONTENTS_H_
#pragma once

#include "android/autofill/profile_android.h"
#include "base/scoped_ptr.h"
#include "chrome/browser/profiles/profile.h"
#include "chrome/browser/autofill/autofill_host.h"

// Autofill does not need the entire TabContents class, just
// access to the RenderViewHost and Profile. Later it would
// be nice to create a small class that contains just this
// data for AutoFill. Then Android won't care about this
// file which as it stands does not compile for us.
class RenderViewHost;
class URLRequestContextGetter;

class TabContents {
public:
  TabContents()
    : profile_(ProfileImplAndroid::CreateProfile(FilePath()))
    , autofill_host_(NULL)
  {
  }

  Profile* profile() { return profile_.get(); }
  void SetProfileRequestContext(net::URLRequestContextGetter* context) { static_cast<ProfileImplAndroid*>(profile_.get())->SetRequestContext(context); }
  AutoFillHost* autofill_host() { return autofill_host_; }
  void SetAutoFillHost(AutoFillHost* autofill_host) { autofill_host_ = autofill_host; }

private:
  scoped_ptr<Profile> profile_;
  AutoFillHost* autofill_host_;
};

#endif // ANDROID_CONTENT_BROWSER_TAB_CONTENTS_TAB_CONTENTS_H_
