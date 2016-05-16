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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * Class that wraps a Data object and exports the same interface. Useful for extending the
 * capabilities of an existing implementation.
 */
public class DelegatedData implements Data {

  private final Data delegate;

  public DelegatedData(Data delegate) {
    if (delegate == null) {
      throw new NullPointerException("Delegate Data must not be null.");
    }
    this.delegate = delegate;
  }

  /**
   * Subclasses will want to override this method to return a Data object of their specific type.
   * 
   * @param newDelegate the Data object to wrap with a new delegator
   * @return a DelegateData type or subclass.
   */
  protected DelegatedData newInstance(Data newDelegate) {
    return newDelegate == null ? null : new DelegatedData(newDelegate);
  }

  protected Data getDelegate() {
    return delegate;
  }

  protected static Data unwrap(Data data) {
    if (data instanceof DelegatedData) {
      data = ((DelegatedData) data).getDelegate();
    }
    return data;
  }

  @Override
  public String getName() {
    return getDelegate().getName();
  }

  @Override
  public String getValue() {
    return getDelegate().getValue();
  }

  @Override
  public int getIntValue() {
    return getDelegate().getIntValue();
  }

  @Override
  public boolean getBooleanValue() {
    return getDelegate().getBooleanValue();
  }

  @Override
  public void setValue(String value) {
    getDelegate().setValue(value);
  }

  @Override
  public String getFullPath() {
    return getDelegate().getFullPath();
  }

  @Override
  public void setAttribute(String key, String value) {
    getDelegate().setAttribute(key, value);
  }

  @Override
  public String getAttribute(String key) {
    return getDelegate().getAttribute(key);
  }

  @Override
  public boolean hasAttribute(String key) {
    return getDelegate().hasAttribute(key);
  }

  @Override
  public int getAttributeCount() {
    return getDelegate().getAttributeCount();
  }

  @Override
  public Iterable<Map.Entry<String, String>> getAttributes() {
    return getDelegate().getAttributes();
  }

  @Override
  public Data getRoot() {
    return newInstance(getDelegate().getRoot());
  }

  @Override
  public Data getParent() {
    return newInstance(getDelegate().getParent());
  }

  @Override
  public boolean isFirstSibling() {
    return getDelegate().isFirstSibling();
  }

  @Override
  public boolean isLastSibling() {
    return getDelegate().isLastSibling();
  }

  @Override
  public Data getNextSibling() {
    return newInstance(getDelegate().getNextSibling());
  }

  @Override
  public int getChildCount() {
    return getDelegate().getChildCount();
  }

  /**
   * Wrapping implementation of iterator that makes sure any Data object returned by the underlying
   * iterator is wrapped with the right DelegatedData type.
   */
  protected class DelegatedIterator implements Iterator<DelegatedData> {
    private final Iterator<? extends Data> iterator;

    DelegatedIterator(Iterator<? extends Data> iterator) {
      this.iterator = iterator;
    }

    public boolean hasNext() {
      return iterator.hasNext();
    }

    public DelegatedData next() {
      return newInstance(iterator.next());
    }

    public void remove() {
      iterator.remove();
    }
  }

  /**
   * Subclasses can override this method to return specialized child iterators. For example, if they
   * don't want to support the remove() operation.
   * 
   * @return Iterator of children of delegate Data object that returns wrapped Data nodes.
   */
  protected Iterator<DelegatedData> newChildIterator() {
    return new DelegatedIterator(getDelegate().getChildren().iterator());
  }

  /**
   * Single Iterable object for each node. All it does is return a DelegatedIterator when asked for
   * iterator.
   */
  private final Iterable<DelegatedData> delegatedIterable = new Iterable<DelegatedData>() {
    public Iterator<DelegatedData> iterator() {
      return newChildIterator();
    }
  };

  @Override
  public Iterable<? extends Data> getChildren() {
    return delegatedIterable;
  }

  @Override
  public Data getChild(String path) {
    return newInstance(getDelegate().getChild(path));
  }

  @Override
  public Data createChild(String path) {
    return newInstance(getDelegate().createChild(path));
  }

  @Override
  public void removeTree(String path) {
    getDelegate().removeTree(path);
  }

  @Override
  public void setSymlink(String sourcePath, String destinationPath) {
    getDelegate().setSymlink(sourcePath, destinationPath);
  }

  @Override
  public void setSymlink(String sourcePath, Data destination) {
    destination = unwrap(destination);
    getDelegate().setSymlink(sourcePath, destination);
  }

  @Override
  public void setSymlink(Data symLink) {
    symLink = unwrap(symLink);
    getDelegate().setSymlink(symLink);
  }

  @Override
  public Data getSymlink() {
    return newInstance(getDelegate().getSymlink());
  }

  @Override
  public void copy(String toPath, Data from) {
    from = unwrap(from);
    getDelegate().copy(toPath, from);
  }

  @Override
  public void copy(Data from) {
    from = unwrap(from);
    getDelegate().copy(from);
  }

  @Override
  public String getValue(String path, String defaultValue) {
    return getDelegate().getValue(path, defaultValue);
  }

  @Override
  public int getIntValue(String path, int defaultValue) {
    return getDelegate().getIntValue(path, defaultValue);
  }

  @Override
  public String getValue(String path) {
    return getDelegate().getValue(path);
  }

  @Override
  public int getIntValue(String path) {
    return getDelegate().getIntValue(path);
  }

  @Override
  public boolean getBooleanValue(String path) {
    return getDelegate().getBooleanValue(path);
  }

  @Override
  public void setValue(String path, String value) {
    getDelegate().setValue(path, value);
  }

  @Override
  public String toString() {
    return getDelegate().toString();
  }

  @Override
  public void toString(StringBuilder out, int indent) {
    getDelegate().toString(out, indent);
  }

  @Override
  public void write(Appendable out, int indent) throws IOException {
    getDelegate().write(out, indent);
  }

  @Override
  public void optimize() {
    getDelegate().optimize();
  }

  @Override
  public void setEscapeMode(EscapeMode mode) {
    getDelegate().setEscapeMode(mode);
  }

  @Override
  public EscapeMode getEscapeMode() {
    return getDelegate().getEscapeMode();
  }
}
