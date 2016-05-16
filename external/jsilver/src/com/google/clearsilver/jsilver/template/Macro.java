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

package com.google.clearsilver.jsilver.template;

import com.google.clearsilver.jsilver.exceptions.JSilverInterpreterException;

/**
 * An executable macro. This exhibits all the same characteristics of a Template.
 */
public interface Macro extends Template {

  /**
   * Name of macro (e.g. showTable). Used to generate error messages.
   */
  String getMacroName();

  /**
   * Get the name of the nth argument defined in the macro. Throws exception if the argument is not
   * found.
   */
  String getArgumentName(int index) throws JSilverInterpreterException;

  /**
   * Return the number of arguments this macro expects. Must be equal to the number of arguments
   * supplied.
   */
  int getArgumentCount();
}
