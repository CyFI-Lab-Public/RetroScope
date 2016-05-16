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

import com.google.clearsilver.jsilver.autoescape.AutoEscapeOptions;
import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.data.DataContext;
import com.google.clearsilver.jsilver.exceptions.JSilverInterpreterException;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;
import com.google.clearsilver.jsilver.values.Value;

/**
 * State that is shared across a template rendering.
 */
public interface RenderingContext {

  /**
   * Execute a named function. Will throw an exception if the function is not found, or the function
   * does not return a value.
   */
  Value executeFunction(String name, Value... args) throws JSilverInterpreterException;

  /**
   * Look up a function by name, and report whether it is an escaping function. Usually, the output
   * of escaping functions will be written using writeUnescaped() instead of writeEscaped().
   * 
   * @see com.google.clearsilver.jsilver.compiler.EscapingEvaluator
   */
  public boolean isEscapingFunction(String name);

  /**
   * Write some text out, using the current escaping function.
   * 
   * @see #pushEscapingFunction(String)
   * @see #popEscapingFunction()
   */
  void writeEscaped(String text);

  /**
   * Write some text out, without doing any escaping. Use this only for trusted content.
   */
  void writeUnescaped(CharSequence text);

  /**
   * Push a new escaping function onto the context. All calls to writeEscape() will use this new
   * function. When done with this function, call popEscapingFunction() to return to the previous
   * escaping function.
   * 
   * If the escaping function is not found, an exception will be thrown. To use no escaping function
   * pass null as the escaperName.
   * 
   * @see #popEscapingFunction()
   */
  void pushEscapingFunction(String escaperName);

  /**
   * @see #pushEscapingFunction(String)
   */
  void popEscapingFunction();

  /**
   * Push a new template onto the current execution context. This is to generate stack traces when
   * things go wrong deep in templates, to allow users to figure out how they got there.
   * 
   * @see #popExecutionContext()
   */
  void pushExecutionContext(Template template);

  /**
   * @see #pushExecutionContext(Template)
   */
  void popExecutionContext();

  /**
   * Sets the current position in the template. Used to help generate error messages.
   */
  void setCurrentPosition(int line, int column);

  /**
   * Register a macro in the current rendering context. This macro will be available to all other
   * templates, regardless of implementation.
   */
  void registerMacro(String name, Macro macro);

  /**
   * Lookup a macro that's already been registered. Throws JSilverInterpreterException if macro not
   * found.
   */
  Macro findMacro(String name) throws JSilverInterpreterException;

  /**
   * Return the DataContext object associated with this RenderingContext.
   */
  DataContext getDataContext();

  /**
   * Returns the ResourceLoader object to use to fetch files needed to render the current template.
   */
  ResourceLoader getResourceLoader();

  /**
   * Returns the configured AutoEscapeOptions to be used while rendering the current template.
   */
  AutoEscapeOptions getAutoEscapeOptions();

  /**
   * Push a new auto escaping mode onto the context. Any template loads via include() or
   * evaluateVariable() will use this auto escaping mode when loading the template.
   * 
   * @see #popAutoEscapeMode
   * 
   */
  void pushAutoEscapeMode(EscapeMode mode);

  /**
   * @see #pushAutoEscapeMode
   */
  void popAutoEscapeMode();

  /**
   * Read the currently set auto escape mode.
   * 
   * @return EscapeMode
   */
  EscapeMode getAutoEscapeMode();

  /**
   * Indicates whether runtime auto escaping is in progress.
   * 
   * @return true if runtime auto escaping is enabled.
   * 
   * @see #isRuntimeAutoEscaping
   */
  boolean isRuntimeAutoEscaping();

  /**
   * Start an auto escaping context to parse and auto escape template contents as they are being
   * rendered.
   * 
   * @see #stopRuntimeAutoEscaping
   */
  void startRuntimeAutoEscaping();

  /**
   * Stop runtime auto escaping.
   * 
   * @see #startRuntimeAutoEscaping
   */
  void stopRuntimeAutoEscaping();

  /**
   * Adds an entry with a name of the template to the stack keeping all names of already included
   * templates. The name is added only if it is not already on the stack.
   * 
   * @param templateName name of the template to be added to the stack. If {@code null} a {@code
   *        NullPointerException} will be thrown.
   * @return true if the {@code templateName} was added.
   */
  boolean pushIncludeStackEntry(String templateName);

  /**
   * Removes an entry with a name of the template from the stack.
   * 
   * @param templateName
   * @return true if the {@code templateName} was on the stack.
   */
  boolean popIncludeStackEntry(String templateName);

  /**
   * Returns the ordered, mutable stack of names of included templates.
   */
  Iterable<String> getIncludedTemplateNames();
}
