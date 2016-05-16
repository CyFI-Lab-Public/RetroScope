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

package com.google.clearsilver.jsilver.compiler;

import com.google.clearsilver.jsilver.autoescape.AutoEscapeOptions;
import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.data.DataContext;
import com.google.clearsilver.jsilver.data.DefaultDataContext;
import com.google.clearsilver.jsilver.data.TypeConverter;
import com.google.clearsilver.jsilver.exceptions.ExceptionUtil;
import com.google.clearsilver.jsilver.exceptions.JSilverInterpreterException;
import com.google.clearsilver.jsilver.functions.FunctionExecutor;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;
import com.google.clearsilver.jsilver.template.DefaultRenderingContext;
import com.google.clearsilver.jsilver.template.Macro;
import com.google.clearsilver.jsilver.template.RenderingContext;
import com.google.clearsilver.jsilver.template.Template;
import com.google.clearsilver.jsilver.template.TemplateLoader;
import com.google.clearsilver.jsilver.values.Value;

import java.io.IOException;
import java.util.Collections;

/**
 * Base class providing help to generated templates.
 * 
 * Note, many of the methods are public as they are also used by macros.
 */
public abstract class BaseCompiledTemplate implements Template {

  private FunctionExecutor functionExecutor;
  private String templateName;
  private TemplateLoader templateLoader;
  private EscapeMode escapeMode = EscapeMode.ESCAPE_NONE;
  private AutoEscapeOptions autoEscapeOptions;

  public void setFunctionExecutor(FunctionExecutor functionExecutor) {
    this.functionExecutor = functionExecutor;
  }

  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  public void setTemplateLoader(TemplateLoader templateLoader) {
    this.templateLoader = templateLoader;
  }

  /**
   * Set auto escaping options so they can be passed to the rendering context.
   * 
   * @see AutoEscapeOptions
   */
  public void setAutoEscapeOptions(AutoEscapeOptions autoEscapeOptions) {
    this.autoEscapeOptions = autoEscapeOptions;
  }

  @Override
  public void render(Data data, Appendable out, ResourceLoader resourceLoader) throws IOException {

    render(createRenderingContext(data, out, resourceLoader));
  }

  @Override
  public RenderingContext createRenderingContext(Data data, Appendable out,
      ResourceLoader resourceLoader) {
    DataContext dataContext = new DefaultDataContext(data);
    return new DefaultRenderingContext(dataContext, resourceLoader, out, functionExecutor,
        autoEscapeOptions);
  }

  @Override
  public String getTemplateName() {
    return templateName;
  }

  /**
   * Sets the EscapeMode in which this template was generated.
   * 
   * @param mode EscapeMode
   */
  public void setEscapeMode(EscapeMode mode) {
    this.escapeMode = mode;
  }

  @Override
  public EscapeMode getEscapeMode() {
    return escapeMode;
  }

  @Override
  public String getDisplayName() {
    return templateName;
  }

  /**
   * Verify that the loop arguments are valid. If not, we will skip the loop.
   */
  public static boolean validateLoopArgs(int start, int end, int increment) {
    if (increment == 0) {
      return false; // No increment. Avoid infinite loop.
    }
    if (increment > 0 && start > end) {
      return false; // Incrementing the wrong way. Avoid infinite loop.
    }
    if (increment < 0 && start < end) {
      return false; // Incrementing the wrong way. Avoid infinite loop.
    }
    return true;
  }


  public static boolean exists(Data data) {
    return TypeConverter.exists(data);
  }

  public static int asInt(String value) {
    return TypeConverter.asNumber(value);
  }

  public static int asInt(int value) {
    return value;
  }

  public static int asInt(boolean value) {
    return value ? 1 : 0;
  }

  public static int asInt(Value value) {
    return value.asNumber();
  }

  public static int asInt(Data data) {
    return TypeConverter.asNumber(data);
  }

  public static String asString(String value) {
    return value;
  }

  public static String asString(int value) {
    return Integer.toString(value);
  }

  public static String asString(boolean value) {
    return value ? "1" : "0";
  }

  public static String asString(Value value) {
    return value.asString();
  }

  public static String asString(Data data) {
    return TypeConverter.asString(data);
  }

  public static Value asValue(String value) {
    // Compiler mode does not use the Value's escapeMode or partiallyEscaped
    // variables. TemplateTranslator uses other means to determine the proper
    // escaping to apply. So just set the default escaping flags here.
    return Value.literalValue(value, EscapeMode.ESCAPE_NONE, false);
  }

  public static Value asValue(int value) {
    // Compiler mode does not use the Value's escapeMode or partiallyEscaped
    // variables. TemplateTranslator uses other means to determine the proper
    // escaping to apply. So just set the default escaping flags here.
    return Value.literalValue(value, EscapeMode.ESCAPE_NONE, false);
  }

  public static Value asValue(boolean value) {
    // Compiler mode does not use the Value's escapeMode or partiallyEscaped
    // variables. TemplateTranslator uses other means to determine the proper
    // escaping to apply. So just set the default escaping flags here.
    return Value.literalValue(value, EscapeMode.ESCAPE_NONE, false);
  }

  public static Value asValue(Value value) {
    return value;
  }

  public static Value asVariableValue(String variableName, DataContext context) {
    return Value.variableValue(variableName, context);
  }

  public static boolean asBoolean(boolean value) {
    return value;
  }

  public static boolean asBoolean(String value) {
    return TypeConverter.asBoolean(value);
  }

  public static boolean asBoolean(int value) {
    return value != 0;
  }

  public static boolean asBoolean(Value value) {
    return value.asBoolean();
  }

  public static boolean asBoolean(Data data) {
    return TypeConverter.asBoolean(data);
  }

  /**
   * Gets the name of the node for writing. Used by cs name command. Returns empty string if not
   * found.
   */
  public static String getNodeName(Data data) {
    return data == null ? "" : data.getSymlink().getName();
  }

  /**
   * Returns child nodes of parent. Parent may be null, in which case an empty iterable is returned.
   */
  public Iterable<? extends Data> getChildren(Data parent) {
    if (parent == null) {
      return Collections.emptySet();
    } else {
      return parent.getChildren();
    }
  }

  protected TemplateLoader getTemplateLoader() {
    return templateLoader;
  }

  public abstract class CompiledMacro implements Macro {

    private final String macroName;
    private final String[] argumentsNames;

    protected CompiledMacro(String macroName, String... argumentsNames) {
      this.macroName = macroName;
      this.argumentsNames = argumentsNames;
    }

    @Override
    public void render(Data data, Appendable out, ResourceLoader resourceLoader) throws IOException {
      render(createRenderingContext(data, out, resourceLoader));
    }

    @Override
    public RenderingContext createRenderingContext(Data data, Appendable out,
        ResourceLoader resourceLoader) {
      return BaseCompiledTemplate.this.createRenderingContext(data, out, resourceLoader);
    }

    @Override
    public String getTemplateName() {
      return BaseCompiledTemplate.this.getTemplateName();
    }

    @Override
    public String getMacroName() {
      return macroName;
    }

    @Override
    public String getArgumentName(int index) {
      if (index >= argumentsNames.length) {
        // TODO: Make sure this behavior of failing if too many
        // arguments are passed to a macro is consistent with JNI / interpreter.
        throw new JSilverInterpreterException("Too many arguments supplied to macro " + macroName);
      }
      return argumentsNames[index];
    }

    public int getArgumentCount() {
      return argumentsNames.length;
    }

    protected TemplateLoader getTemplateLoader() {
      return templateLoader;
    }

    @Override
    public EscapeMode getEscapeMode() {
      return BaseCompiledTemplate.this.getEscapeMode();
    }

    @Override
    public String getDisplayName() {
      return BaseCompiledTemplate.this.getDisplayName() + ":" + macroName;
    }
  }

  /**
   * Code common to all three include commands.
   * 
   * @param templateName String representing name of file to include.
   * @param ignoreMissingFile {@code true} if any FileNotFound error generated by the template
   *        loader should be ignored, {@code false} otherwise.
   * @param context Rendering context to use for the included template.
   */
  protected void include(String templateName, boolean ignoreMissingFile, RenderingContext context) {
    if (!context.pushIncludeStackEntry(templateName)) {
      throw new JSilverInterpreterException(createIncludeLoopErrorMessage(templateName, context
          .getIncludedTemplateNames()));
    }

    loadAndRenderIncludedTemplate(templateName, ignoreMissingFile, context);

    if (!context.popIncludeStackEntry(templateName)) {
      // Include stack trace is corrupted
      throw new IllegalStateException("Unable to find on include stack: " + templateName);
    }
  }

  // This method should ONLY be called from include()
  private void loadAndRenderIncludedTemplate(String templateName, boolean ignoreMissingFile,
      RenderingContext context) {
    Template template = null;
    try {
      template =
          templateLoader.load(templateName, context.getResourceLoader(), context
              .getAutoEscapeMode());
    } catch (RuntimeException e) {
      if (ignoreMissingFile && ExceptionUtil.isFileNotFoundException(e)) {
        return;
      } else {
        throw e;
      }
    }
    // Intepret loaded template.
    try {
      template.render(context);
    } catch (IOException e) {
      throw new JSilverInterpreterException(e.getMessage());
    }
  }

  private String createIncludeLoopErrorMessage(String templateName, Iterable<String> includeStack) {
    StringBuilder message = new StringBuilder();
    message.append("File included twice: ");
    message.append(templateName);

    message.append(" Include stack:");
    for (String fileName : includeStack) {
      message.append("\n -> ");
      message.append(fileName);
    }
    message.append("\n -> ");
    message.append(templateName);
    return message.toString();
  }
}
