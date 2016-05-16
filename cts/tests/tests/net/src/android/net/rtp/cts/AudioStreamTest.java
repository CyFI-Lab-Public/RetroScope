/*
 * Copyright (C) 2012 The Android Open Source Project
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
package android.net.rtp.cts;

import android.net.rtp.AudioCodec;
import android.net.rtp.AudioStream;
import android.test.AndroidTestCase;

import java.net.InetAddress;

public class AudioStreamTest extends AndroidTestCase {

    private void testRtpStream(InetAddress address) throws Exception {
        AudioStream stream = new AudioStream(address);
        assertEquals(stream.getLocalAddress(), address);
        assertEquals(stream.getLocalPort() % 2, 0);

        assertNull(stream.getRemoteAddress());
        assertEquals(stream.getRemotePort(), -1);
        stream.associate(address, 1000);
        assertEquals(stream.getRemoteAddress(), address);
        assertEquals(stream.getRemotePort(), 1000);

        assertFalse(stream.isBusy());
        stream.release();
    }

    public void testV4Stream() throws Exception {
        testRtpStream(InetAddress.getByName("127.0.0.1"));
    }

    public void testV6Stream() throws Exception {
        testRtpStream(InetAddress.getByName("::1"));
    }

    public void testSetDtmfType() throws Exception {
        AudioStream stream = new AudioStream(InetAddress.getByName("::1"));

        assertEquals(stream.getDtmfType(), -1);
        try {
            stream.setDtmfType(0);
            fail("Expecting IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ignore
        }
        stream.setDtmfType(96);
        assertEquals(stream.getDtmfType(), 96);

        stream.setCodec(AudioCodec.getCodec(97, "PCMU/8000", null));
        try {
            stream.setDtmfType(97);
            fail("Expecting IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ignore
        }
        stream.release();
    }

    public void testSetCodec() throws Exception {
        AudioStream stream = new AudioStream(InetAddress.getByName("::1"));

        assertNull(stream.getCodec());
        stream.setCodec(AudioCodec.getCodec(97, "PCMU/8000", null));
        assertNotNull(stream.getCodec());

        stream.setDtmfType(96);
        try {
            stream.setCodec(AudioCodec.getCodec(96, "PCMU/8000", null));
            fail("Expecting IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ignore
        }
        stream.release();
    }

    public void testDoubleRelease() throws Exception {
        AudioStream stream = new AudioStream(InetAddress.getByName("::1"));
        stream.release();
        stream.release();
    }
}
