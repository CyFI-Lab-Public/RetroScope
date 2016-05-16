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
 * Checked exception thrown on an unrecoverable error during parsing.
 * 
 * @see Parser#parse(String)
 */
public class ParseException extends Exception {

  /**
   * Constructs an {@code ParseException} with no detail message.
   */
  public ParseException() {}

  /**
   * Constructs an {@code ParseException} with a detail message obtained
   * from the supplied message and the parser's line and column numbers.
   * @param parser the {@code Parser} that triggered the exception
   * @param msg the error message
   */
  public ParseException(Parser parser, String msg) {
    super(String.format("At line: %d (col: %d); %s",
                        parser.getLineNumber(),
                        parser.getColumnNumber(),
                        msg));
  }
}
