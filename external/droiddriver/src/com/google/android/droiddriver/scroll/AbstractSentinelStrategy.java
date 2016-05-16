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

import com.google.android.droiddriver.UiElement;
import com.google.android.droiddriver.actions.ScrollDirection;
import com.google.android.droiddriver.scroll.Direction.LogicalDirection;
import com.google.android.droiddriver.scroll.Direction.PhysicalToLogicalConverter;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import java.util.List;

/**
 * Base {@link SentinelStrategy} for common code.
 */
public abstract class AbstractSentinelStrategy implements SentinelStrategy {

  /**
   * Gets sentinel based on {@link Predicate}.
   */
  public static abstract class GetStrategy {
    protected final Predicate<? super UiElement> predicate;
    protected final String description;

    protected GetStrategy(Predicate<? super UiElement> predicate, String description) {
      this.predicate = predicate;
      this.description = description;
    }

    public UiElement getSentinel(UiElement parent) {
      return getSentinel(parent.getChildren(predicate));
    }

    protected abstract UiElement getSentinel(List<UiElement> children);

    @Override
    public String toString() {
      return description;
    }
  }

  /**
   * Decorates an existing {@link GetStrategy} by adding another
   * {@link Predicate}.
   */
  public static class MorePredicateGetStrategy extends GetStrategy {
    private final GetStrategy original;

    public MorePredicateGetStrategy(GetStrategy original,
        Predicate<? super UiElement> extraPredicate, String extraDescription) {
      super(Predicates.and(original.predicate, extraPredicate), extraDescription
          + original.description);
      this.original = original;
    }

    @Override
    protected UiElement getSentinel(List<UiElement> children) {
      return original.getSentinel(children);
    }
  }

  /**
   * Returns the first child as the sentinel.
   */
  public static final GetStrategy FIRST_CHILD_GETTER = new GetStrategy(Predicates.alwaysTrue(),
      "FIRST_CHILD") {
    @Override
    protected UiElement getSentinel(List<UiElement> children) {
      return children.get(0);
    }
  };

  /**
   * Returns the last child as the sentinel.
   */
  public static final GetStrategy LAST_CHILD_GETTER = new GetStrategy(Predicates.alwaysTrue(),
      "LAST_CHILD") {
    @Override
    protected UiElement getSentinel(List<UiElement> children) {
      return children.get(children.size() - 1);
    }
  };

  /**
   * Returns the second child as the sentinel. Useful when the activity shows a
   * fixed first child.
   */
  public static final GetStrategy SECOND_CHILD_GETTER = new GetStrategy(Predicates.alwaysTrue(),
      "SECOND_CHILD") {
    @Override
    protected UiElement getSentinel(List<UiElement> children) {
      return children.get(1);
    }
  };

  protected final GetStrategy backwardGetStrategy;
  protected final GetStrategy forwardGetStrategy;
  protected final PhysicalToLogicalConverter physicalToLogicalConverter;

  public AbstractSentinelStrategy(GetStrategy backwardGetStrategy, GetStrategy forwardGetStrategy,
      PhysicalToLogicalConverter physicalToLogicalConverter) {
    this.backwardGetStrategy = backwardGetStrategy;
    this.forwardGetStrategy = forwardGetStrategy;
    this.physicalToLogicalConverter = physicalToLogicalConverter;
  }

  protected UiElement getSentinel(UiElement parent, ScrollDirection direction) {
    LogicalDirection logicalDirection = physicalToLogicalConverter.toLogicalDirection(direction);
    if (logicalDirection == LogicalDirection.BACKWARD) {
      return backwardGetStrategy.getSentinel(parent);
    } else {
      return forwardGetStrategy.getSentinel(parent);
    }
  }

  @Override
  public String toString() {
    return String.format("{backwardGetStrategy=%s, forwardGetStrategy=%s}", backwardGetStrategy,
        forwardGetStrategy);
  }
}
