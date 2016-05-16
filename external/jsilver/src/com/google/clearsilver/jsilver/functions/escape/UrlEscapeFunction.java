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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * This URL encodes the string. This converts characters such as ?, ampersands, and = into their URL
 * safe equivilants using the %hh syntax.
 */
public class UrlEscapeFunction implements TextFilter {

  private final String encoding;

  public UrlEscapeFunction(String encoding) {
    try {
      // Sanity check. Fail at construction time rather than render time.
      new OutputStreamWriter(new ByteArrayOutputStream(), encoding);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Unsupported encoding : " + encoding);
    }
    this.encoding = encoding;
  }

  @Override
  public void filter(String in, Appendable out) throws IOException {
    try {
      out.append(URLEncoder.encode(in, encoding));
    } catch (UnsupportedEncodingException e) {
      // The sanity check in the constructor should have caught this.
      // Things must be really broken for this to happen, so throw an Error.
      throw new Error("Unsuported encoding : " + encoding);
    }
  }

}
