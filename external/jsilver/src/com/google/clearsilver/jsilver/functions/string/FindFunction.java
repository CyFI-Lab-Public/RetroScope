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

/**
 * Returns the numeric position of the substring in the string (if found), otherwise returns -1
 * similar to the Python string.find method.
 */
public class FindFunction extends NonEscapingFunction {

  /**
   * @param args 2 string expressions (full string and substring)
   * @return Position of the start of substring (or -1 if not found) as number value
   */
  public Value execute(Value... args) {
    Value fullStringValue = args[0];
    Value subStringValue = args[1];
    return literalConstant(fullStringValue.asString().indexOf(subStringValue.asString()),
        fullStringValue, subStringValue);
  }
}
