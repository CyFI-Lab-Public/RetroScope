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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.apache.harmony.security.asn1.ASN1UTCTime;
import org.apache.harmony.security.asn1.DerInputStream;
import org.apache.harmony.security.asn1.DerOutputStream;

/**
 * ASN.1 DER test for UTCTime type
 *
 * @see http://asn1.elibel.tm.fr/en/standards/index.htm
 */

public class UTCTimeTest extends TestCase {

    // UTC time decoder/encoder for testing
    private static final ASN1UTCTime utime = ASN1UTCTime.getInstance();

    // data for testing with format: date string/DER encoding/Date object
    public static final Object[][] validUTCTimes;

    static {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

        validUTCTimes = new Object[][] {
                // YYMMDD-HHMMSS = "500708091011Z"
                {
                        "8 Jul 1950 09:10:11",
                        new byte[] { 0x17, 0x0D, 0x35, 0x30, 0x30, 0x37, 0x30,
                                0x38, 0x30, 0x39, 0x31, 0x30, 0x31, 0x31, 0x5A },
                        null },
                //YYMMDD-HHMMSS = "991213141516Z"
                {
                        "13 Dec 1999 14:15:16",
                        new byte[] { 0x17, 0x0D, 0x39, 0x39, 0x31, 0x32, 0x31,
                                0x33, 0x31, 0x34, 0x31, 0x35, 0x31, 0x36, 0x5A },
                        null },
                // YYMMDD-HHMMSS = "000101000000Z"
                {
                        "01 Jan 2000 00:00:00",
                        new byte[] { 0x17, 0x0D, 0x30, 0x30, 0x30, 0x31, 0x30,
                                0x31, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x5A },
                        null },
                // YYMMDD-HHMMSS = "490203040506Z"
                {
                        "3 Feb 2049 04:05:06",
                        new byte[] { 0x17, 0x0D, 0x34, 0x39, 0x30, 0x32, 0x30,
                                0x33, 0x30, 0x34, 0x30, 0x35, 0x30, 0x36, 0x5A },
                        null },
                        };

        try {
            // fill values for Date objects by parsing date string
            for (int i = 0; i < validUTCTimes.length; i++) {
                validUTCTimes[i][2] = sdf
                        .parseObject((String) validUTCTimes[i][0]);
            }
        } catch (ParseException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Verifies decoding/encoding ASN.1 UTCTime.
     * It must interpret the year field (YY) as follows:
     *  - if YY is greater than or equal to 50 then interpreted as 19YY
     *  - and if YY is less than 50 then interpreted as 20YY.
     */
    public void testDecodeEncode() throws Exception {

        // decoding byte array
        for (int i = 0; i < validUTCTimes.length; i++) {
            DerInputStream in = new DerInputStream((byte[]) validUTCTimes[i][1]);
            assertEquals("Decoding array for " + validUTCTimes[i][0],
                    validUTCTimes[i][2], //expected
                    utime.decode(in)); //decoded
        }

        // decoding input stream
        for (int i = 0; i < validUTCTimes.length; i++) {
            DerInputStream in = new DerInputStream(new ByteArrayInputStream(
                    (byte[]) validUTCTimes[i][1]));
            assertEquals("Decoding stream for " + validUTCTimes[i][0],
                    validUTCTimes[i][2], //expected
                    utime.decode(in)); //decoded
        }

        // encoding date object
        for (int i = 0; i < validUTCTimes.length; i++) {
            DerOutputStream out = new DerOutputStream(utime,
                    validUTCTimes[i][2]);
            assertTrue("Encoding date for " + validUTCTimes[i][0], Arrays
                    .equals((byte[]) validUTCTimes[i][1], // expected
                            out.encoded)); //encoded
        }
    }

}
