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

import java.io.IOException;
import java.lang.Character.UnicodeBlock;

/**
 * Validates that a given string is either something that looks like a relative URI, or looks like
 * an absolute URI using one of a set of allowed schemes (http, https, ftp, mailto). If the string
 * is valid according to these criteria, the string is escaped with an appropriate escaping
 * function. Otherwise, the string "#" is returned.
 * 
 * Subclasses will apply the necessary escaping function to the string by overriding {@code
 * applyEscaping}.
 * 
 * <p>
 * Note: this function does <em>not</em> validate that the URI is well-formed beyond the scheme part
 * (and if the URI appears to be relative, not even then). Note in particular that this function
 * considers strings of the form "www.google.com:80" to be invalid.
 */
public abstract class BaseUrlValidateFunction implements TextFilter {

  @Override
  public void filter(String in, Appendable out) throws IOException {
    if (!isValidUri(in)) {
      out.append('#');
      return;
    }
    applyEscaping(in, out);
  }

  /**
   * Called by {@code filter} after verifying that the input is a valid URI. Should apply any
   * appropriate escaping to the input string.
   * 
   * @throws IOException
   */
  protected abstract void applyEscaping(String in, Appendable out) throws IOException;

  /**
   * @return true if a given string either looks like a relative URI, or like an absolute URI with
   *         an allowed scheme.
   */
  protected boolean isValidUri(String in) {
    // Quick check for the allowed absolute URI schemes.
    String maybeScheme = toLowerCaseAsciiOnly(in.substring(0, Math.min(in.length(), 8)));
    if (maybeScheme.startsWith("http://") || maybeScheme.startsWith("https://")
        || maybeScheme.startsWith("ftp://") || maybeScheme.startsWith("mailto:")) {
      return true;
    }

    // If it's an absolute URI with a different scheme, it's invalid.
    // ClearSilver defines an absolute URI as one that contains a colon prior
    // to any slash.
    int slashPos = in.indexOf('/');
    if (slashPos != -1) {
      // only colons before this point are bad.
      return in.lastIndexOf(':', slashPos - 1) == -1;
    } else {
      // then any colon is bad.
      return in.indexOf(':') == -1;
    }
  }

  /**
   * Converts an ASCII string to lowercase. Non-ASCII characters are replaced with '?'.
   */
  private String toLowerCaseAsciiOnly(String string) {
    char[] ca = string.toCharArray();
    for (int i = 0; i < ca.length; i++) {
      char ch = ca[i];
      ca[i] =
          (Character.UnicodeBlock.of(ch) == UnicodeBlock.BASIC_LATIN)
              ? Character.toLowerCase(ch)
              : '?';
    }
    return new String(ca);
  }
}
