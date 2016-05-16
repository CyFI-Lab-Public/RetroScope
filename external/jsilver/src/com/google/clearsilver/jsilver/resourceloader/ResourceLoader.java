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
import java.io.Reader;

/**
 * Loads resources, from somewhere.
 * 
 * This is a service provider interface (SPI) for JSilver. Users of JSilver can easily create their
 * own implementations. However, it is recommended that new implementations don't implement this
 * interface directly, but instead extends {@link BaseResourceLoader}. This allows API changes to be
 * made to JSilver that maintain compatibility with existing ResourceLoader implementations.
 * 
 * @see BaseResourceLoader
 * @see InMemoryResourceLoader
 * @see FileSystemResourceLoader
 * @see ClassLoaderResourceLoader
 * @see ClassResourceLoader
 */
public interface ResourceLoader {

  /**
   * Open a resource. If this resource is not found, null should be returned.
   * 
   * The caller of this method is guaranteed to call {@link #close(Reader)} when done with the
   * reader.
   * 
   * @param name the name of the resource
   * @return Reader, or null if not found.
   * @throws IOException if resource fails to open
   */
  Reader open(String name) throws IOException;

  /**
   * Open a resource or throw an exception if no such resource is found.
   * 
   * The caller of this method is guaranteed to call {@link #close(Reader)} when done with the
   * reader.
   * 
   * @param name the name of the resource
   * @return Reader, or null if not found.
   * @throws JSilverTemplateNotFoundException if resource is not found
   * @throws IOException if resource fails to open
   */
  Reader openOrFail(String name) throws JSilverTemplateNotFoundException, IOException;

  /**
   * Close the reader. Allows ResourceLoader to perform any additional clean up.
   * 
   * @param reader the reader to close
   * @throws IOException if reader fasils to close
   */
  void close(Reader reader) throws IOException;

  /**
   * Returns an object that can be used to uniquely identify the file corresponding to the given
   * file name in the context of this ResourceLoader. (e.g. ordered list of directories + filename,
   * or absolute file path.).
   * 
   * @param filename the name we want to identify
   * @return unique identifier
   */
  Object getKey(String filename);

  /**
   * Returns an object that can be used to identify when a resource has changed. This key should be
   * based on some piece(s) of metadata that strongly indicates the resource has changed, for
   * example a file's last modified time. Since the object is expected to be used as part of a cache
   * key, it should be immutable and implement {@link Object#equals(Object)} and
   * {@link Object#hashCode()} .
   * 
   * If the ResourceLoader does not or cannot compute a version identifier then it is sufficient to
   * always return the same Object, e.g. the resource name. Null, however, should only be returned
   * if a call to {@link #open(String)} would also return null.
   * 
   * @param name the name of the resource to check for resources
   * @return unique identifier for the current version of the resource or null if the resource
   *         cannot be found
   */
  Object getResourceVersionId(String name);
}
