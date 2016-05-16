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

import android.app.Instrumentation;
import android.os.Bundle;

import junit.framework.TestCase;

import org.junit.runner.Runner;
import org.junit.runners.model.RunnerBuilder;

/**
 * A {@link RunnerBuilder} that will build customized runners needed for specialized Android
 * {@link TestCase}s.
 */
public class AndroidJUnit3Builder extends RunnerBuilder {

    private Instrumentation mInstr;
    private boolean mSkipExecution;
    private final Bundle mBundle;

    public AndroidJUnit3Builder(Instrumentation instr, Bundle bundle, boolean skipExecution) {
        mInstr = instr;
        mBundle = bundle;
        mSkipExecution = skipExecution;
    }

    @Override
    public Runner runnerForClass(Class<?> testClass) throws Throwable {
        if (mSkipExecution && isJUnit3TestCase(testClass)) {
            return new NonExecutingJUnit3ClassRunner(testClass);
        } else if (isAndroidTestCase(testClass)) {
            return new AndroidJUnit3ClassRunner(testClass, mBundle, mInstr);
        } else if (isInstrumentationTestCase(testClass)) {
            return new AndroidJUnit3ClassRunner(testClass, mBundle, mInstr);
        } else if (isBundleTest(testClass)) {
            return new AndroidJUnit3ClassRunner(testClass, mBundle, mInstr);
        }
        return null;
    }

    boolean isJUnit3TestCase(Class<?> testClass) {
        return junit.framework.TestCase.class.isAssignableFrom(testClass);
    }

    boolean isAndroidTestCase(Class<?> testClass) {
        return android.test.AndroidTestCase.class.isAssignableFrom(testClass);
    }

    boolean isInstrumentationTestCase(Class<?> testClass) {
        return android.test.InstrumentationTestCase.class.isAssignableFrom(testClass);
    }

    boolean isBundleTest(Class<?> testClass) {
       return com.android.test.BundleTest.class.isAssignableFrom(testClass);
    }

}
