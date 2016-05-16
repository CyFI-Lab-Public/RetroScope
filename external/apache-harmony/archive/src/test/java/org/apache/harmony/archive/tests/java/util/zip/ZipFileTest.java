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
package org.apache.harmony.archive.tests.java.util.zip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import tests.support.Support_PlatformFile;
import tests.support.resource.Support_Resources;

public class ZipFileTest extends junit.framework.TestCase {

    public byte[] getAllBytesFromStream(InputStream is) throws IOException {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        byte[] buf = new byte[512];
        int iRead;
        int off;
        while (is.available() > 0) {
            iRead = is.read(buf, 0, buf.length);
            if (iRead > 0) bs.write(buf, 0, iRead);
        }
        return bs.toByteArray();
    }

	// the file hyts_zipFile.zip in setup must be included as a resource
	private String tempFileName;

	private ZipFile zfile;

	/**
     * @throws IOException 
	 * @tests java.util.zip.ZipFile#ZipFile(java.io.File)
	 */
	public void test_ConstructorLjava_io_File() throws IOException {
		// Test for method java.util.zip.ZipFile(java.io.File)
        zfile.close(); // about to reopen the same temp file
        File file = new File(tempFileName);
        ZipFile zip = new ZipFile(file);
        assertTrue("Zip should exist", file.exists());
        zip.close();        
	}

	/**
	 * @tests java.util.zip.ZipFile#ZipFile(java.io.File, int)
	 */
	public void test_ConstructorLjava_io_FileI() throws IOException {
                zfile.close(); // about to reopen the same temp file
                File file = new File(tempFileName);
                ZipFile zip = new ZipFile(file, ZipFile.OPEN_DELETE
                                          | ZipFile.OPEN_READ);
                zip.close();
                assertTrue("Zip should not exist", !file.exists());

                file = new File(tempFileName);
                try{
                    zip = new ZipFile(file, 3);  //passing unkown argument
                    fail("Should throw IllegalArgumentException");
                }catch(IllegalArgumentException iae ){
                    //expected
                }
	}

	/**
	 * @tests java.util.zip.ZipFile#ZipFile(java.lang.String)
	 */
	public void test_ConstructorLjava_lang_String() {
		// Test for method java.util.zip.ZipFile(java.lang.String)
		/*
		 * try { zfile = new java.util.zip.ZipFile(zipName); zfile.close(); }
		 * catch (java.io.IOException e) {fail( "Failed to construct
		 * ZipFile" );}
		 */
	}

	protected ZipEntry test_finalize1(ZipFile zip) {
		return zip.getEntry("File1.txt");
	}

	protected ZipFile test_finalize2(File file) throws IOException {
                return new ZipFile(file);
	}

	/**
	 * @tests java.util.zip.ZipFile#finalize()
	 */
	public void test_finalize() throws IOException {
            InputStream in = Support_Resources.getStream("hyts_ZipFile.zip");
            File file = Support_Resources.createTempFile(".jar");
            OutputStream out = new FileOutputStream(file);
            int result;
            byte[] buf = new byte[4096];
            while ((result = in.read(buf)) != -1) {
                out.write(buf, 0, result);
            }
            in.close();
            out.close();
            /*
             * ZipFile zip = new ZipFile(file); ZipEntry entry1 =
             * zip.getEntry("File1.txt"); assertNotNull("Did not find entry",
             * entry1); entry1 = null; zip = null;
             */

            assertNotNull("Did not find entry",
                          test_finalize1(test_finalize2(file)));
            System.gc();
            System.gc();
            System.runFinalization();
            file.delete();
            assertTrue("Zip should not exist", !file.exists());
	}

	/**
	 * @tests java.util.zip.ZipFile#close()
	 */
	public void test_close() {
		// Test for method void java.util.zip.ZipFile.close()
		try {
			zfile.close();
			zfile.getInputStream(zfile.getEntry("ztest/file1.txt"));
		} catch (Exception e) {
			return;
		}
		fail("Close test failed");
	}

	/**
	 * @tests java.util.zip.ZipFile#entries()
	 */
	public void test_entries() throws Exception {
		// Test for method java.util.Enumeration java.util.zip.ZipFile.entries()
		Enumeration<? extends ZipEntry> enumer = zfile.entries();
		int c = 0;
		while (enumer.hasMoreElements()) {
			++c;
			enumer.nextElement();
		}
		assertEquals("Incorrect number of entries returned: " + c, 6, c);

		Enumeration<? extends ZipEntry> enumeration = zfile.entries();
		zfile.close();
		zfile = null;
		boolean pass = false;
		try {
			enumeration.hasMoreElements();
		} catch (IllegalStateException e) {
			pass = true;
		}
		assertTrue("did not detect closed jar file", pass);
	}

	/**
	 * @tests java.util.zip.ZipFile#getEntry(java.lang.String)
	 */
	public void test_getEntryLjava_lang_String() throws IOException {
		// Test for method java.util.zip.ZipEntry
		// java.util.zip.ZipFile.getEntry(java.lang.String)
		java.util.zip.ZipEntry zentry = zfile.getEntry("File1.txt");
		assertNotNull("Could not obtain ZipEntry", zentry);

		zentry = zfile.getEntry("testdir1/File1.txt");
		assertNotNull("Could not obtain ZipEntry: testdir1/File1.txt",
				zentry);
                int r;
                InputStream in;
                zentry = zfile.getEntry("testdir1/");
                assertNotNull("Could not obtain ZipEntry: testdir1/", zentry);
                in = zfile.getInputStream(zentry);
                assertNotNull("testdir1/ should not have null input stream",
                              in);
                r = in.read();
                in.close();
                assertEquals("testdir1/ should not contain data", -1, r);
                
                zentry = zfile.getEntry("testdir1");
                assertNotNull("Could not obtain ZipEntry: testdir1", zentry);
                in = zfile.getInputStream(zentry);
                assertNotNull("testdir1 should not have null input stream", in);
                r = in.read();
                in.close();
                assertEquals("testdir1 should not contain data", -1, r);

                zentry = zfile.getEntry("testdir1/testdir1");
                assertNotNull("Could not obtain ZipEntry: testdir1/testdir1",
                              zentry);
                in = zfile.getInputStream(zentry);
                byte[] buf = new byte[256];
                r = in.read(buf);
                in.close();
                assertEquals("incorrect contents",
                             "This is also text", new String(buf, 0, r, "UTF-8"));
	}

    public void test_getEntryLjava_lang_String_Ex() throws IOException {
        java.util.zip.ZipEntry zentry = zfile.getEntry("File1.txt");
        assertNotNull("Could not obtain ZipEntry", zentry);

        zfile.close();
        try {
            zfile.getEntry("File2.txt");
            fail("IllegalStateException expected");
        } catch (IllegalStateException ee) {
            // expected
        }
    }

	/**
	 * @tests java.util.zip.ZipFile#getInputStream(java.util.zip.ZipEntry)
	 */
	public void test_getInputStreamLjava_util_zip_ZipEntry() {
		// Test for method java.io.InputStream
		// java.util.zip.ZipFile.getInputStream(java.util.zip.ZipEntry)
		java.io.InputStream is = null;
		try {
			java.util.zip.ZipEntry zentry = zfile.getEntry("File1.txt");
			is = zfile.getInputStream(zentry);
			byte[] rbuf = new byte[1000];
			int r;
			is.read(rbuf, 0, r = (int) zentry.getSize());
			assertEquals("getInputStream read incorrect data", "This is text", new String(rbuf,
					0, r, "UTF-8"));
		} catch (java.io.IOException e) {
			fail("IOException during getInputStream");
		} finally {
			try {
				is.close();
			} catch (java.io.IOException e) {
				fail("Failed to close input stream");
			}
		}
	}

	/**
	 * @tests java.util.zip.ZipFile#getName()
	 */
	public void test_getName() {
		// Test for method java.lang.String java.util.zip.ZipFile.getName()
		assertEquals("Returned incorrect name: " + zfile.getName(), tempFileName,
		        zfile.getName());
	}
    
    /**
     * @tests java.util.zip.ZipFile#size()
     */
    public void test_size() {
        assertEquals(6, zfile.size());
    }
    
    /**
     * @tests java.io.InputStream#reset()
     */
    public void test_reset() throws IOException {
        // read an uncompressed entry
        ZipEntry zentry = zfile.getEntry("File1.txt");
        InputStream is = zfile.getInputStream(zentry);
        byte[] rbuf1 = new byte[6];
        byte[] rbuf2 = new byte[6];
        int r1, r2;
        r1 = is.read(rbuf1);
        assertEquals(rbuf1.length, r1);
        r2 = is.read(rbuf2);
        assertEquals(rbuf2.length, r2);

        try {
            is.reset();
            fail();
        } catch (IOException expected) {
        }
        is.close();

        // read a compressed entry
        byte[] rbuf3 = new byte[4185];
        ZipEntry zentry2 = zfile.getEntry("File3.txt");
        is = zfile.getInputStream(zentry2);
        r1 = is.read(rbuf3);
        assertEquals(4183, r1);
        try {
            is.reset();
            fail();
        } catch (IOException expected) {
        }
        is.close();

        is = zfile.getInputStream(zentry2);
        r1 = is.read(rbuf3, 0, 3000);
        assertEquals(3000, r1);
        try {
            is.reset();
            fail();
        } catch (IOException expected) {
        }
        is.close();
    }
    
    /**
     * @tests java.io.InputStream#reset()
     */
    public void test_reset_subtest0() throws IOException {
        // read an uncompressed entry
        ZipEntry zentry = zfile.getEntry("File1.txt");
        InputStream is = zfile.getInputStream(zentry);
        byte[] rbuf1 = new byte[12];
        byte[] rbuf2 = new byte[12];
        int r = is.read(rbuf1, 0, 4);
        assertEquals(4, r);
        is.mark(0);
        r = is.read(rbuf1);
        assertEquals(8, r);
        assertEquals(-1, is.read());

        try {
            is.reset();
            fail();
        } catch (IOException expected) {
        }

        is.close();

        // read a compressed entry
        byte[] rbuf3 = new byte[4185];
        ZipEntry zentry2 = zfile.getEntry("File3.txt");
        is = zfile.getInputStream(zentry2);
        r = is.read(rbuf3, 0, 3000);
        assertEquals(3000, r);
        is.mark(0);
        r = is.read(rbuf3);
        assertEquals(1183, r);
        assertEquals(-1, is.read());

        try {
            is.reset();
            fail();
        } catch (IOException expected) {
        }

        is.close();
    }

	/**
	 * Sets up the fixture, for example, open a network connection. This method
	 * is called before a test is executed.
	 */
	@Override
    protected void setUp() {
		try {
			// Create a local copy of the file since some tests want to alter
			// information.
			tempFileName = System.getProperty("user.dir");
			String separator = System.getProperty("file.separator");
			if (tempFileName.charAt(tempFileName.length() - 1) == separator
					.charAt(0)) {
                tempFileName = Support_PlatformFile.getNewPlatformFile(
						tempFileName, "gabba.zip");
            } else {
                tempFileName = Support_PlatformFile.getNewPlatformFile(
						tempFileName + separator, "gabba.zip");
            }

			File f = new File(tempFileName);
			f.delete();
			InputStream is = Support_Resources.getStream("hyts_ZipFile.zip");
			FileOutputStream fos = new FileOutputStream(f);
            byte[] rbuf = getAllBytesFromStream(is);
			fos.write(rbuf, 0, rbuf.length);
			is.close();
			fos.close();
			zfile = new ZipFile(f);
		} catch (Exception e) {
			System.out.println("Exception during ZipFile setup:");
			e.printStackTrace();
		}
	}

	/**
	 * Tears down the fixture, for example, close a network connection. This
	 * method is called after a test is executed.
	 */
	@Override
    protected void tearDown() {
		try {
			if (zfile != null) {
                // Note zfile is a user-defined zip file used by other tests and
				// should not be deleted
				zfile.close();
                tempFileName = System.getProperty("user.dir");
                String separator = System.getProperty("file.separator");
                if (tempFileName.charAt(tempFileName.length() - 1) == separator
                        .charAt(0)) {
                    tempFileName = Support_PlatformFile.getNewPlatformFile(
                            tempFileName, "gabba.zip");
                } else {
                    tempFileName = Support_PlatformFile.getNewPlatformFile(
                            tempFileName + separator, "gabba.zip");
                }

                File f = new File(tempFileName);
                f.delete();
            }
		} catch (Exception e) {
		}
	}

}
