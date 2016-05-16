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

package com.google.streamhtmlparser;

import com.google.streamhtmlparser.impl.JavascriptParserImpl;

/**
 * A factory class to obtain instances of an <code>JavascriptParser</code>.
 * Currently each instance is a new object given these are fairly
 * light-weight.
 *
 * <p>Note that we do not typically expect a caller of this package to require
 * an instance of a <code>JavascriptParser</code> since one is already
 * embedded in the more general-purpose <code>HtmlParser</code>. We still
 * make it possible to require one as it may be useful for more
 * specialized needs.
 *
 */

public class JavascriptParserFactory {

  public static JavascriptParser getInstance() {
    return new JavascriptParserImpl();
  }

  // Static class.
  private JavascriptParserFactory() {
  }  // COV_NF_LINE
}