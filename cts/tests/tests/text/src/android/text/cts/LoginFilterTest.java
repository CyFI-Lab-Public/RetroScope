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

package android.text.cts;

import junit.framework.TestCase;
import android.text.LoginFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;

public class LoginFilterTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testFilter() {
        CharSequence result;
        MockLoginFilter loginFilter = new MockLoginFilter();
        Spanned dest1 = new SpannedString("dest_without_invalid_char");
        Spanned dest2 = new SpannedString("&*dest_with_invalid_char#$");
        String source1 = "source_without_invalid_char";
        String source2 = "+=source_with_invalid_char%!";
        Spanned spannedSource = new SpannedString("&*spanned_source_with_invalid_char#$");

        assertFalse(loginFilter.isStarted());
        assertFalse(loginFilter.isStopped());
        assertEquals(0, loginFilter.getInvalidCharacterCount());

        assertNull(loginFilter.filter(source1, 0, source1.length(), dest1, 0, dest1.length()));
        assertTrue(loginFilter.isStarted());
        assertTrue(loginFilter.isStopped());
        assertEquals(0, loginFilter.getInvalidCharacterCount());

        loginFilter.reset();
        assertNull(loginFilter.filter(source1, 0, source1.length(), dest2, 5, 6));
        assertTrue(loginFilter.isStarted());
        assertTrue(loginFilter.isStopped());
        assertEquals(4, loginFilter.getInvalidCharacterCount());

        loginFilter = new MockLoginFilter(true);
        assertNull(loginFilter.filter(source2, 0, source2.length(),
                dest1, 0, dest1.length()));
        assertTrue(loginFilter.isStarted());
        assertTrue(loginFilter.isStopped());
        assertEquals(3, loginFilter.getInvalidCharacterCount());

        loginFilter.reset();
        assertNull(loginFilter.filter(spannedSource, 0, spannedSource.length(),
                dest1, 0, dest1.length()));
        assertTrue(loginFilter.isStarted());
        assertTrue(loginFilter.isStopped());
        assertEquals(4, loginFilter.getInvalidCharacterCount());

        loginFilter = new MockLoginFilter(false);
        result = loginFilter.filter(source2, 0, source2.length(), dest1, 0, dest1.length());
        assertFalse(result instanceof SpannableString);
        assertEquals("+source_with_invalid_char", result.toString());
        assertTrue(loginFilter.isStarted());
        assertTrue(loginFilter.isStopped());
        assertEquals(3, loginFilter.getInvalidCharacterCount());

        loginFilter.reset();
        result = loginFilter.filter(spannedSource, 0, spannedSource.length(),
                dest1, 0, dest1.length());
        assertEquals("spanned_source_with_invalid_char", result.toString());
        assertTrue(loginFilter.isStarted());
        assertTrue(loginFilter.isStopped());
        assertEquals(4, loginFilter.getInvalidCharacterCount());

        try {
            loginFilter.filter(null, 0, source1.length(), dest1, 0, dest1.length());
            fail("should throw NullPointerException when source is null");
        } catch (NullPointerException e) {
        }

        try {
            // start and end are out of bound.
            loginFilter.filter(source1, -1, source1.length() + 1, dest1, 0, dest1.length());
            fail("should throw StringIndexOutOfBoundsException" +
                    " when start and end are out of bound");
        } catch (StringIndexOutOfBoundsException e) {
        }

        // start is larger than end.
        assertNull("should return null when start is larger than end",
                loginFilter.filter(source1, source1.length(), 0, dest1, 0, dest1.length()));

        try {
            loginFilter.filter(source1, 0, source1.length(), null, 2, dest1.length());
            fail("should throw NullPointerException when dest is null");
        } catch (NullPointerException e) {
        }

        // dstart and dend are out of bound.
        loginFilter.filter(source1, 0, source1.length(), dest1, -1, dest1.length() + 1);

        // dstart is larger than dend.
        loginFilter.filter(source1, 0, source1.length(), dest1, dest1.length(),  0);
    }

    // This method does nothing. we only test onInvalidCharacter function here,
    // the callback should be tested in testFilter()
    public void testOnInvalidCharacter() {
        LoginFilter loginFilter = new MockLoginFilter();
        loginFilter.onInvalidCharacter('a');
    }

    // This method does nothing. we only test onStop function here,
    // the callback should be tested in testFilter()
    public void testOnStop() {
        LoginFilter loginFilter = new MockLoginFilter();
        loginFilter.onStop();
    }

    // This method does nothing. we only test onStart function here,
    // the callback should be tested in testFilter()
    public void testOnStart() {
        LoginFilter loginFilter = new LoginFilter.UsernameFilterGeneric();
        loginFilter.onStart();
    }

    private final class MockLoginFilter extends LoginFilter.UsernameFilterGeneric {
        private int mInvalidCharacterCount;
        private boolean mIsStarted = false;
        private boolean mIsStopped = false;

        public MockLoginFilter() {
            super();
        }

        public MockLoginFilter(boolean appendInvalid) {
            super(appendInvalid);
        }

        @Override
        public void onInvalidCharacter(char c) {
            mInvalidCharacterCount++;
            super.onInvalidCharacter(c);
        }

        public int getInvalidCharacterCount() {
            return mInvalidCharacterCount;
        }

        @Override
        public void onStart() {
            mIsStarted = true;
            super.onStart();
        }

        public boolean isStarted() {
            return mIsStarted;
        }

        @Override
        public void onStop() {
            mIsStopped = true;
            super.onStop();
        }

        public boolean isStopped() {
            return mIsStopped;
        }

        public void reset() {
            mInvalidCharacterCount = 0;
            mIsStarted = false;
            mIsStopped = false;
        }
    }
}
