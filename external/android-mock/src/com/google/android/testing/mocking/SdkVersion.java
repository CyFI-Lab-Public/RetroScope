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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


/**
 * Represents different SDK versions of the Android SDK.
 * 
 * @author swoodward@google.com (Stephen Woodward)
 */
public enum SdkVersion {
  UNKNOWN("", -1), CUPCAKE("v15", 3), DONUT("v16", 4), ECLAIR_0_1("v201", 6),
  ECLAIR_MR1("v21", 7), FROYO("v22", 8), GINGERBREAD("v23", 9);

  private static final int SDK_VERSION;

  static {
    String sdkString = null;
    int sdkInt;
    try {
      Class<?> buildClass = Class.forName("android.os.Build$VERSION");
      Field sdkField = buildClass.getField("SDK");
      sdkString = (String) sdkField.get(null);
      sdkInt = Integer.parseInt(sdkString);
    } catch (Exception e) {
      // This will always happen on the desktop side.  No big deal.
      if (sdkString != null) {
        // But this is unexpected
        System.out.println(e.toString());
        e.printStackTrace();
      }
      sdkInt = -1;
    }
    SDK_VERSION = sdkInt;
  }

  private final String prefix;
  private final String versionName;
  private final int apiLevel;

  private SdkVersion(String packagePrefix, int apiLevel) {
    versionName = packagePrefix;
    prefix = packagePrefix.length() == 0 ? "" : packagePrefix + ".";
    this.apiLevel = apiLevel;
  }

  /**
   * Returns an array of SdkVersion objects.  This is to be favoured over the
   * {@link #values()} method, since that method will also return the UNKNOWN
   * SDK version, which is not usually a valid version on which to operate.
   * 
   * @return an array of SdkVersion objects.
   */
  public static SdkVersion[] getAllVersions() {
    List<SdkVersion> versions = new ArrayList<SdkVersion>();
    for (SdkVersion version : values()) {
      if (!version.equals(UNKNOWN)) {
        versions.add(version);
      }
    }
    return versions.toArray(new SdkVersion[versions.size()]);
  }

  public String getVersionName() {
    return versionName;
  }

  public String getPackagePrefix() {
    return prefix;
  }

  /**
   * Returns the current SDK version, or UNKNOWN if the version cannot be determined (for instance
   * if this method is invoked from within a J2SE environment).
   * @return the current SDK version.
   */
  public static SdkVersion getCurrentVersion() {
    return getVersionFor(SDK_VERSION);
  }

  static SdkVersion getVersionFor(int apiLevel) {
    for (SdkVersion version : values()) {
      if (version.apiLevel == apiLevel) {
        return version;
      }
    }
    return UNKNOWN;
  }
}
