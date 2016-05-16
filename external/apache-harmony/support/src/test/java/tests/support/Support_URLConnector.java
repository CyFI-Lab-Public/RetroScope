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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class implements the Support_HttpConnector interface using java.net
 * URL's
 */
public class Support_URLConnector implements Support_HttpConnector {
	private URLConnection instance;

	private boolean streamOpen = false;

	/**
	 * @see com.ibm.support.Support_HttpConnector#open(String)
	 */
	public void open(String address) throws IOException {
		instance = new URL(address).openConnection();
	}

	public void close() throws IOException {
		if (!streamOpen) {
			((HttpURLConnection) instance).disconnect();
		}
	}

	/**
	 * @see com.ibm.support.Support_HttpConnector#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
		if (instance == null) {
            return null;
        }
		streamOpen = true;
		return instance.getInputStream();
	}

	public OutputStream getOutputStream() throws IOException {
		if (instance == null) {
            return null;
        }
		instance.setDoOutput(true);
		((HttpURLConnection) instance).setRequestMethod("POST");
		streamOpen = true;
		return instance.getOutputStream();
	}

	public boolean isChunkedOnFlush() {
		return false;
	}

	public void setRequestProperty(String key, String value) throws IOException {
		instance.setRequestProperty(key, value);
	}

	public String getHeaderField(int index) throws IOException {
		return ((HttpURLConnection) instance).getHeaderField(index);
	}

	public String getHeaderFieldKey(int index) throws IOException {
		return ((HttpURLConnection) instance).getHeaderFieldKey(index);
	}
}
