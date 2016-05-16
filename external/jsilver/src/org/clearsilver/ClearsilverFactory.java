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

/**
 * A factory for constructing new CS and HDF objects.  Allows applications to
 * provide subclasses of HDF or CS to be used by the Java Clearsilver
 * templating system.
 */
public interface ClearsilverFactory {

  /**
   * Create a new CS object.
   * @param hdf the HDF object to use in constructing the CS object.
   * @return a new CS object
   */
  public CS newCs(HDF hdf);

  /**
   * Create a new CS object.
   * @param hdf the HDF object to use in constructing the CS object.
   * @param globalHdf the global HDF object to use in constructing the
   * CS object.
   * @return a new CS object
   */
  public CS newCs(HDF hdf, HDF globalHdf);

  /**
   * Create a new HDF object.
   * @return a new HDF object
   */
  public HDF newHdf();
}
