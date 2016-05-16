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
 * Utility class that delegates all methods of an CS object. Made to
 * facilitate the transition to CS being an interface and thus not
 * extensible in the same way as it was.
 * <p>
 * This class, and its subclasses must take care to wrap or unwrap HDF and CS
 * objects as they are passed through from the callers to the delegate object.
 *
 */
public abstract class DelegatedCs implements CS {
  private final CS cs;

  public DelegatedCs(CS cs) {
    // Give it an empty HDF. We aren't going to be using the super object anyways.
    this.cs = cs;
  }

  public CS getCs() {
    return cs;
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

  public void setGlobalHDF(HDF global) {
    if (global != null && global instanceof DelegatedHdf) {
      global = ((DelegatedHdf)global).getHdf();
    }
    getCs().setGlobalHDF(global);
  }

  public HDF getGlobalHDF() {
    HDF hdf =  getCs().getGlobalHDF();
    return hdf != null ? newDelegatedHdf(hdf) : null;
  }

  public void close() {
    getCs().close();
  }

  public void parseFile(String filename) throws IOException {
    getCs().parseFile(filename);
  }
  public void parseStr(String content) {
    getCs().parseStr(content);
  }

  public String render() {
    return getCs().render();
  }

  public CSFileLoader getFileLoader() {
    return getCs().getFileLoader();
  }

  public void setFileLoader(CSFileLoader fileLoader) {
    getCs().setFileLoader(fileLoader);
  }

}
