/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.security.tests.asn1.der;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.apache.harmony.security.asn1.ASN1GeneralizedTime;
import org.apache.harmony.security.asn1.DerInputStream;
import org.apache.harmony.security.asn1.DerOutputStream;

/**
 * ASN.1 DER test for GeneralizedTime type
 *
 * @see http://asn1.elibel.tm.fr/en/standards/index.htm
 */

public class GeneralizedTimeTest extends TestCase {

    // decoder/encoder for testing
    private static final ASN1GeneralizedTime gtime = ASN1GeneralizedTime
            .getInstance();

    // data for testing with format: date string/DER encoding/Date object
    private static final Object[][] validGeneralizedTimes;

    static {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        validGeneralizedTimes = new Object[][] {
                // YYYYMMDD-HHMMSS = "19000101000000Z"
                {
                        "1 Jan 1900 00:00:00",
                        new byte[] { 0x18, 0x0F, 0x31, 0x39, 0x30, 0x30, 0x30,
                                0x31, 0x30, 0x31, 0x30, 0x30, 0x30, 0x30, 0x30,
                                0x30, 0x5A }, null },
                // YYYYMMDD-HHMMSS = "19490203040506Z"
                {
                        "3 Feb 1949 04:05:06",
                        new byte[] { 0x18, 0x0F, 0x31, 0x39, 0x34, 0x39, 0x30,
                                0x32, 0x30, 0x33, 0x30, 0x34, 0x30, 0x35, 0x30,
                                0x36, 0x5A }, null },
                // YYYYMMDD-HHMMSS = "2000708091011Z"
                {
                        "8 Jul 2000 09:10:11",
                        new byte[] { 0x18, 0x0F, 0x32, 0x30, 0x30, 0x30, 0x30,
                                0x37, 0x30, 0x38, 0x30, 0x39, 0x31, 0x30, 0x31,
                                0x31, 0x5A }, null },
                // YYYYMMDD-HHMMSS = "20501213141516Z"
                {
                        "13 Dec 2050 14:15:16",
                        new byte[] { 0x18, 0x0F, 0x32, 0x30, 0x35, 0x30, 0x31,
                                0x32, 0x31, 0x33, 0x31, 0x34, 0x31, 0x35, 0x31,
                                0x36, 0x5A }, null },
                // YYYYMMDD-HHMMSS = "20501213141516Z"
                {
                        "29 Mar 2332 06:56:40",
                        new byte[] { 0x18, 0x0F, 0x32, 0x33, 0x33, 0x32, 0x30,
                                0x33, 0x32, 0x39, 0x30, 0x36, 0x35, 0x36, 0x34,
                                0x30, 0x5A }, null },
        };

        try {
            // fill values for Date objects by parsing date string
            for (int i = 0; i < validGeneralizedTimes.length; i++) {
                validGeneralizedTimes[i][2] = sdf
                        .parseObject((String) validGeneralizedTimes[i][0]);
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifies DER decoding/encoding ASN.1 GeneralizedTime.
     * GeneralizedTime expresses Greenwich Mean Time by
     * the following pattern: YYYYMMDDHHMMSS'Z'
     */
    public void test_Decode_Encode() throws Exception {

        // decoding byte array
        for (int i = 0; i < validGeneralizedTimes.length; i++) {
            DerInputStream in = new DerInputStream(
                    (byte[]) validGeneralizedTimes[i][1]);
            assertEquals("Decoding array for " + validGeneralizedTimes[i][0],
                    validGeneralizedTimes[i][2], //expected
                    gtime.decode(in)); //decoded
        }

        // decoding input stream
        for (int i = 0; i < validGeneralizedTimes.length; i++) {
            DerInputStream in = new DerInputStream(new ByteArrayInputStream(
                    (byte[]) validGeneralizedTimes[i][1]));
            assertEquals("Decoding stream for " + validGeneralizedTimes[i][0],
                    validGeneralizedTimes[i][2], //expected
                    gtime.decode(in)); //decoded
        }

        // encoding date object
        for (int i = 0; i < validGeneralizedTimes.length; i++) {
            DerOutputStream out = new DerOutputStream(gtime,
                    validGeneralizedTimes[i][2]);
            assertTrue("Encoding date for " + validGeneralizedTimes[i][0],
                    Arrays.equals((byte[]) validGeneralizedTimes[i][1], // expected
                            out.encoded)); //encoded
        }
    }

    /**
     * Tests milliseconds result of encoding/decoding on the date after 2050.
     */
    public void test_Milliseconds() throws IOException{
        // Regression test for HARMONY-1252
        long old_date = 11431151800000L;
        long new_date = ((Date) gtime.decode(gtime.encode(new Date(old_date))))
                .getTime();
        assertEquals(old_date, new_date);
    }

    public void test_EncodeMilliseconds() throws IOException{
        //cRegression for HARMONY-2302
        long old_date = 1164358741071L;
        long new_date = ((Date) gtime.decode(gtime.encode(new Date(old_date))))
                .getTime();
        assertEquals(old_date, new_date);
    }

}
