/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mail.ui;

import android.test.AndroidTestCase;

public class HierarchicalFolderTruncationTests extends AndroidTestCase {

    private HierarchicalFolderSelectorAdapter mAdapter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mAdapter = new HierarchicalFolderSelectorAdapter(mContext, null, null, 1, null);
    }

    public void testEmpty() {
        assertEquals("", mAdapter.truncateHierarchy(null));
    }

    public void testNoParents() {
        assertEquals("name", mAdapter.truncateHierarchy("name"));
    }

    public void testSingleParent() {
        assertEquals("parent\u2215folder", mAdapter.truncateHierarchy("parent/folder"));
    }

    public void testDoubleParent() {
        assertEquals("grandparent\u2215parent\u2215folder",
                mAdapter.truncateHierarchy("grandparent/parent/folder"));
    }

    public void testEllipsizedDoubleParent() {
        assertEquals("grandparent\u2215\u2026\u2215parent\u2215folder",
                mAdapter.truncateHierarchy("grandparent/stuff/stuff/stuff/stuff/parent/folder"));
    }
}
