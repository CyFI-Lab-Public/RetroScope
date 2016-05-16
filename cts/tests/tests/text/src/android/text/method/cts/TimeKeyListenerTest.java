/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.text.method.cts;

import android.text.InputType;
import android.text.method.cts.KeyListenerTestCase;
import android.text.method.TimeKeyListener;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

public class TimeKeyListenerTest extends KeyListenerTestCase {
    public void testConstructor() {
        new TimeKeyListener();
    }

    public void testGetInstance() {
        TimeKeyListener listener1 = TimeKeyListener.getInstance();
        TimeKeyListener listener2 = TimeKeyListener.getInstance();

        assertNotNull(listener1);
        assertNotNull(listener2);
        assertSame(listener1, listener2);
    }

    public void testGetAcceptedChars() {
        MockTimeKeyListener mockTimeKeyListener = new MockTimeKeyListener();
        TextMethodUtils.assertEquals(TimeKeyListener.CHARACTERS,
                mockTimeKeyListener.getAcceptedChars());
    }

    public void testGetInputType() {
        TimeKeyListener listener = TimeKeyListener.getInstance();
        int expected = InputType.TYPE_CLASS_DATETIME
                | InputType.TYPE_DATETIME_VARIATION_TIME;
        assertEquals(expected, listener.getInputType());
    }

    /*
     * Scenario description:
     * 1. Press '1' key and check if the content of TextView becomes "1"
     * 2. Press '2' key and check if the content of TextView becomes "12"
     * 3. Press 'a' key if it is producible
     * 4. Press 'p' key if it is producible
     * 5. Press 'm' key if it is producible
     * 6. Press an unaccepted key if it exists and this key could not be entered.
     * 7. Remove TimeKeyListener, '1' key will not be accepted.
     */
    public void testTimeKeyListener() {
        final TimeKeyListener timeKeyListener = TimeKeyListener.getInstance();
        String expectedText = "";

        setKeyListenerSync(timeKeyListener);
        assertEquals(expectedText, mTextView.getText().toString());

        // press '1' key.
        mInstrumentation.sendStringSync("1");
        expectedText += "1";
        assertEquals(expectedText, mTextView.getText().toString());

        // press '2' key.
        mInstrumentation.sendStringSync("2");
        expectedText += "2";
        assertEquals("12", mTextView.getText().toString());

        // press 'a' key if producible
        KeyCharacterMap kcm = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        if ('a' == kcm.getMatch(KeyEvent.KEYCODE_A, TimeKeyListener.CHARACTERS)) {
            expectedText += "a";
            mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_A);
            assertEquals(expectedText, mTextView.getText().toString());
        }

        // press 'p' key if producible
        if ('p' == kcm.getMatch(KeyEvent.KEYCODE_P, TimeKeyListener.CHARACTERS)) {
            expectedText += "p";
            mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_P);
            assertEquals(expectedText, mTextView.getText().toString());
        }

        // press 'm' key if producible
        if ('m' == kcm.getMatch(KeyEvent.KEYCODE_M, TimeKeyListener.CHARACTERS)) {
            expectedText += "m";
            mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_M);
            assertEquals(expectedText, mTextView.getText().toString());
        }

        // press an unaccepted key if it exists.
        int keyCode = TextMethodUtils.getUnacceptedKeyCode(TimeKeyListener.CHARACTERS);
        if (-1 != keyCode) {
            sendKeys(keyCode);
            assertEquals(expectedText, mTextView.getText().toString());
        }

        setKeyListenerSync(null);

        // press '1' key.
        mInstrumentation.sendStringSync("1");
        assertEquals(expectedText, mTextView.getText().toString());
    }

    /**
     * A mocked {@link android.text.method.TimeKeyListener} for testing purposes.
     *
     * Allows {@link TimeKeyListenerTest} to call
     * {@link android.text.method.TimeKeyListener#getAcceptedChars()}.
     */
    private class MockTimeKeyListener extends TimeKeyListener {
        @Override
        protected char[] getAcceptedChars() {
            return super.getAcceptedChars();
        }
    }
}
