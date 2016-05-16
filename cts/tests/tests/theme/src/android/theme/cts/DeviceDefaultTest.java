/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.theme.cts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.test.ActivityInstrumentationTestCase2;

public class DeviceDefaultTest extends ActivityInstrumentationTestCase2<DeviceDefaultActivity> {

    public DeviceDefaultTest() {
        super(DeviceDefaultActivity.class);
    }

    public void testGetActionBar_DeviceDefault() {
        assertActionBar(android.R.style.Theme_DeviceDefault);
    }

    public void testGetActionBar_DeviceDefault_Dialog() {
        assertNoActionBar(android.R.style.Theme_DeviceDefault_Dialog);
    }

    public void testGetActionBar_DeviceDefault_MinWidth() {
        assertNoActionBar(android.R.style.Theme_DeviceDefault_Dialog_MinWidth);
    }

    public void testGetActionBar_DeviceDefault_Dialog_NoActionBar() {
        assertNoActionBar(android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
    }

    public void testGetActionBar_DeviceDefault_Dialog_NoActionBar_MinWidth() {
        assertNoActionBar(android.R.style.Theme_DeviceDefault_Dialog_NoActionBar_MinWidth);
    }

    public void testGetActionBar_DeviceDefault_DialogWhenLarge() {
        assertActionBarWhenLarge(android.R.style.Theme_DeviceDefault_DialogWhenLarge);
    }

    public void testGetActionBar_DeviceDefault_DialogWhenLarge_NoActionBar() {
        assertNoActionBar(android.R.style.Theme_DeviceDefault_DialogWhenLarge_NoActionBar);
    }

    public void testGetActionBar_DeviceDefault_InputMethod() {
        assertNoActionBar(android.R.style.Theme_DeviceDefault_InputMethod);
    }

    public void testGetActionBar_DeviceDefault_Light() {
        assertActionBar(android.R.style.Theme_DeviceDefault_Light);
    }

    public void testGetActionBar_DeviceDefault_Light_DarkActionBar() {
        assertActionBar(android.R.style.Theme_DeviceDefault_Light_DarkActionBar);
    }

    public void testGetActionBar_DeviceDefault_Light_Dialog() {
        assertNoActionBar(android.R.style.Theme_DeviceDefault_Light_Dialog);
    }

    public void testGetActionBar_DeviceDefault_Light_Dialog_MinWidth() {
        assertNoActionBar(android.R.style.Theme_DeviceDefault_Light_Dialog_MinWidth);
    }

    public void testGetActionBar_DeviceDefault_Light_Dialog_NoActionBar() {
        assertNoActionBar(android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
    }

    public void testGetActionBar_DeviceDefault_Light_Dialog_NoActionBar_MinWidth() {
        assertNoActionBar(android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth);
    }

    public void testGetActionBar_DeviceDefault_Light_DialogWhenLarge() {
        assertActionBarWhenLarge(android.R.style.Theme_Holo_Light_DialogWhenLarge);
    }

    public void testGetActionBar_DeviceDefault_Light_DialogWhenLarge_NoActionBar() {
        assertNoActionBar(android.R.style.Theme_DeviceDefault_Light_DialogWhenLarge_NoActionBar);
    }

    public void testGetActionBar_DeviceDefault_Light_NoActionBar() {
        assertNoActionBar(android.R.style.Theme_DeviceDefault_Light_NoActionBar);
    }

    public void testGetActionBar_DeviceDefault_Light_NoActionBar_Fullscreen() {
        assertNoActionBar(android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
    }

    public void testGetActionBar_DeviceDefault_Panel() {
        assertNoActionBar(android.R.style.Theme_DeviceDefault_Panel);
    }

    public void testGetActionBar_DeviceDefault_Wallpaper() {
        assertActionBar(android.R.style.Theme_DeviceDefault_Wallpaper);
    }

    public void testGetActionBar_DeviceDefault_Wallpaper_NoTitleBar() {
        assertNoActionBar(android.R.style.Theme_DeviceDefault_Wallpaper_NoTitleBar);
    }

    private void assertActionBar(int themeId) {
        assertGetActionBar(themeId, true);
    }

    private void assertNoActionBar(int themeId) {
        assertGetActionBar(themeId, false);
    }

    private void assertGetActionBar(int themeId, boolean actionBar) {
        Activity activity = startActivity(themeId);
        if (actionBar) {
            assertNotNull(activity.getActionBar());
        } else {
            assertNull(activity.getActionBar());
        }
    }

    private void assertActionBarWhenLarge(int themeId) {
        Activity activity = startActivity(themeId);
        if (isLargeScreen(activity)) {
            // Large screens will have dialogs which don't have action bars...
            assertNoActionBar(themeId);
        } else {
            assertActionBar(themeId);
        }
    }

    private boolean isLargeScreen(Context context) {
        Configuration config = context.getResources().getConfiguration();
        int size = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        return config.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE);
    }

    private Activity startActivity(int themeId) {
        setActivityIntent(new Intent().putExtra(DeviceDefaultActivity.EXTRA_THEME_ID, themeId));
        return getActivity();
    }
}
