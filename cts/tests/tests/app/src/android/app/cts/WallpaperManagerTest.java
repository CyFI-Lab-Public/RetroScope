/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Point;
import android.test.AndroidTestCase;
import android.view.Display;
import android.view.WindowManager;

public class WallpaperManagerTest extends AndroidTestCase {

    private WallpaperManager mWallpaperManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mWallpaperManager = WallpaperManager.getInstance(mContext);
    }

    public void testSuggestDesiredDimensions() {
        Point min = getScreenSize();
        int w = min.x * 3;
        int h = min.y * 2;

        mWallpaperManager.suggestDesiredDimensions(min.x / 2, min.y / 2);
        assertEquals(min.x, mWallpaperManager.getDesiredMinimumWidth());
        assertEquals(min.y, mWallpaperManager.getDesiredMinimumHeight());

        mWallpaperManager.suggestDesiredDimensions(w, h);
        assertEquals(w, mWallpaperManager.getDesiredMinimumWidth());
        assertEquals(h, mWallpaperManager.getDesiredMinimumHeight());

        mWallpaperManager.suggestDesiredDimensions(min.x / 2, h);
        assertEquals(min.x, mWallpaperManager.getDesiredMinimumWidth());
        assertEquals(h, mWallpaperManager.getDesiredMinimumHeight());

        mWallpaperManager.suggestDesiredDimensions(w, min.y / 2);
        assertEquals(w, mWallpaperManager.getDesiredMinimumWidth());
        assertEquals(min.y, mWallpaperManager.getDesiredMinimumHeight());
    }

    private Point getScreenSize() {
        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        Display d = wm.getDefaultDisplay();
        Point p = new Point();
        d.getRealSize(p);
        return p;
    }
}
