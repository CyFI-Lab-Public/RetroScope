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

package com.google.clearsilver.jsilver.functions.bundles;

import com.google.clearsilver.jsilver.functions.escape.*;
import com.google.clearsilver.jsilver.functions.html.CssUrlValidateFunction;
import com.google.clearsilver.jsilver.functions.html.HtmlStripFunction;
import com.google.clearsilver.jsilver.functions.html.HtmlUrlValidateFunction;
import com.google.clearsilver.jsilver.functions.html.TextHtmlFunction;
import com.google.clearsilver.jsilver.functions.numeric.AbsFunction;
import com.google.clearsilver.jsilver.functions.numeric.MaxFunction;
import com.google.clearsilver.jsilver.functions.numeric.MinFunction;
import com.google.clearsilver.jsilver.functions.string.CrcFunction;
import com.google.clearsilver.jsilver.functions.string.FindFunction;
import com.google.clearsilver.jsilver.functions.string.LengthFunction;
import com.google.clearsilver.jsilver.functions.string.SliceFunction;
import com.google.clearsilver.jsilver.functions.structure.FirstFunction;
import com.google.clearsilver.jsilver.functions.structure.LastFunction;
import com.google.clearsilver.jsilver.functions.structure.SubcountFunction;

/**
 * Set of functions required to allow JSilver to be compatible with ClearSilver.
 */
public class ClearSilverCompatibleFunctions extends CoreOperators {

  @Override
  protected void setupDefaultFunctions() {
    super.setupDefaultFunctions();

    // Structure functions.
    registerFunction("subcount", new SubcountFunction());
    registerFunction("first", new FirstFunction());
    registerFunction("last", new LastFunction());

    // Deprecated - but here for ClearSilver compatibility.
    registerFunction("len", new SubcountFunction());

    // Numeric functions.
    registerFunction("abs", new AbsFunction());
    registerFunction("max", new MaxFunction());
    registerFunction("min", new MinFunction());

    // String functions.
    registerFunction("string.slice", new SliceFunction());
    registerFunction("string.find", new FindFunction());
    registerFunction("string.length", new LengthFunction());
    registerFunction("string.crc", new CrcFunction());

    // Escaping functions.
    registerFunction("url_escape", new UrlEscapeFunction("UTF-8"), true);
    registerEscapeMode("url", new UrlEscapeFunction("UTF-8"));
    registerFunction("html_escape", new HtmlEscapeFunction(false), true);
    registerEscapeMode("html", new HtmlEscapeFunction(false));
    registerFunction("js_escape", new JsEscapeFunction(false), true);
    registerEscapeMode("js", new JsEscapeFunction(false));

    // These functions are available as arguments to <?cs escape: ?>
    // though they aren't in ClearSilver. This is so that auto escaping
    // can automatically add <?cs escape ?> nodes with these modes
    registerEscapeMode("html_unquoted", new HtmlEscapeFunction(true));
    registerEscapeMode("js_attr_unquoted", new JsEscapeFunction(true));
    registerEscapeMode("js_check_number", new JsValidateUnquotedLiteral());
    registerEscapeMode("url_validate_unquoted", new HtmlUrlValidateFunction(true));

    registerEscapeMode("css", new StyleEscapeFunction(false));
    registerEscapeMode("css_unquoted", new StyleEscapeFunction(true));

    // HTML functions.
    registerFunction("html_strip", new HtmlStripFunction());
    registerFunction("text_html", new TextHtmlFunction());

    // url_validate is available as an argument to <?cs escape: ?>
    // though it isn't in ClearSilver.
    registerFunction("url_validate", new HtmlUrlValidateFunction(false), true);
    registerEscapeMode("url_validate", new HtmlUrlValidateFunction(false));

    registerFunction("css_url_validate", new CssUrlValidateFunction(), true);
    // Register as an EscapingFunction so that autoescaping will be disabled
    // for the output of this function.
    registerFunction("null_escape", new NullEscapeFunction(), true);
  }

}
