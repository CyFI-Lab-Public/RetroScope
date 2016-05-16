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
package android.speech.tts.cts;

import android.content.Context;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.Log;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wrapper for {@link TextToSpeech} with some handy test functionality.
 */
public class TextToSpeechWrapper {
    private static final String LOG_TAG = "TextToSpeechServiceTest";

    public static final String MOCK_TTS_ENGINE = "com.android.cts.stub";

    private final Context mContext;
    private TextToSpeech mTts;
    private final InitWaitListener mInitListener;
    private final UtteranceWaitListener mUtteranceListener;

    /** maximum time to wait for tts to be initialized */
    private static final int TTS_INIT_MAX_WAIT_TIME = 30 * 1000;
    /** maximum time to wait for speech call to be complete */
    private static final int TTS_SPEECH_MAX_WAIT_TIME = 5 * 1000;

    private TextToSpeechWrapper(Context context) {
        mContext = context;
        mInitListener = new InitWaitListener();
        mUtteranceListener = new UtteranceWaitListener();
    }

    private boolean initTts() throws InterruptedException {
        return initTts(new TextToSpeech(mContext, mInitListener));
    }

    private boolean initTts(String engine) throws InterruptedException {
        return initTts(new TextToSpeech(mContext, mInitListener, engine));
    }

    private boolean initTts(TextToSpeech tts) throws InterruptedException {
        mTts = tts;
        if (!mInitListener.waitForInit()) {
            return false;
        }
        mTts.setOnUtteranceCompletedListener(mUtteranceListener);
        return true;
    }

    public boolean waitForComplete(String utteranceId) throws InterruptedException {
        return mUtteranceListener.waitForComplete(utteranceId);
    }

    public TextToSpeech getTts() {
        return mTts;
    }

    public void shutdown() {
        mTts.shutdown();
    }

    public static TextToSpeechWrapper createTextToSpeechWrapper(Context context)
            throws InterruptedException {
        TextToSpeechWrapper wrapper = new TextToSpeechWrapper(context);
        if (wrapper.initTts()) {
            return wrapper;
        } else {
            return null;
        }
    }

    public static TextToSpeechWrapper createTextToSpeechMockWrapper(Context context)
            throws InterruptedException {
        TextToSpeechWrapper wrapper = new TextToSpeechWrapper(context);
        if (wrapper.initTts(MOCK_TTS_ENGINE)) {
            return wrapper;
        } else {
            return null;
        }
    }

    /**
     * Listener for waiting for TTS engine initialization completion.
     */
    private static class InitWaitListener implements OnInitListener {
        private final Lock mLock = new ReentrantLock();
        private final Condition mDone  = mLock.newCondition();
        private Integer mStatus = null;

        public void onInit(int status) {
            mLock.lock();
            try {
                mStatus = new Integer(status);
                mDone.signal();
            } finally {
                mLock.unlock();
            }
        }

        public boolean waitForInit() throws InterruptedException {
            long timeOutNanos = TimeUnit.MILLISECONDS.toNanos(TTS_INIT_MAX_WAIT_TIME);
            mLock.lock();
            try {
                while (mStatus == null) {
                    if (timeOutNanos <= 0) {
                        return false;
                    }
                    timeOutNanos = mDone.awaitNanos(timeOutNanos);
                }
                return mStatus == TextToSpeech.SUCCESS;
            } finally {
                mLock.unlock();
            }
        }
    }

    /**
     * Listener for waiting for utterance completion.
     */
    private static class UtteranceWaitListener implements OnUtteranceCompletedListener {
        private final Lock mLock = new ReentrantLock();
        private final Condition mDone  = mLock.newCondition();
        private final HashSet<String> mCompletedUtterances = new HashSet<String>();

        public void onUtteranceCompleted(String utteranceId) {
            mLock.lock();
            try {
                mCompletedUtterances.add(utteranceId);
                mDone.signal();
            } finally {
                mLock.unlock();
            }
        }

        public boolean waitForComplete(String utteranceId)
                throws InterruptedException {
            long timeOutNanos = TimeUnit.MILLISECONDS.toNanos(TTS_INIT_MAX_WAIT_TIME);
            mLock.lock();
            try {
                while (!mCompletedUtterances.remove(utteranceId)) {
                    if (timeOutNanos <= 0) {
                        return false;
                    }
                    timeOutNanos = mDone.awaitNanos(timeOutNanos);
                }
                return true;
            } finally {
                mLock.unlock();
            }
        }
    }

    /**
     * Determines if given file path is a valid, playable music file.
     */
    public static boolean isSoundFile(String filePath) {
        // use media player to play the file. If it succeeds with no exceptions, assume file is
        //valid
        MediaPlayer mp = null;
        try {
            mp = new MediaPlayer();
            mp.setDataSource(filePath);
            mp.prepare();
            mp.start();
            mp.stop();
            return true;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Exception while attempting to play music file", e);
            return false;
        } finally {
            if (mp != null) {
                mp.release();
            }
        }
    }

}
