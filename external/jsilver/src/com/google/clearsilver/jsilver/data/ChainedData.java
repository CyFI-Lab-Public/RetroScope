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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Implementation of Data that allows for multiple underlying Data objects and checks each one in
 * order for a value before giving up. Behaves like local HDF and global HDF in the JNI
 * implementation of Clearsilver. This is only meant to be a root Data object and hardcodes that
 * fact.
 * <p>
 * Note: If you have elements foo.1, foo.2, foo.3 in first Data object and foo.4, foo.5, foo.6 in
 * second Data object, then fetching children of foo will return only foo.1 foo.2 foo.3 from first
 * Data object.
 */
public class ChainedData extends DelegatedData {
  public static final Logger logger = Logger.getLogger(ChainedData.class.getName());

  // This mode allows developers to locate occurrences where they set the same HDF
  // variable in multiple Data objects in the chain, which usually indicates
  // bad planning or misuse.
  public static final boolean DEBUG_MULTIPLE_ASSIGNMENTS = false;

  Data[] dataList;

  /**
   * Optmization for case of single item.
   * 
   * @param data a single data object to wrap.
   */
  public ChainedData(Data data) {
    super(data);
    this.dataList = new Data[] {data};
  }

  public ChainedData(Data... dataList) {
    super(getFirstData(dataList));
    this.dataList = dataList;
  }

  public ChainedData(List<Data> dataList) {
    super(getFirstData(dataList));
    this.dataList = dataList.toArray(new Data[dataList.size()]);
  }

  @Override
  protected DelegatedData newInstance(Data newDelegate) {
    return newDelegate == null ? null : new ChainedData(newDelegate);
  }

  private static Data getFirstData(Data[] dataList) {
    if (dataList.length == 0) {
      throw new IllegalArgumentException("Must pass in at least one Data object to ChainedData.");
    }
    Data first = dataList[0];
    if (first == null) {
      throw new IllegalArgumentException("ChainedData does not accept null Data objects.");
    }
    return first;
  }

  private static Data getFirstData(List<Data> dataList) {
    if (dataList.size() == 0) {
      throw new IllegalArgumentException("Must pass in at least one Data object to ChainedData.");
    }
    Data first = dataList.get(0);
    if (first == null) {
      throw new IllegalArgumentException("ChainedData does not accept null Data objects.");
    }
    return first;
  }

  @Override
  public Data getChild(String path) {
    ArrayList<Data> children = null;
    Data first = null;
    for (Data d : dataList) {
      Data child = d.getChild(path);
      if (child != null) {
        if (!DEBUG_MULTIPLE_ASSIGNMENTS) {
          // If not in debug mode just return the first match. This assumes we are using the new
          // style of VariableLocator that does not iteratively ask for each HDF path element
          // separately.
          return child;
        }
        if (first == null) {
          // First match found
          first = child;
        } else if (children == null) {
          // Second match found
          children = new ArrayList<Data>(dataList.length);
          children.add(first);
          children.add(child);
        } else {
          // Third or more match found
          children.add(child);
        }
      }
    }
    if (children == null) {
      // 0 or 1 matches. Return first which is null or Data.
      return first;
    } else {
      // Multiple matches. Pass back the first item found. This is only hit when
      // DEBUG_MULTIPLE_ASSIGNMENTS is true.
      logger.info("Found " + children.size() + " matches for path " + path);
      return first;
    }
  }

  @Override
  public Data createChild(String path) {
    Data child = getChild(path);
    if (child != null) {
      return child;
    } else {
      // We don't call super because we don't want to wrap the result in DelegatedData.
      return dataList[0].createChild(path);
    }
  }

  @Override
  public String getValue(String path, String defaultValue) {
    Data child = getChild(path);
    if (child != null && child.getValue() != null) {
      return child.getValue();
    } else {
      return defaultValue;
    }
  }

  @Override
  public int getIntValue(String path, int defaultValue) {
    Data child = getChild(path);
    if (child != null) {
      String value = child.getValue();
      try {
        return value == null ? defaultValue : TypeConverter.parseNumber(value);
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    } else {
      return defaultValue;
    }
  }

  @Override
  public String getValue(String path) {
    Data child = getChild(path);
    if (child != null) {
      return child.getValue();
    } else {
      return null;
    }
  }

  @Override
  public int getIntValue(String path) {
    Data child = getChild(path);
    if (child != null) {
      return child.getIntValue();
    } else {
      return 0;
    }
  }

  @Override
  public boolean getBooleanValue(String path) {
    Data child = getChild(path);
    if (child != null) {
      return child.getBooleanValue();
    } else {
      return false;
    }
  }

  @Override
  public void toString(StringBuilder out, int indent) {
    for (Data d : dataList) {
      d.toString(out, indent);
    }
  }

  @Override
  public void write(Appendable out, int indent) throws IOException {
    for (Data d : dataList) {
      d.write(out, indent);
    }
  }

  @Override
  public void optimize() {
    for (Data d : dataList) {
      d.optimize();
    }
  }
}
