/*
 * Copyright (C) 2010 The Android Open Source Project
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

import junit.framework.TestResult;

import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.notification.RunNotifier;

/**
 *  A specialized {@link JUnit38ClassRunner} that will skip test execution.
 */
class NonExecutingJUnit3ClassRunner extends JUnit38ClassRunner {

    public NonExecutingJUnit3ClassRunner(Class<?> klass) {
        super(klass);
        // TODO: it would be nice if actually creating Test objects could be skipped too like
        // junit4, but there doesn't seem to be an easy way of doing that.
    }

    @Override
    public void run(RunNotifier notifier) {
        TestResult result = new NoExecTestResult();
        result.addListener(createAdaptingListener(notifier));
        getTest().run(result);
    }

}
