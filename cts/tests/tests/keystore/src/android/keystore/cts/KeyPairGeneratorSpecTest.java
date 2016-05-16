/*
 * Copyright 2013 The Android Open Source Project
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

package android.keystore.cts;

import android.security.KeyPairGeneratorSpec;
import android.test.AndroidTestCase;

import java.math.BigInteger;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

public class KeyPairGeneratorSpecTest extends AndroidTestCase {
    private static final String TEST_ALIAS_1 = "test1";

    private static final X500Principal TEST_DN_1 = new X500Principal("CN=test1");

    private static final long NOW_MILLIS = System.currentTimeMillis();

    private static final BigInteger SERIAL_1 = BigInteger.ONE;

    /* We have to round this off because X509v3 doesn't store milliseconds. */
    private static final Date NOW = new Date(NOW_MILLIS - (NOW_MILLIS % 1000L));

    @SuppressWarnings("deprecation")
    private static final Date NOW_PLUS_10_YEARS = new Date(NOW.getYear() + 10, 0, 1);

    public void testBuilder_Unencrypted_Success() throws Exception {
        KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(getContext())
                .setAlias(TEST_ALIAS_1)
                .setSubject(TEST_DN_1)
                .setSerialNumber(SERIAL_1)
                .setStartDate(NOW)
                .setEndDate(NOW_PLUS_10_YEARS)
                .build();

        assertEquals("Context should be the one specified", getContext(), spec.getContext());

        assertEquals("Alias should be the one specified", TEST_ALIAS_1, spec.getKeystoreAlias());

        assertEquals("subjectDN should be the one specified", TEST_DN_1, spec.getSubjectDN());

        assertEquals("startDate should be the one specified", NOW, spec.getStartDate());

        assertEquals("endDate should be the one specified", NOW_PLUS_10_YEARS, spec.getEndDate());

        assertFalse("encryption flag should not be on", spec.isEncryptionRequired());
    }

    public void testBuilder_Encrypted_Success() throws Exception {
        KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(getContext())
                .setAlias(TEST_ALIAS_1)
                .setSubject(TEST_DN_1)
                .setSerialNumber(SERIAL_1)
                .setStartDate(NOW)
                .setEndDate(NOW_PLUS_10_YEARS)
                .setEncryptionRequired()
                .build();

        assertEquals("Context should be the one specified", getContext(), spec.getContext());

        assertEquals("Alias should be the one specified", TEST_ALIAS_1, spec.getKeystoreAlias());

        assertEquals("subjectDN should be the one specified", TEST_DN_1, spec.getSubjectDN());

        assertEquals("startDate should be the one specified", NOW, spec.getStartDate());

        assertEquals("endDate should be the one specified", NOW_PLUS_10_YEARS, spec.getEndDate());

        assertTrue("encryption flag should be on", spec.isEncryptionRequired());
    }

    public void testBuilder_NullContext_Failure() throws Exception {
        try {
            new KeyPairGeneratorSpec.Builder(null);
            fail("Should throw NullPointerException when context is null");
        } catch (NullPointerException expected) {
        }
    }

    public void testBuilder_MissingAlias_Failure() throws Exception {
        try {
            new KeyPairGeneratorSpec.Builder(getContext())
                    .setSubject(TEST_DN_1)
                    .setSerialNumber(SERIAL_1)
                    .setStartDate(NOW)
                    .setEndDate(NOW_PLUS_10_YEARS)
                    .build();
            fail("Should throw IllegalArgumentException when alias is missing");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testBuilder_MissingSubjectDN_Failure() throws Exception {
        try {
            new KeyPairGeneratorSpec.Builder(getContext())
                    .setAlias(TEST_ALIAS_1)
                    .setSerialNumber(SERIAL_1)
                    .setStartDate(NOW)
                    .setEndDate(NOW_PLUS_10_YEARS)
                    .build();
            fail("Should throw IllegalArgumentException when subject is missing");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testBuilder_MissingSerialNumber_Failure() throws Exception {
        try {
            new KeyPairGeneratorSpec.Builder(getContext())
                    .setAlias(TEST_ALIAS_1)
                    .setSubject(TEST_DN_1)
                    .setStartDate(NOW)
                    .setEndDate(NOW_PLUS_10_YEARS)
                    .build();
            fail("Should throw IllegalArgumentException when serialNumber is missing");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testBuilder_MissingStartDate_Failure() throws Exception {
        try {
            new KeyPairGeneratorSpec.Builder(getContext())
                    .setAlias(TEST_ALIAS_1)
                    .setSubject(TEST_DN_1)
                    .setSerialNumber(SERIAL_1)
                    .setEndDate(NOW_PLUS_10_YEARS)
                    .build();
            fail("Should throw IllegalArgumentException when startDate is missing");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testBuilder_MissingEndDate_Failure() throws Exception {
        try {
            new KeyPairGeneratorSpec.Builder(getContext())
                    .setAlias(TEST_ALIAS_1)
                    .setSubject(TEST_DN_1)
                    .setSerialNumber(SERIAL_1)
                    .setStartDate(NOW)
                    .build();
            fail("Should throw IllegalArgumentException when endDate is missing");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testBuilder_EndBeforeStart_Failure() throws Exception {
        try {
            new KeyPairGeneratorSpec.Builder(getContext())
                    .setAlias(TEST_ALIAS_1)
                    .setSubject(TEST_DN_1)
                    .setSerialNumber(SERIAL_1)
                    .setStartDate(NOW_PLUS_10_YEARS)
                    .setEndDate(NOW)
                    .build();
            fail("Should throw IllegalArgumentException when end is before start");
        } catch (IllegalArgumentException expected) {
        }
    }
}
