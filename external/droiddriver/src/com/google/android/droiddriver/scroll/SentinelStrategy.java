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
import com.google.android.droiddriver.actions.ScrollDirection;
import com.google.android.droiddriver.finders.Finder;

/**
 * Interface for determining whether scrolling is possible based on a sentinel.
 */
public interface SentinelStrategy {
  /**
   * Tries to scroll {@code parentFinder} in {@code direction}. Returns whether
   * scrolling is effective.
   *
   * @param driver
   * @param parentFinder Finder for the container that can scroll, for instance
   *        a ListView
   * @param direction
   * @return whether scrolling is effective
   */
  boolean scroll(DroidDriver driver, Finder parentFinder, ScrollDirection direction);

  /**
   * {@inheritDoc}
   *
   * <p>
   * It is recommended that this method return a description to help debugging.
   */
  @Override
  String toString();
}
