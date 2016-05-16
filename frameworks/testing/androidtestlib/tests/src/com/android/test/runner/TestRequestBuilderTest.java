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
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.Suppress;

import com.android.test.InjectBundle;
import com.android.test.InjectInstrumentation;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Unit tests for {@link TestRequestBuilder}.
 */
public class TestRequestBuilderTest {

    public static class SampleTest {

        @SmallTest
        @Test
        public void testSmall() {
        }

        @Test
        public void testOther() {
        }
    }

    @SmallTest
    public static class SampleClassSize {

        @Test
        public void testSmall() {
        }

        @Test
        public void testSmallToo() {
        }
    }

    public static class SampleNoSize extends TestCase {

        public void testOther() {
        }

        public void testOther2() {
        }

    }

    public static class SampleJUnit3Test extends TestCase {

        @SmallTest
        public void testSmall() {
        }

        @SmallTest
        public void testSmall2() {
        }

        public void testOther() {
        }
    }

    @SmallTest
    public static class SampleJUnit3ClassSize extends TestCase {

        public void testSmall() {
        }

        public void testSmall2() {
        }

    }

    public static class SampleJUnit3Suppressed extends TestCase {

        public void testRun() {
        }

        public void testRun2() {
        }

        @Suppress
        public void testSuppressed() {
        }

    }

    @InjectInstrumentation
    public Instrumentation mInstr;

    @InjectBundle
    public Bundle mBundle;

    /**
     * Test initial condition for size filtering - that all tests run when no filter is attached
     */
    @Test
    public void testNoSize() {
        TestRequestBuilder b = new TestRequestBuilder(new PrintStream(new ByteArrayOutputStream()));
        b.addTestClass(SampleTest.class.getName());
        TestRequest request = b.build(mInstr, mBundle);
        JUnitCore testRunner = new JUnitCore();
        Result result = testRunner.run(request.getRequest());
        Assert.assertEquals(2, result.getRunCount());
    }

    /**
     * Test that size annotation filtering works
     */
    @Test
    public void testSize() {
        TestRequestBuilder b = new TestRequestBuilder(new PrintStream(new ByteArrayOutputStream()));
        b.addTestClass(SampleTest.class.getName());
        b.addTestSizeFilter("small");
        TestRequest request = b.build(mInstr, mBundle);
        JUnitCore testRunner = new JUnitCore();
        Result result = testRunner.run(request.getRequest());
        Assert.assertEquals(1, result.getRunCount());
    }

    /**
     * Test that size annotation filtering by class works
     */
    @Test
    public void testSize_class() {
        TestRequestBuilder b = new TestRequestBuilder(new PrintStream(new ByteArrayOutputStream()));
        b.addTestClass(SampleTest.class.getName());
        b.addTestClass(SampleClassSize.class.getName());
        b.addTestSizeFilter("small");
        TestRequest request = b.build(mInstr, mBundle);
        JUnitCore testRunner = new JUnitCore();
        Result result = testRunner.run(request.getRequest());
        Assert.assertEquals(3, result.getRunCount());
    }

    /**
     * Test case where entire JUnit3 test class has been filtered out
     */
    @Test
    public void testSize_classFiltered() {
        TestRequestBuilder b = new TestRequestBuilder(new PrintStream(new ByteArrayOutputStream()));
        b.addTestClass(SampleTest.class.getName());
        b.addTestClass(SampleNoSize.class.getName());
        b.addTestSizeFilter("small");
        TestRequest request = b.build(mInstr, mBundle);
        MyRunListener l = new MyRunListener();
        JUnitCore testRunner = new JUnitCore();
        testRunner.addListener(l);
        testRunner.run(request.getRequest());
        Assert.assertEquals(1, l.mTestCount);
    }

    private static class MyRunListener extends RunListener {
        private int mTestCount = -1;

        public void testRunStarted(Description description) throws Exception {
            mTestCount = description.testCount();
        }
    }

    /**
     * Test size annotations with JUnit3 test methods
     */
    @Test
    public void testSize_junit3Method() {
        TestRequestBuilder b = new TestRequestBuilder(new PrintStream(new ByteArrayOutputStream()));
        b.addTestClass(SampleJUnit3Test.class.getName());
        b.addTestClass(SampleNoSize.class.getName());
        b.addTestSizeFilter("small");
        TestRequest request = b.build(mInstr, mBundle);
        JUnitCore testRunner = new JUnitCore();
        Result r = testRunner.run(request.getRequest());
        Assert.assertEquals(2, r.getRunCount());
    }

    /**
     * Test @Suppress with JUnit3 tests
     */
    @Test
    public void testSuppress_junit3Method() {
        TestRequestBuilder b = new TestRequestBuilder(new PrintStream(new ByteArrayOutputStream()));
        b.addTestClass(SampleJUnit3Suppressed.class.getName());
        TestRequest request = b.build(mInstr, mBundle);
        JUnitCore testRunner = new JUnitCore();
        Result r = testRunner.run(request.getRequest());
        Assert.assertEquals(2, r.getRunCount());
    }

    /**
     * Test that annotation filtering by class works
     */
    @Test
    public void testAddAnnotationInclusionFilter() {
        TestRequestBuilder b = new TestRequestBuilder(new PrintStream(new ByteArrayOutputStream()));
        b.addAnnotationInclusionFilter(SmallTest.class.getName());
        b.addTestClass(SampleTest.class.getName());
        b.addTestClass(SampleClassSize.class.getName());
        TestRequest request = b.build(mInstr, mBundle);
        JUnitCore testRunner = new JUnitCore();
        Result result = testRunner.run(request.getRequest());
        Assert.assertEquals(3, result.getRunCount());
    }

    /**
     * Test that annotation filtering by class works
     */
    @Test
    public void testAddAnnotationExclusionFilter() {
        TestRequestBuilder b = new TestRequestBuilder(new PrintStream(new ByteArrayOutputStream()));
        b.addAnnotationExclusionFilter(SmallTest.class.getName());
        b.addTestClass(SampleTest.class.getName());
        b.addTestClass(SampleClassSize.class.getName());
        TestRequest request = b.build(mInstr, mBundle);
        JUnitCore testRunner = new JUnitCore();
        Result result = testRunner.run(request.getRequest());
        Assert.assertEquals(1, result.getRunCount());
    }
}
