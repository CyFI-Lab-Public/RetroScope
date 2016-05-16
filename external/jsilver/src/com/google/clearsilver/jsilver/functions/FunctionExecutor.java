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

package com.google.clearsilver.jsilver.functions;

import com.google.clearsilver.jsilver.values.Value;

import java.io.IOException;

/**
 * Execute functions in templates.
 */
public interface FunctionExecutor {

  /**
   * Lookup a function by name, execute it and return the results.
   */
  Value executeFunction(String functionName, Value... args);

  /**
   * Escapes some text.
   * 
   * @param name Strategy for escaping text. If null or "none", text will be left as is.
   * @param input Text to be escaped.
   * @param output Where to write the result to.
   */
  void escape(String name, String input, Appendable output) throws IOException;

  /**
   * Look up a function by name, and report whether it is an escaping function.
   */
  boolean isEscapingFunction(String name);
}
