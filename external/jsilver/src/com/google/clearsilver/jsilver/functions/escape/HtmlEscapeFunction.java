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

package com.google.clearsilver.jsilver.functions.escape;


/**
 * This class HTML escapes a string in the same way as the ClearSilver html_escape function.
 * 
 * This implementation has been optimized for performance.
 * 
 */
public class HtmlEscapeFunction extends SimpleEscapingFunction {

  // The escape chars
  private static final char[] ESCAPE_CHARS = {'<', '>', '&', '\'', '"'};

  // UNQUOTED_ESCAPE_CHARS = ESCAPE_CHARS + UNQUOTED_EXTRA_CHARS + chars < 0x20 + 0x7f
  private static final char[] UNQUOTED_ESCAPE_CHARS;

  private static final char[] UNQUOTED_EXTRA_CHARS = {'=', ' '};

  // The corresponding escape strings for all ascii characters.
  // With control characters, we simply strip them out if necessary.
  private static String[] ESCAPE_CODES =
      {"", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
          "", "", "", "", "", "", "", "", "", "", "!", "&quot;", "#", "$", "%", "&amp;", "&#39;",
          "(", ")", "*", "+", ",", "-", ".", "/", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
          ":", ";", "&lt;", "&#61;", "&gt;", "?", "@", "A", "B", "C", "D", "E", "F", "G", "H", "I",
          "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "[",
          "\\", "]", "^", "_", "`", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l",
          "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "{", "|", "}", "~",
          ""};

  static {
    UNQUOTED_ESCAPE_CHARS = new char[33 + ESCAPE_CHARS.length + UNQUOTED_EXTRA_CHARS.length];
    // In unquoted HTML attributes, strip out control characters also, as they could
    // get interpreted as end of attribute, just like spaces.
    for (int n = 0; n <= 0x1f; n++) {
      UNQUOTED_ESCAPE_CHARS[n] = (char) n;
    }
    UNQUOTED_ESCAPE_CHARS[32] = (char) 0x7f;
    System.arraycopy(ESCAPE_CHARS, 0, UNQUOTED_ESCAPE_CHARS, 33, ESCAPE_CHARS.length);
    System.arraycopy(UNQUOTED_EXTRA_CHARS, 0, UNQUOTED_ESCAPE_CHARS, 33 + ESCAPE_CHARS.length,
        UNQUOTED_EXTRA_CHARS.length);

  }

  /**
   * isUnquoted should be true if the function is escaping a string that will appear inside an
   * unquoted HTML attribute.
   * 
   * If the string is unquoted, we strip out all characters 0 - 0x1f and 0x7f for security reasons.
   */
  public HtmlEscapeFunction(boolean isUnquoted) {
    if (isUnquoted) {
      super.setEscapeChars(UNQUOTED_ESCAPE_CHARS);
    } else {
      super.setEscapeChars(ESCAPE_CHARS);
    }
  }

  @Override
  protected String getEscapeString(char c) {
    if (c < 0x80) {
      return ESCAPE_CODES[c];
    }
    throw new IllegalArgumentException("Unexpected escape character " + c + "[" + (int) c + "]");
  }
}
