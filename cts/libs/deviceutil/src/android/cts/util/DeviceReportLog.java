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

import android.app.Instrumentation;
import android.os.Bundle;
import android.util.Log;

public class DeviceReportLog extends ReportLog {
    private static final String TAG = "DeviceCtsReport";
    private static final String CTS_RESULT = "CTS_RESULT";
    private static final int INST_STATUS_IN_PROGRESS = 2;

    DeviceReportLog() {
        mDepth = 4;
    }

    @Override
    protected void printLog(String msg) {
        Log.i(TAG, msg);
    }

    public void deliverReportToHost(Instrumentation instrumentation) {
        Log.i(TAG, "deliverReportToHost");
        String report = generateReport();
        if (!report.equals("")) {
            Bundle output = new Bundle();
            output.putString(CTS_RESULT, report);
            instrumentation.sendStatus(INST_STATUS_IN_PROGRESS, output);
        }
    }
}
