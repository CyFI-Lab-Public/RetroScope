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

package com.google.android.droiddriver.instrumentation;

import static com.google.android.droiddriver.util.TextUtils.charSequenceToString;

import android.content.res.Resources;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Checkable;
import android.widget.TextView;

import com.google.android.droiddriver.InputInjector;
import com.google.android.droiddriver.base.AbstractUiElement;
import com.google.android.droiddriver.util.Logs;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * A UiElement that is backed by a View.
 */
// TODO: always accessing view on the UI thread even when only get access is
// needed -- the field may be in the middle of updating.
public class ViewElement extends AbstractUiElement {
  private static final Map<String, String> CLASS_NAME_OVERRIDES = Maps.newHashMap();

  private final InstrumentationContext context;
  private final View view;

  /**
   * Typically users find the class name to use in tests using SDK tool
   * uiautomatorviewer. This name is returned by
   * {@link AccessibilityNodeInfo#getClassName}. If the app uses custom View
   * classes that do not call {@link AccessibilityNodeInfo#setClassName} with
   * the actual class name, different types of drivers see different class names
   * (InstrumentationDriver sees the actual class name, while UiAutomationDriver
   * sees {@link AccessibilityNodeInfo#getClassName}).
   * <p>
   * If tests fail with InstrumentationDriver, find the actual class name by
   * examining app code or by calling
   * {@link com.google.android.droiddriver.DroidDriver#dumpUiElementTree}, then
   * call this method in setUp to override it with the class name seen in
   * uiautomatorviewer.
   */
  public static void overrideClassName(String actualClassName, String overridingClassName) {
    CLASS_NAME_OVERRIDES.put(actualClassName, overridingClassName);
  }

  public ViewElement(InstrumentationContext context, View view) {
    this.context = Preconditions.checkNotNull(context);
    this.view = Preconditions.checkNotNull(view);
  }

  @Override
  public String getText() {
    if (!(view instanceof TextView)) {
      return null;
    }
    return charSequenceToString(((TextView) view).getText());
  }

  @Override
  public String getContentDescription() {
    return charSequenceToString(view.getContentDescription());
  }

  @Override
  public String getClassName() {
    String className = view.getClass().getName();
    return CLASS_NAME_OVERRIDES.containsKey(className) ? CLASS_NAME_OVERRIDES.get(className)
        : className;
  }

  @Override
  public String getResourceId() {
    if (view.getId() != View.NO_ID && view.getResources() != null) {
      try {
        return charSequenceToString(view.getResources().getResourceName(view.getId()));
      } catch (Resources.NotFoundException nfe) {
        /* ignore */
      }
    }
    return null;
  }

  @Override
  public String getPackageName() {
    return view.getContext().getPackageName();
  }

  @Override
  public InputInjector getInjector() {
    return context.getInjector();
  }

  @Override
  public boolean isVisible() {
    // isShown() checks the visibility flag of this view and ancestors; it needs
    // to have the VISIBLE flag as well as non-empty bounds to be visible.
    return view.isShown() && !getVisibleBounds().isEmpty();
  }

  @Override
  public boolean isCheckable() {
    return view instanceof Checkable;
  }

  @Override
  public boolean isChecked() {
    if (!isCheckable()) {
      return false;
    }
    return ((Checkable) view).isChecked();
  }

  @Override
  public boolean isClickable() {
    return view.isClickable();
  }

  @Override
  public boolean isEnabled() {
    return view.isEnabled();
  }

  @Override
  public boolean isFocusable() {
    return view.isFocusable();
  }

  @Override
  public boolean isFocused() {
    return view.isFocused();
  }

  @Override
  public boolean isScrollable() {
    // TODO: find a meaningful implementation
    return true;
  }

  @Override
  public boolean isLongClickable() {
    return view.isLongClickable();
  }

  @Override
  public boolean isPassword() {
    // TODO: find a meaningful implementation
    return false;
  }

  @Override
  public boolean isSelected() {
    return view.isSelected();
  }

  @Override
  public Rect getBounds() {
    Rect rect = new Rect();
    int[] xy = new int[2];
    view.getLocationOnScreen(xy);
    rect.set(xy[0], xy[1], xy[0] + view.getWidth(), xy[1] + view.getHeight());
    return rect;
  }

  @Override
  public Rect getVisibleBounds() {
    Rect visibleBounds = new Rect();
    if (!view.getGlobalVisibleRect(visibleBounds)) {
      Logs.log(Log.VERBOSE, "View is invisible: " + toString());
      visibleBounds.setEmpty();
    }
    int[] xy = new int[2];
    view.getLocationOnScreen(xy);
    // Bounds are relative to root view; adjust to screen coordinates.
    visibleBounds.offsetTo(xy[0], xy[1]);
    return visibleBounds;
  }

  @Override
  public int getChildCount() {
    if (!(view instanceof ViewGroup)) {
      return 0;
    }
    return ((ViewGroup) view).getChildCount();
  }

  @Override
  public ViewElement getChild(int index) {
    if (!(view instanceof ViewGroup)) {
      return null;
    }
    View child = ((ViewGroup) view).getChildAt(index);
    return child == null ? null : context.getUiElement(child);
  }

  @Override
  public ViewElement getParent() {
    ViewParent parent = view.getParent();
    if (!(parent instanceof View)) {
      return null;
    }
    return context.getUiElement((View) parent);
  }
}
