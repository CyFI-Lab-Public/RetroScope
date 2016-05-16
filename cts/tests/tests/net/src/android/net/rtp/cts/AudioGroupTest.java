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

import android.content.Context;
import android.media.AudioManager;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.net.rtp.RtpStream;
import android.test.AndroidTestCase;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class AudioGroupTest extends AndroidTestCase {

    private static final String TAG = AudioGroupTest.class.getSimpleName();

    private AudioManager mAudioManager;

    private AudioStream mStreamA;
    private DatagramSocket mSocketA;
    private AudioStream mStreamB;
    private DatagramSocket mSocketB;
    private AudioGroup mGroup;

    @Override
    public void setUp() throws Exception {
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        InetAddress local = InetAddress.getByName("::1");

        mStreamA = new AudioStream(local);
        mStreamA.setMode(RtpStream.MODE_NORMAL);
        mStreamA.setCodec(AudioCodec.PCMU);
        mSocketA = new DatagramSocket();
        mSocketA.connect(mStreamA.getLocalAddress(), mStreamA.getLocalPort());
        mStreamA.associate(mSocketA.getLocalAddress(), mSocketA.getLocalPort());

        mStreamB = new AudioStream(local);
        mStreamB.setMode(RtpStream.MODE_NORMAL);
        mStreamB.setCodec(AudioCodec.PCMU);
        mSocketB = new DatagramSocket();
        mSocketB.connect(mStreamB.getLocalAddress(), mStreamB.getLocalPort());
        mStreamB.associate(mSocketB.getLocalAddress(), mSocketB.getLocalPort());

        mGroup = new AudioGroup();
    }

    @Override
    public void tearDown() throws Exception {
        mGroup.clear();
        mStreamA.release();
        mSocketA.close();
        mStreamB.release();
        mSocketB.close();
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
    }

    private void assertPacket(DatagramSocket socket, int length) throws Exception {
        DatagramPacket packet = new DatagramPacket(new byte[length + 1], length + 1);
        socket.setSoTimeout(3000);
        socket.receive(packet);
        assertEquals(packet.getLength(), length);
    }

    private void drain(DatagramSocket socket) throws Exception {
        DatagramPacket packet = new DatagramPacket(new byte[1], 1);
        socket.setSoTimeout(1);
        try {
            // Drain the socket by retrieving all the packets queued on it.
            // A SocketTimeoutException will be thrown when it becomes empty.
            while (true) {
                socket.receive(packet);
            }
        } catch (Exception e) {
            // ignore.
        }
    }

    public void testTraffic() throws Exception {
        mStreamA.join(mGroup);
        assertPacket(mSocketA, 12 + 160);

        mStreamB.join(mGroup);
        assertPacket(mSocketB, 12 + 160);

        mStreamA.join(null);
        drain(mSocketA);

        drain(mSocketB);
        assertPacket(mSocketB, 12 + 160);

        mStreamA.join(mGroup);
        assertPacket(mSocketA, 12 + 160);
    }

    public void testSetMode() throws Exception {
        mGroup.setMode(AudioGroup.MODE_NORMAL);
        assertEquals(mGroup.getMode(), AudioGroup.MODE_NORMAL);

        mGroup.setMode(AudioGroup.MODE_MUTED);
        assertEquals(mGroup.getMode(), AudioGroup.MODE_MUTED);

        mStreamA.join(mGroup);
        mStreamB.join(mGroup);

        mGroup.setMode(AudioGroup.MODE_NORMAL);
        assertEquals(mGroup.getMode(), AudioGroup.MODE_NORMAL);

        mGroup.setMode(AudioGroup.MODE_MUTED);
        assertEquals(mGroup.getMode(), AudioGroup.MODE_MUTED);
    }

    public void testAdd() throws Exception {
        mStreamA.join(mGroup);
        assertEquals(mGroup.getStreams().length, 1);

        mStreamB.join(mGroup);
        assertEquals(mGroup.getStreams().length, 2);

        mStreamA.join(mGroup);
        assertEquals(mGroup.getStreams().length, 2);
    }

    public void testRemove() throws Exception {
        mStreamA.join(mGroup);
        assertEquals(mGroup.getStreams().length, 1);

        mStreamA.join(null);
        assertEquals(mGroup.getStreams().length, 0);

        mStreamA.join(mGroup);
        assertEquals(mGroup.getStreams().length, 1);
    }

    public void testClear() throws Exception {
        mStreamA.join(mGroup);
        mStreamB.join(mGroup);
        mGroup.clear();

        assertEquals(mGroup.getStreams().length, 0);
        assertFalse(mStreamA.isBusy());
        assertFalse(mStreamB.isBusy());
    }

    public void testDoubleClear() throws Exception {
        mStreamA.join(mGroup);
        mStreamB.join(mGroup);
        mGroup.clear();
        mGroup.clear();
    }
}
