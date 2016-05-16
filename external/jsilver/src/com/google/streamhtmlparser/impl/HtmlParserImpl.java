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
import com.google.common.collect.Maps;
import com.google.streamhtmlparser.ExternalState;
import com.google.streamhtmlparser.HtmlParser;
import com.google.streamhtmlparser.ParseException;
import com.google.streamhtmlparser.util.CharacterRecorder;
import com.google.streamhtmlparser.util.EntityResolver;
import com.google.streamhtmlparser.util.HtmlUtils;

import java.util.Map;

/**
 * A custom specialized parser - ported from the main C++ version - used to
 * implement context-aware escaping of run-time data in web-application
 * templates.
 *
 * <p>This is the main class in the package. It implements the
 * {@code HtmlParser} interface.
 *
 * <p>This class is not thread-safe, in particular you cannot invoke any
 * state changing operations (such as {@code parse} from multiple threads
 * on the same object.
 *
 * <p>If you are looking at this class, chances are very high you are
 * implementing Auto-Escaping for a new template system. Please see the
 * landing page including a design document at
 * <a href="http://go/autoescape">Auto-Escape Landing Page</a>.
 */
public class HtmlParserImpl extends GenericParser implements HtmlParser {

  /*
   * Internal representation of the parser state, which is at a
   * finer-granularity than the external state as given to callers.
   * The relationship between <code>InternalState</code> and
   * <code>ExternalState</code> is a many-to-one relationship.
   */
  private static final InternalState TEXT;
  private static final InternalState TAG_START;
  private static final InternalState TAG_NAME;
  private static final InternalState DECL_START;
  private static final InternalState DECL_BODY;
  private static final InternalState COM_OPEN;
  private static final InternalState COM_BODY;
  private static final InternalState COM_DASH;
  private static final InternalState COM_DASH_DASH;
  private static final InternalState PI;
  private static final InternalState PI_MAY_END;
  private static final InternalState TAG_SPACE;
  private static final InternalState TAG_CLOSE;
  private static final InternalState ATTR;
  private static final InternalState ATTR_SPACE;
  private static final InternalState VALUE;
  private static final InternalState VALUE_TEXT;
  private static final InternalState VALUE_Q_START;
  private static final InternalState VALUE_Q;
  private static final InternalState VALUE_DQ_START;
  private static final InternalState VALUE_DQ;
  private static final InternalState CDATA_COM_START;
  private static final InternalState CDATA_COM_START_DASH;
  private static final InternalState CDATA_COM_BODY;
  private static final InternalState CDATA_COM_DASH;
  private static final InternalState CDATA_COM_DASH_DASH;
  private static final InternalState CDATA_TEXT;
  private static final InternalState CDATA_LT;
  private static final InternalState CDATA_MAY_CLOSE;
  private static final InternalState JS_FILE;
  private static final InternalState CSS_FILE;

  static {
    TEXT = InternalState.getInstanceHtml("TEXT");
    TAG_START = InternalState.getInstanceHtml("TAG_START");
    TAG_NAME = InternalState.getInstanceHtml("TAG_NAME");
    DECL_START = InternalState.getInstanceHtml("DECL_START");
    DECL_BODY = InternalState.getInstanceHtml("DECL_BODY");
    COM_OPEN = InternalState.getInstanceHtml("COM_OPEN");
    COM_BODY = InternalState.getInstanceHtml("COM_BODY");
    COM_DASH = InternalState.getInstanceHtml("COM_DASH");
    COM_DASH_DASH = InternalState.getInstanceHtml("COM_DASH_DASH");
    PI =InternalState.getInstanceHtml("PI");
    PI_MAY_END = InternalState.getInstanceHtml("PI_MAY_END");
    TAG_SPACE = InternalState.getInstanceHtml("TAG_SPACE");
    TAG_CLOSE = InternalState.getInstanceHtml("TAG_CLOSE");
    ATTR = InternalState.getInstanceHtml("ATTR");
    ATTR_SPACE = InternalState.getInstanceHtml("ATTR_SPACE");
    VALUE = InternalState.getInstanceHtml("VALUE");
    VALUE_TEXT = InternalState.getInstanceHtml("VALUE_TEXT");
    VALUE_Q_START = InternalState.getInstanceHtml("VALUE_Q_START");
    VALUE_Q = InternalState.getInstanceHtml("VALUE_Q");
    VALUE_DQ_START = InternalState.getInstanceHtml("VALUE_DQ_START");
    VALUE_DQ = InternalState.getInstanceHtml("VALUE_DQ");
    CDATA_COM_START = InternalState.getInstanceHtml("CDATA_COM_START");
    CDATA_COM_START_DASH =
        InternalState.getInstanceHtml("CDATA_COM_START_DASH");
    CDATA_COM_BODY = InternalState.getInstanceHtml("CDATA_COM_BODY");
    CDATA_COM_DASH = InternalState.getInstanceHtml("CDATA_COM_DASH");
    CDATA_COM_DASH_DASH = InternalState.getInstanceHtml("CDATA_COM_DASH_DASH");
    CDATA_TEXT = InternalState.getInstanceHtml("CDATA_TEXT");
    CDATA_LT = InternalState.getInstanceHtml("CDATA_LT");
    CDATA_MAY_CLOSE = InternalState.getInstanceHtml("CDATA_MAY_CLOSE");
    JS_FILE = InternalState.getInstanceHtml("JS_FILE");
    CSS_FILE = InternalState.getInstanceHtml("CSS_FILE");
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

  private final CharacterRecorder tag;
  private final CharacterRecorder attr;
  private final CharacterRecorder value;
  private final CharacterRecorder cdataCloseTag;
  private final EntityResolver entityResolver;
  private final JavascriptParserImpl jsParser;
  private boolean insideJavascript;
  private int valueIndex;
  // True iff InsertText() was called at the start of a URL attribute value.
  private boolean textInsideUrlValue;

  /**
   * Creates an {@code HtmlParserImpl} object.
   *
   * <p>Both for performance reasons and to leverage code a state-flow machine
   * that is automatically generated from Python for multiple target
   * languages, this object uses a static {@code ParserStateTable} that
   * is read-only and obtained from the generated code in {@code HtmlParserFsm}.
   * That code also maintains the mapping from internal states
   * ({@code InternalState}) to external states ({@code ExternalState}).
   */
  public HtmlParserImpl() {
    super(STATE_TABLE, STATE_MAPPING, TEXT);
    tag = new CharacterRecorder();
    attr = new CharacterRecorder();
    value = new CharacterRecorder();
    cdataCloseTag = new CharacterRecorder();
    entityResolver = new EntityResolver();
    jsParser = new JavascriptParserImpl();
    insideJavascript = false;
    valueIndex = 0;
    textInsideUrlValue = false;
  }

  /**
   * Creates an {@code HtmlParserImpl} that is a copy of the one provided.
   * 
   * @param aHtmlParserImpl the {@code HtmlParserImpl} object to copy
   */
  public HtmlParserImpl(HtmlParserImpl aHtmlParserImpl) {
    super(aHtmlParserImpl);
    tag = new CharacterRecorder(aHtmlParserImpl.tag);
    attr = new CharacterRecorder(aHtmlParserImpl.attr);
    value = new CharacterRecorder(aHtmlParserImpl.value);
    cdataCloseTag = new CharacterRecorder(aHtmlParserImpl.cdataCloseTag);
    entityResolver = new EntityResolver(aHtmlParserImpl.entityResolver);
    jsParser = new JavascriptParserImpl(aHtmlParserImpl.jsParser);
    insideJavascript = aHtmlParserImpl.insideJavascript;
    valueIndex = aHtmlParserImpl.valueIndex;
    textInsideUrlValue = aHtmlParserImpl.textInsideUrlValue;
  }

  @Override
  public boolean inJavascript() {
    return (insideJavascript
            && ( (getState() == STATE_VALUE)
                 || (currentState == CDATA_TEXT)
                 || (currentState == CDATA_COM_START)
                 || (currentState == CDATA_COM_START_DASH)
                 || (currentState == CDATA_COM_BODY)
                 || (currentState == CDATA_COM_DASH)
                 || (currentState == CDATA_COM_DASH_DASH)
                 || (currentState == CDATA_LT)
                 || (currentState == CDATA_MAY_CLOSE)
                 || (currentState == JS_FILE) ));
  }

  @Override
  public boolean isJavascriptQuoted() {
    if (inJavascript()) {
      ExternalState jsParserState = jsParser.getState();
      return (jsParserState == JavascriptParserImpl.STATE_Q
              || jsParserState == JavascriptParserImpl.STATE_DQ);
    }
    return false;
  }

  @Override
  public boolean inAttribute() {
    ExternalState extState = getState();
    return (extState != null && (extState == STATE_ATTR
                                 || extState == STATE_VALUE));
  }

  /**
   * Returns {@code true} if and only if the parser is currently within
   * a CSS context. A CSS context is one of the below:
   * <ul>
   * <li>Inside a STYLE tag.
   * <li>Inside a STYLE attribute.
   * <li>Inside a CSS file when the parser was reset in the CSS mode.
   * </ul>
   *
   * @return {@code true} if and only if the parser is inside CSS
   */
  @Override
  public boolean inCss() {
    return (currentState == CSS_FILE
            || (getState() == STATE_VALUE
                && (getAttributeType() == ATTR_TYPE.STYLE))
            || ("style".equals(getTag())));
  }

  @Override
  public ATTR_TYPE getAttributeType() {
    String attribute = getAttribute();
    if (!inAttribute()) {
      return ATTR_TYPE.NONE;
    }
    if (HtmlUtils.isAttributeJavascript(attribute)) {
      return ATTR_TYPE.JS;
    }
    if (HtmlUtils.isAttributeUri(attribute)) {
      return ATTR_TYPE.URI;
    }
    if (HtmlUtils.isAttributeStyle(attribute)) {
      return ATTR_TYPE.STYLE;
    }

    // Special logic to handle the "content" attribute of the "meta" tag.
    if ("meta".equals(getTag()) && "content".equals(getAttribute())) {
      HtmlUtils.META_REDIRECT_TYPE redirectType =
          HtmlUtils.parseContentAttributeForUrl(getValue());
      if (redirectType == HtmlUtils.META_REDIRECT_TYPE.URL_START ||
          redirectType == HtmlUtils.META_REDIRECT_TYPE.URL)
        return ATTR_TYPE.URI;
    }
    
    return ATTR_TYPE.REGULAR;
  }

  @Override
  public ExternalState getJavascriptState() {
    return jsParser.getState();
  }

  @Override
  public boolean isAttributeQuoted() {
    return (currentState == VALUE_Q_START
            || currentState == VALUE_Q
            || currentState == VALUE_DQ_START
            || currentState == VALUE_DQ);
  }

  @Override
  public String getTag() {
    return tag.getContent().toLowerCase();
  }

  @Override
  public String getAttribute() {
    return inAttribute() ? attr.getContent().toLowerCase() : "";
  }

  @Override
  public String getValue() {
    return (getState() == STATE_VALUE) ? value.getContent() : "";
  }

  @Override
  public int getValueIndex() {
    if (getState() != STATE_VALUE) {
      return 0;
    }
    return valueIndex;
  }

  @Override
  public boolean isUrlStart() {
    // False when not inside an HTML attribute value
    if (getState() != STATE_VALUE) {
      return false;
    }

    //  Or when the HTML attribute is not of URI type.
    if (getAttributeType() != ATTR_TYPE.URI) {
      return false;
    }

    // Or when we received an InsertText() directive at the start of a URL.
    if (textInsideUrlValue) {
      return false;
    }

    if ("meta".equals(getTag())) {
      // At this point, we know we are in the "content" attribute
      // or we would not have the URI attribute type.
      return (HtmlUtils.parseContentAttributeForUrl(getValue()) ==
              HtmlUtils.META_REDIRECT_TYPE.URL_START);
    }

    // For all other URI attributes, check if we are at index 0.
    return (getValueIndex() == 0);
}

  /**
   * {@inheritDoc}
   * 
   * Resets the state of the parser to a state consistent with the
   * {@code Mode} provided. This will reset finer-grained state
   * information back to a default value, hence use only when
   * you want to parse text from a very clean slate.
   *
   * <p>See the {@link HtmlParser.Mode} enum for information on all
   * the valid modes.
   *
   * @param mode is an enum representing the high-level state of the parser
   */
  @Override
  public void resetMode(Mode mode) {
    insideJavascript = false;
    tag.reset();
    attr.reset();
    value.reset();
    cdataCloseTag.reset();
    valueIndex = 0;
    textInsideUrlValue = false;
    jsParser.reset();

    switch (mode) {
      case HTML:
        currentState = TEXT;
        break;
      case JS:
        currentState = JS_FILE;
        insideJavascript = true;
        break;
      case CSS:
        currentState = CSS_FILE;
        break;
      case HTML_IN_TAG:
        currentState = TAG_SPACE;
        break;
      default:
        throw new IllegalArgumentException("Did not recognize Mode: " +
                                           mode.toString());
    }
  }

  /**
   * Resets the state of the parser to the initial state of parsing HTML.
   */
  public void reset() {
    super.reset();
    resetMode(Mode.HTML);
  }

  /**
   * A specialized directive to tell the parser there is some content
   * that will be inserted here but that it will not get to parse. Used
   * by the template system that may not be able to give some content
   * to the parser but wants it to know there typically will be content
   * inserted at that point.  This is a hint used in corner cases within
   * parsing of HTML attribute names and values where content we do not
   * get to see could affect our parsing and alter our current state.
   *
   * <p>The two cases where {@code #insertText()} affects our parsing are:
   * <ul>
   * <li>We are at the start of the value of a URL-accepting HTML attribute. In
   * that case, we change internal state to no longer be considered at the
   * start of the URL. This may affect what escaping template systems may want
   * to perform on the HTML attribute value. We avoid injecting fake data and
   * hence not modify the current index of the value as determined by
   * {@link #getValueIndex()}</li>
   * <li>We just transitioned from an attribute name to an attribute value
   * (by parsing the separating {@code '='} character). In that case, we
   * change internal state to be now inside a non-quoted HTML attribute
   * value.</li>
   * </ul>
   * 
   * @throws ParseException if an unrecoverable error occurred during parsing
   */
  @Override
  public void insertText() throws ParseException {
    // Case: Inside URL attribute value.
    if (getState() == STATE_VALUE
        && getAttributeType() == ATTR_TYPE.URI
        && isUrlStart()) {
      textInsideUrlValue = true;
    }
    // Case: Before parsing any attribute value.
    if (currentState == VALUE) {
      setNextState(VALUE_TEXT);
    }
  }

  @Override
  protected InternalState handleEnterState(InternalState currentState,
                                           InternalState expectedNextState,
                                           char input) {
    InternalState nextState = expectedNextState;
    if (currentState == TAG_NAME) {
      enterTagName();
    } else if (currentState == ATTR) {
      enterAttribute();
    } else if (currentState == TAG_CLOSE) {
      nextState = tagClose(currentState);
    } else if (currentState == CDATA_MAY_CLOSE) {
      enterStateCdataMayClose();
    } else if (currentState == VALUE) {
      enterValue();
    } else
    if (currentState == VALUE_TEXT || currentState == VALUE_Q
        || currentState == VALUE_DQ) {
      enterValueContent();
    }
    return nextState;
  }

  @Override
  protected InternalState handleExitState(InternalState currentState,
                                          InternalState expectedNextState,
                                          char input) {
    InternalState nextState = expectedNextState;
    if (currentState == TAG_NAME) {
      exitTagName();
    } else if (currentState == ATTR) {
      exitAttribute();
    } else if (currentState == CDATA_MAY_CLOSE) {
      nextState = exitStateCdataMayClose(nextState, input);
    } else
    if ((currentState == VALUE_TEXT) || (currentState == VALUE_Q)
        || (currentState == VALUE_DQ)) {
      exitValueContent();
    }
    return nextState;
  }

  @Override
  protected InternalState handleInState(InternalState currentState,
                                        char input) throws ParseException {
    if ((currentState == CDATA_TEXT)
        || (currentState == CDATA_COM_START)
        || (currentState == CDATA_COM_START_DASH)
        || (currentState == CDATA_COM_BODY)
        || (currentState == CDATA_COM_DASH)
        || (currentState == CDATA_COM_DASH_DASH)
        || (currentState == CDATA_LT)
        || (currentState == CDATA_MAY_CLOSE)
        || (currentState == JS_FILE)) {
      inStateCdata(input);
    } else if ((currentState == VALUE_TEXT)
               || (currentState == VALUE_Q)
               || (currentState == VALUE_DQ)) {
      inStateValue(input);
    }
    return currentState;
  }

  /**
   * Invokes recording on all CharacterRecorder objects. Currently we do
   * not check that one and only one of them is recording. I did a fair
   * bit of testing on the C++ parser and was not convinced there is
   * such a guarantee.
   */
  @Override
  protected void record(char input) {
    attr.maybeRecord(input);
    tag.maybeRecord(input);
    value.maybeRecord(input);
    cdataCloseTag.maybeRecord(input);
  }

  /**
   * Starts recording the name of the HTML tag. Called when the parser
   * enters a new tag.
   */
  private void enterTagName() {
    tag.startRecording();
  }

  private void exitTagName() {
    tag.stopRecording();
    String tagString = tag.getContent();
    if (!tagString.isEmpty() && tagString.charAt(0) == '/') {
      tag.reset();
    }
  }

  /**
   * Starts recording the name of the HTML attribute. Called when the parser
   * enters a new HTML attribute.
   */
  private void enterAttribute() {
    attr.startRecording();
  }

  private void exitAttribute() {
    attr.stopRecording();
  }

  /**
   * Tracks the index within the HTML attribute value and initializes
   * the javascript parser for attributes that take javascript.
   *
   * Called when the parser enters a new HTML attribute value.
   */
  private void enterValue() {
    valueIndex = 0;
    textInsideUrlValue = false;
    if (HtmlUtils.isAttributeJavascript(getAttribute())) {
      entityResolver.reset();
      jsParser.reset();
      insideJavascript = true;
    } else {
      insideJavascript = false;
    }
  }

  /**
   * Starts recordning the contents of the attribute value.
   *
   * Called when entering an attribute value.
   */
  private void enterValueContent() {
    value.startRecording();
  }

  /**
   * Stops the recording of the attribute value and exits javascript
   * (in case we were inside it).
   */
  private void exitValueContent() {
    value.stopRecording();
    insideJavascript = false;
  }

  /**
   * Processes javascript after performing entity resolution and updates
   * the position within the attribute value.
   * If the status of the entity resolution is <code>IN_PROGRESS</code>,
   * we don't invoke the javascript parser.
   *
   * <p>Called for every character inside an attribute value.
   *
   * @param input character read
   * @throws ParseException if an unrecoverable error occurred during parsing
   */
  private void inStateValue(char input) throws ParseException {
    valueIndex++;
    if (insideJavascript) {
      EntityResolver.Status status = entityResolver.processChar(input);
      if (status == EntityResolver.Status.COMPLETED) {
        jsParser.parse(entityResolver.getEntity());
        entityResolver.reset();
      } else if (status == EntityResolver.Status.NOT_STARTED) {
        jsParser.parse(input);
      }
    }
  }

  /**
   * Handles the tag it finished reading.
   *
   * <p>For a script tag, it initializes the javascript parser. For all
   * tags that are recognized to have CDATA values
   * (including the script tag), it switches the CDATA state to handle them
   * properly. For code simplification, CDATA and RCDATA sections are
   * treated the same.
   *
   * <p>Called when the parser leaves a tag definition.
   *
   * @param state current state
   * @return state next state, could be the same as current state
   */
  private InternalState tagClose(InternalState state) {
    InternalState nextState = state;
    String tagName = getTag();
    if ("script".equals(tagName)) {
      nextState = CDATA_TEXT;
      jsParser.reset();
      insideJavascript = true;
    } else if ("style".equals(tagName)
                 || "title".equals(tagName)
                 || "textarea".equals(tagName)) {
      nextState = CDATA_TEXT;
      insideJavascript = false;
    }
    return nextState;
  }

  /**
   * Feeds the character to the javascript parser for processing.
   *
   * <p>Called inside CDATA blocks to parse javascript.
   *
   * @param input character read
   * @throws ParseException if an unrecoverable error occurred during parsing
   */
  private void inStateCdata(char input) throws ParseException {
    if (insideJavascript) {
      jsParser.parse(input);
    }
  }

  /**
   * Starts recording. This is so we find the closing tag name in order to
   * know if the tag is going to be closed or not.
   *
   * <p>Called when encountering a '<' character in a CDATA section.
   */
  private void enterStateCdataMayClose() {
    cdataCloseTag.startRecording();
  }

  /**
   * Determines whether to close the tag element, It closes it if it finds
   * the corresponding end tag. Called when reading what could be a
   * closing CDATA tag.
   *
   * @param input the character read
   * @param expectedNextState the expected state to go to next
   *        unless we want to change it here
   * @return the next state to go to
   */
  private InternalState exitStateCdataMayClose(
      InternalState expectedNextState,
      char input) {
    InternalState nextState = expectedNextState;
    cdataCloseTag.stopRecording();
    String cdataCloseTagString = cdataCloseTag.getContent();
    Preconditions.checkState(!cdataCloseTagString.isEmpty()
        && cdataCloseTagString.charAt(0) == '/');  // Developer error.

    if (cdataCloseTagString.substring(1).equalsIgnoreCase(getTag())
        && (input == '>' || HtmlUtils.isHtmlSpace(input))) {
      tag.clear();
      insideJavascript = false;
    } else {
      nextState = CDATA_TEXT;
    }
    return nextState;
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
    registerMapping(InternalState.INTERNAL_ERROR_STATE, HtmlParser.STATE_ERROR);

    registerMapping(TEXT, HtmlParser.STATE_TEXT);
    registerMapping(TAG_START, HtmlParser.STATE_TAG);
    registerMapping(TAG_NAME, HtmlParser.STATE_TAG);
    registerMapping(DECL_START, HtmlParser.STATE_TEXT);
    registerMapping(DECL_BODY, HtmlParser.STATE_TEXT);
    registerMapping(COM_OPEN, HtmlParser.STATE_TEXT);
    registerMapping(COM_BODY, HtmlParser.STATE_COMMENT);
    registerMapping(COM_DASH, HtmlParser.STATE_COMMENT);
    registerMapping(COM_DASH_DASH, HtmlParser.STATE_COMMENT);
    registerMapping(PI, HtmlParser.STATE_TEXT);
    registerMapping(PI_MAY_END, HtmlParser.STATE_TEXT);
    registerMapping(TAG_SPACE, HtmlParser.STATE_TAG);
    registerMapping(TAG_CLOSE, HtmlParser.STATE_TEXT);
    registerMapping(ATTR, HtmlParser.STATE_ATTR);
    registerMapping(ATTR_SPACE, HtmlParser.STATE_ATTR);
    registerMapping(VALUE, HtmlParser.STATE_VALUE);
    registerMapping(VALUE_TEXT, HtmlParser.STATE_VALUE);
    registerMapping(VALUE_Q_START, HtmlParser.STATE_VALUE);
    registerMapping(VALUE_Q, HtmlParser.STATE_VALUE);
    registerMapping(VALUE_DQ_START, HtmlParser.STATE_VALUE);
    registerMapping(VALUE_DQ, HtmlParser.STATE_VALUE);
    registerMapping(CDATA_COM_START, HtmlParser.STATE_TEXT);
    registerMapping(CDATA_COM_START_DASH, HtmlParser.STATE_TEXT);
    registerMapping(CDATA_COM_BODY, HtmlParser.STATE_TEXT);
    registerMapping(CDATA_COM_DASH, HtmlParser.STATE_TEXT);
    registerMapping(CDATA_COM_DASH_DASH, HtmlParser.STATE_TEXT);
    registerMapping(CDATA_TEXT, HtmlParser.STATE_TEXT);
    registerMapping(CDATA_LT, HtmlParser.STATE_TEXT);
    registerMapping(CDATA_MAY_CLOSE, HtmlParser.STATE_TEXT);
    registerMapping(JS_FILE, HtmlParser.STATE_JS_FILE);
    registerMapping(CSS_FILE, HtmlParser.STATE_CSS_FILE);
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

  // NOTE: The "[:default:]" transition should be registered before any
  //   other transitions for a given state or it will over-write them.
  private static void initializeParserStateTable() {
    registerTransition("[:default:]", CSS_FILE, CSS_FILE);
    registerTransition("[:default:]", JS_FILE, JS_FILE);
    registerTransition("[:default:]", CDATA_MAY_CLOSE, CDATA_TEXT);
    registerTransition(" \t\n\r", CDATA_MAY_CLOSE, TAG_SPACE);
    registerTransition(">", CDATA_MAY_CLOSE, TEXT);
    registerTransition("A-Za-z0-9/_:-", CDATA_MAY_CLOSE, CDATA_MAY_CLOSE);
    registerTransition("[:default:]", CDATA_LT, CDATA_TEXT);
    registerTransition("!", CDATA_LT, CDATA_COM_START);
    registerTransition("/", CDATA_LT, CDATA_MAY_CLOSE);
    registerTransition("[:default:]", CDATA_TEXT, CDATA_TEXT);
    registerTransition("<", CDATA_TEXT, CDATA_LT);
    registerTransition("[:default:]", CDATA_COM_DASH_DASH, CDATA_COM_BODY);
    registerTransition(">", CDATA_COM_DASH_DASH, CDATA_TEXT);
    registerTransition("-", CDATA_COM_DASH_DASH, CDATA_COM_DASH_DASH);
    registerTransition("[:default:]", CDATA_COM_DASH, CDATA_COM_BODY);
    registerTransition("-", CDATA_COM_DASH, CDATA_COM_DASH_DASH);
    registerTransition("[:default:]", CDATA_COM_BODY, CDATA_COM_BODY);
    registerTransition("-", CDATA_COM_BODY, CDATA_COM_DASH);
    registerTransition("[:default:]", CDATA_COM_START_DASH, CDATA_TEXT);
    registerTransition("-", CDATA_COM_START_DASH, CDATA_COM_BODY);
    registerTransition("[:default:]", CDATA_COM_START, CDATA_TEXT);
    registerTransition("-", CDATA_COM_START, CDATA_COM_START_DASH);
    registerTransition("[:default:]", VALUE_DQ, VALUE_DQ);
    registerTransition("\"", VALUE_DQ, TAG_SPACE);
    registerTransition("[:default:]", VALUE_DQ_START, VALUE_DQ);
    registerTransition("\"", VALUE_DQ_START, TAG_SPACE);
    registerTransition("[:default:]", VALUE_Q, VALUE_Q);
    registerTransition("\'", VALUE_Q, TAG_SPACE);
    registerTransition("[:default:]", VALUE_Q_START, VALUE_Q);
    registerTransition("\'", VALUE_Q_START, TAG_SPACE);
    registerTransition("[:default:]", VALUE_TEXT, VALUE_TEXT);
    registerTransition(" \t\n\r", VALUE_TEXT, TAG_SPACE);
    registerTransition(">", VALUE_TEXT, TAG_CLOSE);
    registerTransition("[:default:]", VALUE, VALUE_TEXT);
    registerTransition(">", VALUE, TAG_CLOSE);
    registerTransition(" \t\n\r", VALUE, VALUE);
    registerTransition("\"", VALUE, VALUE_DQ_START);
    registerTransition("\'", VALUE, VALUE_Q_START);
    registerTransition("=", ATTR_SPACE, VALUE);
    registerTransition("/", ATTR_SPACE, TAG_SPACE);
    registerTransition("A-Za-z0-9_:-", ATTR_SPACE, ATTR);
    registerTransition(" \t\n\r", ATTR_SPACE, ATTR_SPACE);
    registerTransition(">", ATTR_SPACE, TAG_CLOSE);
    registerTransition(" \t\n\r", ATTR, ATTR_SPACE);
    registerTransition("=", ATTR, VALUE);
    registerTransition("/", ATTR, TAG_SPACE);
    registerTransition(">", ATTR, TAG_CLOSE);
    registerTransition("A-Za-z0-9_:.-", ATTR, ATTR);
    registerTransition("[:default:]", TAG_CLOSE, TEXT);
    registerTransition("<", TAG_CLOSE, TAG_START);
    registerTransition("/", TAG_SPACE, TAG_SPACE);
    registerTransition("A-Za-z0-9_:-", TAG_SPACE, ATTR);
    registerTransition(" \t\n\r", TAG_SPACE, TAG_SPACE);
    registerTransition(">", TAG_SPACE, TAG_CLOSE);
    registerTransition("[:default:]", PI_MAY_END, PI);
    registerTransition(">", PI_MAY_END, TEXT);
    registerTransition("[:default:]", PI, PI);
    registerTransition("?", PI, PI_MAY_END);
    registerTransition("[:default:]", COM_DASH_DASH, COM_BODY);
    registerTransition(">", COM_DASH_DASH, TEXT);
    registerTransition("-", COM_DASH_DASH, COM_DASH_DASH);
    registerTransition("[:default:]", COM_DASH, COM_BODY);
    registerTransition("-", COM_DASH, COM_DASH_DASH);
    registerTransition("[:default:]", COM_BODY, COM_BODY);
    registerTransition("-", COM_BODY, COM_DASH);
    registerTransition("[:default:]", COM_OPEN, TEXT);
    registerTransition("-", COM_OPEN, COM_BODY);
    registerTransition("[:default:]", DECL_BODY, DECL_BODY);
    registerTransition(">", DECL_BODY, TEXT);
    registerTransition("[:default:]", DECL_START, DECL_BODY);
    registerTransition(">", DECL_START, TEXT);
    registerTransition("-", DECL_START, COM_OPEN);
    registerTransition(">", TAG_NAME, TAG_CLOSE);
    registerTransition(" \t\n\r", TAG_NAME, TAG_SPACE);
    registerTransition("A-Za-z0-9/_:-", TAG_NAME, TAG_NAME);

    // Manual change to remain in-sync with CL 10597850 in C HtmlParser.
    registerTransition("[:default:]", TAG_START, TEXT);
    registerTransition("<", TAG_START, TAG_START);
    // End of manual change.

    registerTransition("!", TAG_START, DECL_START);
    registerTransition("?", TAG_START, PI);
    registerTransition("A-Za-z0-9/_:-", TAG_START, TAG_NAME);
    registerTransition("[:default:]", TEXT, TEXT);
    registerTransition("<", TEXT, TAG_START);
  }
}
