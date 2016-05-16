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
 * This function will be used to sanitize variables introduced into javascript that are not string
 * literals. e.g. <script> var x = <?cs var: x ?> </script>
 * 
 * Currently it only accepts boolean and numeric literals. All other values are replaced with a
 * 'null'. This behavior may be extended if required at a later time. This replicates the
 * autoescaping behavior of Clearsilver.
 */
public class JsValidateUnquotedLiteral implements TextFilter {

  public void filter(String in, Appendable out) throws IOException {
    /* Permit boolean literals */
    if (in.equals("true") || in.equals("false")) {
      out.append(in);
      return;
    }

    boolean valid = true;
    if (in.startsWith("0x") || in.startsWith("0X")) {

      /*
       * There must be at least one hex digit after the 0x for it to be valid. Hex number. Check
       * that it is of the form 0(x|X)[0-9A-Fa-f]+
       */
      for (int i = 2; i < in.length(); i++) {
        char c = in.charAt(i);
        if (!((c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || (c >= '0' && c <= '9'))) {
          valid = false;
          break;
        }
      }
    } else {
      /*
       * Must be a base-10 (or octal) number. Check that it has the form [0-9+-.eE]+
       */
      for (int i = 0; i < in.length(); i++) {
        char c = in.charAt(i);
        if (!((c >= '0' && c <= '9') || c == '+' || c == '-' || c == '.' || c == 'e' || c == 'E')) {
          valid = false;
          break;
        }
      }
    }

    if (valid) {
      out.append(in);
    } else {
      out.append("null");
    }
  }

}
