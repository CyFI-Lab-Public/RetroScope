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
package org.eclipse.wb.internal.core.utils.execution;

import org.eclipse.swt.widgets.Display;
import org.eclipse.wb.internal.core.DesignerPlugin;
import org.eclipse.wb.internal.core.utils.reflect.ReflectionUtils;

import java.beans.Beans;

/**
 * Utilities for executing actions, such as {@link RunnableEx}.
 *
 * @author scheglov_ke
 * @coverage core.util
 */
public class ExecutionUtils {
  ////////////////////////////////////////////////////////////////////////////
  //
  // Constructor
  //
  ////////////////////////////////////////////////////////////////////////////
  private ExecutionUtils() {
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Sleep
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Sleeps given number of milliseconds, ignoring exceptions.
   */
  public static void sleep(final int millis) {
    runIgnore(new RunnableEx() {
      @Override
    public void run() throws Exception {
        Thread.sleep(millis);
      }
    });
  }

  /**
   * Waits given number of milliseconds and runs events loop every 1 millisecond. At least one
   * events loop will be executed. If current thread is not UI thread, then this method works just
   * as {@link #sleep(int)}.
   */
  public static void waitEventLoop(int millis) {
    Display display = Display.getCurrent();
    if (display != null) {
      long nanos = millis * 1000000L;
      long start = System.nanoTime();
      do {
        sleep(0);
        while (display.readAndDispatch()) {
          // do nothing
        }
      } while (System.nanoTime() - start < nanos);
    } else {
      sleep(millis);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // void
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Runs given {@link RunnableEx} and ignores exceptions.
   *
   * @return <code>true</code> if execution was finished without exception.
   */
  public static boolean runIgnore(RunnableEx runnable) {
    try {
      runnable.run();
      return true;
    } catch (Throwable e) {
      return false;
    }
  }

  /**
   * Runs given {@link RunnableEx} and logs exceptions using {@link DesignerPlugin#log(Throwable)}.
   *
   * @return <code>true</code> if execution was finished without exception.
   */
  public static boolean runLog(RunnableEx runnable) {
    try {
      runnable.run();
      return true;
    } catch (Throwable e) {
      DesignerPlugin.log(e);
      return false;
    }
  }

  /**
   * Runs given {@link RunnableEx} and re-throws exceptions using {@link RuntimeException}.
   */
  public static void runRethrow(RunnableEx runnable) {
    try {
      runnable.run();
    } catch (Throwable e) {
      throw ReflectionUtils.propagate(e);
    }
  }

  /**
   * Runs given {@link RunnableEx} and re-throws exceptions using {@link RuntimeException}.
   */
  public static void runRethrow(RunnableEx runnable, String format, Object... args) {
    try {
      runnable.run();
    } catch (Throwable e) {
      String message = String.format(format, args);
      throw new RuntimeException(message, e);
    }
  }

  /**
   * Ensures that {@link Beans#isDesignTime()} returns <code>true</code> and runs given
   * {@link RunnableEx}.
   */
  public static void runDesignTime(RunnableEx runnable) throws Exception {
    boolean old_designTime = Beans.isDesignTime();
    try {
      Beans.setDesignTime(true);
      runnable.run();
    } finally {
      Beans.setDesignTime(old_designTime);
    }
  }

  /**
   * Ensures that {@link Beans#isDesignTime()} returns <code>true</code> and runs given
   * {@link RunnableEx}.
   */
  public static <T> T runDesignTime(RunnableObjectEx<T> runnable) throws Exception {
    boolean old_designTime = Beans.isDesignTime();
    try {
      Beans.setDesignTime(true);
      return runnable.runObject();
    } finally {
      Beans.setDesignTime(old_designTime);
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // UI
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Runs given {@link RunnableEx} inside of UI thread, using {@link Display#syncExec(Runnable)}.
   *
   * @return <code>true</code> if {@link RunnableEx} was executed without any {@link Exception}.
   */
  public static boolean runLogUI(final RunnableEx runnable) {
    final boolean[] success = new boolean[1];
    Display.getDefault().syncExec(new Runnable() {
      @Override
    public void run() {
        success[0] = ExecutionUtils.runLog(runnable);
      }
    });
    return success[0];
  }

  /**
   * Runs given {@link RunnableEx} inside of UI thread, using {@link Display#syncExec(Runnable)}.
   */
  public static void runRethrowUI(final RunnableEx runnable) {
    Display.getDefault().syncExec(new Runnable() {
      @Override
    public void run() {
        ExecutionUtils.runRethrow(runnable);
      }
    });
  }

  /**
   * Runs given {@link RunnableEx} within UI thread using {@link Display#asyncExec(Runnable)}. Logs
   * a {@link Throwable} which may occur.
   */
  public static void runAsync(final RunnableEx runnable) {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
    public void run() {
        ExecutionUtils.runLog(runnable);
      }
    });
  }

  /**
   * Runs given {@link RunnableEx} inside of UI thread, using {@link Display#syncExec(Runnable)}.
   */
  @SuppressWarnings("unchecked")
  public static <T> T runObjectUI(final RunnableObjectEx<T> runnable) {
    final Object[] result = new Object[1];
    runRethrowUI(new RunnableEx() {
      @Override
    public void run() throws Exception {
        result[0] = runObject(runnable);
      }
    });
    return (T) result[0];
  }

  /**
   * Runs given {@link RunnableEx} as {@link #runLog(RunnableEx)}, but using
   * {@link Display#asyncExec(Runnable)}.
   */
  public static void runLogLater(final RunnableEx runnable) {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
    public void run() {
        ExecutionUtils.runLog(runnable);
      }
    });
  }

  ////////////////////////////////////////////////////////////////////////////
  //
  // Object
  //
  ////////////////////////////////////////////////////////////////////////////
  /**
   * Runs given {@link RunnableEx} and re-throws exceptions using {@link RuntimeException}.
   *
   * @return the {@link Object} returned by {@link RunnableEx#run()}.
   */
  public static <T> T runObject(RunnableObjectEx<T> runnable) {
    try {
      return runnable.runObject();
    } catch (Throwable e) {
      throw ReflectionUtils.propagate(e);
    }
  }

  /**
   * Runs given {@link RunnableEx} and re-throws exceptions using {@link RuntimeException}.
   *
   * @return the {@link Object} returned by {@link RunnableEx#run()}.
   */
  public static <T> T runObject(RunnableObjectEx<T> runnable, String format, Object... args) {
    try {
      return runnable.runObject();
    } catch (Throwable e) {
      String message = String.format(format, args);
      throw new Error(message, e);
    }
  }

  /**
   * Runs given {@link RunnableEx} and ignores exceptions.
   *
   * @return the {@link Object} returned by {@link RunnableEx#run()} or <code>defaultValue</code> if
   *         exception happened.
   */
  public static <T> T runObjectIgnore(RunnableObjectEx<T> runnable, T defaultValue) {
    try {
      return runnable.runObject();
    } catch (Throwable e) {
      return defaultValue;
    }
  }

  /**
   * Runs given {@link RunnableEx} and logs exceptions using {@link DesignerPlugin#log(Throwable)}.
   *
   * @return the {@link Object} returned by {@link RunnableEx#run()} or <code>defaultValue</code> if
   *         exception was logged.
   */
  public static <T> T runObjectLog(RunnableObjectEx<T> runnable, T defaultValue) {
    try {
      return runnable.runObject();
    } catch (Throwable e) {
      DesignerPlugin.log(e);
      return defaultValue;
    }
  }

}
