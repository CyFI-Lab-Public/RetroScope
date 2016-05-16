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

import android.graphics.Rect;

import com.google.android.droiddriver.UiElement;

public enum Attribute {
  CHECKABLE("checkable") {
    @SuppressWarnings("unchecked")
    @Override
    public Boolean getValue(UiElement element) {
      return element.isCheckable();
    }
  },
  CHECKED("checked") {
    @SuppressWarnings("unchecked")
    @Override
    public Boolean getValue(UiElement element) {
      return element.isChecked();
    }
  },
  CLASS("class") {
    @SuppressWarnings("unchecked")
    @Override
    public String getValue(UiElement element) {
      return element.getClassName();
    }
  },
  CLICKABLE("clickable") {
    @SuppressWarnings("unchecked")
    @Override
    public Boolean getValue(UiElement element) {
      return element.isClickable();
    }
  },
  CONTENT_DESC("content-desc") {
    @SuppressWarnings("unchecked")
    @Override
    public String getValue(UiElement element) {
      return element.getContentDescription();
    }
  },
  ENABLED("enabled") {
    @SuppressWarnings("unchecked")
    @Override
    public Boolean getValue(UiElement element) {
      return element.isEnabled();
    }
  },
  FOCUSABLE("focusable") {
    @SuppressWarnings("unchecked")
    @Override
    public Boolean getValue(UiElement element) {
      return element.isFocusable();
    }
  },
  FOCUSED("focused") {
    @SuppressWarnings("unchecked")
    @Override
    public Boolean getValue(UiElement element) {
      return element.isFocused();
    }
  },
  LONG_CLICKABLE("long-clickable") {
    @SuppressWarnings("unchecked")
    @Override
    public Boolean getValue(UiElement element) {
      return element.isLongClickable();
    }
  },
  PACKAGE("package") {
    @SuppressWarnings("unchecked")
    @Override
    public String getValue(UiElement element) {
      return element.getPackageName();
    }
  },
  PASSWORD("password") {
    @SuppressWarnings("unchecked")
    @Override
    public Boolean getValue(UiElement element) {
      return element.isPassword();
    }
  },
  RESOURCE_ID("resource-id") {
    @SuppressWarnings("unchecked")
    @Override
    public String getValue(UiElement element) {
      return element.getResourceId();
    }
  },
  SCROLLABLE("scrollable") {
    @SuppressWarnings("unchecked")
    @Override
    public Boolean getValue(UiElement element) {
      return element.isScrollable();
    }
  },
  SELECTED("selected") {
    @SuppressWarnings("unchecked")
    @Override
    public Boolean getValue(UiElement element) {
      return element.isSelected();
    }
  },
  TEXT("text") {
    @SuppressWarnings("unchecked")
    @Override
    public String getValue(UiElement element) {
      return element.getText();
    }
  },
  BOUNDS("bounds") {
    @SuppressWarnings("unchecked")
    @Override
    public Rect getValue(UiElement element) {
      // TODO: clip by boundsInParent?
      return element.getBounds();
    }
  };

  private final String name;

  private Attribute(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public abstract <T> T getValue(UiElement element);

  @Override
  public String toString() {
    return name;
  }
}
