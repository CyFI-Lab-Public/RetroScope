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

import com.android.cts.stub.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.graphics.drawable.RotateDrawable;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.Xml;

public class RotateDrawableTest extends AndroidTestCase {
    private RotateDrawable mRotateDrawable;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Resources resources = mContext.getResources();
        mRotateDrawable = (RotateDrawable) resources.getDrawable(R.drawable.rotatedrawable);
    }

    public void testConstructor() {
        new RotateDrawable();
    }

    public void testDraw() {
        Canvas canvas = new Canvas();
        mRotateDrawable.draw(canvas);
    }

    public void testGetChangingConfigurations() {
        assertEquals(0, mRotateDrawable.getChangingConfigurations());

        mRotateDrawable.setChangingConfigurations(Configuration.KEYBOARD_NOKEYS);
        assertEquals(Configuration.KEYBOARD_NOKEYS, mRotateDrawable.getChangingConfigurations());

        mRotateDrawable.setChangingConfigurations(Configuration.KEYBOARD_12KEY);
        assertEquals(Configuration.KEYBOARD_12KEY, mRotateDrawable.getChangingConfigurations());
    }

    public void testSetAlpha() {
        mRotateDrawable.setAlpha(100);
        assertEquals(100, ((BitmapDrawable) mRotateDrawable.getDrawable()).getPaint().getAlpha());

        mRotateDrawable.setAlpha(255);
        assertEquals(255, ((BitmapDrawable) mRotateDrawable.getDrawable()).getPaint().getAlpha());
    }

    public void testSetColorFilter() {
        ColorFilter filter = new ColorFilter();
        mRotateDrawable.setColorFilter(filter);
        assertSame(filter,
                ((BitmapDrawable) mRotateDrawable.getDrawable()).getPaint().getColorFilter());

        mRotateDrawable.setColorFilter(null);
        assertNull(((BitmapDrawable) mRotateDrawable.getDrawable()).getPaint().getColorFilter());
    }

    public void testGetOpacity() {
        assertEquals(PixelFormat.OPAQUE, mRotateDrawable.getOpacity());
    }

    public void testInvalidateDrawable() {
        Drawable drawable = mContext.getResources().getDrawable(R.drawable.pass);
        MockCallback callback = new MockCallback();

        mRotateDrawable.setCallback(callback);
        mRotateDrawable.invalidateDrawable(null);
        assertTrue(callback.hasCalledInvalidate());

        callback.reset();
        mRotateDrawable.invalidateDrawable(drawable);
        assertTrue(callback.hasCalledInvalidate());

        callback.reset();
        mRotateDrawable.setCallback(null);
        mRotateDrawable.invalidateDrawable(drawable);
        assertFalse(callback.hasCalledInvalidate());
    }

    public void testScheduleDrawable() {
        MockCallback callback = new MockCallback();

        mRotateDrawable.setCallback(callback);
        mRotateDrawable.scheduleDrawable(null, null, 0);
        assertTrue(callback.hasCalledSchedule());

        callback.reset();
        mRotateDrawable.scheduleDrawable(new BitmapDrawable(), new Runnable() {
            public void run() {
            }
        }, 1000L);
        assertTrue(callback.hasCalledSchedule());

        callback.reset();
        mRotateDrawable.setCallback(null);
        mRotateDrawable.scheduleDrawable(null, null, 0);
        assertFalse(callback.hasCalledSchedule());
    }

    public void testUnscheduleDrawable() {
        MockCallback callback = new MockCallback();

        mRotateDrawable.setCallback(callback);
        mRotateDrawable.unscheduleDrawable(null, null);
        assertTrue(callback.hasCalledUnschedule());

        callback.reset();
        mRotateDrawable.unscheduleDrawable(new BitmapDrawable(), new Runnable() {
            public void run() {
            }
        });
        assertTrue(callback.hasCalledUnschedule());

        callback.reset();
        mRotateDrawable.setCallback(null);
        mRotateDrawable.unscheduleDrawable(null, null);
        assertFalse(callback.hasCalledUnschedule());
    }

    public void testGetPadding() {
        Rect rect = new Rect();
        assertFalse(mRotateDrawable.getPadding(rect));
        assertEquals(0, rect.left);
        assertEquals(0, rect.top);
        assertEquals(0, rect.right);
        assertEquals(0, rect.bottom);
    }

    public void testSetVisible() {
        assertTrue(mRotateDrawable.isVisible());

        assertTrue(mRotateDrawable.setVisible(false, false));
        assertFalse(mRotateDrawable.isVisible());

        assertFalse(mRotateDrawable.setVisible(false, true));
        assertFalse(mRotateDrawable.isVisible());

        assertTrue(mRotateDrawable.setVisible(true, false));
        assertTrue(mRotateDrawable.isVisible());
    }

    public void testIsStateful() {
        assertFalse(mRotateDrawable.isStateful());
    }

    public void testMethods() {
        // implementation details, do not test.
    }

    public void testGetIntrinsicWidthAndHeight() throws XmlPullParserException, IOException {
        // testimage is set in res/drawable/rotatedrawable.xml
        Drawable drawable = mContext.getResources().getDrawable(R.drawable.testimage);
        assertEquals(drawable.getIntrinsicWidth(), mRotateDrawable.getIntrinsicWidth());
        assertEquals(drawable.getIntrinsicHeight(), mRotateDrawable.getIntrinsicHeight());

        RotateDrawable rotateDrawable = new RotateDrawable();
        Resources r = mContext.getResources();
        XmlPullParser parser = r.getXml(R.drawable.rotatedrawable);
        while (parser.next() != XmlPullParser.START_TAG) {
            // ignore event, just seek to first tag
        }
        AttributeSet attrs = Xml.asAttributeSet(parser);
        rotateDrawable.inflate(r, parser, attrs);
        assertEquals(drawable.getIntrinsicWidth(), rotateDrawable.getIntrinsicWidth());
        assertEquals(drawable.getIntrinsicHeight(), rotateDrawable.getIntrinsicHeight());

        try {
            mRotateDrawable.inflate(null, null, null);
            fail("did not throw NullPointerException when parameters are null.");
        } catch (NullPointerException e) {
            // expected, test success
        }
    }

    public void testGetConstantState() {
        ConstantState state = mRotateDrawable.getConstantState();
        assertNotNull(state);
    }

    public void testMutate() {
        Resources resources = mContext.getResources();

        RotateDrawable d1 = (RotateDrawable) resources.getDrawable(R.drawable.rotatedrawable);
        RotateDrawable d2 = (RotateDrawable) resources.getDrawable(R.drawable.rotatedrawable);
        RotateDrawable d3 = (RotateDrawable) resources.getDrawable(R.drawable.rotatedrawable);

        d1.setAlpha(100);
        assertEquals(100, ((BitmapDrawable) d1.getDrawable()).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d2.getDrawable()).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d3.getDrawable()).getPaint().getAlpha());

        d1.mutate();
        d1.setAlpha(200);
        assertEquals(200, ((BitmapDrawable) d1.getDrawable()).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d2.getDrawable()).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d3.getDrawable()).getPaint().getAlpha());

        d2.setAlpha(50);
        assertEquals(200, ((BitmapDrawable) d1.getDrawable()).getPaint().getAlpha());
        assertEquals(50, ((BitmapDrawable) d2.getDrawable()).getPaint().getAlpha());
        assertEquals(50, ((BitmapDrawable) d3.getDrawable()).getPaint().getAlpha());
    }

    private static class MockCallback implements Drawable.Callback {
        private boolean mCalledInvalidate;
        private boolean mCalledSchedule;
        private boolean mCalledUnschedule;

        public void invalidateDrawable(Drawable who) {
            mCalledInvalidate = true;
        }

        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            mCalledSchedule = true;
        }

        public void unscheduleDrawable(Drawable who, Runnable what) {
            mCalledUnschedule = true;
        }

        public boolean hasCalledInvalidate() {
            return mCalledInvalidate;
        }

        public boolean hasCalledSchedule() {
            return mCalledSchedule;
        }

        public boolean hasCalledUnschedule() {
            return mCalledUnschedule;
        }

        public int getResolvedLayoutDirection(Drawable who) {
            return 0;
        }

        public void reset() {
            mCalledInvalidate = false;
            mCalledSchedule = false;
            mCalledUnschedule = false;
        }
    }
}
