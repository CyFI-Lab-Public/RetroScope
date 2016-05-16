/*
 * Copyright (C) 2009 The Android Open Source Project
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
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.app.Instrumentation.ActivityResult;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.test.InstrumentationTestCase;

public class Instrumentation_ActivityMonitorTest extends InstrumentationTestCase {

    private static final long WAIT_TIMEOUT = 100;

    /**
     * check points:
     * 1 Constructor with blocking true and false
     * 2 waitForActivity with timeout and no timeout
     * 3 get info about ActivityMonitor
     */
    public void testActivityMonitor() throws Exception {
        ActivityResult result = new ActivityResult(Activity.RESULT_OK, new Intent());
        Instrumentation instrumentation = getInstrumentation();
        ActivityMonitor am = instrumentation.addMonitor(
                InstrumentationTestActivity.class.getName(), result, false);
        Context context = instrumentation.getTargetContext();
        Intent intent = new Intent(context, InstrumentationTestActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        Activity lastActivity = am.getLastActivity();
        final long TIMEOUT_MSEC = 5000;
        long timeout = System.currentTimeMillis() + TIMEOUT_MSEC;
        while (lastActivity == null && System.currentTimeMillis() < timeout) {
            Thread.sleep(WAIT_TIMEOUT);
            lastActivity = am.getLastActivity();
        }
        Activity activity = am.waitForActivity();
        assertSame(activity, lastActivity);
        assertEquals(1, am.getHits());
        assertTrue(activity instanceof InstrumentationTestActivity);
        activity.finish();
        instrumentation.waitForIdleSync();
        context.startActivity(intent);
        timeout = System.currentTimeMillis() + TIMEOUT_MSEC;
        activity = null;
        while (activity == null && System.currentTimeMillis() < timeout) {
            Thread.sleep(WAIT_TIMEOUT);
            activity = am.waitForActivityWithTimeout(WAIT_TIMEOUT);
        }
        assertNotNull(activity);
        activity.finish();
        instrumentation.removeMonitor(am);

        am = new ActivityMonitor(InstrumentationTestActivity.class.getName(), result, true);
        assertSame(result, am.getResult());
        assertTrue(am.isBlocking());
        IntentFilter which = new IntentFilter();
        am = new ActivityMonitor(which, result, false);
        assertSame(which, am.getFilter());
        assertFalse(am.isBlocking());
    }
}
