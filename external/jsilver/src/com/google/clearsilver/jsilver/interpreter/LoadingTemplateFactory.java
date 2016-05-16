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
import com.google.clearsilver.jsilver.exceptions.JSilverIOException;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;
import com.google.clearsilver.jsilver.syntax.SyntaxTreeBuilder;
import com.google.clearsilver.jsilver.syntax.TemplateSyntaxTree;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Loads a template from disk, and parses it into an AST. Does not do any caching.
 */
public class LoadingTemplateFactory implements TemplateFactory {

  private final SyntaxTreeBuilder syntaxTreeBuilder = new SyntaxTreeBuilder();

  public TemplateSyntaxTree find(String templateName, ResourceLoader resourceLoader,
      EscapeMode escapeMode) {
    try {
      Reader reader = resourceLoader.openOrFail(templateName);
      try {
        return syntaxTreeBuilder.parse(reader, templateName, escapeMode);
      } finally {
        reader.close();
      }
    } catch (IOException e) {
      throw new JSilverIOException(e);
    }
  }

  public TemplateSyntaxTree createTemp(String content, EscapeMode escapeMode) {
    return syntaxTreeBuilder.parse(new StringReader(content), "", escapeMode);
  }

}
