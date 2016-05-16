/*
 * Copyright (C) 2013 DroidDriver committers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.droiddriver.finders;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;

/**
 * Convenience methods and constants for XPath.
 * <p>
 * DroidDriver implementation uses default XPath library on device, so the
 * support may be limited to <a href="http://www.w3.org/TR/xpath/">XPath
 * 1.0</a>. Newer XPath features may not be supported, for example, the
 * fn:matches function.
 */
public class XPaths {

  private XPaths() {}

  /**
   * @return The tag name used to build UiElement DOM. It is preferable to use
   *         this to build XPath instead of String literals.
   */
  public static String tag(String className) {
    return simpleClassName(className);
  }

  /**
   * @return The tag name used to build UiElement DOM. It is preferable to use
   *         this to build XPath instead of String literals.
   */
  public static String tag(Class<?> clazz) {
    return tag(clazz.getName());
  }

  private static String simpleClassName(String name) {
    // the nth anonymous class has a class name ending in "Outer$n"
    // and local inner classes have names ending in "Outer.$1Inner"
    name = name.replaceAll("\\$[0-9]+", "\\$");

    // we want the name of the inner class all by its lonesome
    int start = name.lastIndexOf('$');

    // if this isn't an inner class, just find the start of the
    // top level class name.
    if (start == -1) {
      start = name.lastIndexOf('.');
    }
    return name.substring(start + 1);
  }

  /**
   * @return XPath predicate (with enclosing []) for boolean attribute that is
   *         present
   */
  public static String is(Attribute attribute) {
    return "[@" + attribute.getName() + "]";
  }

  /**
   * @return XPath predicate (with enclosing []) for boolean attribute that is
   *         NOT present
   */
  public static String not(Attribute attribute) {
    return "[not(@" + attribute.getName() + ")]";
  }

  /** @return XPath predicate (with enclosing []) for attribute with value */
  public static String attr(Attribute attribute, String value) {
    return String.format("[@%s=%s]", attribute.getName(), quoteXPathLiteral(value));
  }

  /** @return XPath predicate (with enclosing []) for attribute containing value */
  public static String containsAttr(Attribute attribute, String containedValue) {
    return String.format("[contains(@%s, %s)]", attribute.getName(),
        quoteXPathLiteral(containedValue));
  }

  /** Shorthand for {@link #attr}{@code (Attribute.TEXT, value)} */
  public static String text(String value) {
    return attr(Attribute.TEXT, value);
  }

  /**
   * Adapted from http://stackoverflow.com/questions/1341847/.
   * <p>
   * Produce an XPath literal equal to the value if possible; if not, produce an
   * XPath expression that will match the value. Note that this function will
   * produce very long XPath expressions if a value contains a long run of
   * double quotes.
   */
  @VisibleForTesting
  static String quoteXPathLiteral(String value) {
    // if the value contains only single or double quotes, construct an XPath
    // literal
    if (!value.contains("\"")) {
      return "\"" + value + "\"";
    }
    if (!value.contains("'")) {
      return "'" + value + "'";
    }

    // if the value contains both single and double quotes, construct an
    // expression that concatenates all non-double-quote substrings with
    // the quotes, e.g.:
    // concat("foo", '"', "bar")
    StringBuilder sb = new StringBuilder();
    sb.append("concat(\"");
    Joiner.on("\",'\"',\"").appendTo(sb, value.split("\""));
    sb.append("\")");
    return sb.toString();
  }
}
