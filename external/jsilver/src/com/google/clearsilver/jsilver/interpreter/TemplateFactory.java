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

/**
 * Responsible for creating/retrieving an AST tree for a template with a given name.
 * <p>
 * This interface always expects to take a ResourceLoader object from the caller. This helps
 * guarantee that per-Template resourceLoaders are respected.
 */
public interface TemplateFactory {

  /**
   * Load a template from the source.
   * 
   * @param templateName e.g. some/path/to/template.cs
   * @param resourceLoader use this ResourceLoader to locate the named template file and any
   *        included files.
   * @param escapeMode the type of escaping to apply to the entire template.
   */
  TemplateSyntaxTree find(String templateName, ResourceLoader resourceLoader, EscapeMode escapeMode);

  /**
   * Create a temporary template from content.
   * 
   * @param content e.g. "Hello &lt;cs var:name &gt;"
   * @param escapeMode
   * @param escapeMode the type of escaping to apply to the entire template.
   */
  TemplateSyntaxTree createTemp(String content, EscapeMode escapeMode);

}
