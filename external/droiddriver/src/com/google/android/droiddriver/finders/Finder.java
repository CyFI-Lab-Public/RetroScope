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

package com.google.android.droiddriver.finders;

import com.google.android.droiddriver.UiElement;
import com.google.android.droiddriver.exceptions.ElementNotFoundException;

/**
 * Interface for finding UiElement.
 */
public interface Finder {
  /**
   * Returns the matching UiElement. The implementing finder should not poll.
   * <p>
   * Invisible UiElements are skipped.
   *
   * @param context The starting UiElement, used as search context
   * @return The first matching element on the current context
   * @throws ElementNotFoundException If no matching elements are found
   */
  UiElement find(UiElement context);

  /**
   * {@inheritDoc}
   *
   * <p>
   * It is recommended that this method return the description of the finder,
   * for example, "ByAttribute{text equals OK}".
   */
  @Override
  String toString();
}
