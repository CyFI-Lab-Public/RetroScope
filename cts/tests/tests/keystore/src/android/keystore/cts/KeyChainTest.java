/*
 * Copyright 2013 The Android Open Source Project
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

package android.keystore.cts;

import android.security.KeyChain;
import junit.framework.TestCase;

public class KeyChainTest extends TestCase {
    public void testIsKeyAlgorithmSupported_RequiredAlgorithmsSupported() throws Exception {
        assertTrue("DSA must be supported", KeyChain.isKeyAlgorithmSupported("DSA"));
        assertTrue("EC must be supported", KeyChain.isKeyAlgorithmSupported("EC"));
        assertTrue("RSA must be supported", KeyChain.isKeyAlgorithmSupported("RSA"));
    }

    /**
     * Tests whether the required algorithms are backed by a Keymaster HAL that
     * binds the key material to the specific device it was created or imported
     * to. For more information on the Keymaster HAL, look at the header file at
     * hardware/libhardware/include/hardware/keymaster.h and the associated
     * tests in hardware/libhardware/tests/keymaster/
     */
    public void testIsBoundKeyAlgorithm_RequiredAlgorithmsSupported() throws Exception {
        assertTrue("RSA must be hardware-backed by a hardware-specific Keymaster HAL",
                KeyChain.isBoundKeyAlgorithm("RSA"));

        // These are not required, but must not throw an exception
        KeyChain.isBoundKeyAlgorithm("DSA");
        KeyChain.isBoundKeyAlgorithm("EC");
    }
}
