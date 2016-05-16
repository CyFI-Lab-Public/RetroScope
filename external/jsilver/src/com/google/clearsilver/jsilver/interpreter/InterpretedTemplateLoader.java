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
import com.google.clearsilver.jsilver.functions.FunctionExecutor;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;
import com.google.clearsilver.jsilver.template.DelegatingTemplateLoader;
import com.google.clearsilver.jsilver.template.Template;
import com.google.clearsilver.jsilver.template.TemplateLoader;

/**
 * TemplateLoader that loads InterpretedTemplates.
 */
public class InterpretedTemplateLoader implements DelegatingTemplateLoader {

  private final TemplateFactory templateFactory;

  private final FunctionExecutor globalFunctionExecutor;
  private final AutoEscapeOptions autoEscapeOptions;
  private TemplateLoader templateLoaderDelegate = this;

  public InterpretedTemplateLoader(TemplateFactory templateFactory,
      FunctionExecutor globalFunctionExecutor, AutoEscapeOptions autoEscapeOptions) {
    this.templateFactory = templateFactory;
    this.globalFunctionExecutor = globalFunctionExecutor;
    this.autoEscapeOptions = autoEscapeOptions;
  }

  @Override
  public void setTemplateLoaderDelegate(TemplateLoader templateLoaderDelegate) {
    this.templateLoaderDelegate = templateLoaderDelegate;
  }

  @Override
  public Template load(String templateName, ResourceLoader resourceLoader, EscapeMode escapeMode) {
    return new InterpretedTemplate(templateLoaderDelegate, templateFactory.find(templateName,
        resourceLoader, escapeMode), templateName, globalFunctionExecutor, autoEscapeOptions,
        escapeMode);
  }

  @Override
  public Template createTemp(String name, String content, EscapeMode escapingMode) {
    return new InterpretedTemplate(templateLoaderDelegate, templateFactory.createTemp(content,
        escapingMode), name, globalFunctionExecutor, autoEscapeOptions, escapingMode);
  }
}
