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

package com.android.cts.process;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.test.AndroidTestCase;

import com.android.cts.process.activity.NoSharePidActivity;
import com.android.cts.process.activity.SharePidActivity;
import com.android.cts.process.activity.SharePidSubActivity;

public class ProcessTest extends AndroidTestCase {
    private final int WAIT_TIME = 2000;

    public void testUid() throws Exception {
        String enableApp = "com.android.cts.process.shareuidapp";
        String disableApp = "com.android.cts.process.noshareuidapp";
        String testApp = mContext.getPackageName();
        int uid1 = mContext.getPackageManager().getApplicationInfo(enableApp,
                PackageManager.GET_META_DATA).uid;
        int uid2 = mContext.getPackageManager().getApplicationInfo(disableApp,
                PackageManager.GET_META_DATA).uid;
        int uid3 = mContext.getPackageManager().getApplicationInfo(testApp,
                PackageManager.GET_META_DATA).uid;
        assertEquals(uid1, uid3);
        assertNotSame(uid2, uid3);
    }

    public void testPid() throws Exception {
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        String shareProcessName = mContext.getPackageName() + ":shareProcess";
        String noShareProcessName = mContext.getPackageName() + ":noShareProcess";
        List<RunningAppProcessInfo> list = am.getRunningAppProcesses();
        assertEquals(-1, getPid(shareProcessName, list));
        // share pid will use same process
        Intent sharePidIntent = new Intent();
        sharePidIntent.setClass(mContext, SharePidActivity.class);
        sharePidIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(sharePidIntent);
        Thread.sleep(WAIT_TIME);
        List<RunningAppProcessInfo> sharelist = am.getRunningAppProcesses();
        int sharePid = getPid(shareProcessName, sharelist);
        assertTrue(-1 != sharePid);
        Intent sharePidStubIntent = new Intent();
        sharePidStubIntent.setClass(mContext, SharePidSubActivity.class);
        sharePidStubIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(sharePidStubIntent);
        Thread.sleep(WAIT_TIME);
        List<RunningAppProcessInfo> shareStublist = am.getRunningAppProcesses();
        int shareStubPid = getPid(shareProcessName, shareStublist);
        assertTrue(-1 != shareStubPid);
        assertEquals(sharePid, shareStubPid);
        // no share pid will create a new process
        Intent noSharePidIntent = new Intent();
        noSharePidIntent.setClass(mContext, NoSharePidActivity.class);
        noSharePidIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(noSharePidIntent);
        Thread.sleep(WAIT_TIME);
        List<RunningAppProcessInfo> noShareStublist = am.getRunningAppProcesses();
        int noSharePid = getPid(noShareProcessName, noShareStublist);
        assertTrue(-1 != noSharePid);
        assertTrue(sharePid != noSharePid);
        // kill the process after test
        android.os.Process.killProcess(noSharePid);
        android.os.Process.killProcess(sharePid);
    }

    private int getPid(String processName, List<RunningAppProcessInfo> list) {
        for (RunningAppProcessInfo rai : list) {
            if (processName.equals(rai.processName))
                return rai.pid;
        }
        return -1;
    }

}
