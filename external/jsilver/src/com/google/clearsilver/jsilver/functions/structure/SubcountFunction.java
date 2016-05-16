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

package com.google.clearsilver.jsilver.functions.structure;

import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.functions.NonEscapingFunction;
import com.google.clearsilver.jsilver.values.Value;
import static com.google.clearsilver.jsilver.values.Value.literalConstant;
import com.google.clearsilver.jsilver.values.VariableValue;

/**
 * Returns the number of child nodes for the HDF variable.
 */
public class SubcountFunction extends NonEscapingFunction {

  /**
   * @param args A variable value referring to an HDF node
   * @return Number of children
   */
  public Value execute(Value... args) {
    VariableValue arg = (VariableValue) args[0];
    if (arg.getReference() == null) {
      return literalConstant(0, arg);
    }

    Data thisNode = arg.getReference().getSymlink();
    return literalConstant(thisNode.getChildCount(), arg);
  }

}
