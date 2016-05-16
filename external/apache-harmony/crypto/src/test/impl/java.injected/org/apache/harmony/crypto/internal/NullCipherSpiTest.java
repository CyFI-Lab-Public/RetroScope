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

/**
* @author Boris V. Kuznetsov
*/

package org.apache.harmony.crypto.internal;

import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.crypto.ShortBufferException;

import org.apache.harmony.crypto.internal.NullCipherSpi;
import junit.framework.TestCase;

/**
 *
 * Tests for NullCipher implementation
 */
public class NullCipherSpiTest extends TestCase {

	public void testEngineGetBlockSize() {
		NullCipherSpi spi = new NullCipherSpi();
        assertEquals("incorrect block size", 1, spi.engineGetBlockSize());
	}

	public void testEngineGetOutputSize() {
		NullCipherSpi spi = new NullCipherSpi();
        assertEquals("incorrect output size", 100, spi.engineGetOutputSize(100));
	}

	public void testEngineGetIV() {
		NullCipherSpi spi = new NullCipherSpi();
        assertTrue("Incorrect IV", Arrays.equals(spi.engineGetIV() , new byte[8]));
	}

	/*
	 * Class under test for byte[] engineUpdate(byte[], int, int)
	 */
	public void testEngineUpdatebyteArrayintint() {
		NullCipherSpi spi = new NullCipherSpi();
		byte[] b = {1,2,3,4,5,6,7,8,9};
		byte[] b1 =  spi.engineUpdate(b, 3, 4);
		for (int i = 0; i < 4; i++) {
            assertEquals("incorrect update result", b[3+i], b1[i]);
		}
	}

	/*
	 * Class under test for int engineUpdate(byte[], int, int, byte[], int)
	 */
	public void testEngineUpdatebyteArrayintintbyteArrayint() throws Exception {
		NullCipherSpi spi = new NullCipherSpi();
		byte[] b = {1,2,3,4,5,6,7,8,9};
		byte[] b1 =  new byte[10];
		assertEquals("incorrect update result", 4, spi.engineUpdate(b, 3, 4, b1, 5));
		for (int i = 0; i < 4; i++) {
            assertEquals("incorrect update result", b[3+i], b1[5+i]);
		}	
	}

	/*
	 * Class under test for byte[] engineDoFinal(byte[], int, int)
	 */
	public void testEngineDoFinalbyteArrayintint() throws Exception {
		NullCipherSpi spi = new NullCipherSpi();
		byte[] b = {1,2,3,4,5,6,7,8,9};
		byte[] b1 = null; 
		b1 = spi.engineDoFinal(b, 3, 4);
		for (int i = 0; i < 4; i++) {
            assertEquals("incorrect doFinal result", b[3+i], b1[i]);
		}
	}

	/*
	 * Class under test for int engineDoFinal(byte[], int, int, byte[], int)
	 */
	public void testEngineDoFinalbyteArrayintintbyteArrayint() throws Exception {
		NullCipherSpi spi = new NullCipherSpi();
		byte[] b = {1,2,3,4,5,6,7,8,9};
		byte[] b1 =  new byte[10];
        assertEquals("incorrect doFinal result", 4, spi.engineDoFinal(b, 3, 4, b1, 5));
		for (int i = 0; i < 4; i++) {
            assertEquals("incorrect doFinal result", b[3+i], b1[5+i]);
		}
		
	}

	/*
	 * Class under test for int engineUpdate(ByteBuffer, ByteBuffer)
	 */
	public void testEngineUpdateByteBufferByteBuffer() throws Exception {
		NullCipherSpi spi = new NullCipherSpi();
		byte[] b = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

		ByteBuffer inbuf = ByteBuffer.wrap(b,0,b.length);
		ByteBuffer outbuf = ByteBuffer.allocate(6);
		
		try {
			spi.engineUpdate(null, outbuf);
			fail("No expected NullPointerException");
		} catch (NullPointerException e) {	
		}
		
		try {
			spi.engineUpdate(inbuf, null);
			fail("No expected NullPointerException");
		} catch (NullPointerException e) {	
		}
		
		inbuf.get();
		inbuf.get();
		inbuf.get();
		inbuf.get();
		int result = spi.engineUpdate(inbuf, outbuf);
        assertEquals("incorrect result", b.length - 4, result);
		for (int i = 0; i < result; i++) {
            assertEquals("incorrect outbuf", i + 4, outbuf.get(i));
		}
		
		inbuf = ByteBuffer.wrap(b,0,b.length);
		outbuf = ByteBuffer.allocate(5);
		inbuf.get();
		inbuf.get();
		inbuf.get();
		inbuf.get();
		try {
			spi.engineUpdate(inbuf, outbuf);
			fail("No expected ShortBufferException");
		} catch (ShortBufferException e) {
		} 
	}

	/*
	 * Class under test for int engineDoFinal(ByteBuffer, ByteBuffer)
	 */
	public void testEngineDoFinalByteBufferByteBuffer() throws Exception {
		NullCipherSpi spi = new NullCipherSpi();
		byte[] b = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

		ByteBuffer inbuf = ByteBuffer.wrap(b,0,b.length);
		ByteBuffer outbuf = ByteBuffer.allocate(6);
		
		try {
			spi.engineDoFinal(null, outbuf);
			fail("No expected NullPointerException");
		} catch (NullPointerException e) {			
		}
		
		try {
			spi.engineDoFinal(inbuf, null);
			fail("No expected NullPointerException");
		} catch (NullPointerException e) {			
		}
		
		inbuf.get();
		inbuf.get();
		inbuf.get();
		inbuf.get();
		int result = spi.engineDoFinal(inbuf, outbuf);
        assertEquals("incorrect result", b.length - 4, result);
        for (int i = 0; i < result; i++) {
            assertEquals("incorrect outbuf", i + 4, outbuf.get(i));
        }
		
		inbuf = ByteBuffer.wrap(b,0,b.length);
		outbuf = ByteBuffer.allocate(5);
		inbuf.get();
		inbuf.get();
		inbuf.get();
		inbuf.get();
		try {
			spi.engineDoFinal(inbuf, outbuf);
			fail("No expected ShortBufferException");
		} catch (ShortBufferException e) {
		}
	}

	/*
	 * Class under test for byte[] engineWrap(Key)
	 */
	public void testEngineWrapKey() throws Exception {
		NullCipherSpi spi = new NullCipherSpi();
		try {
			spi.engineWrap(null);
			fail("No expected UnsupportedOperationException");
		} catch (UnsupportedOperationException e) {
		}	
    }

	/*
	 * Class under test for Key engineUnwrap(byte[], String, int)
	 */
	public void testEngineUnwrapbyteArrayStringint() throws Exception {
		NullCipherSpi spi = new NullCipherSpi();
		try {
			spi.engineUnwrap(new byte[3], "", 10);
			fail("No expected UnsupportedOperationException");
		} catch (UnsupportedOperationException e) {
		} 
	}

	/*
	 * Class under test for int engineGetKeySize(Key)
	 */
	public void testEngineGetKeySize() throws Exception {
		NullCipherSpi spi = new NullCipherSpi();
		try {
			spi.engineGetKeySize(null);
			fail("No expected UnsupportedOperationException");
		} catch (UnsupportedOperationException e) {
		} 
	}

}
