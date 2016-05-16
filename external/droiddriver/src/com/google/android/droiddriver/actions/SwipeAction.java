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
import com.google.android.droiddriver.exceptions.ActionException;
import com.google.android.droiddriver.util.Events;

/**
 * A {@link ScrollAction} that swipes the touch screen.
 */
public class SwipeAction extends ScrollAction {

  private final ScrollDirection direction;
  private final boolean drag;

  /**
   * Defaults timeoutMillis to 0.
   */
  public SwipeAction(ScrollDirection direction, boolean drag) {
    this(direction, drag, 0L);
  }

  public SwipeAction(ScrollDirection direction, boolean drag, long timeoutMillis) {
    super(timeoutMillis);
    this.direction = direction;
    this.drag = drag;
  }

  @Override
  public boolean perform(InputInjector injector, UiElement element) {
    Rect elementRect = element.getVisibleBounds();

    int swipeAreaHeightAdjust = (int) (elementRect.height() * 0.1);
    int swipeAreaWidthAdjust = (int) (elementRect.width() * 0.1);
    int steps = 50;
    int startX;
    int startY;
    int endX;
    int endY;

    switch (direction) {
      case DOWN:
        startX = elementRect.centerX();
        startY = elementRect.bottom - swipeAreaHeightAdjust;
        endX = elementRect.centerX();
        endY = elementRect.top + swipeAreaHeightAdjust;
        break;
      case UP:
        startX = elementRect.centerX();
        startY = elementRect.top + swipeAreaHeightAdjust;
        endX = elementRect.centerX();
        endY = elementRect.bottom - swipeAreaHeightAdjust;
        break;
      case LEFT:
        startX = elementRect.left + swipeAreaWidthAdjust;
        startY = elementRect.centerY();
        endX = elementRect.right - swipeAreaWidthAdjust;
        endY = elementRect.centerY();
        break;
      case RIGHT:
        startX = elementRect.right - swipeAreaWidthAdjust;
        startY = elementRect.centerY();
        endX = elementRect.left + swipeAreaHeightAdjust;
        endY = elementRect.centerY();
        break;
      default:
        throw new ActionException("Unknown scroll direction: " + direction);
    }

    double xStep = ((double) (endX - startX)) / steps;
    double yStep = ((double) (endY - startY)) / steps;

    // First touch starts exactly at the point requested
    long downTime = Events.touchDown(injector, startX, startY);
    if (drag) {
      SystemClock.sleep((long) (ViewConfiguration.getLongPressTimeout() * 1.5f));
    }
    for (int i = 1; i < steps; i++) {
      Events.touchMove(injector, downTime, startX + (int) (xStep * i), startY + (int) (yStep * i));
      SystemClock.sleep(5);
    }
    if (drag) {
      // Hold final position for a little bit to simulate drag.
      SystemClock.sleep(100);
    }
    Events.touchUp(injector, downTime, endX, endY);
    return true;
  }
}
