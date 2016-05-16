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

public class Support_ProcessReadWriteTest {

	public static void main(String[] args) {
		try {
			FileInputStream input = new FileInputStream(FileDescriptor.in);
			FileOutputStream output = new FileOutputStream(FileDescriptor.out);

			// read just three lines since EOF isn't working properly. It would
			// be better to read to the end and echo it all
			for (int i = 0; i < 3; i++) {
				int c = input.read();
				while (c != '\n') {
					output.write(c);
					c = input.read();
				}
				output.write(c);
			}
			input.close();
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
