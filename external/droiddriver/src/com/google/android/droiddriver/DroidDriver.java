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

package com.google.android.droiddriver;

import com.google.android.droiddriver.exceptions.ElementNotFoundException;
import com.google.android.droiddriver.exceptions.TimeoutException;
import com.google.android.droiddriver.finders.Finder;

public interface DroidDriver {
  /**
   * Returns whether a matching element exists without polling. Use this if the
   * UI is not in the progress of updating.
   */
  boolean has(Finder finder);

  /**
   * Returns whether a matching element appears within {@code timeoutMillis}.
   * Use this only if you have no way to determine the content of current page.
   * There are very few occasions using this is justified. For instance, you are
   * looking for UiElements in a scrollable view, whose content varies based on
   * the scroll position. Refrain from using this method in these cases:
   * <ul>
   * <li>You know one of a set of UiElements will show, but are not sure which
   * one. Use this instead:
   *
   * <pre>
   * UiElement el = driver.on(By.anyOf(finder1, finder2, ...));
   * // UI is stable now, find which one is returned
   * if (finder1.matches(el)) ...
   * </pre>
   *
   * </li>
   * <li>You know the UiElement will appear, and want to optimize the speed by
   * using a smaller timeout than the default timeout. It's not worth it -- on
   * and checkExists return as soon as the finder is found. If it is not found,
   * that's a test failure and should be rare.</li>
   * </ul>
   */
  boolean has(Finder finder, long timeoutMillis);

  /**
   * Returns the first {@link UiElement} found using the given finder. This
   * method will poll until a match is found, or the default timeout is reached.
   *
   * @param finder The matching mechanism
   * @return The first matching element
   * @throws TimeoutException If no matching elements are found within the
   *         allowed time
   */
  UiElement on(Finder finder);

  /**
   * Returns the first {@link UiElement} found using the given finder without
   * polling. This method is useful in {@link Poller.PollingListener#onPolling}.
   * In other situations polling is desired, and {@link #on} is more
   * appropriate.
   *
   * @param finder The matching mechanism
   * @return The first matching element
   * @throws ElementNotFoundException If no matching elements are found
   */
  UiElement find(Finder finder);

  /**
   * Polls until a {@link UiElement} is found using the given finder, or the
   * default timeout is reached.
   *
   * @param finder The matching mechanism
   * @throws TimeoutException If matching element does not appear within the
   *         default timeout
   */
  void checkExists(Finder finder);

  /**
   * Polls until the {@link UiElement} found using the given finder is gone, or
   * the default timeout is reached.
   *
   * @param finder The matching mechanism
   * @throws TimeoutException If matching element is not gone within the default
   *         timeout
   */
  void checkGone(Finder finder);

  /**
   * Returns the {@link Poller}.
   */
  Poller getPoller();

  /**
   * Sets the {@link Poller}.
   */
  void setPoller(Poller poller);

  /**
   * Dumps the UiElement tree to a file to help debug. The tree is based on the
   * last used root UiElement if it exists. Screenshot is always current. If
   * they do not match, the UiElement tree must be stale, indicating that you
   * should use a fresh UiElement instead of an old instance.
   *
   * @param path the path of file to save the tree
   * @return whether the dumping succeeded
   */
  boolean dumpUiElementTree(String path);
}
