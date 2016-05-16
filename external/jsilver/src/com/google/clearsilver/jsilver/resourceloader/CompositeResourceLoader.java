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

import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * ResourceLoader composed of other ResourceLoaders. When a resource is loaded, it will search
 * through each ResourceLoader until it finds something.
 *
 * @see ResourceLoader
 */
public class CompositeResourceLoader implements ResourceLoader {

  private final List<ResourceLoader> loaders = new ArrayList<ResourceLoader>();

  public CompositeResourceLoader(Iterable<ResourceLoader> loaders) {
    for (ResourceLoader loader : loaders) {
      add(loader);
    }
  }

  public CompositeResourceLoader(ResourceLoader... loaders) {
    for (ResourceLoader loader : loaders) {
      add(loader);
    }
  }

  public void add(ResourceLoader loader) {
    loaders.add(loader);
  }

  public Reader open(String name) throws IOException {
    for (ResourceLoader loader : loaders) {
      Reader reader = loader.open(name);
      if (reader != null) {
        return new ReaderTracer(reader, loader);
      }
    }
    return null;
  }

  @Override
  public Reader openOrFail(String name) throws JSilverTemplateNotFoundException, IOException {
    Reader reader = open(name);
    if (reader == null) {
      throw new JSilverTemplateNotFoundException(name);
    } else {
      return reader;
    }
  }

  public void close(Reader reader) throws IOException {
    if (!(reader instanceof ReaderTracer)) {
      throw new IllegalArgumentException("I can't close a reader I didn't open.");
    }
    reader.close();
  }

  /**
   * We return the filename as the key of uniqueness as we assume that if this
   * CompositeResourceLoader is in use, then there won't be another ResourceLoader that we are
   * competing against. If we did need to worry about it we would want to prepend the key from
   * above.
   */
  @Override
  public Object getKey(String filename) {
    return filename;
  }

  /**
   * Return the first non-null version identifier found among the ResourceLoaders, using the same
   * search order as {@link #open(String)}.
   */
  @Override
  public Object getResourceVersionId(String filename) {
    for (ResourceLoader loader : loaders) {
      Object currentKey = loader.getResourceVersionId(filename);
      if (currentKey != null) {
        return currentKey;
      }
    }
    return null;
  }

  /**
   * Wraps a reader, associating it with the original ResourceLoader - this is necessary so when
   * close() is called, we delegate back to original ResourceLoader.
   */
  private static class ReaderTracer extends FilterReader {

    private final ResourceLoader originalLoader;

    public ReaderTracer(Reader in, ResourceLoader originalLoader) {
      super(in);
      this.originalLoader = originalLoader;
    }

    public void close() throws IOException {
      originalLoader.close(in);
    }
  }

}
