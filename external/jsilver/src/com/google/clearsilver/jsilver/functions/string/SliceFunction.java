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

package com.google.clearsilver.jsilver.functions.string;

import com.google.clearsilver.jsilver.functions.NonEscapingFunction;
import com.google.clearsilver.jsilver.values.Value;
import static com.google.clearsilver.jsilver.values.Value.literalConstant;
import static com.google.clearsilver.jsilver.values.Value.literalValue;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Returns the string slice starting at start and ending at end, similar to the Python slice
 * operator.
 */
public class SliceFunction extends NonEscapingFunction {

  /**
   * @param args 1 string values then 2 numeric values (start and end).
   * @return Sliced string
   */
  public Value execute(Value... args) {
    Value stringValue = args[0];
    Value startValue = args[1];
    Value endValue = args[2];
    String string = stringValue.asString();
    int start = startValue.asNumber();
    int end = endValue.asNumber();
    int length = string.length();

    if (start < 0) {
      start += max(-start, length);
      if (end == 0) {
        end = length;
      }
    }

    if (end < 0) {
      end += length;
    }

    end = min(end, length);

    if (end < start) {
      return literalConstant("", args[0]);
    }

    return literalValue(string.substring(start, end), stringValue.getEscapeMode(), stringValue
        .isPartiallyEscaped()
        || startValue.isPartiallyEscaped() || endValue.isPartiallyEscaped());
  }
}
