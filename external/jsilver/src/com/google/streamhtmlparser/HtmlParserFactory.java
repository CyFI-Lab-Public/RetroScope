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

import com.google.streamhtmlparser.impl.HtmlParserImpl;

import java.util.Set;
import java.util.logging.Logger;

/**
 * A factory class to obtain instances of an {@link HtmlParser}.
 * Currently each instance is a new object given these are fairly
 * light-weight.
 *
 * <p>In the unlikely case that this class fails to initialize properly
 * (a developer error), an error is emitted to the error console and the logs
 * and the specialized parser creation methods will throw
 * an {@link AssertionError} on all invokations.
 */
public class HtmlParserFactory {

  private static final Logger logger =
      Logger.getLogger(HtmlParserFactory.class.getName());

  /**
   * To provide additional options when creating an {@code HtmlParser} using
   * {@link HtmlParserFactory#createParserInAttribute(HtmlParser.ATTR_TYPE,
   *        boolean, Set)} 
   */
  public enum AttributeOptions {

    /**
     * Indicates that the attribute value is Javascript-quoted. Only takes
     * effect for Javascript-accepting attributes - as identified by
     * {@link HtmlParser.ATTR_TYPE#JS} - and only when the attribute is also
     * HTML quoted.
     */
    JS_QUOTED,

    /**
     * Indicates the attribute value is only a part of a URL as opposed to a
     * full URL. In particular, the value is not at the start of a URL and
     * hence does not necessitate validation of the URL scheme.
     * Only valid for URI-accepting attributes - as identified by
     * {@link HtmlParser.ATTR_TYPE#URI}.
     */
    URL_PARTIAL,
  }

  /**
   * To provide additional options when creating an {@code HtmlParser} using
   * {@link HtmlParserFactory#createParserInMode(HtmlParser.Mode, Set)}
   */
  public enum ModeOptions {

    /**
     * Indicates that the parser is inside a quoted {@code String}. Only
     * valid in the {@link HtmlParser.Mode#JS} mode.
     */
    JS_QUOTED
  }

  private static final HtmlParser parserInDefaultAttr = createParser();
  private static final HtmlParser parserInDefaultAttrQ = createParser();
  private static final HtmlParser parserInUriAttrComplete = createParser();
  private static final HtmlParser parserInUriAttrQComplete = createParser();
  private static final HtmlParser parserInUriAttrPartial = createParser();
  private static final HtmlParser parserInUriAttrQPartial = createParser();
  private static final HtmlParser parserInJsAttr = createParser();
  private static final HtmlParser parserInJsAttrQ = createParser();
  private static final HtmlParser parserInQJsAttr = createParser();
  private static final HtmlParser parserInStyleAttr = createParser();
  private static final HtmlParser parserInStyleAttrQ = createParser();
  private static final HtmlParser parserInJsQ = createParser();

  /**
   * Protects all the createParserXXX methods by throwing a run-time exception
   * if this class failed to initialize properly.
   */
  private static boolean initSuccess = false;

  static {
    try {
      initializeParsers();
      initSuccess = true;
    } catch (ParseException e) {
      // Log a severe error and print it to stderr along with a stack trace.
      String error = HtmlParserFactory.class.getName() +
                     " Failed initialization: " + e.getMessage();
      logger.severe(error);
      System.err.println(error);
      e.printStackTrace();
    }
  }

  // Static class.
  private HtmlParserFactory() {
  }  // COV_NF_LINE

  /**
   * Returns an {@code HtmlParser} object ready to parse HTML input.
   *
   * @return an {@code HtmlParser} in the provided mode
   */
  public static HtmlParser createParser() {
    return new HtmlParserImpl();
  }

  /**
   * Returns an {@code HtmlParser} object initialized with the
   * requested Mode. Provide non {@code null} options to provide
   * a more precise initialization with the desired Mode.
   *
   * @param mode the mode to reset the parser with
   * @param options additional options or {@code null} for none
   * @return an {@code HtmlParser} in the provided mode
   * @throws AssertionError when this class failed to initialize
   */
  public static HtmlParser createParserInMode(HtmlParser.Mode mode,
                                              Set<ModeOptions> options) {
    requireInitialized();

    if (options != null && options.contains(ModeOptions.JS_QUOTED))
      return createParser(parserInJsQ);

    // With no options given, this method is just a convenience wrapper for
    // the two calls below.
    HtmlParser parser = new HtmlParserImpl();
    parser.resetMode(mode);
    return parser;
  }

  /**
   * Returns an {@code HtmlParser} that is a copy of the one
   * supplied. It holds the same internal state and hence can
   * proceed with parsing in-lieu of the supplied parser.
   *
   * @param aHtmlParser a {@code HtmlParser} to copy from
   * @return an {@code HtmlParser} that is a copy of the provided one
   * @throws AssertionError when this class failed to initialize
   */
  public static HtmlParser createParser(HtmlParser aHtmlParser) {
    requireInitialized();

    // Should never get a ClassCastException since there is only one
    // implementation of the HtmlParser interface.
    return new HtmlParserImpl((HtmlParserImpl) aHtmlParser);
  }

  /**
   * A very specialized {@code HtmlParser} accessor that returns a parser
   * in a state where it expects to read the value of an attribute
   * of an HTML tag. This is only useful when the parser has not seen a
   * certain HTML tag and an attribute name and needs to continue parsing
   * from a state as though it has.
   *
   * <p>For example, to create a parser in a state akin to that
   * after the parser has parsed "&lt;a href=\"", invoke:
   * <pre>
   *   createParserInAttribute(HtmlParser.ATTR_TYPE.URI, true)}
   * </pre>
   * 
   * <p>You must provide the proper value of quoting or the parser
   * will go into an unexpected state.
   * As a special-case, when called with the {@code HtmlParser.ATTR_TYPE}
   * of {@code HtmlParser.ATTR_TYPE.NONE}, the parser is created in a state
   * inside an HTML tag where it expects an attribute name not an attribute
   * value. It becomes equivalent to a parser initialized in the
   * {@code HTML_IN_TAG} mode.
   *
   * @param attrtype the attribute type which the parser should be in
   * @param quoted whether the attribute value is enclosed in double quotes
   * @param options additional options or {@code null} for none
   * @return an {@code HtmlParser} initialized in the given attribute type
   *         and quoting
   * @throws AssertionError when this class failed to initialize
   */
  public static HtmlParser createParserInAttribute(
      HtmlParser.ATTR_TYPE attrtype,
      boolean quoted, Set<AttributeOptions> options) {
    requireInitialized();

    HtmlParser parser;
    switch (attrtype) {
      case REGULAR:
        parser = createParser(
            quoted ? parserInDefaultAttrQ : parserInDefaultAttr);
        break;
      case URI:
        if (options != null && options.contains(AttributeOptions.URL_PARTIAL))
          parser = createParser(
              quoted ? parserInUriAttrQPartial : parserInUriAttrPartial);
        else
          parser = createParser(
              quoted ? parserInUriAttrQComplete : parserInUriAttrComplete);
        break;
      case JS:
        // Note: We currently do not support the case of the value being
        // inside a Javascript quoted string that is in an unquoted HTML
        // attribute, such as <a href=bla onmouseover=alert('[VALUE')>.
        // It would be simple to add but currently we assume Javascript
        // quoted attribute values are always HTML quoted.
        if (quoted) {
          if (options != null && options.contains(AttributeOptions.JS_QUOTED))
            parser = createParser(parserInQJsAttr);
          else
            parser = createParser(parserInJsAttrQ);
        } else {
          parser = createParser(parserInJsAttr);
        }
        break;
      case STYLE:
        parser = createParser(
            quoted ? parserInStyleAttrQ : parserInStyleAttr);
        break;
      case NONE:
        parser = createParserInMode(HtmlParser.Mode.HTML_IN_TAG, null);
        break;
      default:
        throw new IllegalArgumentException(
            "Did not recognize ATTR_TYPE given: " + attrtype);
    }
    return parser;
  }

  /**
   * Initializes a set of static parsers to be subsequently used
   * by the various createParserXXX methods.
   * The parsers are set to their proper states by making them parse
   * an appropriate HTML input fragment. This approach is the most likely
   * to ensure all their internal state is consistent.
   *
   * <p>In the very unexpected case of the parsing failing (developer error),
   * this class will fail to initialize properly.
   *
   * <p>In addition:
   * <ul>
   * <li>The HTML tag is set to a fictitious name {@code xparsertag}.
   * <li>The attribute name is chosen to match the required attribute type.
   *     When several possibilities exist, one is chosen arbitrarily.
   * <li>If quoting is required, a double quote is provided after the '='.
   * </ul>
   *
   * @throws ParseException if parsing failed.
   */
  private static void initializeParsers() throws ParseException {
    parserInDefaultAttr.parse("<xparsertag htmlparser=");
    parserInDefaultAttrQ.parse("<xparsertag htmlparser=\"");

    // Chosing the "src" attribute, one of several possible names here
    parserInUriAttrComplete.parse("<xparsertag src=");
    parserInUriAttrQComplete.parse("<xparsertag src=\"");

    // To support a parser that is initialized within a URL parameter
    // rather than at the beginning of a URL. We use a fake domain
    // (example.com from RFC 2606 <http://www.rfc-editor.org/rfc/rfc2606.txt>)
    // and a fake query parameter.
    final String fakeUrlPrefix = "http://example.com/fakequeryparam=";
    parserInUriAttrPartial.parse("<xparsertag src=" + fakeUrlPrefix);
    parserInUriAttrQPartial.parse("<xparsertag src=\"" + fakeUrlPrefix);

    // Using onmouse= which is a fictitious attribute name that the parser
    // understands as being a valid javascript-enabled attribute. Chosing fake
    // names may help during debugging.
    parserInJsAttr.parse("<xparsertag onmouse=");
    parserInJsAttrQ.parse("<xparsertag onmouse=\"");
    // Single quote added as the Javascript is itself quoted.
    parserInQJsAttr.parse("<xparsertag onmouse=\"'");

    // A parser in the Javascript context within a (single) quoted string.
    parserInJsQ.resetMode(HtmlParser.Mode.JS);
    parserInJsQ.parse("var fakeparservar='");

    // Chosing the "style" attribute as it is the only option
    parserInStyleAttr.parse("<xparsertag style=");
    parserInStyleAttrQ.parse("<xparsertag style=\"");
  }

  /**
   * Throws an {@link AssertionError} if the class was not initialized
   * correctly, otherwise simply returns. This is to protect against the
   * possibility the needed parsers were not created successfully during
   * static initialized, which can only happen due to an error during
   * development of this library.
   * 
   * @throws AssertionError when this class failed to initialize
   */
  private static void requireInitialized() {
    if (!initSuccess)
      throw new AssertionError("HtmlParserFactory failed initialization.");
  }
}
