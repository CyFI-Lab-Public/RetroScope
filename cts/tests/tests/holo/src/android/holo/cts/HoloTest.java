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

package android.holo.cts;

import android.content.Intent;
import android.holo.cts.ThemeTestActivity.Result;
import android.test.ActivityInstrumentationTestCase2;

/** Tests for the Holo theme. */
public class HoloTest extends ActivityInstrumentationTestCase2<ThemeTestActivity> {

    /*
     * The tests take a long time, so the test is sharded into separate themes.
     */

    public HoloTest() {
        super(ThemeTestActivity.class);
    }

    public void testHolo() throws Exception {
        runThemeTest(0);
    }

    public void testHoloDialog() throws Exception {
        runThemeTest(1);
    }

    public void testHoloDialogMinimumWidth() throws Exception {
        runThemeTest(2);
    }

    public void testHoloDialogNoActionBar() throws Exception {
        runThemeTest(3);
    }

    public void testHoloDialogNoActionBarMinimumWidth() throws Exception {
        runThemeTest(4);
    }

    public void testHoloDialogWhenLarge() throws Exception {
        runThemeTest(5);
    }

    public void testHoloDialogWhenLargeNoActionBar() throws Exception {
        runThemeTest(6);
    }

    public void testHoloInputMethod() throws Exception {
        runThemeTest(7);
    }

    public void testHoloLight() throws Exception {
        runThemeTest(8);
    }

    public void testHoloLightDarkActionBar() throws Exception {
        runThemeTest(9);
    }

    public void testHoloLightDialog() throws Exception {
        runThemeTest(10);
    }

    public void testHoloLightDialogMinimumWidth() throws Exception {
        runThemeTest(11);
    }

    public void testHoloLightDialogNoActionBar() throws Exception {
        runThemeTest(12);
    }

    public void testHoloLightDialogNoActionBarMinimumWidth() throws Exception {
        runThemeTest(13);
    }

    public void testHoloLightDialogWhenLarge() throws Exception {
        runThemeTest(14);
    }

    public void testHoloLightDialogWhenLargeNoActionBar() throws Exception {
        runThemeTest(15);
    }

    public void testHoloLightNoActionBar() throws Exception {
        runThemeTest(16);
    }

    public void testHoloLightNoActionBarFullscreen() throws Exception {
        runThemeTest(17);
    }

    public void testHoloLightPanel() throws Exception {
        runThemeTest(18);
    }

    public void testHoloNoActionBar() throws Exception {
        runThemeTest(19);
    }

    public void testHoloNoActionBarFullscreen() throws Exception {
        runThemeTest(20);
    }

    public void testHoloPanel() throws Exception {
        runThemeTest(21);
    }

    public void testHoloWallpaper() throws Exception {
        runThemeTest(22);
    }

    public void testHoloWallpaperNoTitleBar() throws Exception {
        runThemeTest(23);
    }

    private void runThemeTest(int themeIndex) throws Exception {
        Intent intent = new Intent();
        intent.putExtra(ThemeTestActivity.EXTRA_TASK, ThemeTestActivity.TASK_COMPARE_BITMAPS);
        intent.putExtra(ThemeTestActivity.EXTRA_THEME_INDEX, themeIndex);
        intent.putExtra(ThemeTestActivity.EXTRA_LAYOUT_ADAPTER_MODE, LayoutAdapter.MODE_TESTING);
        setActivityIntent(intent);

        // Enable touch mode or else widgets will be highlighted causing bitmap
        // comparison failures.
        setActivityInitialTouchMode(true);

        ThemeTestActivity activity = getActivity();
        Result result = activity.getResultFuture().get();
        assertTrue("Failed bitmap names: " + result.getFailedBitmapNames()
                + " Check " + BitmapAssets.getBitmapDir(BitmapAssets.TYPE_FAILED)
                + " and " + BitmapAssets.getBitmapDir(BitmapAssets.TYPE_DIFF) + " for details."
                , result.passed());
    }
}
