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

package com.google.android.droiddriver.base;

import com.google.android.droiddriver.InputInjector;
import com.google.android.droiddriver.UiElement;
import com.google.android.droiddriver.actions.Action;
import com.google.android.droiddriver.actions.ClickAction;
import com.google.android.droiddriver.actions.ScrollDirection;
import com.google.android.droiddriver.actions.SwipeAction;
import com.google.android.droiddriver.actions.TypeAction;
import com.google.android.droiddriver.exceptions.ElementNotVisibleException;
import com.google.android.droiddriver.finders.Attribute;
import com.google.android.droiddriver.finders.ByXPath;
import com.google.android.droiddriver.util.Logs;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;

import org.w3c.dom.Element;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Abstract implementation with common methods already implemented.
 */
public abstract class AbstractUiElement implements UiElement {
  private WeakReference<Element> domNode;

  @Override
  public <T> T get(Attribute attribute) {
    return attribute.getValue(this);
  }

  @Override
  public boolean perform(Action action) {
    Logs.call(this, "perform", action);
    checkVisible();
    return performAndWait(action);
  }

  protected boolean doPerform(Action action) {
    return action.perform(getInjector(), this);
  }

  protected void doPerformAndWait(FutureTask<Boolean> futureTask, long timeoutMillis) {
    // ignores timeoutMillis; subclasses can override this behavior
    futureTask.run();
  }

  private boolean performAndWait(final Action action) {
    // timeoutMillis <= 0 means no need to wait
    if (action.getTimeoutMillis() <= 0) {
      return doPerform(action);
    }

    FutureTask<Boolean> futureTask = new FutureTask<Boolean>(new Callable<Boolean>() {
      @Override
      public Boolean call() {
        return doPerform(action);
      }
    });
    doPerformAndWait(futureTask, action.getTimeoutMillis());
    try {
      return futureTask.get();
    } catch (Exception e) {
      // should not reach here b/c futureTask has run
      return false;
    }
  }

  @Override
  public void setText(String text) {
    // TODO: Define common actions as a const.
    perform(new TypeAction(text));
    // TypeAction may not be effective immediately and reflected bygetText(),
    // so the following will fail.
    // if (Logs.DEBUG) {
    // String actual = getText();
    // if (!text.equals(actual)) {
    // throw new DroidDriverException(String.format(
    // "setText failed: expected=\"%s\", actual=\"%s\"", text, actual));
    // }
    // }
  }

  @Override
  public void click() {
    perform(ClickAction.SINGLE);
  }

  @Override
  public void longClick() {
    perform(ClickAction.LONG);
  }

  @Override
  public void doubleClick() {
    perform(ClickAction.DOUBLE);
  }

  @Override
  public void scroll(ScrollDirection direction) {
    perform(new SwipeAction(direction, false));
  }

  @Override
  public abstract AbstractUiElement getChild(int index);

  protected abstract InputInjector getInjector();

  private void checkVisible() {
    if (!isVisible()) {
      throw new ElementNotVisibleException(this);
    }
  }

  @Override
  public List<UiElement> getChildren(Predicate<? super UiElement> predicate) {
    predicate = Predicates.and(Predicates.notNull(), predicate);
    // TODO: Use internal data when we take snapshot of current node tree.
    ImmutableList.Builder<UiElement> builder = ImmutableList.builder();
    for (int i = 0; i < getChildCount(); i++) {
      UiElement child = getChild(i);
      if (predicate.apply(child)) {
        builder.add(child);
      }
    }
    return builder.build();
  }

  @Override
  public String toString() {
    ToStringHelper toStringHelper = Objects.toStringHelper(this);
    for (Attribute attr : Attribute.values()) {
      addAttribute(toStringHelper, attr, get(attr));
    }
    return toStringHelper.toString();
  }

  private static void addAttribute(ToStringHelper toStringHelper, Attribute attr, Object value) {
    if (value != null) {
      if (value instanceof Boolean) {
        if ((Boolean) value) {
          toStringHelper.addValue(attr.getName());
        }
      } else {
        toStringHelper.add(attr.getName(), value);
      }
    }
  }

  /**
   * Used internally in {@link ByXPath}. Returns the DOM node representing this
   * UiElement. The DOM is constructed from the UiElement tree.
   * <p>
   * TODO: move this to {@link ByXPath}. This requires a BiMap using
   * WeakReference for both keys and values, which is error-prone. This will be
   * deferred until we decide whether to clear cache upon getRootElement.
   */
  public Element getDomNode() {
    if (domNode == null || domNode.get() == null) {
      domNode = new WeakReference<Element>(ByXPath.buildDomNode(this));
    }
    return domNode.get();
  }
}
