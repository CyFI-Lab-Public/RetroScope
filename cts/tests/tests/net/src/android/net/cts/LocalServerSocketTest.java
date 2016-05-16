/*
 * Copyright (C) 2009 The Android Open Source Project
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.test.AndroidTestCase;

public class LocalServerSocketTest extends AndroidTestCase {

    public void testLocalServerSocket() throws IOException {
        LocalServerSocket localServerSocket = new LocalServerSocket(LocalSocketTest.mSockAddr);
        assertNotNull(localServerSocket.getLocalSocketAddress());
        commonFunctions(localServerSocket);
    }

    public void commonFunctions(LocalServerSocket localServerSocket) throws IOException {
        // create client socket
        LocalSocket clientSocket = new LocalSocket();

        // establish connection between client and server
        clientSocket.connect(new LocalSocketAddress(LocalSocketTest.mSockAddr));
        LocalSocket serverSocket = localServerSocket.accept();

        // send data from client to server
        OutputStream clientOutStream = clientSocket.getOutputStream();
        clientOutStream.write(12);
        InputStream serverInStream = serverSocket.getInputStream();
        assertEquals(12, serverInStream.read());

        // send data from server to client
        OutputStream serverOutStream = serverSocket.getOutputStream();
        serverOutStream.write(3);
        InputStream clientInStream = clientSocket.getInputStream();
        assertEquals(3, clientInStream.read());

        // close server socket
        assertNotNull(localServerSocket.getFileDescriptor());
        localServerSocket.close();
        assertNull(localServerSocket.getFileDescriptor());
    }
}
