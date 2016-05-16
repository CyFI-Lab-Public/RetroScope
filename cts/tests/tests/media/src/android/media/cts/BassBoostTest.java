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
import android.media.audiofx.BassBoost;
import android.os.Looper;
import android.test.AndroidTestCase;
import android.util.Log;

public class BassBoostTest extends AndroidTestCase {

    private String TAG = "BassBoostTest";
    private final static short TEST_STRENGTH = 500;
    private final static short TEST_STRENGTH2 = 1000;
    private final static float STRENGTH_TOLERANCE = 1.1f;  // 10%
    private final static int MAX_LOOPER_WAIT_COUNT = 10;

    private BassBoost mBassBoost = null;
    private BassBoost mBassBoost2 = null;
    private int mSession = -1;
    private boolean mHasControl = false;
    private boolean mIsEnabled = false;
    private int mChangedParameter = -1;
    private boolean mInitialized = false;
    private Looper mLooper = null;
    private final Object mLock = new Object();
    private ListenerThread mEffectListenerLooper = null;

    //-----------------------------------------------------------------
    // BASS BOOST TESTS:
    //----------------------------------

    //-----------------------------------------------------------------
    // 0 - constructor
    //----------------------------------

    //Test case 0.0: test constructor and release
    public void test0_0ConstructorAndRelease() throws Exception {
        BassBoost eq = null;
        try {
            eq = new BassBoost(0, 0);
            assertNotNull("could not create BassBoost", eq);
            try {
                assertTrue("invalid effect ID", (eq.getId() != 0));
            } catch (IllegalStateException e) {
                fail("BassBoost not initialized");
            }
            // test passed
        } catch (IllegalArgumentException e) {
            fail("BassBoost not found");
        } catch (UnsupportedOperationException e) {
            fail("Effect library not loaded");
        } finally {
            if (eq != null) {
                eq.release();
            }
        }
    }


    //-----------------------------------------------------------------
    // 1 - get/set parameters
    //----------------------------------

    //Test case 1.0: test strength
    public void test1_0Strength() throws Exception {
        getBassBoost(0);
        try {
            if (mBassBoost.getStrengthSupported()) {
                short strength = mBassBoost.getRoundedStrength();
                strength = (strength == TEST_STRENGTH) ? TEST_STRENGTH2 : TEST_STRENGTH;
                mBassBoost.setStrength((short)strength);
                short strength2 = mBassBoost.getRoundedStrength();
                // allow STRENGTH_TOLERANCE difference between set strength and rounded strength
                assertTrue("got incorrect strength",
                        ((float)strength2 > (float)strength / STRENGTH_TOLERANCE) &&
                        ((float)strength2 < (float)strength * STRENGTH_TOLERANCE));
            } else {
                short strength = mBassBoost.getRoundedStrength();
                assertTrue("got incorrect strength", strength >= 0 && strength <= 1000);
            }
            // test passed
        } catch (IllegalArgumentException e) {
            fail("Bad parameter value");
        } catch (UnsupportedOperationException e) {
            fail("get parameter() rejected");
        } catch (IllegalStateException e) {
            fail("get parameter() called in wrong state");
        } finally {
            releaseBassBoost();
        }
    }

    //Test case 1.1: test properties
    public void test1_1Properties() throws Exception {
        getBassBoost(0);
        try {
            BassBoost.Settings settings = mBassBoost.getProperties();
            String str = settings.toString();
            settings = new BassBoost.Settings(str);

            short strength = settings.strength;
            if (mBassBoost.getStrengthSupported()) {
                strength = (strength == TEST_STRENGTH) ? TEST_STRENGTH2 : TEST_STRENGTH;
            }
            settings.strength = strength;
            mBassBoost.setProperties(settings);
            settings = mBassBoost.getProperties();

            if (mBassBoost.getStrengthSupported()) {
                // allow STRENGTH_TOLERANCE difference between set strength and rounded strength
                assertTrue("got incorrect strength",
                        ((float)settings.strength > (float)strength / STRENGTH_TOLERANCE) &&
                        ((float)settings.strength < (float)strength * STRENGTH_TOLERANCE));
            }
            // test passed
        } catch (IllegalArgumentException e) {
            fail("Bad parameter value");
        } catch (UnsupportedOperationException e) {
            fail("get parameter() rejected");
        } catch (IllegalStateException e) {
            fail("get parameter() called in wrong state");
        } finally {
            releaseBassBoost();
        }
    }

    //Test case 1.2: test setStrength() throws exception after release
    public void test1_2SetStrengthAfterRelease() throws Exception {
        getBassBoost(0);
        mBassBoost.release();
        try {
            mBassBoost.setStrength(TEST_STRENGTH);
            fail("setStrength() processed after release()");
        } catch (IllegalStateException e) {
            // test passed
        } finally {
            releaseBassBoost();
        }
    }

    //-----------------------------------------------------------------
    // 2 - Effect enable/disable
    //----------------------------------

    //Test case 2.0: test setEnabled() and getEnabled() in valid state
    public void test2_0SetEnabledGetEnabled() throws Exception {
        getBassBoost(0);
        try {
            mBassBoost.setEnabled(true);
            assertTrue("invalid state from getEnabled", mBassBoost.getEnabled());
            mBassBoost.setEnabled(false);
            assertFalse("invalid state to getEnabled", mBassBoost.getEnabled());
            // test passed
        } catch (IllegalStateException e) {
            fail("setEnabled() in wrong state");
        } finally {
            releaseBassBoost();
        }
    }

    //Test case 2.1: test setEnabled() throws exception after release
    public void test2_1SetEnabledAfterRelease() throws Exception {
        getBassBoost(0);
        mBassBoost.release();
        try {
            mBassBoost.setEnabled(true);
            fail("setEnabled() processed after release()");
        } catch (IllegalStateException e) {
            // test passed
        } finally {
            releaseBassBoost();
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

            getBassBoost(0);
            int looperWaitCount = MAX_LOOPER_WAIT_COUNT;
            while (mHasControl && (looperWaitCount-- > 0)) {
                try {
                    mLock.wait();
                } catch(Exception e) {
                }
            }
            terminateListenerLooper();
            releaseBassBoost();
        }
        assertFalse("effect control not lost by effect1", mHasControl);
    }

    //Test case 3.1: test enable status listener
    public void test3_1EnableStatusListener() throws Exception {
        synchronized(mLock) {
            mInitialized = false;
            createListenerLooper(false, true, false);
            waitForLooperInitialization_l();

            mBassBoost2.setEnabled(true);
            mIsEnabled = true;
            getBassBoost(0);
            mBassBoost.setEnabled(false);
            int looperWaitCount = MAX_LOOPER_WAIT_COUNT;
            while (mIsEnabled && (looperWaitCount-- > 0)) {
                try {
                    mLock.wait();
                } catch(Exception e) {
                }
            }
            terminateListenerLooper();
            releaseBassBoost();
        }
        assertFalse("enable status not updated", mIsEnabled);
    }

    //Test case 3.2: test parameter changed listener
    public void test3_2ParameterChangedListener() throws Exception {
        synchronized(mLock) {
            mInitialized = false;
            createListenerLooper(false, false, true);
            waitForLooperInitialization_l();

            getBassBoost(0);
            mChangedParameter = -1;
            mBassBoost.setStrength(TEST_STRENGTH);
            int looperWaitCount = MAX_LOOPER_WAIT_COUNT;
            while ((mChangedParameter == -1) && (looperWaitCount-- > 0)) {
                try {
                    mLock.wait();
                } catch(Exception e) {
                }
            }
            terminateListenerLooper();
            releaseBassBoost();
        }
        assertEquals("parameter change not received",
                BassBoost.PARAM_STRENGTH, mChangedParameter);
    }

    //-----------------------------------------------------------------
    // private methods
    //----------------------------------

    private void getBassBoost(int session) {
         if (mBassBoost == null || session != mSession) {
             if (session != mSession && mBassBoost != null) {
                 mBassBoost.release();
                 mBassBoost = null;
             }
             try {
                mBassBoost = new BassBoost(0, session);
                mSession = session;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "getBassBoost() BassBoost not found exception: "+e);
            } catch (UnsupportedOperationException e) {
                Log.e(TAG, "getBassBoost() Effect library not loaded exception: "+e);
            }
         }
         assertNotNull("could not create mBassBoost", mBassBoost);
    }

    private void releaseBassBoost() {
        if (mBassBoost != null) {
            mBassBoost.release();
            mBassBoost = null;
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

    // Initializes the bassboot listener looper
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
            if (mBassBoost2 != null) {
                mBassBoost2.setControlStatusListener(null);
                mBassBoost2.setEnableStatusListener(null);
                mBassBoost2.setParameterListener(
                        (BassBoost.OnParameterChangeListener)null);
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

                mBassBoost2 = new BassBoost(0, 0);
                assertNotNull("could not create bassboot2", mBassBoost2);

                synchronized(mLock) {
                    if (mControl) {
                        mBassBoost2.setControlStatusListener(
                                new AudioEffect.OnControlStatusChangeListener() {
                            public void onControlStatusChange(
                                    AudioEffect effect, boolean controlGranted) {
                                synchronized(mLock) {
                                    if (effect == mBassBoost2) {
                                        mHasControl = controlGranted;
                                        mLock.notify();
                                    }
                                }
                            }
                        });
                    }
                    if (mEnable) {
                        mBassBoost2.setEnableStatusListener(
                                new AudioEffect.OnEnableStatusChangeListener() {
                            public void onEnableStatusChange(AudioEffect effect, boolean enabled) {
                                synchronized(mLock) {
                                    if (effect == mBassBoost2) {
                                        mIsEnabled = enabled;
                                        mLock.notify();
                                    }
                                }
                            }
                        });
                    }
                    if (mParameter) {
                        mBassBoost2.setParameterListener(new BassBoost.OnParameterChangeListener() {
                            public void onParameterChange(BassBoost effect, int status,
                                    int param, short value)
                            {
                                synchronized(mLock) {
                                    if (effect == mBassBoost2) {
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

        if (mBassBoost2 != null) {
            mBassBoost2.release();
            mBassBoost2 = null;
        }
    }

}