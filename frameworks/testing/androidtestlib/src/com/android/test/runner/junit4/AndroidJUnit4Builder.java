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
package com.android.test.runner.junit4;

import android.app.Instrumentation;
import android.os.Bundle;

import com.android.test.InjectContext;
import com.android.test.InjectInstrumentation;

import org.junit.runner.Runner;
import org.junit.runners.model.RunnerBuilder;

import java.lang.reflect.Field;

/**
 * A {@link RunnerBuilder} that will build customized runners needed to handle {@link InjectContext}
 * and {@link InjectInstrumentation}.
 */
public class AndroidJUnit4Builder extends RunnerBuilder {

    private final Instrumentation mInstrumentation;
    private final Bundle mBundle;
    private boolean mSkipExecution;

    public AndroidJUnit4Builder(Instrumentation instr, Bundle bundle, boolean skipExecution) {
        mInstrumentation = instr;
        mBundle = bundle;
        mSkipExecution = skipExecution;
    }

    @Override
    public Runner runnerForClass(Class<?> testClass) throws Throwable {
        if (mSkipExecution) {
            return new NonExecutingJUnit4ClassRunner(testClass);
        }
        if (hasInjectedFields(testClass)) {
            return new AndroidJUnit4ClassRunner(testClass, mInstrumentation, mBundle);
        }
        return null;
    }

    private boolean hasInjectedFields(Class<?> testClass) {
        // TODO: evaluate performance of this method, would be nice to utilize the annotation
        // caching mechanism of ParentRunner
        Class<?> superClass = testClass;
        while (superClass != null) {
            for (Field field : superClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(InjectInstrumentation.class)) {
                    return true;
                }
                if (field.isAnnotationPresent(InjectContext.class)) {
                    return true;
                }
            }
            superClass = superClass.getSuperclass();
        }
        return false;
    }

}
