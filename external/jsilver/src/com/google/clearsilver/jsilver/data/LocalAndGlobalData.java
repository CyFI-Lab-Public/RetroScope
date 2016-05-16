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

/**
 * This is a special implementation of ChainedData to be used for holding the local and global Data
 * objects (like local and global HDFs in Clearsilver). It prevents writes and modifications to the
 * global Data object and applies them all to the local data object.
 */
public class LocalAndGlobalData extends ChainedData {

  private final Data local;

  /**
   * Creates a Data object that encapsulates both request-scoped local HDF and an application
   * global-scoped HDF that can be read from the template renderer. Part of the backwards
   * compatibility with JNI Clearsilver and its globalHdf support.
   * 
   * @param local the request-specific HDF data that takes priority.
   * @param global application global HDF data that should be read but not written to from the
   *        template renderer.
   */
  public LocalAndGlobalData(Data local, Data global) {
    this(local, global, false);
  }

  /**
   * Creates a Data object that encapsulates both request-scoped local HDF and an application
   * global-scoped HDF that can be read from the template renderer. Part of the backwards
   * compatibility with JNI Clearsilver and its globalHdf support. We wrap the global HDF in an
   * UnmodifiableData delegate
   * 
   * @param local the request-specific HDF data that takes priority.
   * @param global application global HDF data that should be read but not written to from the
   *        template renderer.
   * @param allowGlobalDataModification if {@code true} then enable legacy mode where we do not wrap
   *        the global Data with an Unmodifiable wrapper. Should not be set to {@code true} unless
   *        incompatibilities or performance issues found. Note, that setting to {@code true} could
   *        introduce bugs in templates that acquire local references to the global data structure
   *        and then try to modify them, which is not the intended behavior.
   */
  public LocalAndGlobalData(Data local, Data global, boolean allowGlobalDataModification) {
    super(local, prepareGlobal(global, allowGlobalDataModification));
    this.local = local;
  }

  private static Data prepareGlobal(Data global, boolean allowGlobalDataModification) {
    if (allowGlobalDataModification) {
      return global;
    } else {
      return new UnmodifiableData(global);
    }
  }

  @Override
  public Data createChild(String path) {
    // We never want to modify the global Data object.
    return local.createChild(path);
  }
}
