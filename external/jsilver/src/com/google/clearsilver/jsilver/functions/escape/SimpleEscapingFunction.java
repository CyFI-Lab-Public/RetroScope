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
 * Base class to make writing fast, simple escaping functions easy. A simple escaping function is
 * one where each character in the input is treated independently and there is no runtime state. The
 * only decision you make is whether the current character should be escaped into some different
 * string or not.
 * 
 * The only serious limitation on using this class it that only low valued characters can be
 * escaped. This is because (for speed) we use an array of escaped strings, indexed by character
 * value. In future this limitation may be lifted if there's a call for it.
 */
public abstract class SimpleEscapingFunction implements TextFilter {
  // The limit for how many strings we can store here (max)
  private static final int CHAR_INDEX_LIMIT = 256;

  // Our fast lookup array of escaped strings. This array is indexed by char
  // value so it's important not to have it grow too large. For now we have
  // an artificial limit on it.
  private String[] ESCAPE_STRINGS;

  /**
   * Creates an instance to escape the given set of characters.
   */
  protected SimpleEscapingFunction(char[] ESCAPE_CHARS) {
    setEscapeChars(ESCAPE_CHARS);
  }

  protected SimpleEscapingFunction() {
    ESCAPE_STRINGS = new String[0];
  }

  protected void setEscapeChars(char[] ESCAPE_CHARS) throws AssertionError {
    int highestChar = -1;
    for (char c : ESCAPE_CHARS) {
      if (c > highestChar) {
        highestChar = c;
      }
    }
    if (highestChar >= CHAR_INDEX_LIMIT) {
      throw new AssertionError("Cannot escape characters with values above " + CHAR_INDEX_LIMIT);
    }
    ESCAPE_STRINGS = new String[highestChar + 1];
    for (char c : ESCAPE_CHARS) {
      ESCAPE_STRINGS[c] = getEscapeString(c);
    }
  }

  /**
   * Given one of the escape characters supplied to this instance's constructor, return the escape
   * string for it. This method does not need to be efficient.
   */
  protected abstract String getEscapeString(char c);

  /**
   * Algorithm is as follows:
   * <ol>
   * <li>Scan block for contiguous unescaped sequences
   * <li>Append unescaped sequences to output
   * <li>Append escaped string to output (if found)
   * <li>Rinse &amp; Repeat
   * </ol>
   */
  @Override
  public void filter(String in, Appendable out) throws IOException {
    final int len = in.length();
    int pos = 0;
    int start = pos;
    while (pos < len) {
      // We really hope that the hotspot compiler inlines this call properly
      // (without optimization it accounts for > 50% of the time in this call)
      final char chr = in.charAt(pos);
      final String escapeString;
      if (chr < ESCAPE_STRINGS.length && (escapeString = ESCAPE_STRINGS[chr]) != null) {
        // We really hope our appendable handles sub-strings nicely
        // (we know that StringBuilder / StringBuffer does).
        if (pos > start) {
          out.append(in, start, pos);
        }
        out.append(escapeString);
        pos += 1;
        start = pos;
        continue;
      }
      pos += 1;
    }
    if (pos > start) {
      out.append(in, start, pos);
    }
  }
}
