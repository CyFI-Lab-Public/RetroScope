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

package org.clearsilver;

import java.io.Closeable;
import java.io.IOException;

public interface CS extends Closeable {

  /**
   * Specify a new/different global HDF
   */
  void setGlobalHDF(HDF global);

  /**
   * Return global hdf in use
   */
  HDF getGlobalHDF();

  /**
   * Clean up CS object state.
   */
  void close();

  /**
   * Parses the specified file as if it has template content. The file will
   * be located using the HDF's loadpaths.
   * @param filename the name of file to read in and parse.
   * @throws java.io.FileNotFoundException if the specified file does not
   * exist.
   * @throws IOException other problems reading the file.
   */
  void parseFile(String filename) throws IOException;

  /**
   * Parse the given string as a CS template.
   * @param content string to parse.
   */
  void parseStr(String content);
  
  /**
   * Generate output from the CS templates and HDF objects that have been read
   * in.
   * @return the output of the template rendering.
   */
  String render();

  /**
   * Get the file loader in use, if any.
   * @return the file loader in use.
   */
  CSFileLoader getFileLoader();

  /**
   * Set the CS file loader to use
   * @param fileLoader the file loader that should be used.
   */
  void setFileLoader(CSFileLoader fileLoader);

}
