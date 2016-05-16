/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.android.testing.mocking.test;

import android.content.Context;

import com.google.android.testing.mocking.AndroidMock;
import com.google.android.testing.mocking.SdkVersion;
import com.google.android.testing.mocking.UsesMocks;
import com.google.android.testing.mocking.testapp.ClassToMock;

import junit.framework.TestCase;

/**
 * Tests that Android Mock provides correct mocks when running on a Dalvik VM.
 * 
 * @author swoodward (Stephen Woodward)
 */
public class MockingTest extends TestCase {
  /**
   * Test that an SDK class is mocked correctly, that is to say the mock
   * comes from the pre-generated set, and it corresponds to the correct
   * runtime environment
   */
  @UsesMocks(Context.class)
  public void testFrameworkMock() {
    Context mockContext = AndroidMock.createMock(Context.class);
    String packageName = mockContext.getClass().getPackage().getName();
    assertEquals(SdkVersion.getCurrentVersion().getPackagePrefix(),
        packageName.substring(0, packageName.indexOf('.') + 1));
  }

  /**
   * Test that a non-SDK class is mocked correctly
   */
  @UsesMocks(ClassToMock.class)
  public void testNormalMock() {
    ClassToMock myMockClass = AndroidMock.createMock(ClassToMock.class);
    AndroidMock.expect(myMockClass.getString()).andReturn(
        "I'm the king of the world, king of the -- d'oh!");
    AndroidMock.expect(myMockClass.getInt()).andReturn(42);
    AndroidMock.replay(myMockClass);
    assertEquals("I'm the king of the world, king of the -- d'oh!",
        myMockClass.getString());
    assertEquals(42, myMockClass.getInt());
    AndroidMock.verify(myMockClass);
  }
}

