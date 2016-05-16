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
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.cts.KeyListenerTestCase;
import android.text.method.QwertyKeyListener;
import android.text.method.TextKeyListener;
import android.text.method.TextKeyListener.Capitalize;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.TextView.BufferType;

public class QwertyKeyListenerTest extends KeyListenerTestCase {
    public void testConstructor() {
        new QwertyKeyListener(Capitalize.NONE, false);

        new QwertyKeyListener(Capitalize.WORDS, true);

        new QwertyKeyListener(null, true);
    }

    public void testOnKeyDown_capitalizeNone() {
        QwertyKeyListener keyListener = QwertyKeyListener.getInstance(false, Capitalize.NONE);

        prepareEmptyTextView();

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_H);
        assertEquals("h", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_E);
        assertEquals("he", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_L);
        assertEquals("hel", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_L);
        assertEquals("hell", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_O);
        assertEquals("hello", mTextView.getText().toString());
    }

    public void testOnKeyDown_capitalizeCharacters() {
        QwertyKeyListener keyListener = QwertyKeyListener.getInstance(false,
                Capitalize.CHARACTERS);

        prepareEmptyTextView();

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_H);
        assertEquals("H", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_E);
        assertEquals("HE", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_L);
        assertEquals("HEL", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_L);
        assertEquals("HELL", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_O);
        assertEquals("HELLO", mTextView.getText().toString());
    }

    public void testOnKeyDown_capitalizeSentences() {
        QwertyKeyListener keyListener = QwertyKeyListener.getInstance(false,
                Capitalize.SENTENCES);

        prepareEmptyTextView();

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_H);
        assertEquals("H", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_I);
        assertEquals("Hi", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_PERIOD);
        assertEquals("Hi.", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_SPACE);
        assertEquals("Hi. ", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_B);
        assertEquals("Hi. B", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_Y);
        assertEquals("Hi. By", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_E);
        assertEquals("Hi. Bye", mTextView.getText().toString());
    }

    public void testOnKeyDown_capitalizeWords() {
        QwertyKeyListener keyListener = QwertyKeyListener.getInstance(false,
                Capitalize.WORDS);

        prepareEmptyTextView();

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_H);
        assertEquals("H", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_I);
        assertEquals("Hi", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_SPACE);
        assertEquals("Hi ", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_B);
        assertEquals("Hi B", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_Y);
        assertEquals("Hi By", mTextView.getText().toString());

        callOnKeyDown(keyListener, KeyEvent.KEYCODE_E);
        assertEquals("Hi Bye", mTextView.getText().toString());
    }

    private void prepareEmptyTextView() {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setText("", BufferType.EDITABLE);
                Selection.setSelection(mTextView.getEditableText(), 0, 0);
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals("", mTextView.getText().toString());
    }

    private void callOnKeyDown(final QwertyKeyListener keyListener, final int keyCode) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                keyListener.onKeyDown(mTextView, mTextView.getEditableText(), keyCode,
                        new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    public void testGetInstance() {
        QwertyKeyListener listener1 = QwertyKeyListener.getInstance(true, Capitalize.WORDS);
        QwertyKeyListener listener2 = QwertyKeyListener.getInstance(true, Capitalize.WORDS);
        QwertyKeyListener listener3 = QwertyKeyListener.getInstance(false, Capitalize.WORDS);
        QwertyKeyListener listener4 = QwertyKeyListener.getInstance(true, Capitalize.SENTENCES);

        assertNotNull(listener1);
        assertNotNull(listener2);
        assertSame(listener1, listener2);

        assertNotSame(listener1, listener3);
        assertNotSame(listener1, listener4);
        assertNotSame(listener4, listener3);
    }

    public void testMarkAsReplaced() {
        SpannableStringBuilder content = new SpannableStringBuilder("123456");

        Object[] repl = content.getSpans(0, content.length(), Object.class);
        assertEquals(0, repl.length);

        QwertyKeyListener.markAsReplaced(content, 0, content.length(), "abcd");
        repl = content.getSpans(0, content.length(), Object.class);
        assertEquals(1, repl.length);
        assertEquals(0, content.getSpanStart(repl[0]));
        assertEquals(content.length(), content.getSpanEnd(repl[0]));
        assertEquals(Spannable.SPAN_EXCLUSIVE_EXCLUSIVE, content.getSpanFlags(repl[0]));

        QwertyKeyListener.markAsReplaced(content, 1, 2, "abcd");
        repl = content.getSpans(0, content.length(), Object.class);
        assertEquals(1, repl.length);
        assertEquals(1, content.getSpanStart(repl[0]));
        assertEquals(2, content.getSpanEnd(repl[0]));
        assertEquals(Spannable.SPAN_EXCLUSIVE_EXCLUSIVE, content.getSpanFlags(repl[0]));

        try {
            QwertyKeyListener.markAsReplaced(null, 1, 2, "abcd");
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }

        try {
            QwertyKeyListener.markAsReplaced(content, 1, 2, null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    public void testGetInputType() {
        QwertyKeyListener listener = QwertyKeyListener.getInstance(false, Capitalize.NONE);
        int expected = InputType.TYPE_CLASS_TEXT;
        assertEquals(expected, listener.getInputType());

        listener = QwertyKeyListener.getInstance(false, Capitalize.CHARACTERS);
        expected = InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
        assertEquals(expected, listener.getInputType());
    }
}
