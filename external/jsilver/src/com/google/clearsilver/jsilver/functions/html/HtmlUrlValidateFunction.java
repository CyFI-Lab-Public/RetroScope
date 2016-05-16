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

import com.google.clearsilver.jsilver.functions.escape.HtmlEscapeFunction;

import java.io.IOException;

/**
 * Validates that a given string is a valid URI and return the HTML escaped string if it is.
 * Otherwise the string {@code #} is returned.
 * 
 * @see BaseUrlValidateFunction
 */
public class HtmlUrlValidateFunction extends BaseUrlValidateFunction {

  private final HtmlEscapeFunction htmlEscape;

  /**
   * isUnquoted should be true if the URL appears in an unquoted attribute. like: &lt;a href=&lt;?cs
   * var: uri ?&gt;&gt;
   */
  public HtmlUrlValidateFunction(boolean isUnquoted) {
    htmlEscape = new HtmlEscapeFunction(isUnquoted);
  }

  protected void applyEscaping(String in, Appendable out) throws IOException {
    htmlEscape.filter(in, out);
  }
}
