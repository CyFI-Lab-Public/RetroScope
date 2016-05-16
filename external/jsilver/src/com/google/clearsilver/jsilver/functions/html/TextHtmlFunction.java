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

package com.google.clearsilver.jsilver.functions.html;

import com.google.clearsilver.jsilver.functions.TextFilter;
import com.google.clearsilver.jsilver.functions.escape.HtmlEscapeFunction;
import com.google.clearsilver.jsilver.functions.escape.SimpleEscapingFunction;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class implements the ClearSilver text_html function.
 * 
 * It converts plain text into html, including adding 'tt' tags to ascii art and linking email and
 * web addresses.
 * 
 * Note this implementation differs from ClearSilver, in that it html escapes the contents of links
 * and mailtos.
 */
public class TextHtmlFunction implements TextFilter {

  // These regular expressions are adapted from html.c in the ClearSilver
  // source.

  // Regular expression used to match email addresses, taken from the
  // ClearSilver source to maintain compatibility.
  private static final String EMAIL_REGEXP =
      "[^]\\[@:;<>\\\"()\\s\\p{Cntrl}]+@[-+a-zA-Z0-9]+\\.[-+a-zA-Z0-9\\.]+[-+a-zA-Z0-9]";

  // Regular expression used to match urls without a scheme (www.foo.com),
  // adapted from the ClearSilver source to maintain compatibility.
  private static final String WITH_SCHEME_REGEXP = "(?:http|https|ftp|mailto):[^\\s>\"]*";

  // Regular expression used to match urls with a scheme (http://www.foo.com),
  // adapted from the ClearSilver source to maintain compatibility.
  private static final String WITHOUT_SCHEME_REGEXP = "www\\.[-a-z0-9\\.]+[^\\s;\">]*";

  // Pattern to match any string in the input that is linkable.
  private static final Pattern LINKABLES =
      Pattern.compile("(" + EMAIL_REGEXP + ")|(" + WITH_SCHEME_REGEXP + ")|("
          + WITHOUT_SCHEME_REGEXP + ")", Pattern.CASE_INSENSITIVE);

  // Matching groups for the LINKABLES pattern.
  private static final int EMAIL_GROUP = 1;
  private static final int WITH_SCHEME_GROUP = 2;

  // We don't have access to the global html escaper here, so create a new one.
  private final HtmlEscapeFunction htmlEscaper = new HtmlEscapeFunction(false);

  // Escapes a small set of non-safe html characters, and does a a very small
  // amount of formatting.
  private final SimpleEscapingFunction htmlCharEscaper =
      new SimpleEscapingFunction(new char[] {'<', '>', '&', '\n', '\r'}) {

        @Override
        protected String getEscapeString(char c) {
          switch (c) {
            case '<':
              return "&lt;";
            case '>':
              return "&gt;";
            case '&':
              return "&amp;";
            case '\n':
              return "<br/>\n";
            case '\r':
              return "";
            default:
              return null;
          }
        }

      };

  @Override
  public void filter(String in, Appendable out) throws IOException {

    boolean hasAsciiArt = hasAsciiArt(in);

    // Add 'tt' tag to a string that contains 'ascii-art'.
    if (hasAsciiArt) {
      out.append("<tt>");
    }

    splitAndConvert(in, out);

    if (hasAsciiArt) {
      out.append("</tt>");
    }
  }

  /**
   * Splits the input string into blocks of normal text or linkable text. The linkable text is
   * converted into anchor tags before being appended to the output. The normal text is escaped and
   * appended to the output.
   */
  private void splitAndConvert(String in, Appendable out) throws IOException {
    Matcher matcher = LINKABLES.matcher(in);
    int end = in.length();
    int matchStart;
    int matchEnd;
    int regionStart = 0;

    // Keep looking for email addresses and web links until there are none left.
    while (matcher.find()) {
      matchStart = matcher.start();
      matchEnd = matcher.end();

      // Escape all the text from the end of the previous match to the start of
      // this match, and append it to the output.
      htmlCharEscaper.filter(in.subSequence(regionStart, matchStart).toString(), out);

      // Don't include a . or , in the text that is linked.
      if (in.charAt(matchEnd - 1) == ',' || in.charAt(matchEnd - 1) == '.') {
        matchEnd--;
      }

      if (matcher.group(EMAIL_GROUP) != null) {
        formatEmail(in, matchStart, matchEnd, out);
      } else {
        formatUrl(in, matchStart, matchEnd,
        // Add a scheme if the one wasn't found.
            matcher.group(WITH_SCHEME_GROUP) == null, out);
      }

      regionStart = matchEnd;
    }

    // Escape the text after the last match, and append it to the output.
    htmlCharEscaper.filter(in.substring(regionStart, end), out);
  }

  /**
   * Formats the input sequence into a suitable mailto: anchor tag and appends it to the output.
   * 
   * @param in The string that contains the email.
   * @param start The start of the email address in the whole string.
   * @param end The end of the email in the whole string.
   * @param out The text output that the email address should be appended to.
   * @throws IOException
   */
  private void formatEmail(String in, int start, int end, Appendable out) throws IOException {

    String emailPart = in.substring(start, end);

    out.append("<a href=\"mailto:");
    htmlEscaper.filter(emailPart, out);
    out.append("\">");
    htmlEscaper.filter(emailPart, out);
    out.append("</a>");
  }

  /**
   * Formats the input sequence into a suitable anchor tag and appends it to the output.
   * 
   * @param in The string that contains the url.
   * @param start The start of the url in the containing string.
   * @param end The end of the url in the containing string.
   * @param addScheme true if 'http://' should be added to the anchor.
   * @param out The text output that the url should be appended to.
   * @throws IOException
   */
  private void formatUrl(String in, int start, int end, boolean addScheme, Appendable out)
      throws IOException {

    String urlPart = in.substring(start, end);

    out.append(" <a target=\"_blank\" href=\"");
    if (addScheme) {
      out.append("http://");
    }
    htmlEscaper.filter(urlPart, out);
    out.append("\">");
    htmlEscaper.filter(urlPart, out);
    out.append("</a>");
  }

  /**
   * Attempts to detect if a string contains ascii art, whitespace such as tabs will suppress ascii
   * art detection.
   * 
   * This method takes its conditions from ClearSilver to maintain compatibility. See
   * has_space_formatting in html.c in the ClearSilver source.
   * 
   * @param in The string to analyze for ascii art.
   * @return true if it is believed that the string contains ascii art.
   */
  private boolean hasAsciiArt(String in) {
    int spaces = 0;
    int returns = 0;
    int asciiArt = 0;
    int x = 0;
    char[] inChars = in.toCharArray();

    int length = in.length();
    for (x = 0; x < length; x++) {

      switch (inChars[x]) {
        case '\t':
          return false;

        case '\r':
          break;

        case ' ':
          // Ignore spaces after full stops.
          if (x == 0 || inChars[x - 1] != '.') {
            spaces++;
          }
          break;

        case '\n':
          spaces = 0;
          returns++;
          break;

        // Characters to count towards the art total.
        case '/':
        case '\\':
        case '<':
        case '>':
        case ':':
        case '[':
        case ']':
        case '!':
        case '@':
        case '#':
        case '$':
        case '%':
        case '^':
        case '&':
        case '*':
        case '(':
        case ')':
        case '|':
          asciiArt++;
          if (asciiArt > 3) {
            return true;
          }
          break;

        default:
          if (returns > 2) {
            return false;
          }
          if (spaces > 2) {
            return false;
          }
          returns = 0;
          spaces = 0;
          asciiArt = 0;
          break;
      }
    }

    return false;
  }
}
