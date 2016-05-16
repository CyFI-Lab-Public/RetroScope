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

import com.google.clearsilver.jsilver.functions.TextFilter;

import java.io.IOException;

/**
 * This function will be used to sanitize variables in 'style' attributes. It strips out any
 * characters that are not part of a whitelist of safe characters. This replicates the autoescaping
 * behavior of Clearsilver.
 * 
 * It does not extend SimpleEscapingFunction because SimpleEscapingFunction requires a blacklist of
 * characters to escape. The StyleAttrEscapeFunction instead applies a whitelist, and strips out any
 * characters not in the whitelist.
 */
public class StyleEscapeFunction implements TextFilter {

  private static final boolean[] UNQUOTED_VALID_CHARS;
  private static final boolean[] VALID_CHARS;
  private static final int MAX_CHARS = 0x80;

  static {
    // Allow characters likely to occur inside a style property value.
    // Refer http://www.w3.org/TR/CSS21/ for more details.
    String SPECIAL_CHARS = "_.,!#%- ";
    String UNQUOTED_SPECIAL_CHARS = "_.,!#%-";

    VALID_CHARS = new boolean[MAX_CHARS];
    UNQUOTED_VALID_CHARS = new boolean[MAX_CHARS];

    for (int n = 0; n < MAX_CHARS; n++) {
      VALID_CHARS[n] = false;
      UNQUOTED_VALID_CHARS[n] = false;

      if (Character.isLetterOrDigit(n)) {
        VALID_CHARS[n] = true;
        UNQUOTED_VALID_CHARS[n] = true;
      } else {
        if (SPECIAL_CHARS.indexOf(n) != -1) {
          VALID_CHARS[n] = true;
        }

        if (UNQUOTED_SPECIAL_CHARS.indexOf(n) != -1) {
          UNQUOTED_VALID_CHARS[n] = true;
        }
      }
    }
  }

  private final boolean[] validChars;

  /**
   * isUnquoted should be true if the function is escaping a string that will appear inside an
   * unquoted style attribute.
   * 
   */
  public StyleEscapeFunction(boolean isUnquoted) {
    if (isUnquoted) {
      validChars = UNQUOTED_VALID_CHARS;
    } else {
      validChars = VALID_CHARS;
    }
  }

  public void filter(String in, Appendable out) throws IOException {
    for (char c : in.toCharArray()) {
      if (c < MAX_CHARS && validChars[c]) {
        out.append(c);
      } else if (c >= MAX_CHARS) {
        out.append(c);
      }
    }
  }

  public void dumpInfo() {
    for (int i = 0; i < MAX_CHARS; i++) {
      System.out.println(i + "(" + (char) i + ")" + " :" + VALID_CHARS[i]);
    }
  }
}
