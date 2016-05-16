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
 * limitations under the License
 */

package com.android.providers.contacts.aggregation.util;

import android.test.suitebuilder.annotation.SmallTest;

import com.android.providers.contacts.NameNormalizer;
import com.android.providers.contacts.util.Hex;

import junit.framework.TestCase;

/**
 * Unit tests for {@link NameDistance}.
 *
 * Run the test like this:
 * <code>
 * adb shell am instrument -e class com.android.providers.contacts.NameDistanceTest -w \
 *         com.android.providers.contacts.tests/android.test.InstrumentationTestRunner
 * </code>
 */
@SmallTest
public class NameDistanceTest extends TestCase {

    private NameDistance mNameDistance;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mNameDistance = new NameDistance(30);
    }

    public void testExactMatch() {
        assertFloat(1, "Dwayne", "Dwayne");
    }

    public void testWinklerBonus() {
        assertFloat(0.961f, "Martha", "Marhta");
        assertFloat(0.840f, "Dwayne", "Duane");
        assertFloat(0.813f, "DIXON", "DICKSONX");
    }

    public void testJaroDistance() {
        assertFloat(0.600f, "Donny", "Duane");
    }

    public void testPoorMatch() {
        assertFloat(0.467f, "Johny", "Duane");
    }

    public void testNoMatches() {
        assertFloat(0, "Abcd", "Efgh");
    }

    private void assertFloat(float expected, String name1, String name2) {
        byte[] s1 = Hex.decodeHex(NameNormalizer.normalize(name1));
        byte[] s2 = Hex.decodeHex(NameNormalizer.normalize(name2));

        float actual = mNameDistance.getDistance(s1, s2);
        assertTrue("Expected Jaro-Winkler distance: " + expected + ", actual: " + actual,
                Math.abs(actual - expected) < 0.001);
    }
}
