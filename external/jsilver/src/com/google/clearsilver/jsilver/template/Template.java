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
import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;

import java.io.IOException;

/**
 * Represents a template that can be rendered with data.
 */
public interface Template {

  /**
   * Render the template.
   * 
   * @param data Data to merge with template.
   * @param out Target to write to.
   * @param resourceLoader ResourceLoader to use instead of the default template one when loading
   *        files.
   */
  void render(Data data, Appendable out, ResourceLoader resourceLoader) throws IOException;

  /**
   * Render the template with a custom RenderingContext.
   * 
   * @param context RenderingContext to use during rendering.
   */
  void render(RenderingContext context) throws IOException;

  /**
   * Create a new RenderingContext.
   * 
   * @param data Data to merge with template.
   * @param out Target to write to.
   * @param resourceLoader ResourceLoader to load files.
   */
  RenderingContext createRenderingContext(Data data, Appendable out, ResourceLoader resourceLoader);

  /**
   * Name of template (e.g. mytemplate.cs).
   */
  String getTemplateName();

  /**
   * Name to use when displaying error or log messages. May return the same value as
   * #getTemplateName, or may contain more information.
   */
  String getDisplayName();

  /**
   * Return the EscapeMode in which this template was generated.
   * 
   * @return EscapeMode
   */
  EscapeMode getEscapeMode();
}
