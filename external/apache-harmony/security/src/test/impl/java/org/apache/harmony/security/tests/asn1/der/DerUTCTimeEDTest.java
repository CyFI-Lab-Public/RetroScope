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

import org.apache.harmony.security.asn1.ASN1UTCTime;
import org.apache.harmony.security.asn1.DerInputStream;
import org.apache.harmony.security.asn1.DerOutputStream;

import junit.framework.TestCase;


/**
 * ASN.1 DER test for UTCTime type
 * 
 * @see http://asn1.elibel.tm.fr/en/standards/index.htm
 */
public class DerUTCTimeEDTest extends TestCase {

    private ASN1UTCTime uTime = ASN1UTCTime.getInstance();
    
    private final int workersNumber = 10;
    private boolean mtTestPassed;
    /**
     * UTC TIME DER Encoder test
     * @throws ParseException
     */
    public final void testUTCEncoder() throws Exception {
        // no fractional seconds (last 3 0s and "." must be trimmed out)
        Date myDate = getGmtDate(1101980374187L);
        byte[] encoded =
            new DerOutputStream(uTime, myDate).encoded;
        String rep = new String(encoded, 2, encoded[1] & 0xff, "UTF-8");
        assertEquals("no fraction", "041202093934Z", rep);

        // midnight
        SimpleDateFormat sdf =
            new SimpleDateFormat("dd.MM.yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        myDate = sdf.parse("06.06.2004 00:00");
        encoded =
            new DerOutputStream(uTime, myDate).encoded;
        rep = new String(encoded, 2, encoded[1] & 0xff, "UTF-8");
        assertEquals("midnight", "040606000000Z", rep);
    }

    /**
     * UTC TIME DER Encoder/Decoder test
     * (byte array case)
     * @throws ParseException
     * @throws IOException
     */
    public final void testUTCEncoderDecoder01()
        throws ParseException,
               IOException {
        runTest(false);
    }

    /**
     * UTC TIME DER Encoder/Decoder test
     * (InputStream case)
     * @throws ParseException
     * @throws IOException
     */
    public final void testUTCEncoderDecoder02()
        throws ParseException,
               IOException {
        runTest(true);
    }

    private final void runTest(boolean useInputStream)
        throws IOException, ParseException {
        Date myDate = new Date(1101980374187L);
        byte[] encoded =
            new DerOutputStream(uTime, myDate).encoded;
        DerInputStream dis = useInputStream
        ? new DerInputStream(new ByteArrayInputStream(encoded))
        : new DerInputStream(encoded);
        // the difference only fractional-seconds
        assertEquals(187, (myDate.getTime()-((Date)uTime.decode(dis)).getTime()));

        // midnight
        myDate = new SimpleDateFormat("MM.dd.yyyy HH:mm").
            parse("06.06.2004 00:00");
        encoded =
            new DerOutputStream(uTime, myDate).encoded;
        dis = useInputStream
        ? new DerInputStream(new ByteArrayInputStream(encoded))
        : new DerInputStream(encoded);
        assertEquals(myDate, uTime.decode(dis));
    }

    public final void testMt() throws InterruptedException {
        mtTestPassed = true;
        Thread[] workers = new Thread[workersNumber];
            for(int i=0; i<workersNumber; i++) {
                workers[i] = new TestWorker();
            }
            for(int i=0; i<workersNumber; i++) {
                workers[i].start();
            }
            for(int i=0; i<workersNumber; i++) {
                workers[i].join();
            }
            assertTrue(mtTestPassed);
    }

    private static Date getGmtDate(long mills) {
        return new Date(mills);
    }

    /**
     * MT Test worker thread
     * 
     * @author Vladimir Molotkov
     * @version 0.1
     */
    private class TestWorker extends Thread {

        public void run() {
            for (int i=0; i<100; i++) {
                try {
                    // Perform DER encoding/decoding:
                    if(i%2==0) {
                        testUTCEncoderDecoder01();
                    } else {
                        testUTCEncoderDecoder02();
                    }
                } catch (Throwable e) {
                    System.err.println(e);
                    mtTestPassed = false;
                    return;
                }
            }
        }
    }
}
