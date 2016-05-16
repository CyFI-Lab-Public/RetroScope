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

package com.google.android.droiddriver.util;

import android.app.Activity;

import com.google.android.droiddriver.instrumentation.InstrumentationDriver;
import com.google.common.base.Supplier;

/**
 * Static helper methods for retrieving activities.
 */
public class ActivityUtils {
  private static Supplier<Activity> runningActivitySupplier;

  /**
   * Sets the Supplier for the running (a.k.a. resumed or foreground) activity.
   * Called from {@link com.google.android.droiddriver.runner.TestRunner}. If a
   * custom runner is used, this method must be called appropriately, otherwise
   * {@link #getRunningActivity} won't work.
   */
  public static synchronized void setRunningActivitySupplier(Supplier<Activity> activitySupplier) {
    runningActivitySupplier = activitySupplier;
  }

  /**
   * Gets the running (a.k.a. resumed or foreground) activity.
   * {@link InstrumentationDriver} depends on this.
   *
   * @return the currently running activity, or null if no activity has focus.
   */
  public static synchronized Activity getRunningActivity() {
    return runningActivitySupplier.get();
  }
}
