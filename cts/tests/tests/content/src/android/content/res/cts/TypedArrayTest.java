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

package android.content.res.cts;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;

import com.android.cts.stub.R;
import com.android.internal.util.XmlUtils;


public class TypedArrayTest extends AndroidTestCase{
    private TypedArray mTypedArray;
    private static final int DEFINT = -1;
    private static final float DEFFLOAT = -1.0f;
    private static final int EXPECTEDCLOLOR = 0xff0000ff;
    private static final int EXPECTED_COLOR_STATE = 0xff00ff00;
    private static final float EXPECTED_DIMENSION = 0.75f;
    private static final int EXPECTED_PIXEL_OFFSET = 10;
    private static final int EXPECTED_LAYOUT_DIMENSION = 10;
    private static final int EXPECTED_PIXEL_SIZE = 18;
    private static final float EXPECTED_FLOAT = 3.14f;
    private static final float EXPECTED_FRACTION = 10.0f;
    private static final int EXPECTED_INT = 365;
    private static final String EXPECTED_STRING = "Hello, Android!";
    private static final String EXPECTED_TEXT = "TypedArray Test!";
    private static final String[] EXPECTED_TEXT_ARRAY = {"Easy", "Medium", "Hard"};
    private static final int EXPETED_INDEX = 15;
    private static final TypedValue DEF_VALUE = new TypedValue();
    private static final int EXPECTED_INDEX_COUNT = 16;
    private static final String EXPTECTED_POS_DESCRIP = "<internal>";
    private static final int EXPECTED_LENGTH = 16;
    private static final String EXPECTED_NON_RESOURCE_STRING = "testNonResourcesString";
    private static final String XML_BEGIN = "resources";
    private static final int EXPECTED_INT_ATT = 86400;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final int[] attrs = R.styleable.style1;
        mTypedArray = getContext().getTheme().obtainStyledAttributes(R.style.Whatever, attrs);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mTypedArray.recycle();
    }

    /*
     * Test all get attrs methods, all test value are in styles.xml and attrs.xml.
     */
    public void testAttrsMethod() {
        // getBoolean test
        assertTrue(mTypedArray.getBoolean(R.styleable.style1_type1, false));
        assertFalse(mTypedArray.getBoolean(R.styleable.style1_type2, true));

        assertEquals(EXPECTEDCLOLOR, mTypedArray.getColor(R.styleable.style1_type3, DEFINT));

        // getColorStateList test
        final int[] set = new int[1];
        set[0] = 0;
        assertEquals(EXPECTED_COLOR_STATE,
                mTypedArray.getColorStateList(R.styleable.style1_type4).
                getColorForState(set, DEFINT));

        // This get values equals attribute dimension value set in styles.xml
        // multiplied by the appropriate metric, the metric is unknown.
        assertEquals(EXPECTED_DIMENSION,
                mTypedArray.getDimension(R.styleable.style1_type5, DEFFLOAT));
        assertEquals(EXPECTED_PIXEL_OFFSET,
                mTypedArray.getDimensionPixelOffset(R.styleable.style1_type6, DEFINT));
        assertEquals(EXPECTED_LAYOUT_DIMENSION,
                mTypedArray.getLayoutDimension(R.styleable.style1_type6, "type6"));
        assertEquals(EXPECTED_LAYOUT_DIMENSION,
                mTypedArray.getLayoutDimension(R.styleable.style1_type6, 0));
        assertEquals(EXPECTED_PIXEL_SIZE,
                mTypedArray.getDimensionPixelSize(R.styleable.style1_type7, DEFINT));

        // getDrawable test
        assertNotNull(mTypedArray.getDrawable(R.styleable.style1_type8));
        // getResourceId test
        assertEquals(R.drawable.pass,
                mTypedArray.getResourceId(R.styleable.style1_type8, DEFINT));

        assertEquals(EXPECTED_FLOAT, mTypedArray.getFloat(R.styleable.style1_type9, DEFFLOAT));

        assertEquals(EXPECTED_FRACTION,
                mTypedArray.getFraction(R.styleable.style1_type10, 10, 10, DEFFLOAT));

        assertEquals(EXPECTED_INT, mTypedArray.getInt(R.styleable.style1_type11, DEFINT));

        assertEquals(EXPECTED_INT_ATT, mTypedArray.getInteger(R.styleable.style1_type12, DEFINT));

        assertEquals(EXPECTED_STRING, mTypedArray.getString(R.styleable.style1_type13));

        // getNonResourceString test
        assertNull(mTypedArray.getNonResourceString(R.styleable.style1_type14));

        assertEquals(EXPECTED_TEXT, mTypedArray.getText(R.styleable.style1_type14));

        CharSequence[] textArray = mTypedArray.getTextArray(R.styleable.style1_type15);
        assertEquals(EXPECTED_TEXT_ARRAY[0], textArray[0]);
        assertEquals(EXPECTED_TEXT_ARRAY[1], textArray[1]);
        assertEquals(EXPECTED_TEXT_ARRAY[2], textArray[2]);

        // getIndex test
        int index = mTypedArray.getIndex(R.styleable.style1_type16);
        assertEquals(EXPETED_INDEX, index);
        assertTrue(mTypedArray.getValue(index, DEF_VALUE));
        // hasValue test
        assertTrue(mTypedArray.hasValue(R.styleable.style1_type16));

        // peekValue test
        assertNotNull(mTypedArray.peekValue(R.styleable.style1_type16));

        assertEquals(EXPECTED_INDEX_COUNT, mTypedArray.getIndexCount());

        assertEquals(EXPTECTED_POS_DESCRIP,
                mTypedArray.getPositionDescription());

        // getResources test
        assertEquals(getContext().getResources(), mTypedArray.getResources());

        assertEquals(EXPECTED_LENGTH, mTypedArray.length());

        // toString test
        assertNotNull(mTypedArray.toString());
    }

    public void testRecycle() {
        final ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(getContext(), 0);
        contextThemeWrapper.setTheme(R.style.TextAppearance);
        final TypedArray test = contextThemeWrapper.getTheme().obtainStyledAttributes(
                R.styleable.TextAppearance);
        test.recycle();
    }

    public void testNonResourceString() throws XmlPullParserException, IOException {
        final XmlResourceParser parser = getContext().getResources().getXml(R.xml.test_color);
        XmlUtils.beginDocument(parser, XML_BEGIN);
        final AttributeSet set = parser;
        assertEquals(1, set.getAttributeCount());
        final TypedArray ta = getContext().getResources().obtainAttributes(set,
                com.android.internal.R.styleable.AndroidManifest);
        assertEquals(1, ta.getIndexCount());
        assertEquals(EXPECTED_NON_RESOURCE_STRING, ta.getNonResourceString(
                com.android.internal.R.styleable.AndroidManifest_versionName));
        ta.recycle();
        parser.close();
    }
}
