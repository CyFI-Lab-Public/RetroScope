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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.android.droiddriver.UiElement;
import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;

/**
 * Convenience methods to create commonly used finders.
 */
public class By {
  /** Matches by {@link Object#equals}. */
  public static final MatchStrategy<Object> OBJECT_EQUALS = new MatchStrategy<Object>() {
    @Override
    public boolean match(Object expected, Object actual) {
      return Objects.equal(actual, expected);
    }

    @Override
    public String toString() {
      return "equals";
    }
  };
  /** Matches by {@link String#matches}. */
  public static final MatchStrategy<String> STRING_MATCHES = new MatchStrategy<String>() {
    @Override
    public boolean match(String expected, String actual) {
      return actual != null && actual.matches(expected);
    }

    @Override
    public String toString() {
      return "matches pattern";
    }
  };
  /** Matches by {@link String#contains}. */
  public static final MatchStrategy<String> STRING_CONTAINS = new MatchStrategy<String>() {
    @Override
    public boolean match(String expected, String actual) {
      return actual != null && actual.contains(expected);
    }

    @Override
    public String toString() {
      return "contains";
    }
  };

  /**
   * Creates a new ByAttribute finder. Frequently-used finders have shorthands
   * below, for example, {@link #text}, {@link #textRegex}. Users can create
   * custom finders using this method.
   *
   * @param attribute the attribute to match against
   * @param strategy the matching strategy, for instance, equals or matches
   *        regular expression
   * @param expected the expected attribute value
   * @return a new ByAttribute finder
   */
  public static <T> ByAttribute<T> attribute(Attribute attribute,
      MatchStrategy<? super T> strategy, T expected) {
    return new ByAttribute<T>(attribute, strategy, expected);
  }

  /** Shorthand for {@link #attribute}{@code (attribute, OBJECT_EQUALS, expected)} */
  public static <T> ByAttribute<T> attribute(Attribute attribute, T expected) {
    return attribute(attribute, OBJECT_EQUALS, expected);
  }

  /** Shorthand for {@link #attribute}{@code (attribute, true)} */
  public static ByAttribute<Boolean> is(Attribute attribute) {
    return attribute(attribute, true);
  }

  /** Shorthand for {@link #attribute}{@code (attribute, false)} */
  public static ByAttribute<Boolean> not(Attribute attribute) {
    return attribute(attribute, false);
  }

  /**
   * @param resourceId The resource id to match against
   * @return a finder to find an element by resource id
   */
  public static ByAttribute<String> resourceId(String resourceId) {
    return attribute(Attribute.RESOURCE_ID, OBJECT_EQUALS, resourceId);
  }

  /**
   * @param name The exact package name to match against
   * @return a finder to find an element by package name
   */
  public static ByAttribute<String> packageName(String name) {
    return attribute(Attribute.PACKAGE, OBJECT_EQUALS, name);
  }

  /**
   * @param text The exact text to match against
   * @return a finder to find an element by text
   */
  public static ByAttribute<String> text(String text) {
    return attribute(Attribute.TEXT, OBJECT_EQUALS, text);
  }

  /**
   * @param regex The regular expression pattern to match against
   * @return a finder to find an element by text pattern
   */
  public static ByAttribute<String> textRegex(String regex) {
    return attribute(Attribute.TEXT, STRING_MATCHES, regex);
  }

  /**
   * @param substring String inside a text field
   * @return a finder to find an element by text substring
   */
  public static ByAttribute<String> textContains(String substring) {
    return attribute(Attribute.TEXT, STRING_CONTAINS, substring);
  }

  /**
   * @param contentDescription The exact content description to match against
   * @return a finder to find an element by content description
   */
  public static ByAttribute<String> contentDescription(String contentDescription) {
    return attribute(Attribute.CONTENT_DESC, OBJECT_EQUALS, contentDescription);
  }

  /**
   * @param substring String inside a content description
   * @return a finder to find an element by content description substring
   */
  public static ByAttribute<String> contentDescriptionContains(String substring) {
    return attribute(Attribute.CONTENT_DESC, STRING_CONTAINS, substring);
  }

  /**
   * @param className The exact class name to match against
   * @return a finder to find an element by class name
   */
  public static ByAttribute<String> className(String className) {
    return attribute(Attribute.CLASS, OBJECT_EQUALS, className);
  }

  /**
   * @param clazz The class whose name is matched against
   * @return a finder to find an element by class name
   */
  public static ByAttribute<String> className(Class<?> clazz) {
    return className(clazz.getName());
  }

  /**
   * @return a finder to find an element that is selected
   */
  public static ByAttribute<Boolean> selected() {
    return is(Attribute.SELECTED);
  }

  /**
   * Matches by XPath. When applied on an non-root element, it will not evaluate
   * above the context element.
   * <p>
   * XPath is the domain-specific-language for navigating a node tree. It is
   * ideal if the UiElement to match has a complex relationship with surrounding
   * nodes. For simple cases, {@link #withParent} or {@link #withAncestor} are
   * preferred, which can combine with other {@link MatchFinder}s in
   * {@link #allOf}. For complex cases like below, XPath is superior:
   *
   * <pre>
   * {@code
   * <View><!-- a custom view to group a cluster of items -->
   *   <LinearLayout>
   *     <TextView text='Albums'/>
   *     <TextView text='4 MORE'/>
   *   </LinearLayout>
   *   <RelativeLayout>
   *     <TextView text='Forever'/>
   *     <ImageView/>
   *   </RelativeLayout>
   * </View><!-- end of Albums cluster -->
   * <!-- imagine there are other clusters for Artists and Songs -->
   * }
   * </pre>
   *
   * If we need to locate the RelativeLayout containing the album "Forever"
   * instead of a song or an artist named "Forever", this XPath works:
   *
   * <pre>
   * {@code //*[LinearLayout/*[@text='Albums']]/RelativeLayout[*[@text='Forever']]}
   * </pre>
   *
   * @param xPath The xpath to use
   * @return a finder which locates elements via XPath
   */
  @Beta
  public static ByXPath xpath(String xPath) {
    return new ByXPath(xPath);
  }

  /**
   * @return a finder that uses the UiElement returned by parent Finder as
   *         context for the child Finder
   */
  public static ChainFinder chain(Finder parent, Finder child) {
    return new ChainFinder(parent, child);
  }

  // Hamcrest style finder aggregators
  /**
   * Evaluates given {@finders} in short-circuit fashion in the order
   * they are passed. Costly finders (for example those returned by with*
   * methods that navigate the node tree) should be passed after cheap finders
   * (for example the ByAttribute finders).
   *
   * @return a finder that is the logical conjunction of given finders
   */
  public static MatchFinder allOf(final MatchFinder... finders) {
    return new MatchFinder() {
      @Override
      public boolean matches(UiElement element) {
        for (MatchFinder finder : finders) {
          if (!finder.matches(element)) {
            return false;
          }
        }
        return true;
      }

      @Override
      public String toString() {
        return "allOf(" + Joiner.on(",").join(finders) + ")";
      }
    };
  }

  /**
   * Evaluates given {@finders} in short-circuit fashion in the order
   * they are passed. Costly finders (for example those returned by with*
   * methods that navigate the node tree) should be passed after cheap finders
   * (for example the ByAttribute finders).
   *
   * @return a finder that is the logical disjunction of given finders
   */
  public static MatchFinder anyOf(final MatchFinder... finders) {
    return new MatchFinder() {
      @Override
      public boolean matches(UiElement element) {
        for (MatchFinder finder : finders) {
          if (finder.matches(element)) {
            return true;
          }
        }
        return false;
      }

      @Override
      public String toString() {
        return "anyOf(" + Joiner.on(",").join(finders) + ")";
      }
    };
  }

  /**
   * Matches a UiElement whose parent matches the given parentFinder. For
   * complex cases, consider {@link #xpath}.
   */
  public static MatchFinder withParent(final MatchFinder parentFinder) {
    checkNotNull(parentFinder);
    return new MatchFinder() {
      @Override
      public boolean matches(UiElement element) {
        UiElement parent = element.getParent();
        return parent != null && parentFinder.matches(parent);
      }

      @Override
      public String toString() {
        return "withParent(" + parentFinder + ")";
      }
    };
  }

  /**
   * Matches a UiElement whose ancestor matches the given ancestorFinder. For
   * complex cases, consider {@link #xpath}.
   */
  public static MatchFinder withAncestor(final MatchFinder ancestorFinder) {
    checkNotNull(ancestorFinder);
    return new MatchFinder() {
      @Override
      public boolean matches(UiElement element) {
        UiElement parent = element.getParent();
        while (parent != null) {
          if (ancestorFinder.matches(parent)) {
            return true;
          }
          parent = parent.getParent();
        }
        return false;
      }

      @Override
      public String toString() {
        return "withAncestor(" + ancestorFinder + ")";
      }
    };
  }

  /**
   * Matches a UiElement which has a sibling matching the given siblingFinder.
   * For complex cases, consider {@link #xpath}.
   */
  public static MatchFinder withSibling(final MatchFinder siblingFinder) {
    checkNotNull(siblingFinder);
    return new MatchFinder() {
      @Override
      public boolean matches(UiElement element) {
        UiElement parent = element.getParent();
        if (parent == null) {
          return false;
        }
        for (int i = 0; i < parent.getChildCount(); i++) {
          if (siblingFinder.matches(parent.getChild(i))) {
            return true;
          }
        }
        return false;
      }

      @Override
      public String toString() {
        return "withSibling(" + siblingFinder + ")";
      }
    };
  }

  private By() {}
}
