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

import java.io.IOException;

/**
 * Validates that input string is a valid URI. If it is not valid, the string {@code #} is returned.
 * If it is valid, the characters [\n\r\\'"()<>*] are URL encoded to ensure the string can be safely
 * inserted in a CSS URL context. In particular:
 * <ol>
 * <li>In an '@import url("URL");' statement
 * <li>In a CSS property such as 'background: url("URL");'
 * </ol>
 * In both cases, enclosing quotes are optional but parenthesis are not. This filter ensures that
 * the URL cannot exit the parens enclosure, close a STYLE tag or reset the browser's CSS parser
 * (via comments or newlines).
 * <p>
 * References:
 * <ol>
 * <li>CSS 2.1 URLs: http://www.w3.org/TR/CSS21/syndata.html#url
 * <li>CSS 1 URLs: http://www.w3.org/TR/REC-CSS1/#url
 * </ol>
 * 
 * @see BaseUrlValidateFunction
 */
public class CssUrlValidateFunction extends BaseUrlValidateFunction {

  protected void applyEscaping(String in, Appendable out) throws IOException {
    for (int i = 0; i < in.length(); i++) {
      char ch = in.charAt(i);
      switch (ch) {
        case '\n':
          out.append("%0A");
          break;
        case '\r':
          out.append("%0D");
          break;
        case '"':
          out.append("%22");
          break;
        case '\'':
          out.append("%27");
          break;
        case '(':
          out.append("%28");
          break;
        case ')':
          out.append("%29");
          break;
        case '*':
          out.append("%2A");
          break;
        case '<':
          out.append("%3C");
          break;
        case '>':
          out.append("%3E");
          break;
        case '\\':
          out.append("%5C");
          break;
        default:
          out.append(ch);
      }
    }
  }

}
