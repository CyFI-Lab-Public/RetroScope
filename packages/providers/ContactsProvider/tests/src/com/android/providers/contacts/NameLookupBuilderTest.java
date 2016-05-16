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

package com.android.providers.contacts;

import android.provider.ContactsContract.FullNameStyle;
import android.test.suitebuilder.annotation.SmallTest;

import junit.framework.TestCase;

import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;

/**
 * Unit tests for {@link NameLookupBuilder}.
 *
 * Run the test like this:
 * <code>
 * adb shell am instrument -e class com.android.providers.contacts.NameLookupBuilderTest -w \
 *         com.android.providers.contacts.tests/android.test.InstrumentationTestRunner
 * </code>
 */
@SmallTest
public class NameLookupBuilderTest extends TestCase {

    private static class TestNameLookupBuilder extends NameLookupBuilder {

        StringBuilder sb = new StringBuilder();

        public TestNameLookupBuilder(NameSplitter splitter) {
            super(splitter);
        }

        @Override
        protected String normalizeName(String name) {

            // TO make the test more readable, simply return the name unnormalized
            return name;
        }

        @Override
        protected String[] getCommonNicknameClusters(String normalizedName) {
            if (normalizedName.equals("Bill")) {
                return new String[] {"*William"};
            } else if (normalizedName.equals("Al")) {
                return new String[] {"*Alex", "*Alice"};
            }
            return null;
        }

        public String inserted() {
            return sb.toString();
        }

        @Override
        protected void insertNameLookup(long rawContactId, long dataId, int lookupType,
                String string) {
            sb.append("(").append(lookupType).append(":").append(string).append(")");
        }
    }

    private TestNameLookupBuilder mBuilder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mBuilder = new TestNameLookupBuilder(
                new NameSplitter("Mr", "", "", "", Locale.getDefault()));
    }

    public void testEmptyName() {
        mBuilder.insertNameLookup(0, 0, "", FullNameStyle.UNDEFINED);
        assertEquals("", mBuilder.inserted());
    }

    public void testSingleUniqueName() {
        mBuilder.insertNameLookup(0, 0, "Foo", FullNameStyle.UNDEFINED);
        assertEquals("(0:Foo)(2:Foo)", mBuilder.inserted());
    }

    public void testSingleUniqueNameWithPrefix() {
        mBuilder.insertNameLookup(0, 0, "Mr. Foo", FullNameStyle.UNDEFINED);
        assertEquals("(0:Mr.Foo)(2:MrFoo)(1:Foo.Mr)(2:FooMr)", mBuilder.inserted());
    }

    public void testTwoUniqueNames() {
        mBuilder.insertNameLookup(0, 0, "Foo Bar", FullNameStyle.UNDEFINED);
        assertEquals("(0:Foo.Bar)(2:FooBar)(1:Bar.Foo)(2:BarFoo)", mBuilder.inserted());
    }

    public void testThreeUniqueNames() {
        mBuilder.insertNameLookup(0, 0, "Foo Bar Baz", FullNameStyle.UNDEFINED);

        // All permutations
        assertEquals(
                "(0:Foo.Bar.Baz)(2:FooBarBaz)" +
                "(1:Foo.Baz.Bar)(2:FooBazBar)" +

                "(1:Bar.Foo.Baz)(2:BarFooBaz)" +
                "(1:Bar.Baz.Foo)(2:BarBazFoo)" +

                "(1:Baz.Bar.Foo)(2:BazBarFoo)" +
                "(1:Baz.Foo.Bar)(2:BazFooBar)", mBuilder.inserted());
    }

    public void testFourUniqueNames() {
        mBuilder.insertNameLookup(0, 0, "Foo Bar Baz Biz", FullNameStyle.UNDEFINED);

        // All permutations
        assertEquals(
                "(0:Foo.Bar.Baz.Biz)(2:FooBarBazBiz)" +
                "(1:Foo.Bar.Biz.Baz)(2:FooBarBizBaz)" +
                "(1:Foo.Baz.Bar.Biz)(2:FooBazBarBiz)" +
                "(1:Foo.Baz.Biz.Bar)(2:FooBazBizBar)" +
                "(1:Foo.Biz.Baz.Bar)(2:FooBizBazBar)" +
                "(1:Foo.Biz.Bar.Baz)(2:FooBizBarBaz)" +

                "(1:Bar.Foo.Baz.Biz)(2:BarFooBazBiz)" +
                "(1:Bar.Foo.Biz.Baz)(2:BarFooBizBaz)" +
                "(1:Bar.Baz.Foo.Biz)(2:BarBazFooBiz)" +
                "(1:Bar.Baz.Biz.Foo)(2:BarBazBizFoo)" +
                "(1:Bar.Biz.Baz.Foo)(2:BarBizBazFoo)" +
                "(1:Bar.Biz.Foo.Baz)(2:BarBizFooBaz)" +

                "(1:Baz.Bar.Foo.Biz)(2:BazBarFooBiz)" +
                "(1:Baz.Bar.Biz.Foo)(2:BazBarBizFoo)" +
                "(1:Baz.Foo.Bar.Biz)(2:BazFooBarBiz)" +
                "(1:Baz.Foo.Biz.Bar)(2:BazFooBizBar)" +
                "(1:Baz.Biz.Foo.Bar)(2:BazBizFooBar)" +
                "(1:Baz.Biz.Bar.Foo)(2:BazBizBarFoo)" +

                "(1:Biz.Bar.Baz.Foo)(2:BizBarBazFoo)" +
                "(1:Biz.Bar.Foo.Baz)(2:BizBarFooBaz)" +
                "(1:Biz.Baz.Bar.Foo)(2:BizBazBarFoo)" +
                "(1:Biz.Baz.Foo.Bar)(2:BizBazFooBar)" +
                "(1:Biz.Foo.Baz.Bar)(2:BizFooBazBar)" +
                "(1:Biz.Foo.Bar.Baz)(2:BizFooBarBaz)", mBuilder.inserted());
    }

    public void testSingleNickname() {
        mBuilder.insertNameLookup(0, 0, "Bill", FullNameStyle.UNDEFINED);
        assertEquals("(0:Bill)(2:Bill)(1:*William)", mBuilder.inserted());
    }

    public void testSingleNameWithTwoNicknames() {
        mBuilder.insertNameLookup(0, 0, "Al", FullNameStyle.UNDEFINED);
        assertEquals("(0:Al)(2:Al)(1:*Alex)(1:*Alice)", mBuilder.inserted());
    }

    public void testTwoNamesOneOfWhichIsNickname() {
        mBuilder.insertNameLookup(0, 0, "Foo Al", FullNameStyle.UNDEFINED);
        assertEquals(
                "(0:Foo.Al)(2:FooAl)" +
                "(1:Al.Foo)(2:AlFoo)" +
                "(1:Foo.*Alex)(1:*Alex.Foo)" +
                "(1:Foo.*Alice)(1:*Alice.Foo)", mBuilder.inserted());
    }

    public void testTwoNamesBothNickname() {
        mBuilder.insertNameLookup(0, 0, "Bill Al", FullNameStyle.UNDEFINED);
        assertEquals(
                "(0:Bill.Al)(2:BillAl)" +
                "(1:Al.Bill)(2:AlBill)" +
                "(1:*William.Al)(1:Al.*William)" +
                "(1:*William.*Alex)(1:*Alex.*William)" +
                "(1:*William.*Alice)(1:*Alice.*William)" +
                "(1:Bill.*Alex)(1:*Alex.Bill)" +
                "(1:Bill.*Alice)(1:*Alice.Bill)", mBuilder.inserted());
    }

    public void testChineseName() {
        // Only run this test when Chinese collation is supported
        if (!Arrays.asList(Collator.getAvailableLocales()).contains(Locale.CHINA)) {
            return;
        }

        mBuilder.insertNameLookup(0, 0, "\u695A\u8FAD", FullNameStyle.CHINESE);
        assertEquals(
                "(0:\u695A\u8FAD)" +
                "(2:\u695A\u8FAD)",
                mBuilder.inserted());
    }

    public void testKoreanName() {
        // Only run this test when Korean collation is supported.
        if (!Arrays.asList(Collator.getAvailableLocales()).contains(Locale.KOREA)) {
            return;
        }

        // Lee Sang Il
        mBuilder.insertNameLookup(0, 0, "\uC774\uC0C1\uC77C", FullNameStyle.KOREAN);
        assertEquals(
                "(0:\uC774\uC0C1\uC77C)" + // Lee Sang Il
                "(2:\uC774\uC0C1\uC77C)",
                mBuilder.inserted());
    }

    public void testKoreanNameWithTwoCharactersFamilyName() {
        // Only run this test when Chinese collation is supported.
        if (!Arrays.asList(Collator.getAvailableLocales()).contains(Locale.KOREA)) {
            return;
        }

        // Sun Woo Young Nyeu
        mBuilder.insertNameLookup(0, 0, "\uC120\uC6B0\uC6A9\uB140", FullNameStyle.KOREAN);
        assertEquals(
                "(0:\uC120\uC6B0\uC6A9\uB140)" + // Sun Woo Young Nyeu
                "(2:\uC120\uC6B0\uC6A9\uB140)",  // Sun Woo Young Nyeu
                mBuilder.inserted());
    }

    public void testMultiwordName() {
        mBuilder.insertNameLookup(0, 0, "Jo Jeffrey John Jessy Longname", FullNameStyle.UNDEFINED);
        String actual = mBuilder.inserted();

        // Exact name
        assertTrue(actual.contains("(0:Jo.Jeffrey.John.Jessy.Longname)"));

        // Full collation key
        assertTrue(actual.contains("(2:JoJeffreyJohnJessyLongname)"));

        // Variant: four longest parts
        assertTrue(actual.contains("(1:Longname.Jeffrey.Jessy.John)"));

        // All individual words
        assertTrue(actual.contains("(2:Jo"));
        assertTrue(actual.contains("(2:Jeffrey"));
        assertTrue(actual.contains("(2:John"));
        assertTrue(actual.contains("(2:Jessy"));
        assertTrue(actual.contains("(2:Longname"));
    }
}
