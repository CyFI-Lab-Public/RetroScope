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

package android.net.http.cts;

import java.text.DateFormat;
import java.util.Date;

import junit.framework.TestCase;
import android.net.http.SslCertificate;
import android.net.http.SslCertificate.DName;

public class SslCertificate_DNameTest extends TestCase {

    public void testDName() {
        final String TO = "c=ccc,o=testOName,ou=testUName,cn=testCName";
        final String BY = "e=aeei,c=adb,o=testOName,ou=testUName,cn=testCName";
        // new the SslCertificate instance
        Date date1 = new Date(System.currentTimeMillis() - 1000);
        Date date2 = new Date(System.currentTimeMillis());
        SslCertificate ssl = new SslCertificate(TO, BY, DateFormat.getInstance().format(
                date1), DateFormat.getInstance().format(date2));
        DName issuedTo = ssl.getIssuedTo();

        assertNotNull(issuedTo);

        assertEquals("testCName", issuedTo.getCName());
        assertEquals(TO, issuedTo.getDName());
        assertEquals("testOName", issuedTo.getOName());
        assertEquals("testUName", issuedTo.getUName());
    }

}
