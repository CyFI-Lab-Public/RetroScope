/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.test.ActivityInstrumentationTestCase;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import com.android.cts.stub.R;
import org.xmlpull.v1.XmlPullParser;

import static android.view.ViewGroup.LAYOUT_MODE_OPTICAL_BOUNDS;
import static android.widget.GridLayout.spec;

/**
 * Test {@link android.widget.GridLayout}.
 */
public class GridLayoutTest extends ActivityInstrumentationTestCase<GridLayoutStubActivity> {

    // The size of the off-screen test container in which we we will testing layout.
    public static final int MAX_X = 2000;
    public static final int MAX_Y = 2000;

    private static abstract class Alignment {
        String name;
        int gravity;

        abstract int getValue(View v);

        protected Alignment(String name, int gravity) {
            this.name = name;
            this.gravity = gravity;
        }
    }

    private static final Alignment[] HORIZONTAL_ALIGNMENTS = {
            new Alignment("LEFT", Gravity.LEFT) {
                @Override
                int getValue(View v) {
                    return v.getLeft();
                }
            },
            new Alignment("CENTER", Gravity.CENTER_HORIZONTAL) {
                @Override
                int getValue(View v) {
                    return (v.getLeft() + v.getRight()) / 2;
                }
            },
            new Alignment("RIGHT", Gravity.RIGHT) {
                @Override
                int getValue(View v) {
                    return v.getRight();
                }
            },
            new Alignment("FILL", Gravity.FILL_HORIZONTAL) {
                @Override
                int getValue(View v) {
                    return v.getWidth();
                }
            }
    };

    private static final Alignment[] VERTICAL_ALIGNMENTS = {
            new Alignment("TOP", Gravity.TOP) {
                @Override
                int getValue(View v) {
                    return v.getTop();
                }
            },
            new Alignment("CENTER", Gravity.CENTER_VERTICAL) {
                @Override
                int getValue(View v) {
                    return (v.getTop() + v.getBottom()) / 2;
                }
            },
            new Alignment("BASELINE", Gravity.NO_GRAVITY) {
                @Override
                int getValue(View v) {
                    return v.getTop() + v.getBaseline();
                }
            },
            new Alignment("BOTTOM", Gravity.BOTTOM) {
                @Override
                int getValue(View v) {
                    return v.getBottom();
                }
            },
            new Alignment("FILL", Gravity.FILL_VERTICAL) {
                @Override
                int getValue(View v) {
                    return v.getHeight();
                }
            }
    };

    private Context mContext;

    public GridLayoutTest() {
        super("com.android.cts.stub", GridLayoutStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();
    }

    public void testConstructor() {
        new GridLayout(mContext);

        new GridLayout(mContext, null);

        XmlPullParser parser = mContext.getResources().getXml(R.layout.gridlayout_layout);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        new GridLayout(mContext, attrs);

        try {
            new GridLayout(null, null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    public void testCheckLayoutParams() {
        GridLayout gridLayout = new GridLayout(mContext);

        gridLayout.addView(new TextView(mContext), new AbsoluteLayout.LayoutParams(0, 0, 0, 0));

        gridLayout.addView(new TextView(mContext), new GridLayout.LayoutParams(
                GridLayout.spec(0),
                GridLayout.spec(0)));

    }

    public void testGenerateDefaultLayoutParams() {
        GridLayout gridLayout = new GridLayout(mContext);
        ViewGroup.LayoutParams lp = gridLayout.generateLayoutParams(null);
        assertNotNull(lp);
        assertTrue(lp instanceof GridLayout.LayoutParams);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, lp.width);
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, lp.height);
    }

    private View[][] populate(GridLayout container) {
        Context context = container.getContext();
        int N = VERTICAL_ALIGNMENTS.length;
        int M = HORIZONTAL_ALIGNMENTS.length;

        View[][] table = new View[N + 1][M + 1];

        {
            TextView v = new TextView(context);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(spec(0), spec(0));
            lp.setGravity(Gravity.CENTER);
            v.setText("*");
            container.addView(v, lp);
        }
        for (int i = 0; i < N; i++) {
            Alignment va = VERTICAL_ALIGNMENTS[i];
            int row = i + 1;
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(spec(row), spec(0));
            lp.setGravity(va.gravity | Gravity.CENTER_HORIZONTAL);
            TextView v = new TextView(context);
            v.setGravity(Gravity.CENTER);
            v.setText(va.name);
            container.addView(v, lp);
            table[row][0] = v;
        }
        for (int j = 0; j < M; j++) {
            Alignment ha = HORIZONTAL_ALIGNMENTS[j];
            int col = j + 1;
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(spec(0), spec(col));
            lp.setGravity(Gravity.CENTER_VERTICAL | ha.gravity);
            TextView v = new TextView(context);
            v.setGravity(Gravity.CENTER);
            v.setText(ha.name);
            container.addView(v, lp);
            table[0][col] = v;
        }
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                Alignment ha = HORIZONTAL_ALIGNMENTS[j];
                Alignment va = VERTICAL_ALIGNMENTS[i];
                int row = i + 1;
                int col = j + 1;
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams(spec(row), spec(col));
                lp.setGravity(va.gravity | ha.gravity);
                TextView v = new Button(context);
                v.setText("X");
                v.setTextSize(10 + 5 * row * col);
                container.addView(v, lp);
                table[row][col] = v;
            }
        }
        return table;
    }

    private void testAlignment(int row, int col, Alignment a, View v0, View v1, String group) {
        int a0 = a.getValue(v0);
        int a1 = a.getValue(v1);
        assertEquals("View at row " + row + ", column " + col + " was not " + a.name +
                " aligned with its title " + group + ";",
                a0, a1);
    }

    private void test(GridLayout p, View[][] table) {
        p.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        p.layout(0, 0, MAX_X, MAX_Y);

        int N = VERTICAL_ALIGNMENTS.length;
        int M = HORIZONTAL_ALIGNMENTS.length;

        // test all horizontal alignments in each column
        for (int j = 0; j < M; j++) {
            int col = j + 1;
            View v0 = table[0][col];
            Alignment alignment = HORIZONTAL_ALIGNMENTS[j];
            for (int i = 0; i < N; i++) {
                int row = i + 1;
                testAlignment(row, col, alignment, v0, table[row][col], "column");
            }
        }

        // test all vertical alignments in each row
        for (int i = 0; i < N; i++) {
            int row = i + 1;
            View v0 = table[row][0];
            Alignment alignment = VERTICAL_ALIGNMENTS[i];
            for (int j = 0; j < M; j++) {
                int col = j + 1;
                testAlignment(row, col, alignment, v0, table[row][col], "row");
            }
        }
    }
    public void testAlignment() {
        GridLayout p = new GridLayout(mContext);
        View[][] table = populate(p);
        test(p, table);
        //p.setLayoutMode(ViewGroup.LAYOUT_MODE_OPTICAL_BOUNDS);
        //test(p, table);
    }
}
