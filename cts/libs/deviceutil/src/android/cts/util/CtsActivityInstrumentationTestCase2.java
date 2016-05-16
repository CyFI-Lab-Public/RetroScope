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


package android.cts.util;

import com.android.cts.util.ReportLog;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;


public class CtsActivityInstrumentationTestCase2<T extends Activity> extends
        ActivityInstrumentationTestCase2<T> {

    private DeviceReportLog mReportLog = new DeviceReportLog();

    public CtsActivityInstrumentationTestCase2(Class<T> activityClass) {
        super(activityClass);
    }

    public ReportLog getReportLog() {
        return mReportLog;
    }

    @Override
    protected void tearDown() throws Exception {
        mReportLog.deliverReportToHost(getInstrumentation());
        super.tearDown();
    }

}
