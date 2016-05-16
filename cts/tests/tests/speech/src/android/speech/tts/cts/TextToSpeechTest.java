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

import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.test.AndroidTestCase;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Tests for {@link android.speech.tts.TextToSpeech}
 */
public class TextToSpeechTest extends AndroidTestCase {

    private static final String UTTERANCE_ID = "utterance";
    private static final String SAMPLE_TEXT = "This is a sample text to speech string";
    private static final String SAMPLE_FILE_NAME = "mytts.wav";

    private TextToSpeechWrapper mTts;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTts = TextToSpeechWrapper.createTextToSpeechWrapper(getContext());
        assertNotNull(mTts);
        assertTrue(checkAndSetLanguageAvailable());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mTts.shutdown();
    }

    private TextToSpeech getTts() {
        return mTts.getTts();
    }

    /**
     * Ensures at least one language is available for tts
     */
    private boolean checkAndSetLanguageAvailable() {
        // checks if at least one language is available in Tts
        final Locale defaultLocale = Locale.getDefault();
        // If the language for the default locale is available, then
        // use that.
        int defaultAvailability = getTts().isLanguageAvailable(defaultLocale);

        if (defaultAvailability == TextToSpeech.LANG_AVAILABLE ||
            defaultAvailability == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
            defaultAvailability == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
            getTts().setLanguage(defaultLocale);
            return true;
        }

        for (Locale locale : Locale.getAvailableLocales()) {
            int availability = getTts().isLanguageAvailable(locale);
            if (availability == TextToSpeech.LANG_AVAILABLE ||
                availability == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                availability == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
                getTts().setLanguage(locale);
                return true;
            }
        }
        return false;
    }

    public void testSynthesizeToFile() throws Exception {
        File sampleFile = new File(Environment.getExternalStorageDirectory(), SAMPLE_FILE_NAME);
        try {
            assertFalse(sampleFile.exists());

            int result = getTts().synthesizeToFile(SAMPLE_TEXT, createParams(),
                    sampleFile.getPath());
            assertEquals("synthesizeToFile() failed", TextToSpeech.SUCCESS, result);

            assertTrue("synthesizeToFile() completion timeout", waitForUtterance());
            assertTrue("synthesizeToFile() didn't produce a file", sampleFile.exists());
            assertTrue("synthesizeToFile() produced a non-sound file",
                    TextToSpeechWrapper.isSoundFile(sampleFile.getPath()));
        } finally {
            sampleFile.delete();
        }
    }

    public void testSpeak() throws Exception {
        int result = getTts().speak(SAMPLE_TEXT, TextToSpeech.QUEUE_FLUSH, createParams());
        assertEquals("speak() failed", TextToSpeech.SUCCESS, result);
        assertTrue("speak() completion timeout", waitForUtterance());
    }

    public void testGetEnginesIncludesDefault() throws Exception {
        List<TextToSpeech.EngineInfo> engines = getTts().getEngines();
        assertNotNull("getEngines() returned null", engines);
        assertContainsEngine(getTts().getDefaultEngine(), engines);
    }

    public void testGetEnginesIncludesMock() throws Exception {
        List<TextToSpeech.EngineInfo> engines = getTts().getEngines();
        assertNotNull("getEngines() returned null", engines);
        assertContainsEngine(TextToSpeechWrapper.MOCK_TTS_ENGINE, engines);
    }

    private void assertContainsEngine(String engine, List<TextToSpeech.EngineInfo> engines) {
        for (TextToSpeech.EngineInfo engineInfo : engines) {
            if (engineInfo.name.equals(engine)) {
                return;
            }
        }
        fail("Engine " + engine + " not found");
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
