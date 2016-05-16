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

package com.google.clearsilver.jsilver.adaptor;

import com.google.clearsilver.jsilver.exceptions.JSilverTemplateNotFoundException;
import com.google.clearsilver.jsilver.resourceloader.ResourceLoader;

import org.clearsilver.CSFileLoader;
import org.clearsilver.CSUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

/**
 * Wrap a CSFileLoader with a ResourceLoader
 */
public class ResourceLoaderAdaptor implements ResourceLoader {

  private final JHdf hdf;
  private final LoadPathToFileCache loadPathCache;
  private final CSFileLoader csFileLoader;
  private List<String> loadPaths;

  ResourceLoaderAdaptor(JHdf hdf, LoadPathToFileCache loadPathCache, CSFileLoader csFileLoader) {
    this.hdf = hdf;
    this.loadPathCache = loadPathCache;
    this.csFileLoader = csFileLoader;
  }

  @Override
  public Reader open(String name) throws IOException {
    if (csFileLoader != null) {
      if (hdf.getData() == null) {
        throw new IllegalStateException("HDF is already closed");
      }
      return new StringReader(csFileLoader.load(hdf, name));
    } else {
      File file = locateFile(name);
      if (file == null) {
        throw new FileNotFoundException("Could not locate file " + name);
      }
      return new InputStreamReader(new FileInputStream(file), "UTF-8");
    }
  }

  @Override
  public Reader openOrFail(String name) throws JSilverTemplateNotFoundException, IOException {
    Reader reader = open(name);
    if (reader == null) {
      final StringBuffer text = new StringBuffer();
      text.append("No file '");
      text.append(name);
      text.append("' ");
      if (loadPaths == null || loadPaths.isEmpty()) {
        text.append("with no load paths");
      } else if (loadPaths.size() == 1) {
        text.append("inside directory '");
        text.append(loadPaths.get(0));
        text.append("'");
      } else {
        text.append("inside directories ( ");
        for (String path : getLoadPaths()) {
          text.append("'");
          text.append(path);
          text.append("' ");
        }
        text.append(")");
      }
      throw new JSilverTemplateNotFoundException(text.toString());
    } else {
      return reader;
    }
  }

  /**
   * 
   * @param name name of the file to locate.
   * @return a File object corresponding to the existing file or {@code null} if it does not exist.
   */
  File locateFile(String name) {
    if (name.startsWith(File.separator)) {
      // Full path to file was given.
      File file = newFile(name);
      return file.exists() ? file : null;
    }
    File file = null;
    // loadPathCache is null when load path caching is disabled at the
    // JSilverFactory level. This is implied by setting cache size
    // to 0 using JSilverOptions.setLoadPathCacheSize(0).
    if (loadPathCache != null) {
      String filePath = loadPathCache.lookup(getLoadPaths(), name);
      if (filePath != null) {
        file = newFile(filePath);
        return file.exists() ? file : null;
      }
    }

    file = locateFile(getLoadPaths(), name);
    if (file != null && loadPathCache != null) {
      loadPathCache.add(getLoadPaths(), name, file.getAbsolutePath());
    }
    return file;
  }

  /**
   * Given an ordered list of directories to look in, locate the specified file. Returns
   * <code>null</code> if file not found.
   * <p>
   * This is copied from {@link org.clearsilver.CSUtil#locateFile(java.util.List, String)} but has
   * one important difference. It calls our subclassable newFile method.
   * 
   * @param loadPaths the ordered list of paths to search.
   * @param filename the name of the file.
   * @return a File object corresponding to the file. <code>null</code> if file not found.
   */
  File locateFile(List<String> loadPaths, String filename) {
    if (filename == null) {
      throw new NullPointerException("No filename provided");
    }
    if (loadPaths == null) {
      throw new NullPointerException("No loadpaths provided.");
    }
    for (String path : loadPaths) {
      File file = newFile(path, filename);
      if (file.exists()) {
        return file;
      }
    }
    return null;
  }

  /**
   * Separate methods to allow tests to subclass and override File creation and return mocks or
   * fakes.
   */
  File newFile(String filename) {
    return new File(filename);
  }

  File newFile(String path, String filename) {
    return new File(path, filename);
  }

  @Override
  public void close(Reader reader) throws IOException {
    reader.close();
  }

  @Override
  public Object getKey(String filename) {
    if (filename.startsWith(File.separator)) {
      return filename;
    } else {
      File file = locateFile(filename);
      if (file == null) {
        // The file does not exist, use the full loadpath and file name as the
        // key.
        return LoadPathToFileCache.makeCacheKey(getLoadPaths(), filename);
      } else {
        return file.getAbsolutePath();
      }
    }
  }

  /**
   * Some applications, e.g. online help, need to know when a file has changed due to a symlink
   * modification hence the use of {@link File#getCanonicalFile()}, if possible.
   */
  @Override
  public Object getResourceVersionId(String filename) {
    File file = locateFile(filename);
    if (file == null) {
      return null;
    }

    String fullPath;
    try {
      fullPath = file.getCanonicalPath();
    } catch (IOException e) {
      fullPath = file.getAbsolutePath();
    }
    return String.format("%s@%s", fullPath, file.lastModified());
  }

  final CSFileLoader getCSFileLoader() {
    return csFileLoader;
  }

  private synchronized List<String> getLoadPaths() {
    if (loadPaths == null) {
      if (hdf.getData() == null) {
        throw new IllegalStateException("HDF is already closed");
      }
      loadPaths = CSUtil.getLoadPaths(hdf, true);
    }
    return loadPaths;
  }
}
