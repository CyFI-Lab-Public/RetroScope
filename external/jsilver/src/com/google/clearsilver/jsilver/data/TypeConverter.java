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
 * Static methods for converting stuff in a ClearSilver compatible way.
 */
public class TypeConverter {
  private TypeConverter() {}

  private static final String ZERO = "0";
  private static final String ONE = "1";

  /**
   * Determines if the given data node exists in a ClearSilver compatible way.
   */
  public static boolean exists(Data data) {
    return data != null && data.getValue() != null;
  }

  /**
   * Helper method to safely convert an arbitrary data instance (including null) into a valid
   * (non-null) string representation.
   */
  public static String asString(Data data) {
    // Non-existent variables become the empty string
    // (the data instance will return null to us)
    String value = data != null ? data.getValue() : null;
    return value != null ? value : "";
  }

  /**
   * Parses a non-null string in a ClearSilver compatible way.
   * 
   * The is the underlying parsing function which can fail for badly formatted strings. It is really
   * important that anyone doing parsing of strings calls this function (rather than doing it
   * themselves).
   * 
   * This is an area where JSilver and ClearSilver have some notable differences. ClearSilver relies
   * on the template compiler to parse strings in the template and a different parser at runtime for
   * HDF values. JSilver uses the same code for both cases.
   * 
   * In ClearSilver HDF: Numbers are parsed sequentially and partial results are returned when an
   * invalid character is reached. This means that {@code "123abc"} parses to {@code 123}.
   * 
   * Additionally, ClearSilver doesn't do hex in HDF values, so {@code "a.b=0x123"} will just
   * resolve to {@code 0}.
   * 
   * In ClearSilver templates: Hex is supported, including negative values.
   * 
   * In JSilver: A string must be a complete, valid numeric value for parsing. This means {@code
   * "123abc"} is invalid and will default to {@code 0}.
   * 
   * In JSilver: Positive hex values are supported for both HDF and templates but negative values
   * aren't. This means a template containing something like "<?cs if:foo == -0xff ?>" will parse
   * correctly but fail to render.
   * 
   * @throws NumberFormatException is the string is badly formatted
   */
  public static int parseNumber(String value) throws NumberFormatException {
    // NOTE: This is likely to be one of the areas we will want to optimize
    // for speed eventually.
    if (value.startsWith("0x") || value.startsWith("0X")) {
      return Integer.parseInt(value.substring(2), 16);
    } else {
      return Integer.parseInt(value);
    }
  }

  /**
   * Parses and returns the given string as an integer in a ClearSilver compatible way.
   */
  public static int asNumber(String value) {
    if (value == null || value.isEmpty()) {
      return 0;
    }
    // fast detection for common constants to avoid parsing common values
    // TODO: Maybe push this down into parseNumber ??
    if (value.equals(ONE)) {
      return 1;
    }
    if (value.equals(ZERO)) {
      return 0;
    }
    try {
      return parseNumber(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /**
   * Helper method to safely convert an arbitrary data instance (including null) into a valid
   * integer representation.
   */
  public static int asNumber(Data data) {
    // Non-existent variables become zero
    return data != null ? data.getIntValue() : 0;
  }

  /**
   * Parses and returns the given string as a boolean in a ClearSilver compatible way.
   */
  public static boolean asBoolean(String value) {
    if (value == null || value.isEmpty()) {
      return false;
    }
    // fast detection for common constants to avoid parsing common values
    if (value.equals(ONE)) {
      return true;
    }
    if (value.equals(ZERO)) {
      return false;
    }
    
    // fast detection of any string not starting with '0'
    if (value.charAt(0) != '0') {
      return true;
    }
    
    try {
      return parseNumber(value) != 0;
    } catch (NumberFormatException e) {
      // Unlike number parsing, we return a positive value when the
      // string is badly formatted (it's what clearsilver does).
      return true;
    }
  }

  /**
   * Helper method to safely convert an arbitrary data instance (including null) into a valid
   * boolean representation.
   */
  public static boolean asBoolean(Data data) {
    // Non-existent variables become false
    return data != null ? data.getBooleanValue() : false;
  }
}
