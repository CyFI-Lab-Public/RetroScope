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

package android.graphics.drawable.cts;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.graphics.drawable.DrawableContainer.DrawableContainerState;
import android.test.InstrumentationTestCase;
import android.util.Xml;

import com.android.cts.stub.R;


public class LevelListDrawableTest extends InstrumentationTestCase {
    private MockLevelListDrawable mLevelListDrawable;

    private Resources mResources;

    private DrawableContainerState mDrawableContainerState;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mLevelListDrawable = new MockLevelListDrawable();
        mDrawableContainerState = (DrawableContainerState) mLevelListDrawable.getConstantState();
        mResources = getInstrumentation().getTargetContext().getResources();
    }

    public void testLevelListDrawable() {
        new LevelListDrawable();
        // Check the values set in the constructor
        assertNotNull(new LevelListDrawable().getConstantState());
        assertTrue(new MockLevelListDrawable().hasCalledOnLevelChanged());
    }

    public void testAddLevel() {
        assertEquals(0, mDrawableContainerState.getChildCount());

        // nothing happens if drawable is null
        mLevelListDrawable.reset();
        mLevelListDrawable.addLevel(0, 0, null);
        assertEquals(0, mDrawableContainerState.getChildCount());
        assertFalse(mLevelListDrawable.hasCalledOnLevelChanged());

        // call onLevelChanged to assure that the correct drawable is selected.
        mLevelListDrawable.reset();
        mLevelListDrawable.addLevel(Integer.MAX_VALUE, Integer.MIN_VALUE, new MockDrawable());
        assertEquals(1, mDrawableContainerState.getChildCount());
        assertTrue(mLevelListDrawable.hasCalledOnLevelChanged());

        mLevelListDrawable.reset();
        mLevelListDrawable.addLevel(Integer.MIN_VALUE, Integer.MAX_VALUE, new MockDrawable());
        assertEquals(2, mDrawableContainerState.getChildCount());
        assertTrue(mLevelListDrawable.hasCalledOnLevelChanged());
    }

    public void testOnLevelChange() {
        mLevelListDrawable.addLevel(0, 0, new MockDrawable());
        mLevelListDrawable.addLevel(0, 0, new MockDrawable());
        mLevelListDrawable.addLevel(0, 10, new MockDrawable());

        // the method is not called if same level is set
        mLevelListDrawable.reset();
        mLevelListDrawable.setLevel(mLevelListDrawable.getLevel());
        assertFalse(mLevelListDrawable.hasCalledOnLevelChanged());

        // the method is called if different level is set
        mLevelListDrawable.reset();
        mLevelListDrawable.setLevel(mLevelListDrawable.getLevel() - 1);
        assertTrue(mLevelListDrawable.hasCalledOnLevelChanged());

        // check that correct drawable is selected.
        assertTrue(mLevelListDrawable.onLevelChange(10));
        assertSame(mLevelListDrawable.getCurrent(), mDrawableContainerState.getChildren()[2]);

        assertFalse(mLevelListDrawable.onLevelChange(5));
        assertSame(mLevelListDrawable.getCurrent(), mDrawableContainerState.getChildren()[2]);

        assertTrue(mLevelListDrawable.onLevelChange(0));
        assertSame(mLevelListDrawable.getCurrent(), mDrawableContainerState.getChildren()[0]);

        assertTrue(mLevelListDrawable.onLevelChange(100));
        assertNull(mLevelListDrawable.getCurrent());

        assertFalse(mLevelListDrawable.onLevelChange(101));
        assertNull(mLevelListDrawable.getCurrent());
    }

    public void testInflate() throws XmlPullParserException, IOException {
        XmlResourceParser parser = getResourceParser(R.xml.level_list_correct);

        mLevelListDrawable.reset();
        mLevelListDrawable.inflate(mResources, parser, Xml.asAttributeSet(parser));
        assertTrue(mLevelListDrawable.hasCalledOnLevelChanged());
        assertEquals(2, mDrawableContainerState.getChildCount());
        // check the android:minLevel and android:maxLevel by calling setLevel
        mLevelListDrawable.setLevel(200);
        assertSame(mLevelListDrawable.getCurrent(), mDrawableContainerState.getChildren()[0]);
        mLevelListDrawable.setLevel(201);
        assertSame(mLevelListDrawable.getCurrent(), mDrawableContainerState.getChildren()[1]);

        mLevelListDrawable.setLevel(0);
        assertNull(mLevelListDrawable.getCurrent());
        mLevelListDrawable.reset();
        parser = getResourceParser(R.xml.level_list_missing_item_minlevel_maxlevel);
        mLevelListDrawable.inflate(mResources, parser, Xml.asAttributeSet(parser));
        assertTrue(mLevelListDrawable.hasCalledOnLevelChanged());
        assertEquals(3, mDrawableContainerState.getChildCount());
        // default value of android:minLevel and android:maxLevel are both 0
        assertSame(mLevelListDrawable.getCurrent(), mDrawableContainerState.getChildren()[2]);
        mLevelListDrawable.setLevel(1);
        assertNull(mLevelListDrawable.getCurrent());

        parser = getResourceParser(R.xml.level_list_missing_item_drawable);
        try {
            mLevelListDrawable.inflate(mResources, parser, Xml.asAttributeSet(parser));
            fail("Should throw XmlPullParserException if drawable of item is missing");
        } catch (XmlPullParserException e) {
        }
    }

    public void testInflateWithNullParameters() throws XmlPullParserException, IOException{
        XmlResourceParser parser = getResourceParser(R.xml.level_list_correct);
        try {
            mLevelListDrawable.inflate(null, parser, Xml.asAttributeSet(parser));
            fail("Should throw XmlPullParserException if resource is null");
        } catch (NullPointerException e) {
        }

        try {
            mLevelListDrawable.inflate(mResources, null, Xml.asAttributeSet(parser));
            fail("Should throw XmlPullParserException if parser is null");
        } catch (NullPointerException e) {
        }

        try {
            mLevelListDrawable.inflate(mResources, parser, null);
            fail("Should throw XmlPullParserException if AttributeSet is null");
        } catch (NullPointerException e) {
        }
    }

    public void testMutate() throws InterruptedException {
        Resources resources = getInstrumentation().getTargetContext().getResources();
        LevelListDrawable d1 =
            (LevelListDrawable) resources.getDrawable(R.drawable.levellistdrawable);
        LevelListDrawable d2 =
            (LevelListDrawable) resources.getDrawable(R.drawable.levellistdrawable);
        LevelListDrawable d3 =
            (LevelListDrawable) resources.getDrawable(R.drawable.levellistdrawable);

        // the state does not appear to be shared before calling mutate()
        d1.addLevel(100, 200, resources.getDrawable(R.drawable.testimage));
        assertEquals(3, ((DrawableContainerState) d1.getConstantState()).getChildCount());
        assertEquals(2, ((DrawableContainerState) d2.getConstantState()).getChildCount());
        assertEquals(2, ((DrawableContainerState) d3.getConstantState()).getChildCount());

        // simply call mutate to make sure no exception is thrown
        d1.mutate();
    }

    private XmlResourceParser getResourceParser(int resId) throws XmlPullParserException,
            IOException {
        XmlResourceParser parser = getInstrumentation().getTargetContext().getResources().getXml(
                resId);
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) {
            // Empty loop
        }
        return parser;
    }

    private class MockLevelListDrawable extends LevelListDrawable {
        private boolean mHasCalledOnLevelChanged;

        public boolean hasCalledOnLevelChanged() {
            return mHasCalledOnLevelChanged;
        }

        public void reset() {
            mHasCalledOnLevelChanged = false;
        }

        @Override
        protected boolean onLevelChange(int level) {
            boolean result = super.onLevelChange(level);
            mHasCalledOnLevelChanged = true;
            return result;
        }
    }

    private class MockDrawable extends Drawable {
        @Override
        public void draw(Canvas canvas) {
        }

        @Override
        public int getOpacity() {
            return 0;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }
    }
}
