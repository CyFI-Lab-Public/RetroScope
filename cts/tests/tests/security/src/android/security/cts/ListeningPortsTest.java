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

package android.security.cts;

import android.content.pm.PackageManager;
import android.test.AndroidTestCase;
import junit.framework.AssertionFailedError;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Verifies that Android devices are not listening on accessible
 * open ports. Open ports are often targeted by attackers looking to break
 * into computer systems remotely, and minimizing the number of open ports
 * is considered a security best practice.
 */
public class ListeningPortsTest extends AndroidTestCase {

    /** Ports that are allowed to be listening. */
    private static final List<String> EXCEPTION_PATTERNS = new ArrayList<String>(6);

    static {
        // IPv4 exceptions
        EXCEPTION_PATTERNS.add("0.0.0.0:5555");   // emulator port
        EXCEPTION_PATTERNS.add("10.0.2.15:5555"); // net forwarding for emulator
        EXCEPTION_PATTERNS.add("127.0.0.1:5037"); // adb daemon "smart sockets"
    }

    /**
     * Remotely accessible ports are often used by attackers to gain
     * unauthorized access to computers systems without user knowledge or
     * awareness.
     */
    public void testNoRemotelyAccessibleListeningTcpPorts() throws Exception {
        assertNoAccessibleListeningPorts("/proc/net/tcp", true, false);
    }

    /**
     * Remotely accessible ports are often used by attackers to gain
     * unauthorized access to computers systems without user knowledge or
     * awareness.
     */
    public void testNoRemotelyAccessibleListeningTcp6Ports() throws Exception {
        assertNoAccessibleListeningPorts("/proc/net/tcp6", true, false);
    }

    /**
     * Remotely accessible ports are often used by attackers to gain
     * unauthorized access to computers systems without user knowledge or
     * awareness.
     */
    public void testNoRemotelyAccessibleListeningUdpPorts() throws Exception {
        assertNoRemotelyAccessibleListeningUdpPorts("/proc/net/udp", false);
    }

    /**
     * Remotely accessible ports are often used by attackers to gain
     * unauthorized access to computers systems without user knowledge or
     * awareness.
     */
    public void testNoRemotelyAccessibleListeningUdp6Ports() throws Exception {
        assertNoRemotelyAccessibleListeningUdpPorts("/proc/net/udp6", false);
    }

    /**
     * Locally accessible ports are often targeted by malicious locally
     * installed programs to gain unauthorized access to program data or
     * cause system corruption.
     *
     * In all cases, a local listening IP port can be replaced by a UNIX domain
     * socket. Unix domain sockets can be protected with unix filesystem
     * permission. Alternatively, you can use getsockopt(SO_PEERCRED) to
     * determine if a program is authorized to connect to your socket.
     *
     * Please convert loopback IP connections to unix domain sockets.
     */
    public void testNoListeningLoopbackTcpPorts() throws Exception {
        assertNoAccessibleListeningPorts("/proc/net/tcp", true, true);
    }

    /**
     * Locally accessible ports are often targeted by malicious locally
     * installed programs to gain unauthorized access to program data or
     * cause system corruption.
     *
     * In all cases, a local listening IP port can be replaced by a UNIX domain
     * socket. Unix domain sockets can be protected with unix filesystem
     * permission. Alternatively, you can use getsockopt(SO_PEERCRED) to
     * determine if a program is authorized to connect to your socket.
     *
     * Please convert loopback IP connections to unix domain sockets.
     */
    public void testNoListeningLoopbackTcp6Ports() throws Exception {
        assertNoAccessibleListeningPorts("/proc/net/tcp6", true, true);
    }

    /**
     * Locally accessible ports are often targeted by malicious locally
     * installed programs to gain unauthorized access to program data or
     * cause system corruption.
     *
     * In all cases, a local listening IP port can be replaced by a UNIX domain
     * socket. Unix domain sockets can be protected with unix filesystem
     * permission.  Alternately, or you can use setsockopt(SO_PASSCRED) to
     * send credentials, and recvmsg to retrieve the passed credentials.
     *
     * Please convert loopback IP connections to unix domain sockets.
     */
    public void testNoListeningLoopbackUdpPorts() throws Exception {
        assertNoAccessibleListeningPorts("/proc/net/udp", false, true);
    }

    /**
     * Locally accessible ports are often targeted by malicious locally
     * installed programs to gain unauthorized access to program data or
     * cause system corruption.
     *
     * In all cases, a local listening IP port can be replaced by a UNIX domain
     * socket. Unix domain sockets can be protected with unix filesystem
     * permission.  Alternately, or you can use setsockopt(SO_PASSCRED) to
     * send credentials, and recvmsg to retrieve the passed credentials.
     *
     * Please convert loopback IP connections to unix domain sockets.
     */
    public void testNoListeningLoopbackUdp6Ports() throws Exception {
        assertNoAccessibleListeningPorts("/proc/net/udp6", false, true);
    }

    private static final int RETRIES_MAX = 6;

    /**
     * UDP tests can be flaky due to DNS lookups.  Compensate.
     */
    private void assertNoRemotelyAccessibleListeningUdpPorts(
            String procFilePath, boolean loopback)
            throws Exception {
        for (int i = 0; i < RETRIES_MAX; i++) {
            try {
                assertNoAccessibleListeningPorts(procFilePath, false, loopback);
                return;
            } catch (ListeningPortsAssertionError e) {
                if (i == RETRIES_MAX - 1) {
                    throw e;
                }
                Thread.sleep(2 * 1000 * i);
            }
        }
        throw new IllegalStateException("unreachable");
    }

    /**
     * Remotely accessible ports (loopback==false) are often used by
     * attackers to gain unauthorized access to computers systems without
     * user knowledge or awareness.
     *
     * Locally accessible ports (loopback==true) are often targeted by
     * malicious locally installed programs to gain unauthorized access to
     * program data or cause system corruption.
     */
    private void assertNoAccessibleListeningPorts(
            String procFilePath, boolean isTcp, boolean loopback) throws IOException {
        String errors = "";
        List<ParsedProcEntry> entries = ParsedProcEntry.parse(procFilePath);
        for (ParsedProcEntry entry : entries) {
            String addrPort = entry.localAddress.getHostAddress() + ':' + entry.port;

            if (isPortListening(entry.state, isTcp)
                && !isException(addrPort)
                && (!entry.localAddress.isLoopbackAddress() ^ loopback)) {
                errors += "\nFound port listening on addr="
                        + entry.localAddress.getHostAddress() + ", port="
                        + entry.port + ", UID=" + entry.uid
                        + " " + uidToPackage(entry.uid) + " in "
                        + procFilePath;
            }
        }
        if (!errors.equals("")) {
            throw new ListeningPortsAssertionError(errors);
        }
    }

    private String uidToPackage(int uid) {
        PackageManager pm = this.getContext().getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);
        if (packages == null) {
            return "[unknown]";
        }
        return Arrays.asList(packages).toString();
    }

    private static boolean isException(String localAddress) {
        return isPatternMatch(EXCEPTION_PATTERNS, localAddress);
    }

    private static boolean isPatternMatch(List<String> patterns, String input) {
        for (String pattern : patterns) {
            pattern = Pattern.quote(pattern);
            if (Pattern.matches(pattern, input)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPortListening(String state, boolean isTcp) {
        // 0A = TCP_LISTEN from include/net/tcp_states.h
        String listeningState = isTcp ? "0A" : "07";
        return listeningState.equals(state);
    }

    private static class ListeningPortsAssertionError extends AssertionFailedError {
        private ListeningPortsAssertionError(String msg) {
            super(msg);
        }
    }

    private static class ParsedProcEntry {
        private final InetAddress localAddress;
        private final int port;
        private final String state;
        private final int uid;

        private ParsedProcEntry(InetAddress addr, int port, String state, int uid) {
            this.localAddress = addr;
            this.port = port;
            this.state = state;
            this.uid = uid;
        }


        private static List<ParsedProcEntry> parse(String procFilePath) throws IOException {

            List<ParsedProcEntry> retval = new ArrayList<ParsedProcEntry>();
            /*
            * Sample output of "cat /proc/net/tcp" on emulator:
            *
            * sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  ...
            * 0: 0100007F:13AD 00000000:0000 0A 00000000:00000000 00:00000000 00000000     0   ...
            * 1: 00000000:15B3 00000000:0000 0A 00000000:00000000 00:00000000 00000000     0   ...
            * 2: 0F02000A:15B3 0202000A:CE8A 01 00000000:00000000 00:00000000 00000000     0   ...
            *
            */

            File procFile = new File(procFilePath);
            Scanner scanner = null;
            try {
                scanner = new Scanner(procFile);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();

                    // Skip column headers
                    if (line.startsWith("sl")) {
                        continue;
                    }

                    String[] fields = line.split("\\s+");
                    final int expectedNumColumns = 12;
                    assertTrue(procFilePath + " should have at least " + expectedNumColumns
                            + " columns of output " + fields, fields.length >= expectedNumColumns);

                    String state = fields[3];
                    int uid = Integer.parseInt(fields[7]);
                    InetAddress localIp = addrToInet(fields[1].split(":")[0]);
                    int localPort = Integer.parseInt(fields[1].split(":")[1], 16);

                    retval.add(new ParsedProcEntry(localIp, localPort, state, uid));
                }
            } finally {
                if (scanner != null) {
                    scanner.close();
                }
            }
            return retval;
        }

        /**
         * Convert a string stored in little endian format to an IP address.
         */
        private static InetAddress addrToInet(String s) throws UnknownHostException {
            int len = s.length();
            if (len != 8 && len != 32) {
                throw new IllegalArgumentException(len + "");
            }
            byte[] retval = new byte[len / 2];

            for (int i = 0; i < len / 2; i += 4) {
                retval[i] = (byte) ((Character.digit(s.charAt(2*i + 6), 16) << 4)
                        + Character.digit(s.charAt(2*i + 7), 16));
                retval[i + 1] = (byte) ((Character.digit(s.charAt(2*i + 4), 16) << 4)
                        + Character.digit(s.charAt(2*i + 5), 16));
                retval[i + 2] = (byte) ((Character.digit(s.charAt(2*i + 2), 16) << 4)
                        + Character.digit(s.charAt(2*i + 3), 16));
                retval[i + 3] = (byte) ((Character.digit(s.charAt(2*i), 16) << 4)
                        + Character.digit(s.charAt(2*i + 1), 16));
            }
            return InetAddress.getByAddress(retval);
        }
    }
}
