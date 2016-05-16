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

package android.content.res.cts;

import com.android.cts.stub.R;
import com.android.internal.util.XmlUtils;


import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.util.Xml;
import android.view.Display;
import android.view.WindowManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class ResourcesTest extends AndroidTestCase {
    private static final String CONFIG_VARYING = "configVarying";
    private static final String SIMPLE = "simple";
    private static final String CONFIG_VARYING_SIMPLE = "configVarying/simple";
    private static final String PACKAGE_NAME = "com.android.cts.stub";
    private static final String COM_ANDROID_CTS_STUB_IDENTIFIER =
                "com.android.cts.stub:configVarying/simple";
    private Resources mResources;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mResources = getContext().getResources();
    }

    public void testResources() {
        final AssetManager am = new AssetManager();
        final Configuration cfg = new Configuration();
        cfg.keyboard = Configuration.KEYBOARDHIDDEN_YES;
        final DisplayMetrics dm = new DisplayMetrics();
        dm.setToDefaults();

        final Resources r = new Resources(am, dm, cfg);
        final Configuration c = r.getConfiguration();
        assertEquals(Configuration.KEYBOARDHIDDEN_YES, c.keyboard);
    }

    public void testGetString() {
        try {
            mResources.getString(-1, "%s");
            fail("Failed at testGetString2");
        } catch (NotFoundException e) {
          //expected
        }

        final String strGo = mResources.getString(R.string.go, "%1$s%%", 12);
        assertEquals("Go", strGo);
    }

    public void testObtainAttributes() throws XmlPullParserException, IOException {
        final XmlPullParser parser = mResources.getXml(R.xml.test_color);
        XmlUtils.beginDocument(parser, "resources");
        final AttributeSet set = Xml.asAttributeSet(parser);
        final TypedArray testTypedArray = mResources.obtainAttributes(set, R.styleable.Style1);
        assertNotNull(testTypedArray);
        assertEquals(2, testTypedArray.length());
        assertEquals(0, testTypedArray.getColor(0, 0));
        assertEquals(128, testTypedArray.getColor(1, 128));
        assertEquals(mResources, testTypedArray.getResources());
        testTypedArray.recycle();
    }

    public void testObtainTypedArray() {
        try {
            mResources.obtainTypedArray(-1);
            fail("Failed at testObtainTypedArray");
        } catch (NotFoundException e) {
            //expected
        }

        final TypedArray ta = mResources.obtainTypedArray(R.array.string);
        assertEquals(3, ta.length());
        assertEquals("Test String 1", ta.getString(0));
        assertEquals("Test String 2", ta.getString(1));
        assertEquals("Test String 3", ta.getString(2));
        assertEquals(mResources, ta.getResources());
    }

    private Resources getResources(final Configuration config, final int mcc, final int mnc,
            final int touchscreen, final int keyboard, final int keysHidden, final int navigation,
            final int width, final int height) {
        final AssetManager assmgr = new AssetManager();
        assmgr.addAssetPath(mContext.getPackageResourcePath());
        final DisplayMetrics metrics = new DisplayMetrics();
        final WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        final Display d = wm.getDefaultDisplay();
        d.getMetrics(metrics);
        config.mcc = mcc;
        config.mnc = mnc;
        config.touchscreen = touchscreen;
        config.keyboard = keyboard;
        config.keyboardHidden = keysHidden;
        config.navigation = navigation;
        metrics.widthPixels = width;
        metrics.heightPixels = height;
        return new Resources(assmgr, metrics, config);
    }

    private static void checkGetText1(final Resources res, final int resId,
            final String expectedValue) {
        final String actual = res.getText(resId).toString();
        assertNotNull("Returned wrong configuration-based simple value: expected <nothing>, "
                + "got '" + actual + "' from resource 0x" + Integer.toHexString(resId),
                expectedValue);
        assertEquals("Returned wrong configuration-based simple value: expected " + expectedValue
                + ", got '" + actual + "' from resource 0x" + Integer.toHexString(resId),
                expectedValue, actual);
    }

    private static void checkGetText2(final Resources res, final int resId,
            final String expectedValue) {
        final String actual = res.getText(resId, null).toString();
        assertNotNull("Returned wrong configuration-based simple value: expected <nothing>, "
                + "got '" + actual + "' from resource 0x" + Integer.toHexString(resId),
                expectedValue);
        assertEquals("Returned wrong configuration-based simple value: expected " + expectedValue
                + ", got '" + actual + "' from resource 0x" + Integer.toHexString(resId),
                expectedValue, actual);
    }

    public void testGetMovie() {
        try {
            mResources.getMovie(-1);
            fail("Failed at testGetMovie");
        } catch (NotFoundException e) {
            //expected
        }
    }

    public void testGetDimension() {
        try {
            mResources.getDimension(-1);
            fail("Failed at testGetDimension");
        } catch (NotFoundException e) {
            //expected
        }

        // app_icon_size is 48px, as defined in cts/tests/res/values/resources_test.xml
        final float dim = mResources.getDimension(R.dimen.app_icon_size);
        assertEquals(48.0f, dim);
    }

    public void testGetDimensionPixelOffset() {
        try {
            mResources.getDimensionPixelOffset(-1);
            fail("Failed at testGetDimensionPixelOffset");
        } catch (NotFoundException e) {
            //expected
        }

        // app_icon_size is 48px, as defined in cts/tests/res/values/resources_test.xml
        final int dim = mResources.getDimensionPixelOffset(R.dimen.app_icon_size);
        assertEquals(48, dim);
    }

    public void testGetColorStateList() {
        try {
            mResources.getColorStateList(-1);
            fail("Failed at testGetColorStateList");
        } catch (NotFoundException e) {
            //expected
        }

        final ColorStateList colorStateList = mResources.getColorStateList(R.color.color1);
        final int[] focusedState = {android.R.attr.state_focused};
        final int focusColor = colorStateList.getColorForState(focusedState, R.color.failColor);
        assertEquals(mResources.getColor(R.color.testcolor1), focusColor);
    }

    public void testGetColor() {
        try {
            mResources.getColor(-1);
            fail("Failed at testGetColor");
        } catch (NotFoundException e) {
            //expected
        }

        final int color = mResources.getColor(R.color.testcolor1);
        assertEquals(0xff00ff00, color);
    }

    public void testUpdateConfiguration() {
        final Configuration cfg = mResources.getConfiguration();
        assertTrue(cfg.fontScale != 5);

        cfg.fontScale = 5;
        mResources.updateConfiguration(cfg, null);
        Configuration cfgNew = mResources.getConfiguration();
        assertEquals(5.0f, cfgNew.fontScale, 0.001f);
    }

    public void testGetDimensionPixelSize() {
        try {
            mResources.getDimensionPixelSize(-1);
            fail("Failed at testGetDimensionPixelSize");
        } catch (NotFoundException e) {
            //expected
        }

        // app_icon_size is 48px, as defined in cts/tests/res/values/resources_test.xml
        final int size = mResources.getDimensionPixelSize(R.dimen.app_icon_size);
        assertEquals(48, size);
    }

    public void testGetDrawable() {
        try {
            mResources.getDrawable(-1);
            fail("Failed at testGetDrawable");
        } catch (NotFoundException e) {
            //expected
        }

        // testimage is defined in cts/tests/res/drawable/testimage.jpg and measures 212px x 142px
        final Drawable draw = mResources.getDrawable(R.drawable.testimage);
        int targetDensity = mResources.getDisplayMetrics().densityDpi;
        int defaultDensity = DisplayMetrics.DENSITY_DEFAULT;
        assertNotNull(draw);
        assertEquals(212 * targetDensity / defaultDensity, draw.getIntrinsicWidth(), 1);
        assertEquals(142 * targetDensity / defaultDensity, draw.getIntrinsicHeight(), 1);
    }

    public void testGetAnimation() throws Exception {
        try {
            mResources.getAnimation(-1);
            fail("Failed at testGetAnimation");
        } catch (NotFoundException e) {
            //expected
        }

        final XmlResourceParser ani = mResources.getAnimation(R.anim.anim_rotate);
        assertNotNull(ani);
        XmlUtils.beginDocument(ani, "rotate");
        assertEquals(7, ani.getAttributeCount());
        assertEquals("Binary XML file line #18", ani.getPositionDescription());
        assertEquals("interpolator", ani.getAttributeName(0));
        assertEquals("@17432582", ani.getAttributeValue(0));
    }

    public void testGetQuantityString1() {
        try {
            mResources.getQuantityString(-1, 1, "");
            fail("Failed at testGetQuantityString1");
        } catch (NotFoundException e) {
            //expected
        }

        final String strGo = mResources.getQuantityString(R.plurals.plurals_test, 1, "");
        assertEquals("A dog", strGo);
    }

    public void testGetQuantityString2() {
        try {
            mResources.getQuantityString(-1, 1);
            fail("Failed at testGetQuantityString2");
        } catch (NotFoundException e) {
            //expected
        }

        final String strGo = mResources.getQuantityString(R.plurals.plurals_test, 1);
        assertEquals("A dog", strGo);
    }

    public void testGetInteger() {
        try {
            mResources.getInteger(-1);
            fail("Failed at testGetInteger");
        } catch (NotFoundException e) {
            //expected
        }

        final int i = mResources.getInteger(R.integer.resource_test_int);
        assertEquals(10, i);
    }

    public void testGetValue() {
        final TypedValue tv = new TypedValue();

        try {
            mResources.getValue("null", tv, false);
            fail("Failed at testGetValue");
        } catch (NotFoundException e) {
            //expected
        }

        mResources.getValue("com.android.cts.stub:raw/text", tv, false);
        assertNotNull(tv);
        assertEquals("res/raw/text.txt", tv.coerceToString());
    }

    public void testGetAssets() {
        final AssetManager aM = mResources.getAssets();
        assertNotNull(aM);
        assertTrue(aM.isUpToDate());
    }

    public void testGetSystem() {
        assertNotNull(Resources.getSystem());
    }

    public void testGetLayout() throws Exception {
        try {
            mResources.getLayout(-1);
            fail("Failed at testGetLayout");
        } catch (NotFoundException e) {
            //expected
        }

        final XmlResourceParser layout = mResources.getLayout(R.layout.abslistview_layout);
        assertNotNull(layout);
        XmlUtils.beginDocument(layout, "ViewGroup_Layout");
        assertEquals(3, layout.getAttributeCount());
        assertEquals("id", layout.getAttributeName(0));
        assertEquals("@" + R.id.abslistview_root, layout.getAttributeValue(0));
    }

    public void testGetBoolean() {
        try {
            mResources.getBoolean(-1);
            fail("Failed at testGetBoolean");
        } catch (NotFoundException e) {
            //expected
        }

        final boolean b = mResources.getBoolean(R.integer.resource_test_int);
        assertTrue(b);
    }

    public void testgetFraction() {
        assertEquals(1, (int)mResources.getFraction(R.dimen.frac100perc, 1, 1));
        assertEquals(100, (int)mResources.getFraction(R.dimen.frac100perc, 100, 1));
    }

    public void testParseBundleExtras() throws XmlPullParserException, IOException {
        final Bundle b = new Bundle();
        XmlResourceParser parser = mResources.getXml(R.xml.extra);
        XmlUtils.beginDocument(parser, "tag");

        assertEquals(0, b.size());
        mResources.parseBundleExtras(parser, b);
        assertEquals(1, b.size());
        assertEquals("android", b.getString("google"));
    }

    public void testParseBundleExtra() throws XmlPullParserException, IOException {
        final Bundle b = new Bundle();
        XmlResourceParser parser = mResources.getXml(R.xml.extra);

        XmlUtils.beginDocument(parser, "tag");
        assertEquals(0, b.size());
        mResources.parseBundleExtra("test", parser, b);
        assertEquals(1, b.size());
        assertEquals("Lee", b.getString("Bruce"));
    }

    public void testGetIdentifier() {

        int resid = mResources.getIdentifier(COM_ANDROID_CTS_STUB_IDENTIFIER, null, null);
        assertEquals(R.configVarying.simple, resid);

        resid = mResources.getIdentifier(CONFIG_VARYING_SIMPLE, null, PACKAGE_NAME);
        assertEquals(R.configVarying.simple, resid);

        resid = mResources.getIdentifier(SIMPLE, CONFIG_VARYING, PACKAGE_NAME);
        assertEquals(R.configVarying.simple, resid);
    }

    public void testGetIntArray() {
        final int NO_EXIST_ID = -1;
        try {
            mResources.getIntArray(NO_EXIST_ID);
            fail("should throw out NotFoundException");
        } catch (NotFoundException e) {
            // expected
        }
        // expected value is defined in res/value/arrays.xml
        final int[] expectedArray1 = new int[] {
                0, 0, 0
        };
        final int[] expectedArray2 = new int[] {
                0, 1, 101
        };
        int[]array1 = mResources.getIntArray(R.array.strings);
        int[]array2 = mResources.getIntArray(R.array.integers);

        checkArrayEqual(expectedArray1, array1);
        checkArrayEqual(expectedArray2, array2);

    }

    private void checkArrayEqual(int[] array1, int[] array2) {
        assertNotNull(array2);
        assertEquals(array1.length, array2.length);
        for (int i = 0; i < array1.length; i++) {
            assertEquals(array1[i], array2[i]);
        }
    }

    public void testGetQuantityText() {
        CharSequence cs;
        final Resources res = resourcesForLanguage("cs");

        cs = res.getQuantityText(R.plurals.plurals_test, 0);
        assertEquals("Some Czech dogs", cs.toString());

        cs = res.getQuantityText(R.plurals.plurals_test, 1);
        assertEquals("A Czech dog", cs.toString());

        cs = res.getQuantityText(R.plurals.plurals_test, 2);
        assertEquals("Few Czech dogs", cs.toString());

        cs = res.getQuantityText(R.plurals.plurals_test, 5);
        assertEquals("Some Czech dogs", cs.toString());

        cs = res.getQuantityText(R.plurals.plurals_test, 500);
        assertEquals("Some Czech dogs", cs.toString());

    }

    private Resources resourcesForLanguage(final String lang) {
        final Configuration config = new Configuration();
        config.updateFrom(mResources.getConfiguration());
        config.locale = new Locale(lang);
        return new Resources(mResources.getAssets(), mResources.getDisplayMetrics(), config);
    }

    public void testGetResourceEntryName() {
        assertEquals(SIMPLE, mResources.getResourceEntryName(R.configVarying.simple));
    }

    public void testGetResourceName() {
        final String fullName = mResources.getResourceName(R.configVarying.simple);
        assertEquals(COM_ANDROID_CTS_STUB_IDENTIFIER, fullName);

        final String packageName = mResources.getResourcePackageName(R.configVarying.simple);
        assertEquals(PACKAGE_NAME, packageName);

        final String typeName = mResources.getResourceTypeName(R.configVarying.simple);
        assertEquals(CONFIG_VARYING, typeName);
    }

    public void testGetStringWithIntParam() {
        checkString(R.string.formattedStringNone,
                mResources.getString(R.string.formattedStringNone),
                "Format[]");
        checkString(R.string.formattedStringOne,
                mResources.getString(R.string.formattedStringOne),
                "Format[%d]");
        checkString(R.string.formattedStringTwo, mResources.getString(R.string.formattedStringTwo),
                "Format[%3$d,%2$s]");
        // Make sure the formatted one works
        checkString(R.string.formattedStringNone,
                mResources.getString(R.string.formattedStringNone),
                "Format[]");
        checkString(R.string.formattedStringOne,
                mResources.getString(R.string.formattedStringOne, 42),
                "Format[42]");
        checkString(R.string.formattedStringTwo,
                mResources.getString(R.string.formattedStringTwo, "unused", "hi", 43),
                "Format[43,hi]");
    }

    private static void checkString(final int resid, final String actual, final String expected) {
        assertEquals("Expecting string value \"" + expected + "\" got \""
                + actual + "\" in resources 0x" + Integer.toHexString(resid),
                expected, actual);
    }

    public void testGetStringArray() {
        checkStringArray(R.array.strings, new String[] {
                "zero", "1", "here"
        });
        checkTextArray(R.array.strings, new String[] {
                "zero", "1", "here"
        });
        checkStringArray(R.array.integers, new String[] {
                null, null, null
        });
        checkTextArray(R.array.integers, new String[] {
                null, null, null
        });
    }

    private void checkStringArray(final int resid, final String[] expected) {
        final String[] res = mResources.getStringArray(resid);
        assertEquals(res.length, expected.length);
        for (int i = 0; i < expected.length; i++) {
            checkEntry(resid, i, res[i], expected[i]);
        }
    }

    private void checkEntry(final int resid, final int index, final Object res,
            final Object expected) {
        assertEquals("in resource 0x" + Integer.toHexString(resid)
                + " at index " + index, expected, res);
    }

    private void checkTextArray(final int resid, final String[] expected) {
        final CharSequence[] res = mResources.getTextArray(resid);
        assertEquals(res.length, expected.length);
        for (int i = 0; i < expected.length; i++) {
            checkEntry(resid, i, res[i], expected[i]);
        }
    }

    public void testGetValueWithID() {
        tryBoolean(R.bool.trueRes, true);
        tryBoolean(R.bool.falseRes, false);

        tryString(R.string.coerceIntegerToString, "100");
        tryString(R.string.coerceBooleanToString, "true");
        tryString(R.string.coerceColorToString, "#fff");
        tryString(R.string.coerceFloatToString, "100.0");
        tryString(R.string.coerceDimensionToString, "100px");
        tryString(R.string.coerceFractionToString, "100%");
    }

    private void tryBoolean(final int resid, final boolean expected) {
        final TypedValue v = new TypedValue();
        mContext.getResources().getValue(resid, v, true);
        assertEquals(TypedValue.TYPE_INT_BOOLEAN, v.type);
        assertEquals("Expecting boolean value " + expected + " got " + v
                + " from TypedValue: in resource 0x" + Integer.toHexString(resid),
                expected, v.data != 0);
        assertEquals("Expecting boolean value " + expected + " got " + v
                + " from getBoolean(): in resource 0x" + Integer.toHexString(resid),
                expected, mContext.getResources().getBoolean(resid));
    }

    private void tryString(final int resid, final String expected) {
        final TypedValue v = new TypedValue();
        mContext.getResources().getValue(resid, v, true);
        assertEquals(TypedValue.TYPE_STRING, v.type);
        assertEquals("Expecting string value " + expected + " got " + v
                + ": in resource 0x" + Integer.toHexString(resid),
                expected, v.string);
    }

    public void testRawResource() throws Exception {
        assertNotNull(mResources.newTheme());

        InputStream is = mResources.openRawResource(R.raw.text);
        verifyTextAsset(is);

        is = mResources.openRawResource(R.raw.text, new TypedValue());
        verifyTextAsset(is);

        assertNotNull(mResources.openRawResourceFd(R.raw.text));
    }

    static void verifyTextAsset(final InputStream is) throws IOException {
        final String expectedString = "OneTwoThreeFourFiveSixSevenEightNineTen";
        final byte[] buffer = new byte[10];

        int readCount;
        int curIndex = 0;
        while ((readCount = is.read(buffer, 0, buffer.length)) > 0) {
            for (int i = 0; i < readCount; i++) {
                assertEquals("At index " + curIndex
                            + " expected " + expectedString.charAt(curIndex)
                            + " but found " + ((char) buffer[i]),
                        buffer[i], expectedString.charAt(curIndex));
                curIndex++;
            }
        }

        readCount = is.read(buffer, 0, buffer.length);
        assertEquals("Reading end of buffer: expected readCount=-1 but got " + readCount,
                -1, readCount);

        readCount = is.read(buffer, buffer.length, 0);
        assertEquals("Reading end of buffer length 0: expected readCount=0 but got " + readCount,
                0, readCount);

        is.close();
    }
}
