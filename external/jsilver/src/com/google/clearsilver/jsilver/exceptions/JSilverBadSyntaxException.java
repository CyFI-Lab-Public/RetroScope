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

package com.google.clearsilver.jsilver.exceptions;

/**
 * Thrown when resource (e.g. template or HDF) contains bad syntax.
 */
public class JSilverBadSyntaxException extends JSilverException {

  private final String resourceName;

  private final int line;

  private final int column;

  /**
   * Signifies line or column is not known.
   */
  public static final int UNKNOWN_POSITION = -1;

  /**
   * Constructor of JSilverBadSyntaxException.
   * 
   * @param message text of an error message
   * @param lineContent content of a line where error occurred (can be null)
   * @param resourceName name of a file where error occurred (can be null)
   * @param line number of a line in {@code resourceName} where error occurred (ignored if set to
   *        {@link #UNKNOWN_POSITION})
   * @param column number of a column in {@code resourceName} where error occurred (ignored if set
   *        to {@link #UNKNOWN_POSITION})
   * @param cause an original exception of an error. Null value is permitted and indicates that the
   *        cause is nonexistent or unknown.
   */
  public JSilverBadSyntaxException(String message, String lineContent, String resourceName,
      int line, int column, Throwable cause) {
    super(makeMessage(message, lineContent, resourceName, line, column), cause);
    this.resourceName = resourceName;
    this.line = line;
    this.column = column;
  }

  private static String makeMessage(String message, String lineContent, String resourceName,
      int line, int column) {
    StringBuilder result = new StringBuilder(message);
    if (resourceName != null) {
      result.append(" resource=").append(resourceName);
    }
    if (lineContent != null) {
      result.append(" content=").append(lineContent);
    }
    if (line != UNKNOWN_POSITION) {
      result.append(" line=").append(line);
    }
    if (column != UNKNOWN_POSITION) {
      result.append(" column=").append(column);
    }
    return result.toString();
  }

  /**
   * Name of resource that had syntax error (typically a file name).
   */
  public String getResourceName() {
    return resourceName;
  }

  /**
   * Line number this syntax error occured, or {@link #UNKNOWN_POSITION}.
   */
  public int getLine() {
    return line;
  }

  /**
   * Column number this syntax error occured, or {@link #UNKNOWN_POSITION}.
   */
  public int getColumn() {
    return column;
  }

}
