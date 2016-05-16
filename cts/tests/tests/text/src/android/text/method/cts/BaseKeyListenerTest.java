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

import android.os.SystemClock;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.BaseKeyListener;
import android.text.method.cts.KeyListenerTestCase;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.TextView.BufferType;

/**
 * Test {@link android.text.method.BaseKeyListener}.
 */
public class BaseKeyListenerTest extends KeyListenerTestCase {
    private static final CharSequence TEST_STRING = "123456";

    public void testBackspace() {
        final Editable content = Editable.Factory.getInstance().newEditable(TEST_STRING);
        final MockBaseKeyListener mockBaseKeyListener = new MockBaseKeyListener();

        // Nothing to delete when the cursor is at the beginning.
        final KeyEvent delKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL);
        prepTextViewSync(content, mockBaseKeyListener, false, 0, 0);
        mockBaseKeyListener.backspace(mTextView, content, KeyEvent.KEYCODE_DEL, delKeyEvent);
        assertEquals("123456", content.toString());

        // Delete the first three letters using a selection.
        prepTextViewSync(content, mockBaseKeyListener, false, 0, 3);
        mockBaseKeyListener.backspace(mTextView, content, KeyEvent.KEYCODE_DEL, delKeyEvent);
        assertEquals("456", content.toString());

        // Delete the entire line with ALT + DEL
        final KeyEvent altDelKeyEvent = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DEL, 0, KeyEvent.META_ALT_ON);
        prepTextViewSync(content, mockBaseKeyListener, false, 0, 0);
        mockBaseKeyListener.backspace(mTextView, content, KeyEvent.KEYCODE_DEL, altDelKeyEvent);
        assertEquals("", content.toString());
    }

    public void testBackspace_withSendKeys() {
        final MockBaseKeyListener mockBaseKeyListener = new MockBaseKeyListener();

        // Delete the first character '1'
        prepTextViewSync(TEST_STRING, mockBaseKeyListener, true, 1, 1);
        sendKeys(KeyEvent.KEYCODE_DEL);
        assertEquals("23456", mTextView.getText().toString());

        // Delete character '2' and '3'
        prepTextViewSync(TEST_STRING, mockBaseKeyListener, true, 1, 3);
        sendKeys(KeyEvent.KEYCODE_DEL);
        assertEquals("1456", mTextView.getText().toString());

        // Delete everything on the line the cursor is on.
        prepTextViewSync(TEST_STRING, mockBaseKeyListener, true, 0, 0);
        sendAltDelete();
        assertEquals("", mTextView.getText().toString());

        // ALT+DEL deletes the selection only.
        prepTextViewSync(TEST_STRING, mockBaseKeyListener, true, 2, 4);
        sendAltDelete();
        assertEquals("1256", mTextView.getText().toString());

        // DEL key does not take effect when TextView does not have BaseKeyListener.
        prepTextViewSync(TEST_STRING, null, true, 1, 1);
        sendKeys(KeyEvent.KEYCODE_DEL);
        assertEquals(TEST_STRING, mTextView.getText().toString());
    }

    /*
     * Check point:
     * 1. Press 0 key, the content of TextView does not changed.
     * 2. Set a selection and press DEL key, the selection is deleted.
     * 3. ACTION_MULTIPLE KEYCODE_UNKNOWN by inserting the event's text into the content.
     */
    public void testPressKey() {
        final MockBaseKeyListener mockBaseKeyListener = new MockBaseKeyListener();

        // press '0' key.
        prepTextViewSync(TEST_STRING, mockBaseKeyListener, true, 0, 0);
        sendKeys(KeyEvent.KEYCODE_0);
        assertEquals("123456", mTextView.getText().toString());

        // delete character '2'
        prepTextViewSync(mTextView.getText(), mockBaseKeyListener, true, 1, 2);
        sendKeys(KeyEvent.KEYCODE_DEL);
        assertEquals("13456", mTextView.getText().toString());

        // test ACTION_MULTIPLE KEYCODE_UNKNOWN key event.
        KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(), "abcd",
                KeyCharacterMap.BUILT_IN_KEYBOARD, 0);
        prepTextViewSync(mTextView.getText(), mockBaseKeyListener, true, 2, 2);
        mInstrumentation.sendKeySync(event);
        mInstrumentation.waitForIdleSync();
        // the text of TextView is never changed, onKeyOther never works.
//        assertEquals("13abcd456", mTextView.getText().toString());
    }

    /**
     * Prepares mTextView state for tests by synchronously setting the content and key listener, on
     * the UI thread.
     */
    private void prepTextViewSync(final CharSequence content, final BaseKeyListener keyListener,
            final boolean selectInTextView, final int selectionStart, final int selectionEnd) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setText(content, BufferType.EDITABLE);
                mTextView.setKeyListener(keyListener);
                Selection.setSelection(
                        (Spannable) (selectInTextView ? mTextView.getText() : content),
                        selectionStart, selectionEnd);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertTrue(mTextView.hasWindowFocus());
    }

    /**
     * Sends alt-delete key combo via {@link #sendKeys(int... keys)}.
     */
    private void sendAltDelete() {
        mInstrumentation.sendKeySync(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ALT_LEFT));
        mInstrumentation.sendKeySync(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_DEL, 0, KeyEvent.META_ALT_ON));
        mInstrumentation.sendKeySync(new KeyEvent(0, 0, KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_DEL, 0, KeyEvent.META_ALT_ON));
        mInstrumentation.sendKeySync(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ALT_LEFT));
    }

    /**
     * A mocked {@link android.text.method.BaseKeyListener} for testing purposes.
     */
    private class MockBaseKeyListener extends BaseKeyListener {
        public int getInputType() {
            return InputType.TYPE_CLASS_DATETIME
                    | InputType.TYPE_DATETIME_VARIATION_DATE;
        }
    }
}
