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
 * Methods exposed for HTML parsing of text to facilitate implementation
 * of Automatic context-aware escaping. The HTML parser also embeds a
 * Javascript parser for processing Javascript fragments. In the future,
 * it will also embed other specific parsers and hence most likely remain
 * the main interface to callers of this package.
 *
 * <p>Note: These are the exact methods exposed in the original C++ Parser. The
 * names are simply modified to conform to Java.
 */
public interface HtmlParser extends Parser {

  /**
   * The Parser Mode requested for parsing a given template.
   * Currently we support:
   * <ul>
   * <li>{@code HTML} for HTML templates.
   * <li>{@code JS} for javascript templates.
   * <li>{@code CSS} for Cascading Style-Sheets templates.
   * <li>{@code HTML_IN_TAG} for HTML templates that consist only of
   *     HTML attribute name and value pairs. This is typically the case for
   *     a template that is being included from a parent template where the
   *     parent template contains the start and the closing of the HTML tag.
   *     This is a special mode, for standard HTML templates please use
   *     {@link #HTML}.
   *     An example of such as template is:
   *     <p><code>class="someClass" target="_blank"</code></p>
   *     <p>Which could be included from a parent template that contains
   *     an anchor tag, say:</p>
   *     <p><code>&lt;a href="/bla" ["INCLUDED_TEMPLATE"]&gt;</code></p>
   * </ul>
   */
  public enum Mode {
    HTML,
    JS,
    CSS,
    HTML_IN_TAG
  }

  /**
   * Indicates the type of HTML attribute that the parser is currently in or
   * {@code NONE} if the parser is not currently in an attribute.
   * {@code URI} is for attributes taking a URI such as "href" and "src".
   * {@code JS} is for attributes taking javascript such as "onclick".
   * {@code STYLE} is for the "style" attribute.
   * All other attributes fall under {@code REGULAR}.
   *
   * Returned by {@link HtmlParser#getAttributeType()}
   */
  public enum ATTR_TYPE {
    NONE,
    REGULAR,
    URI,
    JS,
    STYLE
  }

  /**
   * All the states in which the parser can be. These are external states.
   * The parser has many more internal states that are not exposed and which
   * are instead mapped to one of these external ones.
   * {@code STATE_TEXT} the parser is in HTML proper.
   * {@code STATE_TAG} the parser is inside an HTML tag name.
   * {@code STATE_COMMENT} the parser is inside an HTML comment.
   * {@code STATE_ATTR} the parser is inside an HTML attribute name.
   * {@code STATE_VALUE} the parser is inside an HTML attribute value.
   * {@code STATE_JS_FILE} the parser is inside javascript code.
   * {@code STATE_CSS_FILE} the parser is inside CSS code.
   *
   * <p>All these states map exactly to those exposed in the C++ (original)
   * version of the HtmlParser.
   */
  public final static ExternalState STATE_TEXT =
      new ExternalState("STATE_TEXT");
  public final static ExternalState STATE_TAG =
      new ExternalState("STATE_TAG");
  public final static ExternalState STATE_COMMENT =
      new ExternalState("STATE_COMMENT");
  public final static ExternalState STATE_ATTR =
      new ExternalState("STATE_ATTR");
  public final static ExternalState STATE_VALUE =
      new ExternalState("STATE_VALUE");
  public final static ExternalState STATE_JS_FILE =
      new ExternalState("STATE_JS_FILE");
  public final static ExternalState STATE_CSS_FILE =
      new ExternalState("STATE_CSS_FILE");

  /**
   * Returns {@code true} if the parser is currently processing Javascript.
   * Such is the case if and only if, the parser is processing an attribute
   * that takes Javascript, a Javascript script block or the parser
   * is (re)set with {@link Mode#JS}.
   *
   * @return {@code true} if the parser is processing Javascript,
   *         {@code false} otherwise
   */
  public boolean inJavascript();

  /**
   * Returns {@code true} if the parser is currently processing
   * a Javascript litteral that is quoted. The caller will typically
   * invoke this method after determining that the parser is processing
   * Javascript. Knowing whether the element is quoted or not helps
   * determine which escaping to apply to it when needed.
   *
   * @return {@code true} if and only if the parser is inside a quoted
   *         Javascript literal
   */
  public boolean isJavascriptQuoted();


  /**
   * Returns {@code true} if and only if the parser is currently within
   * an attribute, be it within the attribute name or the attribute value.
   *
   * @return {@code true} if and only if inside an attribute
   */
  public boolean inAttribute();

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
  public boolean inCss();

  /**
   * Returns the type of the attribute that the parser is in
   * or {@code ATTR_TYPE.NONE} if we are not parsing an attribute.
   * The caller will typically invoke this method after determining
   * that the parser is processing an attribute.
   *
   * <p>This is useful to determine which escaping to apply based
   * on the type of value this attribute expects.
   *
   * @return type of the attribute
   * @see HtmlParser.ATTR_TYPE
   */
  public ATTR_TYPE getAttributeType();

  /**
   * Returns {@code true} if and only if the parser is currently within
   * an attribute value and that attribute value is quoted.
   *
   * @return {@code true} if and only if the attribute value is quoted
   */
  public boolean isAttributeQuoted();


  /**
   * Returns the name of the HTML tag if the parser is currently within one.
   * Note that the name may be incomplete if the parser is currently still
   * parsing the name. Returns an empty {@code String} if the parser is not
   * in a tag as determined by {@code getCurrentExternalState}.
   *
   * @return the name of the HTML tag or an empty {@code String} if we are
   *         not within an HTML tag
   */
  public String getTag();

  /**
   * Returns the name of the HTML attribute the parser is currently processing.
   * If the parser is still parsing the name, then the returned name
   * may be incomplete. Returns an empty {@code String} if the parser is not
   * in an attribute as determined by {@code getCurrentExternalState}.
   *
   * @return the name of the HTML attribute or an empty {@code String}
   *         if we are not within an HTML attribute
   */
  public String getAttribute();

  /**
   * Returns the value of an HTML attribute if the parser is currently
   * within one. If the parser is currently parsing the value, the returned
   * value may be incomplete. The caller will typically first determine
   * that the parser is processing a value by calling
   * {@code getCurrentExternalState}.
   *
   * @return the value, could be an empty {@code String} if the parser is not
   *         in an HTML attribute value
   */
  public String getValue();

  /**
   * Returns the current position of the parser within the HTML attribute
   * value, zero being the position of the first character in the value.
   * The caller will typically first determine that the parser is
   * processing a value by calling {@link #getState()}.
   *
   * @return the index or zero if the parser is not processing a value
   */
  public int getValueIndex();

  /**
   * Returns {@code true} if and only if the current position of the parser is
   * at the start of a URL HTML attribute value. This is the case when the
   * following three conditions are all met:
   * <p>
   * <ol>
   * <li>The parser is in an HTML attribute value.
   * <li>The HTML attribute expects a URL, as determined by
   *     {@link #getAttributeType()} returning {@code .ATTR_TYPE#URI}.
   * <li>The parser has not yet seen any characters from that URL.
   * </ol>
   * 
   * <p> This method may be used by an Html Sanitizer or an Auto-Escape system
   * to determine whether to validate the URL for well-formedness and validate
   * the scheme of the URL (e.g. {@code HTTP}, {@code HTTPS}) is safe.
   * In particular, it is recommended to use this method instead of
   * checking that {@link #getValueIndex()} is {@code 0} to support attribute
   * types where the URL does not start at index zero, such as the
   * {@code content} attribute of the {@code meta} HTML tag.
   *
   * @return {@code true} if and only if the parser is at the start of the URL
   */
  public boolean isUrlStart();

  /**
   * Resets the state of the parser, allowing for reuse of the
   * {@code HtmlParser} object.
   *
   * <p>See the {@link HtmlParser.Mode} enum for information on all
   * the valid modes.
   *
   * @param mode is an enum representing the high-level state of the parser
   */
  public void resetMode(HtmlParser.Mode mode);

  /**
   * A specialized directive to tell the parser there is some content
   * that will be inserted here but that it will not get to parse. Used
   * by the template system that may not be able to give some content
   * to the parser but wants it to know there typically will be content
   * inserted at that point. This is a hint used in corner cases within
   * parsing of HTML attribute names and values where content we do not
   * get to see could affect our parsing and alter our current state.
   *
   * <p>Returns {@code false} if and only if the parser encountered
   * a fatal error which prevents it from continuing further parsing.
   *
   * <p>Note: The return value is different from the C++ Parser which
   * always returns {@code true} but in my opinion makes more sense.
   *
   * @throws ParseException if an unrecoverable error occurred during parsing
   */
  public void insertText() throws ParseException;

  /**
   * Returns the state the Javascript parser is in.
   *
   * <p>See {@link JavascriptParser} for more information on the valid
   * external states. The caller will typically first determine that the
   * parser is processing Javascript and then invoke this method to
   * obtain more fine-grained state information.
   *
   * @return external state of the javascript parser
   */
  public ExternalState getJavascriptState();
}
