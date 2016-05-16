/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.testing.mocking;

import junit.framework.TestCase;

import java.io.File;
import java.util.Vector;

/**
 * @author swoodward@google.com (Stephen Woodward)
 */
public class FileUtilsTest extends TestCase {
  public void testGetFilenameForClass() {
    assertEquals(convertPathToNative("java/lang/Object.class"),
        FileUtils.getFilenameFor(Object.class.getName()));
    assertEquals(convertPathToNative(
            "com/google/android/testing/mocking/FileUtilsTest$InnerClass.class"),
        FileUtils.getFilenameFor(InnerClass.class.getName()));
  }

  public void testGetClassNameFor() {
    assertEquals(convertPathToNative("java/lang/Object.class"),
        FileUtils.getFilenameFor(Object.class.getName()));
    assertEquals(convertPathToNative(
            "com/google/android/testing/mocking/FileUtilsTest$InnerClass.class"),
        FileUtils.getFilenameFor(InnerClass.class.getName()));
  }

  public void testGetInterfaceNameFor() {
    assertEquals("v15.genmocks.java.util.VectorDelegateInterface",
        FileUtils.getInterfaceNameFor(Vector.class, SdkVersion.CUPCAKE));
    assertEquals("v16.genmocks.java.util.VectorDelegateInterface",
        FileUtils.getInterfaceNameFor(Vector.class, SdkVersion.DONUT));
    assertEquals("v201.genmocks.java.util.VectorDelegateInterface",
        FileUtils.getInterfaceNameFor(Vector.class, SdkVersion.ECLAIR_0_1));
    assertEquals("v21.genmocks.java.util.VectorDelegateInterface",
        FileUtils.getInterfaceNameFor(Vector.class, SdkVersion.ECLAIR_MR1));
    assertEquals("v22.genmocks.java.util.VectorDelegateInterface",
        FileUtils.getInterfaceNameFor(Vector.class, SdkVersion.FROYO));
    assertEquals("genmocks.java.util.VectorDelegateInterface",
        FileUtils.getInterfaceNameFor(Vector.class, SdkVersion.UNKNOWN));
  }

  public void testGetSubclassNameFor() {
    assertEquals("v15.genmocks.java.util.VectorDelegateSubclass",
        FileUtils.getSubclassNameFor(Vector.class, SdkVersion.CUPCAKE));
    assertEquals("v16.genmocks.java.util.VectorDelegateSubclass",
        FileUtils.getSubclassNameFor(Vector.class, SdkVersion.DONUT));
    assertEquals("v201.genmocks.java.util.VectorDelegateSubclass",
        FileUtils.getSubclassNameFor(Vector.class, SdkVersion.ECLAIR_0_1));
    assertEquals("v21.genmocks.java.util.VectorDelegateSubclass",
        FileUtils.getSubclassNameFor(Vector.class, SdkVersion.ECLAIR_MR1));
    assertEquals("v22.genmocks.java.util.VectorDelegateSubclass",
        FileUtils.getSubclassNameFor(Vector.class, SdkVersion.FROYO));
    assertEquals("genmocks.java.util.VectorDelegateSubclass",
        FileUtils.getSubclassNameFor(Vector.class, SdkVersion.UNKNOWN));
  }

  private String convertPathToNative(String path) {
    return path.replace('/', File.separatorChar).replace('\\', File.separatorChar);
  }

  class InnerClass {
  }
}
