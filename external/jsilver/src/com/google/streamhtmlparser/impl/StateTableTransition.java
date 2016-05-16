/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.streamhtmlparser.impl;

import com.google.common.base.Preconditions;

/**
 * Holds one state transition as derived from a Python configuration
 * file. A state transition is a triplet as follows:
 * <ul>
 * <li>An expression which consists of one or more characters and/or
 *     one or more range of characters.
 * <li> A source state.
 * <li> A destination state.
 * </ul>
 *
 * <p>For example, the triplet ("a-z123", A, B) will cause the
 * state to go from A to B for any character that is either 1,2,3 or in
 * the range a-z inclusive.
 */
class StateTableTransition {

  private final String expression;
  private final InternalState from;
  private final InternalState to;

   /**
   * Returns the full state of the {@code StateTableTransition} in a
   * human readable form. The format of the returned {@code String} is not
   * specified and is subject to change.
   *
   * @return full state of the {@code StateTableTransition}
   */
  @Override
  public String toString() {
    return String.format("Expression: %s; From: %s; To: %s",
                         expression, from, to);
  }

  StateTableTransition(String expression, InternalState from,
                       InternalState to) {
    // Developer error if any triggers.
    Preconditions.checkNotNull(expression);
    Preconditions.checkNotNull(from);
    Preconditions.checkNotNull(to);
    this.expression = expression;
    this.from = from;
    this.to = to;
  }

   String getExpression() {
     return expression;
   }

   InternalState getFrom() {
     return from;
   }

   InternalState getTo() {
     return to;
   }
}
