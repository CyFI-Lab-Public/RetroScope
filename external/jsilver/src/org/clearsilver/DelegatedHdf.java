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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class that delegates all methods of an HDF object. Made to
 * facilitate the transition to HDF being an interface and thus not
 * extensible in the same way as it was.
 * <p>
 * This class, and its subclasses must take care to wrap or unwrap HDF and CS
 * objects as they are passed through from the callers to the delegate object.
 */
public abstract class DelegatedHdf implements HDF {

  private final HDF hdf;

  public DelegatedHdf(HDF hdf) {
    if (hdf == null) {
      throw new NullPointerException("Null HDF is not allowed in constructor of DelegatedHdf.");
    }
    this.hdf = hdf;
  }

  /**
   * Utility function for concrete ClearsilverFactories to unwrap DelegatedHdfs
   * and get down to a concrete (or unknown) HDF object.
   * @param hdf the possibly DelegatedHdf to unwrap
   * @return the innermost non-DelegatedHdf HDF object.
   */
  public static HDF getFullyUnwrappedHdf(HDF hdf) {
    while (hdf instanceof DelegatedHdf) {
      hdf = ((DelegatedHdf)hdf).getHdf();
    }
    return hdf;
  }

  public HDF getHdf() {
    return hdf;
  }

  /**
   * Method subclasses are required to override with a method that returns a
   * new DelegatedHdf object that wraps the specified HDF object.
   *
   * @param hdf an HDF object that should be wrapped in a new DelegatedHdf
   * object of the same type as this current object.
   * @return an object that is a subclass of DelegatedHdf and which wraps the
   * given HDF object.
   */
  protected abstract DelegatedHdf newDelegatedHdf(HDF hdf);

  public void close() {
    getHdf().close();
  }

  public boolean readFile(String filename) throws IOException, FileNotFoundException {
    return getHdf().readFile(filename);
  }

  public CSFileLoader getFileLoader() {
    return getHdf().getFileLoader();
  }

  public void setFileLoader(CSFileLoader fileLoader) {
    getHdf().setFileLoader(fileLoader);
  }

  public boolean writeFile(String filename) throws IOException {
    return getHdf().writeFile(filename);
  }

  public boolean readString(String data) {
    return getHdf().readString(data);
  }

  public String writeString() {
    return getHdf().writeString();
  }

  public int getIntValue(String hdfname,
      int default_value) {
    return getHdf().getIntValue(hdfname, default_value);
  }

  public String getValue(String hdfname, String default_value) {
    return getHdf().getValue(hdfname, default_value);
  }

  public void setValue(
      String hdfname, String value) {
    getHdf().setValue(hdfname, value);
  }

  public void removeTree(String hdfname) {
    getHdf().removeTree(hdfname);
  }

  public void setSymLink(String hdf_name_src,
      String hdf_name_dest) {
    getHdf().setSymLink(hdf_name_src, hdf_name_dest);
  }

  public void exportDate(
      String hdfname, TimeZone timeZone, Date date) {
    getHdf().exportDate(hdfname, timeZone, date);
  }

  public void exportDate(
      String hdfname, String tz, int tt) {
    getHdf().exportDate(hdfname, tz, tt);
  }

  public DelegatedHdf getObj(String hdfpath) {
    HDF hdf = getHdf().getObj(hdfpath);
    return hdf != null ? newDelegatedHdf(hdf) : null;
  }

  public DelegatedHdf getChild(String hdfpath) {
    HDF hdf = getHdf().getChild(hdfpath);
    return hdf != null ? newDelegatedHdf(hdf) : null;
  }

  public DelegatedHdf getRootObj() {
    HDF hdf = getHdf().getRootObj();
    return hdf != null ? newDelegatedHdf(hdf) : null;
  }

  public boolean belongsToSameRoot(HDF hdf) {
    return getFullyUnwrappedHdf(this).belongsToSameRoot(getFullyUnwrappedHdf(hdf));
  }

  public DelegatedHdf getOrCreateObj(String hdfpath) {
    HDF hdf = getHdf().getOrCreateObj(hdfpath);
    return hdf != null ? newDelegatedHdf(hdf) : null;
  }

  public String objName() {
    return getHdf().objName();
  }

  public String objValue() {
    return getHdf().objValue();
  }

  public DelegatedHdf objChild() {
    HDF hdf = getHdf().objChild();
    return hdf != null ? newDelegatedHdf(hdf) : null;
  }

  public DelegatedHdf objNext() {
    HDF hdf = getHdf().objNext();
    return hdf != null ? newDelegatedHdf(hdf) : null;
  }

  public void copy(String hdfpath, HDF src) {
    if (src != null && src instanceof DelegatedHdf) {
      src = ((DelegatedHdf)src).getHdf();
    }
    getHdf().copy(hdfpath, src);
  }

  public String dump() {
    return getHdf().dump();
  }
}

