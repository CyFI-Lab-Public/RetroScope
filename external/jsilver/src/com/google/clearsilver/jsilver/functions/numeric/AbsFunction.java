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

package com.google.clearsilver.jsilver.functions.numeric;

import com.google.clearsilver.jsilver.functions.NonEscapingFunction;
import com.google.clearsilver.jsilver.values.Value;
import static com.google.clearsilver.jsilver.values.Value.literalConstant;

import static java.lang.Math.abs;

/**
 * Returns the absolute value of the numeric expressions.
 */
public class AbsFunction extends NonEscapingFunction {

  /**
   * @param args Single numeric value
   * @return Absolute value
   */
  public Value execute(Value... args) {
    Value arg = args[0];
    return literalConstant(abs(arg.asNumber()), arg);
  }

}
