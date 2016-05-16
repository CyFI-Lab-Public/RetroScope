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

package com.google.clearsilver.jsilver.interpreter;

import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.exceptions.JSilverInterpreterException;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;
import com.google.clearsilver.jsilver.syntax.node.PCommand;
import com.google.clearsilver.jsilver.template.Macro;
import com.google.clearsilver.jsilver.template.RenderingContext;
import com.google.clearsilver.jsilver.template.Template;

import java.io.IOException;

/**
 * User defined macro that will be executed by the interpreter.
 * 
 * NOTE: This is not thread safe and cannot be shared between RenderingContexts. This is taken care
 * of by the TemplateInterpreter.
 */
public class InterpretedMacro implements Macro {

  private final PCommand command;
  private final Template owningTemplate;
  private final String macroName;
  private final String[] argumentNames;
  private final TemplateInterpreter templateInterpreter;
  private final RenderingContext owningContext;

  public InterpretedMacro(PCommand command, Template owningTemplate, String macroName,
      String[] argumentNames, TemplateInterpreter templateInterpreter,
      RenderingContext owningContext) {
    this.command = command;
    this.owningTemplate = owningTemplate;
    this.macroName = macroName;
    this.argumentNames = argumentNames;
    this.templateInterpreter = templateInterpreter;
    this.owningContext = owningContext;
  }

  @Override
  public void render(RenderingContext context) throws IOException {
    assert context == owningContext : "Cannot render macro defined in another context";
    context.pushExecutionContext(this);
    boolean doRuntimeAutoEscaping = !(context.isRuntimeAutoEscaping());
    if (doRuntimeAutoEscaping) {
      context.startRuntimeAutoEscaping();
    }
    command.apply(templateInterpreter);
    if (doRuntimeAutoEscaping) {
      context.stopRuntimeAutoEscaping();
    }
    context.popExecutionContext();
  }

  @Override
  public void render(Data data, Appendable out, ResourceLoader resourceLoader) throws IOException {
    render(createRenderingContext(data, out, resourceLoader));
  }

  @Override
  public RenderingContext createRenderingContext(Data data, Appendable out,
      ResourceLoader resourceLoader) {
    return owningTemplate.createRenderingContext(data, out, resourceLoader);
  }

  @Override
  public String getTemplateName() {
    return owningTemplate.getTemplateName();
  }

  @Override
  public EscapeMode getEscapeMode() {
    return owningTemplate.getEscapeMode();
  }

  @Override
  public String getDisplayName() {
    return owningTemplate.getDisplayName() + ":" + macroName;
  }

  @Override
  public String getMacroName() {
    return macroName;
  }

  @Override
  public String getArgumentName(int index) {
    if (index >= argumentNames.length) {
      // TODO: Make sure this behavior of failing if too many
      // arguments are passed to a macro is consistent with JNI / interpreter.
      throw new JSilverInterpreterException("Too many arguments supplied to macro " + macroName);
    }
    return argumentNames[index];
  }

  @Override
  public int getArgumentCount() {
    return argumentNames.length;
  }
}
