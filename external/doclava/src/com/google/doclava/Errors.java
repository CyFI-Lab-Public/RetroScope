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

package com.google.doclava;

import java.util.Set;
import java.util.TreeSet;

public class Errors {
  public static boolean hadError = false;
  private static boolean warningsAreErrors = false;
  private static TreeSet<ErrorMessage> allErrors = new TreeSet<ErrorMessage>();

  public static class ErrorMessage implements Comparable {
    Error error;
    SourcePositionInfo pos;
    String msg;

    ErrorMessage(Error e, SourcePositionInfo p, String m) {
      error = e;
      pos = p;
      msg = m;
    }

    public int compareTo(Object o) {
      ErrorMessage that = (ErrorMessage) o;
      int r = this.pos.compareTo(that.pos);
      if (r != 0) return r;
      return this.msg.compareTo(that.msg);
    }

    @Override
    public String toString() {
      String whereText = this.pos == null ? "unknown: " : this.pos.toString() + ':';
      return whereText + this.msg;
    }
    
    public Error error() {
      return error;
    }
  }

  public static void error(Error error, SourcePositionInfo where, String text) {
    if (error.level == HIDDEN) {
      return;
    }

    int level = (!warningsAreErrors && error.level == WARNING) ? WARNING : ERROR;
    String which = level == WARNING ? " warning " : " error ";
    String message = which + error.code + ": " + text;

    if (where == null) {
      where = new SourcePositionInfo("unknown", 0, 0);
    }

    allErrors.add(new ErrorMessage(error, where, message));

    if (error.level == ERROR || (warningsAreErrors && error.level == WARNING)) {
      hadError = true;
    }
  }
  
  public static void clearErrors() {
    hadError = false;
    allErrors.clear();
  }

  public static void printErrors() {
    printErrors(allErrors);
  }
  
  public static void printErrors(Set<ErrorMessage> errors) {
    for (ErrorMessage m : errors) {
      if (m.error.level == WARNING) {
        System.err.println(m.toString());
      }
    }
    for (ErrorMessage m : errors) {
      if (m.error.level == ERROR) {
        System.err.println(m.toString());
      }
    }
  }
  
  public static Set<ErrorMessage> getErrors() {
    return allErrors;
  }

  public static int HIDDEN = 0;
  public static int WARNING = 1;
  public static int ERROR = 2;

  public static void setWarningsAreErrors(boolean val) {
    warningsAreErrors = val;
  }

  public static class Error {
    public int code;
    public int level;

    public Error(int code, int level) {
      this.code = code;
      this.level = level;
    }
    
    public String toString() {
      return "Error #" + this.code;
    }
  }

  // Errors for API verification
  public static Error PARSE_ERROR = new Error(1, ERROR);
  public static Error ADDED_PACKAGE = new Error(2, WARNING);
  public static Error ADDED_CLASS = new Error(3, WARNING);
  public static Error ADDED_METHOD = new Error(4, WARNING);
  public static Error ADDED_FIELD = new Error(5, WARNING);
  public static Error ADDED_INTERFACE = new Error(6, WARNING);
  public static Error REMOVED_PACKAGE = new Error(7, WARNING);
  public static Error REMOVED_CLASS = new Error(8, WARNING);
  public static Error REMOVED_METHOD = new Error(9, WARNING);
  public static Error REMOVED_FIELD = new Error(10, WARNING);
  public static Error REMOVED_INTERFACE = new Error(11, WARNING);
  public static Error CHANGED_STATIC = new Error(12, WARNING);
  public static Error ADDED_FINAL = new Error(13, WARNING);
  public static Error CHANGED_TRANSIENT = new Error(14, WARNING);
  public static Error CHANGED_VOLATILE = new Error(15, WARNING);
  public static Error CHANGED_TYPE = new Error(16, WARNING);
  public static Error CHANGED_VALUE = new Error(17, WARNING);
  public static Error CHANGED_SUPERCLASS = new Error(18, WARNING);
  public static Error CHANGED_SCOPE = new Error(19, WARNING);
  public static Error CHANGED_ABSTRACT = new Error(20, WARNING);
  public static Error CHANGED_THROWS = new Error(21, WARNING);
  public static Error CHANGED_NATIVE = new Error(22, HIDDEN);
  public static Error CHANGED_CLASS = new Error(23, WARNING);
  public static Error CHANGED_DEPRECATED = new Error(24, WARNING);
  public static Error CHANGED_SYNCHRONIZED = new Error(25, ERROR);
  public static Error ADDED_FINAL_UNINSTANTIABLE = new Error(26, WARNING);
  public static Error REMOVED_FINAL = new Error(27, WARNING);

  // Errors in javadoc generation
  public static final Error UNRESOLVED_LINK = new Error(101, WARNING);
  public static final Error BAD_INCLUDE_TAG = new Error(102, WARNING);
  public static final Error UNKNOWN_TAG = new Error(103, WARNING);
  public static final Error UNKNOWN_PARAM_TAG_NAME = new Error(104, WARNING);
  public static final Error UNDOCUMENTED_PARAMETER = new Error(105, HIDDEN);
  public static final Error BAD_ATTR_TAG = new Error(106, ERROR);
  public static final Error BAD_INHERITDOC = new Error(107, HIDDEN);
  public static final Error HIDDEN_LINK = new Error(108, WARNING);
  public static final Error HIDDEN_CONSTRUCTOR = new Error(109, WARNING);
  public static final Error UNAVAILABLE_SYMBOL = new Error(110, ERROR);
  public static final Error HIDDEN_SUPERCLASS = new Error(111, WARNING);
  public static final Error DEPRECATED = new Error(112, HIDDEN);
  public static final Error DEPRECATION_MISMATCH = new Error(113, WARNING);
  public static final Error MISSING_COMMENT = new Error(114, WARNING);
  public static final Error IO_ERROR = new Error(115, HIDDEN);
  public static final Error NO_SINCE_DATA = new Error(116, HIDDEN);
  public static final Error NO_FEDERATION_DATA = new Error(117, WARNING);
  public static final Error BROKEN_SINCE_FILE = new Error(118, ERROR);
  public static final Error INVALID_CONTENT_TYPE = new Error(119, ERROR);
  public static final Error INVALID_SAMPLE_INDEX = new Error(120, ERROR);

  public static final Error[] ERRORS =
      {UNRESOLVED_LINK, BAD_INCLUDE_TAG, UNKNOWN_TAG, UNKNOWN_PARAM_TAG_NAME,
          UNDOCUMENTED_PARAMETER, BAD_ATTR_TAG, BAD_INHERITDOC, HIDDEN_LINK, HIDDEN_CONSTRUCTOR,
          UNAVAILABLE_SYMBOL, HIDDEN_SUPERCLASS, DEPRECATED, DEPRECATION_MISMATCH, MISSING_COMMENT,
          IO_ERROR, NO_SINCE_DATA, NO_FEDERATION_DATA, PARSE_ERROR, ADDED_PACKAGE, ADDED_CLASS,
          ADDED_METHOD, ADDED_FIELD, ADDED_INTERFACE, REMOVED_PACKAGE, REMOVED_CLASS,
          REMOVED_METHOD, REMOVED_FIELD, REMOVED_INTERFACE, CHANGED_STATIC, ADDED_FINAL,
          CHANGED_TRANSIENT, CHANGED_VOLATILE, CHANGED_TYPE, CHANGED_VALUE, CHANGED_SUPERCLASS,
          CHANGED_SCOPE, CHANGED_ABSTRACT, CHANGED_THROWS, CHANGED_NATIVE, CHANGED_CLASS,
          CHANGED_DEPRECATED, CHANGED_SYNCHRONIZED, ADDED_FINAL_UNINSTANTIABLE, REMOVED_FINAL,
          BROKEN_SINCE_FILE, INVALID_CONTENT_TYPE};

  public static boolean setErrorLevel(int code, int level) {
    for (Error e : ERRORS) {
      if (e.code == code) {
        e.level = level;
        return true;
      }
    }
    return false;
  }
}
