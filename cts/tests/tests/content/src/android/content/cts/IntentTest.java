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

package android.content.cts;

import com.android.internal.app.ResolverActivity;
import com.android.internal.util.Objects;
import com.android.internal.util.XmlUtils;


import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.cts.MockActivity;
import android.app.cts.MockReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ServiceManager;
import android.provider.Contacts.People;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Set;

public class IntentTest extends AndroidTestCase {

    private Intent mIntent;
    private static final String TEST_ACTION = "android.content.IntentTest_test";
    private static final Uri TEST_URI = People.CONTENT_URI;
    private static final Uri ANOTHER_TEST_URI = People.CONTENT_FILTER_URI;
    private static final String TEST_EXTRA_NAME = "testExtraName";
    private Context mContext;
    private PackageManager mPm;
    private ComponentName mComponentName;
    private ComponentName mAnotherComponentName;
    private static final String TEST_TYPE = "testType";
    private static final String ANOTHER_TEST_TYPE = "anotherTestType";
    private static final String TEST_CATEGORY = "testCategory";
    private static final String ANOTHER_TEST_CATEGORY = "testAnotherCategory";
    private static final String TEST_PACKAGE = "android.content.cts";
    private static final String ANOTHER_TEST_PACKAGE = "android.database.cts";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mIntent = new Intent();
        mContext = getContext();
        mPm = mContext.getPackageManager();
        mComponentName = new ComponentName(mContext, MockActivity.class);
        mAnotherComponentName = new ComponentName(mContext, "tmp");
    }

    public void testConstructor() {
        mIntent = new Intent();
        assertNotNull(mIntent);

        Intent intent = new Intent();
        intent.setAction(TEST_ACTION);

        mIntent = new Intent(intent);
        assertNotNull(mIntent);
        assertEquals(TEST_ACTION, mIntent.getAction());

        mIntent = new Intent(TEST_ACTION);
        assertNotNull(mIntent);
        assertEquals(TEST_ACTION, mIntent.getAction());

        mIntent = new Intent(TEST_ACTION, TEST_URI);
        assertNotNull(mIntent);
        assertEquals(TEST_ACTION, mIntent.getAction());
        assertEquals(TEST_URI, mIntent.getData());

        mIntent = new Intent(mContext, MockActivity.class);
        assertNotNull(mIntent);
        assertEquals(mComponentName, mIntent.getComponent());

        mIntent = new Intent(TEST_ACTION, TEST_URI, mContext, MockActivity.class);
        assertNotNull(mIntent);
        assertEquals(TEST_ACTION, mIntent.getAction());
        assertEquals(TEST_URI, mIntent.getData());
        assertEquals(mComponentName, mIntent.getComponent());
    }

    public void testRemoveExtra() {
        mIntent = new Intent();
        mIntent.putExtra(TEST_EXTRA_NAME, "testvalue");
        assertNotNull(mIntent.getStringExtra(TEST_EXTRA_NAME));
        mIntent.removeExtra(TEST_EXTRA_NAME);
        assertNull(mIntent.getStringExtra(TEST_EXTRA_NAME));
    }

    public void testGetCharSequenceExtra() {
        final CharSequence expected = "CharSequencetest";
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getCharSequenceExtra(TEST_EXTRA_NAME));
    }

    public void testReadFromParcel() {
        mIntent.setAction(TEST_ACTION);
        mIntent.setData(TEST_URI);
        mIntent.setType(TEST_TYPE);
        mIntent.setFlags(0);
        mIntent.setComponent(mComponentName);
        mIntent.addCategory(TEST_CATEGORY);
        final Parcel parcel = Parcel.obtain();
        mIntent.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        final Intent target = new Intent();
        target.readFromParcel(parcel);
        assertEquals(mIntent.getAction(), target.getAction());
        assertEquals(mIntent.getData(), target.getData());
        assertEquals(mIntent.getFlags(), target.getFlags());
        assertEquals(mIntent.getComponent(), target.getComponent());
        assertEquals(mIntent.getCategories(), target.getCategories());
        assertEquals(mIntent.toURI(), target.toURI());
    }

    public void testGetParcelableArrayListExtra() {
        final ArrayList<Intent> expected = new ArrayList<Intent>();
        Intent intent = new Intent(TEST_ACTION);
        expected.add(intent);

        mIntent.putParcelableArrayListExtra(TEST_EXTRA_NAME, expected);
        final ArrayList<Intent> target = mIntent.getParcelableArrayListExtra(TEST_EXTRA_NAME);
        assertEquals(expected.size(), target.size());
        assertEquals(expected, target);
    }

    public void testFilterHashCode() {
        mIntent.filterHashCode();
    }

    public void testGetCategories() {
        mIntent.addCategory(TEST_CATEGORY);
        final Set<String> target = mIntent.getCategories();
        assertEquals(TEST_CATEGORY, target.toArray()[0]);
    }

    public void testGetScheme() {
        assertNull(mIntent.getScheme());
        mIntent.setData(TEST_URI);
        assertEquals(TEST_URI.getScheme(), mIntent.getScheme());
    }

    public void testGetIntegerArrayListExtra() {
        final ArrayList<Integer> expected = new ArrayList<Integer>();
        expected.add(0);
        mIntent.putIntegerArrayListExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getIntegerArrayListExtra(TEST_EXTRA_NAME));
    }

    public void testHasExtra() {
        mIntent = new Intent();
        assertFalse(mIntent.hasExtra(TEST_EXTRA_NAME));
        mIntent.putExtra(TEST_EXTRA_NAME, "test");
        assertTrue(mIntent.hasExtra(TEST_EXTRA_NAME));
    }

    public void testGetIntArrayExtra() {
        final int[] expected = { 1, 2, 3 };
        assertNull(mIntent.getIntArrayExtra(TEST_EXTRA_NAME));
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getIntArrayExtra(TEST_EXTRA_NAME));
    }

    public void testSetClassName1() {
        final Intent intent = mIntent.setClassName(mContext, MockActivity.class.getName());
        assertEquals(mComponentName, mIntent.getComponent());
        assertSame(mIntent, intent);
    }

    public void testSetClassName2() {
        mIntent.setClassName(mContext.getPackageName(), MockActivity.class.getName());
        assertEquals(mComponentName, mIntent.getComponent());
    }

    public void testGetIntExtra() {
        final int expected = 0;
        mIntent = new Intent();
        assertEquals(expected, mIntent.getIntExtra(TEST_EXTRA_NAME, expected));
        mIntent.putExtra(TEST_EXTRA_NAME, 100);
        assertEquals(100, mIntent.getIntExtra(TEST_EXTRA_NAME, 1));

    }

    public void testPutIntegerArrayListExtra() {
        final ArrayList<Integer> expected = new ArrayList<Integer>();
        expected.add(0);
        mIntent = new Intent();
        mIntent.putIntegerArrayListExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getIntegerArrayListExtra(TEST_EXTRA_NAME));
    }

    public void testAccessType() {
        mIntent.setType(TEST_TYPE);
        assertEquals(TEST_TYPE, mIntent.getType());
    }

    public void testGetBundleExtra() {
        final Bundle expected = new Bundle();
        expected.putBoolean("testTrue", true);
        mIntent.putExtras(expected);
        mIntent.putExtra(TEST_EXTRA_NAME, expected);

        assertEquals(expected, mIntent.getBundleExtra(TEST_EXTRA_NAME));
    }

    public void testGetCharArrayExtra() {
        final char[] expected = { 'a', 'b', 'c' };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        final char[] actual= mIntent.getCharArrayExtra(TEST_EXTRA_NAME);
        assertEquals(expected.length, actual.length);
        assertEquals(expected[0], actual[0]);
        assertEquals(expected[1], actual[1]);
        assertEquals(expected[2], actual[2]);
    }

    public void testGetDoubleArrayExtra() {
        final double[] expected = { 1d, 2d };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getDoubleArrayExtra(TEST_EXTRA_NAME));
    }

    public void testPutStringArrayListExtra() {
        final ArrayList<String> expected = new ArrayList<String>();
        expected.add("testString");
        mIntent.putStringArrayListExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getStringArrayListExtra(TEST_EXTRA_NAME));
    }

    public void testResolveType1() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        assertNull(mIntent.resolveType(mContext));
        mIntent.setType(TEST_TYPE);
        assertEquals(TEST_TYPE, mIntent.resolveType(mContext));
        mIntent.setType(null);
        mIntent.setData(TEST_URI);
        assertEquals(contentResolver.getType(TEST_URI), mIntent.resolveType(mContext));
        mIntent.setData(Uri.parse("test"));
        assertNull(mIntent.resolveType(mContext));
    }

    public void testResolveType2() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        assertNull(mIntent.resolveType(contentResolver));
        mIntent.setType(TEST_TYPE);
        assertEquals(TEST_TYPE, mIntent.resolveType(contentResolver));
        mIntent.setType(null);
        mIntent.setData(TEST_URI);
        assertEquals(contentResolver.getType(TEST_URI), mIntent.resolveType(contentResolver));
        mIntent.setData(Uri.parse("test"));
        assertNull(mIntent.resolveType(contentResolver));
    }

    public void testAccessComponent() {
        mIntent.setComponent(mComponentName);
        assertEquals(mComponentName, mIntent.getComponent());
    }

    public void testGetDataString() {
        assertNull(mIntent.getDataString());
        mIntent.setData(TEST_URI);
        assertEquals(TEST_URI.toString(), mIntent.getDataString());
    }

    public void testHasCategory() {
        assertFalse(mIntent.hasCategory(TEST_CATEGORY));
        mIntent.addCategory(TEST_CATEGORY);
        assertTrue(mIntent.hasCategory(TEST_CATEGORY));
    }

    public void testGetLongArrayExtra() {
        final long[] expected = { 1l, 2l, 3l };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getLongArrayExtra(TEST_EXTRA_NAME));
    }

    public void testParseIntent() throws XmlPullParserException, IOException,
        NameNotFoundException {
        mIntent = null;
        XmlResourceParser parser = null;
        AttributeSet attrs = null;
        ActivityInfo ai = null;
        try {
            mIntent = Intent.parseIntent(mContext.getResources(), parser, attrs);
            fail("should thow exception!");
        } catch (NullPointerException e) {
            // expected
        }

        ai = mContext.getPackageManager().getActivityInfo(mComponentName,
                PackageManager.GET_META_DATA);
        parser = ai.loadXmlMetaData(mContext.getPackageManager(), "android.app.alias");

        attrs = Xml.asAttributeSet(parser);
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && type != XmlPullParser.START_TAG) {
        }

        String nodeName = parser.getName();
        if (!"alias".equals(nodeName)) {
            throw new RuntimeException();
        }

        int outerDepth = parser.getDepth();
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }

            nodeName = parser.getName();
            if ("intent".equals(nodeName)) {
                mIntent = Intent.parseIntent(mContext.getResources(), parser, attrs);
            } else {
                XmlUtils.skipCurrentTag(parser);
            }
        }

        assertNotNull(mIntent);
        assertEquals("android.intent.action.MAIN", mIntent.getAction());
        assertEquals(Uri.parse("http://www.google.com/"), mIntent.getData());
    }

    public void testSetClass() {
        assertNull(mIntent.getComponent());
        mIntent.setClass(mContext, MockActivity.class);
        assertEquals(mComponentName, mIntent.getComponent());
    }

    public void testResolveTypeIfNeeded() {
        ContentResolver contentResolver = mContext.getContentResolver();
        assertNull(mIntent.resolveTypeIfNeeded(contentResolver));
        mIntent.setType(TEST_TYPE);
        assertEquals(TEST_TYPE, mIntent.resolveTypeIfNeeded(contentResolver));

        mIntent.setType(null);
        mIntent.setComponent(mComponentName);
        assertEquals(null, mIntent.resolveTypeIfNeeded(contentResolver));

        mIntent.setType(TEST_TYPE);
        mIntent.setComponent(mComponentName);
        assertEquals(TEST_TYPE, mIntent.resolveTypeIfNeeded(contentResolver));

        mIntent.setType(null);
        mIntent.setData(TEST_URI);
        assertNull(mIntent.resolveTypeIfNeeded(contentResolver));
    }

    public void testPutExtra1() {
        assertFalse(mIntent.getBooleanExtra(TEST_EXTRA_NAME, false));
        mIntent.putExtra(TEST_EXTRA_NAME, true);
        assertTrue(mIntent.getBooleanExtra(TEST_EXTRA_NAME, false));
        mIntent.putExtra(TEST_EXTRA_NAME, false);
        assertFalse(mIntent.getBooleanExtra(TEST_EXTRA_NAME, false));
    }

    public void testPutExtra2() {
        final byte expected = Byte.valueOf("1");
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getByteExtra(TEST_EXTRA_NAME, Byte.valueOf("1")));
    }

    public void testPutExtra3() {
        assertEquals('a', mIntent.getCharExtra(TEST_EXTRA_NAME, 'a'));
        final char expected = 'a';
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getCharExtra(TEST_EXTRA_NAME, 'a'));
    }

    public void testPutExtra4() {
        final Short expected = Short.valueOf("2");
        assertEquals(Short.valueOf("1").shortValue(), mIntent.getShortExtra(
                TEST_EXTRA_NAME, Short.valueOf("1")));
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected.shortValue(), mIntent.getShortExtra(TEST_EXTRA_NAME, Short.valueOf("1")));
    }

    public void testPutExtra5() {
        final int expected = 2;
        assertEquals(1, mIntent.getIntExtra(TEST_EXTRA_NAME, 1));
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getIntExtra(TEST_EXTRA_NAME, 1));
    }

    public void testPutExtra6() {
        final long expected = 2l;
        assertEquals(1l, mIntent.getLongExtra(TEST_EXTRA_NAME, 1l));
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getLongExtra(TEST_EXTRA_NAME, 1l));
    }

    public void testPutExtra7() {
        final float expected = 2f;
        assertEquals(1f, mIntent.getFloatExtra(TEST_EXTRA_NAME, 1f));
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getFloatExtra(TEST_EXTRA_NAME, 1f));
    }

    public void testPutExtra8() {
        final double expected = 2d;
        assertEquals(1d, mIntent.getDoubleExtra(TEST_EXTRA_NAME, 1d));
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getDoubleExtra(TEST_EXTRA_NAME, 1d));
    }

    public void testPutExtra9() {
        final String expected = "testString";
        assertNull(mIntent.getStringExtra(TEST_EXTRA_NAME));
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getStringExtra(TEST_EXTRA_NAME));
    }

    public void testPutExtra10() {
        final CharSequence expected = "testString";
        assertNull(mIntent.getCharSequenceExtra(TEST_EXTRA_NAME));
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getCharSequenceExtra(TEST_EXTRA_NAME));
    }

    public void testPutExtra11() {
        final Intent expected = new Intent(TEST_ACTION);
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getParcelableExtra(TEST_EXTRA_NAME));
    }

    public void testPutExtra12() {
        final Intent[] expected = { new Intent(TEST_ACTION), new Intent(mContext, MockActivity.class) };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getParcelableArrayExtra(TEST_EXTRA_NAME));
    }

    public void testPutExtra13() {
        final TestSerializable expected = new TestSerializable();
        expected.Name = "testName";
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getSerializableExtra(TEST_EXTRA_NAME));
        TestSerializable target = (TestSerializable) mIntent.getSerializableExtra(TEST_EXTRA_NAME);
        assertEquals(expected.Name, target.Name);
    }

    public void testPutExtra14() {
        final boolean[] expected = { true, true, false };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getBooleanArrayExtra(TEST_EXTRA_NAME));
    }

    public void testPutExtra15() {
        final byte[] expected = TEST_ACTION.getBytes();
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getByteArrayExtra(TEST_EXTRA_NAME));
    }

    public void testPutExtra16() {
        final short[] expected = { 1, 2, 3 };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getShortArrayExtra(TEST_EXTRA_NAME));
    }

    public void testPutExtra17() {
        final char[] expected = { '1', '2', '3' };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getCharArrayExtra(TEST_EXTRA_NAME));
    }

    public void testPutExtra18() {
        final int[] expected = { 1, 2, 3 };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getIntArrayExtra(TEST_EXTRA_NAME));
    }

    public void testPutExtra19() {
        final long[] expected = { 1l, 2l, 3l };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getLongArrayExtra(TEST_EXTRA_NAME));
    }

    public void testPutExtra20() {
        final float[] expected = { 1f, 2f, 3f };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getFloatArrayExtra(TEST_EXTRA_NAME));
    }

    public void testPutExtra21() {
        final double[] expected = { 1d, 2d, 3d };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getDoubleArrayExtra(TEST_EXTRA_NAME));
    }

    public void testPutExtra22() {
        final String[] expected = { "1d", "2d", "3d" };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getStringArrayExtra(TEST_EXTRA_NAME));
    }

    public void testPutExtra23() {
        final Bundle expected = new Bundle();
        expected.putString("key", "value");
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getBundleExtra(TEST_EXTRA_NAME));
    }

    @SuppressWarnings("deprecation")
    public void testPutExtra24() {
        final IBinder expected = ServiceManager.getService("activity");
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getIBinderExtra(TEST_EXTRA_NAME));
    }

    public void testAddCategory() {
        assertFalse(mIntent.hasCategory(TEST_CATEGORY));
        mIntent.addCategory(TEST_CATEGORY);
        assertTrue(mIntent.hasCategory(TEST_CATEGORY));
    }

    public void testPutParcelableArrayListExtra() {
        ArrayList<Intent> expected = new ArrayList<Intent>();
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getParcelableArrayListExtra(TEST_EXTRA_NAME));
    }

    public void testToString() {
        assertNotNull(mIntent.toString());
    }

    public void testAccessData() {
        mIntent.setData(TEST_URI);
        assertEquals(TEST_URI, mIntent.getData());
    }

    public void testSetExtrasClassLoader() {
    }

    public void testGetStringArrayListExtra() {
        final ArrayList<String> expected = new ArrayList<String>();
        expected.add("testString");
        mIntent.putStringArrayListExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getStringArrayListExtra(TEST_EXTRA_NAME));
    }

    public void testGetCharSequenceArrayListExtra() {
        final ArrayList<CharSequence> expected = new ArrayList<CharSequence>();
        expected.add("testCharSequence");
        mIntent.putCharSequenceArrayListExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getCharSequenceArrayListExtra(TEST_EXTRA_NAME));
    }

    public void testResolveActivityInfo() throws NameNotFoundException {
        final PackageManager pm = mContext.getPackageManager();
        assertEquals(null, mIntent.resolveActivityInfo(pm, 1));
        mIntent.setComponent(mComponentName);
        ActivityInfo target = null;

        target = pm.getActivityInfo(mComponentName, 1);
        assertEquals(target.targetActivity, mIntent.resolveActivityInfo(pm, 1).targetActivity);
    }

    public void testGetParcelableExtra() {
        final Intent expected = new Intent(TEST_ACTION);
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getParcelableExtra(TEST_EXTRA_NAME));
    }

    public void testAccessAction() {
        mIntent.setAction(TEST_ACTION);
        assertEquals(TEST_ACTION, mIntent.getAction());
    }

    public void testAddFlags() {
        final int flag = 1;
        int expected = 0;
        mIntent.addFlags(flag);
        expected |= flag;
        assertEquals(expected, mIntent.getFlags());
    }

    public void testDescribeContents() {
        final int expected = 0;
        assertEquals(expected, mIntent.describeContents());
        mIntent.putExtra(TEST_EXTRA_NAME, "test");
        assertEquals(mIntent.getExtras().describeContents(), mIntent.describeContents());
    }

    public void testGetShortExtra() {

        final Short expected = Short.valueOf("2");
        assertEquals(Short.valueOf("1").shortValue(), mIntent.getShortExtra(
                TEST_EXTRA_NAME, Short.valueOf("1")));
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected.shortValue(), mIntent.getShortExtra(TEST_EXTRA_NAME, Short.valueOf("1")));
    }

    public void testClone() {
        mIntent.setAction(TEST_ACTION);
        mIntent.setClass(mContext, MockActivity.class);
        mIntent.setComponent(mComponentName);
        mIntent.setDataAndType(TEST_URI, TEST_TYPE);
        mIntent.addCategory(TEST_CATEGORY);
        final String key = "testkey";
        final String excepted = "testValue";
        mIntent.putExtra(key, excepted);
        Intent actual = (Intent) mIntent.clone();
        assertEquals(mComponentName, actual.getComponent());
        assertEquals(TEST_ACTION, actual.getAction());
        assertEquals(mComponentName, actual.getComponent());
        assertEquals(TEST_URI, actual.getData());
        assertEquals(TEST_TYPE, actual.getType());
        assertEquals(TEST_CATEGORY, (String) (actual.getCategories().toArray()[0]));
        assertEquals(excepted, actual.getStringExtra(key));
    }

    public void testGetDoubleExtra() {
        final double expected = 2d;
        assertEquals(1d, mIntent.getDoubleExtra(TEST_EXTRA_NAME, 1d));
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getDoubleExtra(TEST_EXTRA_NAME, 1d));
    }

    public void testCloneFilter() {
        mIntent.setAction(TEST_ACTION);
        mIntent.setClass(mContext, MockActivity.class);
        mIntent.setComponent(mComponentName);
        mIntent.setDataAndType(TEST_URI, TEST_TYPE);
        mIntent.addCategory(TEST_CATEGORY);
        final String key = "testkey";
        mIntent.putExtra(key, "testValue");
        Intent actual = mIntent.cloneFilter();
        assertEquals(mComponentName, actual.getComponent());
        assertEquals(TEST_ACTION, actual.getAction());
        assertEquals(mComponentName, actual.getComponent());
        assertEquals(TEST_URI, actual.getData());
        assertEquals(TEST_TYPE, actual.getType());
        assertEquals(TEST_CATEGORY, (String) (actual.getCategories().toArray()[0]));
        assertNull(actual.getStringExtra(key));
    }

    public void testGetIntentOld() throws URISyntaxException {
        String uri = "test";
        mIntent = Intent.getIntentOld(uri);
        assertEquals(Intent.ACTION_VIEW, mIntent.getAction());

        mIntent = null;
        try {
            uri = "test#action(test)categories(test)type(mtype)launchFlags(test)extras(test";
            mIntent = Intent.getIntentOld(uri);
            fail("should throw URISyntaxException.");
        } catch (Exception e) {
            // expected
        }

        final String compnent =
                "component(" + mContext.getPackageName() + "!" + MockActivity.class.getName() + ")";
        uri = "testdata#action(test)categories(test!test2)type(mtype)launchFlags(1)" + compnent
                + "extras(Stest=testString!btestbyte=1!"
                + "Btestboolean=true!ctestchar=a!dtestdouble=1d!"
                + "itestint=1!ltestlong=1!stestshort=1!ftestfloat=1f)";
        mIntent = Intent.getIntentOld(uri);
        assertEquals("test", mIntent.getAction());
        assertEquals("testdata", mIntent.getData().toString());
        assertEquals(mComponentName, mIntent.getComponent());
        assertEquals("test", (String) (mIntent.getCategories().toArray()[0]));
        assertEquals("mtype", mIntent.getType());
        assertEquals(1, mIntent.getFlags());
        assertEquals("testString", mIntent.getStringExtra("test"));
        assertTrue(mIntent.getBooleanExtra("testboolean", false));
        final byte b = 1;
        final byte defaulttByte = 2;
        assertEquals(b, mIntent.getByteExtra("testbyte", defaulttByte));
        assertEquals('a', mIntent.getCharExtra("testchar", 'b'));
        final float testFloat = 1f;
        assertEquals(testFloat, mIntent.getFloatExtra("testfloat", 2f));
        final double testDouble = 1d;
        assertEquals(testDouble, mIntent.getDoubleExtra("testdouble", 2d));

        final long testLong = 1;
        assertEquals(testLong, mIntent.getLongExtra("testlong", 2l));

        final short testShort = 1;
        final short defaultShort = 2;
        assertEquals(testShort, mIntent.getShortExtra("testshort", defaultShort));
        assertEquals(1, mIntent.getIntExtra("testint", 2));
    }

    public void testGetParcelableArrayExtra() {
        final Intent[] expected = { new Intent(TEST_ACTION), new Intent(mContext, MockActivity.class) };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getParcelableArrayExtra(TEST_EXTRA_NAME));
    }

    public void testResolveActivityEmpty() {
        final Intent emptyIntent = new Intent();

        // Empty intent shouldn't resolve to anything
        final ComponentName target = emptyIntent.resolveActivity(mPm);
        assertNull(target);
    }

    public void testResolveActivitySingleMatch() {
        final Intent intent = new Intent("com.android.cts.content.action.TEST_ACTION");
        intent.addCategory("com.android.cts.content.category.TEST_CATEGORY");

        // Should only have one activity responding to narrow category
        final ComponentName target = intent.resolveActivity(mPm);
        assertEquals("com.android.cts.content", target.getPackageName());
        assertEquals("android.app.cts.MockActivity", target.getClassName());
    }

    public void testResolveActivityShortcutMatch() {
        final Intent intent = new Intent("com.android.cts.content.action.TEST_ACTION");
        intent.setComponent(
                new ComponentName("com.android.cts.content", "android.app.cts.MockActivity2"));

        // Multiple activities match, but we asked for explicit component
        final ComponentName target = intent.resolveActivity(mPm);
        assertEquals("com.android.cts.content", target.getPackageName());
        assertEquals("android.app.cts.MockActivity2", target.getClassName());
    }

    public void testResolveActivityMultipleMatch() {
        final Intent intent = new Intent("com.android.cts.content.action.TEST_ACTION");

        // Should have multiple activities, resulting in resolver dialog
        final ComponentName target = intent.resolveActivity(mPm);
        final String pkgName = target.getPackageName();
        assertFalse("com.android.cts.content".equals(pkgName));

        // Whoever they are must be able to set preferred activities
        if (!"android".equals(pkgName)) {
            if (mPm.checkPermission(android.Manifest.permission.SET_PREFERRED_APPLICATIONS, pkgName)
                    != PackageManager.PERMISSION_GRANTED) {
                fail("Resolved target " + target
                        + " doesn't have SET_PREFERRED_APPLICATIONS permission");
            }
        }
    }

    public void testGetCharExtra() {
        assertEquals('a', mIntent.getCharExtra(TEST_EXTRA_NAME, 'a'));
        final char expected = 'b';
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getCharExtra(TEST_EXTRA_NAME, 'a'));
    }

    public void testGetIntent() throws URISyntaxException {
        mIntent = Intent.getIntent("test#");
        assertEquals(Intent.ACTION_VIEW, mIntent.getAction());

        try {
            String uri = "#Intent;action=android.content.IntentTest_test;"
                    + "category=testCategory;type=testtype;launchFlags=0x1;"
                    + "component=com.android/.app.MockActivity;K.testExtraName=1;end";
            mIntent = Intent.getIntent(uri);
            fail("should throw URISyntaxException.");
        } catch (Exception e) {
            // expected
        }
        mIntent = new Intent();

        String uri = mIntent.toURI();
        Intent target = Intent.getIntent(uri);
        assertEquals(Intent.ACTION_VIEW, target.getAction());

        mIntent.setAction(TEST_ACTION);
        uri = mIntent.toURI();
        target = Intent.getIntent(uri);
        assertEquals(TEST_ACTION, target.getAction());

        mIntent.setData(TEST_URI);
        uri = mIntent.toURI();
        target = Intent.getIntent(uri);
        assertEquals(TEST_URI, target.getData());

        mIntent.setComponent(mComponentName);
        uri = mIntent.toURI();
        target = Intent.getIntent(uri);
        assertEquals(mComponentName, target.getComponent());

        mIntent.addCategory(TEST_CATEGORY);
        uri = mIntent.toURI();
        target = Intent.getIntent(uri);
        assertEquals(1, target.getCategories().size());
        assertEquals(TEST_CATEGORY, (String) (target.getCategories().toArray()[0]));

        mIntent.setType(TEST_TYPE);
        uri = mIntent.toURI();
        target = Intent.getIntent(uri);
        assertEquals(TEST_TYPE, target.getType());

        mIntent.setFlags(1);
        uri = mIntent.toURI();
        target = Intent.getIntent(uri);
        assertEquals(1, target.getFlags());

        String stringValue = "testString";
        mIntent.putExtra(TEST_EXTRA_NAME, stringValue);
        uri = mIntent.toURI();
        target = Intent.getIntent(uri);
        assertEquals(stringValue, target.getStringExtra(TEST_EXTRA_NAME));

        mIntent.putExtra(TEST_EXTRA_NAME, true);
        uri = mIntent.toURI();
        target = Intent.getIntent(uri);
        assertTrue(target.getBooleanExtra(TEST_EXTRA_NAME, false));

        final byte b = 1;
        mIntent.putExtra(TEST_EXTRA_NAME, b);

        uri = mIntent.toURI();
        target = Intent.getIntent(uri);
        final byte defaulttByte = 2;
        assertEquals(b, target.getByteExtra(TEST_EXTRA_NAME, defaulttByte));

        final char testChar = 'a';
        mIntent.putExtra(TEST_EXTRA_NAME, testChar);
        uri = mIntent.toURI();
        target = Intent.getIntent(uri);
        assertEquals(testChar, target.getCharExtra(TEST_EXTRA_NAME, 'b'));

        final double testDouble = 1;
        mIntent.putExtra(TEST_EXTRA_NAME, testDouble);
        uri = mIntent.toURI();
        target = Intent.getIntent(uri);
        assertEquals(testDouble, target.getDoubleExtra(TEST_EXTRA_NAME, 2));

        final int testInt = 1;
        mIntent.putExtra(TEST_EXTRA_NAME, testInt);
        uri = mIntent.toURI();
        target = Intent.getIntent(uri);
        assertEquals(testInt, target.getIntExtra(TEST_EXTRA_NAME, 2));

        final long testLong = 1l;
        mIntent.putExtra(TEST_EXTRA_NAME, testLong);
        uri = mIntent.toURI();
        target = Intent.getIntent(uri);
        assertEquals(testLong, target.getLongExtra(TEST_EXTRA_NAME, 2l));

        final short testShort = 1;
        final short defaultShort = 2;
        mIntent.putExtra(TEST_EXTRA_NAME, testShort);
        uri = mIntent.toURI();
        target = Intent.getIntent(uri);
        assertEquals(testShort, target.getShortExtra(TEST_EXTRA_NAME, defaultShort));
    }

    public void testToURI() {
        mIntent.setFlags(0);
        assertEquals("#Intent;end", mIntent.toURI());

        mIntent.setData(TEST_URI);
        assertTrue(mIntent.toURI().indexOf(TEST_URI.toString()) != -1);

        mIntent.setAction(TEST_ACTION);
        assertTrue(mIntent.toURI().indexOf("action=" + TEST_ACTION) != -1);

        mIntent.addCategory(TEST_CATEGORY);
        assertTrue(mIntent.toURI().indexOf("category=") != -1);

        mIntent.setType(TEST_TYPE);

        assertTrue(mIntent.toURI().indexOf("type=" + TEST_TYPE) != -1);

        mIntent.setFlags(1);
        assertFalse(mIntent.toURI().indexOf("launchFlags=" + Integer.toHexString(1)) != -1);

        mIntent.setComponent(mComponentName);
        assertTrue(mIntent.toURI().indexOf(
                "component=" + mComponentName.flattenToShortString()) != -1);

        final String stringValue = "testString";
        mIntent.putExtra(TEST_EXTRA_NAME, stringValue);

        assertTrue(mIntent.toURI().indexOf(getString("S", TEST_EXTRA_NAME, stringValue)) != -1);

        mIntent.putExtra(TEST_EXTRA_NAME, true);

        assertTrue(mIntent.toURI().indexOf(getString("B", TEST_EXTRA_NAME, true)) != -1);

        final byte b = 1;
        mIntent.putExtra(TEST_EXTRA_NAME, b);
        assertTrue(mIntent.toURI().indexOf(getString("b", TEST_EXTRA_NAME, b)) != -1);

        final Character testChar = 'a';
        mIntent.putExtra(TEST_EXTRA_NAME, testChar);

        assertTrue(mIntent.toURI().indexOf(getString("c", TEST_EXTRA_NAME, testChar)) != -1);

        final double testDouble = 1;
        mIntent.putExtra(TEST_EXTRA_NAME, testDouble);
        assertTrue(mIntent.toURI().indexOf(getString("d", TEST_EXTRA_NAME, testDouble)) != -1);

        final int testInt = 1;
        mIntent.putExtra(TEST_EXTRA_NAME, testInt);
        assertTrue(mIntent.toURI().indexOf(getString("i", TEST_EXTRA_NAME, testInt)) != -1);

        final long testLong = 1l;
        mIntent.putExtra(TEST_EXTRA_NAME, testLong);
        assertTrue(mIntent.toURI().indexOf(getString("l", TEST_EXTRA_NAME, testLong)) != -1);
        final short testShort = 1;
        mIntent.putExtra(TEST_EXTRA_NAME, testShort);
        assertTrue(mIntent.toURI().indexOf(getString("s", TEST_EXTRA_NAME, testShort)) != -1);
        assertTrue(mIntent.toURI().indexOf("end") != -1);
    }

    private String getString(String entryType, String key, Object value) {
        StringBuilder uri = new StringBuilder();
        uri.append(entryType);
        uri.append('.');
        uri.append(Uri.encode(key));
        uri.append('=');
        uri.append(Uri.encode(value.toString()));
        return uri.toString();
    }

    public void testAccessFlags() {
        int expected = 1;
        mIntent.setFlags(expected);
        assertEquals(expected, mIntent.getFlags());
    }

    public void testCreateChooser() {
        Intent target = Intent.createChooser(mIntent, null);
        assertEquals(Intent.ACTION_CHOOSER, target.getAction());
        Intent returnIntent = (Intent) target.getParcelableExtra(Intent.EXTRA_INTENT);
        assertEquals(mIntent.toString(), returnIntent.toString());
        assertEquals(mIntent.toURI(), returnIntent.toURI());
        assertNull(returnIntent.getStringExtra(Intent.EXTRA_INTENT));
        final String title = "title String";
        target = Intent.createChooser(mIntent, title);
        assertEquals(title, target.getStringExtra(Intent.EXTRA_TITLE));
    }

    public void testGetFloatArrayExtra() {
        final float[] expected = { 1f, 2f, 3f };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getFloatArrayExtra(TEST_EXTRA_NAME));
    }

    public void testSetDataAndType() {
        mIntent.setDataAndType(TEST_URI, TEST_TYPE);
        assertEquals(TEST_URI, mIntent.getData());
        assertEquals(TEST_TYPE, mIntent.getType());
    }

    public void testSetData() {
        mIntent.setData(TEST_URI);
        assertEquals(TEST_URI, mIntent.getData());
        assertNull(mIntent.getType());

        mIntent.setType(TEST_TYPE);
        mIntent.setData(TEST_URI);
        assertEquals(TEST_URI, mIntent.getData());
        assertNull(mIntent.getType());
    }

    public void testSetType() {
        mIntent.setType(TEST_TYPE);
        assertEquals(TEST_TYPE, mIntent.getType());
        assertNull(mIntent.getData());

        mIntent.setData(TEST_URI);
        mIntent.setType(TEST_TYPE);
        assertEquals(TEST_TYPE, mIntent.getType());
        assertNull(mIntent.getData());
    }

    public void testGetStringExtra() {
        final String expected = "testString";
        assertNull(mIntent.getStringExtra(TEST_EXTRA_NAME));
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getStringExtra(TEST_EXTRA_NAME));
    }

    /**
     * Test that fillIn has no effect when no fields are set.
     */
    public void testFillIn_blank() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        assertEquals(0, destIntent.fillIn(sourceIntent, Intent.FILL_IN_ACTION));
        assertEquals(0, destIntent.fillIn(sourceIntent, 0));
        assertNull(destIntent.getAction());
    }

    /**
     * Test that fillIn copies the action field.
     */
    public void testFillIn_action() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        // test action copied when null
        sourceIntent.setAction(TEST_ACTION);
        assertEquals(Intent.FILL_IN_ACTION, destIntent.fillIn(sourceIntent, 0));
        assertEquals(TEST_ACTION, destIntent.getAction());
    }

    /**
     * Test that fillIn does not copy action when its already set in target Intent.
     */
    public void testFillIn_actionSet() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        final String newAction = "foo";
        sourceIntent = new Intent();
        sourceIntent.setAction(newAction);
        destIntent.setAction(TEST_ACTION);

        assertEquals(0, destIntent.fillIn(sourceIntent, 0));
        assertEquals(TEST_ACTION, destIntent.getAction());
    }

    /**
     * Test that fillIn copies action when {@link Intent#FILL_IN_ACTION} flag is set.
     */
    public void testFillIn_actionOverride() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        final String newAction = "foo";
        sourceIntent = new Intent();
        sourceIntent.setAction(newAction);
        destIntent.setAction(TEST_ACTION);

        assertEquals(Intent.FILL_IN_ACTION, destIntent.fillIn(sourceIntent, Intent.FILL_IN_ACTION));
        assertEquals(newAction, destIntent.getAction());
    }

    /**
     * Test that fillIn copies data.
     */
    public void testFillIn_data() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        sourceIntent.setData(TEST_URI);
        assertEquals(Intent.FILL_IN_DATA, destIntent.fillIn(sourceIntent, 0));
        assertEquals(TEST_URI, destIntent.getData());
    }

    /**
     * Test that fillIn does not copy data when already its already set in target Intent.
     */
    public void testFillIn_dataSet() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        sourceIntent.setData(TEST_URI);
        destIntent.setData(ANOTHER_TEST_URI);
        assertEquals(0, destIntent.fillIn(sourceIntent, 0));
        assertEquals(ANOTHER_TEST_URI, destIntent.getData());
    }

    /**
     * Test that fillIn overrides data when {@link Intent#FILL_IN_DATA} flag is set.
     */
    public void testFillIn_dataOverride() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        sourceIntent.setData(TEST_URI);
        destIntent.setData(ANOTHER_TEST_URI);
        assertEquals(Intent.FILL_IN_DATA, destIntent.fillIn(sourceIntent, Intent.FILL_IN_DATA));
        assertEquals(TEST_URI, destIntent.getData());
    }

    /**
     * Test that fillIn copies data type.
     */
    public void testFillIn_dataType() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        sourceIntent.setType(TEST_TYPE);
        assertEquals(Intent.FILL_IN_DATA, destIntent.fillIn(sourceIntent, 0));
        assertEquals(TEST_TYPE, destIntent.getType());
    }

    /**
     * Test that fillIn does not copy data type when already its already set in target Intent.
     */
    public void testFillIn_dataTypeSet() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        sourceIntent.setType(TEST_TYPE);
        destIntent.setType(ANOTHER_TEST_TYPE);
        assertEquals(0, destIntent.fillIn(sourceIntent, 0));
        assertEquals(ANOTHER_TEST_TYPE, destIntent.getType());
    }

    /**
     * Test that fillIn overrides data type when {@link Intent#FILL_IN_DATA} flag is set.
     */
    public void testFillIn_dataTypeOverride() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        sourceIntent.setType(TEST_TYPE);
        destIntent.setType(ANOTHER_TEST_TYPE);
        assertEquals(Intent.FILL_IN_DATA, destIntent.fillIn(sourceIntent, Intent.FILL_IN_DATA));
        assertEquals(TEST_TYPE, destIntent.getType());
    }

    /**
     * Test component is not copied by fillIn method when {@link Intent#FILL_IN_COMPONENT} flag is
     * not set.
     */
    public void testFillIn_componentNoCopy() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        sourceIntent.setComponent(mComponentName);
        assertEquals(0, destIntent.fillIn(sourceIntent, 0));
        assertEquals(null, destIntent.getComponent());
    }

    /**
     * Test that fillIn copies component when {@link Intent#FILL_IN_COMPONENT} flag is set.
     */
    public void testFillIn_componentOverride() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        sourceIntent.setComponent(mComponentName);
        destIntent.setComponent(mAnotherComponentName);
        assertEquals(Intent.FILL_IN_COMPONENT, destIntent.fillIn(sourceIntent,
                Intent.FILL_IN_COMPONENT));
        assertEquals(mComponentName, destIntent.getComponent());
    }

    /**
     * Test that fillIn copies categories.
     */
    public void testFillIn_category() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        // add two categories to ensure all categories are copied
        sourceIntent.addCategory(TEST_CATEGORY);
        sourceIntent.addCategory(ANOTHER_TEST_CATEGORY);
        assertEquals(Intent.FILL_IN_CATEGORIES, destIntent.fillIn(sourceIntent, 0));
        assertEquals(2, destIntent.getCategories().size());
        assertTrue(destIntent.getCategories().contains(TEST_CATEGORY));
        assertTrue(destIntent.getCategories().contains(ANOTHER_TEST_CATEGORY));
    }

    /**
     * Test fillIn does not copy categories by default when already set.
     */
    public void testFillIn_categorySet() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        sourceIntent = new Intent();
        sourceIntent.addCategory(TEST_CATEGORY);
        destIntent.addCategory(ANOTHER_TEST_CATEGORY);

        assertEquals(0, destIntent.fillIn(sourceIntent, 0));
        assertEquals(1, destIntent.getCategories().size());
        assertTrue(destIntent.getCategories().contains(ANOTHER_TEST_CATEGORY));
        assertFalse(destIntent.getCategories().contains(TEST_CATEGORY));
    }

    /**
     * Test that fillIn adds categories when {@link Intent#FILL_IN_CATEGORIES} flag is set.
     */
    public void testFillIn_categoryOverride() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        sourceIntent = new Intent();
        sourceIntent.addCategory(TEST_CATEGORY);
        destIntent.addCategory(ANOTHER_TEST_CATEGORY);

        assertEquals(Intent.FILL_IN_CATEGORIES, destIntent.fillIn(sourceIntent, Intent.FILL_IN_CATEGORIES));
        assertEquals(1, destIntent.getCategories().size());
        assertFalse(destIntent.getCategories().contains(ANOTHER_TEST_CATEGORY));
        assertTrue(destIntent.getCategories().contains(TEST_CATEGORY));
    }

    /**
     * Test fillIn copies package.
     */
    public void testFillIn_package() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        sourceIntent.setPackage(TEST_PACKAGE);
        assertEquals(Intent.FILL_IN_PACKAGE, destIntent.fillIn(sourceIntent, 0));
        assertEquals(TEST_PACKAGE, destIntent.getPackage());
    }

    /**
     * Test fillIn does not copy package by default when already set.
     */
    public void testFillIn_packageSet() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        sourceIntent.setPackage(TEST_PACKAGE);
        destIntent.setPackage(ANOTHER_TEST_PACKAGE);
        assertEquals(0, destIntent.fillIn(sourceIntent, 0));
        assertEquals(ANOTHER_TEST_PACKAGE, destIntent.getPackage());
    }

    /**
     * Test that fillIn overrides package when {@link Intent#FILL_IN_PACKAGE} flag is set.
     */
    public void testFillIn_packageOverride() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        sourceIntent.setPackage(TEST_PACKAGE);
        destIntent.setPackage(ANOTHER_TEST_PACKAGE);
        assertEquals(Intent.FILL_IN_PACKAGE, destIntent.fillIn(sourceIntent, Intent.FILL_IN_PACKAGE));
        assertEquals(TEST_PACKAGE, destIntent.getPackage());
    }

    /**
     * Test that fillIn copies extras.
     */
    public void testFillIn_extras() {
        Intent sourceIntent = new Intent();
        Intent destIntent = new Intent();
        final Bundle bundle = new Bundle();
        bundle.putBoolean(TEST_EXTRA_NAME, true);
        sourceIntent.putExtras(bundle);
        assertEquals(0, destIntent.fillIn(sourceIntent, 0));
        assertTrue(destIntent.getExtras().getBoolean(TEST_EXTRA_NAME));
    }

    public void testGetExtras() {
        assertNull(mIntent.getExtras());
        final String expected = "testString";
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertNotNull(mIntent.getExtras());
        assertEquals(expected, mIntent.getExtras().getString(TEST_EXTRA_NAME));
    }

    public void testGetBooleanExtra() {
        assertFalse(mIntent.getBooleanExtra(TEST_EXTRA_NAME, false));
        mIntent.putExtra(TEST_EXTRA_NAME, true);
        assertTrue(mIntent.getBooleanExtra(TEST_EXTRA_NAME, false));
        mIntent.putExtra(TEST_EXTRA_NAME, false);
        assertFalse(mIntent.getBooleanExtra(TEST_EXTRA_NAME, false));
    }

    public void testGetFloatExtra() {
        float expected = 2f;
        assertEquals(1f, mIntent.getFloatExtra(TEST_EXTRA_NAME, 1f));
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getFloatExtra(TEST_EXTRA_NAME, 1f));
    }

    public void testGetShortArrayExtra() {
        final short[] expected = { 1, 2, 3 };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getShortArrayExtra(TEST_EXTRA_NAME));
    }

    public void testGetStringArrayExtra() {
        final String[] expected = { "1d", "2d", "3d" };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getStringArrayExtra(TEST_EXTRA_NAME));
    }

    public void testGetCharSequenceArrayExtra() {
        final String[] expected = { "1d", "2d", "3d" };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getCharSequenceArrayExtra(TEST_EXTRA_NAME));
    }

    public void testGetByteArrayExtra() {
        final byte[] expected = TEST_ACTION.getBytes();
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getByteArrayExtra(TEST_EXTRA_NAME));
    }

    public void testHasFileDescriptors() {
        Bundle bundle = mIntent.getExtras();
        assertEquals(bundle != null && bundle.hasFileDescriptors(), mIntent.hasFileDescriptors());
        final byte[] expected = TEST_ACTION.getBytes();
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        bundle = mIntent.getExtras();
        assertEquals(bundle != null && bundle.hasFileDescriptors(), mIntent.hasFileDescriptors());
    }

    public void testGetBooleanArrayExtra() {
        final boolean[] expected = { true, true, false };
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getBooleanArrayExtra(TEST_EXTRA_NAME));
    }

    public void testGetLongExtra() {
        final long expected = 2l;
        assertEquals(1l, mIntent.getLongExtra(TEST_EXTRA_NAME, 1l));
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getLongExtra(TEST_EXTRA_NAME, 1l));
    }

    public void testRemoveCategory() {
        assertNull(mIntent.getCategories());
        mIntent.addCategory(TEST_CATEGORY);
        assertNotNull(mIntent.getCategories());
        assertEquals(1, mIntent.getCategories().size());
        assertEquals(TEST_CATEGORY, (String) (mIntent.getCategories().toArray()[0]));
        mIntent.removeCategory(TEST_CATEGORY);
        assertFalse(mIntent.hasCategory(TEST_CATEGORY));
    }

    public void testFilterEquals() {
        assertFalse(mIntent.filterEquals(null));

        Intent target = new Intent();
        assertTrue(mIntent.filterEquals(target));

        target.setAction(TEST_ACTION);
        assertFalse(mIntent.filterEquals(target));
        mIntent.setAction(TEST_ACTION + "test");
        assertFalse(mIntent.filterEquals(target));
        mIntent.setAction(null);
        assertFalse(mIntent.filterEquals(target));
        mIntent.setAction(TEST_ACTION);
        assertTrue(mIntent.filterEquals(target));

        target.setData(TEST_URI);
        assertFalse(mIntent.filterEquals(target));
        mIntent.setData(Uri.parse("myURI"));
        assertFalse(mIntent.filterEquals(target));
        mIntent.setData(null);
        assertFalse(mIntent.filterEquals(target));
        mIntent.setData(TEST_URI);
        assertTrue(mIntent.filterEquals(target));

        target.setType(TEST_TYPE);
        assertFalse(mIntent.filterEquals(target));
        mIntent.setType(TEST_TYPE + "test");
        assertFalse(mIntent.filterEquals(target));
        mIntent.setType(null);
        assertFalse(mIntent.filterEquals(target));
        mIntent.setType(TEST_TYPE);
        assertTrue(mIntent.filterEquals(target));

        target.setComponent(mComponentName);
        assertFalse(mIntent.filterEquals(target));
        mIntent.setComponent(new ComponentName(mContext, MockReceiver.class));
        assertFalse(mIntent.filterEquals(target));
        mIntent.setComponent(null);
        assertFalse(mIntent.filterEquals(target));
        mIntent.setComponent(mComponentName);
        assertTrue(mIntent.filterEquals(target));

        target.addCategory(TEST_CATEGORY);
        assertFalse(mIntent.filterEquals(target));
        mIntent.addCategory(TEST_CATEGORY + "test");
        assertFalse(mIntent.filterEquals(target));
        mIntent.addCategory(TEST_CATEGORY);
        assertFalse(mIntent.filterEquals(target));
    }

    public void testPutExtras1() {
        final Intent intent = new Intent();
        mIntent.putExtras(intent);
        assertEquals(intent.getExtras(), mIntent.getExtras());
        intent.putExtra("test2", true);
        mIntent.putExtras(intent);
        assertEquals(intent.getExtras().toString(), mIntent.getExtras().toString());
    }

    public void testPutExtras2() {
        final Bundle bundle = new Bundle();
        mIntent.putExtras(bundle);
        assertEquals(0, mIntent.getExtras().size());
        String expected = "testString";
        bundle.putString(TEST_EXTRA_NAME, expected);
        mIntent.putExtras(bundle);
        assertEquals(1, mIntent.getExtras().size());
        assertEquals(expected, mIntent.getExtras().getString(TEST_EXTRA_NAME));
        mIntent.putExtra(TEST_EXTRA_NAME, bundle);
        assertEquals(bundle, mIntent.getBundleExtra(TEST_EXTRA_NAME));
    }

    public void testGetByteExtra() {
        final byte expected = Byte.valueOf("1");
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getByteExtra(TEST_EXTRA_NAME, Byte.valueOf("1")));
    }

    public void testGetSerializableExtra() {
        TestSerializable expected = new TestSerializable();
        expected.Name = "testName";
        mIntent.putExtra(TEST_EXTRA_NAME, expected);
        assertEquals(expected, mIntent.getSerializableExtra(TEST_EXTRA_NAME));
        TestSerializable target = (TestSerializable) mIntent.getSerializableExtra(TEST_EXTRA_NAME);
        assertEquals(expected.Name, target.Name);
    }

    public void testReplaceExtras() {
        Bundle extras = new Bundle();
        String bundleKey = "testKey";
        String bundleValue = "testValue";
        extras.putString(bundleKey, bundleValue);

        Intent intent = mIntent.replaceExtras(extras);
        assertSame(mIntent, intent);
        String actualValue = intent.getExtras().getString(bundleKey);
        assertEquals(bundleValue, actualValue);

        Intent src = new Intent();
        String intentName = "srcName";
        String intentValue = "srcValue";
        src.putExtra(intentName, intentValue);

        intent = mIntent.replaceExtras(src);
        assertSame(mIntent, intent);
        actualValue = intent.getExtras().getString(intentName);
        assertEquals(intentValue, actualValue);
    }

    public void testNormalizeMimeType() {
        assertEquals(null, Intent.normalizeMimeType(null));
        assertEquals("text/plain", Intent.normalizeMimeType("text/plain; charset=UTF-8"));
        assertEquals("text/x-vcard", Intent.normalizeMimeType("text/x-vCard"));
        assertEquals("foo/bar", Intent.normalizeMimeType("   foo/bar    "));
    }

    private static class TestSerializable implements Serializable {
        static final long serialVersionUID = 1l;
        public String Name;
    }
}
