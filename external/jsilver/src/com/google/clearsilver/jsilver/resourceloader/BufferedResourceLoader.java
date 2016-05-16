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

import java.io.Reader;
import java.io.BufferedReader;

/**
 * Base class for ResourceLoader implementations that require the Reader to be buffered (i.e.
 * there's IO latency involved).
 * 
 * @see ResourceLoader
 */
public abstract class BufferedResourceLoader extends BaseResourceLoader {

  public static final int DEFAULT_BUFFER_SIZE = 1024;
  public static final String DEFAULT_CHARACTER_SET = "UTF-8";

  private int bufferSize = DEFAULT_BUFFER_SIZE;
  private String characterSet = DEFAULT_CHARACTER_SET;

  /**
   * Subclasses can wrap a Reader in a BufferedReader by calling this method.
   */
  protected Reader buffer(Reader reader) {
    return reader == null ? null : new BufferedReader(reader, bufferSize);
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  public void setCharacterSet(String characterSet) {
    this.characterSet = characterSet;
  }

  public String getCharacterSet() {
    return characterSet;
  }

}
