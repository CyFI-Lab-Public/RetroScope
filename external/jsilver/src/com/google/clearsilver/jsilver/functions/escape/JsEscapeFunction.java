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
 * This Javascript escapes the string so it will be valid data for placement into a Javascript
 * string. This converts characters such as ", ', and \ into their Javascript string safe
 * equivilants \", \', and \\.
 * 
 * This behaves in the same way as the ClearSilver js_escape function.
 * 
 * This implementation has been optimized for performance.
 */
public class JsEscapeFunction extends SimpleEscapingFunction {

  private static final char[] DIGITS = "0123456789ABCDEF".toCharArray();

  private static final char[] ESCAPE_CHARS;

  private static final char[] UNQUOTED_ESCAPE_CHARS;

  static {
    char[] SPECIAL_CHARS = {'/', '"', '\'', '\\', '>', '<', '&', ';'};
    char[] UNQUOTED_SPECIAL_CHARS = {'/', '"', '\'', '\\', '>', '<', '&', ';', '=', ' '};

    ESCAPE_CHARS = new char[32 + SPECIAL_CHARS.length];
    UNQUOTED_ESCAPE_CHARS = new char[33 + UNQUOTED_SPECIAL_CHARS.length];
    for (int n = 0; n < 32; n++) {
      ESCAPE_CHARS[n] = (char) n;
      UNQUOTED_ESCAPE_CHARS[n] = (char) n;
    }

    System.arraycopy(SPECIAL_CHARS, 0, ESCAPE_CHARS, 32, SPECIAL_CHARS.length);

    UNQUOTED_ESCAPE_CHARS[32] = 0x7F;
    System.arraycopy(UNQUOTED_SPECIAL_CHARS, 0, UNQUOTED_ESCAPE_CHARS, 33,
        UNQUOTED_SPECIAL_CHARS.length);
  }

  /**
   * isUnquoted should be true if the function is escaping a string that will appear inside an
   * unquoted JS attribute (like onClick or onMouseover).
   * 
   */
  public JsEscapeFunction(boolean isAttrUnquoted) {
    if (isAttrUnquoted) {
      super.setEscapeChars(UNQUOTED_ESCAPE_CHARS);
    } else {
      super.setEscapeChars(ESCAPE_CHARS);
    }
  }

  @Override
  protected String getEscapeString(char c) {
    return "\\x" + DIGITS[(c >> 4) & 0xF] + DIGITS[c & 0xF];
  }
}
