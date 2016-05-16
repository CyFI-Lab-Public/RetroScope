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
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;
import com.google.clearsilver.jsilver.syntax.TemplateSyntaxTree;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a template factory with a series of optimization steps. Any null optimization steps are
 * ignored.
 */
public class OptimizingTemplateFactory implements TemplateFactory {
  private final TemplateFactory wrapped;
  private final List<OptimizerProvider> optimizers;

  /**
   * Creates a factory from the given optimization steps that wraps another TemplateFactory.
   * 
   * @param wrapped the template factory instance to be wrapped.
   * @param optimizers the optimizers to apply (null optimizations are ignored).
   */
  public OptimizingTemplateFactory(TemplateFactory wrapped, OptimizerProvider... optimizers) {
    this.wrapped = wrapped;
    // Ignore null providers during construction.
    this.optimizers = new ArrayList<OptimizerProvider>();
    for (OptimizerProvider optimizer : optimizers) {
      if (optimizer != null) {
        this.optimizers.add(optimizer);
      }
    }
  }

  private void optimize(TemplateSyntaxTree ast) {
    for (OptimizerProvider optimizer : optimizers) {
      ast.apply(optimizer.getOptimizer());
    }
  }

  @Override
  public TemplateSyntaxTree createTemp(String content, EscapeMode escapeMode) {
    TemplateSyntaxTree result = wrapped.createTemp(content, escapeMode);
    optimize(result);
    return result;
  }

  @Override
  public TemplateSyntaxTree find(String templateName, ResourceLoader resourceLoader,
      EscapeMode escapeMode) {
    TemplateSyntaxTree result = wrapped.find(templateName, resourceLoader, escapeMode);
    optimize(result);
    return result;
  }
}
