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

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Support_AvailTest {

	public static void main(String[] args) {
		// This test is for:

		// This program accepts from stdin a
		// string of the form '<int1> <int2> <data>'
		// where data is some bytes, <int1> is the length of the whole string
		// and <int2> is the length in bytes of <data> (all bytes until the end
		// of
		// the input)

		// If the string is formatted correctly, and the available method works
		// the string "true" will be sent to stdout, otherwise "false"
		String output = "true";
		try {
			FileInputStream myin = new FileInputStream(FileDescriptor.in);
			StringBuffer input = new StringBuffer("");

			try {
				Thread.sleep(500);
			} catch (Exception sleepException) {
			}

			int real = myin.available();
			int expected;
			int c = 0;
			while (true) {
				c = myin.read();
				if (c == ' ' || c == -1) {
                    break;
                }
				input.append((char) c);
			}
			expected = Integer.parseInt(input.toString());
			// Verify correct value at start of read
			if (real != expected) {
                output = "Failed avail test1 - " + real + "!=" + expected;
            }

			c = 0;
			input = new StringBuffer("");
			while (true) {
				c = myin.read();
				if (c == ' ' || c == -1) {
                    break;
                }
				input.append((char) c);
			}
			expected = Integer.parseInt(input.toString());
			real = myin.available();
			// Verify value at middle of reading
			// This test doesn't work on Windows, at present 
			// if(real != expected) output = "Failed avail test2 - " + real +
			// "!=" + expected;

			// Verify value at end of reading
			// loop to EOF, then check if available = 0
			// replace this:
			for (int i = 0; i < 5; i++) {
                myin.read();
			// with:
			// while(myin.read() != -1);
            }

			// The current for loop reads exactly to the end
			// of the data, but is dependent on knowing the length of the data
			// sent to it, which isn't nice

			expected = 0;
			real = myin.available();
			if (real != 0) {
                output = "Failed avail test3 - " + real + "!=" + expected;
            }

		} catch (IOException e) {
			output = "IOException during available() testing";
		}

		try {
			FileOutputStream myout = new FileOutputStream(FileDescriptor.out);
			myout.write(output.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
