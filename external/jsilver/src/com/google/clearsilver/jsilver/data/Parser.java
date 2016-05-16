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

package com.google.clearsilver.jsilver.data;

import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;

import java.io.Reader;
import java.io.IOException;

/**
 * Parses data in HierachicalDataFormat (HDF), generating callbacks for data encountered in the
 * stream.
 */
public interface Parser {

  /** Called whenever an error occurs. */
  public interface ErrorHandler {
    /**
     * Report an error to the ErrorHandler.
     * 
     * @param line number of the line where error occurred. The value of -1 represents line number
     *        unknown
     * @param lineContent text of the line with error
     * @param fileName name of the file in which the error occurred
     * @param errorMessage description of an error
     */
    void error(int line, String lineContent, String fileName, String errorMessage);
  }

  /**
   * Reads in a stream of characters and parses data from it, putting it into the given Data object.
   * 
   * @param reader Reader used to read in the formatted data.
   * @param output Data object that the read data structure will be dumped into.
   * @param errorHandler Error callback to be called on any error.
   * @param resourceLoader ResourceLoader to use to read in included files.
   * @param dataFileName Name of a file that is read with reader. It is needed for the purpose of
   *        handling include loops and error messages.
   * @param ignoreAttributes whether to store parsed HDF attributes in the Data object or not.
   * @throws IOException when errors occur reading input.
   */
  void parse(Reader reader, Data output, ErrorHandler errorHandler, ResourceLoader resourceLoader,
      String dataFileName, boolean ignoreAttributes) throws IOException;
}
