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
import android.media.audiofx.Virtualizer;
import android.os.Looper;
import android.test.AndroidTestCase;
import android.util.Log;

public class VirtualizerTest extends AndroidTestCase {

    private String TAG = "VirtualizerTest";
    private final static short TEST_STRENGTH = 500;
    private final static short TEST_STRENGTH2 = 1000;
    private final static float STRENGTH_TOLERANCE = 1.1f;  // 10%
    private final static int MAX_LOOPER_WAIT_COUNT = 10;

    private Virtualizer mVirtualizer = null;
    private Virtualizer mVirtualizer2 = null;
    private int mSession = -1;
    private boolean mHasControl = false;
    private boolean mIsEnabled = false;
    private int mChangedParameter = -1;
    private boolean mInitialized = false;
    private Looper mLooper = null;
    private final Object mLock = new Object();
    private ListenerThread mEffectListenerLooper = null;

    //-----------------------------------------------------------------
    // VIRTUALIZER TESTS:
    //----------------------------------

    //-----------------------------------------------------------------
    // 0 - constructor
    //----------------------------------

    //Test case 0.0: test constructor and release
    public void test0_0ConstructorAndRelease() throws Exception {
        Virtualizer eq = null;
        try {
            eq = new Virtualizer(0, 0);
            assertNotNull(" could not create Virtualizer", eq);
            try {
                assertTrue(" invalid effect ID", (eq.getId() != 0));
            } catch (IllegalStateException e) {
                fail("Virtualizer not initialized");
            }
        } catch (IllegalArgumentException e) {
            fail("Virtualizer not found");
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
        getVirtualizer(0);
        try {
            if (mVirtualizer.getStrengthSupported()) {
                short strength = mVirtualizer.getRoundedStrength();
                strength = (strength == TEST_STRENGTH) ? TEST_STRENGTH2 : TEST_STRENGTH;
                mVirtualizer.setStrength((short)strength);
                short strength2 = mVirtualizer.getRoundedStrength();
                // allow STRENGTH_TOLERANCE difference between set strength and rounded strength
                assertTrue("got incorrect strength",
                        ((float)strength2 > (float)strength / STRENGTH_TOLERANCE) &&
                        ((float)strength2 < (float)strength * STRENGTH_TOLERANCE));
            } else {
                short strength = mVirtualizer.getRoundedStrength();
                assertTrue("got incorrect strength", strength >= 0 && strength <= 1000);
            }
        } catch (IllegalArgumentException e) {
            fail("Bad parameter value");
        } catch (UnsupportedOperationException e) {
            fail("get parameter() rejected");
        } catch (IllegalStateException e) {
            fail("get parameter() called in wrong state");
        } finally {
            releaseVirtualizer();
        }
    }

    //Test case 1.1: test properties
    public void test1_1Properties() throws Exception {
        getVirtualizer(0);
        try {
            Virtualizer.Settings settings = mVirtualizer.getProperties();
            String str = settings.toString();
            settings = new Virtualizer.Settings(str);

            short strength = settings.strength;
            if (mVirtualizer.getStrengthSupported()) {
                strength = (strength == TEST_STRENGTH) ? TEST_STRENGTH2 : TEST_STRENGTH;
            }
            settings.strength = strength;
            mVirtualizer.setProperties(settings);
            settings = mVirtualizer.getProperties();

            if (mVirtualizer.getStrengthSupported()) {
                // allow STRENGTH_TOLERANCE difference between set strength and rounded strength
                assertTrue("got incorrect strength",
                        ((float)settings.strength > (float)strength / STRENGTH_TOLERANCE) &&
                        ((float)settings.strength < (float)strength * STRENGTH_TOLERANCE));
            }
        } catch (IllegalArgumentException e) {
            fail("Bad parameter value");
        } catch (UnsupportedOperationException e) {
            fail("get parameter() rejected");
        } catch (IllegalStateException e) {
            fail("get parameter() called in wrong state");
        } finally {
            releaseVirtualizer();
        }
    }

    //Test case 1.2: test setStrength() throws exception after release
    public void test1_2SetStrengthAfterRelease() throws Exception {
        getVirtualizer(0);
        mVirtualizer.release();
        try {
            mVirtualizer.setStrength(TEST_STRENGTH);
        } catch (IllegalStateException e) {
            // test passed
        } finally {
            releaseVirtualizer();
        }
    }

    //-----------------------------------------------------------------
    // 2 - Effect enable/disable
    //----------------------------------

    //Test case 2.0: test setEnabled() and getEnabled() in valid state
    public void test2_0SetEnabledGetEnabled() throws Exception {
        getVirtualizer(0);
        try {
            mVirtualizer.setEnabled(true);
            assertTrue(" invalid state from getEnabled", mVirtualizer.getEnabled());
            mVirtualizer.setEnabled(false);
            assertFalse(" invalid state to getEnabled", mVirtualizer.getEnabled());
        } catch (IllegalStateException e) {
            fail("setEnabled() in wrong state");
        } finally {
            releaseVirtualizer();
        }
    }

    //Test case 2.1: test setEnabled() throws exception after release
    public void test2_1SetEnabledAfterRelease() throws Exception {
        getVirtualizer(0);
        mVirtualizer.release();
        try {
            mVirtualizer.setEnabled(true);
        } catch (IllegalStateException e) {
            // test passed
        } finally {
            releaseVirtualizer();
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

            getVirtualizer(0);
            int looperWaitCount = MAX_LOOPER_WAIT_COUNT;
            while (mHasControl && (looperWaitCount-- > 0)) {
                try {
                    mLock.wait();
                } catch(Exception e) {
                }
            }
            terminateListenerLooper();
            releaseVirtualizer();
        }
        assertFalse("effect control not lost by effect1", mHasControl);
    }

    //Test case 3.1: test enable status listener
    public void test3_1EnableStatusListener() throws Exception {
        synchronized(mLock) {
            mInitialized = false;
            createListenerLooper(false, true, false);
            waitForLooperInitialization_l();

            mVirtualizer2.setEnabled(true);
            mIsEnabled = true;
            getVirtualizer(0);
            mVirtualizer.setEnabled(false);
            int looperWaitCount = MAX_LOOPER_WAIT_COUNT;
            while (mIsEnabled && (looperWaitCount-- > 0)) {
                try {
                    mLock.wait();
                } catch(Exception e) {
                }
            }
            terminateListenerLooper();
            releaseVirtualizer();
        }
        assertFalse("enable status not updated", mIsEnabled);
    }

    //Test case 3.2: test parameter changed listener
    public void test3_2ParameterChangedListener() throws Exception {
        synchronized(mLock) {
            mInitialized = false;
            createListenerLooper(false, false, true);
            waitForLooperInitialization_l();

            getVirtualizer(0);
            mChangedParameter = -1;
            mVirtualizer.setStrength(TEST_STRENGTH);

            int looperWaitCount = MAX_LOOPER_WAIT_COUNT;
            while ((mChangedParameter == -1) && (looperWaitCount-- > 0)) {
                try {
                    mLock.wait();
                } catch(Exception e) {
                }
            }
            terminateListenerLooper();
            releaseVirtualizer();
        }
        assertEquals("parameter change not received",
                Virtualizer.PARAM_STRENGTH, mChangedParameter);
    }

    //-----------------------------------------------------------------
    // private methods
    //----------------------------------

    private void getVirtualizer(int session) {
         if (mVirtualizer == null || session != mSession) {
             if (session != mSession && mVirtualizer != null) {
                 mVirtualizer.release();
                 mVirtualizer = null;
             }
             try {
                mVirtualizer = new Virtualizer(0, session);
                mSession = session;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "getVirtualizer() Virtualizer not found exception: "+e);
            } catch (UnsupportedOperationException e) {
                Log.e(TAG, "getVirtualizer() Effect library not loaded exception: "+e);
            }
         }
         assertNotNull("could not create mVirtualizer", mVirtualizer);
    }

    private void releaseVirtualizer() {
        if (mVirtualizer != null) {
            mVirtualizer.release();
            mVirtualizer = null;
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

    // Initializes the virtualizer listener looper
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
            if (mVirtualizer2 != null) {
                mVirtualizer2.setControlStatusListener(null);
                mVirtualizer2.setEnableStatusListener(null);
                mVirtualizer2.setParameterListener(
                        (Virtualizer.OnParameterChangeListener)null);
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

                mVirtualizer2 = new Virtualizer(0, 0);
                assertNotNull("could not create virtualizer2", mVirtualizer2);

                synchronized(mLock) {
                    if (mControl) {
                        mVirtualizer2.setControlStatusListener(
                                new AudioEffect.OnControlStatusChangeListener() {
                            public void onControlStatusChange(
                                    AudioEffect effect, boolean controlGranted) {
                                synchronized(mLock) {
                                    if (effect == mVirtualizer2) {
                                        mHasControl = controlGranted;
                                        mLock.notify();
                                    }
                                }
                            }
                        });
                    }
                    if (mEnable) {
                        mVirtualizer2.setEnableStatusListener(
                                new AudioEffect.OnEnableStatusChangeListener() {
                            public void onEnableStatusChange(AudioEffect effect, boolean enabled) {
                                synchronized(mLock) {
                                    if (effect == mVirtualizer2) {
                                        mIsEnabled = enabled;
                                        mLock.notify();
                                    }
                                }
                            }
                        });
                    }
                    if (mParameter) {
                        mVirtualizer2.setParameterListener(new Virtualizer.OnParameterChangeListener() {
                            public void onParameterChange(Virtualizer effect, int status,
                                    int param, short value)
                            {
                                synchronized(mLock) {
                                    if (effect == mVirtualizer2) {
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
        if (mVirtualizer2 != null) {
            mVirtualizer2.release();
            mVirtualizer2 = null;
        }
    }

}