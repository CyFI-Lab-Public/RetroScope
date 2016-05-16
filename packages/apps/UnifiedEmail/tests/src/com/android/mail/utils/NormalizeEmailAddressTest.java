// Copyright 2013 Google Inc. All Rights Reserved.

package com.android.mail.utils;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.mail.utils.Utils;

@SmallTest
public class NormalizeEmailAddressTest extends AndroidTestCase {
    public void testNormalizeEmailAddress() {
        final String emailAddress = "user@example.com";

        assertEquals(Utils.normalizeEmailAddress("User@EXAMPLE.COM"), emailAddress);

        assertEquals(Utils.normalizeEmailAddress("User@example.com"), emailAddress);

        assertEquals(Utils.normalizeEmailAddress("User@exaMple.com"), emailAddress);

        assertEquals(Utils.normalizeEmailAddress(null), null);

        assertEquals(Utils.normalizeEmailAddress(""), "");

        assertEquals(Utils.normalizeEmailAddress("Not an EMAIL address"), "not an email address");
    }
}