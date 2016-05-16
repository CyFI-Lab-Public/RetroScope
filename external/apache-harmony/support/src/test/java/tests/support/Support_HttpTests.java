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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import junit.framework.Assert;

/**
 * Performs some basic testing of either HttpConnection or HttpURLConnection
 * depending on the Support_ServerSocket and Support_HttpConnector passed to the
 * constructor.
 * 
 */
public class Support_HttpTests {

	private Support_ServerSocket serversocket;

	private Support_HttpConnector connector;

	public Support_HttpTests(Support_ServerSocket serversocket,
			Support_HttpConnector client) {
		this.serversocket = serversocket;
		this.connector = client;
	}

	public void runTests(junit.framework.TestCase test) {

		// get a port to use for the test
		int portNumber = Support_PortManager.getNextPort();

		// url's for the various tests
		final String chunkedTestUrl = "http://localhost:" + portNumber
				+ Support_HttpServer.CHUNKEDTEST;
		final String contentTestUrl = "http://localhost:" + portNumber
				+ Support_HttpServer.CONTENTTEST;
		final String redirectTestUrl = "http://localhost:" + portNumber
				+ Support_HttpServer.REDIRECTTEST;
		final String postTestUrl = "http://localhost:" + portNumber
				+ Support_HttpServer.POSTTEST;
		final String headersTestUrl = "http://localhost:" + portNumber
				+ Support_HttpServer.HEADERSTEST;

		// start the test server. It will timeout and shut down after
		// 5 seconds of inactivity
		Support_HttpServer server = new Support_HttpServer(serversocket, test);
		server.startServer(portNumber);

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		InputStream is;
		int c;

		// Chunked HTTP Transfer Coding Test
		try {
			// access the url and open a stream
			connector.open(chunkedTestUrl);
			is = connector.getInputStream();

			// receive the data, and then read again after EOF
			c = is.read();
			while (c > 0) {
                c = is.read();
            }
			c = is.read();
			is.close();
			connector.close();
			Assert.assertEquals("Error receiving chunked transfer coded data",
					-1, c);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception during test a: " + e);
		}

		// Content-Length Test
		try {
			connector.open(contentTestUrl);
			is = connector.getInputStream();
			bout.reset();
			do {
				c = is.read();
				if (c != -1) {
                    bout.write(c);
                }
			} while (c != -1);
			is.close();
			connector.close();
			String result = new String(bout.toByteArray(), "ISO8859_1");
			Assert.assertTrue("Error receiving content coded data: " + result,
					"ABCDE".equals(result));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception during test b: " + e);
		}

		// Headers Test
		try {
			connector.open(headersTestUrl);
			connector.setRequestProperty("header1", "value1");
			connector.setRequestProperty("header1", "value2");
			int i = 0, found = 0;
			String[] expected = new String[] { "no-cache=\"set-cookie\"",
					"private", "no-transform" };
			while (true) {
				String key = connector.getHeaderFieldKey(i);
				if (key == null && i > 0) {
                    break;
                }
				if ("Cache-Control".equals(key)) {
					Assert.assertTrue("Too many headers at: " + i, found <= 2);
					String value = connector.getHeaderField(i);
					Assert.assertTrue("Invalid header value: " + found + ": "
							+ value, expected[found].equals(value));
					found++;
				}
				i++;
			}
			Assert.assertTrue("Invalid headers: " + found, found == 3);
			connector.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception during test c: " + e);
		}

		// Post Test
		// Same as "Basic post" test below, but uses read() instead
		// of read(buf, offset, length) to read the results
		try {
			String toWrite = "abcdef";
			connector.open(postTestUrl);
			OutputStream out = connector.getOutputStream();
			out.write(toWrite.getBytes("ISO8859_1"));
			out.close();
			is = connector.getInputStream();
			bout.reset();
			do {
				c = is.read();
				if (c != -1) {
                    bout.write(c);
                }
			} while (c != -1);
			is.close();
			connector.close();
			String result = new String(bout.toByteArray(), "ISO8859_1");
			Assert.assertTrue("Error sending data 1: " + result, toWrite
					.equals(result));
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception during test d: " + e);
		}

		// Post Test chunked
		try {
			String toWrite = "zyxwvuts";
			connector.open(postTestUrl);
			connector.setRequestProperty("Transfer-encoding", "chunked");
			OutputStream out = connector.getOutputStream();
			out.write(toWrite.getBytes("ISO8859_1"));
			out.close();
			is = connector.getInputStream();
			bout.reset();
			do {
				c = is.read();
				if (c != -1) {
                    bout.write(c);
                }
			} while (c != -1);
			is.close();
			connector.close();
			String result = new String(bout.toByteArray(), "ISO8859_1");
            Assert.assertEquals(toWrite, result);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception during test e: " + e);
		}

		OutputStream os = null;

		byte[] data = new byte[1024];
		int len = 0;

		// Basic post
		try {
			String message = "test";
			connector.open(postTestUrl);
			os = connector.getOutputStream();
			os.write(message.getBytes("ISO8859_1"));
			os.close();
			is = connector.getInputStream();
			len = 0;
			do {
				int r = is.read(data, len, data.length - len);
				if (r == -1) {
                    break;
                }
				len += r;
			} while (true);
			is.close();
			connector.close();
			String result = new String(data, 0, len, "ISO8859_1");
			Assert.assertTrue("Basic port error: " + result, message
					.equals(result));
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Exception during basic post test: " + e);
		}

		String chunkChar = connector.isChunkedOnFlush() ? "C" : "";

		// Flushing with post
		try {
			String message1 = "test2", message2 = "test3";
			connector.open(postTestUrl);
			os = connector.getOutputStream();
			os.write(message1.getBytes("ISO8859_1"));
			os.flush();
			os.write(message2.getBytes("ISO8859_1"));
			os.close();
			is = connector.getInputStream();
			len = 0;
			do {
				int r = is.read(data, len, data.length - len);
				if (r == -1) {
                    break;
                }
				len += r;
			} while (true);
			is.close();
			connector.close();
			String result = new String(data, 0, len, "ISO8859_1");
			Assert.assertTrue("Flushing with post: " + result, (chunkChar
					+ message1 + chunkChar + message2).equals(result));
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Exception during flushing post test: " + e);
		}

		// Flushing with post and setting content-length
		try {
			String message1 = "test4", message2 = "test5";
			connector.open(postTestUrl);
			connector.setRequestProperty("Content-Length", "10");
			os = connector.getOutputStream();
			os.write(message1.getBytes("ISO8859_1"));
			os.flush();
			os.write(message2.getBytes("ISO8859_1"));
			os.close();
			is = connector.getInputStream();
			len = 0;
			do {
				int r = is.read(data, len, data.length - len);
				if (r == -1) {
                    break;
                }
				len += r;
			} while (true);
			is.close();
			connector.close();
			String result = new String(data, 0, len, "ISO8859_1");
			Assert.assertTrue("Flushing with post and setting content-length: "
					+ result, (chunkChar + message1 + chunkChar + message2)
					.equals(result));
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Exception during flushing with content-length post test: "
							+ e);
		}

		// Flushing followed immediately by a close()
		try {
			String message = "test6";
			connector.open(postTestUrl);
			os = connector.getOutputStream();
			os.write(message.getBytes("ISO8859_1"));
			os.flush();
			os.close();
			is = connector.getInputStream();
			len = 0;
			do {
				int r = is.read(data, len, data.length - len);
				if (r == -1) {
                    break;
                }
				len += r;
			} while (true);
			is.close();
			connector.close();
			String result = new String(data, 0, len, "ISO8859_1");
			Assert.assertTrue("Flushing followed immediately by a close(): "
					+ result, (chunkChar + message).equals(result));
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Exception during flush followed by close post test: "
					+ e);
		}

		// Redirection Tests
		final int[] testCodes = { Support_HttpServer.MULT_CHOICE,
				Support_HttpServer.MOVED_PERM, Support_HttpServer.FOUND,
				Support_HttpServer.SEE_OTHER, Support_HttpServer.NOT_MODIFIED,
				Support_HttpServer.UNUSED, Support_HttpServer.TEMP_REDIRECT, };

		final int[] results = { 'A', 'A', 'A', 'A', 'P', 'P', 'P' };
		// see Support_HTTPServer for the source of this data

		final String fullLocalLocation = contentTestUrl;
		final String partLocalLocation = Support_HttpServer.CONTENTTEST;

		for (int i = 0; i < testCodes.length; i++) {

			// test each of the redirection response codes
			try {
				// append the response code for the server to return
				// and the location to redirect to
				connector.open(redirectTestUrl + "/" + testCodes[i] + "-"
						+ fullLocalLocation);
				is = connector.getInputStream();
				connector.close();

				c = is.read();
                
				if (testCodes[i] == Support_HttpServer.NOT_MODIFIED) {
					// accept either the message-body or nothing, since the spec
					// says there MUST NOT be a message body on 304 responses.
					// But Java returns the message-body
					if (!(c == results[i] || c == -1)) {
						Assert.fail("Incorrect data returned on test of HTTP response "
										+ testCodes[i]);
					}
				} else if (c != results[i]) {
					Assert.fail("Incorrect data returned on test of HTTP response "
									+ testCodes[i]);
				}
				while (c > 0) {
                    c = is.read();
                }
				c = is.read();
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail("Error during redirection test " + i + ": " + e);
			}
		}

		// Test redirecting to a location on a different port
		Class<?> serversocketclass = serversocket.getClass();
		try {
			Support_ServerSocket serversocket2 = (Support_ServerSocket) serversocketclass
					.newInstance();

			Support_HttpServer server2 = new Support_HttpServer(serversocket2,
					test);
			int newport = Support_PortManager.getNextPort();
			server2.startServer(newport);
			server2.setPortRedirectTestEnable(true);

			// Test if redirection to a different port works
			final String otherPortLocation = "http://localhost:" + newport
					+ Support_HttpServer.PORTREDIRTEST;

			try {
				// append the response code for the server to return
				// and the location to redirect to

				connector.open(redirectTestUrl + "/"
						+ Support_HttpServer.MOVED_PERM + "-"
						+ otherPortLocation);
				is = connector.getInputStream();
				connector.close();

				c = is.read();
				Assert.assertEquals("Incorrect data returned on redirection to a different port.",
								'A', c);
				while (c > 0) {
                    c = is.read();
                }
				c = is.read();
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail("Exception during test f: " + e);
			}
			server2.stopServer();
		} catch (IllegalAccessException e) {
			Assert.fail("Exception during redirection to a different port:" + e);
		} catch (InstantiationException e) {
			Assert.fail("Exception during redirection to a different port:" + e);
		}

		// test redirecting to a relative URL on the same host
		try {
			// append the response code for the server to return
			connector.open(redirectTestUrl + "/"
					+ Support_HttpServer.MOVED_PERM + "-" + partLocalLocation);
			is = connector.getInputStream();
			connector.close();

			c = is.read();
			Assert.assertEquals("Incorrect data returned on redirect to relative URI.",
					'A', c);
			while (c > 0) {
                c = is.read();
            }
			c = is.read();
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Exception during redirection test to a relative URL: " + e);
		}
		server.stopServer();
	}

}
