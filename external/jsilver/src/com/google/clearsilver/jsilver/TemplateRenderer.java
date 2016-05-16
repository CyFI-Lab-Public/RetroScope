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

package com.google.clearsilver.jsilver;

import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.exceptions.JSilverException;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;
import com.google.clearsilver.jsilver.template.Template;

import java.io.IOException;

/**
 * Renders a template.
 */
public interface TemplateRenderer {

  /**
   * Renders a given template and provided data, writing to an arbitrary output.
   * 
   * @param templateName Name of template to load (e.g. "things/blah.cs").
   * @param data Data to be used in template.
   * @param output Where template should be rendered to. This can be a Writer, PrintStream,
   *        System.out/err), StringBuffer/StringBuilder or anything that implements
   *        java.io.Appendable
   * @param resourceLoader ResourceLoader to use when reading in included files.
   */
  void render(String templateName, Data data, Appendable output, ResourceLoader resourceLoader)
      throws IOException, JSilverException;

  /**
   * Same as {@link #render(String, Data, Appendable, ResourceLoader)}, except it uses the default
   * ResourceLoader passed in to the JSilver constructor.
   */
  void render(String templateName, Data data, Appendable output) throws IOException,
      JSilverException;

  /**
   * Same as {@link #render(String, Data, Appendable)}, except returns rendered template as a
   * String.
   */
  String render(String templateName, Data data) throws IOException, JSilverException;

  /**
   * Renders a given template and provided data, writing to an arbitrary output.
   * 
   * @param template Template to render.
   * @param data Data to be used in template.
   * @param output Where template should be rendered to. This can be a Writer, PrintStream,
   *        System.out/err), StringBuffer/StringBuilder or anything that implements
   *        java.io.Appendable.
   * @param resourceLoader ResourceLoader to use when reading in included files.
   * 
   */
  void render(Template template, Data data, Appendable output, ResourceLoader resourceLoader)
      throws IOException, JSilverException;

  /**
   * Same as {@link #render(Template,Data,Appendable,ResourceLoader)}, except it uses the
   * ResourceLoader passed into the JSilver constructor.
   */
  void render(Template template, Data data, Appendable output) throws IOException, JSilverException;

  /**
   * Same as {@link #render(Template,Data,Appendable)}, except returns rendered template as a
   * String.
   */
  String render(Template template, Data data) throws IOException, JSilverException;

  /**
   * Renders a given template from the content passed in. That is, the first parameter is the actual
   * template content rather than the filename to load.
   * 
   * @param content Content of template (e.g. "Hello &lt;cs var:name ?&gt;").
   * @param data Data to be used in template.
   * @param output Where template should be rendered to. This can be a Writer, PrintStream,
   *        System.out/err), StringBuffer/StringBuilder or anything that implements
   *        java.io.Appendable
   */
  void renderFromContent(String content, Data data, Appendable output) throws IOException,
      JSilverException;

  /**
   * Same as {@link #renderFromContent(String, Data, Appendable)}, except returns rendered template
   * as a String.
   */
  String renderFromContent(String content, Data data) throws IOException, JSilverException;

}
