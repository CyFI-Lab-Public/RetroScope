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
package android.mediastress.cts;

import android.app.Instrumentation;
import android.content.Intent;
import android.media.CamcorderProfile;
import android.media.MediaRecorder.AudioEncoder;
import android.media.MediaRecorder.VideoEncoder;
import android.os.Environment;
import android.test.ActivityInstrumentationTestCase2;

import junit.framework.Assert;

public class NativeMediaTest extends ActivityInstrumentationTestCase2<NativeMediaActivity> {
    private static final int NUMBER_PLAY_PAUSE_REPEATITIONS = 10;
    private static final long PLAY_WAIT_TIME_MS = 4000;

    public NativeMediaTest() {
        super(NativeMediaActivity.class);
    }

    public void test1080pPlay() throws InterruptedException {
        runPlayTest(CamcorderProfile.QUALITY_1080P);
    }

    public void test720pPlay() throws InterruptedException {
        runPlayTest(CamcorderProfile.QUALITY_720P);
    }

    public void test480pPlay() throws InterruptedException {
        runPlayTest(CamcorderProfile.QUALITY_480P);
    }

    public void testDefaultPlay() throws InterruptedException {
        runPlayTest(0);
    }

    private void runPlayTest(int quality) throws InterruptedException {
        if (quality != 0) {
            if (!isResolutionSupported(quality)) {
                return;
            }
        }
        Intent intent = new Intent();
        intent.putExtra(NativeMediaActivity.EXTRA_VIDEO_QUALITY,
                quality);
        setActivityIntent(intent);
        final NativeMediaActivity activity = getActivity();
        final Instrumentation instrumentation = getInstrumentation();
        waitForNativeMediaLifeCycle(activity, true);
        Thread.sleep(PLAY_WAIT_TIME_MS); // let it play for some time
        for (int i = 0; i < NUMBER_PLAY_PAUSE_REPEATITIONS; i++) {
            instrumentation.callActivityOnPause(activity);
            instrumentation.waitForIdleSync();
            waitForNativeMediaLifeCycle(activity, false);
            instrumentation.callActivityOnResume(activity);
            waitForNativeMediaLifeCycle(activity, true);
            Thread.sleep(PLAY_WAIT_TIME_MS); // let it play for some time
        }
    }

    /**
     * wait until life cycle change and checks if the current status is in line with expectation
     * @param activity
     * @param expectAlive expected status, true if it should be alive.
     * @throws InterruptedException
     */
    private void waitForNativeMediaLifeCycle(NativeMediaActivity activity, boolean expectAlive)
            throws InterruptedException {
        Boolean status = activity.waitForNativeMediaLifeCycle();
        Assert.assertNotNull(status); // null means time-out
        Assert.assertEquals(expectAlive, status.booleanValue());
    }

    private boolean isResolutionSupported(int quality) {
        Assert.assertEquals(Environment.getExternalStorageState(), Environment.MEDIA_MOUNTED);
        if (!CamcorderProfile.hasProfile(quality)) {
            return false;
        }
        CamcorderProfile profile = CamcorderProfile.get(quality);
        if ((profile != null) && (profile.videoCodec == VideoEncoder.H264) &&
                (profile.audioCodec == AudioEncoder.AAC)) {
            return true;
        }
        return false;
    }
}
