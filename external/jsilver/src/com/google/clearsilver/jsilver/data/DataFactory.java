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

import java.io.IOException;

/**
 * Loads data from resources.
 */
public interface DataFactory {

  /**
   * Create new Data instance, ready to be populated.
   */
  Data createData();

  /**
   * Loads data in Hierarchical Data Format (HDF) into an existing Data object.
   */
  void loadData(final String dataFileName, ResourceLoader resourceLoader, Data output)
      throws JSilverBadSyntaxException, IOException;

  /**
   * Loads data in Hierarchical Data Format (HDF) into a new Data object.
   */
  Data loadData(String dataFileName, ResourceLoader resourceLoader) throws IOException;

  /**
   * Returns the Parser used by this factory to parse the HDF content.
   * 
   * @return
   */
  Parser getParser();
}
