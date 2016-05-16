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

package android.view.cts;


import android.graphics.Rect;
import android.test.ActivityInstrumentationTestCase2;
import android.view.FocusFinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class FocusFinderTest extends ActivityInstrumentationTestCase2<FocusFinderStubActivity> {

    private FocusFinder mFocusFinder;
    private ViewGroup mLayout;
    private Button mTopLeft;
    private Button mTopRight;
    private Button mBottomLeft;
    private Button mBottomRight;

    public FocusFinderTest() {
        super("com.android.cts.stub", FocusFinderStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mFocusFinder = FocusFinder.getInstance();
        mLayout = getActivity().layout;
        mTopLeft = getActivity().topLeftButton;
        mTopRight = getActivity().topRightButton;
        mBottomLeft = getActivity().bottomLeftButton;
        mBottomRight = getActivity().bottomRightButton;
    }

    public void testGetInstance() {
        mFocusFinder = null;
        mFocusFinder = FocusFinder.getInstance();
        assertNotNull(mFocusFinder);
    }

    public void testFindNextFocus() {
        /*
         * Go clockwise around the buttons from the top left searching for focus.
         *
         * +---+---+
         * | 1 | 2 |
         * +---+---+
         * | 3 | 4 |
         * +---+---+
         */
        assertNextFocus(mTopLeft, View.FOCUS_RIGHT, mTopRight);
        assertNextFocus(mTopRight, View.FOCUS_DOWN, mBottomRight);
        assertNextFocus(mBottomRight, View.FOCUS_LEFT, mBottomLeft);
        assertNextFocus(mBottomLeft, View.FOCUS_UP, mTopLeft);

        assertNextFocus(null, View.FOCUS_RIGHT, mTopLeft);
        assertNextFocus(null, View.FOCUS_DOWN, mTopLeft);
        assertNextFocus(null, View.FOCUS_LEFT, mBottomRight);
        assertNextFocus(null, View.FOCUS_UP, mBottomRight);
    }

    private void assertNextFocus(View currentFocus, int direction, View expectedNextFocus) {
        View actualNextFocus = mFocusFinder.findNextFocus(mLayout, currentFocus, direction);
        assertEquals(expectedNextFocus, actualNextFocus);
    }

    public void testFindNextFocusFromRect() {
        /*
         * Create a small rectangle on the border between the top left and top right buttons.
         *
         * +---+---+
         * |  [ ]  |
         * +---+---+
         * |   |   |
         * +---+---+
         */
        Rect rect = new Rect();
        mTopLeft.getDrawingRect(rect);
        rect.offset(mTopLeft.getWidth() / 2, 0);
        rect.inset(mTopLeft.getWidth() / 4, mTopLeft.getHeight() / 4);

        assertNextFocusFromRect(rect, View.FOCUS_LEFT, mTopLeft);
        assertNextFocusFromRect(rect, View.FOCUS_RIGHT, mTopRight);

        /*
         * Create a small rectangle on the border between the top left and bottom left buttons.
         *
         * +---+---+
         * |   |   |
         * +[ ]+---+
         * |   |   |
         * +---+---+
         */
        mTopLeft.getDrawingRect(rect);
        rect.offset(0, mTopRight.getHeight() / 2);
        rect.inset(mTopLeft.getWidth() / 4, mTopLeft.getHeight() / 4);

        assertNextFocusFromRect(rect, View.FOCUS_UP, mTopLeft);
        assertNextFocusFromRect(rect, View.FOCUS_DOWN, mBottomLeft);
    }

    private void assertNextFocusFromRect(Rect rect, int direction, View expectedNextFocus) {
        View actualNextFocus = mFocusFinder.findNextFocusFromRect(mLayout, rect, direction);
        assertEquals(expectedNextFocus, actualNextFocus);
    }

    public void testFindNearestTouchable() {
        /*
         * Table layout with two rows and coordinates are relative to those parent rows.
         * Lines outside the box signify touch points used in the tests.
         *      |
         *   +---+---+
         *   | 1 | 2 |--
         *   +---+---+
         * --| 3 | 4 |
         *   +---+---+
         *         |
         */

        // 1
        int x = mTopLeft.getWidth() / 2 - 5;
        int y = 0;
        int[] deltas = new int[2];
        View view = mFocusFinder.findNearestTouchable(mLayout, x, y, View.FOCUS_DOWN, deltas);
        assertEquals(mTopLeft, view);
        assertEquals(0, deltas[0]);
        assertEquals(0, deltas[1]);

        // 2
        deltas = new int[2];
        x = mTopRight.getRight();
        y = mTopRight.getBottom() / 2;
        view = mFocusFinder.findNearestTouchable(mLayout, x, y, View.FOCUS_LEFT, deltas);
        assertEquals(mTopRight, view);
        assertEquals(-1, deltas[0]);
        assertEquals(0, deltas[1]);

        // 3
        deltas = new int[2];
        x = 0;
        y = mTopLeft.getBottom() + mBottomLeft.getHeight() / 2;
        view = mFocusFinder.findNearestTouchable(mLayout, x, y, View.FOCUS_RIGHT, deltas);
        assertEquals(mBottomLeft, view);
        assertEquals(0, deltas[0]);
        assertEquals(0, deltas[1]);

        // 4
        deltas = new int[2];
        x = mBottomRight.getRight();
        y = mTopRight.getBottom() + mBottomRight.getBottom();
        view = mFocusFinder.findNearestTouchable(mLayout, x, y, View.FOCUS_UP, deltas);
        assertEquals(mBottomRight, view);
        assertEquals(0, deltas[0]);
        assertEquals(-1, deltas[1]);
    }
}
