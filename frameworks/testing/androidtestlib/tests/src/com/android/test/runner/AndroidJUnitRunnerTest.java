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

import android.content.Context;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Unit tests for {@link AndroidJUnitRunner}.
 */
public class AndroidJUnitRunnerTest {

    private AndroidJUnitRunner mAndroidJUnitRunner;
    private PrintStream mStubStream;
    @Mock
    private TestRequestBuilder mMockBuilder;
    @Mock
    private Context mMockContext;

    @Before
    public void setUp() throws Exception {
        mAndroidJUnitRunner = new AndroidJUnitRunner() {


            @Override
            TestRequestBuilder createTestRequestBuilder(PrintStream writer,
                    String... packageCodePaths) {
                return mMockBuilder;
            }

            @Override
            public Context getContext() {
                return mMockContext;
            }
        };
        mAndroidJUnitRunner.setArguments(new Bundle());
        mStubStream = new PrintStream(new ByteArrayOutputStream());
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test {@link AndroidJUnitRunner#buildRequest(Bundle, PrintStream)} when
     * a single class name is provided.
     */
    @Test
    public void testBuildRequest_singleClass() {
        Bundle b = new Bundle();
        b.putString(AndroidJUnitRunner.ARGUMENT_TEST_CLASS, "ClassName");
        mAndroidJUnitRunner.buildRequest(b, mStubStream);
        Mockito.verify(mMockBuilder).addTestClass("ClassName");
    }

    /**
     * Test {@link AndroidJUnitRunner#buildRequest(Bundle, PrintStream)} when
     * multiple class names are provided.
     */
    @Test
    public void testBuildRequest_multiClass() {
        Bundle b = new Bundle();
        b.putString(AndroidJUnitRunner.ARGUMENT_TEST_CLASS, "ClassName1,ClassName2");
        mAndroidJUnitRunner.buildRequest(b, mStubStream);
        Mockito.verify(mMockBuilder).addTestClass("ClassName1");
        Mockito.verify(mMockBuilder).addTestClass("ClassName2");
    }

    /**
     * Test {@link AndroidJUnitRunner#buildRequest(Bundle, PrintStream)} when
     * class name and method name is provided.
     */
    @Test
    public void testBuildRequest_method() {
        Bundle b = new Bundle();
        b.putString(AndroidJUnitRunner.ARGUMENT_TEST_CLASS, "ClassName1#method");
        mAndroidJUnitRunner.buildRequest(b, mStubStream);
        Mockito.verify(mMockBuilder).addTestMethod("ClassName1", "method");
    }
}
