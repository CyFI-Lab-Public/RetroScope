/*******************************************************************************
 * Copyright (c) 2011 Google, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Google, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.wb.internal.core.utils.check;

import java.text.MessageFormat;

/**
 * <code>Assert</code> is useful for for embedding runtime sanity checks in code. The predicate
 * methods all test a condition and throw some type of unchecked exception if the condition does not
 * hold.
 * <p>
 * Assertion failure exceptions, like most runtime exceptions, are thrown when something is
 * misbehaving. Assertion failures are invariably unspecified behavior; consequently, clients should
 * never rely on these being thrown (and certainly should not being catching them specifically).
 *
 * @author scheglov_ke
 * @coverage core.util
 */
public final class Assert {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  private Assert() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // "legal"
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Asserts that an argument is legal. If the given boolean is not <code>true</code>, an
   * <code>IllegalArgumentException</code> is thrown.
   *
   * @param expression
   *          the boolean expression of the check
   * @return <code>true</code> if the check passes (does not return if the check fails)
   * @exception IllegalArgumentException
   *              if the legality test failed
   */
  public static boolean isLegal(boolean expression) {
    return isLegal(expression, ""); //$NON-NLS-1$
  }

  /**
   * Asserts that an argument is legal. If the given boolean is not <code>true</code>, an
   * <code>IllegalArgumentException</code> is thrown. The given message is included in that
   * exception, to aid debugging.
   *
   * @param expression
   *          the boolean expression of the check
   * @param message
   *          the message to include in the exception
   * @return <code>true</code> if the check passes (does not return if the check fails)
   * @exception IllegalArgumentException
   *              if the legality test failed
   */
  public static boolean isLegal(boolean expression, String message) {
    if (!expression) {
      throw new IllegalArgumentException(message);
    }
    return expression;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // "null"
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Asserts that the given object is <code>null</code>. If this is not the case, some kind of
   * unchecked exception is thrown.
   *
   * @param object
   *          the value to test
   */
  public static void isNull(Object object) {
    isNull(object, ""); //$NON-NLS-1$
  }

  /**
   * Asserts that the given object is <code>null</code>. If this is not the case, some kind of
   * unchecked exception is thrown. The given message is included in that exception, to aid
   * debugging.
   *
   * @param object
   *          the value to test
   * @param message
   *          the message to include in the exception
   */
  public static void isNull(Object object, String message) {
    if (object != null) {
      throw new AssertionFailedException("null argument expected: " + message); //$NON-NLS-1$
    }
  }

  /**
   * Asserts that the given object is not <code>null</code>. If this is not the case, some kind of
   * unchecked exception is thrown. The given message is included in that exception, to aid
   * debugging.
   *
   * @param object
   *          the value to test
   * @param errorFormat
   *          the format of error message to produce if the check fails, as expected by
   *          {@link String#format(String, Object...)}. For example
   *          <code>"Execution flow problem. %s expected, but %s found."</code>.
   * @param args
   *          the arguments for {@code errorFormat}
   */
  public static void isNull(Object object, String errorFormat, Object... args) {
    if (object != null) {
      fail("null argument expected: " + String.format(errorFormat, args)); //$NON-NLS-1$
    }
  }

  /**
   * @param errorFormat
   *          the format of error message suitable for {@link MessageFormat}.
   * @param errorFormat
   *          the format of error message to produce if the check fails, as expected by
   *          {@link MessageFormat}. For example
   *          <code>"Execution flow problem. {0} expected, but {1} found."</code>.
   */
  public static void isNull2(Object object, String errorFormat, Object... args) {
    if (object != null) {
      String message = "null argument expected: " + MessageFormat.format(errorFormat, args); //$NON-NLS-1$
      fail(message);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // not "null"
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Asserts that the given object is not <code>null</code>. If this is not the case, some kind of
   * unchecked exception is thrown.
   *
   * @param object
   *          the value to test
   */
  public static void isNotNull(Object object) {
    isNotNull(object, ""); //$NON-NLS-1$
  }

  /**
   * Asserts that the given object is not <code>null</code>. If this is not the case, some kind of
   * unchecked exception is thrown. The given message is included in that exception, to aid
   * debugging.
   *
   * @param object
   *          the value to test
   * @param message
   *          the message to include in the exception
   */
  public static void isNotNull(Object object, String message) {
    if (object == null) {
      fail("null argument: " + message); //$NON-NLS-1$
    }
  }

  /**
   * Asserts that the given object is not <code>null</code>. If this is not the case, some kind of
   * unchecked exception is thrown. The given message is included in that exception, to aid
   * debugging.
   *
   * @param object
   *          the value to test
   * @param errorFormat
   *          the format of error message to produce if the check fails, as expected by
   *          {@link String#format(String, Object...)}. For example
   *          <code>"Execution flow problem. %s expected, but %s found."</code>.
   * @param args
   *          the arguments for {@code errorFormat}
   */
  public static void isNotNull(Object object, String errorFormat, Object... args) {
    if (object == null) {
      fail("null argument: " + String.format(errorFormat, args)); //$NON-NLS-1$
    }
  }

  /**
   * @param errorFormat
   *          the format of error message suitable for {@link MessageFormat}.
   * @param errorFormat
   *          the format of error message to produce if the check fails, as expected by
   *          {@link MessageFormat}. For example
   *          <code>"Execution flow problem. {0} expected, but {1} found."</code>.
   */
  public static void isNotNull2(Object object, String errorFormat, Object... args) {
    if (object == null) {
      String message = "null argument: " + MessageFormat.format(errorFormat, args); //$NON-NLS-1$
      fail(message);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Fail
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Fails with given message.
   *
   * @param message
   *          the message to include in the exception
   */
  public static void fail(String message) {
    throw new AssertionFailedException(message);
  }

  /**
   * @param errorFormat
   *          the format of error message to produce if the check fails, as expected by
   *          {@link MessageFormat}. For example <code>"{0} expected, but {1} found."</code>.
   */
  public static void fail(String errorFormat, Object... args) {
    String message = MessageFormat.format(errorFormat, args);
    throw new AssertionFailedException(message);
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // "true"
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Asserts that the given boolean is <code>true</code>. If this is not the case, some kind of
   * unchecked exception is thrown.
   *
   * @param expression
   *          the boolean expression of the check
   * @return <code>true</code> if the check passes (does not return if the check fails)
   */
  public static boolean isTrue(boolean expression) {
    return isTrue(expression, ""); //$NON-NLS-1$
  }

  /**
   * Asserts that the given boolean is <code>true</code>. If this is not the case, some kind of
   * unchecked exception is thrown. The given message is included in that exception, to aid
   * debugging.
   *
   * @param expression
   *          the boolean expression of the check
   * @param message
   *          the message to include in the exception
   * @return <code>true</code> if the check passes (does not return if the check fails)
   */
  public static boolean isTrue(boolean expression, String message) {
    if (!expression) {
      fail("assertion failed: " + message); //$NON-NLS-1$
    }
    return expression;
  }

  /**
   * Asserts that the given boolean is <code>true</code>. If this is not the case, some kind of
   * unchecked exception is thrown. The given message is included in that exception, to aid
   * debugging.
   *
   * @param expression
   *          the boolean expression of the check
   * @param errorFormat
   *          the format of error message to produce if the check fails, as expected by
   *          {@link String#format(String, Object...)}. For example
   *          <code>"Execution flow problem. %s expected, but %s found."</code>.
   * @param args
   *          the arguments for {@code errorFormat}
   * @return <code>true</code> if the check passes (does not return if the check fails)
   */
  public static boolean isTrue(boolean expression, String errorFormat, Object... args) {
    if (!expression) {
      fail("assertion failed: " + String.format(errorFormat, args)); //$NON-NLS-1$
    }
    return expression;
  }

  /**
   * Asserts that the given boolean is <code>true</code>. If this is not the case, some kind of
   * unchecked exception is thrown. The given message is included in that exception, to aid
   * debugging.
   *
   * @param expression
   *          the boolean expression to check.
   * @param errorFormat
   *          the format of error message to produce if the check fails, as expected by
   *          {@link MessageFormat}. For example <code>"{0} expected, but {1} found."</code>.
   */
  public static boolean isTrue2(boolean expression, String errorFormat, Object... args) {
    if (!expression) {
      fail(errorFormat, args);
    }
    return expression;
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // equals
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Asserts that given actual value equals expected value. If this is not the case, some kind of
   * unchecked exception is thrown.
   *
   * @param expected
   *          the expected value
   * @param the
   *          actual value to check
   */
  public static void equals(int expected, int actual) {
    equals(expected, actual, expected + " expected, but " + actual + " found");
  }

  /**
   * Asserts that given actual value equals expected value. If this is not the case, some kind of
   * unchecked exception is thrown. The given message is included in that exception, to aid
   * debugging.
   *
   * @param expected
   *          the expected value
   * @param the
   *          actual value to check
   * @param message
   *          the message to include in the exception
   */
  public static void equals(int expected, int actual, String message) {
    if (expected != actual) {
      fail("assertation failed: " + message);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // instanceOf
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Asserts that given object is not <code>null</code> and has class compatible with given.
   */
  public static void instanceOf(Class<?> expectedClass, Object o) {
    if (o == null) {
      fail(expectedClass.getName() + " expected, but 'null' found.");
    }
    if (!expectedClass.isAssignableFrom(o.getClass())) {
      fail(expectedClass.getName() + " expected, but " + o.getClass().getName() + " found.");
    }
  }
}
