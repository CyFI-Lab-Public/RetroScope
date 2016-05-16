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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ResourceLoader that pulls all items from memory. This is particularly useful for small templates
 * that can be embedded in code (e.g. in unit tests).
 * 
 * Content needs to be stored first using the {@link #store(String, String)} method.
 * 
 * @see ResourceLoader
 */
public class InMemoryResourceLoader extends BaseResourceLoader {

  private ConcurrentMap<String, String> items = new ConcurrentHashMap<String, String>();

  @Override
  public Reader open(String name) throws IOException {
    String content = items.get(name);
    return content == null ? null : new StringReader(content);
  }

  @Override
  public Reader openOrFail(String name) throws JSilverTemplateNotFoundException, IOException {
    Reader reader = open(name);
    if (reader == null) {
      throw new JSilverTemplateNotFoundException(name);
    } else {
      return reader;
    }
  }

  public void store(String name, String contents) {
    items.put(name, contents);
  }

  public void remove(String name) {
    items.remove(name);
  }

  public ConcurrentMap<String, String> getItems() {
    return items;
  }

}
