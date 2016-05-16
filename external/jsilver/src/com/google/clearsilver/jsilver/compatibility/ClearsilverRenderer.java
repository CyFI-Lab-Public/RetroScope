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

package com.google.clearsilver.jsilver.compatibility;

import com.google.clearsilver.jsilver.TemplateRenderer;
import com.google.clearsilver.jsilver.template.Template;
import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.exceptions.JSilverException;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;

import org.clearsilver.CS;
import org.clearsilver.CSFileLoader;
import org.clearsilver.ClearsilverFactory;
import org.clearsilver.HDF;
import org.clearsilver.jni.JniClearsilverFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;

/**
 * A {@link TemplateRenderer} implemented using ClearSilver itself.
 */
public class ClearsilverRenderer implements TemplateRenderer {
  private final ClearsilverFactory factory;
  private final ResourceLoader defaultResourceLoader;

  /**
   * Creates an implementation using the provided ClearSilver factory and JSilver resource loader.
   */
  public ClearsilverRenderer(ClearsilverFactory factory, ResourceLoader resourceLoader) {
    this.factory = factory;
    this.defaultResourceLoader = resourceLoader;
  }

  /**
   * Creates a JSilver implementation using the JNI ClearSilver factory and provided JSilver
   * resource loader.
   */
  public ClearsilverRenderer(ResourceLoader resourceLoader) {
    this(new JniClearsilverFactory(), resourceLoader);
  }

  @Override
  public void render(String templateName, Data data, Appendable output,
      final ResourceLoader resourceLoader) throws IOException, JSilverException {
    CSFileLoader fileLoader = new CSFileLoader() {
      @Override
      public String load(HDF hdf, String filename) throws IOException {
        return loadResource(filename, resourceLoader);
      }
    };

    HDF hdf = factory.newHdf();
    try {
      // Copy the Data into the HDF.
      hdf.readString(data.toString());

      CS cs = factory.newCs(hdf);
      try {
        cs.setFileLoader(fileLoader);
        cs.parseFile(templateName);
        output.append(cs.render());
      } finally {
        cs.close();
      }
    } finally {
      hdf.close();
    }
  }

  @Override
  public void render(String templateName, Data data, Appendable output) throws IOException,
      JSilverException {
    render(templateName, data, output, defaultResourceLoader);
  }

  @Override
  public String render(String templateName, Data data) throws IOException, JSilverException {
    Appendable output = new StringBuilder(8192);
    render(templateName, data, output);
    return output.toString();
  }

  @Override
  public void render(Template template, Data data, Appendable output, ResourceLoader resourceLoader)
      throws IOException, JSilverException {
    throw new UnsupportedOperationException("ClearsilverRenderer only expects "
        + "template names, not Templates");
  }

  @Override
  public void render(Template template, Data data, Appendable output) throws IOException,
      JSilverException {
    render(template, data, output, defaultResourceLoader);
  }

  @Override
  public String render(Template template, Data data) throws IOException, JSilverException {
    Appendable output = new StringBuilder(8192);
    render(template, data, output);
    return output.toString();
  }

  @Override
  public void renderFromContent(String content, Data data, Appendable output) throws IOException,
      JSilverException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String renderFromContent(String content, Data data) throws IOException, JSilverException {
    Appendable output = new StringBuilder(8192);
    renderFromContent(content, data, output);
    return output.toString();
  }

  /**
   * @return the contents of a resource, or null if the resource was not found.
   */
  private String loadResource(String filename, ResourceLoader resourceLoader) throws IOException {
    Reader reader = resourceLoader.open(filename);
    if (reader == null) {
      throw new FileNotFoundException(filename);
    }
    StringBuilder sb = new StringBuilder();
    char buf[] = new char[4096];
    int count;
    while ((count = reader.read(buf)) != -1) {
      sb.append(buf, 0, count);
    }
    return sb.toString();
  }
}
