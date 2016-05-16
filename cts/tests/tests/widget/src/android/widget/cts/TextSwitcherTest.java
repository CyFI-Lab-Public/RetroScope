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


import android.content.Context;
import android.test.InstrumentationTestCase;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextSwitcher;
import android.widget.TextView;

/**
 * Test {@link TextSwitcher}.
 */
public class TextSwitcherTest extends InstrumentationTestCase {
    private Context mContext;

    /**
     * test width to be used in addView() method.
     */
    private static final int PARAMS_WIDTH = 200;
    /**
     * test height to be used in addView() method.
     */
    private static final int PARAMS_HEIGHT = 300;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getContext();
    }

    public void testConstructor() {
        new TextSwitcher(mContext);

        new TextSwitcher(mContext, null);
    }

    public void testSetText() {
        final String viewText1 = "Text 1";
        final String viewText2 = "Text 2";
        final String changedText = "Changed";

        TextSwitcher textSwitcher = new TextSwitcher(mContext);

        TextView tv1 = new TextView(mContext);
        TextView tv2 = new TextView(mContext);
        tv1.setText(viewText1);
        tv2.setText(viewText2);
        textSwitcher.addView(tv1, 0, new ViewGroup.LayoutParams(PARAMS_WIDTH, PARAMS_HEIGHT));
        textSwitcher.addView(tv2, 1, new ViewGroup.LayoutParams(PARAMS_WIDTH, PARAMS_HEIGHT));

        TextView tvChild1 = (TextView) textSwitcher.getChildAt(0);
        TextView tvChild2 = (TextView) textSwitcher.getChildAt(1);
        assertEquals(viewText1, (tvChild1.getText().toString()));
        assertEquals(viewText2, (tvChild2.getText().toString()));
        assertSame(tv1, textSwitcher.getCurrentView());

        // tvChild2's text is changed
        textSwitcher.setText(changedText);
        assertEquals(viewText1, (tvChild1.getText().toString()));
        assertEquals(changedText, (tvChild2.getText().toString()));
        assertSame(tv2, textSwitcher.getCurrentView());

        // tvChild1's text is changed
        textSwitcher.setText(changedText);
        assertEquals(changedText, (tvChild1.getText().toString()));
        assertEquals(changedText, (tvChild2.getText().toString()));
        assertSame(tv1, textSwitcher.getCurrentView());

        // tvChild2's text is changed
        textSwitcher.setText(null);
        assertEquals(changedText, (tvChild1.getText().toString()));
        assertEquals("", (tvChild2.getText().toString()));
        assertSame(tv2, textSwitcher.getCurrentView());
    }

    public void testSetCurrentText() {
        final String viewText1 = "Text 1";
        final String viewText2 = "Text 2";
        final String changedText1 = "Changed 1";
        final String changedText2 = "Changed 2";

        TextSwitcher textSwitcher = new TextSwitcher(mContext);

        TextView tv1 = new TextView(mContext);
        TextView tv2 = new TextView(mContext);
        tv1.setText(viewText1);
        tv2.setText(viewText2);
        textSwitcher.addView(tv1, 0, new ViewGroup.LayoutParams(PARAMS_WIDTH, PARAMS_HEIGHT));
        textSwitcher.addView(tv2, 1, new ViewGroup.LayoutParams(PARAMS_WIDTH, PARAMS_HEIGHT));

        TextView tvChild1 = (TextView) textSwitcher.getChildAt(0);
        TextView tvChild2 = (TextView) textSwitcher.getChildAt(1);
        assertEquals(viewText1, (tvChild1.getText().toString()));
        assertEquals(viewText2, (tvChild2.getText().toString()));
        assertSame(tv1, textSwitcher.getCurrentView());

        // tvChild1's text is changed
        textSwitcher.setCurrentText(changedText1);
        assertEquals(changedText1, (tvChild1.getText().toString()));
        assertEquals(viewText2, (tvChild2.getText().toString()));
        assertSame(tv1, textSwitcher.getCurrentView());

        // tvChild1's text is changed
        textSwitcher.setCurrentText(changedText2);
        assertEquals(changedText2, (tvChild1.getText().toString()));
        assertEquals(viewText2, (tvChild2.getText().toString()));
        assertSame(tv1, textSwitcher.getCurrentView());

        // tvChild1's text is changed
        textSwitcher.setCurrentText(null);
        assertEquals("", (tvChild1.getText().toString()));
        assertEquals(viewText2, (tvChild2.getText().toString()));
        assertSame(tv1, textSwitcher.getCurrentView());
    }

    public void testAddView() {
        TextSwitcher textSwitcher = new TextSwitcher(mContext);

        TextView tv1 = new TextView(mContext);
        TextView tv2 = new TextView(mContext);

        textSwitcher.addView(tv1, 0, new ViewGroup.LayoutParams(PARAMS_WIDTH, PARAMS_HEIGHT));
        assertSame(tv1, textSwitcher.getChildAt(0));
        assertEquals(1, textSwitcher.getChildCount());

        try {
            // tv1 already has a parent
            textSwitcher.addView(tv1, 0, new ViewGroup.LayoutParams(PARAMS_WIDTH, PARAMS_HEIGHT));
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }

        try {
            textSwitcher.addView(tv2, Integer.MAX_VALUE,
                    new ViewGroup.LayoutParams(PARAMS_WIDTH, PARAMS_HEIGHT));
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected
        }

        textSwitcher.addView(tv2, 1,
                new ViewGroup.LayoutParams(PARAMS_WIDTH, PARAMS_HEIGHT));
        assertSame(tv2, textSwitcher.getChildAt(1));
        assertEquals(2, textSwitcher.getChildCount());

        TextView tv3 = new TextView(mContext);

        try {
            // textSwitcher already has 2 children.
            textSwitcher.addView(tv3, 2, new ViewGroup.LayoutParams(PARAMS_WIDTH, PARAMS_HEIGHT));
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }

        textSwitcher = new TextSwitcher(mContext);
        ListView lv = new ListView(mContext);

        try {
            textSwitcher.addView(lv, 0, new ViewGroup.LayoutParams(PARAMS_WIDTH, PARAMS_HEIGHT));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            textSwitcher.addView(null, 0, new ViewGroup.LayoutParams(PARAMS_WIDTH, PARAMS_HEIGHT));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            textSwitcher.addView(tv3, 0, null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
            // issue 1695243, not clear what is supposed to happen if the LayoutParams is null.
        }
    }
}
