/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.net.cts;

import android.net.TrafficStats;
import android.os.Process;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TrafficStatsTest extends AndroidTestCase {
    private static final String LOG_TAG = "TrafficStatsTest";

    public void testValidMobileStats() {
        // We can't assume a mobile network is even present in this test, so
        // we simply assert that a valid value is returned.

        assertTrue(TrafficStats.getMobileTxPackets() >= 0);
        assertTrue(TrafficStats.getMobileRxPackets() >= 0);
        assertTrue(TrafficStats.getMobileTxBytes() >= 0);
        assertTrue(TrafficStats.getMobileRxBytes() >= 0);
    }

    public void testValidTotalStats() {
        assertTrue(TrafficStats.getTotalTxPackets() >= 0);
        assertTrue(TrafficStats.getTotalRxPackets() >= 0);
        assertTrue(TrafficStats.getTotalTxBytes() >= 0);
        assertTrue(TrafficStats.getTotalRxBytes() >= 0);
    }

    public void testThreadStatsTag() throws Exception {
        TrafficStats.setThreadStatsTag(0xf00d);
        assertTrue("Tag didn't stick", TrafficStats.getThreadStatsTag() == 0xf00d);

        final CountDownLatch latch = new CountDownLatch(1);

        new Thread("TrafficStatsTest.testThreadStatsTag") {
            @Override
            public void run() {
                assertTrue("Tag leaked", TrafficStats.getThreadStatsTag() != 0xf00d);
                TrafficStats.setThreadStatsTag(0xcafe);
                assertTrue("Tag didn't stick", TrafficStats.getThreadStatsTag() == 0xcafe);
                latch.countDown();
            }
        }.start();

        latch.await(5, TimeUnit.SECONDS);
        assertTrue("Tag lost", TrafficStats.getThreadStatsTag() == 0xf00d);

        TrafficStats.clearThreadStatsTag();
        assertTrue("Tag not cleared", TrafficStats.getThreadStatsTag() != 0xf00d);
    }

    long tcpPacketToIpBytes(long packetCount, long bytes) {
        // ip header + tcp header + data.
        // Tcp header is mostly 32. Syn has different tcp options -> 40. Don't care.
        return packetCount * (20 + 32 + bytes);
    }

    private void accessOwnTrafficStats() throws IOException {
        final int ownAppUid = getContext().getApplicationInfo().uid;
        Log.d(LOG_TAG, "accesOwnTrafficStatsWithTags(): about to read qtaguid stats for own uid " + ownAppUid);

        boolean foundOwnDetailedStats = false;
        try {
            BufferedReader qtaguidReader = new BufferedReader(new FileReader("/proc/net/xt_qtaguid/stats"));
            String line;
            while ((line = qtaguidReader.readLine()) != null) {
                String tokens[] = line.split(" ");
                if (tokens.length > 3 && tokens[3].equals(String.valueOf(ownAppUid))) {
                    Log.d(LOG_TAG, "accessOwnTrafficStatsWithTags(): got own stats: " + line);
                }
            }
            qtaguidReader.close();
        } catch (FileNotFoundException e) {
            fail("Was not able to access qtaguid/stats: " + e);
        }
    }

    public void testTrafficStatsForLocalhost() throws IOException {
        final long mobileTxPacketsBefore = TrafficStats.getMobileTxPackets();
        final long mobileRxPacketsBefore = TrafficStats.getMobileRxPackets();
        final long mobileTxBytesBefore = TrafficStats.getMobileTxBytes();
        final long mobileRxBytesBefore = TrafficStats.getMobileRxBytes();
        final long totalTxPacketsBefore = TrafficStats.getTotalTxPackets();
        final long totalRxPacketsBefore = TrafficStats.getTotalRxPackets();
        final long totalTxBytesBefore = TrafficStats.getTotalTxBytes();
        final long totalRxBytesBefore = TrafficStats.getTotalRxBytes();
        final long uidTxBytesBefore = TrafficStats.getUidTxBytes(Process.myUid());
        final long uidRxBytesBefore = TrafficStats.getUidRxBytes(Process.myUid());
        final long uidTxPacketsBefore = TrafficStats.getUidTxPackets(Process.myUid());
        final long uidRxPacketsBefore = TrafficStats.getUidRxPackets(Process.myUid());

        // Transfer 1MB of data across an explicitly localhost socket.
        final int byteCount = 1024;
        final int packetCount = 1024;

        final ServerSocket server = new ServerSocket(0);
        new Thread("TrafficStatsTest.testTrafficStatsForLocalhost") {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket("localhost", server.getLocalPort());
                    // Make sure that each write()+flush() turns into a packet:
                    // disable Nagle.
                    socket.setTcpNoDelay(true);
                    OutputStream out = socket.getOutputStream();
                    byte[] buf = new byte[byteCount];
                    TrafficStats.setThreadStatsTag(0x42);
                    TrafficStats.tagSocket(socket);
                    accessOwnTrafficStats();
                    for (int i = 0; i < packetCount; i++) {
                        out.write(buf);
                        out.flush();
                        try {
                            // Bug: 10668088, Even with Nagle disabled, and flushing the 1024 bytes
                            // the kernel still regroups data into a larger packet.
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                        }
                    }
                    out.close();
                    socket.close();
                    accessOwnTrafficStats();
                } catch (IOException e) {
                    Log.i(LOG_TAG, "Badness during writes to socket: " + e);
                }
            }
        }.start();

        int read = 0;
        try {
            Socket socket = server.accept();
            socket.setTcpNoDelay(true);
            TrafficStats.setThreadStatsTag(0x43);
            TrafficStats.tagSocket(socket);
            InputStream in = socket.getInputStream();
            byte[] buf = new byte[byteCount];
            while (read < byteCount * packetCount) {
                int n = in.read(buf);
                assertTrue("Unexpected EOF", n > 0);
                read += n;
            }
        } finally {
            server.close();
        }
        assertTrue("Not all data read back", read >= byteCount * packetCount);

        // It's too fast to call getUidTxBytes function.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        long mobileTxPacketsAfter = TrafficStats.getMobileTxPackets();
        long mobileRxPacketsAfter = TrafficStats.getMobileRxPackets();
        long mobileTxBytesAfter = TrafficStats.getMobileTxBytes();
        long mobileRxBytesAfter = TrafficStats.getMobileRxBytes();
        long totalTxPacketsAfter = TrafficStats.getTotalTxPackets();
        long totalRxPacketsAfter = TrafficStats.getTotalRxPackets();
        long totalTxBytesAfter = TrafficStats.getTotalTxBytes();
        long totalRxBytesAfter = TrafficStats.getTotalRxBytes();
        long uidTxBytesAfter = TrafficStats.getUidTxBytes(Process.myUid());
        long uidRxBytesAfter = TrafficStats.getUidRxBytes(Process.myUid());
        long uidTxPacketsAfter = TrafficStats.getUidTxPackets(Process.myUid());
        long uidRxPacketsAfter = TrafficStats.getUidRxPackets(Process.myUid());
        long uidTxDeltaBytes = uidTxBytesAfter - uidTxBytesBefore;
        long uidTxDeltaPackets = uidTxPacketsAfter - uidTxPacketsBefore;
        long uidRxDeltaBytes = uidRxBytesAfter - uidRxBytesBefore;
        long uidRxDeltaPackets = uidRxPacketsAfter - uidRxPacketsBefore;

        // Localhost traffic *does* count against per-UID stats.
        /*
         * Calculations:
         *  - bytes
         *   bytes is approx: packets * data + packets * acks;
         *   but sometimes there are less acks than packets, so we set a lower
         *   limit of 1 ack.
         *  - setup/teardown
         *   + 7 approx.: syn, syn-ack, ack, fin-ack, ack, fin-ack, ack;
         *   but sometimes the last find-acks just vanish, so we set a lower limit of +5.
         */
        final int maxExpectedExtraPackets = 7;
        final int minExpectedExtraPackets = 5;


        assertTrue("uidtxp: " + uidTxPacketsBefore + " -> " + uidTxPacketsAfter + " delta=" + uidTxDeltaPackets +
            " Wanted: " + uidTxDeltaPackets + ">=" + packetCount + "+" + minExpectedExtraPackets + " && " +
            uidTxDeltaPackets + "<=" + packetCount + "+" + packetCount + "+" + maxExpectedExtraPackets,
            uidTxDeltaPackets >= packetCount + minExpectedExtraPackets &&
            uidTxDeltaPackets <= packetCount + packetCount + maxExpectedExtraPackets);
        assertTrue("uidrxp: " + uidRxPacketsBefore + " -> " + uidRxPacketsAfter + " delta=" + uidRxDeltaPackets +
            " Wanted: " + uidRxDeltaPackets + ">=" + packetCount + "+" + minExpectedExtraPackets + " && " +
            uidRxDeltaPackets + "<=" + packetCount + "+" + packetCount + "+" + maxExpectedExtraPackets,
            uidRxDeltaPackets >= packetCount + minExpectedExtraPackets &&
            uidRxDeltaPackets <= packetCount + packetCount + maxExpectedExtraPackets);
        assertTrue("uidtxb: " + uidTxBytesBefore + " -> " + uidTxBytesAfter + " delta=" + uidTxDeltaBytes +
            " Wanted: " + uidTxDeltaBytes + ">=" + tcpPacketToIpBytes(packetCount, byteCount) + "+" + tcpPacketToIpBytes(minExpectedExtraPackets, 0) + " && " +
            uidTxDeltaBytes + "<=" + tcpPacketToIpBytes(packetCount, byteCount) + "+" + tcpPacketToIpBytes(packetCount + maxExpectedExtraPackets, 0),
            uidTxDeltaBytes >= tcpPacketToIpBytes(packetCount, byteCount) + tcpPacketToIpBytes(minExpectedExtraPackets, 0) &&
            uidTxDeltaBytes <= tcpPacketToIpBytes(packetCount, byteCount) + tcpPacketToIpBytes(packetCount + maxExpectedExtraPackets, 0));
        assertTrue("uidrxb: " + uidRxBytesBefore + " -> " + uidRxBytesAfter + " delta=" + uidRxDeltaBytes +
            " Wanted: " + uidRxDeltaBytes + ">=" + tcpPacketToIpBytes(packetCount, byteCount) + "+" + tcpPacketToIpBytes(minExpectedExtraPackets, 0) + " && " +
            uidRxDeltaBytes + "<=" + tcpPacketToIpBytes(packetCount, byteCount) + "+" + tcpPacketToIpBytes(packetCount + maxExpectedExtraPackets, 0),
            uidRxDeltaBytes >= tcpPacketToIpBytes(packetCount, byteCount) + tcpPacketToIpBytes(minExpectedExtraPackets, 0) &&
            uidRxDeltaBytes <= tcpPacketToIpBytes(packetCount, byteCount) + tcpPacketToIpBytes(packetCount + maxExpectedExtraPackets, 0));

        // Localhost traffic *does* count against total stats.
        // Fudge by 132 packets of 1500 bytes not related to the test.
        assertTrue("ttxp: " + totalTxPacketsBefore + " -> " + totalTxPacketsAfter,
            totalTxPacketsAfter >= totalTxPacketsBefore + uidTxDeltaPackets &&
            totalTxPacketsAfter <= totalTxPacketsBefore + uidTxDeltaPackets + 132);
        assertTrue("trxp: " + totalRxPacketsBefore + " -> " + totalRxPacketsAfter,
            totalRxPacketsAfter >= totalRxPacketsBefore + uidRxDeltaPackets &&
            totalRxPacketsAfter <= totalRxPacketsBefore + uidRxDeltaPackets + 132);
        assertTrue("ttxb: " + totalTxBytesBefore + " -> " + totalTxBytesAfter,
            totalTxBytesAfter >= totalTxBytesBefore + uidTxDeltaBytes &&
            totalTxBytesAfter <= totalTxBytesBefore + uidTxDeltaBytes + 132 * 1500);
        assertTrue("trxb: " + totalRxBytesBefore + " -> " + totalRxBytesAfter,
            totalRxBytesAfter >= totalRxBytesBefore + uidRxDeltaBytes &&
            totalRxBytesAfter <= totalRxBytesBefore + uidRxDeltaBytes + 132 * 1500);

        // Localhost traffic should *not* count against mobile stats,
        // There might be some other traffic, but nowhere near 1MB.
        assertTrue("mtxp: " + mobileTxPacketsBefore + " -> " + mobileTxPacketsAfter,
            mobileTxPacketsAfter >= mobileTxPacketsBefore &&
            mobileTxPacketsAfter <= mobileTxPacketsBefore + 500);
        assertTrue("mrxp: " + mobileRxPacketsBefore + " -> " + mobileRxPacketsAfter,
            mobileRxPacketsAfter >= mobileRxPacketsBefore &&
            mobileRxPacketsAfter <= mobileRxPacketsBefore + 500);
        assertTrue("mtxb: " + mobileTxBytesBefore + " -> " + mobileTxBytesAfter,
            mobileTxBytesAfter >= mobileTxBytesBefore &&
            mobileTxBytesAfter <= mobileTxBytesBefore + 200000);
        assertTrue("mrxb: " + mobileRxBytesBefore + " -> " + mobileRxBytesAfter,
            mobileRxBytesAfter >= mobileRxBytesBefore &&
            mobileRxBytesAfter <= mobileRxBytesBefore + 200000);

    }
}
