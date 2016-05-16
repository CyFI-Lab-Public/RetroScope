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

package com.google.clearsilver.jsilver.data;

import com.google.clearsilver.jsilver.autoescape.EscapeMode;

/**
 * Manages the global Data object and local variable mappings during rendering.
 */
public interface DataContext {

  /**
   * Returns the main Data object this RenderingContext was defined with.
   */
  Data getRootData();

  /**
   * Creates a new Data object to hold local references, pushes it onto the variable map stack. This
   * is used to hold macro parameters after a call command, and local variables defined for 'each'
   * and 'with'.
   */
  void pushVariableScope();

  /**
   * Removes the most recent Data object added to the local variable map stack.
   */
  void popVariableScope();

  /**
   * Creates and sets a local variable in the current scope equal to the given value.
   * 
   * @param name the name of the local variable to fetch or create.
   * @param value The String value to store at the local variable.
   */
  void createLocalVariableByValue(String name, String value);

  /**
   * Creates and sets a local variable in the current scope equal to the given value. Also set the
   * EscapeMode that was applied to its value. This may be used by the template renderer to decide
   * whether to autoescape the variable.
   * 
   * @param name the name of the local variable to fetch or create.
   * @param value The String value to store at the local variable.
   * @param mode EscapeMode that describes the escaping this variable has. {@code
   *        EscapeMode.ESCAPE_NONE} if the variable was not escaped. {@code
   *        EscapeMode.ESCAPE_IS_CONSTANT} if the variable was populated with a string or numeric
   *        literal.
   */
  void createLocalVariableByValue(String name, String value, EscapeMode mode);

  /**
   * Creates and sets a local variable in the current scope equal to the given value.
   * <p>
   * This method is a helper method for supporting the first() and last() functions on loops without
   * requiring loops create a full Data tree.
   * 
   * @param name the name of the local variable to fetch or create.
   * @param value The String value to store at the local variable.
   * @param isFirst What the local variable should return for
   *        {@link com.google.clearsilver.jsilver.data.Data#isFirstSibling}
   * @param isLast What the local variable should return for
   *        {@link com.google.clearsilver.jsilver.data.Data#isLastSibling}
   */
  void createLocalVariableByValue(String name, String value, boolean isFirst, boolean isLast);

  /**
   * Creates a local variable that references a (possibly non-existent) Data node. When the Data
   * object for this variable is requested, it will return the Data object at the path location or
   * {@code null}, if it does not exist. If {@link #findVariable} is called with {@code create ==
   * true}, then if no Data object exists at the path location, it will be created.
   * 
   * @param name the name of the local variable to fetch or create.
   * @param path The path to the Data object
   */
  void createLocalVariableByPath(String name, String path);

  /**
   * Searches the variable map stack for the specified variable name. If not found, it then searches
   * the root Data object. If not found then and create is {@code true}, then a new node is created
   * with the given name in the root Data object and that node is returned.
   * <p>
   * Note: This only creates nodes in the root Data object, not in any local variable map. To do
   * that, use the Data node returned by {@link #pushVariableScope()}.
   * 
   * @param name the name of the variable to find and/or create.
   * @param create if {@link true} then a new node will be created if an existing Data node with the
   *        given name does not exist.
   * @return The first Data node in the variable map stack that matches the given name, or a Data
   *         node in the root Data object matching the given name, or {@code null} if no such node
   *         exists and {@code create} is {@code false}.
   */
  Data findVariable(String name, boolean create);

  /**
   * Searches the variable map stack for the specified variable name, and returns its
   * {@link EscapeMode}.
   * 
   * @return If the variable is found, returns its {@link EscapeMode}, otherwise returns {@code
   *         EscapeMode.ESCAPE_NONE}.
   */
  EscapeMode findVariableEscapeMode(String name);
}
