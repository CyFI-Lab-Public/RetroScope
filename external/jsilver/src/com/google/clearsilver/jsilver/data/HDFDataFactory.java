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

import com.google.clearsilver.jsilver.exceptions.JSilverBadSyntaxException;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;

/**
 * Loads data in Hierarchical Data Format (HDF) into Data objects.
 */
public class HDFDataFactory implements DataFactory {
  private final Parser hdfParser;
  private final boolean ignoreAttributes;

  public HDFDataFactory(boolean ignoreAttributes) {
    this(ignoreAttributes, new NoOpStringInternStrategy());
  }

  public HDFDataFactory(boolean ignoreAttributes, StringInternStrategy stringInternStrategy) {
    this(NewHdfParser.newFactory(stringInternStrategy), ignoreAttributes);
  }

  public HDFDataFactory(ParserFactory hdfParserFactory, boolean ignoreAttributes) {
    this.ignoreAttributes = ignoreAttributes;
    this.hdfParser = hdfParserFactory.newInstance();
  }

  @Override
  public Data createData() {
    return new DefaultData();
  }

  @Override
  public void loadData(final String dataFileName, ResourceLoader resourceLoader, Data output)
      throws JSilverBadSyntaxException, IOException {
    Reader reader = resourceLoader.open(dataFileName);
    if (reader == null) {
      throw new FileNotFoundException(dataFileName);
    }
    try {
      hdfParser.parse(reader, output, new Parser.ErrorHandler() {
        public void error(int line, String lineContent, String fileName, String errorMessage) {
          throw new JSilverBadSyntaxException("HDF parsing error : '" + errorMessage + "'",
              lineContent, fileName, line, JSilverBadSyntaxException.UNKNOWN_POSITION, null);
        }
      }, resourceLoader, dataFileName, ignoreAttributes);
    } finally {
      resourceLoader.close(reader);
    }
  }

  @Override
  public Data loadData(String dataFileName, ResourceLoader resourceLoader) throws IOException {
    Data result = createData();
    loadData(dataFileName, resourceLoader, result);
    return result;
  }

  @Override
  public Parser getParser() {
    return hdfParser;
  }
}
