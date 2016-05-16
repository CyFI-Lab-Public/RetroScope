/**
 * Copyright (c) 2004, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.mail.common.html.parser;

import com.google.android.mail.common.base.CharEscapers;
import com.google.android.mail.common.base.CharMatcher;
import com.google.android.mail.common.base.Preconditions;
import com.google.android.mail.common.base.StringUtil;
import com.google.android.mail.common.base.X;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HtmlParser is a simple but efficient html parser.
 * - It's simple because it does not do incremental parsing like some other
 * parser. It assumes that the entire html text is available.
 * - It offers 3 levels of aggressiveness in correcting errors in HTML (see
 * HtmlParser.ParseStyle).
 * - HTML comments are ignored unless initialized with ParseStyle.PRESERVE_ALL.
 */
public class HtmlParser {

  // States
  private enum State {
    IN_TEXT, IN_TAG, IN_COMMENT, IN_CDATA
  }

  // The current state
  private State state;

  private int clipLength = Integer.MAX_VALUE;
  private boolean clipped;

  // The html text
  private String html;

  // The entire array of nodes
  private List<HtmlDocument.Node> nodes;

  // Turn on for debug information.
  private static boolean DEBUG = false;

  // Default whitelist
  public static final HtmlWhitelist DEFAULT_WHITELIST = HTML4.getWhitelist();

  // Whitelists for looking up accepted HTML tags and attributes
  private List<HtmlWhitelist> whitelists = Lists.newArrayList(DEFAULT_WHITELIST);

  /**
   * This setting controls how much of the original HTML is preserved.  In
   * ascending order of aggressiveness:
   * - PRESERVE_ALL: Preserves all of original content.
   * *** Warning - PRESERVE_ALL mode preserves invalid and unsafe HTML. ***
   * - PRESERVE_VALID: Preserves only valid visible HTML handled by
   * most browsers.  Discards comments, unknown tags and attributes, and
   * nameless end tags.  Encodes all '<' characters that aren't part of a tag.
   * - NORMALIZE: In addition to the changes made by PRESERVE_VALID, also
   *   - unescapes then reescapes text to normalize entities
   *   - normalizes whitespace and quotes around tag attributes
   */
  public enum ParseStyle { NORMALIZE, PRESERVE_VALID, PRESERVE_ALL }

  /**
   * True only in PRESERVE_ALL mode.
   * @see HtmlParser.ParseStyle
   */
  private final boolean preserveAll;

  /**
   * True in either PRESERVE_ALL or PRESERVE_VALID mode.
   * @see HtmlParser.ParseStyle
   */
  private final boolean preserveValidHtml;

  /**
   * @see HtmlParser#HtmlParser(HtmlParser.ParseStyle)
   */
  public HtmlParser() {
    this(ParseStyle.NORMALIZE);
  }

  /**
   * @param parseStyle Level of aggressiveness for how different
   * toHTML()/toXHTML() are from original.
   * @see HtmlParser.ParseStyle
   */
  public HtmlParser(ParseStyle parseStyle) {
    preserveAll = (parseStyle == ParseStyle.PRESERVE_ALL);
    preserveValidHtml = preserveAll || (parseStyle == ParseStyle.PRESERVE_VALID);
  }

  /**
   * Sets the maximum length, in characters, of an HTML message.
   *
   * @param clipLength must be greater than zero.
   * (It starts as Integer.MAX_VALUE)
   */
  public void setClipLength(int clipLength) {
    if (clipLength <= 0) {
      throw new IllegalArgumentException(
        "clipLength '" + clipLength + "' <= 0");
    }
    this.clipLength = clipLength;
  }

  public boolean isClipped() {
    return clipped;
  }

  /**
   * Sets the HTML whitelist. Calling this overrides any whitelist(s) that
   * the parser is configured to use. By default, the parser uses the standard
   * HTML4 whitelist.
   *
   * This has no effect in <code>ParseStyle.PRESERVE_ALL</code> mode.
   *
   * @param whitelist The whitelist to use. Must not be null.
   */
  public void setWhitelist(HtmlWhitelist whitelist) {
    Preconditions.checkNotNull(whitelist);
    whitelists = Lists.newArrayList(whitelist);
  }

  /**
   * Adds an HTML whitelist to the list of whitelists consulted when
   * processing an element or attribute. By default, the parser only uses
   * the standard HTML4 whitelist.
   *
   * Whitelists are consulted in reverse chronological order (starting from
   * the most recently added whitelist). The last whitelist consulted will
   * always be the standard HTML4 whitelist, unless this was overridden by
   * a call to {@link #setWhitelist}.
   *
   * This has no effect in <code>ParseStyle.PRESERVE_ALL</code> mode.
   *
   * @param whitelist The whitelist to use.
   */
  public void addWhitelist(HtmlWhitelist whitelist) {
    whitelists.add(whitelist);
  }

  /**
   * These are characters that we don't want to allow unquoted in an attribute
   * value because they might be interpreted by the browser as HTML control
   * characters. They are the 5 characters that are escaped by
   * com.google.common.base.CharEscapers.HTML_ESCAPE, plus '=' and whitespace.
   * Note that it shouldn't be possible for '>' or whitespace to be parsed as
   * part of an unquoted attribute value, but we leave them here for
   * completeness.
   * Package visibility for unit tests.
   */
  static Pattern NEEDS_QUOTING_ATTRIBUTE_VALUE_REGEX = Pattern.compile("[\"\'&<>=\\s]");

  //------------------------------------------------------------------------
  // Parsing
  //------------------------------------------------------------------------

  /**
   * Parses a String as HTML.
   *
   * @param html String to parse
   * @return an Html document
   */
  public HtmlDocument parse(String html) {
    this.html = html;
    // Use a LinkedList because we don't know the number of nodes ahead of
    // time. This will be compacted into an ArrayList in coalesceTextNodes().
    nodes = Lists.newLinkedList();
    state = State.IN_TEXT;

    clipped = false;
    int end = html.length();
    int clipEnd = Math.min(clipLength, end);

    for (int i = 0; i < end && !clipped;) {

      // At any one time, the parser is in one of these states:
      int pos;
      switch (state) {
        case IN_TEXT:
          // text will not attempt to parse beyond the clipping length
          pos = scanText(i, clipEnd);
          X.assertTrue(pos > i || state != State.IN_TEXT); // Must make progress.
          break;

        case IN_TAG:
          pos = scanTag(i, end);
          X.assertTrue(pos > i);        // Must make progress
          break;

        case IN_COMMENT:
          pos = scanComment(i, end);
          state = State.IN_TEXT;
          X.assertTrue(pos > i);        // Must make progress
          break;

        case IN_CDATA:
          pos = scanCDATA(i, end);
          X.assertTrue(pos > i || state != State.IN_CDATA); // Must make progress
          break;

        default:
          throw new Error("Unknown state!");
      }

      i = pos;

      // If we've reached or gone beyond the clipping length, stop.
      clipped = pos >= clipLength;
    }

    nodes = coalesceTextNodes(nodes);

    HtmlDocument doc = new HtmlDocument(nodes);
    nodes = null;
    html = null;
    return doc;
  }

  /**
   * During the course of parsing, we may have multiple adjacent Text nodes,
   * due to the sanitizer stripping out nodes between Text nodes. It is
   * important to coalesce them so that later steps in the pipeline can
   * treat the text as a single block (e.g. the step that inserts <wbr> tags).
   * @param nodes Original nodes.
   * @return Nodes with text nodes changed.
   */
  static List<HtmlDocument.Node> coalesceTextNodes(
      List<HtmlDocument.Node> nodes) {
    List<HtmlDocument.Node> out =
        new ArrayList<HtmlDocument.Node>(nodes.size());
    LinkedList<HtmlDocument.Text> textNodes = Lists.newLinkedList();

    for (HtmlDocument.Node node : nodes) {
      if (node instanceof HtmlDocument.Text) {
        textNodes.add((HtmlDocument.Text) node);
      } else {
        mergeTextNodes(textNodes, out);
        out.add(node);
      }
    }
    mergeTextNodes(textNodes, out);
    return out;
  }

  /**
   * Flushes any Text nodes in {@code textNodes} into a single Text node
   * in {@code output}. {@code textNodes} is guaranteed to be empty when
   * the function returns.
   * @param textNodes Text nodes.
   * @param output Destination to which results are added.
   */
  private static void mergeTextNodes(LinkedList<HtmlDocument.Text> textNodes,
                                     List<HtmlDocument.Node> output) {
    if (!textNodes.isEmpty()) {
      if (textNodes.size() == 1) {
        output.add(textNodes.removeFirst());
      } else {
        int combinedTextLen = 0;
        int combinedInputLen = 0;
        for (HtmlDocument.Text text : textNodes) {
          combinedTextLen += text.getText().length();
          if (text.getOriginalHTML() != null) {
            combinedInputLen += text.getOriginalHTML().length();
          }
        }
        StringBuilder combinedText = new StringBuilder(combinedTextLen);
        StringBuilder combinedInput = new StringBuilder(combinedInputLen);
        while (!textNodes.isEmpty()) {
          HtmlDocument.Text text = textNodes.removeFirst();
          combinedText.append(text.getText());
          if (text.getOriginalHTML() != null) {
            combinedInput.append(text.getOriginalHTML());
          }
        }
        String originalInput = combinedInputLen > 0 ? combinedInput.toString() : null;
        output.add(HtmlDocument.createText(combinedText.toString(), originalInput));
      }
    }
  }

  //------------------------------------------------------------------------
  // Text scanning
  //------------------------------------------------------------------------
  /**
   * A truncated entity is something like <pre>&nbs or &#1a3</pre>.
   * We only want to find these at the end of a clipped text.
   */
  private static final Pattern TRUNCATED_ENTITY =
    Pattern.compile("\\& \\#? [0-9a-zA-Z]{0,8} $", Pattern.COMMENTS);

  /**
   * In a text mode, scan for a tag
   * @param start Position in original html.
   * @param end Position in original html.
   * @return End position of scanned content.
   */
  int scanText(final int start, final int end) {
    int pos;
    for (pos = start; pos < end; pos++) {
      char ch = html.charAt(pos);
      if (ch == '<' && pos + 1 < end) {
        // Check the next char
        ch = html.charAt(pos + 1);
        if (ch == '/' || Character.isLetter(ch) || ch == '!' || ch == '?') {

          // Check if it's an html comment or tag
          if (html.regionMatches(pos + 1, "!--", 0, 3)) {
            state = State.IN_COMMENT;
          } else {
            state = State.IN_TAG;
          }
          break;
        }
      }
    }

    if (pos > start) {
      int finalPos = pos;
      String htmlTail = this.html.substring(start, finalPos);

      if ((pos == clipLength) && (clipLength < html.length())) {
        // We're clipping this HTML, not running off the end.
        // If we're ending with what looks like a truncated entity,
        // then clip that part off, too.
        // If it really was a truncated entity, great.
        // If it was a false positive, the user won't notice that we clipped
        // an additional handful of characters.
        Matcher matcher = TRUNCATED_ENTITY.matcher(htmlTail);
        if (matcher.find()) {
          int matchStart = matcher.start();
          // The matcher matched in htmlTail, not html.
          // htmlTail starts at html[start]
          finalPos = start + matchStart;
          htmlTail = htmlTail.substring(0, matchStart);
        }
      }

      if (finalPos > start) {
        String originalHtml = null;
        if (preserveAll) {
          originalHtml = htmlTail;
        } else if (preserveValidHtml) {
          // the only way htmlTail can start with '<' is if it's the last character
          // in html; otherwise, we would have entered State.IN_TAG or
          // State.IN_COMMENT above

          // officially a '<' can be valid in a text node, but to be safe we
          // always escape them
          originalHtml = CharMatcher.is('<').replaceFrom(htmlTail, "&lt;");
        }

        HtmlDocument.Text textnode = HtmlDocument.createEscapedText(htmlTail, originalHtml);
        nodes.add(textnode);
      }
    }
    return pos;
  }

  //------------------------------------------------------------------------
  // Tag name scanning utility class
  //------------------------------------------------------------------------
  private static class TagNameScanner {
    private final String html;
    private String tagName;
    private int startNamePos = -1;
    private int endNamePos = -1;

    public TagNameScanner(String html) {
      this.html = html;
    }

    /**
     * Scans for a tag name. Sets #startNamePos and #endNamePos.
     * @param start Position in original html.
     * @param end Position in original html.
     * @return End position of scanned content.
     */
    public int scanName(final int start, final int end) {
      int pos;
      for (pos = start; pos < end; pos++) {
        char ch = html.charAt(pos);

        // End of tag or end of name.
        if ((ch == '>') || (ch == '/') || Character.isWhitespace(ch)) {
          break;
        }
      }
      if (pos > start) {
        startNamePos = start;
        endNamePos = pos;
      }
      return pos;
    }

    /**
     * @return Tag name.
     */
    public String getTagName() {
      if (tagName == null && startNamePos != -1 && endNamePos != -1) {
        tagName = html.substring(startNamePos, endNamePos);
      }
      return tagName;
    }
  }

  //------------------------------------------------------------------------
  // Attribute scanning utility class
  //------------------------------------------------------------------------
  private static class AttributeScanner {
    private final String html;
    private String name;
    private String value;

    // The following have package visibility because they are accessed from
    // HtmlParser.addAttribute() to handle preservation of original content
    // around the attribute value, but quoting and escaping of the value itself.
    int startNamePos = -1;
    int endNamePos = -1;
    int startValuePos = -1;
    int endValuePos = -1;
    boolean attrValueIsQuoted = false;

    public AttributeScanner(String html) {
      this.html = html;
    }

    /**
     * Reset to scan another attribute.
     */
    public void reset() {
      startNamePos = -1;
      endNamePos = -1;
      startValuePos = -1;
      endValuePos = -1;
      attrValueIsQuoted = false;
      name = null;
      value = null;
    }

    /**
     * Scans for a tag attribute name. Sets startNamePos and endNamePos. Sets
     * 'attrName'.
     *
     * @param start Position in original html
     * @param end Position in original html
     * @return End position of scanned content
     */
    int scanName(final int start, final int end) {
      X.assertTrue(html.charAt(start) != '>');
      if (start == end) {
        // No attribute name
        return start;
      }

      int pos;
      for (pos = start + 1; pos < end; pos++) {
        char ch = html.charAt(pos);

        // End of tag or end of name.
        if ((ch == '>') || (ch == '=') || (ch == '/') || Character.isWhitespace(ch)) {
          break;
        }
      }
      startNamePos = start;
      endNamePos = pos;
      return pos;
    }

    /**
     * Scans for a tag attribute value. Sets startValuePos_ and endValuePos_.
     *
     * @param start Position in original html
     * @param end Position in original html
     * @return End position of scanned content
     */
    int scanValue(final int start, final int end) {
      // Skip whitespace before '='.
      int pos = skipSpaces(start, end);

      // Handle cases with no attribute value.
      if ((pos == end) || (html.charAt(pos) != '=')) {
        // Return start so spaces will be parsed as part of next attribute,
        // or end of tag.
        return start;
      }

      // Skip '=' and whitespace after it.
      pos++;
      pos = skipSpaces(pos, end);

      // Handle cases with no attribute value.
      if (pos == end) {
        return pos;
      }

      // Check for quote character ' or "
      char ch = html.charAt(pos);
      if (ch == '\'' || ch == '\"') {
        attrValueIsQuoted = true;
        pos++;
        int valueStart = pos;
        while (pos < end && html.charAt(pos) != ch) {
          pos++;
        }
        startValuePos = valueStart;
        endValuePos = pos;
        if (pos < end) {
          pos++;                        // Skip the ending quote char
        }
      } else {
        int valueStart = pos;
        for (; pos < end; pos++) {
          ch = html.charAt(pos);

          // End of tag or end of value. Not that '/' is included in the value
          // even if it is the '/>' at the end of the tag.
          if ((ch == '>') || Character.isWhitespace(ch)) {
            break;
          }
        }
        startValuePos = valueStart;
        endValuePos = pos;
      }

      X.assertTrue(startValuePos > -1);
      X.assertTrue(endValuePos > -1);
      X.assertTrue(startValuePos <= endValuePos);
      X.assertTrue(pos <= end);

      return pos;
    }

    /**
     * Skips white spaces.
     *
     * @param start Position in original html
     * @param end Position in original html
     * @return End position of scanned content
     */
    private int skipSpaces(final int start, final int end) {
      int pos;
      for (pos = start; pos < end; pos++) {
        if (!Character.isWhitespace(html.charAt(pos))) {
          break;
        }
      }
      return pos;
    }

    public String getName() {
      if (name == null && startNamePos != -1 && endNamePos != -1) {
        name = html.substring(startNamePos, endNamePos);
      }
      return name;
    }

    public String getValue() {
      if (value == null && startValuePos != -1 && endValuePos != -1) {
        value = html.substring(startValuePos, endValuePos);
      }
      return value;
    }
  }

  /**
   * Holds any unrecognized elements we encounter.  Only applicable in
   * PRESERVE_ALL mode.
   */
  private final HashMap<String,HTML.Element> unknownElements = Maps.newHashMap();

  /**
   * Holds any unrecognized attributes we encounter.  Only applicable in
   * PRESERVE_ALL mode.
   */
  private final HashMap<String,HTML.Attribute> unknownAttributes = Maps.newHashMap();

  /**
   * @param name Element name.
   * @return "Dummy" element.  Not useful for any real HTML processing, but
   * gives us a placeholder for tracking original HTML contents.
   */
  private HTML.Element lookupUnknownElement(String name) {
    name = name.toLowerCase();
    HTML.Element result = unknownElements.get(name);
    if (result == null) {
      result = new HTML.Element(name,
          HTML.Element.NO_TYPE,
          /* empty */ false,
          /* optional end tag */ true,
          /* breaks flow*/ false,
          HTML.Element.Flow.NONE);
      unknownElements.put(name, result);
    }
    return result;
  }

  /**
   * @param name Attribute name.
   * @return "Dummy" attribute. Not useful for any real HTML processing, but
   *         gives us a placeholder for tracking original HTML contents.
   */
  private HTML.Attribute lookupUnknownAttribute(String name) {
    name = name.toLowerCase();
    HTML.Attribute result = unknownAttributes.get(name);
    if (result == null) {
      result = new HTML.Attribute(name, HTML.Attribute.NO_TYPE);
      unknownAttributes.put(name, result);
    }
    return result;
  }

  /**
   * Scans for an HTML tag.
   *
   * @param start Position in original html.
   * @param end Position in original html.
   * @return End position of scanned content.
   */
  int scanTag(final int start, final int end) {
    X.assertTrue(html.charAt(start) == '<');

    // nameStart is where we begin scanning for the tag name and attributes,
    // so we skip '<'.
    int nameStart = start + 1;

    // Next state is Text, except the case when we see a STYLE/SCRIPT tag. See
    // code below.
    state = State.IN_TEXT;

    // End tag?
    boolean isEndTag = false;
    if (html.charAt(nameStart) == '/') {
      isEndTag = true;
      ++nameStart;
    }

    // Tag name and element
    TagNameScanner tagNameScanner = new TagNameScanner(html);
    int pos = tagNameScanner.scanName(nameStart, end);
    String tagName = tagNameScanner.getTagName();
    HTML.Element element = null;
    if (tagName == null) {
      // For some reason, browsers treat start and end tags differently
      // when they don't have a valid tag name - end tags are swallowed
      // (e.g., "</ >"), start tags treated as text (e.g., "< >")
      if (!isEndTag) {
        // This is not really a tag, treat the '<' as text.
        HtmlDocument.Text text = HtmlDocument.createText("<", preserveAll ? "<" : null);
        nodes.add(text);
        state = State.IN_TEXT;
        return nameStart;
      }

      if (preserveAll) {
        element = lookupUnknownElement("");
      }
    } else {
      element = lookupElement(tagName);
      if (element == null) {
        if (DEBUG) {
          // Unknown element
          debug("Unknown element: " + tagName);
        }
        if (preserveAll) {
          element = lookupUnknownElement(tagName);
        }
      }
    }

    // Attributes
    boolean isSingleTag = false;
    ArrayList<HtmlDocument.TagAttribute> attributes = null;
    int allAttributesStartPos = pos;
    int nextAttributeStartPos = pos;
    AttributeScanner attributeScanner = new AttributeScanner(html);
    while (pos < end) {
      int startPos = pos;
      char ch = html.charAt(pos);

      // Are we at the end of the tag?
      if ((pos + 1 < end) && (ch == '/') && (html.charAt(pos + 1) == '>')) {
        isSingleTag = true;
        ++pos;
        break;                          // Done
      }
      if (ch == '>') {
        break;                          // Done
      }

      // See bug 870742 (Buganizer).
      if (isEndTag && ('<' == ch)) {
        // '<' not allowed in end tag, so we finish processing this tag and
        // return to State.IN_TEXT. We mimic Safari & Firefox, which both
        // terminate the tag when it contains a '<'.
        if (element != null) {
          addEndTag(element, start, allAttributesStartPos, pos);
        }
        state = State.IN_TEXT;
        return pos;
      }

      if (Character.isWhitespace(ch)) {
        // White space, skip it.
        ++pos;
      } else {
        // Scan for attribute
        attributeScanner.reset();
        pos = attributeScanner.scanName(pos, end);
        X.assertTrue(pos > startPos);

        // If it's a valid attribute, scan attribute values
        if (attributeScanner.getName() != null) {
          pos = attributeScanner.scanValue(pos, end);

          // Add the attribute to the list
          if (element != null) {
            if (attributes == null) {
              attributes = new ArrayList<HtmlDocument.TagAttribute>();
            }
            addAttribute(attributes, attributeScanner, nextAttributeStartPos, pos);
          }
          nextAttributeStartPos = pos;
        }
      }

      // Make sure that we make progress!
      X.assertTrue(pos > startPos);
    }

    // Cannot find the close tag, so we treat this as text
    if (pos == end) {
      X.assertTrue(start < end);
      String textNodeContent = html.substring(start, end);
      String originalContent = null;
      if (preserveAll) {
        originalContent = textNodeContent;
      } else if (preserveValidHtml) {
        // Officially a '<' can be valid in a text node, but to be safe we
        // always escape them.
        originalContent =
            CharMatcher.is('<').replaceFrom(html.substring(start, end), "&lt;");
      }
      nodes.add(HtmlDocument.createEscapedText(textNodeContent, originalContent));
      return end;
    }

    // Skip '>'
    X.assertTrue(html.charAt(pos) == '>');
    pos++;

    // Check if it's an element we're keeping (either an HTML4 element, or an
    // unknown element we're preserving). If not, ignore the tag.
    if (element != null) {
      if (isEndTag) {
        addEndTag(element, start, allAttributesStartPos, pos);
      } else {
        // Special case: if it's a STYLE/SCRIPT element, we go to into
        // CDATA state.
        if (HTML4.SCRIPT_ELEMENT.equals(element) || HTML4.STYLE_ELEMENT.equals(element)) {
          state = State.IN_CDATA;
        }

        addStartTag(element, start, allAttributesStartPos,
            nextAttributeStartPos,
            pos, isSingleTag, attributes);
      }
    }

    return pos;
  }

  /**
   * Lookups the element in our whitelist(s). Whitelists are consulted in
   * reverse chronological order (starting from the most recently added
   * whitelist), allowing clients to override the default behavior.
   *
   * @param name Element name.
   * @return Element.
   */
  HTML.Element lookupElement(String name) {
    ListIterator<HtmlWhitelist> iter = whitelists.listIterator(whitelists.size());
    while (iter.hasPrevious()) {
      HTML.Element elem = iter.previous().lookupElement(name);
      if (elem != null) {
        return elem;
      }
    }
    return null;
  }

  /**
   * Lookups the attribute in our whitelist(s). Whitelists are consulted in
   * reverse chronological order (starting from the most recently added
   * whitelist), allowing clients to override the default behavior.
   *
   * @param name Attribute name.
   * @return Attribute.
   */
  HTML.Attribute lookupAttribute(String name) {
    ListIterator<HtmlWhitelist> iter = whitelists.listIterator(whitelists.size());
    while (iter.hasPrevious()) {
      HTML.Attribute attr = iter.previous().lookupAttribute(name);
      if (attr != null) {
        return attr;
      }
    }
    return null;
  }

  /**
   * @param element Tag element
   * @param startPos Start of tag, including '<'
   * @param startAttributesPos Start of attributes. This is the first
   * character after the tag name. If there are no attributes, this is the end
   * of the tag.
   * @param endAttributesPos First position after last attribute
   * @param endPos End of tag, including '>' character
   * @param isSingleTag True iff this is a self-terminating tag
   * @param attributes Tag attributes
   */
  private void addStartTag(HTML.Element element, final int startPos,
      final int startAttributesPos, final int endAttributesPos,
      final int endPos, final boolean isSingleTag,
      ArrayList<HtmlDocument.TagAttribute> attributes) {
    X.assertTrue(startPos < startAttributesPos);
    X.assertTrue(startAttributesPos <= endAttributesPos);
    X.assertTrue(endAttributesPos <= endPos);

    if (preserveAll) {
      String beforeAttrs = html.substring(startPos, startAttributesPos);
      String afterAttrs = html.substring(endAttributesPos, endPos);
      HtmlDocument.Tag tag = (isSingleTag)
          ? HtmlDocument.createSelfTerminatingTag(element, attributes,
              beforeAttrs, afterAttrs)
          : HtmlDocument.createTag(element, attributes,
              beforeAttrs, afterAttrs);
      nodes.add(tag);
    } else if (preserveValidHtml) {
      // This is the beginning of the tag up through the tag name. It should not
      // be possible for this to contain characters needing escaping, but we add
      // this redundant check to avoid an XSS attack that might get past our
      // parser but trick a browser into executing a script.
      X.assertTrue(html.charAt(startPos) == '<');
      StringBuilder beforeAttrs = new StringBuilder("<");
      String tagName = html.substring(startPos + 1, startAttributesPos);
      beforeAttrs.append(CharEscapers.asciiHtmlEscaper().escape(tagName));

      // Verify end-of-tag characters
      int endContentPos = endPos - 1;
      X.assertTrue(html.charAt(endContentPos) == '>');
      if (isSingleTag) {
        --endContentPos;
        X.assertTrue(html.charAt(endContentPos) == '/');
      }
      X.assertTrue(endAttributesPos <= endContentPos);

      // This is any extra characters between the last attribute and the end of
      // the tag.
      X.assertTrue(endAttributesPos < endPos);
      String afterAttrs = html.substring(endAttributesPos, endPos);

      // Strip all but preceding whitespace.
      HtmlDocument.Tag tag = (isSingleTag)
          ? HtmlDocument.createSelfTerminatingTag(element, attributes,
              beforeAttrs.toString(), afterAttrs)
          : HtmlDocument.createTag(element, attributes,
              beforeAttrs.toString(), afterAttrs);
      nodes.add(tag);
    } else {
      // Normalize.
      HtmlDocument.Tag tag = (isSingleTag)
          ? HtmlDocument.createSelfTerminatingTag(element, attributes)
          : HtmlDocument.createTag(element, attributes);
      nodes.add(tag);
    }
  }

 /**
   * @param element End tag element.
   * @param startPos Start of tag, including '<'.
   * @param startAttributesPos Start of attributes. This is the first
   * character after the tag name. If there are no attributes, this is the end
   * of the tag.
   * @param endPos End of tag. This usually contains the '>' character, but in
   * the case where browsers force a termination of a malformed tag, it doesn't.
   */
  private void addEndTag(HTML.Element element, final int startPos,
      final int startAttributesPos, final int endPos) {
    X.assertTrue(element != null);
    X.assertTrue(html.charAt(startPos) == '<');
    X.assertTrue(html.charAt(startPos + 1) == '/');

    if (preserveAll) {
      // Preserve all: keep actual content even if it's malformed.
      X.assertTrue(startPos < endPos);
      String content = html.substring(startPos, endPos);
      nodes.add(HtmlDocument.createEndTag(element, content));
    } else if (preserveValidHtml) {
      // Preserve valid: terminate the tag.

      StringBuilder validContent = new StringBuilder("</");

      // This is the beginning of the tag up through the tag name. It should not
      // be possible for this to contain characters needing escaping, but we add
      // this redundant check to avoid an XSS attack that might get past our
      // parser but trick a browser into executing a script.
      X.assertTrue(startPos < startAttributesPos);
      String tagName = html.substring(startPos + 2, startAttributesPos);
      validContent.append(CharEscapers.asciiHtmlEscaper().escape(tagName));

      // This is the rest of the tag, including any attributes.
      // See bug 874396 (Buganizer). We don't allow attributes in an end tag.
      X.assertTrue(startAttributesPos <= endPos);
      String endOfTag = html.substring(startAttributesPos, endPos);
      if (endOfTag.charAt(endOfTag.length() - 1) != '>') {
        endOfTag += '>';
      }

      // Strip everything but leading whitespace.
      validContent.append(endOfTag.replaceAll("\\S+.*>", ">"));

      nodes.add(HtmlDocument.createEndTag(element, validContent.toString()));
    } else {
      // Normalize: ignore the original content.
      nodes.add(HtmlDocument.createEndTag(element));
    }
  }

  /**
   * Creates and adds an attribute to the list.
   *
   * @param attributes Destination of new attribute.
   * @param scanner Scanned attribute.
   * @param startPos start position (inclusive) in original HTML of this
   *        attribute, including preceeding separator characters (generally this
   *        is whitespace, but it might contain other characters). This is the
   *        end position of the tag name or previous attribute +1.
   * @param endPos end position (exclusive) in original HTML of this attribute.
   */
  private void addAttribute(ArrayList<HtmlDocument.TagAttribute> attributes,
      AttributeScanner scanner, final int startPos, final int endPos) {
    X.assertTrue(startPos < endPos);

    String name = scanner.getName();
    X.assertTrue(name != null);
    HTML.Attribute htmlAttribute = lookupAttribute(name);

    // This can be null when there's no value, e.g., input.checked attribute.
    String value = scanner.getValue();

    if (htmlAttribute == null) {
      // Unknown attribute.
      if (DEBUG) {
        debug("Unknown attribute: " + name);
      }
      if (preserveAll) {
        String original = html.substring(startPos, endPos);
        attributes.add(HtmlDocument.createTagAttribute(
            lookupUnknownAttribute(name), value, original));
      }
    } else {
      String unescapedValue = (value == null) ? null : StringUtil.unescapeHTML(value);
      if (preserveAll) {
        attributes.add(HtmlDocument.createTagAttribute(htmlAttribute,
            unescapedValue, html.substring(startPos, endPos)));
      } else if (preserveValidHtml) {
        StringBuilder original = new StringBuilder();

        // This includes any separator characters between the tag name or
        // preceding attribute and this one.
        // This addresses bugs 870757 and 875303 (Buganizer).
        // Don't allow non-whitespace separators between attributes.
        X.assertTrue(startPos <= scanner.startNamePos);
        String originalPrefix = html.substring(
            startPos, scanner.startNamePos).replaceAll("\\S+", "");
        if (originalPrefix.length() == 0) {
          originalPrefix = " ";
        }
        original.append(originalPrefix);

        if (value == null) {
          // This includes the name and any following whitespace. Escape in case
          // the name has any quotes or '<' that could confuse a browser.
          X.assertTrue(scanner.startNamePos < endPos);
          String nameEtc = html.substring(scanner.startNamePos, endPos);
          original.append(CharEscapers.asciiHtmlEscaper().escape(nameEtc));
        } else {
          // Escape name in case the name has any quotes or '<' that could
          // confuse a browser.
          original.append(CharEscapers.asciiHtmlEscaper().escape(name));

          // This includes the equal sign, and any other whitespace
          // between the name and value. It also contains the opening quote
          // character if there is one.
          X.assertTrue(scanner.endNamePos < scanner.startValuePos);
          original.append(html.substring(scanner.endNamePos, scanner.startValuePos));

          // This is the value, excluding any quotes.
          if (scanner.attrValueIsQuoted) {
            // Officially a '<' can be valid in an attribute value, but to be
            // safe we always escape them.
            original.append(value.replaceAll("<", "&lt;"));
          } else {
            // This addresses bug 881426 (Buganizer). Put quotes around any
            // dangerous characters, which is what most of the browsers do.
            if (NEEDS_QUOTING_ATTRIBUTE_VALUE_REGEX.matcher(value).find()) {
              original.append('"');
              original.append(value.replaceAll("\"", "&quot;"));
              original.append('"');
            } else {
              original.append(value);
            }
          }

          // This includes end quote, if applicable.
          X.assertTrue(scanner.endValuePos <= endPos);
          original.append(html.substring(scanner.endValuePos, endPos));
        }

        attributes.add(HtmlDocument.createTagAttribute(
            htmlAttribute, unescapedValue, original.toString()));
      } else {
        attributes.add(HtmlDocument.createTagAttribute(
            htmlAttribute, unescapedValue));
      }
    }
  }

  //------------------------------------------------------------------------
  // Comment scanning
  //------------------------------------------------------------------------
  private static final String START_COMMENT = "<!--";
  private static final String END_COMMENT = "-->";

  private int scanComment(final int start, final int end) {

    X.assertTrue(html.regionMatches(start, START_COMMENT, 0, START_COMMENT.length()));

    // Scan for end of comment
    int pos = html.indexOf(END_COMMENT, start + START_COMMENT.length());
    if (pos != -1) {
      pos += END_COMMENT.length();
    } else {
      // Look for '>'. If we can't find that, the rest of the text is comments.
      pos = html.indexOf('>', start + 4);
      if (pos != -1) {
        ++pos;
      } else {
        pos = end;
      }
    }

    if (preserveAll) {
      nodes.add(HtmlDocument.createHtmlComment(html.substring(start, pos)));
    }

    return pos;
  }

  //------------------------------------------------------------------------
  // CDATA scanning
  //------------------------------------------------------------------------
  int scanCDATA(final int start, final int end) {

    // Get the tag: must be either STYLE or SCRIPT
    HtmlDocument.Tag tag = (HtmlDocument.Tag) nodes.get(nodes.size() - 1);
    HTML.Element element = tag.getElement();
    X.assertTrue(HTML4.SCRIPT_ELEMENT.equals(element) || HTML4.STYLE_ELEMENT.equals(element));

    int pos;
    for (pos = start; pos < end; pos++) {
      if (pos + 2 < end &&
          html.charAt(pos) == '<' &&
          html.charAt(pos + 1) == '/' &&
          html.regionMatches(true, pos + 2, element.getName(), 0,
                              element.getName().length())) {
        break;
      }
    }

    // Add a CDATA node
    if (pos > start) {
      HtmlDocument.CDATA cdata =
        HtmlDocument.createCDATA(html.substring(start, pos));
      nodes.add(cdata);
    }

    state = State.IN_TAG;
    return pos;
  }

  //------------------------------------------------------------------------
  public static void main(String[] args) throws IOException {

    DEBUG = true;

    String html = new String(ByteStreams.toByteArray(System.in), "ISO-8859-1");

    HtmlParser parser = new HtmlParser();
    HtmlDocument doc = parser.parse(html);
    System.out.println(doc.toString());
  }

  private static void debug(String str) {
    System.err.println(str);
  }
}