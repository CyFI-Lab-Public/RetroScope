/*
 * Copyright (C) 2010 Google Inc.
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

package org.clearsilver.jni;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Loads the ClearSilver JNI library.
 *
 * <p>By default, it attempts to load the library 'clearsilver-jni' from the
 * path specified in the 'java.library.path' system property. However, this
 * can be overriden by calling {@link #setLibraryName(String)} and
 * {@link #setLibrarySearchPaths(String[])}.</p>
 *
 * <p>If this fails, the JVM exits with a code of 1. However, this strategy
 * can be changed using {@link #setFailureCallback(Runnable)}.</p>
 */
public final class JNI {

  /**
   * Failure callback strategy that writes a message to sysout, then calls
   * System.exit(1).
   */
  public static Runnable EXIT_JVM = new Runnable() {
    public void run() {
      System.err.println("Could not load '" + libraryName + "'. Searched:");
      String platformLibraryName = System.mapLibraryName(libraryName);
      for (String path : librarySearchPaths) {
        System.err.println("  " +
            new File(path, platformLibraryName).getAbsolutePath());
      }
      System.err.println(
          "Try specifying -Djava.library.path=[directory] or calling "
              + JNI.class.getName() + ".setLibrarySearchPaths(String...)");
      System.exit(1);
    }
  };

  /**
   * Failure callback strategy that throws an UnsatisfiedLinkError, which
   * should be caught be client code.
   */
  public static Runnable THROW_ERROR = new Runnable() {
    public void run() {
      throw new UnsatisfiedLinkError("Could not load '" + libraryName + "'");
    }
  };

  private static Runnable failureCallback = EXIT_JVM;

  private static Object callbackLock = new Object();

  private static String libraryName = "clearsilver-jni";

  private static String[] librarySearchPaths
      = System.getProperty("java.library.path", ".").split(
          Pattern.quote(File.pathSeparator));

  private static volatile boolean successfullyLoadedLibrary;

  /**
   * Attempts to load the ClearSilver JNI library.
   *
   * @see #setFailureCallback(Runnable)
   */
  public static void loadLibrary() {

    // Library already loaded? Great - nothing to do.
    if (successfullyLoadedLibrary) {
      return;
    }

    synchronized (callbackLock) {

      // Search librarySearchPaths...
      String platformLibraryName = System.mapLibraryName(libraryName);
      for (String path : librarySearchPaths) {
        try {
          // Attempt to load the library in that path.
          System.load(new File(path, platformLibraryName).getAbsolutePath());
          // If we got here, it worked. We're done.
          successfullyLoadedLibrary = true;
          return;
        } catch (UnsatisfiedLinkError e) {
          // Library not found. Continue loop.
        }
      }

      // Still here? Couldn't load library. Fail.
      if (failureCallback != null) {
        failureCallback.run();
      }
    }

  }

  /**
   * Sets a callback for what should happen if the JNI library cannot
   * be loaded. The default is {@link #EXIT_JVM}.
   *
   * @see #EXIT_JVM
   * @see #THROW_ERROR
   */
  public static void setFailureCallback(Runnable failureCallback) {
    synchronized(callbackLock) {
      JNI.failureCallback = failureCallback;
    }
  }

  /**
   * Set name of JNI library to load. Default is 'clearsilver-jni'.
   */
  public static void setLibraryName(String libraryName) {
    JNI.libraryName = libraryName;
  }

  /**
   * Sets locations where JNI library is searched.
   */
  public static void setLibrarySearchPaths(String... paths) {
    JNI.librarySearchPaths = paths;
  }

}
