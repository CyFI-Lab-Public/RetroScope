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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Loads resources from classpath, alongside a given class.
 * 
 * <p>For example, suppose the classpath contains:
 * <pre>
 * com/foo/SomeThing.class
 * com/foo/my-template.cs
 * com/foo/subdir/another-template.cs
 * </pre>
 * 
 * <p>You can access the resources in the class's package like this:
 * <pre>
 * ResourceLoader loader = new ClassResourceLoader(SomeThing.class);
 * loader.open("my-template.cs");
 * loader.open("subdir/my-template.cs");
 * </pre>
 * Or by using a relative path:
 * <pre>
 * ResourceLoader loader = new ClassResourceLoader(Something.class, "subdir");
 * loader.open("my-template.cs");
 * </pre>
 * 
 * @see ResourceLoader
 * @see ClassLoaderResourceLoader
 */
public class ClassResourceLoader extends BufferedResourceLoader {

  private final Class<?> cls;
  private final String basePath;

  public ClassResourceLoader(Class<?> cls) {
    this.cls = cls;
    this.basePath = "/" + cls.getPackage().getName().replace('.', '/');
  }
  
  /**
   * Load resources from the given subdirectory {@code basePath},
   * relative to the .class file of {@code cls}.
   */
  public ClassResourceLoader(Class<?> cls, String basePath) {
    this.cls = cls;
    this.basePath = basePath;
  }

  @Override
  public Reader open(String name) throws IOException {
    InputStream stream = cls.getResourceAsStream(basePath + '/' + name);
    return stream == null ? null : buffer(new InputStreamReader(stream, getCharacterSet()));
  }

  @Override
  public Reader openOrFail(String name) throws JSilverTemplateNotFoundException, IOException {
    Reader reader = open(name);
    if (reader == null) {
      throw new JSilverTemplateNotFoundException("No '" + name + "' as class resource of "
          + cls.getName());
    } else {
      return reader;
    }
  }

}
