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


import android.cts.util.PollingCheck;
import android.graphics.Rect;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.test.ActivityInstrumentationTestCase2;
import android.text.Editable;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyCharacterMap;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

/**
 * Test {@link PasswordTransformationMethod}.
 */
public class PasswordTransformationMethodTest extends
        ActivityInstrumentationTestCase2<StubActivity> {
    private static final int EDIT_TXT_ID = 1;

    /** original text */
    private static final String TEST_CONTENT = "test content";

    /** text after transformation: ************(12 dots) */
    private static final String TEST_CONTENT_TRANSFORMED =
        "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022";

    private int mPasswordPrefBackUp;

    private boolean isPasswordPrefSaved;

    private StubActivity mActivity;

    private MockPasswordTransformationMethod mMethod;

    private EditText mEditText;

    private CharSequence mTransformedText;

    public PasswordTransformationMethodTest() {
        super("com.android.cts.stub", StubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        new PollingCheck(1000) {
            @Override
            protected boolean check() {
                return mActivity.hasWindowFocus();
            }
        }.run();
        mMethod = new MockPasswordTransformationMethod();
        try {
            runTestOnUiThread(new Runnable() {
                public void run() {
                    EditText editText = new EditText(mActivity);
                    editText.setId(EDIT_TXT_ID);
                    editText.setTransformationMethod(mMethod);
                    Button button = new Button(mActivity);
                    LinearLayout layout = new LinearLayout(mActivity);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.addView(editText, new LayoutParams(LayoutParams.MATCH_PARENT,
                            LayoutParams.WRAP_CONTENT));
                    layout.addView(button, new LayoutParams(LayoutParams.MATCH_PARENT,
                            LayoutParams.WRAP_CONTENT));
                    mActivity.setContentView(layout);
                    editText.requestFocus();
                }
            });
        } catch (Throwable e) {
            fail("Exception thrown is UI thread:" + e.getMessage());
        }
        getInstrumentation().waitForIdleSync();

        mEditText = (EditText) getActivity().findViewById(EDIT_TXT_ID);
        assertTrue(mEditText.isFocused());

        savePasswordPref();
        switchShowPassword(true);
    }

    @Override
    protected void tearDown() throws Exception {
        resumePasswordPref();
        super.tearDown();
    }

    public void testConstructor() {
        new PasswordTransformationMethod();
    }

    public void testTextChangedCallBacks() throws Throwable {
        runTestOnUiThread(new Runnable() {
            public void run() {
                mTransformedText = mMethod.getTransformation(mEditText.getText(), mEditText);
            }
        });

        mMethod.reset();
        // 12-key support
        KeyCharacterMap keymap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        if (keymap.getKeyboardType() == KeyCharacterMap.NUMERIC) {
            // "HELLO" in case of 12-key(NUMERIC) keyboard
            sendKeys("6*4 6*3 7*5 DPAD_RIGHT 7*5 7*6 DPAD_RIGHT");
        }
        else {
            sendKeys("H E 2*L O");
        }
        assertTrue(mMethod.hasCalledBeforeTextChanged());
        assertTrue(mMethod.hasCalledOnTextChanged());
        assertTrue(mMethod.hasCalledAfterTextChanged());

        mMethod.reset();

        runTestOnUiThread(new Runnable() {
            public void run() {
                mEditText.append(" ");
            }
        });

        // the appended string will not get transformed immediately
        // "***** "
        assertEquals("\u2022\u2022\u2022\u2022\u2022 ", mTransformedText.toString());
        assertTrue(mMethod.hasCalledBeforeTextChanged());
        assertTrue(mMethod.hasCalledOnTextChanged());
        assertTrue(mMethod.hasCalledAfterTextChanged());

        // it will get transformed after a while
        new PollingCheck() {
            @Override
            protected boolean check() {
                // "******"
                return mTransformedText.toString()
                        .equals("\u2022\u2022\u2022\u2022\u2022\u2022");
            }
        }.run();
    }

    public void testGetTransformation() {
        PasswordTransformationMethod method = new PasswordTransformationMethod();

        assertEquals(TEST_CONTENT_TRANSFORMED,
                method.getTransformation(TEST_CONTENT, null).toString());

        CharSequence transformed = method.getTransformation(null, mEditText);
        assertNotNull(transformed);
        try {
            transformed.toString();
            fail("Should throw NullPointerException if the source is null.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testGetInstance() {
        PasswordTransformationMethod method0 = PasswordTransformationMethod.getInstance();
        assertNotNull(method0);

        PasswordTransformationMethod method1 = PasswordTransformationMethod.getInstance();
        assertNotNull(method1);
        assertSame(method0, method1);
    }

    public void testOnFocusChanged() {
        // lose focus
        mMethod.reset();
        assertTrue(mEditText.isFocused());
        sendKeys("DPAD_DOWN");
        assertFalse(mEditText.isFocused());
        assertTrue(mMethod.hasCalledOnFocusChanged());

        // gain focus
        mMethod.reset();
        assertFalse(mEditText.isFocused());
        sendKeys("DPAD_UP");
        assertTrue(mEditText.isFocused());
        assertTrue(mMethod.hasCalledOnFocusChanged());
    }

    private void savePasswordPref() {
        try {
            mPasswordPrefBackUp = System.getInt(mActivity.getContentResolver(),
                    System.TEXT_SHOW_PASSWORD);
            isPasswordPrefSaved = true;
        } catch (SettingNotFoundException e) {
            isPasswordPrefSaved = false;
        }
    }

    private void resumePasswordPref() {
        if (isPasswordPrefSaved) {
            System.putInt(mActivity.getContentResolver(), System.TEXT_SHOW_PASSWORD,
                    mPasswordPrefBackUp);
        }
    }

    private void switchShowPassword(boolean on) {
        System.putInt(mActivity.getContentResolver(), System.TEXT_SHOW_PASSWORD,
                on ? 1 : 0);
    }

    private static class MockPasswordTransformationMethod extends PasswordTransformationMethod {
        private boolean mHasCalledBeforeTextChanged;

        private boolean mHasCalledOnTextChanged;

        private boolean mHasCalledAfterTextChanged;

        private boolean mHasCalledOnFocusChanged;

        @Override
        public void afterTextChanged(Editable s) {
            super.afterTextChanged(s);
            mHasCalledAfterTextChanged = true;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            super.beforeTextChanged(s, start, count, after);
            mHasCalledBeforeTextChanged = true;
        }

        @Override
        public void onFocusChanged(View view, CharSequence sourceText, boolean focused,
                int direction, Rect previouslyFocusedRect) {
            super.onFocusChanged(view, sourceText, focused, direction, previouslyFocusedRect);
            mHasCalledOnFocusChanged = true;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            super.onTextChanged(s, start, before, count);
            mHasCalledOnTextChanged = true;
        }

        public boolean hasCalledBeforeTextChanged() {
            return mHasCalledBeforeTextChanged;
        }

        public boolean hasCalledOnTextChanged() {
            return mHasCalledOnTextChanged;
        }

        public boolean hasCalledAfterTextChanged() {
            return mHasCalledAfterTextChanged;
        }

        public boolean hasCalledOnFocusChanged() {
            return mHasCalledOnFocusChanged;
        }

        public void reset() {
            mHasCalledBeforeTextChanged = false;
            mHasCalledOnTextChanged = false;
            mHasCalledAfterTextChanged = false;
            mHasCalledOnFocusChanged = false;
        }
    }
}
