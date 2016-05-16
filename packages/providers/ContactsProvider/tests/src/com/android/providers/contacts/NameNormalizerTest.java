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

import android.test.MoreAsserts;
import android.test.suitebuilder.annotation.SmallTest;

import java.text.RuleBasedCollator;
import java.util.Locale;

import junit.framework.TestCase;

/**
 * Unit tests for {@link NameNormalizer}.
 *
 * Run the test like this:
 * <code>
   adb shell am instrument -e class com.android.providers.contacts.NameNormalizerTest -w \
           com.android.providers.contacts.tests/android.test.InstrumentationTestRunner
 * </code>
 */
@SmallTest
public class NameNormalizerTest extends TestCase {

    private Locale mOriginalLocale;


    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mOriginalLocale = Locale.getDefault();

        // Run all test in en_US
        Locale.setDefault(Locale.US);
    }

    @Override
    protected void tearDown() throws Exception {
        Locale.setDefault(mOriginalLocale);
        super.tearDown();
    }

    public void testDifferent() {
        final String name1 = NameNormalizer.normalize("Helene");
        final String name2 = NameNormalizer.normalize("Francesca");
        assertFalse(name2.equals(name1));
    }

    public void testAccents() {
        final String name1 = NameNormalizer.normalize("Helene");
        final String name2 = NameNormalizer.normalize("H\u00e9l\u00e8ne");
        assertTrue(name2.equals(name1));
    }

    public void testMixedCase() {
        final String name1 = NameNormalizer.normalize("Helene");
        final String name2 = NameNormalizer.normalize("hEL\uFF25NE"); // FF25 = FULL WIDTH E
        assertTrue(name2.equals(name1));
    }

    public void testNonLetters() {
        // U+FF1E: 'FULLWIDTH GREATER-THAN SIGN'
        // U+FF03: 'FULLWIDTH NUMBER SIGN'
        final String name1 = NameNormalizer.normalize("h-e?l \uFF1ee+\uFF03n=e");
        final String name2 = NameNormalizer.normalize("helene");
        assertTrue(name2.equals(name1));
    }

    public void testComplexityCase() {
        assertTrue(NameNormalizer.compareComplexity("Helene", "helene") > 0);
    }

    public void testComplexityAccent() {
        assertTrue(NameNormalizer.compareComplexity("H\u00e9lene", "Helene") > 0);
    }

    public void testComplexityLength() {
        assertTrue(NameNormalizer.compareComplexity("helene2009", "helene") > 0);
    }

    public void testGetCollators() {
        final RuleBasedCollator compressing1 = NameNormalizer.getCompressingCollator();
        final RuleBasedCollator complexity1 = NameNormalizer.getComplexityCollator();

        assertNotNull(compressing1);
        assertNotNull(complexity1);
        assertNotSame(compressing1, complexity1);

        // Get again.  Should be cached.
        final RuleBasedCollator compressing2 = NameNormalizer.getCompressingCollator();
        final RuleBasedCollator complexity2 = NameNormalizer.getComplexityCollator();

        assertSame(compressing1, compressing2);
        assertSame(complexity1, complexity2);

        // Change locale -- now new collators should be returned.
        Locale.setDefault(Locale.FRANCE);

        final RuleBasedCollator compressing3 = NameNormalizer.getCompressingCollator();
        final RuleBasedCollator complexity3 = NameNormalizer.getComplexityCollator();

        assertNotNull(compressing3);
        assertNotNull(complexity3);
        assertNotSame(compressing3, complexity3);

        assertNotSame(compressing1, compressing3);
        assertNotSame(complexity1, complexity3);
    }
}
