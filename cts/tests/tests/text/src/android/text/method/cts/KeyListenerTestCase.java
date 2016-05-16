/*
 * Copyright (C) 2013 The Android Open Source Project
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

import com.android.cts.stub.R;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.text.format.DateUtils;
import android.text.method.cts.KeyListenerStubActivity;
import android.text.method.KeyListener;
import android.widget.TextView;

/**
 * Base class for various KeyListener tests.
 * {@link BaseKeyListenerTest}
 * {@link DateKeyListenerTest}
 * {@link DateTimeKeyListenerTest}
 * {@link DigitsKeyListenerTest}
 * {@link MultiTapKeyListenerTest}
 * {@link NumberKeyListenerTest}
 * {@link QwertyKeyListenerTest}
 * {@link TextKeyKeyListenerTest}
 *
 * @see BaseKeyListenerTest
 * @see DateKeyListenerTest
 * @see DateTimeKeyListenerTest
 * @see DigitsKeyListenerTest
 * @see MultiTapKeyListenerTest
 * @see NumberKeyListenerTest
 * @see QwertyKeyListenerTest
 * @see TextKeyKeyListenerTest
 */
public abstract class KeyListenerTestCase extends
        ActivityInstrumentationTestCase2<KeyListenerStubActivity> {
    protected KeyListenerStubActivity mActivity;
    protected Instrumentation mInstrumentation;
    protected TextView mTextView;

    public KeyListenerTestCase() {
        super("com.android.cts.stub", KeyListenerStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
        mTextView = (TextView) mActivity.findViewById(R.id.keylistener_textview);

        assertTrue(mActivity.waitForWindowFocus(5 * DateUtils.SECOND_IN_MILLIS));
    }

    /**
     * Synchronously sets mTextView's key listener on the UI thread.
     */
    protected void setKeyListenerSync(final KeyListener keyListener) {
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setKeyListener(keyListener);
            }
        });
        mInstrumentation.waitForIdleSync();
    }
}
