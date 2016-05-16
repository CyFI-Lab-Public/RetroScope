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

package com.google.android.droiddriver.actions;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.ViewConfiguration;

import com.google.android.droiddriver.InputInjector;
import com.google.android.droiddriver.UiElement;
import com.google.android.droiddriver.util.Events;

/**
 * An action that does clicks on an UiElement.
 */
public abstract class ClickAction extends BaseAction {

  public static final ClickAction SINGLE = new SingleClick(1000L);
  public static final ClickAction LONG = new LongClick(1000L);
  public static final ClickAction DOUBLE = new DoubleClick(1000L);

  private static final long CLICK_DURATION_MILLIS = 100L;

  public static class DoubleClick extends ClickAction {
    public DoubleClick(long timeoutMillis) {
      super(timeoutMillis);
    }

    @Override
    public boolean perform(InputInjector injector, UiElement element) {
      SINGLE.perform(injector, element);
      SINGLE.perform(injector, element);
      return true;
    }
  }

  private static class LongClick extends ClickAction {
    public LongClick(long timeoutMillis) {
      super(timeoutMillis);
    }

    @Override
    public boolean perform(InputInjector injector, UiElement element) {
      Rect elementRect = element.getVisibleBounds();
      long downTime = Events.touchDown(injector, elementRect.centerX(), elementRect.centerY());
      // see android.test.TouchUtils - *1.5 to make sure it's long press
      SystemClock.sleep((long) (ViewConfiguration.getLongPressTimeout() * 1.5));
      Events.touchUp(injector, downTime, elementRect.centerX(), elementRect.centerY());
      return true;
    }
  }

  public static class SingleClick extends ClickAction {
    public SingleClick(long timeoutMillis) {
      super(timeoutMillis);
    }

    @Override
    public boolean perform(InputInjector injector, UiElement element) {
      Rect elementRect = element.getVisibleBounds();
      long downTime = Events.touchDown(injector, elementRect.centerX(), elementRect.centerY());
      // UiAutomator clickAndSync does this, while
      // android.test.TouchUtils#clickView sleep 1000
      SystemClock.sleep(CLICK_DURATION_MILLIS);
      Events.touchUp(injector, downTime, elementRect.centerX(), elementRect.centerY());
      return true;
    }
  }

  protected ClickAction(long timeoutMillis) {
    super(timeoutMillis);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
