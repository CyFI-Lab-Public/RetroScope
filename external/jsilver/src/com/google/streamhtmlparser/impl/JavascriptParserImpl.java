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

import com.google.common.collect.Maps;
import com.google.streamhtmlparser.ExternalState;
import com.google.streamhtmlparser.JavascriptParser;
import com.google.streamhtmlparser.util.HtmlUtils;
import com.google.streamhtmlparser.util.JavascriptTokenBuffer;

import java.util.Map;

/**
 * <p>Many comments copied almost verbatim from the original C version.
 */
public class JavascriptParserImpl extends GenericParser
    implements JavascriptParser {

  final static InternalState JS_TEXT;
  final static InternalState JS_Q;
  final static InternalState JS_Q_E;
  final static InternalState JS_DQ;
  final static InternalState JS_DQ_E;
  final static InternalState JS_SLASH;
  final static InternalState JS_REGEXP_SLASH;
  final static InternalState JS_REGEXP;
  final static InternalState JS_REGEXP_BRK;
  final static InternalState JS_REGEXP_BRK_E;
  final static InternalState JS_REGEXP_E;
  final static InternalState JS_COM_LN;
  final static InternalState JS_COM_ML;
  final static InternalState JS_COM_ML_CLOSE;
  final static InternalState JS_COM_AFTER;

  static {
    JS_TEXT = InternalState.getInstanceJavascript("JS_TEXT");
    JS_Q  = InternalState.getInstanceJavascript("JS_Q");
    JS_Q_E = InternalState.getInstanceJavascript("JS_Q_E");
    JS_DQ = InternalState.getInstanceJavascript("JS_DQ");
    JS_DQ_E = InternalState.getInstanceJavascript("JS_DQ_E");
    JS_SLASH = InternalState.getInstanceJavascript("JS_SLASH");
    JS_REGEXP = InternalState.getInstanceJavascript("JS_REGEXP");
    JS_REGEXP_SLASH = InternalState.getInstanceJavascript("JS_REGEXP_SLASH");
    JS_REGEXP_E = InternalState.getInstanceJavascript("JS_REGEXP_E");
    JS_REGEXP_BRK = InternalState.getInstanceJavascript("JS_REGEXP_BRK");
    JS_REGEXP_BRK_E = InternalState.getInstanceJavascript("JS_REGEXP_BRK_E");
    JS_COM_LN = InternalState.getInstanceJavascript("COMMENT_LN");
    JS_COM_ML = InternalState.getInstanceJavascript("COMMENT_ML");
    JS_COM_ML_CLOSE = InternalState.getInstanceJavascript("COMMENT_ML_CLOSE");
    JS_COM_AFTER = InternalState.getInstanceJavascript("COMMENT_AFTER");
  }

  private static final Map<InternalState, ExternalState> STATE_MAPPING =
      Maps.newHashMap();
  static {
    initializeStateMapping();
  }

  private static final ParserStateTable STATE_TABLE = new ParserStateTable();
  static {
    initializeParserStateTable();
  }

  private final JavascriptTokenBuffer ccBuffer;

  /**
   * Creates a {@code JavascriptParserImpl} object.
   */
  public JavascriptParserImpl() {
    super(STATE_TABLE, STATE_MAPPING, JS_TEXT);
    ccBuffer = new JavascriptTokenBuffer();
  }

  /**
   * Creates a {@code JavascriptParserImpl} object that is a copy
   * of the one provided.
   *
   * @param aJavascriptParserImpl the {@code JavascriptParserImpl} to copy
   */
  public JavascriptParserImpl(JavascriptParserImpl aJavascriptParserImpl) {
    super(aJavascriptParserImpl);
    ccBuffer = new JavascriptTokenBuffer(aJavascriptParserImpl.ccBuffer);
  }

  @Override
  public void reset() {
    super.reset();
    currentState = JS_TEXT;
  }

  @Override
  protected InternalState handleEnterState(InternalState currentState,
                                           InternalState expectedNextState,
                                           char input) {
    InternalState nextState = expectedNextState;
    if (currentState == JS_SLASH) {
      nextState = enterStateJsSlash(currentState, input);
    } else if (currentState == JS_COM_AFTER) {
      enterStateJsCommentAfter();
    }
    return nextState;
  }

  @Override
  protected InternalState handleExitState(InternalState currentState,
                                          InternalState expectedNextState,
                                          char input) {
    // Nothing to do - no handlers for exit states
    return expectedNextState;
  }

  @Override
  protected InternalState handleInState(InternalState currentState,
                                        char input) {
    if (currentState == JS_TEXT) {
      inStateJsText(input);
    }
    return currentState;
  }

  /**
   * Called every time we find a slash ('/') character in the javascript
   * text (except for slashes that close comments or regexp literals).
   *
   * <p>Comment copied verbatim from the corresponding C-version.
   *
   * <p>Implements the logic to figure out if this slash character is a
   * division operator or if it opens a regular expression literal.
   * This is heavily inspired by the syntactic resynchronization
   * for javascript 2.0:
   *
   * <p>When we receive a '/', we look at the previous non space character
   * to figure out if it's the ending of a punctuator that can precede a
   * regexp literal, in which case we assume the current '/' is part of a
   * regular expression literal (or the opening of a javascript comment,
   * but that part is dealt with in the state machine). The exceptions to
   * this are unary operators, so we look back a second character to rule
   * out '++' and '--'.
   *
   * <p> Although it is not straightforward to figure out if the binary
   * operator is a postfix of the previous expression or a prefix of the
   * regular expression, we rule out the later as it is an uncommon practice.
   *
   * <p>If we ruled out the previous token to be a valid regexp preceding
   * punctuator, we extract the last identifier in the buffer and match
   * against a list of keywords that are known to precede expressions in
   * the grammar. If we get a match on any of these keywords, then we are
   * opening a regular expression, if not, then we have a division operator.
   *
   * <p>Known cases that are accepted by the grammar but we handle
   * differently, although I (falmeida) don't believe there is a
   * legitimate usage for those:
   *   Division of a regular expression: var result = /test/ / 5;
   *   Prefix unary increment of a regular expression: var result = ++/test/;
   *   Division of an object literal: { a: 1 } /x/.exec('x');
   *
   * @param state being entered to
   * @param input character being processed
   * @return state next state to go to, may be the same as the one we
   *     were called with
   *
   * <a>http://www.mozilla.org/js/language/js20-2000-07/rationale/syntax.html>
   * Syntactic Resynchronization</a>
   */
  private InternalState enterStateJsSlash(InternalState state, char input) {

    InternalState nextState = state;
    int position = -1;

    // Consume the last whitespace
    if (HtmlUtils.isJavascriptWhitespace(ccBuffer.getChar(position))) {
      --position;
    }

    switch (ccBuffer.getChar(position)) {
      // Ignore unary increment
      case '+':
        if (ccBuffer.getChar(position - 1) != '+') {
          nextState = JS_REGEXP_SLASH;
        }
        break;
      case '-':
        // Ignore unary decrement
        if (ccBuffer.getChar(position - 1) != '-') {
          nextState = JS_REGEXP_SLASH;
        }
        break;
        // List of punctuator endings except ), ], }, + and - *
      case '=':
      case '<':
      case '>':
      case '&':
      case '|':
      case '!':
      case '%':
      case '*':
      case '/':
      case ',':
      case ';':
      case '?':
      case ':':
      case '^':
      case '~':
      case '{':
      case '(':
      case '[':
      case '}':
      case '\0':
        nextState = JS_REGEXP_SLASH;
        break;
      default:
        String lastIdentifier = ccBuffer.getLastIdentifier();
        if (lastIdentifier != null && HtmlUtils
            .isJavascriptRegexpPrefix(lastIdentifier)) {
          nextState = JS_REGEXP_SLASH;
        }
    }
    ccBuffer.appendChar(input);
    return nextState;
  }

  /**
   * Called at the end of a javascript comment.
   *
   * <p>When we open a comment, the initial '/' was inserted into the ring
   * buffer, but it is not a token and should be considered whitespace
   * for parsing purposes.
   *
   * <p>When we first saw the '/' character, we didn't yet know if it was
   * the beginning of a comment, a division operator, or a regexp.
   *
   * <p>In this function we just replace the inital '/' with a whitespace
   * character, unless we had a preceding whitespace character, in which
   * case we just remove the '/'. This is needed to ensure all spaces in
   * the buffer are correctly folded.
   */
  private void enterStateJsCommentAfter() {
    if (HtmlUtils.isJavascriptWhitespace(ccBuffer.getChar(-2))) {
      ccBuffer.popChar();
    } else {
      ccBuffer.setChar(-1, ' ');
    }
  }

  private void inStateJsText(char input) {
    ccBuffer.appendChar(input);
  }

// ======================================================= //
// SECTION BELOW WILL ALL BE AUTO-GENERATED IN FUTURE.     //
// ======================================================= //

  private static void registerMapping(InternalState internalState,
                                      ExternalState externalState) {
    STATE_MAPPING.put(internalState, externalState);
  }

  private static void initializeStateMapping() {
    // Each parser implementation must map the error state appropriately.
    registerMapping(InternalState.INTERNAL_ERROR_STATE,
                    JavascriptParser.STATE_ERROR);

    registerMapping(JS_TEXT, JavascriptParser.STATE_TEXT);
    registerMapping(JS_Q, JavascriptParser.STATE_Q);
    registerMapping(JS_Q_E, JavascriptParser.STATE_Q);
    registerMapping(JS_DQ, JavascriptParser.STATE_DQ);
    registerMapping(JS_DQ_E, JavascriptParser.STATE_DQ);
    registerMapping(JS_SLASH, JavascriptParser.STATE_TEXT);
    registerMapping(JS_REGEXP_SLASH, JavascriptParser.STATE_TEXT);
    registerMapping(JS_REGEXP, JavascriptParser.STATE_REGEXP);
    registerMapping(JS_REGEXP_BRK,JavascriptParser.STATE_REGEXP);
    registerMapping(JS_REGEXP_BRK_E, JavascriptParser.STATE_REGEXP);
    registerMapping(JS_REGEXP_E,JavascriptParser.STATE_REGEXP);
    registerMapping(JS_COM_LN, JavascriptParser.STATE_COMMENT);
    registerMapping(JS_COM_ML, JavascriptParser.STATE_COMMENT);
    registerMapping(JS_COM_ML_CLOSE, JavascriptParser.STATE_COMMENT);
    registerMapping(JS_COM_AFTER, JavascriptParser.STATE_TEXT);
  }

  private static void registerTransition(String expression,
                                         InternalState source,
                                         InternalState to) {
    // It seems to silly to go through a StateTableTransition here
    // but it adds extra data checking.
    StateTableTransition stt = new StateTableTransition(expression,
                                                        source, to);
    STATE_TABLE.setExpression(stt.getExpression(), stt.getFrom(),
                              stt.getTo());
  }

  private static void initializeParserStateTable() {
    registerTransition("[:default:]", JS_COM_AFTER, JS_TEXT);
    registerTransition("/", JS_COM_AFTER, JS_SLASH);
    registerTransition("\"", JS_COM_AFTER, JS_DQ);
    registerTransition("\'", JS_COM_AFTER, JS_Q);
    registerTransition("[:default:]", JS_COM_ML_CLOSE, JS_COM_ML);
    registerTransition("/", JS_COM_ML_CLOSE,JS_COM_AFTER);
    registerTransition("[:default:]", JS_COM_ML, JS_COM_ML);
    registerTransition("*", JS_COM_ML, JS_COM_ML_CLOSE);
    registerTransition("[:default:]", JS_COM_LN,JS_COM_LN);
    registerTransition("\n", JS_COM_LN,JS_COM_AFTER);
    registerTransition("[:default:]", JS_REGEXP_E, JS_REGEXP);
    registerTransition("[:default:]", JS_REGEXP_BRK_E, JS_REGEXP_BRK);
    registerTransition("[:default:]", JS_REGEXP_BRK, JS_REGEXP_BRK);
    registerTransition("]", JS_REGEXP_BRK, JS_REGEXP);
    registerTransition("\\", JS_REGEXP_BRK, JS_REGEXP_BRK_E);
    registerTransition("[:default:]", JS_REGEXP, JS_REGEXP);
    registerTransition("/", JS_REGEXP, JS_TEXT);
    registerTransition("[", JS_REGEXP, JS_REGEXP_BRK);
    registerTransition("\\", JS_REGEXP, JS_REGEXP_E);
    registerTransition("[:default:]", JS_REGEXP_SLASH, JS_REGEXP);
    registerTransition("[", JS_REGEXP_SLASH, JS_REGEXP_BRK);
    registerTransition("\\", JS_REGEXP_SLASH, JS_REGEXP_E);
    registerTransition("*", JS_REGEXP_SLASH, JS_COM_ML);
    registerTransition("/", JS_REGEXP_SLASH, JS_COM_LN);
    registerTransition("[:default:]", JS_SLASH, JS_TEXT);
    registerTransition("*", JS_SLASH, JS_COM_ML);
    registerTransition("/", JS_SLASH, JS_COM_LN);
    registerTransition("[:default:]", JS_DQ_E,JS_DQ);
    registerTransition("[:default:]", JS_DQ,JS_DQ);
    registerTransition("\"", JS_DQ, JS_TEXT);
    registerTransition("\\", JS_DQ, JS_DQ_E);
    registerTransition("[:default:]", JS_Q_E,JS_Q);
    registerTransition("[:default:]", JS_Q,JS_Q);
    registerTransition("\'", JS_Q, JS_TEXT);
    registerTransition("\\", JS_Q, JS_Q_E);
    registerTransition("[:default:]", JS_TEXT, JS_TEXT);
    registerTransition("/", JS_TEXT, JS_SLASH);
    registerTransition("\"", JS_TEXT, JS_DQ);
    registerTransition("\'", JS_TEXT, JS_Q);
  }
}