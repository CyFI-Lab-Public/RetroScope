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

import java.util.Arrays;
import java.util.List;

/**
 * @author swoodward@google.com (Stephen Woodward)
 */
public class SdkVersionTest extends TestCase {

  public void testGetAllVersions() {
    List<SdkVersion> versions = Arrays.asList(SdkVersion.getAllVersions());
    List<SdkVersion> values = Arrays.asList(SdkVersion.values());
    assertEquals(6, versions.size());
    assertTrue(values.containsAll(versions));
    assertFalse(versions.contains(SdkVersion.UNKNOWN));
  }

  public void testGetVersionName() {
    assertEquals("v15", SdkVersion.CUPCAKE.getVersionName());
    assertEquals("v16", SdkVersion.DONUT.getVersionName());
    assertEquals("v201", SdkVersion.ECLAIR_0_1.getVersionName());
    assertEquals("v21", SdkVersion.ECLAIR_MR1.getVersionName());
    assertEquals("v22", SdkVersion.FROYO.getVersionName());
    assertEquals("v23", SdkVersion.GINGERBREAD.getVersionName());
    assertEquals("", SdkVersion.UNKNOWN.getVersionName());
    assertEquals("Unknown new SDK has been added, update this test",
        7, SdkVersion.values().length);
  }

  public void testGetPackagePrefix() {
    assertEquals("v15.", SdkVersion.CUPCAKE.getPackagePrefix());
    assertEquals("v16.", SdkVersion.DONUT.getPackagePrefix());
    assertEquals("v201.", SdkVersion.ECLAIR_0_1.getPackagePrefix());
    assertEquals("v21.", SdkVersion.ECLAIR_MR1.getPackagePrefix());
    assertEquals("v22.", SdkVersion.FROYO.getPackagePrefix());
    assertEquals("v23.", SdkVersion.GINGERBREAD.getPackagePrefix());
    assertEquals("", SdkVersion.UNKNOWN.getPackagePrefix());
    assertEquals("Unknown new SDK has been added, update this test",
        7, SdkVersion.values().length);
  }

  public void testGetCurrentVersion() {
    // Always UNKNOWN on the desktop
    assertEquals(SdkVersion.UNKNOWN, SdkVersion.getCurrentVersion());
  }

  public void testGetVersionFor() {
    assertEquals(SdkVersion.CUPCAKE, SdkVersion.getVersionFor(3));
    assertEquals(SdkVersion.DONUT, SdkVersion.getVersionFor(4));
    assertEquals(SdkVersion.ECLAIR_0_1, SdkVersion.getVersionFor(6));
    assertEquals(SdkVersion.ECLAIR_MR1, SdkVersion.getVersionFor(7));
    assertEquals(SdkVersion.FROYO, SdkVersion.getVersionFor(8));
    assertEquals(SdkVersion.GINGERBREAD, SdkVersion.getVersionFor(9));
    assertEquals(SdkVersion.UNKNOWN, SdkVersion.getVersionFor(-1));
    // All likely future version numbers...  Modify this when a new version is added
    for (int i = 10; i < 50; ++i) {
      assertEquals("Unknown new SDK has been added, update this test",
          SdkVersion.UNKNOWN, SdkVersion.getVersionFor(i));
    }
  }
}
