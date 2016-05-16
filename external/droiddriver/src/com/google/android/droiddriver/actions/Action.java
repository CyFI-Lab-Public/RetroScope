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

package com.google.android.droiddriver.actions;

import android.view.InputEvent;

import com.google.android.droiddriver.InputInjector;
import com.google.android.droiddriver.UiElement;

/**
 * Interface for performing action on a UiElement. An action is a high-level
 * user interaction that consists of a series of {@link InputEvent}s.
 */
public interface Action {
  /**
   * Performs the action.
   *
   * @param injector the injector to inject {@link InputEvent}s
   * @param element the Ui element to perform the action on
   * @return Whether the action is successful. Some actions throw exceptions in
   *         case of failure, when that behavior is more appropriate. For
   *         example, ClickAction.
   */
  boolean perform(InputInjector injector, UiElement element);

  /**
   * Gets the timeout to wait for an indicator that the action has been carried
   * out. Different DroidDriver implementations use this value in different
   * ways. For example, UiAutomationDriver waits for AccessibilityEvent up to
   * this value. InstrumentationDriver ignores this value because it
   * synchronizes on the event loop.
   */
  long getTimeoutMillis();

  /**
   * {@inheritDoc}
   *
   * <p>
   * It is recommended that this method return the description of the action,
   * for example, "TypeAction{text to type}".
   */
  @Override
  String toString();
}
