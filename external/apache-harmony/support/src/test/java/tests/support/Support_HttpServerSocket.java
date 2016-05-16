/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests.support;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class implements the Support_ServerSocket interface using java.net
 * Serversockets
 */

public class Support_HttpServerSocket implements Support_ServerSocket {
	private ServerSocket instance = null;

	private int port = -1;

	private int timeout = 8000;

	/**
	 * Blocks until a connection is made, or the socket times out.
	 * 
	 * @see tests.support.Support_ServerSocket#accept()
	 */
	public Support_Socket accept() throws IOException {
		if (port == -1) {
            return null;
        }
		if (instance == null) {
            return null;
        }
		instance.setSoTimeout(timeout);
		Socket s = instance.accept();
		return new Support_HttpSocket(s);
	}

	/**
	 * @see tests.support.Support_ServerSocket#setTimeout(int) Sets the
	 *      timeout for the server.
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * @see tests.support.Support_ServerSocket#setPort(int)
	 */
	public void setPort(int port) {
		this.port = port;
	}

	public void open() throws IOException {
		instance = new ServerSocket(port);
	}

	/**
	 * @see tests.support.Support_ServerSocket#close()
	 */
	public void close() throws IOException {
		if (instance != null) {
            instance.close();
        }
	}

}
