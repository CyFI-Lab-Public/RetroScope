/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.testing.littlemock;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for helping guess the application data directory.
 */
public class AppDataDirGuesser {

  /** A single default instance of app data dir guesser, for overriding if you really need to. */
  private static volatile AppDataDirGuesser sInjectableInstance = new AppDataDirGuesser();

  public static void setInstance(AppDataDirGuesser instance) {
    sInjectableInstance = instance;
  }

  public static AppDataDirGuesser getsInstance() {
    return sInjectableInstance;
  }

  public File guessSuitableDirectoryForGeneratedClasses() {
    try {
      ClassLoader classLoader = AppDataDirGuesser.class.getClassLoader();
      // Check that we have an instance of the PathClassLoader.
      Class<?> clazz = Class.forName("dalvik.system.PathClassLoader");
      clazz.cast(classLoader);
      // Use the toString() method to calculate the data directory.
      String pathFromThisClassLoader =
          getPathFromPathClassLoader(classLoader, clazz);
      File[] results = guessPath(pathFromThisClassLoader);
      if (results.length > 0) {
        return results[0];
      }
    } catch (ClassCastException e) {
      // Fall through, we will return null.
    } catch (ClassNotFoundException e) {
      // Fall through, we will return null.
    }
    return null;
  }

  private String getPathFromPathClassLoader(
      ClassLoader classLoader, Class<?> pathClassLoaderClass) {
    // Prior to ICS, we can simply read the "path" field of the
    // PathClassLoader.
    try {
      Field pathField = pathClassLoaderClass.getDeclaredField("path");
      pathField.setAccessible(true);
      return (String) pathField.get(classLoader);
    } catch (NoSuchFieldException e) {
      // Ignore and fall back on parsing the output of toString below
    } catch (IllegalAccessException e) {
      // Ignore and fall back on parsing the output of toString below
    } catch (ClassCastException e) {
      // Ignore and fall back on parsing the output of toString below
    }

    // Parsing toString() method: yuck.  But no other way to get the path.
    // Strip out the bit between square brackets, that's our path.
    String result = classLoader.toString();
    int index = result.lastIndexOf('[');
    result = (index == -1) ? result : result.substring(index + 1);
    index = result.indexOf(']');
    return (index == -1) ? result : result.substring(0, index);
  }

  // @VisibleForTesting
  File[] guessPath(String input) {
    List<File> results = new ArrayList<File>();
    for (String potential : splitPathList(input)) {
      if (!potential.startsWith("/data/app/")) {
        continue;
      }
      int start = "/data/app/".length();
      int end = potential.lastIndexOf(".apk");
      if (end != potential.length() - 4) {
        continue;
      }
      int dash = potential.indexOf("-");
      if (dash != -1) {
        end = dash;
      }
      String packageName = potential.substring(start, end);
      File dataDir = new File("/data/data/" + packageName);
      if (isWriteableDirectory(dataDir)) {
        File cacheDir = new File(dataDir, "cache");
        if (fileOrDirExists(cacheDir) || makeDirectory(cacheDir)) {
          if (isWriteableDirectory(cacheDir)) {
            results.add(cacheDir);
          }
        }
      }
    }

    return results.toArray(new File[results.size()]);
  }

  // @VisibleForTesting
  static String[] splitPathList(String input) {
    String trimmed = input;
    if (input.startsWith("dexPath=")) {
      int start = "dexPath=".length();
      int end = input.indexOf(',');

      trimmed = (end == -1) ? input.substring(start) :
          input.substring(start, end);
    }

    return trimmed.split(":");
  }

  // @VisibleForTesting
  boolean fileOrDirExists(File file) {
      return file.exists();
  }

  // @VisibleForTesting
  boolean makeDirectory(File file) {
      return file.mkdir();
  }

  // @VisibleForTesting
  boolean isWriteableDirectory(File file) {
    return file.isDirectory() && file.canWrite();
  }
}
