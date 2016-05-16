/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.app.cts;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.WindowManager.LayoutParams;

public class LaunchTest extends ActivityTestsBase {

    public void testClearTopWhilResumed() {
        mIntent.putExtra("component", new ComponentName(getContext(), ClearTop.class));
        mIntent.putExtra(ClearTop.WAIT_CLEAR_TASK, true);
        runLaunchpad(LaunchpadActivity.LAUNCH);
    }

    public void testClearTopInCreate() throws Exception {
        mIntent.putExtra("component", new ComponentName(getContext(), ClearTop.class));
        runLaunchpad(LaunchpadActivity.LAUNCH);
    }

    public void testForwardResult() {
        runLaunchpad(LaunchpadActivity.FORWARD_RESULT);
    }

    public void testLocalScreen() {
        mIntent.putExtra("component", new ComponentName(getContext(), LocalScreen.class));
        runLaunchpad(LaunchpadActivity.LAUNCH);
    }

    public void testColdScreen() {
        mIntent.putExtra("component", new ComponentName(getContext(), TestedScreen.class));
        runLaunchpad(LaunchpadActivity.LAUNCH);
    }

    public void testLocalActivity() {
        mIntent.putExtra("component", new ComponentName(getContext(), LocalActivity.class));
        runLaunchpad(LaunchpadActivity.LAUNCH);
    }

    public void testColdActivity() {
        mIntent.putExtra("component", new ComponentName(getContext(), TestedActivity.class));
        runLaunchpad(LaunchpadActivity.LAUNCH);
    }
}
