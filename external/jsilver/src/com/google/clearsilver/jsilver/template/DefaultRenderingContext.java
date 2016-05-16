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

import com.google.clearsilver.jsilver.autoescape.AutoEscapeContext;
import com.google.clearsilver.jsilver.autoescape.AutoEscapeOptions;
import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.data.DataContext;
import com.google.clearsilver.jsilver.data.UniqueStack;
import com.google.clearsilver.jsilver.exceptions.JSilverAutoEscapingException;
import com.google.clearsilver.jsilver.exceptions.JSilverIOException;
import com.google.clearsilver.jsilver.exceptions.JSilverInterpreterException;
import com.google.clearsilver.jsilver.functions.FunctionExecutor;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;
import com.google.clearsilver.jsilver.values.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Default implementation of RenderingContext.
 */
public class DefaultRenderingContext implements RenderingContext, FunctionExecutor {

  public static final Logger logger = Logger.getLogger(DefaultRenderingContext.class.getName());
  private final DataContext dataContext;
  private final ResourceLoader resourceLoader;
  private final Appendable out;
  private final FunctionExecutor globalFunctionExecutor;
  private final AutoEscapeOptions autoEscapeOptions;
  private final UniqueStack<String> includeStack;

  private List<String> escaperStack = new ArrayList<String>(8); // seems like a reasonable initial
                                                                // capacity.
  private String currentEscaper; // optimization to reduce List lookup.

  private List<Template> executionStack = new ArrayList<Template>(8);

  private Map<String, Macro> macros = new HashMap<String, Macro>();
  private List<EscapeMode> autoEscapeStack = new ArrayList<EscapeMode>();
  private EscapeMode autoEscapeMode;
  private AutoEscapeContext autoEscapeContext;
  private int line;
  private int column;
  private AutoEscapeContext.AutoEscapeState startingAutoEscapeState;

  public DefaultRenderingContext(DataContext dataContext, ResourceLoader resourceLoader,
      Appendable out, FunctionExecutor globalFunctionExecutor, AutoEscapeOptions autoEscapeOptions) {
    this.dataContext = dataContext;
    this.resourceLoader = resourceLoader;
    this.out = out;
    this.globalFunctionExecutor = globalFunctionExecutor;
    this.autoEscapeOptions = autoEscapeOptions;
    this.autoEscapeMode = EscapeMode.ESCAPE_NONE;
    this.autoEscapeContext = null;
    this.includeStack = new UniqueStack<String>();
  }

  /**
   * Lookup a function by name, execute it and return the results.
   */
  @Override
  public Value executeFunction(String name, Value... args) {
    return globalFunctionExecutor.executeFunction(name, args);
  }

  @Override
  public void escape(String name, String input, Appendable output) throws IOException {
    globalFunctionExecutor.escape(name, input, output);
  }

  @Override
  public boolean isEscapingFunction(String name) {
    return globalFunctionExecutor.isEscapingFunction(name);
  }

  @Override
  public void pushEscapingFunction(String name) {
    escaperStack.add(currentEscaper);
    if (name == null || name.equals("")) {
      currentEscaper = null;
    } else {
      currentEscaper = name;
    }
  }

  @Override
  public void popEscapingFunction() {
    int len = escaperStack.size();
    if (len == 0) {
      throw new IllegalStateException("No more escaping functions to pop.");
    }
    currentEscaper = escaperStack.remove(len - 1);
  }

  @Override
  public void writeEscaped(String text) {
    // If runtime auto escaping is enabled, only apply it if
    // we are not going to do any other default escaping on the variable.
    boolean applyAutoEscape = isRuntimeAutoEscaping() && (currentEscaper == null);
    if (applyAutoEscape) {
      autoEscapeContext.setCurrentPosition(line, column);
      pushEscapingFunction(autoEscapeContext.getEscapingFunctionForCurrentState());
    }
    try {
      if (shouldLogEscapedVariables()) {
        StringBuilder tmp = new StringBuilder();
        globalFunctionExecutor.escape(currentEscaper, text, tmp);
        if (!tmp.toString().equals(text)) {
          logger.warning(new StringBuilder(getLoggingPrefix()).append(" Auto-escape changed [")
              .append(text).append("] to [").append(tmp.toString()).append("]").toString());
        }
        out.append(tmp);
      } else {
        globalFunctionExecutor.escape(currentEscaper, text, out);
      }
    } catch (IOException e) {
      throw new JSilverIOException(e);
    } finally {
      if (applyAutoEscape) {
        autoEscapeContext.insertText();
        popEscapingFunction();
      }
    }
  }

  private String getLoggingPrefix() {
    return "[" + getCurrentResourceName() + ":" + line + ":" + column + "]";
  }

  private boolean shouldLogEscapedVariables() {
    return (autoEscapeOptions != null && autoEscapeOptions.getLogEscapedVariables());
  }

  @Override
  public void writeUnescaped(CharSequence text) {
    if (isRuntimeAutoEscaping() && (currentEscaper == null)) {
      autoEscapeContext.setCurrentPosition(line, column);
      autoEscapeContext.parseData(text.toString());
    }
    try {
      out.append(text);
    } catch (IOException e) {
      throw new JSilverIOException(e);
    }
  }

  @Override
  public void pushExecutionContext(Template template) {
    executionStack.add(template);
  }

  @Override
  public void popExecutionContext() {
    executionStack.remove(executionStack.size() - 1);
  }

  @Override
  public void setCurrentPosition(int line, int column) {
    // TODO: Should these be saved in executionStack as part
    // of pushExecutionContext?
    this.line = line;
    this.column = column;
  }

  @Override
  public void registerMacro(String name, Macro macro) {
    macros.put(name, macro);
  }

  @Override
  public Macro findMacro(String name) {
    Macro macro = macros.get(name);
    if (macro == null) {
      throw new JSilverInterpreterException("No such macro: " + name);
    }
    return macro;
  }

  @Override
  public DataContext getDataContext() {
    return dataContext;
  }

  @Override
  public ResourceLoader getResourceLoader() {
    return resourceLoader;
  }

  @Override
  public AutoEscapeOptions getAutoEscapeOptions() {
    return autoEscapeOptions;
  }

  @Override
  public EscapeMode getAutoEscapeMode() {
    if (isRuntimeAutoEscaping() || (currentEscaper != null)) {
      return EscapeMode.ESCAPE_NONE;
    } else {
      return autoEscapeMode;
    }
  }

  @Override
  public void pushAutoEscapeMode(EscapeMode mode) {
    if (isRuntimeAutoEscaping()) {
      throw new JSilverInterpreterException(
          "cannot call pushAutoEscapeMode while runtime auto escaping is in progress");
    }
    autoEscapeStack.add(autoEscapeMode);
    autoEscapeMode = mode;
  }

  @Override
  public void popAutoEscapeMode() {
    int len = autoEscapeStack.size();
    if (len == 0) {
      throw new IllegalStateException("No more auto escaping modes to pop.");
    }
    autoEscapeMode = autoEscapeStack.remove(autoEscapeStack.size() - 1);
  }

  @Override
  public boolean isRuntimeAutoEscaping() {
    return autoEscapeContext != null;
  }

  /**
   * {@inheritDoc}
   * 
   * @throws JSilverInterpreterException if startRuntimeAutoEscaping is called while runtime
   *         autoescaping is already in progress.
   */
  @Override
  public void startRuntimeAutoEscaping() {
    if (isRuntimeAutoEscaping()) {
      throw new JSilverInterpreterException("startRuntimeAutoEscaping() is not re-entrant at "
          + getCurrentResourceName());
    }
    if (!autoEscapeMode.equals(EscapeMode.ESCAPE_NONE)) {
      // TODO: Get the resourceName as a parameter to this function
      autoEscapeContext = new AutoEscapeContext(autoEscapeMode, getCurrentResourceName());
      startingAutoEscapeState = autoEscapeContext.getCurrentState();
    } else {
      autoEscapeContext = null;
    }
  }

  private String getCurrentResourceName() {
    if (executionStack.size() == 0) {
      return "";
    } else {
      return executionStack.get(executionStack.size() - 1).getDisplayName();
    }
  }

  @Override
  public void stopRuntimeAutoEscaping() {
    if (autoEscapeContext != null) {
      if (!startingAutoEscapeState.equals(autoEscapeContext.getCurrentState())) {
        // We do not allow a macro call to change context of the rest of the template.
        // Since the rest of the template has already been auto-escaped at parse time
        // with the assumption that the macro call will not modify the context.
        throw new JSilverAutoEscapingException("Macro starts in context " + startingAutoEscapeState
            + " but ends in different context " + autoEscapeContext.getCurrentState(),
            autoEscapeContext.getResourceName());
      }
    }
    autoEscapeContext = null;
  }

  @Override
  public boolean pushIncludeStackEntry(String templateName) {
    return includeStack.push(templateName);
  }

  @Override
  public boolean popIncludeStackEntry(String templateName) {
    return templateName.equals(includeStack.pop());
  }

  @Override
  public Iterable<String> getIncludedTemplateNames() {
    return includeStack;
  }
}
