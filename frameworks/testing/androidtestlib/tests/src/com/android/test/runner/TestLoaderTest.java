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

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Unit tests for {@link TestLoader}.
 */
public class TestLoaderTest {

    public static class JUnit3Test extends TestCase {
    }

    public static abstract class AbstractTest extends TestCase {
    }

    public static class JUnit4Test {
        @Test
        public void thisIsATest() {
        }
    }

    @RunWith(value = Parameterized.class)
    public static class JUnit4RunTest {
        public void thisIsMayBeATest() {
        }
    }

    public static class NotATest {
        public void thisIsNotATest() {
        }
    }

    private TestLoader mLoader;

    @Before
    public void setUp() throws Exception {
        mLoader = new TestLoader(new PrintStream(new ByteArrayOutputStream()));
    }

    @Test
    public void testLoadTests_junit3() {
        assertLoadTestSuccess(JUnit3Test.class);
    }

    @Test
    public void testLoadTests_junit4() {
        assertLoadTestSuccess(JUnit4Test.class);
    }

    @Test
    public void testLoadTests_runWith() {
        assertLoadTestSuccess(JUnit4RunTest.class);
    }

    @Test
    public void testLoadTests_notATest() {
        Assert.assertNull(mLoader.loadIfTest(NotATest.class.getName()));
        Assert.assertEquals(0, mLoader.getLoadedClasses().size());
        Assert.assertEquals(0, mLoader.getLoadFailures().size());
    }

    @Test
    public void testLoadTests_notExist() {
        Assert.assertNull(mLoader.loadIfTest("notexist"));
        Assert.assertEquals(0, mLoader.getLoadedClasses().size());
        Assert.assertEquals(1, mLoader.getLoadFailures().size());
    }

    private void assertLoadTestSuccess(Class<?> clazz) {
        Assert.assertNotNull(mLoader.loadIfTest(clazz.getName()));
        Assert.assertEquals(1, mLoader.getLoadedClasses().size());
        Assert.assertEquals(0, mLoader.getLoadFailures().size());
        Assert.assertTrue(mLoader.getLoadedClasses().contains(clazz));
    }

    @Test
    public void testLoadTests_abstract() {
        Assert.assertNull(mLoader.loadIfTest(AbstractTest.class.getName()));
        Assert.assertEquals(0, mLoader.getLoadedClasses().size());
        Assert.assertEquals(0, mLoader.getLoadFailures().size());
    }
}
