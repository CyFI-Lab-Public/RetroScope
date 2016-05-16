/**
 * Copyright (c) 2004, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.mail.common.html.parser;

import com.google.android.mail.common.base.Preconditions;

import java.util.Set;

/**
 * HTML class defines Element and Attribute classes.
 *
 * @author jlim@google.com (Jing Yee Lim)
 */
public final class HTML {

  /**
   * Html element
   */
  public static final class Element {

    // TODO(ptucker) other candidate types are list and form elements. Better for this to be
    // enumerated type.
    /** Types */
    public static final int NO_TYPE = 0;
    public static final int TABLE_TYPE = 1;

    /**
     * INLINE - charater level elements and text strings
     * BLOCK  - block-like elements; e.g., paragraphs and lists
     * NONE   - everything else
     */
    public enum Flow {
      INLINE,
      BLOCK,
      NONE
    }

    private final String name;
    private final int type;
    private final boolean empty;
    private final boolean optionalEndTag;
    private final boolean breaksFlow;
    private final Flow flow;

    /**
     * Construct an Element.
     *
     * NOTE: Even though breaksFlow and flow are named similarly, they're not quite the same thing.
     * Flow refers to whether the element is inherently character or block level. Breaks flow
     * refers to whether it forces a line break.
     *
     * @throws IllegalArgumentException if name or flow is null.
     */
    public Element(String name, int type, boolean empty,
                   boolean optionalEndTag, boolean breaksFlow, Flow flow) {
      Preconditions.checkNotNull(name, "Element name can not be null");
      Preconditions.checkNotNull(flow, "Element flow can not be null");
      this.name = name;
      this.type = type;
      this.empty = empty;
      this.optionalEndTag = optionalEndTag;
      this.breaksFlow = breaksFlow;
      this.flow = flow;
    }

    /**
     * Construct an Element with inline=true.
     */
    public Element(String name, int type, boolean empty,
                   boolean optionalEndTag, boolean breaksFlow) {
      this(name, type, empty, optionalEndTag, breaksFlow, Flow.NONE);
    }

    /** Name of the element, in lowercase, e.g. "a", "br" */
    public String getName() {
      return name;
    }

    /** Type, e.g. TABLE_TYPE */
    public int getType() {
      return type;
    }

    /** True if it's empty, has no inner elements or end tag */
    public boolean isEmpty() {
      return empty;
    }

    /** True if the end tag is optional */
    public boolean isEndTagOptional() {
      return optionalEndTag;
    }

    /**
     * True if it breaks the flow, and may force a new line before/after the
     * tag.
     */
    public boolean breaksFlow() {
      return breaksFlow;
    }

    /** Flow type. */
    public Flow getFlow() {
      return flow;
    }

    /**
     * @return just name, not proper HTML
     */
    @Override
    public String toString() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof HTML.Element) {
        HTML.Element that = (HTML.Element) o;
        return this.name.equals(that.name);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return this.name.hashCode();
    }
  }

  /**
   * Html attribute
   */
  public static final class Attribute {
    /** Value types */
    public static final int NO_TYPE = 0;
    public static final int URI_TYPE = 1;
    public static final int SCRIPT_TYPE = 2;
    public static final int ENUM_TYPE = 3;
    public static final int BOOLEAN_TYPE = 4;

    /** Name of the element, e.g. "HREF" */
    private final String name;

    /** Type of the attribute value, e.g. URI_TYPE */
    private final int type;

    /** The list of allowed values, or null if any value is allowed */
    private final Set<String> values;

    /**
     * Construct an Attribute
     * @throws IllegalArgumentException if name is null
     */
    public Attribute(String name, int type) {
      this(name, type, null);
    }

    /**
     * Construct an Attribute
     * @throws IllegalArgumentException if name is null
     * or if Attribute is of type ENUM_TYPE and the values are null
     */
    public Attribute(String name, int type, Set<String> values) {
      Preconditions.checkNotNull(name, "Attribute name can not be null");
      Preconditions.checkArgument((values == null) ^ (type == ENUM_TYPE),
          "Only ENUM_TYPE can have values != null");
      this.name = name;
      this.type = type;
      this.values = values;
    }

    /** Gets the name of the attribute, in lowercase */
    public String getName() {
      return name;
    }

    /** Gets the type, e.g. URI_TYPE */
    public int getType() {
      return type;
    }

    /**
     * When called on an attribute of ENUM_TYPE, returns a Set of Strings
     * containing the allowed attribute values. The return set is guaranteed to
     * only contain lower case Strings.
     *
     * @return a Set of Strings, in lower case, for the allowed attribute
     *         values.
     * @throws IllegalStateException if attribute type is not ENUM_TYPE
     */
    public Set<String> getEnumValues() {
      Preconditions.checkState(type == ENUM_TYPE);
      return values;
    }

    /**
     * @return Element name (name only, not proper HTML).
     */
    @Override
    public String toString() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof HTML.Attribute) {
        HTML.Attribute that = (HTML.Attribute) o;
        return this.name.equals(that.name);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return this.name.hashCode();
    }
  }
}