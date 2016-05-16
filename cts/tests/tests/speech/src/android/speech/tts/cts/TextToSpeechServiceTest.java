/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;

/**
 * Tests for {@link android.speech.tts.TextToSpeechService} using StubTextToSpeechService.
 */
public class TextToSpeechServiceTest extends AndroidTestCase {

    private static final String UTTERANCE_ID = "utterance";
    private static final String UTTERANCE = "text to speech cts test";
    private static final String SAMPLE_FILE_NAME = "mytts.wav";

    private TextToSpeechWrapper mTts;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTts = TextToSpeechWrapper.createTextToSpeechMockWrapper(getContext());
        assertNotNull(mTts);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mTts.shutdown();
    }

    private TextToSpeech getTts() {
        return mTts.getTts();
    }

    public void testSynthesizeToFile() throws Exception {
        File sampleFile = new File(Environment.getExternalStorageDirectory(), SAMPLE_FILE_NAME);
        try {
            assertFalse(sampleFile.exists());

            int result = getTts().synthesizeToFile(UTTERANCE, createParams(), sampleFile.getPath());
            assertEquals("synthesizeToFile() failed", TextToSpeech.SUCCESS, result);

            assertTrue("synthesizeToFile() completion timeout", mTts.waitForComplete(UTTERANCE_ID));
            assertTrue("synthesizeToFile() didn't produce a file", sampleFile.exists());
            assertTrue("synthesizeToFile() produced a non-sound file",
                    TextToSpeechWrapper.isSoundFile(sampleFile.getPath()));
        } finally {
            sampleFile.delete();
        }
    }

    public void testSpeak() throws Exception {
        int result = getTts().speak(UTTERANCE, TextToSpeech.QUEUE_FLUSH, createParams());
        assertEquals("speak() failed", TextToSpeech.SUCCESS, result);
        assertTrue("speak() completion timeout", waitForUtterance());
    }

    public void testMediaPlayerFails() throws Exception {
        File sampleFile = new File(Environment.getExternalStorageDirectory(), "notsound.wav");
        try {
            assertFalse(TextToSpeechWrapper.isSoundFile(sampleFile.getPath()));
            FileOutputStream out = new FileOutputStream(sampleFile);
            out.write(new byte[] { 0x01, 0x02 });
            out.close();
            assertFalse(TextToSpeechWrapper.isSoundFile(sampleFile.getPath()));
        } finally {
            sampleFile.delete();
        }
    }

    private HashMap<String, String> createParams() {
        HashMap<String, String> params = new HashMap<String,String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID);
        return params;
    }

    private boolean waitForUtterance() throws InterruptedException {
        return mTts.waitForComplete(UTTERANCE_ID);
    }

}
