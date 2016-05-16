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

package com.google.clearsilver.jsilver.autoescape;

import static com.google.clearsilver.jsilver.autoescape.EscapeMode.ESCAPE_AUTO_ATTR;
import static com.google.clearsilver.jsilver.autoescape.EscapeMode.ESCAPE_AUTO_ATTR_CSS;
import static com.google.clearsilver.jsilver.autoescape.EscapeMode.ESCAPE_AUTO_ATTR_JS;
import static com.google.clearsilver.jsilver.autoescape.EscapeMode.ESCAPE_AUTO_ATTR_UNQUOTED_JS;
import static com.google.clearsilver.jsilver.autoescape.EscapeMode.ESCAPE_AUTO_ATTR_URI;
import static com.google.clearsilver.jsilver.autoescape.EscapeMode.ESCAPE_AUTO_ATTR_URI_START;
import static com.google.clearsilver.jsilver.autoescape.EscapeMode.ESCAPE_AUTO_HTML;
import static com.google.clearsilver.jsilver.autoescape.EscapeMode.ESCAPE_AUTO_JS;
import static com.google.clearsilver.jsilver.autoescape.EscapeMode.ESCAPE_AUTO_JS_UNQUOTED;
import static com.google.clearsilver.jsilver.autoescape.EscapeMode.ESCAPE_AUTO_STYLE;
import static com.google.clearsilver.jsilver.autoescape.EscapeMode.ESCAPE_AUTO_UNQUOTED_ATTR;
import static com.google.clearsilver.jsilver.autoescape.EscapeMode.ESCAPE_AUTO_UNQUOTED_ATTR_CSS;
import static com.google.clearsilver.jsilver.autoescape.EscapeMode.ESCAPE_AUTO_UNQUOTED_ATTR_JS;
import static com.google.clearsilver.jsilver.autoescape.EscapeMode.ESCAPE_AUTO_UNQUOTED_ATTR_UNQUOTED_JS;
import static com.google.clearsilver.jsilver.autoescape.EscapeMode.ESCAPE_AUTO_UNQUOTED_ATTR_URI;
import static com.google.clearsilver.jsilver.autoescape.EscapeMode.ESCAPE_AUTO_UNQUOTED_ATTR_URI_START;
import com.google.clearsilver.jsilver.exceptions.JSilverAutoEscapingException;
import com.google.streamhtmlparser.ExternalState;
import com.google.streamhtmlparser.HtmlParser;
import com.google.streamhtmlparser.HtmlParserFactory;
import com.google.streamhtmlparser.ParseException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Encapsulates auto escaping logic.
 */
public class AutoEscapeContext {
  /**
   * Map of content-type to corresponding {@code HtmlParser.Mode}, used by {@code setContentType} to
   * specify the content type of provided input. Valid values and the corresponding mode are: <br>
   * <table>
   * <tr>
   * <td>text/html</td>
   * <td>HtmlParser.Mode.HTML</td>
   * </tr>
   * <tr>
   * <td>text/plain</td>
   * <td>HtmlParser.Mode.HTML</td>
   * </tr>
   * <tr>
   * <td>application/javascript</td>
   * <td>HtmlParser.Mode.JS</td>
   * </tr>
   * <tr>
   * <td>application/json</td>
   * <td>HtmlParser.Mode.JS</td>
   * </tr>
   * <tr>
   * <td>text/javascript</td>
   * <td>HtmlParser.Mode.JS</td>
   * </tr>
   * <tr>
   * <td>text/css</td>
   * <td>HtmlParser.Mode.CSS</td>
   * </tr>
   * </table>
   * 
   * @see #setContentType
   */
  public static final Map<String, HtmlParser.Mode> CONTENT_TYPE_LIST;

  // These options are used to provide extra information to HtmlParserFactory.createParserInMode or
  // HtmlParserFactory.createParserInAttribute, which is required for certain modes.
  private static final HashSet<HtmlParserFactory.AttributeOptions> quotedJsAttributeOption;
  private static final HashSet<HtmlParserFactory.AttributeOptions> partialUrlAttributeOption;
  private static final HashSet<HtmlParserFactory.ModeOptions> jsModeOption;

  private HtmlParser htmlParser;

  static {
    quotedJsAttributeOption = new HashSet<HtmlParserFactory.AttributeOptions>();
    quotedJsAttributeOption.add(HtmlParserFactory.AttributeOptions.JS_QUOTED);

    partialUrlAttributeOption = new HashSet<HtmlParserFactory.AttributeOptions>();
    partialUrlAttributeOption.add(HtmlParserFactory.AttributeOptions.URL_PARTIAL);

    jsModeOption = new HashSet<HtmlParserFactory.ModeOptions>();
    jsModeOption.add(HtmlParserFactory.ModeOptions.JS_QUOTED);

    CONTENT_TYPE_LIST = new HashMap<String, HtmlParser.Mode>();
    CONTENT_TYPE_LIST.put("text/html", HtmlParser.Mode.HTML);
    CONTENT_TYPE_LIST.put("text/plain", HtmlParser.Mode.HTML);
    CONTENT_TYPE_LIST.put("application/javascript", HtmlParser.Mode.JS);
    CONTENT_TYPE_LIST.put("application/json", HtmlParser.Mode.JS);
    CONTENT_TYPE_LIST.put("text/javascript", HtmlParser.Mode.JS);
    CONTENT_TYPE_LIST.put("text/css", HtmlParser.Mode.CSS);
  }

  /**
   * Name of resource being auto escaped. Will be used in error and display messages.
   */
  private String resourceName;

  public AutoEscapeContext() {
    this(EscapeMode.ESCAPE_AUTO, null);
  }

  /**
   * Create a new context in the state represented by mode.
   * 
   * @param mode EscapeMode object.
   */
  public AutoEscapeContext(EscapeMode mode) {
    this(mode, null);
  }

  /**
   * Create a new context in the state represented by mode. If a non-null resourceName is provided,
   * it will be used in displaying error messages.
   * 
   * @param mode The initial EscapeMode for this context
   * @param resourceName Name of the resource being auto escaped.
   */
  public AutoEscapeContext(EscapeMode mode, String resourceName) {
    this.resourceName = resourceName;
    htmlParser = createHtmlParser(mode);
  }

  /**
   * Create a new context that is a copy of the current state of this context.
   * 
   * @return New {@code AutoEscapeContext} that is a snapshot of the current state of this context.
   */
  public AutoEscapeContext cloneCurrentEscapeContext() {
    AutoEscapeContext autoEscapeContext = new AutoEscapeContext();
    autoEscapeContext.resourceName = resourceName;
    autoEscapeContext.htmlParser = HtmlParserFactory.createParser(htmlParser);
    return autoEscapeContext;
  }

  /**
   * Sets the current position in the resource being auto escaped. Useful for generating detailed
   * error messages.
   * 
   * @param line line number.
   * @param column column number within line.
   */
  public void setCurrentPosition(int line, int column) {
    htmlParser.setLineNumber(line);
    htmlParser.setColumnNumber(column);
  }

  /**
   * Returns the name of the resource currently being auto escaped.
   */
  public String getResourceName() {
    return resourceName;
  }

  /**
   * Returns the current line number within the resource being auto escaped.
   */
  public int getLineNumber() {
    return htmlParser.getLineNumber();
  }

  /**
   * Returns the current column number within the resource being auto escaped.
   */
  public int getColumnNumber() {
    return htmlParser.getColumnNumber();
  }

  private HtmlParser createHtmlParser(EscapeMode mode) {
    switch (mode) {
      case ESCAPE_AUTO:
      case ESCAPE_AUTO_HTML:
        return HtmlParserFactory.createParser();

      case ESCAPE_AUTO_JS_UNQUOTED:
        // <script>START HERE
        return HtmlParserFactory.createParserInMode(HtmlParser.Mode.JS, null);

      case ESCAPE_AUTO_JS:
        // <script> var a = 'START HERE
        return HtmlParserFactory.createParserInMode(HtmlParser.Mode.JS, jsModeOption);

      case ESCAPE_AUTO_STYLE:
        // <style>START HERE
        return HtmlParserFactory.createParserInMode(HtmlParser.Mode.CSS, null);

      case ESCAPE_AUTO_ATTR:
        // <input text="START HERE
        return HtmlParserFactory.createParserInAttribute(HtmlParser.ATTR_TYPE.REGULAR, true, null);

      case ESCAPE_AUTO_UNQUOTED_ATTR:
        // <input text=START HERE
        return HtmlParserFactory.createParserInAttribute(HtmlParser.ATTR_TYPE.REGULAR, false, null);

      case ESCAPE_AUTO_ATTR_URI:
        // <a href="http://www.google.com/a?START HERE
        return HtmlParserFactory.createParserInAttribute(HtmlParser.ATTR_TYPE.URI, true,
            partialUrlAttributeOption);

      case ESCAPE_AUTO_UNQUOTED_ATTR_URI:
        // <a href=http://www.google.com/a?START HERE
        return HtmlParserFactory.createParserInAttribute(HtmlParser.ATTR_TYPE.URI, false,
            partialUrlAttributeOption);

      case ESCAPE_AUTO_ATTR_URI_START:
        // <a href="START HERE
        return HtmlParserFactory.createParserInAttribute(HtmlParser.ATTR_TYPE.URI, true, null);

      case ESCAPE_AUTO_UNQUOTED_ATTR_URI_START:
        // <a href=START HERE
        return HtmlParserFactory.createParserInAttribute(HtmlParser.ATTR_TYPE.URI, false, null);

      case ESCAPE_AUTO_ATTR_JS:
        // <input onclick="doClick('START HERE
        return HtmlParserFactory.createParserInAttribute(HtmlParser.ATTR_TYPE.JS, true,
            quotedJsAttributeOption);

      case ESCAPE_AUTO_ATTR_UNQUOTED_JS:
        // <input onclick="doClick(START HERE
        return HtmlParserFactory.createParserInAttribute(HtmlParser.ATTR_TYPE.JS, true, null);

      case ESCAPE_AUTO_UNQUOTED_ATTR_JS:
        // <input onclick=doClick('START HERE
        throw new JSilverAutoEscapingException(
            "Attempting to start HTML parser in unsupported mode" + mode, resourceName);

      case ESCAPE_AUTO_UNQUOTED_ATTR_UNQUOTED_JS:
        // <input onclick=doClick(START HERE
        return HtmlParserFactory.createParserInAttribute(HtmlParser.ATTR_TYPE.JS, false, null);

      case ESCAPE_AUTO_ATTR_CSS:
        // <input style="START HERE
        return HtmlParserFactory.createParserInAttribute(HtmlParser.ATTR_TYPE.STYLE, true, null);

      case ESCAPE_AUTO_UNQUOTED_ATTR_CSS:
        // <input style=START HERE
        return HtmlParserFactory.createParserInAttribute(HtmlParser.ATTR_TYPE.STYLE, false, null);

      default:
        throw new JSilverAutoEscapingException("Attempting to start HTML parser in invalid mode"
            + mode, resourceName);
    }
  }

  /**
   * Parse the given data and update internal state accordingly.
   * 
   * @param data Input to parse, usually the contents of a template.
   */
  public void parseData(String data) {
    try {
      htmlParser.parse(data);
    } catch (ParseException e) {
      // ParseException displays the proper position, so do not store line and column
      // number here.
      throw new JSilverAutoEscapingException("Error in HtmlParser: " + e, resourceName);
    }
  }

  /**
   * Lets the AutoEscapeContext know that some input was skipped.
   * 
   * This method will usually be called for variables in the input stream. The AutoEscapeContext is
   * told that the input stream contained some additional data but does not get to see the data. It
   * can adjust its internal state accordingly.
   */
  public void insertText() {
    try {
      htmlParser.insertText();
    } catch (ParseException e) {
      throw new JSilverAutoEscapingException("Error during insertText(): " + e, resourceName,
          htmlParser.getLineNumber(), htmlParser.getColumnNumber());
    }
  }

  /**
   * Determines whether an included template that begins in state {@code start} is allowed to end in
   * state {@code end}. Usually included templates are only allowed to end in the same context they
   * begin in. This lets auto escaping parse the remainder of the parent template without needing to
   * know the ending context of the included template. However, there is one exception where auto
   * escaping will allow a different ending context: if the included template is a URI attribute
   * value, it is allowed to change context from {@code ATTR_URI_START} to {@code ATTR_URI}. This
   * does not cause any issues because the including template will call {@code insertText} when it
   * encounters the include command, and {@code insertText} will cause the HTML parser to switch its
   * internal state in the same way.
   */
  public boolean isPermittedStateChangeForIncludes(AutoEscapeState start, AutoEscapeState end) {
    return start.equals(end)
        || (start.equals(AutoEscapeState.ATTR_URI_START) && end.equals(AutoEscapeState.ATTR_URI))
        || (start.equals(AutoEscapeState.UNQUOTED_ATTR_URI_START) && end
            .equals(AutoEscapeState.UNQUOTED_ATTR_URI));
  }

  /**
   * Determine the correct escaping to apply for a variable.
   * 
   * Looks at the current state of the htmlParser, and determines what escaping to apply to a
   * variable in this state.
   * 
   * @return Name of escaping function to use in this state.
   */
  public String getEscapingFunctionForCurrentState() {
    return getCurrentState().getFunctionName();
  }

  /**
   * Returns the EscapeMode which will bring AutoEscapeContext into this state.
   * 
   * Initializing a new AutoEscapeContext with this EscapeMode will bring it into the state that the
   * current AutoEscapeContext object is in.
   * 
   * @return An EscapeMode object.
   */
  public EscapeMode getEscapeModeForCurrentState() {
    return getCurrentState().getEscapeMode();
  }

  /**
   * Calls the HtmlParser API to determine current state.
   * 
   * This function is mostly a wrapper around the HtmlParser API. It gathers all the necessary
   * information using that API and returns a single enum representing the current state.
   * 
   * @return AutoEscapeState enum representing the current state.
   */
  public AutoEscapeState getCurrentState() {
    ExternalState state = htmlParser.getState();
    String tag = htmlParser.getTag();

    // Currently we do not do any escaping inside CSS blocks, so ignore them.
    if (state.equals(HtmlParser.STATE_CSS_FILE) || tag.equals("style")) {

      return AutoEscapeState.STYLE;
    }

    // Handle variables inside <script> tags.
    if (htmlParser.inJavascript() && !state.equals(HtmlParser.STATE_VALUE)) {
      if (htmlParser.isJavascriptQuoted()) {
        // <script> var a = "<?cs var: Blah ?>"; </script>
        return AutoEscapeState.JS;
      } else {
        // <script> var a = <?cs var: Blah ?>; </script>
        // No quotes around the variable, hence it can inject arbitrary javascript.
        // So severely restrict the values it may contain.
        return AutoEscapeState.JS_UNQUOTED;
      }
    }

    // Inside an HTML tag or attribute name
    if (state.equals(HtmlParser.STATE_ATTR) || state.equals(HtmlParser.STATE_TAG)) {
      return AutoEscapeState.ATTR;
      // TODO: Need a strict validation function for tag and attribute names.
    } else if (state.equals(HtmlParser.STATE_VALUE)) {
      // Inside an HTML attribute value
      return getCurrentAttributeState();
    } else if (state.equals(HtmlParser.STATE_COMMENT) || state.equals(HtmlParser.STATE_TEXT)) {
      // Default is assumed to be HTML body
      // <b>Hello <?cs var: UserName ?></b> :
      return AutoEscapeState.HTML;
    }

    throw new JSilverAutoEscapingException("Invalid state received from HtmlParser: "
        + state.toString(), resourceName, htmlParser.getLineNumber(), htmlParser.getColumnNumber());
  }

  private AutoEscapeState getCurrentAttributeState() {
    HtmlParser.ATTR_TYPE type = htmlParser.getAttributeType();
    boolean attrQuoted = htmlParser.isAttributeQuoted();

    switch (type) {
      case REGULAR:
        // <input value="<?cs var: Blah ?>"> :
        if (attrQuoted) {
          return AutoEscapeState.ATTR;
        } else {
          return AutoEscapeState.UNQUOTED_ATTR;
        }

      case URI:
        if (htmlParser.isUrlStart()) {
          // <a href="<?cs var: X ?>">
          if (attrQuoted) {
            return AutoEscapeState.ATTR_URI_START;
          } else {
            return AutoEscapeState.UNQUOTED_ATTR_URI_START;
          }
        } else {
          // <a href="http://www.google.com/a?x=<?cs var: X ?>">
          if (attrQuoted) {
            // TODO: Html escaping because that is what Clearsilver does right now.
            // May change this to url escaping soon.
            return AutoEscapeState.ATTR_URI;
          } else {
            return AutoEscapeState.UNQUOTED_ATTR_URI;
          }
        }

      case JS:
        if (htmlParser.isJavascriptQuoted()) {
          /*
           * Note: js_escape() hex encodes all html metacharacters. Therefore it is safe to not do
           * an HTML escape around this.
           */
          if (attrQuoted) {
            // <input onclick="alert('<?cs var:Blah ?>');">
            return AutoEscapeState.ATTR_JS;
          } else {
            // <input onclick=alert('<?cs var: Blah ?>');>
            return AutoEscapeState.UNQUOTED_ATTR_JS;
          }
        } else {
          if (attrQuoted) {
            /* <input onclick="alert(<?cs var:Blah ?>);"> */
            return AutoEscapeState.ATTR_UNQUOTED_JS;
          } else {

            /* <input onclick=alert(<?cs var:Blah ?>);> */
            return AutoEscapeState.UNQUOTED_ATTR_UNQUOTED_JS;
          }
        }

      case STYLE:
        // <input style="border:<?cs var: FancyBorder ?>"> :
        if (attrQuoted) {
          return AutoEscapeState.ATTR_CSS;
        } else {
          return AutoEscapeState.UNQUOTED_ATTR_CSS;
        }

      default:
        throw new JSilverAutoEscapingException("Invalid attribute type in HtmlParser: " + type,
            resourceName, htmlParser.getLineNumber(), htmlParser.getColumnNumber());
    }
  }

  /**
   * Resets the state of the underlying html parser to a state consistent with the {@code
   * contentType} provided. This method should be used when the starting auto escaping context of a
   * resource cannot be determined from its contents - for example, a CSS stylesheet or a javascript
   * source file.
   * 
   * @param contentType MIME type header representing the content being parsed.
   * @see #CONTENT_TYPE_LIST
   */
  public void setContentType(String contentType) {
    HtmlParser.Mode mode = CONTENT_TYPE_LIST.get(contentType);
    if (mode == null) {
      throw new JSilverAutoEscapingException("Invalid content type specified: " + contentType,
          resourceName, htmlParser.getLineNumber(), htmlParser.getColumnNumber());

    }
    htmlParser.resetMode(mode);
  }

  /**
   * Enum representing states of the data being parsed.
   * 
   * This enumeration lists all the states in which autoescaping would have some effect.
   * 
   */
  public static enum AutoEscapeState {
    HTML("html", ESCAPE_AUTO_HTML), JS("js", ESCAPE_AUTO_JS), STYLE("css", ESCAPE_AUTO_STYLE), JS_UNQUOTED(
        "js_check_number", ESCAPE_AUTO_JS_UNQUOTED), ATTR("html", ESCAPE_AUTO_ATTR), UNQUOTED_ATTR(
        "html_unquoted", ESCAPE_AUTO_UNQUOTED_ATTR), ATTR_URI("html", ESCAPE_AUTO_ATTR_URI), UNQUOTED_ATTR_URI(
        "html_unquoted", ESCAPE_AUTO_UNQUOTED_ATTR_URI), ATTR_URI_START("url_validate",
        ESCAPE_AUTO_ATTR_URI_START), UNQUOTED_ATTR_URI_START("url_validate_unquoted",
        ESCAPE_AUTO_UNQUOTED_ATTR_URI_START), ATTR_JS("js", ESCAPE_AUTO_ATTR_JS), ATTR_UNQUOTED_JS(
        "js_check_number", ESCAPE_AUTO_ATTR_UNQUOTED_JS), UNQUOTED_ATTR_JS("js_attr_unquoted",
        ESCAPE_AUTO_UNQUOTED_ATTR_JS), UNQUOTED_ATTR_UNQUOTED_JS("js_check_number",
        ESCAPE_AUTO_UNQUOTED_ATTR_UNQUOTED_JS), ATTR_CSS("css", ESCAPE_AUTO_ATTR_CSS), UNQUOTED_ATTR_CSS(
        "css_unquoted", ESCAPE_AUTO_UNQUOTED_ATTR_CSS);

    private final String functionName;
    private final EscapeMode escapeMode;

    private AutoEscapeState(String functionName, EscapeMode mode) {
      this.functionName = functionName;
      this.escapeMode = mode;
    }

    public String getFunctionName() {
      return functionName;
    }

    public EscapeMode getEscapeMode() {
      return escapeMode;
    }
  }
}
