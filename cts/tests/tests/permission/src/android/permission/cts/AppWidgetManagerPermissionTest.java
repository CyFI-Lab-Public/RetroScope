/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.permission.cts;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
* Test that protected AppWidgetManager APIs cannot be called without permissions
*/
public class AppWidgetManagerPermissionTest extends AndroidTestCase {

    private AppWidgetManager mAppWidgetManager = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mAppWidgetManager = AppWidgetManager.getInstance(getContext());
        assertNotNull(mAppWidgetManager);
    }

    /**
     * Verify that calling
     * {@link AppWidgetManager#bindAppWidgetId(int, android.content.ComponentName)}
     * requires permissions.
     * <p>Tests Permission:
     *   {@link android.Manifest.permission#BIND_APP_WIDGET}.
     */
    @SmallTest
    public void testBindAppWidget() {
        try {
            mAppWidgetManager.bindAppWidgetId(1, new ComponentName(mContext, "foo"));
            fail("Was able to call bindAppWidgetId");
        } catch (SecurityException e) {
            // expected
        }
    }
}

