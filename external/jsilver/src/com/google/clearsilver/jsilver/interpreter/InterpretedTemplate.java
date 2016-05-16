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

import com.google.clearsilver.jsilver.autoescape.AutoEscapeOptions;
import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.data.DataContext;
import com.google.clearsilver.jsilver.data.DefaultDataContext;
import com.google.clearsilver.jsilver.functions.FunctionExecutor;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;
import com.google.clearsilver.jsilver.syntax.TemplateSyntaxTree;
import com.google.clearsilver.jsilver.template.DefaultRenderingContext;
import com.google.clearsilver.jsilver.template.RenderingContext;
import com.google.clearsilver.jsilver.template.Template;
import com.google.clearsilver.jsilver.template.TemplateLoader;

import java.io.IOException;

/**
 * Template implementation that uses the interpreter to render itself.
 */
public class InterpretedTemplate implements Template {

  private final TemplateLoader loader;

  private final TemplateSyntaxTree syntaxTree;
  private final String name;
  private final FunctionExecutor functionExecutor;
  private final EscapeMode escapeMode;
  private final AutoEscapeOptions autoEscapeOptions;

  public InterpretedTemplate(TemplateLoader loader, TemplateSyntaxTree syntaxTree, String name,
      FunctionExecutor functionExecutor, AutoEscapeOptions autoEscapeOptions, EscapeMode mode) {
    this.loader = loader;
    this.syntaxTree = syntaxTree;
    this.name = name;
    this.functionExecutor = functionExecutor;
    this.escapeMode = mode;
    this.autoEscapeOptions = autoEscapeOptions;
  }

  @Override
  public void render(Data data, Appendable out, ResourceLoader resourceLoader) throws IOException {
    render(createRenderingContext(data, out, resourceLoader));
  }

  @Override
  public void render(RenderingContext context) throws IOException {
    TemplateInterpreter interpreter =
        new TemplateInterpreter(this, loader, context, functionExecutor);
    context.pushExecutionContext(this);
    syntaxTree.apply(interpreter);
    context.popExecutionContext();
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
    return name;
  }

  @Override
  public EscapeMode getEscapeMode() {
    return escapeMode;
  }

  @Override
  public String getDisplayName() {
    return name;
  }
}
