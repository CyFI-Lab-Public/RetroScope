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
 * Defines essential functionality that every parser we implement
 * will support. This is then extended for HTML and Javascript parsing.
 *
 * <p>The typical caller is a Template System and will usually ask
 * us to parse either a character at a time or a fragment of a template
 * at a time, stopping only when it needs to determine the state of the
 * parser for escaping purposes.
 *
 * <p>We will later add methods to save and restore the full state
 * of the parser to better support conditional processing.
 */
public interface Parser {

  // Consider using a Constants class instead
  public final static ExternalState STATE_ERROR =
      new ExternalState("STATE_ERROR");

  /**
   * Tell the parser to process the provided {@code char}. Throws exception
   * on an unrecoverable parsing error.
   *
   * @param input the character read
   * @throws ParseException if an unrecoverable error occurred during parsing
   */
  void parse(char input) throws ParseException;

  /**
   * Tell the parser to process the provided {@code String}. Throws exception
   * on an unrecoverable parsing error.
   *
   * @param input the {@code String} to parse
   * @throws ParseException if an unrecoverable error occurred during parsing
   */
  void parse(String input) throws ParseException;

  /**
   * Reset the parser back to its initial default state.
   */
  void reset();

  /**
   * Returns the current state of the parser. May be {@link #STATE_ERROR}
   * if the parser encountered an error. Such an error may be recoverable
   * and the caller may want to continue parsing until {@link #parse(String)}
   * returns {@code false}.
   *
   * @return current state of the parser
   */
  ExternalState getState();

  /**
   * Sets the current line number which is returned during error messages.
   * @param lineNumber the line number to set in the parser
   */
  void setLineNumber(int lineNumber);

  /**
   * Returns the current line number.
   */
  int getLineNumber();

  /**
   * Sets the current column number which is returned during error messages.
   * @param columnNumber the column number to set in the parser
   */
  void setColumnNumber(int columnNumber);

  /**
   * Returns the current column number.
   */
  int getColumnNumber();
}
