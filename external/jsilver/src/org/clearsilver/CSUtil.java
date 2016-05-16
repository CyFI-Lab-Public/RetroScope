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

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility class containing helper methods
 */
public final class CSUtil {

  private CSUtil() { }

  public static final String HDF_LOADPATHS = "hdf.loadpaths";

  /**
   * Helper function that returns a concatenation of the loadpaths in the
   * provided HDF.
   * @param hdf an HDF structure containing load paths.
   * @return A list of loadpaths in order in which to search.
   * @throws NullPointerException if no loadpaths are found.
   */
  public static List<String> getLoadPaths(HDF hdf) {
    return getLoadPaths(hdf, false);
  }

  /**
   * Helper function that returns a concatenation of the loadpaths in the
   * provided HDF.
   * @param hdf an HDF structure containing load paths.
   * @param allowEmpty if {@code true} then this will return an empty list when
   *     no loadpaths are found in the HDF object, otherwise a
   *     {@link NullPointerException} is thrown. Loadpaths are not needed if
   *     no files are read in or are all specified by absolute paths.
   * @return A list of loadpaths in order in which to search.
   * @throws NullPointerException if no loadpaths are found and allowEmpty is
   *     {@code false}.
   */
  public static List<String> getLoadPaths(HDF hdf, boolean allowEmpty) {
    List<String> list = new LinkedList<String>();
    HDF loadpathsHdf = hdf.getObj(HDF_LOADPATHS);
    if (loadpathsHdf == null) {
      if (allowEmpty) {
        return list;
      } else {
        throw new NullPointerException("No HDF loadpaths located in the "
            + "specified HDF structure");
      }
    }
    for (HDF lpHdf = loadpathsHdf.objChild(); lpHdf != null;
        lpHdf = lpHdf.objNext()) {
      list.add(lpHdf.objValue());
    }
    return list;
  }

  /**
   * Given an ordered list of directories to look in, locate the specified file.
   * Returns <code>null</code> if file not found.
   * @param loadpaths the ordered list of paths to search.
   * @param filename the name of the file.
   * @return a File object corresponding to the file. <code>null</code> if
   *     file not found.
   */
  public static File locateFile(List<String> loadpaths, String filename) {
    if (filename == null) {
      throw new NullPointerException("No filename provided");
    }
    if (loadpaths == null) {
      throw new NullPointerException("No loadpaths provided.");
    }
    for (String path : loadpaths) {
      File file = new File(path, filename);
      if (file.exists()) {
        return file;
      }
    }
    return null;
  }
}
