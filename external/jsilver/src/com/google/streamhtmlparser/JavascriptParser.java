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

/**
 * Methods exposed for Javascript parsing of text to facilitate implementation
 * of Automatic context-aware escaping. This interface does not add
 * additional methods on top of {@code Parser} for the time being,
 * it simply exposes the states in which the Javascript parser may be in.
 *
 * <p>Note: These are the exact states exposed in the original C++ Parser.
 */
public interface JavascriptParser extends Parser {

  public static final ExternalState STATE_TEXT =
      new ExternalState("STATE_TEXT");
  public static final ExternalState STATE_Q =
      new ExternalState("STATE_Q");
  public static final ExternalState STATE_DQ =
      new ExternalState("STATE_DQ");
  public static final ExternalState STATE_REGEXP =
      new ExternalState("STATE_REGEXP");
  public static ExternalState STATE_COMMENT =
      new ExternalState("STATE_COMMENT");
}
