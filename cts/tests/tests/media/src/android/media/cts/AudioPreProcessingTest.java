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

package android.media.cts;

import com.android.cts.media.R;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.media.MediaRecorder;
import android.test.AndroidTestCase;


public class AudioPreProcessingTest extends AndroidTestCase {

    private String TAG = "AudioPreProcessingTest";
    // AudioRecord sampling rate
    private final static int SAMPLING_RATE = 8000;

    //-----------------------------------------------------------------
    // AUDIO PRE PROCESSING TESTS:
    //----------------------------------

    //-----------------------------------------------------------------
    // 1 - Noise Suppressor
    //----------------------------------

    //-----------------------------------------------------------------
    // 1.1 - creation
    //----------------------------------

    //Test case 1.1: test NS creation and release
    public void test1_1NsCreateAndRelease() throws Exception {
        AudioRecord ar = getAudioRecord();
        assertNotNull("could not create AudioRecord", ar);

        boolean isAvailable = NoiseSuppressor.isAvailable();

        NoiseSuppressor ns = NoiseSuppressor.create(ar.getAudioSessionId());
        assertTrue("NS not available but created or available and not created",
                isAvailable == (ns != null));
        if (ns != null) {
            ns.release();
        }
        ar.release();
    }

    //-----------------------------------------------------------------
    // 1.2 - NS Enable/disable
    //----------------------------------

    //Test case 1.2: test setEnabled() and getEnabled()
    public void test1_2NsSetEnabledGetEnabled() throws Exception {
        if (!NoiseSuppressor.isAvailable()) {
            return;
        }

        AudioRecord ar = getAudioRecord();
        assertNotNull("could not create AudioRecord", ar);

        NoiseSuppressor ns = NoiseSuppressor.create(ar.getAudioSessionId());
        assertNotNull("could not create NoiseSupressor", ns);
        try {
            ns.setEnabled(true);
            assertTrue("invalid state from getEnabled", ns.getEnabled());
            ns.setEnabled(false);
            assertFalse("invalid state to getEnabled", ns.getEnabled());
            // test passed
        } catch (IllegalStateException e) {
            fail("setEnabled() in wrong state");
        } finally {
            ns.release();
            ar.release();
        }
    }

    //-----------------------------------------------------------------
    // 2 - Acoustic Echo Canceller
    //----------------------------------

    //-----------------------------------------------------------------
    // 2.1 - creation
    //----------------------------------

    //Test case 2.1: test AEC creation and release
    public void test2_1AecCreateAndRelease() throws Exception {
        AudioRecord ar = getAudioRecord();
        assertNotNull("could not create AudioRecord", ar);

        boolean isAvailable = AcousticEchoCanceler.isAvailable();

        AcousticEchoCanceler aec = AcousticEchoCanceler.create(ar.getAudioSessionId());
        assertTrue("AEC not available but created or available and not created",
                isAvailable == (aec != null));
        if (aec != null) {
            aec.release();
        }
        ar.release();
    }

    //-----------------------------------------------------------------
    // 2.2 - AEC Enable/disable
    //----------------------------------

    //Test case 2.2: test AEC setEnabled() and getEnabled()
    public void test2_2AecSetEnabledGetEnabled() throws Exception {
        if (!AcousticEchoCanceler.isAvailable()) {
            return;
        }

        AudioRecord ar = getAudioRecord();
        assertNotNull("could not create AudioRecord", ar);

        AcousticEchoCanceler aec = AcousticEchoCanceler.create(ar.getAudioSessionId());
        assertNotNull("could not create AcousticEchoCanceler", aec);
        try {
            aec.setEnabled(true);
            assertTrue("invalid state from getEnabled", aec.getEnabled());
            aec.setEnabled(false);
            assertFalse("invalid state to getEnabled", aec.getEnabled());
            // test passed
        } catch (IllegalStateException e) {
            fail("setEnabled() in wrong state");
        } finally {
            aec.release();
            ar.release();
        }
    }

    //-----------------------------------------------------------------
    // 3 - Automatic Gain Control
    //----------------------------------

    //-----------------------------------------------------------------
    // 3.1 - creation
    //----------------------------------

    //Test case 3.1: test AGC creation and release
    public void test3_1AgcCreateAndRelease() throws Exception {
        AudioRecord ar = getAudioRecord();
        assertNotNull("could not create AudioRecord", ar);

        boolean isAvailable = AutomaticGainControl.isAvailable();

        AutomaticGainControl agc = AutomaticGainControl.create(ar.getAudioSessionId());
        assertTrue("AGC not available but created or available and not created",
                isAvailable == (agc != null));
        if (agc != null) {
            agc.release();
        }
        ar.release();
    }

    //-----------------------------------------------------------------
    // 3.2 - AEC Enable/disable
    //----------------------------------

    //Test case 3.2: test AGC setEnabled() and getEnabled()
    public void test3_2AgcSetEnabledGetEnabled() throws Exception {
        if (!AutomaticGainControl.isAvailable()) {
            return;
        }

        AudioRecord ar = getAudioRecord();
        assertNotNull("could not create AudioRecord", ar);

        AutomaticGainControl agc = AutomaticGainControl.create(ar.getAudioSessionId());
        assertNotNull("could not create AutomaticGainControl", agc);
        try {
            agc.setEnabled(true);
            assertTrue("invalid state from getEnabled", agc.getEnabled());
            agc.setEnabled(false);
            assertFalse("invalid state to getEnabled", agc.getEnabled());
            // test passed
        } catch (IllegalStateException e) {
            fail("setEnabled() in wrong state");
        } finally {
            agc.release();
            ar.release();
        }
    }

    //-----------------------------------------------------------------
    // private methods
    //----------------------------------

    private AudioRecord getAudioRecord() {
        AudioRecord ar = null;
        try {
            ar = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                    SAMPLING_RATE,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioRecord.getMinBufferSize(SAMPLING_RATE,
                            AudioFormat.CHANNEL_CONFIGURATION_MONO,
                            AudioFormat.ENCODING_PCM_16BIT) * 10);
            assertNotNull("Could not create AudioRecord", ar);
            assertEquals("AudioRecord not initialized",
                    AudioRecord.STATE_INITIALIZED, ar.getState());
        } catch (IllegalArgumentException e) {
            fail("AudioRecord invalid parameter");
        }
        return ar;
    }

}
