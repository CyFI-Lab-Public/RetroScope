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
package android.view.inputmethod.cts;

import com.android.cts.stub.R;

import android.app.Instrumentation;
import android.content.Context;
import android.cts.util.PollingCheck;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.test.ActivityInstrumentationTestCase2;
import android.view.KeyEvent;
import android.view.Window;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.List;

public class InputMethodManagerTest
                  extends ActivityInstrumentationTestCase2<InputMethodStubActivity> {

    public InputMethodManagerTest() {
        super("com.android.cts.stub", InputMethodStubActivity.class);
    }

    private InputMethodStubActivity mActivity;
    private Instrumentation mInstrumentation;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mInstrumentation = getInstrumentation();
    }

    @Override
    protected void tearDown() throws Exception {
        // Close soft input just in case.
        sendKeys(KeyEvent.KEYCODE_BACK);
        super.tearDown();
    }

    public void testInputMethodManager() throws Throwable {
        Window window = mActivity.getWindow();
        final EditText view = (EditText) window.findViewById(R.id.entry);

        new PollingCheck(1000) {
            @Override
            protected boolean check() {
                return view.hasWindowFocus();
            }
        }.run();

        runTestOnUiThread(new Runnable() {
           @Override
            public void run() {
               view.requestFocus();
            }
        });
        getInstrumentation().waitForIdleSync();
        assertTrue(view.isFocused());

        BaseInputConnection connection = new BaseInputConnection(view, false);
        Context context = mInstrumentation.getTargetContext();
        final InputMethodManager imManager = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        new PollingCheck() {
            @Override
            protected boolean check() {
                return imManager.isActive();
            }
        }.run();

        assertTrue(imManager.isAcceptingText());
        assertTrue(imManager.isActive(view));

        connection.reportFullscreenMode(false);
        assertFalse(imManager.isFullscreenMode());
        connection.reportFullscreenMode(true);
        assertTrue(imManager.isFullscreenMode());

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                IBinder token = view.getWindowToken();

                // Show and hide input method.
                assertTrue(imManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT));
                assertTrue(imManager.hideSoftInputFromWindow(token, 0));

                Handler handler = new Handler();
                ResultReceiver receiver = new ResultReceiver(handler);
                assertTrue(imManager.showSoftInput(view, 0, receiver));
                receiver = new ResultReceiver(handler);
                assertTrue(imManager.hideSoftInputFromWindow(token, 0, receiver));

                imManager.showSoftInputFromInputMethod(token, InputMethodManager.SHOW_FORCED);
                imManager.hideSoftInputFromInputMethod(token, InputMethodManager.HIDE_NOT_ALWAYS);

                // status: hide to show to hide
                imManager.toggleSoftInputFromWindow(token, 0, InputMethodManager.HIDE_NOT_ALWAYS);
                imManager.toggleSoftInputFromWindow(token, 0, InputMethodManager.HIDE_NOT_ALWAYS);

                List<InputMethodInfo> enabledImList = imManager.getEnabledInputMethodList();
                if (enabledImList != null && enabledImList.size() > 0) {
                    imManager.setInputMethod(token, enabledImList.get(0).getId());
                    // cannot test whether setting was successful
                }

                List<InputMethodInfo> imList = imManager.getInputMethodList();
                if (imList != null && enabledImList != null) {
                    assertTrue(imList.size() >= enabledImList.size());
                }
            }
        });
        getInstrumentation().waitForIdleSync();
    }
}
