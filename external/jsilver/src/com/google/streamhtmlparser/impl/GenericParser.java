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
import com.google.streamhtmlparser.ExternalState;
import com.google.streamhtmlparser.Parser;
import com.google.streamhtmlparser.ParseException;
import com.google.streamhtmlparser.util.HtmlUtils;

import java.util.Map;

/**
 * An implementation of the {@code Parser} interface that is common to both
 * {@code HtmlParser} and {@code JavascriptParser}.
 *
 * <p>Provides methods for parsing input and ensuring that all in-state,
 * entering-a-state and exiting-a-state callbacks are invoked as appropriate.
 *
 * <p>This class started as abstract but it was found better for testing to
 * make it instantiatable so that the parsing logic can be tested with dummy
 * state transitions.
 */
public class GenericParser implements Parser {

  protected final ParserStateTable parserStateTable;
  protected final Map<InternalState, ExternalState> intToExtStateTable;
  protected final InternalState initialState;
  protected InternalState currentState;
  protected int lineNumber;
  protected int columnNumber;

  protected GenericParser(ParserStateTable parserStateTable,
                          Map<InternalState, ExternalState> intToExtStateTable,
                          InternalState initialState) {
    this.parserStateTable = parserStateTable;
    this.intToExtStateTable = intToExtStateTable;
    this.initialState = initialState;
    this.currentState = initialState;
    this.lineNumber = 1;
    this.columnNumber = 1;
  }

  /**
   * Constructs a generic parser that is an exact copy of the
   * one given. Note that here too, data structures that do not
   * change are shallow-copied (parser state table and state mappings).
   *
   * @param aGenericParser the {@code GenericParser} to copy
   */
  protected GenericParser(GenericParser aGenericParser) {
    parserStateTable = aGenericParser.parserStateTable;
    intToExtStateTable = aGenericParser.intToExtStateTable;
    initialState = aGenericParser.initialState;
    currentState = aGenericParser.currentState;
    lineNumber = aGenericParser.lineNumber;
    columnNumber = aGenericParser.columnNumber;
  }

  /**
   * Tell the parser to process the provided {@code String}. This is just a
   * convenience method that wraps over {@link Parser#parse(char)}.
   * @param input the {@code String} to parse
   * @throws ParseException if an unrecoverable error occurred during parsing
   */
  @Override
  public void parse(String input) throws ParseException {
    for (int i = 0; i < input.length(); i++)
      parse(input.charAt(i));
  }

  /**
   * Main loop for parsing of input.
   *
   * <p>Absent any callbacks defined, this function simply determines the
   * next state to switch to based on the <code>ParserStateTable</code> which is
   * derived from a state-machine configuration file in the original C++ parser.
   *
   * <p>However some states have specific callbacks defined which when
   * receiving specific characters may decide to overwrite the next state to
   * go to. Hence the next state is a function both of the main state table
   * in {@code ParserStateTable} as well as specific run-time information
   * from the callback functions.
   *
   * <p>Also note that the callbacks are called in a proper sequence,
   * first the exit-state one then the enter-state one and finally the
   * in-state one. Changing the order may result in a functional change.
   *
   * @param input the input character to parse (process)
   * @throws ParseException if an unrecoverable error occurred during parsing
   */
  @Override
  public void parse(char input) throws ParseException {
    InternalState nextState =
        parserStateTable.getNextState(currentState, input);

    if (nextState == InternalState.INTERNAL_ERROR_STATE) {
        String errorMsg =
            String.format("Unexpected character '%s' in int_state '%s' " +
                          "(ext_state '%s')",
                          HtmlUtils.encodeCharForAscii(input),
                          currentState.getName(), getState().getName());
      currentState = InternalState.INTERNAL_ERROR_STATE;
      throw new ParseException(this, errorMsg);
    }

    if (currentState != nextState) {
      nextState = handleExitState(currentState, nextState, input);
    }
    if (currentState != nextState) {
      nextState = handleEnterState(nextState, nextState, input);
    }
    nextState = handleInState(nextState, input);
    currentState = nextState;
    record(input);

    columnNumber++;
    if (input == '\n') {
      lineNumber++;
      columnNumber = 1;
    }
  }

  /**
   * Return the current state of the parser.
   */
  @Override
  public ExternalState getState() {
    if (!intToExtStateTable.containsKey(currentState)) {
      throw new NullPointerException("Did not find external state mapping " +
                                     "For internal state: " + currentState);
    }
    return intToExtStateTable.get(currentState);
  }

  /**
   * Reset the parser back to its initial default state.
   */
  @Override
  public void reset() {
    currentState = initialState;
    lineNumber = 1;
    columnNumber = 1;
  }

  /**
   * Sets the current line number which is returned during error messages.
   */
  @Override
  public void setLineNumber(int lineNumber) {
    this.lineNumber = lineNumber;
  }

  /**
   * Returns the current line number.
   */
  @Override
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * Sets the current column number which is returned during error messages.
   */
  @Override
  public void setColumnNumber(int columnNumber) {
    this.columnNumber = columnNumber;
  }

  /**
   * Returns the current column number.
   */
  @Override
  public int getColumnNumber() {
    return columnNumber;
  }

  InternalState getCurrentInternalState() {
    return currentState;
  }

  protected void setNextState(InternalState nextState) throws ParseException {
    Preconditions.checkNotNull(nextState);   // Developer error if it triggers.

    /* We are not actually parsing hence providing
     * a null char to the event handlers.
     */
    // TODO: Complicated logic to follow in C++ but clean it up.
    final char nullChar = '\0';

    if (currentState != nextState) {
      nextState = handleExitState(currentState, nextState, nullChar);
    }
    if (currentState != nextState) {
      handleEnterState(nextState, nextState, nullChar);
    }
    currentState = nextState;
  }

  /**
   * Invoked when the parser enters a new state.
   *
   * @param currentState the current state of the parser
   * @param expectedNextState the next state according to the
   *        state table definition
   * @param input the last character parsed
   * @return the state to change to, could be the same as the
   *         {@code expectedNextState} provided
   * @throws ParseException if an unrecoverable error occurred during parsing
   */
  protected InternalState handleEnterState(InternalState currentState,
                                           InternalState expectedNextState,
                                           char input) throws ParseException {
    return expectedNextState;
  }

  /**
   * Invoked when the parser exits a state.
   *
   * @param currentState the current state of the parser
   * @param expectedNextState the next state according to the
   *        state table definition
   * @param input the last character parsed
   * @return the state to change to, could be the same as the
   *         {@code expectedNextState} provided
   * @throws ParseException if an unrecoverable error occurred during parsing
   */
  protected InternalState handleExitState(InternalState currentState,
                                          InternalState expectedNextState,
                                          char input) throws ParseException {
    return expectedNextState;
  }

  /**
   * Invoked for each character read when no state change occured.
   *
   * @param currentState the current state of the parser
   * @param input the last character parsed
   * @return the state to change to, could be the same as the
   *         {@code expectedNextState} provided
   * @throws ParseException if an unrecoverable error occurred during parsing
   */
  protected InternalState handleInState(InternalState currentState,
                                        char input) throws ParseException {
    return currentState;
  }

  /**
   * Perform some processing on the given character. Derived classes
   * may override this method in order to perform additional logic
   * on every processed character beyond the logic defined in
   * state transitions.
   *
   * @param input the input character to operate on
   */
  protected void record(char input) { }
}
