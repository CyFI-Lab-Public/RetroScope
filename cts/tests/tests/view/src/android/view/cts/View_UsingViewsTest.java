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

package android.view.cts;

import com.android.cts.stub.R;


import android.app.Activity;
import android.app.Instrumentation;
import android.cts.util.PollingCheck;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.test.UiThreadTest;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class View_UsingViewsTest extends ActivityInstrumentationTestCase2<UsingViewsStubActivity> {
    /**
     * country of Argentina
     */
    private static final String ARGENTINA = "Argentina";

    /**
     * country of America
     */
    private static final String AMERICA = "America";

    /**
     * country of China
     */
    private static final String CHINA = "China";

    /**
     * the symbol of Argentina is football
     */
    private static final String ARGENTINA_SYMBOL = "football";

    /**
     * the symbol of America is basketball
     */
    private static final String AMERICA_SYMBOL = "basketball";

    /**
     * the symbol of China is table tennis
     */
    private static final String CHINA_SYMBOL = "table tennis";

    private Activity mActivity;
    private Instrumentation mInstrumentation;

    private EditText mEditText;
    private Button mButtonOk;
    private Button mButtonCancel;
    private TextView mSymbolTextView;
    private TextView mWarningTextView;

    public View_UsingViewsTest() {
        super("com.android.cts.stub", UsingViewsStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mActivity = getActivity();
        mInstrumentation = getInstrumentation();

        mEditText = (EditText) mActivity.findViewById(R.id.entry);
        mButtonOk = (Button) mActivity.findViewById(R.id.ok);
        mButtonCancel = (Button) mActivity.findViewById(R.id.cancel);
        mSymbolTextView = (TextView) mActivity.findViewById(R.id.symbolball);
        mWarningTextView = (TextView) mActivity.findViewById(R.id.warning);
    }

    @UiThreadTest
    public void testSetProperties() {
        /**
         * setClickable, setOnClickListener
         */
        mButtonOk.setClickable(true);
        assertTrue(mButtonOk.isClickable());

        MockOnClickOkListener okButtonListener = new MockOnClickOkListener();
        mButtonOk.setOnClickListener(okButtonListener);
        assertFalse(okButtonListener.hasOnClickCalled());

        mButtonOk.performClick();
        assertTrue(okButtonListener.hasOnClickCalled());

        mButtonCancel.setClickable(false);
        assertFalse(mButtonCancel.isClickable());

        MockOnClickCancelListener cancelButtonListener = new MockOnClickCancelListener();
        mButtonCancel.setOnClickListener(cancelButtonListener);
        assertFalse(cancelButtonListener.hasOnClickCalled());
        assertTrue(mButtonCancel.isClickable());

        mButtonCancel.performClick();
        assertTrue(cancelButtonListener.hasOnClickCalled());

        /**
         * setDrawingCacheEnabled, setDrawingCacheQuality, setDrawingCacheBackgroundColor,
         */
        mEditText.setDrawingCacheEnabled(true);
        assertTrue(mEditText.isDrawingCacheEnabled());

        // the default quality is auto
        assertEquals(View.DRAWING_CACHE_QUALITY_AUTO, mEditText.getDrawingCacheQuality());
        mEditText.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
        assertEquals(View.DRAWING_CACHE_QUALITY_LOW, mEditText.getDrawingCacheQuality());
        mEditText.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        assertEquals(View.DRAWING_CACHE_QUALITY_HIGH, mEditText.getDrawingCacheQuality());

        mEditText.setDrawingCacheBackgroundColor(Color.GREEN);
        assertEquals(Color.GREEN, mEditText.getDrawingCacheBackgroundColor());

        // create the cache
        Bitmap b = mEditText.getDrawingCache();
        assertNotNull(b);
        assertEquals(mEditText.getHeight(), b.getHeight());
        assertEquals(mEditText.getWidth(), b.getWidth());
        assertEquals(Color.GREEN, b.getPixel(0, 0));

        // setDrawingCacheEnabled to false
        mEditText.setDrawingCacheEnabled(false);
        assertFalse(mEditText.isDrawingCacheEnabled());

        mEditText.setDrawingCacheBackgroundColor(Color.YELLOW);
        assertEquals(Color.YELLOW, mEditText.getDrawingCacheBackgroundColor());

        // build drawable cache
        mEditText.buildDrawingCache();
        b = mEditText.getDrawingCache();
        assertNotNull(b);
        assertEquals(mEditText.getHeight(), b.getHeight());
        assertEquals(mEditText.getWidth(), b.getWidth());
        assertEquals(Color.YELLOW, b.getPixel(0, 0));
        mEditText.destroyDrawingCache();

        /**
         * setDuplicateParentStateEnabled
         */
        TextView v = new TextView(mActivity);
        v.setSingleLine(); // otherwise the multiline state interferes with theses tests
        v.setEnabled(false);
        v.setText("Test setDuplicateParentStateEnabled");

        v.setDuplicateParentStateEnabled(false);
        assertFalse(v.isDuplicateParentStateEnabled());

        RelativeLayout parent = (RelativeLayout) mEditText.getParent();
        parent.addView(v);

        assertFalse(parent.getDrawableState().length == v.getDrawableState().length);
        parent.removeView(v);

        v.setDuplicateParentStateEnabled(true);
        assertTrue(v.isDuplicateParentStateEnabled());

        parent.addView(v);
        v.refreshDrawableState();

        assertEquals(parent.getDrawableState().length, v.getDrawableState().length);
        assertEquals(parent.getDrawableState().toString(), v.getDrawableState().toString());
        parent.removeView(v);

        /**
         * setEnabled
         */
        mWarningTextView.setEnabled(false);
        assertFalse(mWarningTextView.isEnabled());

        mWarningTextView.setEnabled(true);
        assertTrue(mWarningTextView.isEnabled());

        /**
         * setFadingEdgeLength, setVerticalFadingEdgeEnabled and
         * setHorizontalFadingEdgeEnabled(boolean)
         */
        mWarningTextView.setVerticalFadingEdgeEnabled(true);
        assertTrue(mWarningTextView.isVerticalFadingEdgeEnabled());
        mWarningTextView.setFadingEdgeLength(10);

        mSymbolTextView.setHorizontalFadingEdgeEnabled(true);
        assertTrue(mSymbolTextView.isHorizontalFadingEdgeEnabled());
        mSymbolTextView.setFadingEdgeLength(100);

        /**
         * setFocusable and setFocusableInTouchMode
         */
        mButtonCancel.setFocusable(false);
        assertFalse(mButtonCancel.isFocusable());
        assertFalse(mButtonCancel.isFocusableInTouchMode());

        mButtonCancel.setFocusable(true);
        assertTrue(mButtonCancel.isFocusable());
        assertFalse(mButtonCancel.isFocusableInTouchMode());

        mButtonCancel.setFocusableInTouchMode(true);
        assertTrue(mButtonCancel.isFocusable());
        assertTrue(mButtonCancel.isFocusableInTouchMode());

        mButtonOk.setFocusable(false);
        assertFalse(mButtonOk.isFocusable());
        assertFalse(mButtonOk.isFocusableInTouchMode());

        mButtonOk.setFocusableInTouchMode(true);
        assertTrue(mButtonOk.isFocusable());
        assertTrue(mButtonOk.isFocusableInTouchMode());

        /**
         * setHorizontalScrollBarEnabled and setVerticalScrollBarEnabled
         */
        // both two bar is not drawn by default
        assertFalse(parent.isHorizontalScrollBarEnabled());
        assertFalse(parent.isVerticalScrollBarEnabled());

        parent.setHorizontalScrollBarEnabled(true);
        assertTrue(parent.isHorizontalScrollBarEnabled());

        parent.setVerticalScrollBarEnabled(true);
        assertTrue(parent.isVerticalScrollBarEnabled());

        /**
         * setId
         */
        assertEquals(View.NO_ID, parent.getId());
        assertEquals(R.id.entry, mEditText.getId());
        assertEquals(R.id.symbolball, mSymbolTextView.getId());

        mSymbolTextView.setId(0x5555);
        assertEquals(0x5555, mSymbolTextView.getId());
        TextView t = (TextView) parent.findViewById(0x5555);
        assertSame(mSymbolTextView, t);

        mSymbolTextView.setId(R.id.symbolball);
        assertEquals(R.id.symbolball, mSymbolTextView.getId());
    }

    @UiThreadTest
    public void testSetFocus() throws Throwable {
        boolean focusWasOnEditText = mEditText.hasFocus();

        MockOnFocusChangeListener editListener = new MockOnFocusChangeListener();
        MockOnFocusChangeListener okListener = new MockOnFocusChangeListener();
        MockOnFocusChangeListener cancelListener = new MockOnFocusChangeListener();
        MockOnFocusChangeListener symbolListener = new MockOnFocusChangeListener();
        MockOnFocusChangeListener warningListener = new MockOnFocusChangeListener();

        mEditText.setOnFocusChangeListener(editListener);
        mButtonOk.setOnFocusChangeListener(okListener);
        mButtonCancel.setOnFocusChangeListener(cancelListener);
        mSymbolTextView.setOnFocusChangeListener(symbolListener);
        mWarningTextView.setOnFocusChangeListener(warningListener);

        mSymbolTextView.setText(ARGENTINA_SYMBOL);
        mWarningTextView.setVisibility(View.VISIBLE);

        assertTrue(mEditText.requestFocus());
        assertTrue(mEditText.hasFocus());
        assertFalse(mButtonOk.hasFocus());
        assertFalse(mButtonCancel.hasFocus());
        assertFalse(mSymbolTextView.hasFocus());
        assertFalse(mWarningTextView.hasFocus());

        assertTrue(editListener.hasFocus() || focusWasOnEditText);
        assertFalse(okListener.hasFocus());
        assertFalse(cancelListener.hasFocus());
        assertFalse(symbolListener.hasFocus());
        assertFalse(warningListener.hasFocus());

        // set ok button to focus
        assertTrue(mButtonOk.requestFocus());
        assertTrue(mButtonOk.hasFocus());
        assertTrue(okListener.hasFocus());
        assertFalse(mEditText.hasFocus());
        assertFalse(editListener.hasFocus());

        // set cancel button to focus
        assertTrue(mButtonCancel.requestFocus());
        assertTrue(mButtonCancel.hasFocus());
        assertTrue(cancelListener.hasFocus());
        assertFalse(mButtonOk.hasFocus());
        assertFalse(okListener.hasFocus());

        // set symbol text to focus
        mSymbolTextView.setFocusable(true);
        assertTrue(mSymbolTextView.requestFocus());
        assertTrue(mSymbolTextView.hasFocus());
        assertTrue(symbolListener.hasFocus());
        assertFalse(mButtonCancel.hasFocus());
        assertFalse(cancelListener.hasFocus());

        // set warning text to focus
        mWarningTextView.setFocusable(true);
        assertTrue(mWarningTextView.requestFocus());
        assertTrue(mWarningTextView.hasFocus());
        assertTrue(warningListener.hasFocus());
        assertFalse(mSymbolTextView.hasFocus());
        assertFalse(symbolListener.hasFocus());

        // set edit text to focus
        assertTrue(mEditText.requestFocus());
        assertTrue(mEditText.hasFocus());
        assertTrue(editListener.hasFocus());
        assertFalse(mWarningTextView.hasFocus());
        assertFalse(warningListener.hasFocus());
    }

    public void testSetupListeners() throws Throwable {
        // set ok button OnClick listener
        mButtonOk.setClickable(true);
        assertTrue(mButtonOk.isClickable());

        MockOnClickOkListener okButtonListener = new MockOnClickOkListener();
        mButtonOk.setOnClickListener(okButtonListener);

        // set cancel button OnClick listener
        mButtonCancel.setClickable(true);
        assertTrue(mButtonCancel.isClickable());

        MockOnClickCancelListener cancelButtonListener = new MockOnClickCancelListener();
        mButtonCancel.setOnClickListener(cancelButtonListener);

        // set edit text OnLongClick listener
        mEditText.setLongClickable(true);
        assertTrue(mEditText.isLongClickable());

        final MockOnLongClickListener onLongClickListener = new MockOnLongClickListener();
        mEditText.setOnLongClickListener(onLongClickListener);

        // long click the edit text
        assertFalse(onLongClickListener.isOnLongClickCalled());
        assertNull(onLongClickListener.getView());

        mInstrumentation.waitForIdleSync();
        TouchUtils.longClickView(this, mEditText);
        new PollingCheck() {
            @Override
            protected boolean check() {
                return onLongClickListener.isOnLongClickCalled();
            }
        }.run();
        assertSame(mEditText, onLongClickListener.getView());

        // click the Cancel button
        runTestOnUiThread(new Runnable() {
            public void run() {
                mEditText.setText("Germany");
            }
        });
        mInstrumentation.waitForIdleSync();

        TouchUtils.clickView(this, mButtonCancel);
        assertEquals("", mEditText.getText().toString());

        // click the OK button
        runTestOnUiThread(new Runnable() {
            public void run() {
                mEditText.setText(ARGENTINA);
            }
        });
        mInstrumentation.waitForIdleSync();

        TouchUtils.clickView(this, mButtonOk);
        assertEquals(ARGENTINA_SYMBOL, mSymbolTextView.getText().toString());

        runTestOnUiThread(new Runnable() {
            public void run() {
                mEditText.setText(AMERICA);
            }
        });
        mInstrumentation.waitForIdleSync();

        TouchUtils.clickView(this, mButtonOk);
        assertEquals(AMERICA_SYMBOL, mSymbolTextView.getText().toString());

        runTestOnUiThread(new Runnable() {
            public void run() {
                mEditText.setText(CHINA);
            }
        });
        mInstrumentation.waitForIdleSync();

        TouchUtils.clickView(this, mButtonOk);
        assertEquals(CHINA_SYMBOL, mSymbolTextView.getText().toString());

        runTestOnUiThread(new Runnable() {
            public void run() {
                mEditText.setText("Unknown");
            }
        });
        mInstrumentation.waitForIdleSync();

        TouchUtils.clickView(this, mButtonOk);
        assertEquals(View.VISIBLE, mWarningTextView.getVisibility());
    }

    @UiThreadTest
    public void testSetVisibility() throws Throwable {
        mActivity.setContentView(R.layout.view_visibility_layout);

        View v1 = mActivity.findViewById(R.id.textview1);
        View v2 = mActivity.findViewById(R.id.textview2);
        View v3 = mActivity.findViewById(R.id.textview3);

        assertNotNull(v1);
        assertNotNull(v2);
        assertNotNull(v3);

        assertEquals(View.VISIBLE, v1.getVisibility());
        assertEquals(View.INVISIBLE, v2.getVisibility());
        assertEquals(View.GONE, v3.getVisibility());

        v1.setVisibility(View.GONE);
        assertEquals(View.GONE, v1.getVisibility());

        v2.setVisibility(View.VISIBLE);
        assertEquals(View.VISIBLE, v2.getVisibility());

        v3.setVisibility(View.INVISIBLE);
        assertEquals(View.INVISIBLE, v3.getVisibility());
    }

    private static class MockOnFocusChangeListener implements OnFocusChangeListener {
        private boolean mHasFocus;

        public void onFocusChange(View v, boolean hasFocus) {
            mHasFocus = hasFocus;
        }

        public boolean hasFocus() {
            return mHasFocus;
        }
    }

    private class MockOnClickOkListener implements OnClickListener {
        private boolean mHasOnClickCalled = false;

        private boolean showPicture(String country) {
            if (ARGENTINA.equals(country)) {
                mSymbolTextView.setText(ARGENTINA_SYMBOL);
                return true;
            } else if (AMERICA.equals(country)) {
                mSymbolTextView.setText(AMERICA_SYMBOL);
                return true;
            } else if (CHINA.equals(country)) {
                mSymbolTextView.setText(CHINA_SYMBOL);
                return true;
            }

            return false;
        }

        public void onClick(View v) {
            mHasOnClickCalled = true;

            String country = mEditText.getText().toString();
            if (!showPicture(country)) {
                mWarningTextView.setVisibility(View.VISIBLE);
            } else if (View.VISIBLE == mWarningTextView.getVisibility()) {
                mWarningTextView.setVisibility(View.INVISIBLE);
            }
        }

        public boolean hasOnClickCalled() {
            return mHasOnClickCalled;
        }

        public void reset() {
            mHasOnClickCalled = false;
        }
    }

    private class MockOnClickCancelListener implements OnClickListener {
        private boolean mHasOnClickCalled = false;

        public void onClick(View v) {
            mHasOnClickCalled = true;

            mEditText.setText(null);
        }

        public boolean hasOnClickCalled() {
            return mHasOnClickCalled;
        }

        public void reset() {
            mHasOnClickCalled = false;
        }
    }

    private static class MockOnLongClickListener implements OnLongClickListener {
        private boolean mIsOnLongClickCalled;
        private View mView;

        public boolean onLongClick(View v) {
            mIsOnLongClickCalled = true;
            mView = v;
            return true;
        }

        public boolean isOnLongClickCalled() {
            return mIsOnLongClickCalled;
        }

        public View getView() {
            return mView;
        }
    }
}
