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

import com.google.clearsilver.jsilver.autoescape.EscapeMode;

import java.util.Iterator;

/**
 * Data wrapper that prevents modifying the delegated Data node or its tree.
 */
public class UnmodifiableData extends DelegatedData {

  private static final String MODIFICATION_ERROR_MSG = "Cannot modify this Data object.";

  public UnmodifiableData(Data delegate) {
    super(delegate);
  }

  @Override
  protected DelegatedData newInstance(Data newDelegate) {
    return newDelegate == null ? null : new UnmodifiableData(newDelegate);
  }

  @Override
  public void copy(Data from) {
    throw new UnsupportedOperationException(MODIFICATION_ERROR_MSG);
  }

  @Override
  public void copy(String toPath, Data from) {
    throw new UnsupportedOperationException(MODIFICATION_ERROR_MSG);
  }

  /**
   * {@link #createChild} calls {@link #getChild} and throws {@link UnsupportedOperationException}
   * if no object was found.
   */
  @Override
  public Data createChild(String path) {
    // Check if child already exists
    Data child = getChild(path);

    if (child == null) {
      // If the child described by path does not exist we throw
      // an exception as we cannot create a new one.
      throw new UnsupportedOperationException(MODIFICATION_ERROR_MSG);
    }
    return child;
  }

  protected class UnmodifiableIterator extends DelegatedIterator {
    UnmodifiableIterator(Iterator<? extends Data> iterator) {
      super(iterator);
    }

    public void remove() {
      throw new UnsupportedOperationException(MODIFICATION_ERROR_MSG);
    }
  }

  /**
   * Override in order to not allow modifying children with remove().
   */
  @Override
  protected Iterator<DelegatedData> newChildIterator() {
    return new UnmodifiableIterator(getDelegate().getChildren().iterator());
  }

  // Below we disable modification operations.

  @Override
  public void setSymlink(String sourcePath, Data destination) {
    throw new UnsupportedOperationException(MODIFICATION_ERROR_MSG);
  }

  @Override
  public void setSymlink(String sourcePath, String destinationPath) {
    throw new UnsupportedOperationException(MODIFICATION_ERROR_MSG);
  }

  @Override
  public void setSymlink(Data symLink) {
    throw new UnsupportedOperationException(MODIFICATION_ERROR_MSG);
  }

  @Override
  public void setAttribute(String key, String value) {
    throw new UnsupportedOperationException(MODIFICATION_ERROR_MSG);
  }

  @Override
  public void removeTree(String path) {
    throw new UnsupportedOperationException(MODIFICATION_ERROR_MSG);
  }

  @Override
  public void setValue(String path, String value) {
    throw new UnsupportedOperationException(MODIFICATION_ERROR_MSG);
  }

  @Override
  public void setValue(String value) {
    throw new UnsupportedOperationException(MODIFICATION_ERROR_MSG);
  }

  @Override
  public void setEscapeMode(EscapeMode mode) {
    throw new UnsupportedOperationException(MODIFICATION_ERROR_MSG);
  }
}
