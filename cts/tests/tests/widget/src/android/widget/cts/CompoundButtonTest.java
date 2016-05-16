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


import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.StateSet;
import android.util.Xml;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

/**
 * Test {@link CompoundButton}.
 */
public class CompoundButtonTest extends AndroidTestCase {
    private Resources mResources;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResources = mContext.getResources();
    }

    public void testConstructor() {
        XmlPullParser parser = mContext.getResources().getXml(R.layout.togglebutton_layout);
        AttributeSet mAttrSet = Xml.asAttributeSet(parser);

        new MockCompoundButton(mContext, mAttrSet, 0);
        new MockCompoundButton(mContext, mAttrSet);
        new MockCompoundButton(mContext);

        try {
            new MockCompoundButton(null, null, -1);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }

        try {
            new MockCompoundButton(null, null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }

        try {
            new MockCompoundButton(null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    public void testAccessChecked() {
        CompoundButton compoundButton = new MockCompoundButton(mContext);
        MockOnCheckedChangeListener listener = new MockOnCheckedChangeListener();
        compoundButton.setOnCheckedChangeListener(listener);
        assertFalse(compoundButton.isChecked());
        assertFalse(listener.hasCalledCheckedChange());

        compoundButton.setChecked(true);
        assertTrue(compoundButton.isChecked());
        assertTrue(listener.hasCalledCheckedChange());
        assertSame(compoundButton, listener.getInputCompoundButton());
        assertTrue(listener.getInputChecked());

        listener.reset();
        compoundButton.setChecked(true);
        assertTrue(compoundButton.isChecked());
        assertFalse(listener.hasCalledCheckedChange());

        compoundButton.setChecked(false);
        assertFalse(compoundButton.isChecked());
        assertTrue(listener.hasCalledCheckedChange());
        assertSame(compoundButton, listener.getInputCompoundButton());
        assertFalse(listener.getInputChecked());
    }

    public void testSetOnCheckedChangeListener() {
        CompoundButton compoundButton = new MockCompoundButton(mContext);
        MockOnCheckedChangeListener listener = new MockOnCheckedChangeListener();
        compoundButton.setOnCheckedChangeListener(listener);
        assertFalse(compoundButton.isChecked());
        assertFalse(listener.hasCalledCheckedChange());

        compoundButton.setChecked(true);
        assertTrue(listener.hasCalledCheckedChange());

        // set null
        compoundButton.setOnCheckedChangeListener(null);
        listener.reset();
        compoundButton.setChecked(false);
        assertFalse(listener.hasCalledCheckedChange());
    }

    public void testToggle() {
        CompoundButton compoundButton = new MockCompoundButton(mContext);
        assertFalse(compoundButton.isChecked());

        compoundButton.toggle();
        assertTrue(compoundButton.isChecked());

        compoundButton.toggle();
        assertFalse(compoundButton.isChecked());

        compoundButton.setChecked(true);
        compoundButton.toggle();
        assertFalse(compoundButton.isChecked());
    }

    public void testPerformClick() {
        CompoundButton compoundButton = new MockCompoundButton(mContext);
        assertFalse(compoundButton.isChecked());

        // performClick without OnClickListener will return false.
        assertFalse(compoundButton.performClick());
        assertTrue(compoundButton.isChecked());

        assertFalse(compoundButton.performClick());
        assertFalse(compoundButton.isChecked());

        // performClick with OnClickListener will return true.
        compoundButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            }
        });
        assertTrue(compoundButton.performClick());
        assertTrue(compoundButton.isChecked());

        assertTrue(compoundButton.performClick());
        assertFalse(compoundButton.isChecked());
    }

    public void testDrawableStateChanged() {
        MockCompoundButton compoundButton = new MockCompoundButton(mContext);
        assertFalse(compoundButton.isChecked());
        // drawableStateChanged without any drawables.
        compoundButton.drawableStateChanged();

        // drawableStateChanged when CheckMarkDrawable is not null.
        Drawable drawable = mResources.getDrawable(R.drawable.scenery);
        compoundButton.setButtonDrawable(drawable);
        drawable.setState(null);
        assertNull(drawable.getState());

        compoundButton.drawableStateChanged();
        assertNotNull(drawable.getState());
        assertSame(compoundButton.getDrawableState(), drawable.getState());
    }

    public void testSetButtonDrawableByDrawable() {
        CompoundButton compoundButton;

        // set null drawable
        compoundButton = new MockCompoundButton(mContext);
        compoundButton.setButtonDrawable(null);

        // set drawable when checkedTextView is GONE
        compoundButton = new MockCompoundButton(mContext);
        compoundButton.setVisibility(View.GONE);
        Drawable firstDrawable = mResources.getDrawable(R.drawable.scenery);
        firstDrawable.setVisible(true, false);
        assertEquals(StateSet.WILD_CARD, firstDrawable.getState());

        compoundButton.setButtonDrawable(firstDrawable);
        assertFalse(firstDrawable.isVisible());

        // update drawable when checkedTextView is VISIBLE
        compoundButton.setVisibility(View.VISIBLE);
        Drawable secondDrawable = mResources.getDrawable(R.drawable.pass);
        secondDrawable.setVisible(true, false);
        assertEquals(StateSet.WILD_CARD, secondDrawable.getState());

        compoundButton.setButtonDrawable(secondDrawable);
        assertTrue(secondDrawable.isVisible());
        // the firstDrawable is not active.
        assertFalse(firstDrawable.isVisible());
    }

    public void testSetButtonDrawableById() {
        CompoundButton compoundButton;
        // resId is 0
        compoundButton = new MockCompoundButton(mContext);
        compoundButton.setButtonDrawable(0);

        // set drawable
        compoundButton = new MockCompoundButton(mContext);
        compoundButton.setButtonDrawable(R.drawable.scenery);

        // set the same drawable again
        compoundButton.setButtonDrawable(R.drawable.scenery);

        // update drawable
        compoundButton.setButtonDrawable(R.drawable.pass);
    }

    public void testOnCreateDrawableState() {
        MockCompoundButton compoundButton;

        // compoundButton is not checked, append 0 to state array.
        compoundButton = new MockCompoundButton(mContext);
        int[] state = compoundButton.onCreateDrawableState(0);
        assertEquals(0, state[state.length - 1]);

        // compoundButton is checked, append R.attr.state_checked to state array.
        compoundButton.setChecked(true);
        int[] checkedState = compoundButton.onCreateDrawableState(0);
        assertEquals(state[0], checkedState[0]);
        assertEquals(com.android.internal.R.attr.state_checked,
                checkedState[checkedState.length - 1]);

        // compoundButton is not checked again.
        compoundButton.setChecked(false);
        state = compoundButton.onCreateDrawableState(0);
        assertEquals(0, state[state.length - 1]);
    }

    public void testOnDraw() {
        int viewHeight;
        int drawableWidth;
        int drawableHeight;
        Rect bounds;
        Drawable drawable;
        Canvas canvas = new Canvas(android.graphics.Bitmap.createBitmap(100, 100,
                android.graphics.Bitmap.Config.ARGB_8888));
        MockCompoundButton compoundButton;

        // onDraw when there is no drawable
        compoundButton = new MockCompoundButton(mContext);
        compoundButton.onDraw(canvas);

        // onDraw when Gravity.TOP, it's default.
        compoundButton = new MockCompoundButton(mContext);
        drawable = mResources.getDrawable(R.drawable.scenery);
        compoundButton.setButtonDrawable(drawable);
        viewHeight = compoundButton.getHeight();
        drawableWidth = drawable.getIntrinsicWidth();
        drawableHeight = drawable.getIntrinsicHeight();

        compoundButton.onDraw(canvas);
        bounds = drawable.copyBounds();
        assertEquals(0, bounds.left);
        assertEquals(drawableWidth, bounds.right);
        assertEquals(0, bounds.top);
        assertEquals(drawableHeight, bounds.bottom);

        // onDraw when Gravity.BOTTOM
        compoundButton.setGravity(Gravity.BOTTOM);
        compoundButton.onDraw(canvas);
        bounds = drawable.copyBounds();
        assertEquals(0, bounds.left);
        assertEquals(drawableWidth, bounds.right);
        assertEquals(viewHeight - drawableHeight, bounds.top);
        assertEquals(viewHeight, bounds.bottom);

        // onDraw when Gravity.CENTER_VERTICAL
        compoundButton.setGravity(Gravity.CENTER_VERTICAL);
        compoundButton.onDraw(canvas);
        bounds = drawable.copyBounds();
        assertEquals(0, bounds.left);
        assertEquals(drawableWidth, bounds.right);
        assertEquals( (viewHeight - drawableHeight) / 2, bounds.top);
        assertEquals( (viewHeight - drawableHeight) / 2 + drawableHeight, bounds.bottom);
    }

    public void testAccessInstanceState() {
        CompoundButton compoundButton = new MockCompoundButton(mContext);
        Parcelable state;

        assertFalse(compoundButton.isChecked());
        assertFalse(compoundButton.getFreezesText());

        state = compoundButton.onSaveInstanceState();
        assertNotNull(state);
        assertTrue(compoundButton.getFreezesText());

        compoundButton.setChecked(true);

        compoundButton.onRestoreInstanceState(state);
        assertFalse(compoundButton.isChecked());
        assertTrue(compoundButton.isLayoutRequested());
    }

    public void testVerifyDrawable() {
        MockCompoundButton compoundButton = new MockCompoundButton(mContext);
        Drawable drawable = mContext.getResources().getDrawable(R.drawable.scenery);

        assertTrue(compoundButton.verifyDrawable(null));
        assertFalse(compoundButton.verifyDrawable(drawable));

        compoundButton.setButtonDrawable(drawable);
        assertTrue(compoundButton.verifyDrawable(null));
        assertTrue(compoundButton.verifyDrawable(drawable));
    }

    private final class MockCompoundButton extends CompoundButton {
        public MockCompoundButton(Context context) {
            super(context);
        }

        public MockCompoundButton(Context context, AttributeSet attrs) {
            super(context, attrs, 0);
        }

        public MockCompoundButton(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected void drawableStateChanged() {
            super.drawableStateChanged();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
        }

        @Override
        protected int[] onCreateDrawableState(int extraSpace) {
            return super.onCreateDrawableState(extraSpace);
        }

        @Override
        protected boolean verifyDrawable(Drawable who) {
            return super.verifyDrawable(who);
        }
    }

    private final class MockOnCheckedChangeListener implements OnCheckedChangeListener {
        private boolean mHasCalledChecked;
        private CompoundButton mCompoundButton;
        private boolean mIsChecked;

        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mHasCalledChecked = true;
            mCompoundButton = buttonView;
            mIsChecked = isChecked;
        }

        public boolean getInputChecked() {
            return mIsChecked;
        }

        public CompoundButton getInputCompoundButton() {
            return mCompoundButton;
        }

        public boolean hasCalledCheckedChange() {
            return mHasCalledChecked;
        }

        public void reset() {
            mHasCalledChecked = false;
            mCompoundButton = null;
            mIsChecked = false;
        }
    }
}
