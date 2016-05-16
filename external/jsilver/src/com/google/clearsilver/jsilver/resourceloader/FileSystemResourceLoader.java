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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Loads resources from a directory.
 * 
 * @see ResourceLoader
 */
public class FileSystemResourceLoader extends BufferedResourceLoader {

  private final File rootDir;

  public FileSystemResourceLoader(File rootDir) {
    this.rootDir = rootDir;
  }

  public FileSystemResourceLoader(String rootDir) {
    this(new File(rootDir));
  }

  @Override
  public Reader open(String name) throws IOException {
    File file = new File(rootDir, name);
    // Check for non-directory rather than is-file so that reads from
    // e.g. pipes work.
    if (file.exists() && !file.isDirectory() && file.canRead()) {
      return buffer(new InputStreamReader(new FileInputStream(file), getCharacterSet()));
    } else {
      return null;
    }
  }

  @Override
  public Reader openOrFail(String name) throws JSilverTemplateNotFoundException, IOException {
    Reader reader = open(name);
    if (reader == null) {
      throw new JSilverTemplateNotFoundException("No file '" + name + "' inside directory '"
          + rootDir + "'");
    } else {
      return reader;
    }
  }

  /**
   * Some applications, e.g. online help, need to know when a file has changed due to a symlink
   * modification hence the use of {@link File#getCanonicalFile()}, if possible.
   */
  @Override
  public Object getResourceVersionId(String filename) {
    File file = new File(rootDir, filename);
    // Check for non-directory rather than is-file so that reads from
    // e.g. pipes work.
    if (file.exists() && !file.isDirectory() && file.canRead()) {
      String fullPath;
      try {
        fullPath = file.getCanonicalPath();
      } catch (IOException e) {
        fullPath = file.getAbsolutePath();
      }
      return String.format("%s@%s", fullPath, file.lastModified());
    } else {
      return null;
    }
  }
}
