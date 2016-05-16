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

/**
* @author Vladimir N. Molotkov
*/

package org.apache.harmony.security.tests.asn1.der;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.harmony.security.asn1.ASN1GeneralizedTime;
import org.apache.harmony.security.asn1.DerInputStream;
import org.apache.harmony.security.asn1.DerOutputStream;

import junit.framework.TestCase;


/**
 * ASN.1 DER test for GeneralizedTime type
 * 
 * @see http://asn1.elibel.tm.fr/en/standards/index.htm
 */
public class DerGeneralizedTimeEDTest extends TestCase {

    private ASN1GeneralizedTime gTime = ASN1GeneralizedTime.getInstance();
    
    /**
     * GENERALIZED TIME DER Encoder test
     * @throws ParseException
     */
    public final void testGeneralizedEncoder() throws Exception {
        // full fractional seconds
        Date myDate = getGmtDate(1101980374187L);
        byte[] encoded =
            new DerOutputStream(gTime, myDate).encoded;
        String rep = new String(encoded, 2, encoded[1] & 0xff, "UTF-8");
        assertEquals("full", "20041202093934.187Z", rep);

        // 2 digit fractional seconds (last 0 must be trimmed out)
        myDate = getGmtDate(1101980374180L);
        encoded =
            new DerOutputStream(gTime, myDate).encoded;
        rep = new String(encoded, 2, encoded[1] & 0xff, "UTF-8");
        assertEquals("2 fraction", "20041202093934.18Z", rep);

        // 1 digit fractional seconds (last 2 0s must be trimmed out)
        myDate = getGmtDate(1101980374100L);
        encoded =
            new DerOutputStream(gTime, myDate).encoded;
        rep = new String(encoded, 2, encoded[1] & 0xff, "UTF-8");
        assertEquals("1 fraction", "20041202093934.1Z", rep);

        // no fractional seconds (last 3 0s and "." must be trimmed out)
        myDate = getGmtDate(1101980374000L);
        encoded =
            new DerOutputStream(gTime, myDate).encoded;
        rep = new String(encoded, 2, encoded[1] & 0xff, "UTF-8");
        assertEquals("no fraction", "20041202093934Z", rep);

        // midnight
        SimpleDateFormat sdf =
            new SimpleDateFormat("dd.MM.yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        myDate = sdf.parse("06.06.2004 00:00");
        encoded =
            new DerOutputStream(gTime, myDate).encoded;
        rep = new String(encoded, 2, encoded[1] & 0xff, "UTF-8");
        assertEquals("midnight", "20040606000000Z", rep);
    }

    /**
     * GENERALIZED TIME DER Encoder/Decoder test
     * (byte array case)
     * @throws ParseException
     * @throws IOException
     */
    public final void testGeneralizedEncoderDecoder01()
        throws ParseException,
               IOException {
        runTest(false);
    }

    /**
     * GENERALIZED TIME DER Encoder/Decoder test
     * (InputStream case)
     * @throws ParseException
     * @throws IOException
     */
    public final void testGeneralizedEncoderDecoder02()
        throws ParseException,
               IOException {
        runTest(true);
    }

    private final void runTest(boolean useInputStream)
        throws IOException, ParseException {
        // full fractional seconds
        Date myDate = new Date(1101980374187L);
        byte[] encoded =
            new DerOutputStream(gTime, myDate).encoded;
        DerInputStream dis = useInputStream
        ? new DerInputStream(new ByteArrayInputStream(encoded))
        : new DerInputStream(encoded);
        assertEquals("full", myDate, gTime.decode(dis));

        // 2 digit fractional seconds (last 0 must be trimmed out)
        myDate = new Date(1101980374180L);
        encoded =
            new DerOutputStream(gTime, myDate).encoded;
        dis = useInputStream
        ? new DerInputStream(new ByteArrayInputStream(encoded))
        : new DerInputStream(encoded);
        assertEquals("2 fraction", myDate, gTime.decode(dis));

        // 1 digit fractional seconds (last 2 0s must be trimmed out)
        myDate = new Date(1101980374100L);
        encoded =
            new DerOutputStream(gTime, myDate).encoded;
        dis = useInputStream
        ? new DerInputStream(new ByteArrayInputStream(encoded))
        : new DerInputStream(encoded);
        assertEquals("1 fraction", myDate, gTime.decode(dis));

        // no fractional seconds (last 3 0s and "." must be trimmed out)
        myDate = new Date(1101980374000L);
        encoded =
            new DerOutputStream(gTime, myDate).encoded;
        dis = useInputStream
        ? new DerInputStream(new ByteArrayInputStream(encoded))
        : new DerInputStream(encoded);
        assertEquals("no fraction", myDate, gTime.decode(dis));

        // midnight
        myDate = new SimpleDateFormat("MM.dd.yyyy HH:mm").
            parse("06.06.2004 00:00");
        encoded =
            new DerOutputStream(gTime, myDate).encoded;
        dis = useInputStream
        ? new DerInputStream(new ByteArrayInputStream(encoded))
        : new DerInputStream(encoded);
        assertEquals("midnight", myDate, gTime.decode(dis));

        // date 100 ms
        myDate = new Date(100L);
        encoded =
            new DerOutputStream(gTime, myDate).encoded;
        dis = useInputStream
        ? new DerInputStream(new ByteArrayInputStream(encoded))
        : new DerInputStream(encoded);
        assertEquals("100ms", myDate, gTime.decode(dis));
    }

    private static Date getGmtDate(long mills) {
        return new Date(mills);
    }
}
