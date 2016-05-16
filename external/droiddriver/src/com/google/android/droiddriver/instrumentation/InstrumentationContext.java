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

import android.app.Instrumentation;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.google.android.droiddriver.InputInjector;
import com.google.android.droiddriver.base.AbstractContext;
import com.google.android.droiddriver.exceptions.ActionException;
import com.google.common.collect.MapMaker;

import java.util.Map;

/**
 * Internal helper for managing all instances.
 */
public class InstrumentationContext extends AbstractContext {
  private final Map<View, ViewElement> map = new MapMaker().weakKeys().weakValues().makeMap();

  InstrumentationContext(final Instrumentation instrumentation) {
    super(new InputInjector() {
      @Override
      public boolean injectInputEvent(InputEvent event) {
        if (event instanceof MotionEvent) {
          instrumentation.sendPointerSync((MotionEvent) event);
        } else if (event instanceof KeyEvent) {
          instrumentation.sendKeySync((KeyEvent) event);
        } else {
          throw new ActionException("Unknown input event type: " + event);
        }
        return true;
      }
    });
  }

  public ViewElement getUiElement(View view) {
    ViewElement element = map.get(view);
    if (element == null) {
      element = new ViewElement(this, view);
      map.put(view, element);
    }
    return element;
  }

  @Override
  public void clearData() {
    map.clear();
  }
}
