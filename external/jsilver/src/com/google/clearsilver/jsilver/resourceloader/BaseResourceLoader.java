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

import java.io.IOException;
import java.io.Reader;

/**
 * Implementations of ResourceLoader should extend this class rather than directly implement the
 * ResourceLoader interface - this allows changes to be made to the ResourceLoader interface whilst
 * retaining backwards compatibility with existing implementations.
 * 
 * @see ResourceLoader
 */
public abstract class BaseResourceLoader implements ResourceLoader {

  @Override
  public void close(Reader reader) throws IOException {
    reader.close();
  }

  /**
   * Default implementation returns the filename as the ResourceLoaders that subclass this class
   * tend to assume they are the only ResourceLoader in use. Or at least that the filename is the
   * only necessary form of uniqueness between two instances of this same ResourceLoader.
   */
  @Override
  public Object getKey(String filename) {
    return filename;
  }

  /**
   * Default implementation does not check whether the resource has changed.
   */
  @Override
  public Object getResourceVersionId(String filename) {
    return filename;
  }
}
