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

package org.clearsilver.jni;

import org.clearsilver.CS;
import org.clearsilver.CSFileLoader;
import org.clearsilver.HDF;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * JNI implementation of the CS interface.
 */
public class JniCs implements CS {
  long csptr;

  protected JniHdf globalHDF;
  protected JniHdf localHDF;

  static {
    JNI.loadLibrary();
  }

  JniCs(JniHdf ho) {
    this.globalHDF = null;
    this.localHDF = ho;
    csptr = _init(ho.hdfptr);
  }

  JniCs(JniHdf ho, JniHdf global) {
    this(ho);

    this.globalHDF = global;
    if (global != null) {
      _setGlobalHdf(csptr,global.hdfptr);
    }
  }

  // Specify a new/different global HDF
  public void setGlobalHDF(HDF global) {
    JniHdf globalHdf = JniHdf.cast(global);
    _setGlobalHdf(csptr, globalHdf.hdfptr);
    this.globalHDF = globalHdf;
  }

  // Return global hdf in use
  public HDF getGlobalHDF() {
    return this.globalHDF;
  }

  public void close() {
    if (csptr != 0) {
      _dealloc(csptr);
      csptr = 0;
    }
  }

  // Don't rely on this being called.
  protected void finalize() {
    close();
  }
  
  /**
   * Parses the specified file as if it has template content. The file will
   * be located using the HDF's loadpaths.
   * @param filename the name of file to read in and parse.
   * @throws java.io.FileNotFoundException if the specified file does not
   * exist.
   * @throws IOException other problems reading the file.
   */
  public void parseFile(String filename) throws IOException {
    if (csptr == 0) {
     throw new NullPointerException("CS is closed.");
    }
    _parseFile(csptr, filename, fileLoader != null);
  }

  public void parseStr(String content) {
    if (csptr == 0) {
      throw new NullPointerException("CS is closed.");
    }
    _parseStr(csptr,content);
  }

  public String render() {
    if (csptr == 0) {
      throw new NullPointerException("CS is closed.");
    }
    return _render(csptr, fileLoader != null);
  }


  protected String fileLoad(String filename) throws IOException,
      FileNotFoundException {
    if (csptr == 0) {
      throw new NullPointerException("CS is closed.");
    }
    CSFileLoader aFileLoader = fileLoader;
    if (aFileLoader == null) {
      throw new NullPointerException("No fileLoader specified.");
    } else {
      String result = aFileLoader.load(localHDF, filename);
      if (result == null) {
        throw new NullPointerException("CSFileLoader.load() returned null");
      }
      return result;
    }
  }

  // The optional CS file loader to use to read in files
  private CSFileLoader fileLoader = null;

  /**
   * Get the file loader in use, if any.
   * @return the file loader in use.
   */
  public CSFileLoader getFileLoader() {
    return fileLoader;
  }

  /**
   * Set the CS file loader to use
   * @param fileLoader the file loader that should be used.
   */
  public void setFileLoader(CSFileLoader fileLoader) {
    this.fileLoader = fileLoader;
  }


  // Native methods
  private native long    _init(long ptr);
  private native void   _dealloc(long ptr);
  private native void   _parseFile(long ptr, String filename,
      boolean use_cb) throws IOException;
  private native void   _parseStr(long ptr, String content);
  private native String _render(long ptr, boolean use_cb);
  private native void   _setGlobalHdf(long csptr, long hdfptr);
}
