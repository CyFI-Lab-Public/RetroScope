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
import android.test.AndroidTestCase;

public class AudioCodecTest extends AndroidTestCase {

    private void assertEquals(AudioCodec codec, int type, String rtpmap, String fmtp) {
        if (type >= 0) {
            assertEquals(codec.type, type);
        } else {
            assertTrue(codec.type >= 96 && codec.type <= 127);
        }
        assertEquals(codec.rtpmap.compareToIgnoreCase(rtpmap), 0);
        assertEquals(codec.fmtp, fmtp);
    }

    public void testConstants() throws Exception {
        assertEquals(AudioCodec.PCMU, 0, "PCMU/8000", null);
        assertEquals(AudioCodec.PCMA, 8, "PCMA/8000", null);
        assertEquals(AudioCodec.GSM, 3, "GSM/8000", null);
        assertEquals(AudioCodec.GSM_EFR, -1, "GSM-EFR/8000", null);
        assertEquals(AudioCodec.AMR, -1, "AMR/8000", null);

        assertFalse(AudioCodec.AMR.type == AudioCodec.GSM_EFR.type);
    }

    public void testGetCodec() throws Exception {
        // Bad types.
        assertNull(AudioCodec.getCodec(128, "PCMU/8000", null));
        assertNull(AudioCodec.getCodec(-1, "PCMU/8000", null));
        assertNull(AudioCodec.getCodec(96, null, null));

        // Fixed types.
        assertEquals(AudioCodec.getCodec(0, null, null), 0, "PCMU/8000", null);
        assertEquals(AudioCodec.getCodec(8, null, null), 8, "PCMA/8000", null);
        assertEquals(AudioCodec.getCodec(3, null, null), 3, "GSM/8000", null);

        // Dynamic types.
        assertEquals(AudioCodec.getCodec(96, "pcmu/8000", null), 96, "PCMU/8000", null);
        assertEquals(AudioCodec.getCodec(97, "pcma/8000", null), 97, "PCMA/8000", null);
        assertEquals(AudioCodec.getCodec(98, "gsm/8000", null), 98, "GSM/8000", null);
        assertEquals(AudioCodec.getCodec(99, "gsm-efr/8000", null), 99, "GSM-EFR/8000", null);
        assertEquals(AudioCodec.getCodec(100, "amr/8000", null), 100, "AMR/8000", null);
    }

    public void testGetCodecs() throws Exception {
        AudioCodec[] codecs = AudioCodec.getCodecs();
        assertTrue(codecs.length >= 5);

        // The types of the codecs should be different.
        boolean[] types = new boolean[128];
        for (AudioCodec codec : codecs) {
            assertFalse(types[codec.type]);
            types[codec.type] = true;
        }
    }
}
