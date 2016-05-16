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

package com.google.streamhtmlparser.util;

import com.google.common.collect.ImmutableSortedSet;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Utility functions for HTML and Javascript that are most likely
 * not interesting to users outside this package.
 *
 * <p>The <code>HtmlParser</code> will be open-sourced hence we took the
 * decision to keep these utilities in this package as well as not to
 * leverage others that may exist in the <code>google3</code> code base.
 *
 * <p>The functionality exposed is designed to be 100% compatible with
 * the corresponding logic in the C-version of the HtmlParser as such
 * we are particularly concerned with cross-language compatibility.
 *
 * <p>Note: The words {@code Javascript} and {@code ECMAScript} are used
 * interchangeably unless otherwise noted.
 */
public final class HtmlUtils {

  /**
   * static utility class
   */
  private HtmlUtils() {
  }  // COV_NF_LINE

  /**
   * Indicates the type of content contained in the {@code content} HTML
   * attribute of the {@code meta} HTML tag. Used by
   * {@link HtmlUtils#parseContentAttributeForUrl(String)}.
   * <p>The values are:
   * <ul>
   * <li>{@code NONE} if it does not contain a URL in the expected format.
   * <li>{@code URL_START} if it contains a URL but hasn't seen any of
   * its contents.
   * <li>{@code URL} if it contains a URL and has seen at least some of
   * its contents.
   * </ul>
   */
  public enum META_REDIRECT_TYPE {
    NONE,
    URL_START,
    URL
  }

  /**
   * A regular expression matching the format of a {@code content} attribute
   * that contains a URL. Used by {@link #parseContentAttributeForUrl}.
   */
  private static final String META_REDIRECT_REGEX =
      "^\\s*\\d*\\s*;\\s*URL\\s*=\\s*[\'\"]?";

  // Safe for use by concurrent threads so we compile once.
  private static final Pattern META_REDIRECT_PATTERN =
      Pattern.compile(META_REDIRECT_REGEX, Pattern.CASE_INSENSITIVE);

  /**
   * Set of keywords that can precede a regular expression literal. Taken from:
   * <a href="http://www.mozilla.org/js/language/js20-2000-07/rationale/syntax.html">
   * Language Syntax</a>
   *
   * <p>The token {@code void} was added to the list. Several keywords are
   * defined in Ecmascript 4 not Ecmascript 3. However, to keep the logic
   * simple we do not differentiate on the version and bundle them all together.
   */
  private static final Set<String> REGEXP_TOKEN_PREFIXS =
      ImmutableSortedSet.of(
          "abstract",
          "break",
          "case",
          "catch",
          "class",
          "const",
          "continue",
          "debugger",
          "default",
          "delete",
          "do",
          "else",
          "enum",
          "eval",
          "export",
          "extends",
          "field",
          "final",
          "finally",
          "for",
          "function",
          "goto",
          "if",
          "implements",
          "import",
          "in",
          "instanceof",
          "native",
          "new",
          "package",
          "private",
          "protected",
          "public",
          "return",
          "static",
          "switch",
          "synchronized",
          "throw",
          "throws",
          "transient",
          "try",
          "typeof",
          "var",
          "void",
          "volatile",
          "while",
          "with");

  /**
   * Set of all HTML attributes which expect a URI (as the value).
   * <a href="http://www.w3.org/TR/html4/index/attributes.html">Index of Attributes</a>
   */
  private static final Set<String> ATTRIBUTE_EXPECTS_URI =
      ImmutableSortedSet.of(
          "action",
          "archive",
          "background",
          "cite",
          "classid",
          "codebase",
          "data",
          "dynsrc",
          "href",
          "longdesc",
          "src",
          "usemap");

  /**
   * Set of {@code Character}s considered whitespace in Javascript.
   * See {@link #isJavascriptWhitespace(char)}
   */
  private static final Set<Character> JAVASCRIPT_WHITESPACE =
      ImmutableSortedSet.of(
            '\u0009',         /* Tab \t */
            '\n',             /* Line-Feed 0x0A */
            '\u000B',         /* Vertical Tab 0x0B */
            '\u000C',         /* Form Feed \f */
            '\r',             /* Carriage Return 0x0D */
            ' ',              /* Space 0x20 */
            '\u00A0',         /* Non-breaking space 0xA0 */
            '\u2028',         /* Line separator */
            '\u2029');        /* Paragraph separator */

  /**
  * Set of {@code Character}s considered whitespace in HTML.
  * See {@link #isHtmlSpace(char)}
  */
 private static final Set<Character> HTML_WHITESPACE =
      ImmutableSortedSet.of(
          ' ',
          '\t',
          '\n',
          '\r',
          '\u200B');


  /**
   * Determines if the HTML attribute specified expects javascript
   * for its value. Such is the case for example with the {@code onclick}
   * attribute.
   *
   * <p>Currently returns {@code true} for any attribute name that starts
   * with "on" which is not exactly correct but we trust a developer to
   * not use non-spec compliant attribute names (e.g. onbogus).
   *
   * @param attribute the name of an HTML attribute
   * @return {@code false} if the input is null or is not an attribute
   *         that expects javascript code; {@code true}
   */
  public static boolean isAttributeJavascript(String attribute) {
    return ((attribute != null) && attribute.startsWith("on"));
  }

  /**
   * Determines if the HTML attribute specified expects a {@code style}
   * for its value. Currently this is only true for the {@code style}
   * HTML attribute.
   *
   * @param attribute the name of an HTML attribute
   * @return {@code true} iff the attribute name is one that expects a
   *     style for a value; otherwise {@code false}
   */
  public static boolean isAttributeStyle(String attribute) {
    return "style".equals(attribute);
  }

  /**
   * Determines if the HTML attribute specified expects a {@code URI}
   * for its value. For example, both {@code href} and {@code src}
   * expect a {@code URI} but {@code style} does not. Returns
   * {@code false} if the attribute given was {@code null}.
   *
   * @param attribute the name of an HTML attribute
   * @return {@code true} if the attribute name is one that expects
   *         a URI for a value; otherwise {@code null}
   *
   * @see #ATTRIBUTE_EXPECTS_URI
   */
  public static boolean isAttributeUri(String attribute) {
    return ATTRIBUTE_EXPECTS_URI.contains(attribute);
  }

  /**
   * Determines if the specified character is an HTML whitespace character.
   * A character is an HTML whitespace character if and only if it is one
   * of the characters below.
   * <ul>
   * <li>A <code>Space</code> character
   * <li>A <code>Tab</code> character
   * <li>A <code>Line feed</code> character
   * <li>A <code>Carriage Return</code> character
   * <li>A <code>Zero-Width Space</code> character
   * </ul>
   *
   * Note: The list includes the zero-width space (<code>&amp;#x200B;</code>)
   * which is not included in the C version.
   *
   * @param chr the {@code char} to check
   * @return {@code true} if the character is an HTML whitespace character
   *
   * <a href="http://www.w3.org/TR/html401/struct/text.html#h-9.1">White space</a>
   */
  public static boolean isHtmlSpace(char chr) {
    return HTML_WHITESPACE.contains(chr);
  }

  /**
   * Determines if the specified character is an ECMAScript whitespace or line
   * terminator character. A character is a whitespace or line terminator if
   * and only if it is one of the characters below:
   * <ul>
   * <li>A white-space character (<code>Tab</code>, <code>Vertical Tab</code>,
   *     <code>Form Feed</code>, <code>Space</code>,
   *     <code>No-break space</code>)
   * <li>A line terminator character (<code>Line Feed</code>,
   *     <code>Carriage Return</code>, <code>Line separator</code>,
   *     <code>Paragraph Separator</code>).
   * </ul>
   *
   * <p>Encompasses the characters in sections 7.2 and 7.3 of ECMAScript 3, in
   * particular, this list is quite different from that in
   * <code>Character.isWhitespace</code>.
   * <a href="http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-262.pdf">
   * ECMAScript Language Specification</a>
   *
   * @param chr the {@code char} to check
   * @return {@code true} or {@code false}
   *
   */
  public static boolean isJavascriptWhitespace(char chr) {
    return JAVASCRIPT_WHITESPACE.contains(chr);
  }

  /**
   * Determines if the specified character is a valid character in an
   * ECMAScript identifier. This determination is currently not exact,
   * in particular:
   * <ul>
   * <li>It does not accept Unicode letters, only ASCII ones.
   * <li>It does not distinguish between the first character of an identifier
   *     (which cannot contain numbers) and subsequent characters.
   * </li>
   * </ul>
   *
   * We are considering leveraging <code>Character.isJavaIdentifierStart</code>
   * and <code>Character.isJavaIdentifierPart</code> given that Java
   * and Javascript follow similar identifier naming rules but we lose
   * compatibility with the C-version.
   *
   * @param chr {@code char} to check
   * @return {@code true} if the {@code chr} is a Javascript whitespace
   *         character; otherwise {@code false}
   */
  public static boolean isJavascriptIdentifier(char chr) {
    return ((chr >= 'a' && chr <= 'z')
        || (chr >= 'A' && chr <= 'Z')
        || (chr >= '0' && chr <= '9')
        || chr == '_' || chr == '$');
  }

  /**
   * Determines if the input token provided is a valid token prefix to a
   * javascript regular expression.  The token argument is compared against
   * a {@code Set} of identifiers that can precede a regular expression in the
   * javascript grammar, and returns {@code true} if the provided
   * {@code String} is in that {@code Set}.
   *
   * @param input the {@code String} token to check
   * @return {@code true} iff the token is a valid prefix of a regexp
   */
  public static boolean isJavascriptRegexpPrefix(String input) {
    return REGEXP_TOKEN_PREFIXS.contains(input);
  }

  /**
   * Encodes the specified character using Ascii for convenient insertion into
   * a single-quote enclosed {@code String}. Printable characters
   * are returned as-is. Carriage Return, Line Feed, Horizontal Tab,
   * back-slash and single quote are all backslash-escaped. All other characters
   * are returned hex-encoded.
   * 
   * @param chr {@code char} to encode
   * @return an Ascii-friendly encoding of the given {@code char}
   */
  public static String encodeCharForAscii(char chr) {
    if (chr == '\'') {
      return "\\'";
    } else if (chr == '\\') {
      return "\\\\";
    } else if (chr >= 32 && chr <= 126) {
      return String.format("%c", chr);
    } else if (chr == '\n') {
      return "\\n";
    } else if (chr == '\r') {
      return "\\r";
    } else if (chr == '\t') {
      return "\\t";
    } else {
      // Cannot apply a precision specifier for integral types. Specifying
      // 0-padded hex-encoding with minimum width of two.
      return String.format("\\u%04x", (int)chr);
    }
  }

  /**
   * Parses the given {@code String} to determine if it contains a URL in the
   * format followed by the {@code content} attribute of the {@code meta}
   * HTML tag.
   *
   * <p>This function expects to receive the value of the {@code content} HTML
   * attribute. This attribute takes on different meanings depending on the
   * value of the {@code http-equiv} HTML attribute of the same {@code meta}
   * tag. Since we may not have access to the {@code http-equiv} attribute,
   * we instead rely on parsing the given value to determine if it contains
   * a URL.
   *
   * The specification of the {@code meta} HTML tag can be found in:
   *   http://dev.w3.org/html5/spec/Overview.html#attr-meta-http-equiv-refresh
   *
   * <p>We return {@link HtmlUtils.META_REDIRECT_TYPE} indicating whether the
   * value contains a URL and whether we are at the start of the URL or past
   * the start. We are at the start of the URL if and only if one of the two
   * conditions below is true:
   * <ul>
   * <li>The given input does not contain any characters from the URL proper.
   * Example "5; URL=".
   * <li>The given input only contains the optional leading single or double
   * quote leading the URL. Example "5; URL='".
   * </li>
   * </ul>
   *
   * <p>Examples:
   * <ul>
   * <li> Example of a complete {@code meta} tag where the {@code content}
   * attribute contains a URL [we are not at the start of the URL]:
   * <pre>
   * &lt;meta http-equiv="refresh" content="5; URL=http://www.google.com"&gt;
   * </pre>
   * <li> Example of a complete {@code meta} tag where the {@code content}
   * attribute contains a URL [we are at the start of the URL]:
   * <pre>
   * &lt;meta http-equiv="refresh" content="5; URL="&gt;
   * </pre>
   * <li>Example of a complete {@code meta} tag where the {@code content}
   * attribute does not contain a URL:
   * <pre>
   * &lt;meta http-equiv="content-type" content="text/html"&gt;
   * </pre>
   * </ul>
   *
   * @param value {@code String} to parse
   * @return {@link HtmlUtils.META_REDIRECT_TYPE} indicating the presence
   * of a URL in the given value
   */
  public static META_REDIRECT_TYPE parseContentAttributeForUrl(String value) {
    if (value == null)
      return META_REDIRECT_TYPE.NONE;

    Matcher matcher = META_REDIRECT_PATTERN.matcher(value);
    if (!matcher.find())
      return META_REDIRECT_TYPE.NONE;

    // We have more content.
    if (value.length() > matcher.end())
      return META_REDIRECT_TYPE.URL;

    return META_REDIRECT_TYPE.URL_START;
  }
}
