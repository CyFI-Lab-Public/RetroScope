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

package com.google.streamhtmlparser;

import com.google.common.base.Preconditions;

/**
 * A representation of the parser state suitable for use by the caller
 * of the Parser. The meaning of each state and therefore which action
 * the caller should perform on that state is not self-evident. In particular,
 * it depends on which parser is used (currently {@link HtmlParser} and
 * {@link JavascriptParser}). For examples, you will have to look
 * at the <code>Google Template System</code> and <code>ClearSilver</code>
 * both of which support Auto-Escaping by interfacing with our parser
 * (using the parser written in C++).
 *
 * <p>The caller of the Parser will query for the current parser state at
 * points of interest during parsing of templates. Based on the parser's
 * current state as represented by this class, the caller can determine
 * the appropriate escaping to apply.
 *
 * <p>Note: Given this class is external-facing, I considered creating
 * an interface but it is not likely we'll ever need to add more flexibility
 * and the class is so simple, I figured it was not warranted.
 * 
 *
 * @see HtmlParser
 * @see JavascriptParser
 */
public class ExternalState {

  private final String name;

  /**
   * Creates an {@code ExternalState} object.
   *
   * @param name the name to assign to that state
   * @see HtmlParser
   * @see JavascriptParser
   */
  public ExternalState(String name) {
    Preconditions.checkNotNull(name);   // Developer error if it happens.
    this.name = name;
  }

  /**
   * Returns the name of the object. The name is only needed
   * to provide human-readable information when debugging.
   *
   * @return the name of that object
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the string representation of this external state.
   * The details of this representation are subject to change.
   */
  @Override
  public String toString() {
    return String.format("ExternalState: %s", name);
  }
}
