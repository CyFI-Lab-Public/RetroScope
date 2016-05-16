/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.test.runner.junit3;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.junit.Ignore;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.test.InstrumentationTestCase;

import com.android.test.BundleTest;

/**
 * A {@link TestSuite} used to pass {@link Context} and {@link Instrumentation} references to child
 * tests.
 */
@Ignore
class AndroidTestSuite extends TestSuite {

    private final Instrumentation mInstr;
    private final Bundle mBundle;

    AndroidTestSuite(Class<?> clazz, Bundle bundle, Instrumentation instrumentation) {
        super(clazz);
        mBundle = bundle;
        mInstr = instrumentation;
    }

    AndroidTestSuite(String name, Bundle bundle, Instrumentation instrumentation) {
        super(name);
        mBundle = bundle;
        mInstr = instrumentation;
    }

    @Override
    public void runTest(Test test, TestResult result) {
        if (test instanceof AndroidTestCase) {
            ((AndroidTestCase)test).setContext(mInstr.getTargetContext());
        }
        if (test instanceof InstrumentationTestCase) {
            ((InstrumentationTestCase)test).injectInstrumentation(mInstr);
        }
        if (test instanceof BundleTest) {
            ((BundleTest)test).injectBundle(mBundle);
        }
        super.runTest(test, result);
    }

    Instrumentation getInstrumentation() {
        return mInstr;
    }

    Bundle getBundle() {
        return mBundle;
    }
}
