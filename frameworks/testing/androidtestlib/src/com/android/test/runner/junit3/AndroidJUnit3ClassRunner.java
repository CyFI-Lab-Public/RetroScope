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
import junit.framework.TestSuite;

import org.junit.internal.runners.JUnit38ClassRunner;

/**
 * A specialized {@link JUnit38ClassRunner} that can handle specialized Android {@link TestCase}s.
 */
class AndroidJUnit3ClassRunner extends JUnit38ClassRunner {

    public AndroidJUnit3ClassRunner(Class<?> klass, Bundle bundle, Instrumentation instr) {
        super(new AndroidTestSuite(klass.asSubclass(TestCase.class), bundle, instr));
    }

    @Override
    protected TestSuite createCopyOfSuite(TestSuite s) {
        if (s instanceof AndroidTestSuite) {
            AndroidTestSuite a = (AndroidTestSuite)s;
            return new AndroidTestSuite(a.getName(), a.getBundle(), a.getInstrumentation());
        } else {
            return super.createCopyOfSuite(s);
        }
    }
}
