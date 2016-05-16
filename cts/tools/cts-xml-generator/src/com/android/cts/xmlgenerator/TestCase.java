/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.cts.xmlgenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class TestCase implements Comparable<TestCase> {

    private final String mName;

    private final List<Test> mTests = new ArrayList<Test>();

    public TestCase(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void addTest(String testName, int timeout) {
        mTests.add(new Test(testName, timeout));
    }

    public Collection<Test> getTests() {
        return Collections.unmodifiableCollection(mTests);
    }

    @Override
    public int compareTo(TestCase another) {
        return getName().compareTo(another.getName());
    }
}
