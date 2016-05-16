// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#ifndef ANDROID_CONTENT_BROWSER_TAB_CONTENTS_TAB_CONTENTS_OBSERVER_H_
#define ANDROID_CONTENT_BROWSER_TAB_CONTENTS_TAB_CONTENTS_OBSERVER_H_

// Android specific implementation

#include "content/browser/tab_contents/tab_contents.h"

// An observer API implemented by classes which are interested in various page
// load events from TabContents.  They also get a chance to filter IPC messages.
class TabContentsObserver {
 public:

 protected:
  TabContentsObserver(TabContents* tab_contents) {
    tab_contents_ = tab_contents;
  }

  virtual ~TabContentsObserver() {}

  TabContents* tab_contents() { return tab_contents_; }

 private:
  friend class TabContents;

  TabContents* tab_contents_;

  DISALLOW_COPY_AND_ASSIGN(TabContentsObserver);
};

#endif  // CONTENT_BROWSER_TAB_CONTENTS_TAB_CONTENTS_OBSERVER_H_
