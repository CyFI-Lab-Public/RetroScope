/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.res.AssetFileDescriptor;
import android.media.audiofx.AudioEffect;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.EnvironmentalReverb;
import android.media.audiofx.Equalizer;
import android.media.MediaPlayer;
import android.media.MediaRecorder;

import android.os.Looper;
import android.test.AndroidTestCase;
import android.util.Log;
import java.util.UUID;

public class AudioEffectTest extends AndroidTestCase {

    private String TAG = "AudioEffectTest";
    private final static int MIN_NUMBER_EFFECTS = 5;
    // allow +/- 5% tolerance between set and get delays
    private final static float DELAY_TOLERANCE = 1.05f;
    // allow +/- 5% tolerance between set and get ratios
    private final static float RATIO_TOLERANCE = 1.05f;
    // AudioRecord sampling rate
    private final static int SAMPLING_RATE = 44100;

    private final static int MAX_LOOPER_WAIT_COUNT = 10;

    private AudioEffect mEffect = null;
    private AudioEffect mEffect2 = null;
    private int mSession = -1;
    private boolean mHasControl = false;
    private boolean mIsEnabled = false;
    private int mChangedParameter = -1;
    private boolean mInitialized = false;
    private Looper mLooper = null;
    private MediaPlayer mMediaPlayer = null;
    private int mError = 0;

    private final Object mLock = new Object();

    private ListenerThread mEffectListenerLooper = null;

    //-----------------------------------------------------------------
    // AUDIOEFFECT TESTS:
    //----------------------------------

    //-----------------------------------------------------------------
    // 0 - static methods
    //----------------------------------

    //Test case 0.0: test queryEffects() and platfrom at least provides Equalizer, Bass Boost,
    // Virtualizer, Environmental reverb and Preset reverb effects
    public void test0_0QueryEffects() throws Exception {

        AudioEffect.Descriptor[] desc = AudioEffect.queryEffects();

        assertTrue("test0_0QueryEffects: number of effects < MIN_NUMBER_EFFECTS: "+desc.length,
                (desc.length >= MIN_NUMBER_EFFECTS));

        boolean hasEQ = false;
        boolean hasBassBoost = false;
        boolean hasVirtualizer = false;
        boolean hasEnvReverb = false;
        boolean hasPresetReverb = false;

        for (int i = 0; i < desc.length; i++) {
            if (desc[i].type.equals(AudioEffect.EFFECT_TYPE_EQUALIZER)) {
                hasEQ = true;
            } else if (desc[i].type.equals(AudioEffect.EFFECT_TYPE_BASS_BOOST)) {
                hasBassBoost = true;
            } else if (desc[i].type.equals(AudioEffect.EFFECT_TYPE_VIRTUALIZER)) {
                hasVirtualizer = true;
            } else if (desc[i].type.equals(AudioEffect.EFFECT_TYPE_ENV_REVERB)) {
                hasEnvReverb = true;
            } else if (desc[i].type.equals(AudioEffect.EFFECT_TYPE_PRESET_REVERB)) {
                hasPresetReverb = true;
            }
        }
        assertTrue("test0_0QueryEffects: equalizer not found", hasEQ);
        assertTrue("test0_0QueryEffects: bass boost not found", hasBassBoost);
        assertTrue("test0_0QueryEffects: virtualizer not found", hasVirtualizer);
        assertTrue("test0_0QueryEffects: environmental reverb not found", hasEnvReverb);
        assertTrue("test0_0QueryEffects: preset reverb not found", hasPresetReverb);
    }

    //-----------------------------------------------------------------
    // 1 - constructor
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

    //Test case 1.0: test constructor from effect type and get effect ID
    public void test1_0ConstructorFromType() throws Exception {
        AudioEffect.Descriptor[] desc = AudioEffect.queryEffects();
        assertTrue("no effects found", (desc.length != 0));
        for (int i = 0; i < desc.length; i++) {
            if (!desc[i].type.equals(AudioEffect.EFFECT_TYPE_NULL)) {
                try {
                    int sessionId;
                    AudioRecord ar = null;
                    if (AudioEffect.EFFECT_PRE_PROCESSING.equals(desc[i].connectMode)) {
                        ar = getAudioRecord();
                        sessionId = ar.getAudioSessionId();
                    } else {
                        sessionId = 0;
                    }
                    AudioEffect effect = new AudioEffect(desc[i].type,
                            AudioEffect.EFFECT_TYPE_NULL,
                            0,
                            sessionId);

                    assertNotNull("could not create AudioEffect", effect);
                    try {
                        assertTrue("invalid effect ID", (effect.getId() != 0));
                    } catch (IllegalStateException e) {
                        fail("AudioEffect not initialized");
                    } finally {
                        effect.release();
                        if (ar != null) {
                            ar.release();
                        }
                    }
                } catch (IllegalArgumentException e) {
                    fail("Effect not found: "+desc[i].name);
                } catch (UnsupportedOperationException e) {
                    fail("Effect library not loaded");
                }
            }
        }
    }

    //Test case 1.1: test constructor from effect uuid
    public void test1_1ConstructorFromUuid() throws Exception {
        AudioEffect.Descriptor[] desc = AudioEffect.queryEffects();
        assertTrue("no effects found", (desc.length != 0));
        for (int i = 0; i < desc.length; i++) {
            try {
                int sessionId;
                AudioRecord ar = null;
                if (AudioEffect.EFFECT_PRE_PROCESSING.equals(desc[i].connectMode)) {
                    ar =  getAudioRecord();
                    sessionId = ar.getAudioSessionId();
                } else {
                    sessionId = 0;
                }
                AudioEffect effect = new AudioEffect(AudioEffect.EFFECT_TYPE_NULL,
                        desc[i].uuid,
                        0,
                        sessionId);
                assertNotNull("could not create AudioEffect", effect);
                try {
                    assertTrue("invalid effect ID", (effect.getId() != 0));
                } catch (IllegalStateException e) {
                    fail("AudioEffect not initialized");
                } finally {
                    effect.release();
                    if (ar != null) {
                        ar.release();
                    }
                }
            } catch (IllegalArgumentException e) {
                fail("Effect not found: "+desc[i].name);
            } catch (UnsupportedOperationException e) {
                fail("Effect library not loaded");
            }
        }
    }

    //Test case 1.2: test constructor failure from unknown type
    public void test1_2ConstructorUnknownType() throws Exception {

        try {
            AudioEffect effect = new AudioEffect(UUID.randomUUID(),
                    AudioEffect.EFFECT_TYPE_NULL,
                    0,
                    0);
            fail("could create random AudioEffect");
            if (effect != null) {
                effect.release();
            }
        } catch (IllegalArgumentException e) {

        } catch (UnsupportedOperationException e) {
            fail("Effect library not loaded");
        }
    }

    //Test case 1.3: test getEnabled() failure when called on released effect
    public void test1_3GetEnabledAfterRelease() throws Exception {

        try {
            AudioEffect effect = new AudioEffect(AudioEffect.EFFECT_TYPE_EQUALIZER,
                    AudioEffect.EFFECT_TYPE_NULL,
                    0,
                    0);
            assertNotNull("could not create AudioEffect", effect);
            effect.release();
            try {
                effect.getEnabled();
                fail("getEnabled() processed after release()");
            } catch (IllegalStateException e) {

            }
        } catch (IllegalArgumentException e) {
            fail("AudioEffect not found");
        } catch (UnsupportedOperationException e) {
            fail("Effect library not loaded");
        }
    }

    //Test case 1.4: test contructor on mediaPlayer audio session
    public void test1_4InsertOnMediaPlayer() throws Exception {
        MediaPlayer mp = new MediaPlayer();
        assertNotNull("could not create mediaplayer", mp);
        AssetFileDescriptor afd = mContext.getResources().openRawResourceFd(R.raw.testmp3);
        mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        afd.close();
        getEffect(AudioEffect.EFFECT_TYPE_EQUALIZER, mp.getAudioSessionId());
        try {
            mEffect.setEnabled(true);

        } catch (IllegalStateException e) {
            fail("AudioEffect not initialized");
        } finally {
            mp.release();
            releaseEffect();
        }
    }

    //Test case 1.5: test auxiliary effect attachement on MediaPlayer
    public void test1_5AuxiliaryOnMediaPlayer() throws Exception {
        synchronized(mLock) {
            mInitialized = false;
            createMediaPlayerLooper();
            waitForLooperInitialization_l();

            mError = 0;
            AssetFileDescriptor afd = mContext.getResources().openRawResourceFd(R.raw.testmp3);
            mMediaPlayer.setDataSource(afd.getFileDescriptor(),
                                       afd.getStartOffset(),
                                       afd.getLength());
            afd.close();
            getEffect(AudioEffect.EFFECT_TYPE_PRESET_REVERB, 0);
            try {
                try {
                    mMediaPlayer.attachAuxEffect(mEffect.getId());
                    mMediaPlayer.setAuxEffectSendLevel(1.0f);
                    mLock.wait(1000);
                } catch(Exception e) {
                    fail("Attach effect: wait was interrupted.");
                }
                assertTrue("error on attachAuxEffect", mError == 0);
            } catch (IllegalStateException e) {
                fail("attach aux effect failed");
            } finally {
                terminateMediaPlayerLooper();
                releaseEffect();
            }
        }
    }

    //Test case 1.6: test auxiliary effect attachement failure before setDatasource
    public void test1_6AuxiliaryOnMediaPlayerFailure() throws Exception {
        synchronized(mLock) {
            mInitialized = false;
            createMediaPlayerLooper();
            waitForLooperInitialization_l();

            getEffect(AudioEffect.EFFECT_TYPE_PRESET_REVERB, 0);

            mError = 0;
            int looperWaitCount = MAX_LOOPER_WAIT_COUNT;
            while (mError == 0 && (looperWaitCount-- > 0)) {
                try {
                    try {
                        mMediaPlayer.attachAuxEffect(mEffect.getId());
                    } catch (IllegalStateException e) {
                        terminateMediaPlayerLooper();
                        releaseEffect();
                        fail("attach aux effect failed");
                    }
                    mLock.wait();
                } catch(Exception e) {
                }
            }
            assertTrue("no error on attachAuxEffect", mError != 0);
        }
        terminateMediaPlayerLooper();
        releaseEffect();
    }


    //Test case 1.7: test auxiliary effect attachement on AudioTrack
    public void test1_7AuxiliaryOnAudioTrack() throws Exception {
        AudioTrack track = null;
        getEffect(AudioEffect.EFFECT_TYPE_PRESET_REVERB, 0);
        try {
            track = new AudioTrack(
                                AudioManager.STREAM_MUSIC,
                                44100,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                AudioTrack.getMinBufferSize(44100,
                                                            AudioFormat.CHANNEL_OUT_MONO,
                                                            AudioFormat.ENCODING_PCM_16BIT),
                                                            AudioTrack.MODE_STREAM);
            assertNotNull("could not create AudioTrack", track);

            int status = track.attachAuxEffect(mEffect.getId());
            if (status != AudioTrack.SUCCESS) {
                fail("could not attach aux effect");
            }
            status = track.setAuxEffectSendLevel(1.0f);
            if (status != AudioTrack.SUCCESS) {
                fail("could not set send level");
            }
        } catch (IllegalStateException e) {
            fail("could not attach aux effect");
        } catch (IllegalArgumentException e) {
            fail("could not create AudioTrack");
        } finally {
            if (track != null) {
                track.release();
            }
            releaseEffect();
        }
    }

    //-----------------------------------------------------------------
    // 2 - enable/ disable
    //----------------------------------


    //Test case 2.0: test setEnabled() and getEnabled() in valid state
    public void test2_0SetEnabledGetEnabled() throws Exception {

        try {
            AudioEffect effect = new AudioEffect(AudioEffect.EFFECT_TYPE_EQUALIZER,
                    AudioEffect.EFFECT_TYPE_NULL,
                    0,
                    0);
            assertNotNull("could not create AudioEffect", effect);
            try {
                effect.setEnabled(true);
                assertTrue("invalid state from getEnabled", effect.getEnabled());
                effect.setEnabled(false);
                assertFalse("invalid state to getEnabled", effect.getEnabled());

            } catch (IllegalStateException e) {
                fail("setEnabled() in wrong state");
            } finally {
                effect.release();
            }
        } catch (IllegalArgumentException e) {
            fail("AudioEffect not found");

        } catch (UnsupportedOperationException e) {
            fail("Effect library not loaded");
        }
    }

    //Test case 2.1: test setEnabled() throws exception after release
    public void test2_1SetEnabledAfterRelease() throws Exception {

        try {
            AudioEffect effect = new AudioEffect(AudioEffect.EFFECT_TYPE_EQUALIZER,
                    AudioEffect.EFFECT_TYPE_NULL,
                    0,
                    0);
            assertNotNull("could not create AudioEffect", effect);
            effect.release();
            try {
                effect.setEnabled(true);
                fail("setEnabled() processed after release");
            } catch (IllegalStateException e) {
                // test passed
            }
        } catch (IllegalArgumentException e) {
            fail("AudioEffect not found");
        } catch (UnsupportedOperationException e) {
            fail("Effect library not loaded");
        }
    }

    //-----------------------------------------------------------------
    // 3 - set/get parameters
    //----------------------------------

    //Test case 3.0: test setParameter(byte[], byte[]) / getParameter(byte[], byte[])
    public void test3_0SetParameterByteArrayByteArray() throws Exception {
        getEffect(AudioEffect.EFFECT_TYPE_PRESET_REVERB, 0);
        try {
            byte[] param = mEffect.intToByteArray(PresetReverb.PARAM_PRESET);
            byte[] value = new byte[2];
            int status = mEffect.getParameter(param, value);
            assertFalse("getParameter 1 failed", AudioEffect.isError(status));
            short preset = PresetReverb.PRESET_SMALLROOM;
            if (mEffect.byteArrayToShort(value) == preset) {
                preset = PresetReverb.PRESET_MEDIUMROOM;
            }
            value = mEffect.shortToByteArray(preset);
            status = mEffect.setParameter(param, value);
            assertEquals("setParameter failed", AudioEffect.SUCCESS, status);
            status = mEffect.getParameter(param, value);
            assertFalse("getParameter 2 failed", AudioEffect.isError(status));
            assertEquals("get/set Parameter failed", preset,
                    mEffect.byteArrayToShort(value));

        } catch (IllegalArgumentException e) {
            fail("Bad parameter value");
        } catch (UnsupportedOperationException e) {
            fail("setParameter() rejected");
        } catch (IllegalStateException e) {
            fail("setParameter() called in wrong state");
        } finally {
            releaseEffect();
        }
    }

    //Test case 3.1: test setParameter(int, int) / getParameter(int, int[])
    public void test3_1SetParameterIntInt() throws Exception {
        getEffect(AudioEffect.EFFECT_TYPE_ENV_REVERB, 0);
        try {
            int param = EnvironmentalReverb.PARAM_DECAY_TIME;
            int[] value = new int[1];
            int status = mEffect.getParameter(param, value);
            assertFalse("getParameter 1 failed", AudioEffect.isError(status));
            int time = 500;
            if (value[0] == time) {
                time = 1000;
            }
            status = mEffect.setParameter(param, time);
            assertEquals("setParameter failed", AudioEffect.SUCCESS, status);
            status = mEffect.getParameter(param, value);
            assertFalse("getParameter 2 failed", AudioEffect.isError(status));
            assertTrue("got incorrect decay time",
                    ((float)value[0] > (float)(time / DELAY_TOLERANCE)) &&
                    ((float)value[0] < (float)(time * DELAY_TOLERANCE)));

        } catch (IllegalArgumentException e) {
            fail("Bad parameter value");
        } catch (UnsupportedOperationException e) {
            fail("setParameter() rejected");
        } catch (IllegalStateException e) {
            fail("setParameter() called in wrong state");
        } finally {
            releaseEffect();
        }
    }

    //Test case 3.2: test setParameter(int, short) / getParameter(int, short[])
    public void test3_2SetParameterIntShort() throws Exception {
        getEffect(AudioEffect.EFFECT_TYPE_PRESET_REVERB, 0);
        try {
            int param = PresetReverb.PARAM_PRESET;
            short[] value = new short[1];
            int status = mEffect.getParameter(param, value);
            assertFalse("getParameter 1 failed", AudioEffect.isError(status));
            short preset = PresetReverb.PRESET_SMALLROOM;
            if (value[0] == preset) {
                preset = PresetReverb.PRESET_MEDIUMROOM;
            }
            status = mEffect.setParameter(param, preset);
            assertEquals("setParameter failed", AudioEffect.SUCCESS, status);
            status = mEffect.getParameter(param, value);
            assertFalse("getParameter 2 failed", AudioEffect.isError(status));
            assertEquals("get/set Parameter failed", preset, value[0]);

        } catch (IllegalArgumentException e) {
            fail("Bad parameter value");
        } catch (UnsupportedOperationException e) {
            fail("setParameter() rejected");
        } catch (IllegalStateException e) {
            fail("setParameter() called in wrong state");
        } finally {
            releaseEffect();
        }
    }

    //Test case 3.3: test setParameter(int, byte[]) / getParameter(int, byte[])
    public void test3_3SetParameterIntByteArray() throws Exception {
        getEffect(AudioEffect.EFFECT_TYPE_ENV_REVERB, 0);
        try {
            int param = EnvironmentalReverb.PARAM_DECAY_TIME;
            byte[] value = new byte[4];
            int status = mEffect.getParameter(param, value);
            assertFalse("getParameter 1 failed", AudioEffect.isError(status));
            int time = 500;
            if (mEffect.byteArrayToInt(value) == time) {
                time = 1000;
            }
            value = mEffect.intToByteArray(time);
            status = mEffect.setParameter(param, value);
            assertEquals("setParameter failed", AudioEffect.SUCCESS, status);
            status = mEffect.getParameter(param, value);
            assertFalse("getParameter 2 failed", AudioEffect.isError(status));
            int time2 = mEffect.byteArrayToInt(value);
            assertTrue("got incorrect decay time",
                    ((float)time2 > (float)(time / DELAY_TOLERANCE)) &&
                    ((float)time2 < (float)(time * DELAY_TOLERANCE)));

        } catch (IllegalArgumentException e) {
            fail("Bad parameter value");
        } catch (UnsupportedOperationException e) {
            fail("setParameter() rejected");
        } catch (IllegalStateException e) {
            fail("setParameter() called in wrong state");
        } finally {
            releaseEffect();
        }
    }

    //Test case 3.4: test setParameter(int[], int[]) / getParameter(int[], int[])
    public void test3_4SetParameterIntArrayIntArray() throws Exception {
        getEffect(AudioEffect.EFFECT_TYPE_ENV_REVERB, 0);
        try {
            int[] param = new int[1];
            int[] value = new int[1];
            param[0] = EnvironmentalReverb.PARAM_DECAY_TIME;
            int status = mEffect.getParameter(param, value);
            assertFalse("getParameter 1 failed", AudioEffect.isError(status));
            int[] time = new int[1];
            time[0] = 500;
            if (value[0] == time[0]) {
                time[0] = 1000;
            }
            status = mEffect.setParameter(param, time);
            assertEquals("setParameter failed", AudioEffect.SUCCESS, status);
            status = mEffect.getParameter(param, value);
            assertFalse("getParameter 2 failed", AudioEffect.isError(status));
            assertTrue("got incorrect decay time",
                    ((float)value[0] > (float)(time[0] / DELAY_TOLERANCE)) &&
                    ((float)value[0] < (float)(time[0] * DELAY_TOLERANCE)));

        } catch (IllegalArgumentException e) {
            fail("Bad parameter value");
        } catch (UnsupportedOperationException e) {
            fail("setParameter() rejected");
        } catch (IllegalStateException e) {
            fail("setParameter() called in wrong state");
        } finally {
            releaseEffect();
        }
    }

    //Test case 3.5: test setParameter(int[], short[]) / getParameter(int[], short[])

    public void test3_5SetParameterIntArrayShortArray() throws Exception {
        getEffect(AudioEffect.EFFECT_TYPE_PRESET_REVERB, 0);
        try {
            int[] param = new int[1];
            param[0] = PresetReverb.PARAM_PRESET;
            short[] value = new short[1];
            int status = mEffect.getParameter(param, value);
            assertFalse("getParameter 1 failed", AudioEffect.isError(status));
            short[] preset = new short[1];
            preset[0] = PresetReverb.PRESET_SMALLROOM;
            if (value[0] == preset[0]) {
                preset[0] = PresetReverb.PRESET_MEDIUMROOM;
            }
            status = mEffect.setParameter(param, preset);
            assertEquals("setParameter failed", AudioEffect.SUCCESS, status);
            status = mEffect.getParameter(param, value);
            assertFalse("getParameter 2 failed", AudioEffect.isError(status));
            assertEquals("get/set Parameter failed", preset[0], value[0]);

        } catch (IllegalArgumentException e) {
            fail("Bad parameter value");
        } catch (UnsupportedOperationException e) {
            fail("setParameter() rejected");
        } catch (IllegalStateException e) {
            fail("setParameter() called in wrong state");
        } finally {
            releaseEffect();
        }
    }

    //Test case 3.6: test setParameter(int[], byte[]) / getParameter(int[], byte[])
    public void test3_6SetParameterIntArrayByteArray() throws Exception {
        getEffect(AudioEffect.EFFECT_TYPE_ENV_REVERB, 0);
        try {
            int[] param = new int[1];
            param[0] = EnvironmentalReverb.PARAM_DECAY_TIME;
            byte[] value = new byte[4];
            int status = mEffect.getParameter(param, value);
            assertFalse("getParameter 1 failed", AudioEffect.isError(status));
            int time = 500;
            if (mEffect.byteArrayToInt(value) == time) {
                time = 1000;
            }

            status = mEffect.setParameter(param, mEffect.intToByteArray(time));
            assertEquals("setParameter failed", AudioEffect.SUCCESS, status);
            status = mEffect.getParameter(param, value);
            assertFalse("getParameter 2 failed", AudioEffect.isError(status));
            int time2 = mEffect.byteArrayToInt(value);
            assertTrue("got incorrect decay time",
                    ((float)time2 > (float)(time / DELAY_TOLERANCE)) &&
                    ((float)time2 < (float)(time * DELAY_TOLERANCE)));

        } catch (IllegalArgumentException e) {
            fail("Bad parameter value");
        } catch (UnsupportedOperationException e) {
            fail("setParameter() rejected");
        } catch (IllegalStateException e) {
            fail("setParameter() called in wrong state");
        } finally {
            releaseEffect();
        }
    }

    //Test case 3.7: test setParameter() throws exception after release()
    public void test3_7SetParameterAfterRelease() throws Exception {
        AudioEffect effect = null;
        try {
            effect = new AudioEffect(AudioEffect.EFFECT_TYPE_PRESET_REVERB,
                                    AudioEffect.EFFECT_TYPE_NULL,
                                    0,
                                    0);
            assertNotNull("could not create AudioEffect", effect);
            effect.release();
            effect.setParameter(PresetReverb.PARAM_PRESET, PresetReverb.PRESET_SMALLROOM);
            fail("setParameter() processed after release");
        } catch (IllegalArgumentException e) {
            fail("Bad parameter value");
        } catch (UnsupportedOperationException e) {
            fail("setParameter() rejected");
        } catch (IllegalStateException e) {
            // test passed
        } finally {
            if (effect != null) {
                effect.release();
            }
        }
    }

    //Test case 3.8: test getParameter() throws exception after release()
    public void test3_8GetParameterAfterRelease() throws Exception {
        AudioEffect effect = null;
        try {
            effect = new AudioEffect(AudioEffect.EFFECT_TYPE_PRESET_REVERB,
                                    AudioEffect.EFFECT_TYPE_NULL,
                                    0,
                                    0);
            assertNotNull("could not create AudioEffect", effect);
            effect.release();
            short[] value = new short[1];
            effect.getParameter(PresetReverb.PARAM_PRESET, value);
            fail("getParameter() processed after release");
        } catch (IllegalArgumentException e) {
            fail("Bad parameter value");
        } catch (UnsupportedOperationException e) {
            fail("getParameter() rejected");
        } catch (IllegalStateException e) {
            // test passed
        } finally {
            if (effect != null) {
                effect.release();
            }
        }
    }

    //-----------------------------------------------------------------
    // 4 priority and listeners
    //----------------------------------

    //Test case 4.0: test control passed to higher priority client
    public void test4_0setEnabledLowerPriority() throws Exception {
        AudioEffect effect1 = null;
        AudioEffect effect2 = null;
        try {
            effect1 = new AudioEffect(AudioEffect.EFFECT_TYPE_EQUALIZER,
                                    AudioEffect.EFFECT_TYPE_NULL,
                                    0,
                                    0);
            effect2 = new AudioEffect(AudioEffect.EFFECT_TYPE_EQUALIZER,
                    AudioEffect.EFFECT_TYPE_NULL,
                    1,
                    0);

            assertNotNull("could not create AudioEffect", effect1);
            assertNotNull("could not create AudioEffect", effect2);

            assertTrue("Effect2 does not have control", effect2.hasControl());
            assertFalse("Effect1 has control", effect1.hasControl());
            assertTrue("Effect1 can enable",
                    effect1.setEnabled(true) == AudioEffect.ERROR_INVALID_OPERATION);
            assertFalse("Effect1 has enabled", effect2.getEnabled());

        } catch (IllegalArgumentException e) {
            fail("Effect not found");
        } catch (UnsupportedOperationException e) {
            fail("Effect library not loaded");
        } finally {
            if (effect1 != null) {
                effect1.release();
            }
            if (effect2 != null) {
                effect2.release();
            }
        }
    }

    //Test case 4.1: test control passed to higher priority client
    public void test4_1setParameterLowerPriority() throws Exception {
        AudioEffect effect1 = null;
        AudioEffect effect2 = null;
        try {
            effect1 = new AudioEffect(AudioEffect.EFFECT_TYPE_PRESET_REVERB,
                                    AudioEffect.EFFECT_TYPE_NULL,
                                    0,
                                    0);
            effect2 = new AudioEffect(AudioEffect.EFFECT_TYPE_PRESET_REVERB,
                    AudioEffect.EFFECT_TYPE_NULL,
                    1,
                    0);

            assertNotNull("could not create AudioEffect", effect1);
            assertNotNull("could not create AudioEffect", effect2);

            int status = effect2.setParameter(PresetReverb.PARAM_PRESET,
                    PresetReverb.PRESET_SMALLROOM);
            assertEquals("Effect2 setParameter failed",
                    AudioEffect.SUCCESS, status);

            status = effect1.setParameter(PresetReverb.PARAM_PRESET,
                    PresetReverb.PRESET_MEDIUMROOM);
            assertEquals("Effect1 setParameter did not fail",
                    AudioEffect.ERROR_INVALID_OPERATION, status);

            short[] value = new short[1];
            status = effect2.getParameter(PresetReverb.PARAM_PRESET, value);
            assertFalse("Effect2 getParameter failed",
                    AudioEffect.isError(status));
            assertEquals("Effect1 changed parameter", PresetReverb.PRESET_SMALLROOM
                    , value[0]);


        } catch (IllegalArgumentException e) {
            fail("Effect not found");
        } catch (UnsupportedOperationException e) {
            fail("Effect library not loaded");
        } finally {
            if (effect1 != null) {
                effect1.release();
            }
            if (effect2 != null) {
                effect2.release();
            }
        }
    }

    //Test case 4.2: test control status listener
    public void test4_2ControlStatusListener() throws Exception {

        synchronized(mLock) {
            mHasControl = true;
            mInitialized = false;
            createListenerLooper(true, false, false);
            waitForLooperInitialization_l();

            getEffect(AudioEffect.EFFECT_TYPE_PRESET_REVERB, 0);
            int looperWaitCount = MAX_LOOPER_WAIT_COUNT;
            while (mHasControl && (looperWaitCount-- > 0)) {
                try {
                    mLock.wait();
                } catch(Exception e) {
                }
            }
            terminateListenerLooper();
            releaseEffect();
        }
        assertFalse("effect control not lost by effect1", mHasControl);
    }

    //Test case 4.3: test enable status listener
    public void test4_3EnableStatusListener() throws Exception {

        synchronized(mLock) {
            mInitialized = false;
            createListenerLooper(false, true, false);
            waitForLooperInitialization_l();

            mEffect2.setEnabled(true);
            mIsEnabled = true;

            getEffect(AudioEffect.EFFECT_TYPE_PRESET_REVERB, 0);
            assertTrue("effect not enabled", mEffect.getEnabled());
            int looperWaitCount = MAX_LOOPER_WAIT_COUNT;
            while (mIsEnabled && (looperWaitCount-- > 0)) {
                try {
                    mEffect.setEnabled(false);
                    mLock.wait();
                } catch(Exception e) {
                }
            }
            terminateListenerLooper();
            releaseEffect();
        }
        assertFalse("enable status not updated", mIsEnabled);
    }

    //Test case 4.4: test parameter changed listener
    public void test4_4ParameterChangedListener() throws Exception {
        synchronized(mLock) {
            mInitialized = false;
            createListenerLooper(false, false, true);
            waitForLooperInitialization_l();
            int status = mEffect2.setParameter(PresetReverb.PARAM_PRESET,
                    PresetReverb.PRESET_SMALLROOM);
            assertEquals("mEffect2 setParameter failed",
                    AudioEffect.SUCCESS, status);
            getEffect(AudioEffect.EFFECT_TYPE_PRESET_REVERB, 0);
            mChangedParameter = -1;
            mEffect.setParameter(PresetReverb.PARAM_PRESET,
                    PresetReverb.PRESET_MEDIUMROOM);
            int looperWaitCount = MAX_LOOPER_WAIT_COUNT;
            while (mChangedParameter == -1 && (looperWaitCount-- > 0)) {
                try {
                    mLock.wait();
                } catch(Exception e) {
                }
            }
            terminateListenerLooper();
            releaseEffect();
        }
        assertEquals("parameter change not received",
                PresetReverb.PARAM_PRESET, mChangedParameter);
    }

    //-----------------------------------------------------------------
    // 5 command method
    //----------------------------------


    //Test case 5.0: test command method
    public void test5_0Command() throws Exception {
        getEffect(AudioEffect.EFFECT_TYPE_PRESET_REVERB, 0);
        try {
            byte[] cmd = new byte[0];
            byte[] reply = new byte[4];
            // command 3 is ENABLE
            int status = mEffect.command(3, cmd, reply);
            assertFalse("command failed", AudioEffect.isError(status));
            assertTrue("effect not enabled", mEffect.getEnabled());

        } catch (IllegalStateException e) {
            fail("command in illegal state");
        } finally {
            releaseEffect();
        }
    }


    //-----------------------------------------------------------------
    // private methods
    //----------------------------------

    private void getEffect(UUID type, int session) {
         if (mEffect == null || session != mSession) {
             if (session != mSession && mEffect != null) {
                 mEffect.release();
                 mEffect = null;
             }
             try {
                 mEffect = new AudioEffect(type,
                                             AudioEffect.EFFECT_TYPE_NULL,
                                             0,
                                             session);
                 mSession = session;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "getEffect() AudioEffect not found exception: "+e);
            } catch (UnsupportedOperationException e) {
                Log.e(TAG, "getEffect() Effect library not loaded exception: "+e);
            }
         }
         assertNotNull("could not create mEffect", mEffect);
    }

    private void releaseEffect() {
        if (mEffect != null) {
            mEffect.release();
            mEffect = null;
        }
    }

    private void waitForLooperInitialization_l() {
        int looperWaitCount = MAX_LOOPER_WAIT_COUNT;
        while (!mInitialized && (looperWaitCount-- > 0)) {
            try {
                mLock.wait();
            } catch(Exception e) {
            }
        }
        assertTrue(mInitialized);
    }

    // Initializes the equalizer listener looper
    class ListenerThread extends Thread {
        boolean mControl;
        boolean mEnable;
        boolean mParameter;

        public ListenerThread(boolean control, boolean enable, boolean parameter) {
            super();
            mControl = control;
            mEnable = enable;
            mParameter = parameter;
        }

        public void cleanUp() {
            if (mEffect2 != null) {
                mEffect2.setControlStatusListener(null);
                mEffect2.setEnableStatusListener(null);
                mEffect2.setParameterListener(null);
            }
        }
    }

    private void createListenerLooper(boolean control, boolean enable, boolean parameter) {
        mEffectListenerLooper = new ListenerThread(control, enable, parameter) {
            @Override
            public void run() {
                // Set up a looper
                Looper.prepare();

                // Save the looper so that we can terminate this thread
                // after we are done with it.
                mLooper = Looper.myLooper();

                mEffect2 = new AudioEffect(AudioEffect.EFFECT_TYPE_PRESET_REVERB,
                        AudioEffect.EFFECT_TYPE_NULL,
                        0,
                        0);
                assertNotNull("could not create Equalizer2", mEffect2);

                synchronized(mLock) {
                    if (mControl) {
                        mEffect2.setControlStatusListener(
                                new AudioEffect.OnControlStatusChangeListener() {
                            public void onControlStatusChange(
                                    AudioEffect effect, boolean controlGranted) {
                                synchronized(mLock) {
                                    if (effect == mEffect2) {
                                        mHasControl = controlGranted;
                                        mLock.notify();
                                    }
                                }
                            }
                        });
                    }
                    if (mEnable) {
                        mEffect2.setEnableStatusListener(
                                new AudioEffect.OnEnableStatusChangeListener() {
                            public void onEnableStatusChange(AudioEffect effect, boolean enabled) {
                                synchronized(mLock) {
                                    if (effect == mEffect2) {
                                        mIsEnabled = enabled;
                                        mLock.notify();
                                    }
                                }
                            }
                        });
                    }
                    if (mParameter) {
                        mEffect2.setParameterListener(new AudioEffect.OnParameterChangeListener() {
                            public void onParameterChange(AudioEffect effect, int status, byte[] param,
                                    byte[] value)
                            {
                                synchronized(mLock) {
                                    if (effect == mEffect2) {
                                        mChangedParameter = mEffect2.byteArrayToInt(param);
                                        mLock.notify();
                                    }
                                }
                            }
                        });
                    }
                    mInitialized = true;
                    mLock.notify();
                }
                Looper.loop();  // Blocks forever until Looper.quit() is called.
            }
        };
        mEffectListenerLooper.start();
    }

    // Terminates the listener looper thread.
    private void terminateListenerLooper() {
        if (mEffectListenerLooper != null) {
            mEffectListenerLooper.cleanUp();
            if (mLooper != null) {
                mLooper.quit();
                mLooper = null;
            }
            try {
                mEffectListenerLooper.join();
            } catch(InterruptedException e) {
            }
            mEffectListenerLooper = null;
        }
        if (mEffect2 != null) {
            mEffect2.release();
            mEffect2 = null;
        }
    }

    /*
     * Initializes the message looper so that the MediaPlayer object can
     * receive the callback messages.
     */
    private void createMediaPlayerLooper() {
        new Thread() {
            @Override
            public void run() {
                // Set up a looper to be used by mMediaPlayer.
                Looper.prepare();

                // Save the looper so that we can terminate this thread
                // after we are done with it.
                mLooper = Looper.myLooper();

                mMediaPlayer = new MediaPlayer();

                synchronized(mLock) {
                    mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        public boolean onError(MediaPlayer player, int what, int extra) {
                            synchronized(mLock) {
                                mError = what;
                                mLock.notify();
                            }
                            return true;
                        }
                    });
                    mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        public void onCompletion(MediaPlayer player) {
                            synchronized(mLock) {
                                mLock.notify();
                            }
                        }
                    });
                    mInitialized = true;
                    mLock.notify();
                }
                Looper.loop();  // Blocks forever until Looper.quit() is called.
            }
        }.start();
    }
    /*
     * Terminates the message looper thread.
     */
    private void terminateMediaPlayerLooper() {
        if (mLooper != null) {
            mLooper.quit();
            mLooper = null;
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
    }

}
