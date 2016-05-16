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

import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;

/**
 * Loads a Template.
 */
public interface TemplateLoader {

  /**
   * Load a template from a named resource, with the provided escape mode. If the mode is
   * ESCAPE_HTML, ESCAPE_URL or ESCAPE_JS, the corresponding escaping will be all variables in the
   * template. If the mode is ESCAPE_AUTO, enable <a href="http://go/autoescapecs">auto escaping</a>
   * on templates. For each variable in the template, this will determine what type of escaping
   * should be applied to the variable, and automatically apply this escaping.
   * 
   * @param templateName e.g. some/path/to/template.cs
   * @param resourceLoader the ResourceLoader object to use to load any files needed to satisfy this
   *        request.
   * @param escapeMode the type of escaping to apply to the entire template.
   */
  Template load(String templateName, ResourceLoader resourceLoader, EscapeMode escapeMode);

  /**
   * Create a temporary template from content, with the provided escape mode. If the mode is
   * ESCAPE_HTML, ESCAPE_URL or ESCAPE_JS, the corresponding escaping will be all variables in the
   * template. If the mode is ESCAPE_AUTO, enable <a href="http://go/autoescapecs">auto escaping</a>
   * on templates. For each variable in the template, this will determine what type of escaping
   * should be applied to the variable, and automatically apply this escaping.
   * 
   * @param name A name to identify the temporary template in stack traces.
   * @param content e.g. "Hello &lt;cs var:name &gt;"
   * @param escapeMode the type of escaping to apply to the entire template.
   */
  Template createTemp(String name, String content, EscapeMode escapeMode);
}
