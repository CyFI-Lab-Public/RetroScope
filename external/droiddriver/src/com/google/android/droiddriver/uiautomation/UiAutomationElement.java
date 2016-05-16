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

package com.google.android.droiddriver.uiautomation;

import static com.google.android.droiddriver.util.TextUtils.charSequenceToString;

import android.app.UiAutomation;
import android.app.UiAutomation.AccessibilityEventFilter;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.android.droiddriver.InputInjector;
import com.google.android.droiddriver.base.AbstractUiElement;
import com.google.android.droiddriver.util.Logs;
import com.google.common.base.Preconditions;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;

/**
 * A UiElement that is backed by the UiAutomation object.
 */
public class UiAutomationElement extends AbstractUiElement {
  private static final AccessibilityEventFilter ANY_EVENT_FILTER = new AccessibilityEventFilter() {
    @Override
    public boolean accept(AccessibilityEvent arg0) {
      return true;
    }
  };

  private final UiAutomationContext context;
  private final AccessibilityNodeInfo node;
  private final UiAutomation uiAutomation;

  public UiAutomationElement(UiAutomationContext context, AccessibilityNodeInfo node) {
    this.context = Preconditions.checkNotNull(context);
    this.node = Preconditions.checkNotNull(node);
    this.uiAutomation = context.getUiAutomation();
  }

  @Override
  public String getText() {
    return charSequenceToString(node.getText());
  }

  @Override
  public String getContentDescription() {
    return charSequenceToString(node.getContentDescription());
  }

  @Override
  public String getClassName() {
    return charSequenceToString(node.getClassName());
  }

  @Override
  public String getResourceId() {
    return charSequenceToString(node.getViewIdResourceName());
  }

  @Override
  public String getPackageName() {
    return charSequenceToString(node.getPackageName());
  }

  @Override
  public InputInjector getInjector() {
    return context.getInjector();
  }

  @Override
  public boolean isVisible() {
    return node.isVisibleToUser();
  }

  @Override
  public boolean isCheckable() {
    return node.isCheckable();
  }

  @Override
  public boolean isChecked() {
    return node.isChecked();
  }

  @Override
  public boolean isClickable() {
    return node.isClickable();
  }

  @Override
  public boolean isEnabled() {
    return node.isEnabled();
  }

  @Override
  public boolean isFocusable() {
    return node.isFocusable();
  }

  @Override
  public boolean isFocused() {
    return node.isFocused();
  }

  @Override
  public boolean isScrollable() {
    return node.isScrollable();
  }

  @Override
  public boolean isLongClickable() {
    return node.isLongClickable();
  }

  @Override
  public boolean isPassword() {
    return node.isPassword();
  }

  @Override
  public boolean isSelected() {
    return node.isSelected();
  }

  @Override
  public Rect getBounds() {
    Rect rect = new Rect();
    node.getBoundsInScreen(rect);
    return rect;
  }

  @Override
  public Rect getVisibleBounds() {
    if (!isVisible()) {
      Logs.log(Log.DEBUG, "Node is invisible: " + node);
      return new Rect();
    }
    Rect visibleBounds = getBounds();
    UiAutomationElement parent = getParent();
    Rect parentBounds;
    while (parent != null) {
      parentBounds = parent.getBounds();
      visibleBounds.intersect(parentBounds);
      parent = parent.getParent();
    }
    return visibleBounds;
  }

  @Override
  public int getChildCount() {
    return node.getChildCount();
  }

  @Override
  public UiAutomationElement getChild(int index) {
    AccessibilityNodeInfo child = node.getChild(index);
    return child == null ? null : context.getUiElement(child);
  }

  @Override
  public UiAutomationElement getParent() {
    AccessibilityNodeInfo parent = node.getParent();
    return parent == null ? null : context.getUiElement(parent);
  }

  @Override
  protected void doPerformAndWait(FutureTask<Boolean> futureTask, long timeoutMillis) {
    try {
      uiAutomation.executeAndWaitForEvent(futureTask, ANY_EVENT_FILTER, timeoutMillis);
    } catch (TimeoutException e) {
      // This is for sync'ing with Accessibility API on best-effort because
      // it is not reliable.
      // Exception is ignored here. Tests will fail anyways if this is
      // critical.
    }
  }
}
