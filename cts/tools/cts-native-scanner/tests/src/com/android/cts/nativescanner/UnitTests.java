/*
 * Copyright 2012 The Android Open Source Project
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
package com.android.cts.nativescanner;

import com.android.cts.nativescanner.TestScannerTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * A test suite for all cts-native-scanner unit tests.
 * <p/>
 * All tests listed here should be self-contained, and do not require any external dependencies
 */
public class UnitTests extends TestSuite {

    public UnitTests() {
        super();

        // result package
        addTestSuite(TestScannerTest.class);
    }

    public static Test suite() {
        return new UnitTests();
    }
}
