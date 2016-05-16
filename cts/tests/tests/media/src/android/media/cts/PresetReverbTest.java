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

import android.media.audiofx.AudioEffect;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.audiofx.PresetReverb;
import android.os.Looper;
import android.test.AndroidTestCase;
import android.util.Log;

public class PresetReverbTest extends AndroidTestCase {

    private String TAG = "PresetReverbTest";
    private final static short FIRST_PRESET = PresetReverb.PRESET_NONE;
    private final static short LAST_PRESET = PresetReverb.PRESET_PLATE;
    private final static int MAX_LOOPER_WAIT_COUNT = 10;

    private PresetReverb mReverb = null;
    private PresetReverb mReverb2 = null;
    private int mSession = -1;
    private boolean mHasControl = false;
    private boolean mIsEnabled = false;
    private int mChangedParameter = -1;
    private boolean mInitialized = false;
    private Looper mLooper = null;
    private final Object mLock = new Object();
    private ListenerThread mEffectListenerLooper = null;

    //-----------------------------------------------------------------
    // PRESET REVERB TESTS:
    //----------------------------------

    //-----------------------------------------------------------------
    // 0 - constructor
    //----------------------------------

    //Test case 0.0: test constructor and release
    public void test0_0ConstructorAndRelease() throws Exception {
        PresetReverb reverb = null;
        try {
            reverb = new PresetReverb(0, 0);
            assertNotNull("could not create PresetReverb", reverb);
            try {
                assertTrue("invalid effect ID", (reverb.getId() != 0));
            } catch (IllegalStateException e) {
                fail("PresetReverb not initialized");
            }
        } catch (IllegalArgumentException e) {
            fail("PresetReverb not found");
        } catch (UnsupportedOperationException e) {
            fail("Effect library not loaded");
        } finally {
            if (reverb != null) {
                reverb.release();
            }
        }
    }

    //-----------------------------------------------------------------
    // 1 - get/set parameters
    //----------------------------------

    //Test case 1.0: test presets
    public void test1_0Presets() throws Exception {
        getReverb(0);
        try {
            for (short preset = FIRST_PRESET;
                 preset <= LAST_PRESET;
                 preset++) {
                mReverb.setPreset(preset);
                assertEquals("got incorrect preset", preset, mReverb.getPreset());
            }
        } catch (IllegalArgumentException e) {
            fail("Bad parameter value");
        } catch (UnsupportedOperationException e) {
            fail("get parameter() rejected");
        } catch (IllegalStateException e) {
            fail("get parameter() called in wrong state");
        } finally {
            releaseReverb();
        }
    }

    //Test case 1.1: test properties
    public void test1_1Properties() throws Exception {
        getReverb(0);
        try {
            PresetReverb.Settings settings = mReverb.getProperties();
            String str = settings.toString();
            settings = new PresetReverb.Settings(str);
            short preset = (settings.preset == PresetReverb.PRESET_SMALLROOM) ?
                            PresetReverb.PRESET_MEDIUMROOM : PresetReverb.PRESET_SMALLROOM;
            settings.preset = preset;
            mReverb.setProperties(settings);
            settings = mReverb.getProperties();
            assertEquals("setProperties failed", settings.preset, preset);
        } catch (IllegalArgumentException e) {
            fail("Bad parameter value");
        } catch (UnsupportedOperationException e) {
            fail("get parameter() rejected");
        } catch (IllegalStateException e) {
            fail("get parameter() called in wrong state");
        } finally {
            releaseReverb();
        }
    }

    //-----------------------------------------------------------------
    // 2 - Effect enable/disable
    //----------------------------------

    //Test case 2.0: test setEnabled() and getEnabled() in valid state
    public void test2_0SetEnabledGetEnabled() throws Exception {
        getReverb(0);
        try {
            mReverb.setEnabled(true);
            assertTrue("invalid state from getEnabled", mReverb.getEnabled());
            mReverb.setEnabled(false);
            assertFalse("invalid state to getEnabled", mReverb.getEnabled());
        } catch (IllegalStateException e) {
            fail("setEnabled() in wrong state");
        } finally {
            releaseReverb();
        }
    }

    //Test case 2.1: test setEnabled() throws exception after release
    public void test2_1SetEnabledAfterRelease() throws Exception {
        getReverb(0);
        mReverb.release();
        try {
            mReverb.setEnabled(true);
        } catch (IllegalStateException e) {
            // test passed
        } finally {
            releaseReverb();
        }
    }

    //-----------------------------------------------------------------
    // 3 priority and listeners
    //----------------------------------

    //Test case 3.0: test control status listener
    public void test3_0ControlStatusListener() throws Exception {
         synchronized(mLock) {
            mHasControl = true;
            mInitialized = false;
            createListenerLooper(true, false, false);
            waitForLooperInitialization_l();

            getReverb(0);
            int looperWaitCount = MAX_LOOPER_WAIT_COUNT;
            while (mHasControl && (looperWaitCount-- > 0)) {
                try {
                    mLock.wait();
                } catch(Exception e) {
                }
            }
            terminateListenerLooper();
            releaseReverb();
        }
        assertFalse("effect control not lost by effect1", mHasControl);
    }

    //Test case 3.1: test enable status listener
    public void test3_1EnableStatusListener() throws Exception {
         synchronized(mLock) {
            mInitialized = false;
            createListenerLooper(false, true, false);
            waitForLooperInitialization_l();

            mReverb2.setEnabled(true);
            mIsEnabled = true;
            getReverb(0);
            mReverb.setEnabled(false);
            int looperWaitCount = MAX_LOOPER_WAIT_COUNT;
            while (mIsEnabled && (looperWaitCount-- > 0)) {
                try {
                    mLock.wait();
                } catch(Exception e) {
                }
            }
            terminateListenerLooper();
            releaseReverb();
        }
        assertFalse("enable status not updated", mIsEnabled);
    }

    //Test case 3.2: test parameter changed listener
    public void test3_2ParameterChangedListener() throws Exception {
        synchronized(mLock) {
            mInitialized = false;
            createListenerLooper(false, false, true);
            waitForLooperInitialization_l();

            getReverb(0);
            mChangedParameter = -1;
            mReverb.setPreset(PresetReverb.PRESET_SMALLROOM);

            int looperWaitCount = MAX_LOOPER_WAIT_COUNT;
            while ((mChangedParameter == -1) && (looperWaitCount-- > 0)) {
                try {
                    mLock.wait();
                } catch(Exception e) {
                }
            }
            terminateListenerLooper();
            releaseReverb();
        }
        assertEquals("parameter change not received",
                PresetReverb.PARAM_PRESET, mChangedParameter);
    }

    //-----------------------------------------------------------------
    // private methods
    //----------------------------------

    private void getReverb(int session) {
         if (mReverb == null || session != mSession) {
             if (session != mSession && mReverb != null) {
                 mReverb.release();
                 mReverb = null;
             }
             try {
                mReverb = new PresetReverb(0, session);
                mSession = session;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "getReverb() PresetReverb not found exception: "+e);
            } catch (UnsupportedOperationException e) {
                Log.e(TAG, "getReverb() Effect library not loaded exception: "+e);
            }
         }
         assertNotNull("could not create mReverb", mReverb);
    }

    private void releaseReverb() {
        if (mReverb != null) {
            mReverb.release();
            mReverb = null;
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

    // Initializes the reverb listener looper
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
            if (mReverb2 != null) {
                mReverb2.setControlStatusListener(null);
                mReverb2.setEnableStatusListener(null);
                mReverb2.setParameterListener(
                            (PresetReverb.OnParameterChangeListener)null);
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

                mReverb2 = new PresetReverb(0, 0);
                assertNotNull("could not create Reverb2", mReverb2);

                synchronized(mLock) {
                    if (mControl) {
                        mReverb2.setControlStatusListener(
                                new AudioEffect.OnControlStatusChangeListener() {
                            public void onControlStatusChange(
                                    AudioEffect effect, boolean controlGranted) {
                                synchronized(mLock) {
                                    if (effect == mReverb2) {
                                        mHasControl = controlGranted;
                                        mLock.notify();
                                    }
                                }
                            }
                        });
                    }
                    if (mEnable) {
                        mReverb2.setEnableStatusListener(
                                new AudioEffect.OnEnableStatusChangeListener() {
                            public void onEnableStatusChange(AudioEffect effect, boolean enabled) {
                                synchronized(mLock) {
                                    if (effect == mReverb2) {
                                        mIsEnabled = enabled;
                                        mLock.notify();
                                    }
                                }
                            }
                        });
                    }
                    if (mParameter) {
                        mReverb2.setParameterListener(new PresetReverb.OnParameterChangeListener() {
                            public void onParameterChange(PresetReverb effect,
                                    int status, int param, short value)
                            {
                                synchronized(mLock) {
                                    if (effect == mReverb2) {
                                        mChangedParameter = param;
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

        if (mReverb2 != null) {
            mReverb2.release();
            mReverb2 = null;
        }
    }

}