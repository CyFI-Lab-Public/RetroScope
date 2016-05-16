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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TestSuite implements Comparable<TestSuite> {

    private final String mName;

    private final Map<String, TestSuite> mSuites = new HashMap<String, TestSuite>();

    private final List<TestCase> mCases = new ArrayList<TestCase>();

    public TestSuite(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public boolean hasSuite(String name) {
        return mSuites.containsKey(name);
    }

    public TestSuite getSuite(String name) {
        return mSuites.get(name);
    }

    public void addSuite(TestSuite suite) {
        mSuites.put(suite.mName, suite);
    }

    public Collection<TestSuite> getSuites() {
        return Collections.unmodifiableCollection(mSuites.values());
    }

    public void addCase(TestCase testCase) {
        mCases.add(testCase);
    }

    public Collection<TestCase> getCases() {
        return Collections.unmodifiableCollection(mCases);
    }

    @Override
    public int compareTo(TestSuite another) {
        return getName().compareTo(another.getName());
    }
}
