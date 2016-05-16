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
package com.android.test.runner;

import android.app.Instrumentation;
import android.os.Bundle;

import com.android.test.runner.junit3.AndroidJUnit3Builder;
import com.android.test.runner.junit4.AndroidJUnit4Builder;

import org.junit.internal.builders.AllDefaultPossibilitiesBuilder;
import org.junit.runner.Runner;
import org.junit.runners.model.RunnerBuilder;

/**
 * A {@link RunnerBuilder} that can handle all types of tests.
 */
class AndroidRunnerBuilder extends AllDefaultPossibilitiesBuilder {

    private final AndroidJUnit3Builder mAndroidJUnit3Builder;
    private final AndroidJUnit4Builder mAndroidJUnit4Builder;

    public AndroidRunnerBuilder(boolean canUseSuiteMethod, Instrumentation instr, Bundle bundle,
            boolean skipExecution) {
        super(canUseSuiteMethod);
        mAndroidJUnit3Builder = new AndroidJUnit3Builder(instr, bundle, skipExecution);
        mAndroidJUnit4Builder = new AndroidJUnit4Builder(instr, bundle, skipExecution);
    }

    @Override
    public Runner runnerForClass(Class<?> testClass) throws Throwable {
        Runner runner = mAndroidJUnit3Builder.safeRunnerForClass(testClass);
        if (runner != null) {
            return runner;
        }
        runner = mAndroidJUnit4Builder.safeRunnerForClass(testClass);
        if (runner != null) {
            return runner;
        }
        return super.runnerForClass(testClass);
    }

}
