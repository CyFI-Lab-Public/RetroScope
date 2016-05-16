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
 * Thrown when there is a problem applying auto escaping.
 */
public class JSilverAutoEscapingException extends JSilverException {

  public static final int UNKNOWN_POSITION = -1;

  public JSilverAutoEscapingException(String message, String templateName, int line, int column) {
    super(createMessage(message, templateName, line, column));
  }

  public JSilverAutoEscapingException(String message, String templateName) {
    this(message, templateName, UNKNOWN_POSITION, UNKNOWN_POSITION);
  }

  /**
   * Keeping the same format as JSilverBadSyntaxException.
   */
  private static String createMessage(String message, String resourceName, int line, int column) {
    StringBuilder result = new StringBuilder(message);
    if (resourceName != null) {
      result.append(" resource=").append(resourceName);
    }
    if (line != UNKNOWN_POSITION) {
      result.append(" line=").append(line);
    }
    if (column != UNKNOWN_POSITION) {
      result.append(" column=").append(column);
    }
    return result.toString();
  }

  public JSilverAutoEscapingException(String message) {
    super(message);
  }

  public JSilverAutoEscapingException(String message, Throwable cause) {
    super(message, cause);
  }
}
