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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.android.droiddriver.UiElement;

/**
 * Matches UiElement by a single attribute.
 */
public class ByAttribute<T> extends MatchFinder {
  private final Attribute attribute;
  private final MatchStrategy<? super T> strategy;
  private final T expected;

  protected ByAttribute(Attribute attribute, MatchStrategy<? super T> strategy, T expected) {
    this.attribute = checkNotNull(attribute);
    this.strategy = checkNotNull(strategy);
    this.expected = checkNotNull(expected);
  }

  @Override
  public boolean matches(UiElement element) {
    T value = attribute.getValue(element);
    return strategy.match(expected, value);
  }

  @Override
  public String toString() {
    return String.format("ByAttribute{%s %s %s}", attribute, strategy, expected);
  }
}
