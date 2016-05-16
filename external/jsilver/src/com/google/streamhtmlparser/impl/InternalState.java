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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A very simple representation of the parser internal state. The state
 * contains a small integer identifier (from 1 to 255) to allow for
 * the implementation of a simple finite state machine. The name is
 * purely informational.
 *
 * <p>In order to eliminate the possibility that different states have
 * the same identifier, this class manages the idenitifiers themselves.
 * The HTML and Javascript parser states are managed elsewhere in different
 * "namespaces" hence will not clash and there is no current need for this
 * class to disambiguate them further.
 *
 * <p>The methods to create new <code>InternalState</code> instances are
 * package-scope only as they are only needed by <code>HtmlParserImpl</code>
 * and <code>JavascriptParserImpl</code>.
 */
class InternalState {

  // An InternalState to represent an error condition for all parsers.
  static final InternalState INTERNAL_ERROR_STATE = new InternalState();

  // MAX_ID and FIRST_ID are only used for asserts against developer error.
  private static final int MAX_ID = 255;
  private static final int FIRST_ID = 1;

  private static AtomicInteger htmlStates = new AtomicInteger(FIRST_ID);
  private static AtomicInteger javascriptStates = new AtomicInteger(FIRST_ID);
  private final String name;
  private final int id;

  /**
   * @param name the {@code String} identifier for this state
   * @param id the integer identiifer for this state, guaranteed to be unique
   */
  private InternalState(String name, int id) {
    Preconditions.checkNotNull(name);
    Preconditions.checkArgument(id >= FIRST_ID);
    Preconditions.checkArgument(id <= MAX_ID);
    this.name = name;
    this.id = id;
  }

  /**
   * Used only for the error state. Bypasses assert checks.
   */
  private InternalState() {
    name = "InternalStateError";
    id = 0;
  }

  /**
   * @return {@code String} name of that state.
   */
  public String getName() {
    return name;
  }

  /**
   * @return {@code int} id of that state.
   */
  public int getId() {
    return id;
  }

  /**
   * @return {@code String} representation of that object, the format
   *         may change.
   */
  @Override
  public String toString() {
    return String.format("InternalState: Name: %s; Id: %d", name, id);
  }

  /**
   * Obtain a new {@code InternalState} instance for the HTML parser.
   *
   * @param name a unique identifier for this state useful during debugging
   * @return a new {@code InternalState} object
   */
  static InternalState getInstanceHtml(String name) {
    int htmlStateId = htmlStates.getAndIncrement();
    return new InternalState(name, htmlStateId);
  }

  /**
   * Obtain a new <code>InternalState</code> instance for the Javascript parser.
   *
   * @param name A unique identifier for this state useful during debugging
   * @return a new {@code InternalState} object
   */
  static InternalState getInstanceJavascript(String name) {
    int javascriptStateId = javascriptStates.getAndIncrement();
    return new InternalState(name, javascriptStateId);
  }
}
