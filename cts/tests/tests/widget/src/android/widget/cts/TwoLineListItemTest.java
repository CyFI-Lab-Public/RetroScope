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

package android.widget.cts;

import com.android.cts.stub.R;


import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase;
import android.util.AttributeSet;
import android.widget.TextView;
import android.widget.TwoLineListItem;
import android.widget.RelativeLayout.LayoutParams;

/**
 * Test {@link TwoLineListItem}.
 */
public class TwoLineListItemTest extends
        ActivityInstrumentationTestCase<TwoLineListItemStubActivity> {
    private Activity mActivity;

    public TwoLineListItemTest() {
        super("com.android.cts.stub", TwoLineListItemStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testConstructor() {
        AttributeSet attrs = mActivity.getResources().getLayout(R.layout.twolinelistitem);
        assertNotNull(attrs);

        new TwoLineListItem(mActivity);
        try {
            new TwoLineListItem(null);
            fail("The constructor should throw NullPointerException when param Context is null.");
        } catch (NullPointerException e) {
        }

        new TwoLineListItem(mActivity, attrs);
        try {
            new TwoLineListItem(null, attrs);
            fail("The constructor should throw NullPointerException when param Context is null.");
        } catch (NullPointerException e) {
        }
        new TwoLineListItem(mActivity, null);

        new TwoLineListItem(mActivity, attrs, 0);
        try {
            new TwoLineListItem(null, attrs, 0);
            fail("The constructor should throw NullPointerException when param Context is null.");
        } catch (NullPointerException e) {
        }
        new TwoLineListItem(mActivity, null, 0);
        new TwoLineListItem(mActivity, attrs, Integer.MAX_VALUE);
        new TwoLineListItem(mActivity, attrs, Integer.MIN_VALUE);
    }

    public void testGetTexts() {
        TwoLineListItem twoLineListItem =
            (TwoLineListItem) mActivity.findViewById(R.id.twoLineListItem);

        Resources res = mActivity.getResources();
        assertNotNull(twoLineListItem.getText1());
        assertEquals(res.getString(R.string.twolinelistitem_test_text1),
                twoLineListItem.getText1().getText().toString());
        assertNotNull(twoLineListItem.getText2());
        assertEquals(res.getString(R.string.twolinelistitem_test_text2),
                twoLineListItem.getText2().getText().toString());
    }

    public void testOnFinishInflate() {
        MockTwoLineListItem twoLineListItem = new MockTwoLineListItem(mActivity);
        TextView text1 = new TextView(mActivity);
        text1.setId(com.android.internal.R.id.text1);
        TextView text2 = new TextView(mActivity);
        text2.setId(com.android.internal.R.id.text2);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        twoLineListItem.addView(text1, params);
        twoLineListItem.addView(text2, params);

        assertNull(twoLineListItem.getText1());
        assertNull(twoLineListItem.getText2());
        twoLineListItem.onFinishInflate();
        assertSame(text1, twoLineListItem.getText1());
        assertSame(text2, twoLineListItem.getText2());
    }

    /**
     * The Class MockTwoLineListItem is just a wrapper of TwoLineListItem to
     * make access to protected method possible .
     */
    private class MockTwoLineListItem extends TwoLineListItem {
        public MockTwoLineListItem(Context context) {
            super(context);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
        }
    }
}
