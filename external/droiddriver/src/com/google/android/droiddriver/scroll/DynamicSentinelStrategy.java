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

import android.util.Log;

import com.google.android.droiddriver.DroidDriver;
import com.google.android.droiddriver.UiElement;
import com.google.android.droiddriver.actions.ScrollDirection;
import com.google.android.droiddriver.exceptions.ElementNotFoundException;
import com.google.android.droiddriver.finders.Finder;
import com.google.android.droiddriver.scroll.Direction.PhysicalToLogicalConverter;
import com.google.android.droiddriver.util.Logs;
import com.google.common.base.Objects;

/**
 * Determines whether scrolling is possible by checking whether the sentinel
 * child is updated after scrolling. Use this when
 * {@link UiElement#getChildCount()} is not reliable. This can happen, for
 * instance, when UiAutomationDriver is used, which skips invisible children, or
 * in the case of dynamic list, which shows more items when scrolling beyond the
 * end.
 */
public class DynamicSentinelStrategy extends AbstractSentinelStrategy {

  /**
   * Interface for determining whether sentinel is updated.
   */
  public static interface IsUpdatedStrategy {
    /**
     * Returns whether {@code newSentinel} is updated from {@code oldSentinel}.
     */
    boolean isSentinelUpdated(UiElement newSentinel, UiElement oldSentinel);

    /**
     * {@inheritDoc}
     *
     * <p>
     * It is recommended that this method return a description to help
     * debugging.
     */
    @Override
    String toString();
  }

  /**
   * Determines whether the sentinel is updated by checking a single unique
   * String attribute of a child element of the sentinel (or itself).
   */
  public static abstract class SingleStringUpdated implements IsUpdatedStrategy {
    private final Finder uniqueStringFinder;

    /**
     * @param uniqueStringFinder a Finder relative to the sentinel that finds
     *        its child element which contains a unique String.
     */
    public SingleStringUpdated(Finder uniqueStringFinder) {
      this.uniqueStringFinder = uniqueStringFinder;
    }

    /**
     * @param uniqueStringChild the child of sentinel (or itself) that contains
     *        the unique String
     * @return the unique String
     */
    protected abstract String getUniqueString(UiElement uniqueStringChild);

    private String getUniqueStringFromSentinel(UiElement sentinel) {
      try {
        return getUniqueString(uniqueStringFinder.find(sentinel));
      } catch (ElementNotFoundException e) {
        return null;
      }
    }

    @Override
    public boolean isSentinelUpdated(UiElement newSentinel, UiElement oldSentinel) {
      String newString = getUniqueStringFromSentinel(newSentinel);
      // If newString is null, newSentinel must be partially shown. In this case
      // we return true to allow further scrolling. But program error could also
      // cause this, e.g. a bad choice of GetStrategy. log for debugging.
      if (newString == null) {
        Logs.logfmt(Log.WARN, "Unique String under sentinel %s is null", newSentinel);
        return true;
      }
      if (newString.equals(getUniqueStringFromSentinel(oldSentinel))) {
        Logs.log(Log.INFO, "Unique String is not updated: " + newString);
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(this).addValue(uniqueStringFinder).toString();
    }
  }

  /**
   * Determines whether the sentinel is updated by checking the text of a child
   * element of the sentinel (or itself).
   */
  public static class TextUpdated extends SingleStringUpdated {
    public TextUpdated(Finder uniqueStringFinder) {
      super(uniqueStringFinder);
    }

    @Override
    protected String getUniqueString(UiElement uniqueStringChild) {
      return uniqueStringChild.getText();
    }
  }

  /**
   * Determines whether the sentinel is updated by checking the content
   * description of a child element of the sentinel (or itself).
   */
  public static class ContentDescriptionUpdated extends SingleStringUpdated {
    public ContentDescriptionUpdated(Finder uniqueStringFinder) {
      super(uniqueStringFinder);
    }

    @Override
    protected String getUniqueString(UiElement uniqueStringChild) {
      return uniqueStringChild.getContentDescription();
    }
  }

  private final IsUpdatedStrategy isUpdatedStrategy;

  /**
   * Constructs with {@code GetStrategy}s that decorate the given
   * {@code GetStrategy}s with {@link UiElement#VISIBLE}, and the given
   * {@code isUpdatedStrategy} and {@code physicalToLogicalConverter}. Be
   * careful with {@code GetStrategy}s: the sentinel after each scroll should be
   * unique.
   */
  public DynamicSentinelStrategy(IsUpdatedStrategy isUpdatedStrategy,
      GetStrategy backwardGetStrategy, GetStrategy forwardGetStrategy,
      PhysicalToLogicalConverter physicalToLogicalConverter) {
    super(new MorePredicateGetStrategy(backwardGetStrategy, UiElement.VISIBLE, "VISIBLE_"),
        new MorePredicateGetStrategy(forwardGetStrategy, UiElement.VISIBLE, "VISIBLE_"),
        physicalToLogicalConverter);
    this.isUpdatedStrategy = isUpdatedStrategy;
  }

  /**
   * Defaults to the standard {@link PhysicalToLogicalConverter}.
   */
  public DynamicSentinelStrategy(IsUpdatedStrategy isUpdatedStrategy,
      GetStrategy backwardGetStrategy, GetStrategy forwardGetStrategy) {
    this(isUpdatedStrategy, backwardGetStrategy, forwardGetStrategy,
        PhysicalToLogicalConverter.STANDARD_CONVERTER);
  }

  /**
   * Defaults to LAST_CHILD_GETTER for forward scrolling, and the standard
   * {@link PhysicalToLogicalConverter}.
   */
  public DynamicSentinelStrategy(IsUpdatedStrategy isUpdatedStrategy,
      GetStrategy backwardGetStrategy) {
    this(isUpdatedStrategy, backwardGetStrategy, LAST_CHILD_GETTER,
        PhysicalToLogicalConverter.STANDARD_CONVERTER);
  }

  @Override
  public boolean scroll(DroidDriver driver, Finder parentFinder, ScrollDirection direction) {
    UiElement parent = driver.on(parentFinder);
    UiElement oldSentinel = getSentinel(parent, direction);
    parent.scroll(direction);
    UiElement newSentinel = getSentinel(driver.on(parentFinder), direction);
    return isUpdatedStrategy.isSentinelUpdated(newSentinel, oldSentinel);
  }

  @Override
  public String toString() {
    return String.format("DynamicSentinelStrategy{%s, isUpdatedStrategy=%s}", super.toString(),
        isUpdatedStrategy);
  }
}
