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

import android.app.Activity;
import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.View;

import com.google.android.droiddriver.base.AbstractDroidDriver;
import com.google.android.droiddriver.exceptions.TimeoutException;
import com.google.android.droiddriver.util.ActivityUtils;
import com.google.common.primitives.Longs;

/**
 * Implementation of a UiDriver that is driven via instrumentation.
 */
public class InstrumentationDriver extends AbstractDroidDriver {
  private final InstrumentationContext context;

  public InstrumentationDriver(Instrumentation instrumentation) {
    super(instrumentation);
    this.context = new InstrumentationContext(instrumentation);
  }

  @Override
  protected ViewElement getNewRootElement() {
    return context.getUiElement(findRootView());
  }

  @Override
  protected InstrumentationContext getContext() {
    return context;
  }

  private View findRootView() {
    Activity runningActivity = getRunningActivity();
    View[] views = RootFinder.getRootViews();
    if (views.length > 1) {
      for (View view : views) {
        if (view.hasWindowFocus()) {
          return view;
        }
      }
    }
    return runningActivity.getWindow().getDecorView();
  }

  private Activity getRunningActivity() {
    long timeoutMillis = getPoller().getTimeoutMillis();
    long end = SystemClock.uptimeMillis() + timeoutMillis;
    while (true) {
      instrumentation.waitForIdleSync();
      Activity runningActivity = ActivityUtils.getRunningActivity();
      if (runningActivity != null) {
        return runningActivity;
      }
      long remainingMillis = end - SystemClock.uptimeMillis();
      if (remainingMillis < 0) {
        throw new TimeoutException(String.format(
            "Timed out after %d milliseconds waiting for foreground activity", timeoutMillis));
      }
      SystemClock.sleep(Longs.min(250, remainingMillis));
    }
  }

  private static class ScreenshotRunnable implements Runnable {
    private final View rootView;
    private Bitmap screenshot;

    private ScreenshotRunnable(View rootView) {
      this.rootView = rootView;
    }

    @Override
    public void run() {
      rootView.destroyDrawingCache();
      rootView.buildDrawingCache(false);
      screenshot = Bitmap.createBitmap(rootView.getDrawingCache());
      rootView.destroyDrawingCache();
    }
  }

  @Override
  protected Bitmap takeScreenshot() {
    ScreenshotRunnable screenshotRunnable = new ScreenshotRunnable(findRootView());
    instrumentation.runOnMainSync(screenshotRunnable);
    return screenshotRunnable.screenshot;
  }
}
