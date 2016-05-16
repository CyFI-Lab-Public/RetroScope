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

import com.google.clearsilver.jsilver.JSilverOptions;
import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.data.DataFactory;
import com.google.clearsilver.jsilver.data.Parser;
import com.google.clearsilver.jsilver.exceptions.JSilverBadSyntaxException;

import org.clearsilver.CSFileLoader;
import org.clearsilver.HDF;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.TimeZone;

/**
 * Adaptor that wraps a JSilver Data object so it can be used as an HDF object.
 */
public class JHdf implements HDF {

  // Only changed to null on close()
  private Data data;
  private final DataFactory dataFactory;
  private final JSilverOptions options;

  private final LoadPathToFileCache loadPathCache;
  private ResourceLoaderAdaptor resourceLoader;


  JHdf(Data data, DataFactory dataFactory, LoadPathToFileCache loadPathCache, JSilverOptions options) {
    this.data = data;
    this.loadPathCache = loadPathCache;
    this.dataFactory = dataFactory;
    this.options = options;
    this.resourceLoader = new ResourceLoaderAdaptor(this, loadPathCache, null);
  }

  static JHdf cast(HDF hdf) {
    if (!(hdf instanceof JHdf)) {
      throw new IllegalArgumentException("HDF object not of type JHdf.  "
          + "Make sure you use the same ClearsilverFactory to construct all "
          + "related HDF and CS objects.");
    }
    return (JHdf) hdf;
  }

  Data getData() {
    return data;
  }

  ResourceLoaderAdaptor getResourceLoaderAdaptor() {
    return resourceLoader;
  }

  @Override
  public void close() {
    // This looks pointless but it actually reduces the lifetime of the large
    // Data object as far as the garbage collector is concerned and
    // dramatically improves performance.
    data = null;
  }

  @Override
  public boolean readFile(String filename) throws IOException {
    dataFactory.loadData(filename, resourceLoader, data);
    return false;
  }

  @Override
  public CSFileLoader getFileLoader() {
    return resourceLoader.getCSFileLoader();
  }

  @Override
  public void setFileLoader(CSFileLoader fileLoader) {
    this.resourceLoader = new ResourceLoaderAdaptor(this, loadPathCache, fileLoader);
  }

  @Override
  public boolean writeFile(String filename) throws IOException {
    FileWriter writer = new FileWriter(filename);
    try {
      data.write(writer, 2);
    } finally {
      writer.close();
    }
    return true;
  }

  @Override
  public boolean readString(String content) {
    Parser hdfParser = dataFactory.getParser();
    try {
      hdfParser.parse(new StringReader(content), data, new Parser.ErrorHandler() {
        public void error(int line, String lineContent, String fileName, String errorMessage) {
          throw new JSilverBadSyntaxException("HDF parsing error : '" + errorMessage + "'",
              lineContent, fileName, line, JSilverBadSyntaxException.UNKNOWN_POSITION, null);
        }
      }, resourceLoader, null, options.getIgnoreAttributes());
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  public int getIntValue(String hdfName, int defaultValue) {
    return data.getIntValue(hdfName, defaultValue);
  }

  @Override
  public String getValue(String hdfName, String defaultValue) {
    return data.getValue(hdfName, defaultValue);
  }

  @Override
  public void setValue(String hdfName, String value) {
    data.setValue(hdfName, value);
  }

  @Override
  public void removeTree(String hdfName) {
    data.removeTree(hdfName);
  }

  @Override
  public void setSymLink(String hdfNameSrc, String hdfNameDest) {
    data.setSymlink(hdfNameSrc, hdfNameDest);
  }

  @Override
  public void exportDate(String hdfName, TimeZone timeZone, Date date) {
    throw new UnsupportedOperationException("TBD");
  }

  @Override
  public void exportDate(String hdfName, String tz, int tt) {
    throw new UnsupportedOperationException("TBD");
  }

  @Override
  public HDF getObj(String hdfpath) {
    Data d = data.getChild(hdfpath);
    return d == null ? null : new JHdf(d, dataFactory, loadPathCache, options);
  }

  @Override
  public HDF getChild(String hdfpath) {
    Data d = data.getChild(hdfpath);
    if (d == null) {
      return null;
    }
    for (Data child : d.getChildren()) {
      if (child.isFirstSibling()) {
        return new JHdf(child, dataFactory, loadPathCache, options);
      } else {
        // The first child returned should be the first sibling. Throw an error
        // if not.
        throw new IllegalStateException("First child was not first sibling.");
      }
    }
    return null;
  }

  @Override
  public HDF getRootObj() {
    Data root = data.getRoot();
    if (root == data) {
      return this;
    } else {
      return new JHdf(root, dataFactory, loadPathCache, options);
    }
  }

  @Override
  public boolean belongsToSameRoot(HDF hdf) {
    JHdf jHdf = cast(hdf);
    return this.data.getRoot() == jHdf.data.getRoot();
  }

  @Override
  public HDF getOrCreateObj(String hdfpath) {
    return new JHdf(data.createChild(hdfpath), dataFactory, loadPathCache, options);
  }

  @Override
  public String objName() {
    return data.getName();
  }

  @Override
  public String objValue() {
    return data.getValue();
  }

  @Override
  public HDF objChild() {
    for (Data child : data.getChildren()) {
      if (child.isFirstSibling()) {
        return new JHdf(child, dataFactory, loadPathCache, options);
      }
    }
    return null;
  }

  @Override
  public HDF objNext() {
    Data next = data.getNextSibling();
    return next == null ? null : new JHdf(next, dataFactory, loadPathCache, options);
  }

  @Override
  public void copy(String hdfpath, HDF src) {
    JHdf srcJHdf = cast(src);
    if (hdfpath.equals("")) {
      data.copy(srcJHdf.data);
    } else {
      data.copy(hdfpath, srcJHdf.data);
    }
  }

  @Override
  public String dump() {
    StringBuilder sb = new StringBuilder();
    try {
      data.write(sb, 0);
      return sb.toString();
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public String writeString() {
    return dump();
  }

  @Override
  public String toString() {
    return dump();
  }

  /**
   * JSilver-specific method that optimizes the underlying data object. Should only be used on
   * long-lived HDF objects (e.g. global HDF).
   */
  public void optimize() {
    data.optimize();
  }
}
