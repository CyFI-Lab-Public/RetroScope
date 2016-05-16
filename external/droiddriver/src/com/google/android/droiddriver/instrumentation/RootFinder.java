/*
 * Copyright (C) 2013 DroidDriver committers
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

package com.google.android.droiddriver.instrumentation;

import com.google.android.droiddriver.exceptions.DroidDriverException;
import com.google.android.droiddriver.util.Logs;

import android.os.Build;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Class to find the root view.
 * Note(twickham): This class is no longer being used.
 */
public class RootFinder {

  private static final String VIEW_FIELD_NAME = "mViews";
  private static final Field viewsField;
  private static final Object windowManagerObj;

  static {
    String windowManagerClassName = Build.VERSION.SDK_INT >= 17 ? "android.view.WindowManagerGlobal"
        : "android.view.WindowManagerImpl";
    String instanceMethod = Build.VERSION.SDK_INT >= 17 ? "getInstance" : "getDefault";
    try {
      Class<?> clazz = Class.forName(windowManagerClassName);
      Method getMethod = clazz.getMethod(instanceMethod);
      windowManagerObj = getMethod.invoke(null);
      viewsField = clazz.getDeclaredField(VIEW_FIELD_NAME);
      viewsField.setAccessible(true);
    } catch (InvocationTargetException ite) {
      throw new DroidDriverException(String.format("could not invoke: %s on %s", instanceMethod,
          windowManagerClassName), ite.getCause());
    } catch (ClassNotFoundException cnfe) {
      throw new DroidDriverException(
          String.format("could not find class: %s", windowManagerClassName), cnfe);
    } catch (NoSuchFieldException nsfe) {
      throw new DroidDriverException(String.format("could not find field: %s on %s",
          VIEW_FIELD_NAME, windowManagerClassName), nsfe);
    } catch (NoSuchMethodException nsme) {
      throw new DroidDriverException(String.format("could not find method: %s on %s",
          instanceMethod, windowManagerClassName), nsme);
    } catch (RuntimeException re) {
      throw new DroidDriverException(
          String.format("reflective setup failed using obj: %s method: %s field: %s",
          windowManagerClassName, instanceMethod, VIEW_FIELD_NAME), re);
    } catch (IllegalAccessException iae) {
      throw new DroidDriverException(
          String.format("reflective setup failed using obj: %s method: %s field: %s",
          windowManagerClassName, instanceMethod, VIEW_FIELD_NAME), iae);
    }
  }

  /**
   * @return a list of {@link View}s.
   */
  public static View[] getRootViews() {
    View[] views = null;

    try {
      views = (View[]) viewsField.get(windowManagerObj);
      Logs.log(Log.DEBUG, "View size:" +views.length);
      return views;
    } catch (RuntimeException re) {
      throw new DroidDriverException(String.format("Reflective access to %s on %s failed.",
          viewsField, windowManagerObj), re);

    } catch (IllegalAccessException iae) {
      throw new DroidDriverException(String.format("Reflective access to %s on %s failed.",
          viewsField, windowManagerObj), iae);
    }
  }
}
