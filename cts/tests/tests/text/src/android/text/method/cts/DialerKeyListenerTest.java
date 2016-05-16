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
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.cts.KeyListenerTestCase;
import android.text.method.DialerKeyListener;
import android.view.KeyEvent;

/**
 * Test {@link android.text.method.DialerKeyListener}.
 */
public class DialerKeyListenerTest extends KeyListenerTestCase {
    public void testConstructor() {
        new DialerKeyListener();
    }

    public void testLookup() {
        MockDialerKeyListener mockDialerKeyListener = new MockDialerKeyListener();
        final int[] events = { KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_A };
        SpannableString span = new SpannableString(""); // no meta spans
        for (int event: events) {
            KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, event);
            int keyChar = keyEvent.getNumber();
            if (keyChar != 0) {
                assertEquals(keyChar, mockDialerKeyListener.lookup(keyEvent, span));
            } else {
                // cannot make any assumptions how the key code gets translated
            }
        }

        try {
            mockDialerKeyListener.lookup(null, span);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    public void testGetInstance() {
        assertNotNull(DialerKeyListener.getInstance());

        DialerKeyListener listener1 = DialerKeyListener.getInstance();
        DialerKeyListener listener2 = DialerKeyListener.getInstance();

        assertTrue(listener1 instanceof DialerKeyListener);
        assertTrue(listener2 instanceof DialerKeyListener);
        assertSame(listener1, listener2);
    }

    public void testGetAcceptedChars() {
        MockDialerKeyListener mockDialerKeyListener = new MockDialerKeyListener();

        TextMethodUtils.assertEquals(DialerKeyListener.CHARACTERS,
                mockDialerKeyListener.getAcceptedChars());
    }

    public void testGetInputType() {
        DialerKeyListener listener = DialerKeyListener.getInstance();

        assertEquals(InputType.TYPE_CLASS_PHONE, listener.getInputType());
    }

    /**
     * A mocked {@link android.text.method.DialerKeyListener} for testing purposes.
     *
     * Allows {@link DialerKeyListenerTest} to call
     * {@link android.text.method.DialerKeyListener#getAcceptedChars()} and
     * {@link android.text.method.DialerKeyListener#lookup(KeyEvent, Spannable)}.
     */
    private class MockDialerKeyListener extends DialerKeyListener {
        @Override
        protected char[] getAcceptedChars() {
            return super.getAcceptedChars();
        }

        @Override
        protected int lookup(KeyEvent event, Spannable content) {
            return super.lookup(event, content);
        }
    }
}
