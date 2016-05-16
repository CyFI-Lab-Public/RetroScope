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

/**
 * This class is meant to hold implementation common to different instances of Data interface.
 */
public abstract class AbstractData implements Data {

  protected EscapeMode escapeMode = EscapeMode.ESCAPE_NONE;

  public int getIntValue() {
    // If we ever use the integer value of a node to create the string
    // representation we must ensure that an empty node is not mistaken
    // for a node with the integer value '0'.
    return TypeConverter.asNumber(getValue());
  }

  public boolean getBooleanValue() {
    // If we ever use the boolean value of a node to create the string
    // representation we must ensure that an empty node is not mistaken
    // for a node with the boolean value 'false'.
    return TypeConverter.asBoolean(getValue());
  }

  // ******************* Convenience methods *******************

  /**
   * Retrieves the value at the specified path in this HDF node's subtree.
   * 
   * Use {@link #getValue(String)} in preference to ensure ClearSilver compatibility.
   */
  public String getValue(String path, String defaultValue) {
    Data child = getChild(path);
    if (child == null) {
      return defaultValue;
    } else {
      String result = child.getValue();
      return result == null ? defaultValue : result;
    }
  }

  /**
   * Retrieves the integer value at the specified path in this HDF node's subtree. If the value does
   * not exist, or cannot be converted to an integer, default_value will be returned.
   * 
   * Use {@link #getValue(String)} in preference to ensure ClearSilver compatibility.
   */
  public int getIntValue(String path, int defaultValue) {
    Data child = getChild(path);
    if (child == null) {
      return defaultValue;
    } else {
      String result = child.getValue();
      try {
        return result == null ? defaultValue : TypeConverter.parseNumber(result);
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }
  }

  /**
   * Retrieves the value at the specified path in this HDF node's subtree. If not found, returns
   * null.
   */
  public String getValue(String path) {
    return getValue(path, null);
  }

  /**
   * Retrieves the value at the specified path in this HDF node's subtree. If not found or invalid,
   * returns 0.
   */
  public int getIntValue(String path) {
    return TypeConverter.asNumber(getChild(path));
  }

  /**
   * Retrieves the value at the specified path in this HDF node's subtree. If not found or invalid,
   * returns false.
   */
  public boolean getBooleanValue(String path) {
    return TypeConverter.asBoolean(getChild(path));
  }

  /**
   * Sets the value at the specified path in this HDF node's subtree.
   */
  public void setValue(String path, String value) {
    Data child = createChild(path);
    child.setValue(value);
  }

  // ******************* String representation *******************

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    toString(stringBuilder, 0);
    return stringBuilder.toString();
  }

  public void toString(StringBuilder out, int indent) {
    try {
      write(out, indent);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe); // COV_NF_LINE
    }
  }

  @Override
  public void optimize() {
  // Do nothing.
  }

  @Override
  public void setEscapeMode(EscapeMode mode) {
    this.escapeMode = mode;
  }

  @Override
  public EscapeMode getEscapeMode() {
    return escapeMode;
  }
}
