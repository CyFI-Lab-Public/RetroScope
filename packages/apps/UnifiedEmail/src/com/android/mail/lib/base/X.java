/*
 * Copyright (C) 2000 Google Inc.
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

package com.android.mail.lib.base;

/**
 * A utility class that contains some very widely used functionality.
 * This class is named "X" just to get a short name that can be typed
 * everywhere without cluttering up the code.  For example, it
 * seems a lot less verbose to say: "X.assertTrue(empty())" instead of
 * "Assert.assertTrue(empty())".
 *
 * <p>Consider using {@link Preconditions} instead though.
 *
 * <p>If your application is using JDK 1.4, feel free to use the built-in
 * assert() methods instead. <b>NOTE:</b> Except remember that JDK assertions
 * are not normally enabled unless you pass the -ea flag to the jvm.
 */
public final class X {

  /**
   * This class should not be instantiated. It provides static methods
   * only.
   */
  private X() {}

  /**
   * Raise a runtime exception if the supplied argument is false (note: if you
   * are checking a precondition, please use {@link Preconditions} instead).
   */
  public static void assertTrue(boolean b) {
    if (!b)
      throw new RuntimeException("Assertion failed");
  }

  /**
   * Raise a runtime exception if the supplied argument is false and print
   * out the error message (note: if you are checking a precondition, please use
   * {@link Preconditions} instead).
   */
  public static void assertTrue(boolean b, String msg) {
    if (!b)
      throw new RuntimeException("Assertion failed: " + msg);
  }
}