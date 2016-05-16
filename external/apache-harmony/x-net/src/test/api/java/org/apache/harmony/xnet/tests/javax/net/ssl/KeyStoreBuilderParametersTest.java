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

package org.apache.harmony.xnet.tests.javax.net.ssl;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStore.Builder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.KeyStoreBuilderParameters;

import junit.framework.TestCase;

/**
 * Tests for <code>KeyStoreBuilderParameters</code> class constructors and
 * methods.
 */
public class KeyStoreBuilderParametersTest extends TestCase {
    
    class EmptyBuilder extends KeyStore.Builder {
        @Override
        public KeyStore getKeyStore() throws KeyStoreException {
            return null;
        }

        @Override
        public KeyStore.ProtectionParameter getProtectionParameter(String alias)
                throws KeyStoreException {
            return null;
        }
    }

    /*
     * Class under test for void KeyStoreBuilderParameters(KeyStore.Builder)
     */
    public final void testKeyStoreBuilderParametersBuilder() {
        try {
            new KeyStoreBuilderParameters((KeyStore.Builder) null);
        } catch (NullPointerException e) {
            // javadoc says this should throw NPE, but it doesn't
            fail("no NPE expected");
        }
    }

    /*
     * Class under test for void KeyStoreBuilderParameters(List)
     */
    public final void testKeyStoreBuilderParametersList() {
        try {
            new KeyStoreBuilderParameters((List<?>) null);
            fail("expected a NPE");
        } catch (NullPointerException e) {
        }

        try {
            new KeyStoreBuilderParameters(new ArrayList<Builder>());
            fail("expected a IAE");
        } catch (IllegalArgumentException e) {
        }

    }

    @SuppressWarnings("unchecked")
    public final void testGetParameters() {
        List<Builder> ksbuilders;
        KeyStore.Builder builder = new EmptyBuilder();
        List<Object> result;
        KeyStoreBuilderParameters param = new KeyStoreBuilderParameters(builder);
        result = param.getParameters();
        try {
            result.add(new EmptyBuilder());
            fail("The list is modifiable");
        } catch (UnsupportedOperationException e) {
        }
        assertEquals("incorrect size", 1, result.size());
        assertTrue("incorrect list", result.contains(builder));
        
        ksbuilders = new ArrayList<Builder>();
        ksbuilders.add(builder);
        ksbuilders.add(new EmptyBuilder());  
        param = new KeyStoreBuilderParameters(ksbuilders);
        result = param.getParameters();
        try {
            result.add(new Object());
            fail("The list is modifiable");
        } catch (UnsupportedOperationException e) {
        }
        assertEquals("incorrect size", 2, result.size());
        assertTrue("incorrect list", result.containsAll(ksbuilders));
    }
}
