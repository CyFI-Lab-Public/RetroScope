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

import com.google.clearsilver.jsilver.exceptions.JSilverAutoEscapingException;

public enum EscapeMode {
  ESCAPE_NONE("none", false), ESCAPE_HTML("html", false), ESCAPE_JS("js", false), ESCAPE_URL("url",
      false), ESCAPE_IS_CONSTANT("constant", false),

  // These modes are used as starting modes, and a parser parses the
  // subsequent template contents to determine the right escaping command to use.
  ESCAPE_AUTO("auto", true), // Identical to ESCAPE_AUTO_HTML
  ESCAPE_AUTO_HTML("auto_html", true), ESCAPE_AUTO_JS("auto_js", true), ESCAPE_AUTO_JS_UNQUOTED(
      "auto_js_unquoted", true), ESCAPE_AUTO_STYLE("auto_style", true), ESCAPE_AUTO_ATTR(
      "auto_attr", true), ESCAPE_AUTO_UNQUOTED_ATTR("auto_attr_unquoted", true), ESCAPE_AUTO_ATTR_URI(
      "auto_attr_uri", true), ESCAPE_AUTO_UNQUOTED_ATTR_URI("auto_attr_uri_unquoted", true), ESCAPE_AUTO_ATTR_URI_START(
      "auto_attr_uri_start", true), ESCAPE_AUTO_UNQUOTED_ATTR_URI_START(
      "auto_attr_uri_start_unquoted", true), ESCAPE_AUTO_ATTR_JS("auto_attr_js", true), ESCAPE_AUTO_ATTR_UNQUOTED_JS(
      "auto_attr_unquoted_js", true), ESCAPE_AUTO_UNQUOTED_ATTR_JS("auto_attr_js_unquoted", true), ESCAPE_AUTO_UNQUOTED_ATTR_UNQUOTED_JS(
      "auto_attr_js_unquoted_js", true), ESCAPE_AUTO_ATTR_CSS("auto_attr_style", true), ESCAPE_AUTO_UNQUOTED_ATTR_CSS(
      "auto_attr_style_unquoted", true);

  private String escapeCmd;
  private boolean autoEscaper;

  private EscapeMode(String escapeCmd, boolean autoEscaper) {
    this.escapeCmd = escapeCmd;
    this.autoEscaper = autoEscaper;
  }

  /**
   * This function maps the type of escaping requested (escapeCmd) to the appropriate EscapeMode. If
   * no explicit escaping is requested, but doAutoEscape is true, the function chooses auto escaping
   * (EscapeMode.ESCAPE_AUTO). This mirrors the behaviour of ClearSilver.
   * 
   * @param escapeCmd A string indicating type of escaping requested.
   * @param doAutoEscape Whether auto escaping should be applied if escapeCmd is null. Corresponds
   *        to the Config.AutoEscape HDF variable.
   * @return
   */
  public static EscapeMode computeEscapeMode(String escapeCmd, boolean doAutoEscape) {
    EscapeMode escapeMode;

    // If defined, the explicit escaping mode (configured using "Config.VarEscapeMode")
    // takes preference over auto escaping
    if (escapeCmd != null) {
      for (EscapeMode e : EscapeMode.values()) {
        if (e.escapeCmd.equals(escapeCmd)) {
          return e;
        }
      }
      throw new JSilverAutoEscapingException("Invalid escaping mode specified: " + escapeCmd);

    } else {
      if (doAutoEscape) {
        escapeMode = ESCAPE_AUTO;
      } else {
        escapeMode = ESCAPE_NONE;
      }
      return escapeMode;
    }
  }

  /**
   * Calls {@link #computeEscapeMode(String, boolean)} with {@code doAutoEscape = false}.
   * 
   * @param escapeCmd A string indicating type of escaping requested.
   * @return EscapeMode
   * @throws JSilverAutoEscapingException if {@code escapeCmd} is not recognized.
   */
  public static EscapeMode computeEscapeMode(String escapeCmd) {
    return computeEscapeMode(escapeCmd, false);
  }

  /**
   * Computes the EscapeMode of the result of concatenating two values. The EscapeModes of the two
   * values are provided by {@code left} and {@code right} respectively. For now, if either of the
   * values was escaped or a constant, we return {@code ESCAPE_IS_CONSTANT}. This is how ClearSilver
   * behaves.
   * 
   * @return {@code ESCAPE_NONE} if either of the values was not escaped or constant. {@code
   *         ESCAPE_IS_CONSTANT} otherwise.
   */
  public static EscapeMode combineModes(EscapeMode left, EscapeMode right) {
    if (left.equals(ESCAPE_NONE) || right.equals(ESCAPE_NONE)) {
      // If either of the values has not been escaped,
      // do not trust the result.
      return ESCAPE_NONE;
    } else {
      // For now, indicate that this result is always safe in all contexts.
      // This is what ClearSilver does. We may introduce a stricter autoescape
      // rule later on which also requires that the escaping be the same as the
      // context its used in.
      return ESCAPE_IS_CONSTANT;
    }
  }

  public boolean isAutoEscapingMode() {
    return autoEscaper;
  }

  // TODO: Simplify enum names, and just use toString() instead.
  public String getEscapeCommand() {
    return escapeCmd;
  }
}
