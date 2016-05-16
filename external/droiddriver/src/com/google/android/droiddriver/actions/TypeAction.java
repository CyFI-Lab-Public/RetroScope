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

import com.google.android.droiddriver.InputInjector;
import com.google.android.droiddriver.UiElement;
import com.google.android.droiddriver.exceptions.ActionException;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import android.os.SystemClock;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

/**
 * An action to type text.
 */
public class TypeAction extends KeyAction {

  private static final KeyCharacterMap KEY_CHAR_MAP = KeyCharacterMap
      .load(KeyCharacterMap.VIRTUAL_KEYBOARD);

  private final String text;

  /**
   * Defaults timeoutMillis to 0.
   */
  public TypeAction(String text) {
    this(text, 0L);
  }

  public TypeAction(String text, long timeoutMillis) {
    super(timeoutMillis);
    this.text = Preconditions.checkNotNull(text);
  }

  @Override
  public boolean perform(InputInjector injector, UiElement element) {
    // TODO: recycle events?
    KeyEvent[] events = KEY_CHAR_MAP.getEvents(text.toCharArray());
    boolean success = false;

    if (events != null) {
      for (KeyEvent event : events) {
        // We have to change the time of an event before injecting it because
        // all KeyEvents returned by KeyCharacterMap.getEvents() have the same
        // time stamp and the system rejects too old events. Hence, it is
        // possible for an event to become stale before it is injected if it
        // takes too long to inject the preceding ones.
        KeyEvent modifiedEvent = KeyEvent.changeTimeRepeat(event, SystemClock.uptimeMillis(), 0);
        success = injector.injectInputEvent(modifiedEvent);
        if (!success) {
          break;
        }
      }
    } else {
      throw new ActionException("The given text is not supported: " + text);
    }
    return success;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).addValue(text).toString();
  }
}
