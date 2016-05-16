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

package android.graphics.drawable.cts;

import com.android.cts.stub.R;


import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.cts.util.PollingCheck;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer.DrawableContainerState;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Xml;
import android.widget.ImageView;
import android.widget.cts.ImageViewStubActivity;

import java.io.IOException;

public class AnimationDrawableTest extends ActivityInstrumentationTestCase2<ImageViewStubActivity> {
    private static final int FRAMES_COUNT        = 3;
    private static final int FIRST_FRAME_INDEX   = 0;
    private static final int SECOND_FRAME_INDEX  = 1;
    private static final int THIRD_FRAME_INDEX   = 2;
    private static final long TOLERANCE = 500;
    private static final long FIRST_FRAME_DURATION   = 3000;
    private static final long SECOND_FRAME_DURATION  = 2000;
    private static final long THIRD_FRAME_DURATION   = 1000;

    private AnimationDrawable mAnimationDrawable;
    private Resources mResources;

    public AnimationDrawableTest() {
        super("com.android.cts.stub", ImageViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final Activity activity = getActivity();
        mResources = activity.getResources();
        try {
            runTestOnUiThread(new Runnable() {
                public void run() {
                    ImageView imageView = (ImageView) activity.findViewById(R.id.imageview);
                    imageView.setBackgroundResource(R.drawable.animationdrawable);
                    mAnimationDrawable = (AnimationDrawable) imageView.getBackground();
                }
            });
        } catch (Throwable t) {
            throw new Exception(t);
        }
    }

    public void testConstructor() {
        mAnimationDrawable = new AnimationDrawable();
        // Check the values set in the constructor
        assertNotNull(mAnimationDrawable.getConstantState());
        assertFalse(mAnimationDrawable.isRunning());
        assertTrue(mAnimationDrawable.isOneShot());
    }

    public void testSetVisible() throws Throwable {
        assertTrue(mAnimationDrawable.isVisible());
        runTestOnUiThread(new Runnable() {
            public void run() {
                mAnimationDrawable.start();
            }
        });
        assertTrue(mAnimationDrawable.isRunning());
        assertSame(mAnimationDrawable.getFrame(FIRST_FRAME_INDEX),
                mAnimationDrawable.getCurrent());

        pollingCheckDrawable(SECOND_FRAME_INDEX, FIRST_FRAME_DURATION);

        runTestOnUiThread(new Runnable() {
            public void run() {
                assertTrue(mAnimationDrawable.setVisible(false, false));
            }
        });
        assertFalse(mAnimationDrawable.isVisible());
        assertFalse(mAnimationDrawable.isRunning());
        assertStoppedAnimation(SECOND_FRAME_INDEX, SECOND_FRAME_DURATION);

        // restart animation
        runTestOnUiThread(new Runnable() {
            public void run() {
                assertTrue(mAnimationDrawable.setVisible(true, true));
            }
        });
        assertTrue(mAnimationDrawable.isVisible());
        assertTrue(mAnimationDrawable.isRunning());
        pollingCheckDrawable(SECOND_FRAME_INDEX, FIRST_FRAME_DURATION);
    }

    public void testStart() throws Throwable {
        // animation should play repeat if do not stop it.
        assertFalse(mAnimationDrawable.isOneShot());
        assertFalse(mAnimationDrawable.isRunning());
        runTestOnUiThread(new Runnable() {
            public void run() {
                mAnimationDrawable.start();
            }
        });

        assertTrue(mAnimationDrawable.isRunning());
        assertSame(mAnimationDrawable.getFrame(FIRST_FRAME_INDEX),
                mAnimationDrawable.getCurrent());
        pollingCheckDrawable(SECOND_FRAME_INDEX, FIRST_FRAME_DURATION);

        runTestOnUiThread(new Runnable() {
            public void run() {
                // This method has no effect if the animation is running.
                mAnimationDrawable.start();
            }
        });
        pollingCheckDrawable(THIRD_FRAME_INDEX, SECOND_FRAME_DURATION);

        runTestOnUiThread(new Runnable() {
            public void run() {
                mAnimationDrawable.stop();
            }
        });
        assertFalse(mAnimationDrawable.isRunning());
        assertStoppedAnimation(THIRD_FRAME_INDEX, THIRD_FRAME_DURATION);

        runTestOnUiThread(new Runnable() {
            public void run() {
                // This method has no effect if the animation is not running.
                mAnimationDrawable.stop();
            }
        });
        assertFalse(mAnimationDrawable.isRunning());
        assertStoppedAnimation(THIRD_FRAME_INDEX, THIRD_FRAME_DURATION);
    }

    public void testRun() {
        // This method should not be called directly.
    }

    public void testUnscheduleSelf() throws Throwable {
        assertFalse(mAnimationDrawable.isRunning());
        runTestOnUiThread(new Runnable() {
            public void run() {
                mAnimationDrawable.start();
            }
        });

        assertTrue(mAnimationDrawable.isRunning());
        pollingCheckDrawable(SECOND_FRAME_INDEX, FIRST_FRAME_DURATION);

        runTestOnUiThread(new Runnable() {
            public void run() {
                mAnimationDrawable.unscheduleSelf(mAnimationDrawable);
            }
        });
        assertFalse(mAnimationDrawable.isRunning());
        assertStoppedAnimation(SECOND_FRAME_INDEX, SECOND_FRAME_DURATION);
    }

    public void testGetNumberOfFrames() {
        assertEquals(FRAMES_COUNT, mAnimationDrawable.getNumberOfFrames());

        Drawable frame = mResources.getDrawable(R.drawable.failed);
        mAnimationDrawable.addFrame(frame, 2000);
        assertEquals(FRAMES_COUNT + 1, mAnimationDrawable.getNumberOfFrames());

        // add same frame with same duration
        mAnimationDrawable.addFrame(frame, 2000);
        assertEquals(FRAMES_COUNT + 2, mAnimationDrawable.getNumberOfFrames());

        try {
            mAnimationDrawable.addFrame(null, 1000);
            fail("Should throw NullPointerException if param frame is null.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testGetFrame() {
        Drawable frame = mAnimationDrawable.getFrame(FIRST_FRAME_INDEX);
        Drawable drawable = mResources.getDrawable(R.drawable.testimage);
        assertEquals(drawable.getIntrinsicWidth(), frame.getIntrinsicWidth());
        assertEquals(drawable.getIntrinsicHeight(), frame.getIntrinsicHeight());

        frame = mAnimationDrawable.getFrame(SECOND_FRAME_INDEX);
        drawable = mResources.getDrawable(R.drawable.pass);
        assertEquals(drawable.getIntrinsicWidth(), frame.getIntrinsicWidth());
        assertEquals(drawable.getIntrinsicHeight(), frame.getIntrinsicHeight());

        frame = mAnimationDrawable.getFrame(THIRD_FRAME_INDEX);
        drawable = mResources.getDrawable(R.drawable.scenery);
        assertEquals(drawable.getIntrinsicWidth(), frame.getIntrinsicWidth());
        assertEquals(drawable.getIntrinsicHeight(), frame.getIntrinsicHeight());

        assertNull(mAnimationDrawable.getFrame(THIRD_FRAME_INDEX + 1));

        try {
            mAnimationDrawable.getFrame(-1);
            fail("Should throw ArrayIndexOutOfBoundsException.");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        try {
            mAnimationDrawable.getFrame(10);
            fail("Should throw ArrayIndexOutOfBoundsException.");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }

    public void testGetDuration() {
        assertEquals(FIRST_FRAME_DURATION, mAnimationDrawable.getDuration(FIRST_FRAME_INDEX));
        assertEquals(SECOND_FRAME_DURATION, mAnimationDrawable.getDuration(SECOND_FRAME_INDEX));
        assertEquals(THIRD_FRAME_DURATION, mAnimationDrawable.getDuration(THIRD_FRAME_INDEX));
        assertEquals(0, mAnimationDrawable.getDuration(THIRD_FRAME_INDEX + 1));

        try {
            mAnimationDrawable.getDuration(-1);
            fail("Should throw ArrayIndexOutOfBoundsException.");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }

        try {
            mAnimationDrawable.getDuration(10);
            fail("Should throw ArrayIndexOutOfBoundsException.");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }

    public void testAccessOneShot() throws Throwable {
        // animation should play repeat if do not stop it.
        assertFalse(mAnimationDrawable.isOneShot());
        runTestOnUiThread(new Runnable() {
            public void run() {
                mAnimationDrawable.start();
            }
        });
        pollingCheckDrawable(SECOND_FRAME_INDEX, FIRST_FRAME_DURATION);
        pollingCheckDrawable(THIRD_FRAME_INDEX, SECOND_FRAME_DURATION);
        // begin to repeat
        pollingCheckDrawable(FIRST_FRAME_INDEX, THIRD_FRAME_DURATION);

        runTestOnUiThread(new Runnable() {
            public void run() {
                mAnimationDrawable.stop();
                mAnimationDrawable.setOneShot(true);
                assertTrue(mAnimationDrawable.isOneShot());
                mAnimationDrawable.start();
            }
        });
        pollingCheckDrawable(SECOND_FRAME_INDEX, FIRST_FRAME_DURATION);
        pollingCheckDrawable(THIRD_FRAME_INDEX, SECOND_FRAME_DURATION);
        // do not repeat
        assertStoppedAnimation(THIRD_FRAME_INDEX, THIRD_FRAME_DURATION);
    }

    public void testInflate() throws XmlPullParserException, IOException {
        mAnimationDrawable = new AnimationDrawable();
        DrawableContainerState drawableContainerState =
            (DrawableContainerState) mAnimationDrawable.getConstantState();

        XmlResourceParser parser = getResourceParser(R.xml.anim_list_correct);
        mAnimationDrawable.inflate(mResources, parser, Xml.asAttributeSet(parser));
        // android:visible="false"
        assertFalse(mAnimationDrawable.isVisible());
        // android:oneShot="true"
        assertTrue(mAnimationDrawable.isOneShot());
        // android:variablePadding="true"
        assertNull(drawableContainerState.getConstantPadding());
        assertEquals(2, mAnimationDrawable.getNumberOfFrames());
        assertEquals(2000, mAnimationDrawable.getDuration(0));
        assertEquals(1000, mAnimationDrawable.getDuration(1));
        assertSame(mAnimationDrawable.getFrame(0), mAnimationDrawable.getCurrent());

        parser = getResourceParser(R.xml.anim_list_missing_list_attrs);
        mAnimationDrawable.inflate(mResources, parser, Xml.asAttributeSet(parser));
        // use current the visibility
        assertFalse(mAnimationDrawable.isVisible());
        // default value of android:oneShot is false
        assertFalse(mAnimationDrawable.isOneShot());
        // default value of android:variablePadding is false
        // TODO: its not clear what the value of constant padding should be when variablePadding
        // is false
        //assertNotNull(drawableContainerState.getConstantPadding());
        // add a new frame from xml
        assertEquals(3, mAnimationDrawable.getNumberOfFrames());
        assertEquals(2000, mAnimationDrawable.getDuration(0));
        assertEquals(1000, mAnimationDrawable.getDuration(1));
        assertEquals(2000, mAnimationDrawable.getDuration(2));
        assertSame(mAnimationDrawable.getFrame(0), mAnimationDrawable.getCurrent());

        parser = getResourceParser(R.xml.anim_list_missing_item_drawable);
        try {
            mAnimationDrawable.inflate(mResources, parser, Xml.asAttributeSet(parser));
            fail("Should throw XmlPullParserException if drawable of item is missing");
        } catch (XmlPullParserException e) {
            // expected
        }
    }

    public void testInflateWithNullParameters() throws XmlPullParserException, IOException {
        XmlResourceParser parser = getResourceParser(R.drawable.animationdrawable);
        try {
            mAnimationDrawable.inflate(null, parser, Xml.asAttributeSet(parser));
            fail("Should throw NullPointerException if resource is null");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            mAnimationDrawable.inflate(mResources, null, Xml.asAttributeSet(parser));
            fail("Should throw NullPointerException if parser is null");
        } catch (NullPointerException e) {
            // expected
        }

        try {
            mAnimationDrawable.inflate(mResources, parser, null);
            fail("Should throw NullPointerException if AttributeSet is null");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testMutate() {
        AnimationDrawable d1 = (AnimationDrawable) mResources
                .getDrawable(R.drawable.animationdrawable);
        // multiple instances inflated from the same resource do not share the state
        // simply call mutate to make sure it does not throw an exception
        d1.mutate();
    }

    private XmlResourceParser getResourceParser(int resId) throws XmlPullParserException,
            IOException {
        XmlResourceParser parser = mResources.getXml(resId);
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) {
            // Empty loop
        }
        return parser;
    }

    /**
     * Polling check specific frame should be current one in timeout.
     * @param index - expected index of frame.
     * @param timeout - timeout.
     */
    private void pollingCheckDrawable(final int index, long timeout) {
        new PollingCheck(timeout + TOLERANCE) {
            Drawable expected = mAnimationDrawable.getFrame(index);
            @Override
            protected boolean check() {
                return mAnimationDrawable.getCurrent().equals(expected);
            }
        }.run();
    }

    /**
     * Assert animation had been stopped. It will sleep duration + TOLERANCE milliseconds and
     * then make sure current frame had not been changed.
     * @param index - index of current frame.
     * @param duration - duration of current frame.
     */
    private void assertStoppedAnimation(int index, long duration) throws InterruptedException {
        Thread.sleep(duration + TOLERANCE);
        assertSame(mAnimationDrawable.getFrame(index), mAnimationDrawable.getCurrent());
    }
}
