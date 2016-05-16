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

package com.google.clearsilver.jsilver.resourceloader;

import com.google.clearsilver.jsilver.exceptions.JSilverTemplateNotFoundException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Loads resources from classpath.
 * 
 * <p>
 * For example, suppose the classpath contains:
 * 
 * <pre>
 * com/foo/my-template.cs
 * com/foo/subdir/another-template.cs
 * </pre>
 * 
 * <p>
 * You can access the resources like this:
 * 
 * <pre>
 * ResourceLoader loader =
 *     new ClassPathResourceLoader(getClassLoader(), "com/foo");
 * loader.open("my-template.cs");
 * loader.open("subdir/my-template.cs");
 * </pre>
 * 
 * @see ResourceLoader
 * @see ClassResourceLoader
 */
public class ClassLoaderResourceLoader extends BufferedResourceLoader {

  private final ClassLoader classLoader;
  private String basePath;

  public ClassLoaderResourceLoader(ClassLoader classLoader, String basePath) {
    this.classLoader = classLoader;
    this.basePath = basePath;
  }

  public ClassLoaderResourceLoader(ClassLoader classLoader) {
    this(classLoader, ".");
  }

  @Override
  public Reader open(String name) throws IOException {
    String path = basePath + '/' + name;
    InputStream stream = classLoader.getResourceAsStream(path);
    return stream == null ? null : buffer(new InputStreamReader(stream, getCharacterSet()));
  }

  @Override
  public Reader openOrFail(String name) throws JSilverTemplateNotFoundException, IOException {
    Reader reader = open(name);
    if (reader == null) {
      throw new JSilverTemplateNotFoundException("No class loader resource '" + name + "' in '"
          + basePath + "'");
    } else {
      return reader;
    }
  }

}
