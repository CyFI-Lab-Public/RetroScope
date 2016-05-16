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

import java.io.IOException;

/**
 * Interface for CS file hook
 */
public interface CSFileLoader {

  /**
   * Callback method that is expected to return the contents of the specified
   * file as a string.
   * @param hdf the HDF structure associated with HDF or CS object making the
   * callback.
   * @param filename the name of the file that should be loaded.
   * @return a string containing the contents of the file.
   */
  public String load(HDF hdf, String filename) throws IOException;

}
