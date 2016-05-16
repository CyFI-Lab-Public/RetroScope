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
package com.google.android.droiddriver.scroll;

import com.google.android.droiddriver.DroidDriver;
import com.google.android.droiddriver.UiElement;
import com.google.android.droiddriver.actions.ScrollDirection;
import com.google.android.droiddriver.exceptions.ElementNotFoundException;
import com.google.android.droiddriver.finders.Finder;

/**
 * Interface for scrolling to the desired child in a scrollable parent view.
 */
public interface Scroller {
  /**
   * Scrolls {@code parentFinder} in both directions if necessary to find
   * {@code childFinder}.
   *
   * @param driver
   * @param parentFinder Finder for the container that can scroll, for instance
   *        a ListView
   * @param childFinder Finder for the desired child; relative to
   *        {@code parentFinder}
   * @return the UiElement matching {@code childFinder}
   * @throws ElementNotFoundException If no match is found
   */
  UiElement scrollTo(DroidDriver driver, Finder parentFinder, Finder childFinder);

  /**
   * Scrolls {@code parentFinder} in {@code direction} if necessary to find
   * {@code childFinder}.
   *
   * @param driver
   * @param parentFinder Finder for the container that can scroll, for instance
   *        a ListView
   * @param childFinder Finder for the desired child; relative to
   *        {@code parentFinder}
   * @param direction
   * @return the UiElement matching {@code childFinder}
   * @throws ElementNotFoundException If no match is found
   */
  UiElement scrollTo(DroidDriver driver, Finder parentFinder, Finder childFinder,
      ScrollDirection direction);
}
