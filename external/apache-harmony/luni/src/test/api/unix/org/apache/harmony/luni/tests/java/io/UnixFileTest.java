/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.luni.tests.java.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.TestCase;

/**
 * Please note that this case can only be passed on Linux due to some file
 * system dependent reason.
 * 
 */
public class UnixFileTest extends TestCase {
	private boolean root = false;
	
	private File testFile;

	private File testDir;
	
	private static final int TOTAL_SPACE_NUM = 0;

	private static final int FREE_SPACE_NUM = 1;

	private static final int USABLE_SPACE_NUM = 2;

	private static class ConsoleResulter extends Thread {
		Process proc;

		InputStream is;

		String resStr;

		ConsoleResulter(Process p, InputStream in) {
			proc = p;
			is = in;
		}

		@Override
		public void run() {
			StringBuffer result = new StringBuffer();
			synchronized (result) {
				try {
					BufferedReader br = new BufferedReader(
							new InputStreamReader(is));
					String line;
					while ((line = br.readLine()) != null) {
						result.append(line);
					}
					if (result.length() != 0) {
						resStr = result.toString();
					}

					br.close();
				} catch (IOException ioe) {
					result = null;
				}
				synchronized (proc) {
					proc.notifyAll();
				}
			}
		}
	}

	private static long getLinuxSpace(int index, File file) throws Exception {
		long[] result = new long[3];
		String par = file.getAbsolutePath();
		String osName = System.getProperty("os.name");
		// in case the test case will run under other OS.
		if (osName.toLowerCase().indexOf("linux") != -1) {
			String[] cmd = new String[2];
			cmd[0] = "df";
			cmd[1] = par; // get the total space of file
			Runtime rt = Runtime.getRuntime();

			Process proc = rt.exec(cmd);
			// get output from the command
			ConsoleResulter outputResult = new ConsoleResulter(proc, proc
					.getInputStream());

			synchronized (proc) {
				outputResult.start();
				proc.wait();
			}
			// If there is no error, obtain the result
			if (outputResult.resStr != null) {
				// exit the subprocess safely
				proc.waitFor();

				// filter unnecessary information
				String[] txtResult = outputResult.resStr
						.split("\\D|\\p{javaLowerCase}|\\p{javaUpperCase}");
				for (int i = 0, j = 0; i < txtResult.length; i++) {
					if (txtResult[i].length() > 3) {
						result[j++] = Long.parseLong(txtResult[i]) * 1024L;
					}
				}
			}
		}

		// calculate free spaces according to df command
		result[1] = result[0] - result[1];
		return result[index];
	}
	
	/**
	 * @tests java.io.File#canExecute()
	 * 
	 * @since 1.6
	 */
	public void test_canExecute() {		
		assertFalse(testFile.canExecute());
		assertTrue(testFile.setExecutable(true, false));
		assertTrue(testFile.canExecute());
		assertTrue(testFile.setExecutable(true, true));
		assertTrue(testFile.canExecute());

		assertTrue(testFile.setExecutable(false, false));
		assertFalse(testFile.canExecute());
		assertTrue(testFile.setExecutable(false, true));
		assertFalse(testFile.canExecute());

		assertTrue(testFile.setExecutable(true, false));
		assertTrue(testFile.canExecute());

		// tests directory
		assertTrue(testDir.canExecute());
		assertTrue(testDir.setExecutable(false, true));
		if (root) {
			assertTrue(testDir.canExecute());
		} else {
			assertFalse(testDir.canExecute());			
		}		
		assertTrue(testDir.setExecutable(true, false));
		assertTrue(testDir.canExecute());
	}
	
	/**
	 * @tests java.io.File#getFreeSpace()
	 * 
	 * @since 1.6
	 */
	public void test_getFreeSpace() throws Exception {
		long fileSpace = getLinuxSpace(FREE_SPACE_NUM, testFile);
		long dirSpace = getLinuxSpace(FREE_SPACE_NUM, testDir);
		// in case we cannot fetch the value from command line
		if (fileSpace > 0) {
			assertEquals(fileSpace, testFile.getFreeSpace());			
		}
		
		if (dirSpace > 0) {
			assertEquals(dirSpace, testDir.getFreeSpace());		
		}		
	}

	/**
	 * @tests java.io.File#getTotalSpace()
	 * 
	 * @since 1.6
	 */
	public void test_getTotalSpace() throws Exception {
		long fileSpace = getLinuxSpace(TOTAL_SPACE_NUM, testFile);
		long dirSpace = getLinuxSpace(TOTAL_SPACE_NUM, testDir);
		if (fileSpace > 0) {
			assertEquals(fileSpace, testFile.getTotalSpace());
		}
		if (dirSpace > 0) {
			assertEquals(dirSpace, testDir.getTotalSpace());			
		}
	}

	/**
	 * @tests java.io.File#getUsableSpace()
	 * 
	 * @since 1.6
	 */
	public void test_getUsableSpace() throws Exception {
		long fileSpace = getLinuxSpace(USABLE_SPACE_NUM, testFile);
		long dirSpace = getLinuxSpace(USABLE_SPACE_NUM, testDir);
		if (fileSpace > 0) {
			assertEquals(fileSpace, testFile.getUsableSpace());
		}
		if (dirSpace > 0) {
			assertEquals(dirSpace, testDir.getUsableSpace());			
		}
	}
	
	/**
	 * @tests java.io.File#setExecutable(boolean, boolean)
	 * 
	 * @since 1.6
	 */
	public void test_setExecutableZZ() {		
		// setExecutable(true, true/false)
		assertFalse(testFile.canExecute());
		assertTrue(testFile.setExecutable(true, false));
		assertTrue(testFile.canExecute());
		assertTrue(testFile.setExecutable(true, true));
		assertTrue(testFile.canExecute());

		// setExecutable(false, true/false)
		assertTrue(testFile.setExecutable(false, true));
		if (root) {
			assertTrue(testFile.canExecute());			
		} else {
			assertFalse(testFile.canExecute());			
		}		
		assertTrue(testFile.setExecutable(false, false));
		assertFalse(testFile.canExecute());

		// tests directory
		assertTrue(testDir.canExecute());
		assertTrue(testDir.setExecutable(false, true));
		if (root) {
			assertTrue(testDir.canExecute());
		} else {
			assertFalse(testDir.canExecute());
		}
		assertTrue(testDir.setExecutable(false, false));
		if (root) {
			assertTrue(testDir.canExecute());
		} else {
			assertFalse(testDir.canExecute());
		}

		assertTrue(testDir.setExecutable(true, true));
		assertTrue(testDir.canExecute());
		assertTrue(testDir.setExecutable(true, false));
		assertTrue(testDir.canExecute());
	}

	/**
	 * @tests java.io.File#setExecutable(boolean)
	 * 
	 * @since 1.6
	 */
	public void test_setExecutableZ() {
		// So far this method only deals with the situation that the user is the
		// owner of the file
		assertTrue(testFile.setExecutable(true));
		assertTrue(testFile.canExecute());
		assertTrue(testFile.setExecutable(false));
		assertFalse(testFile.canExecute());
		assertTrue(testFile.setExecutable(true));

		// tests directory
		assertTrue(testDir.canExecute());
		assertTrue(testDir.setExecutable(false));
		if (root) {
			assertTrue(testDir.canExecute());
		} else {
			assertFalse(testDir.canExecute());			
		}		
		assertTrue(testDir.setExecutable(true));
		assertTrue(testDir.canExecute());
	}
	
	/**
	 * @tests java.io.File#setReadable(boolean, boolean)
	 * 
	 * @since 1.6
	 */
	public void test_setReadableZZ() throws Exception {
		// setReadable(false, false/true) succeeds on Linux
		// However, canRead() always returns true when the user is 'root'.
		assertTrue(testFile.canRead());
		assertTrue(testFile.setReadable(false, false));
		if (root) {
			assertTrue(testFile.canRead());
		} else {
			assertFalse(testFile.canRead());			
		}
		assertTrue(testFile.setReadable(false, true));
		if (root) {
			assertTrue(testFile.canRead());
		} else {
			assertFalse(testFile.canRead());			
		}

		// tests directory, setReadable(false, true/false)
		assertTrue(testDir.canRead());
		assertTrue(testDir.setReadable(false, true));
		if (root) {
			assertTrue(testDir.canRead());
		} else {
			assertFalse(testDir.canRead());			
		}
		assertTrue(testDir.setReadable(false, false));
		if (root) {
			assertTrue(testDir.canRead());
		} else {
			assertFalse(testDir.canRead());			
		}

		// setReadable(true, false/true) and set them in turn
		assertTrue(testFile.setReadable(true, false));
		assertTrue(testFile.canRead());
		assertTrue(testFile.setReadable(false, true));
		if (root) {
			assertTrue(testFile.canRead());
		} else {
			assertFalse(testFile.canRead());			
		}
		assertTrue(testFile.setReadable(true, true));
		assertTrue(testFile.canRead());
		assertTrue(testFile.setReadable(false, true));
		if (root) {
			assertTrue(testFile.canRead());
		} else {
			assertFalse(testFile.canRead());			
		}

		// tests directory, setReadable(true, true/false)
		assertTrue(testDir.setReadable(true, false));
		assertTrue(testDir.canRead());
		assertTrue(testDir.setReadable(true, true));
		assertTrue(testDir.canRead());		
	}

	/**
	 * @tests java.io.File#setReadable(boolean)
	 * 
	 * @since 1.6
	 */
	public void test_setReadableZ() {
		// So far this method only deals with the situation that the user is the
		// owner of the file. setReadable(false) succeeds on Linux
		// However, canRead() always returns true when the user is 'root'.		
		assertTrue(testFile.canRead());
		assertTrue(testFile.setReadable(false));
		if (root) {
			assertTrue(testFile.canRead());
		} else {
			assertFalse(testFile.canRead());			
		}		
		assertTrue(testFile.setReadable(true));
		assertTrue(testFile.canRead());

		assertTrue(testDir.canRead());	
		assertTrue(testDir.setReadable(false));
		if (root) {
			assertTrue(testDir.canRead());
		} else {
			assertFalse(testDir.canRead());			
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		testFile = File.createTempFile("testfile", null);
		testDir = new File(System.getProperty("java.io.tmpdir") + "/temp");
		if (!testDir.exists()) {
			testDir.mkdir();
		}
		root = System.getProperty("user.name").equals("root");
	}

	@Override
	protected void tearDown() throws Exception {
		testFile.delete();
		testDir.delete();
		testFile = null;
		testDir = null;
		root = false;
		super.tearDown();
	}

    public void test_getCanonicalPath() throws IOException,
                                               InterruptedException {
        File tmpFolder1 = new File("folder1");
        tmpFolder1.mkdirs();
        tmpFolder1.deleteOnExit();

        File tmpFolder2 = new File(tmpFolder1.toString() + "/folder2");
        tmpFolder2.mkdirs();
        tmpFolder2.deleteOnExit();

        File tmpFolder3 = new File(tmpFolder2.toString() + "/folder3");
        tmpFolder3.mkdirs();
        tmpFolder3.deleteOnExit();

        File tmpFolder4 = new File(tmpFolder3.toString() + "/folder4");
        tmpFolder4.mkdirs();
        tmpFolder4.deleteOnExit();

        // make a link to folder1/folder2
        Process ln = Runtime.getRuntime().exec("ln -s folder1/folder2 folder2");
        ln.waitFor();
        File linkFile = new File("folder2");
        linkFile.deleteOnExit();

        File file = new File("folder2");
        assertEquals(tmpFolder2.getCanonicalPath(), file.getCanonicalPath());

        file = new File("folder1/folder2");
        assertEquals(tmpFolder2.getCanonicalPath(), file.getCanonicalPath());

        file = new File("folder2/folder3");
        assertEquals(tmpFolder3.getCanonicalPath(), file.getCanonicalPath());

        file = new File("folder2/folder3/folder4");
        assertEquals(tmpFolder4.getCanonicalPath(), file.getCanonicalPath());

        file = new File("folder1/folder2/folder3");
        assertEquals(tmpFolder3.getCanonicalPath(), file.getCanonicalPath());

        file = new File("folder1/folder2/folder3/folder4");
        assertEquals(tmpFolder4.getCanonicalPath(), file.getCanonicalPath());
    }
}
