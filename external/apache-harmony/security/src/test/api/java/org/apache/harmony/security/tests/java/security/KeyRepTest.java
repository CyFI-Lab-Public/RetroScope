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
* @author Vladimir N. Molotkov
*/

package org.apache.harmony.security.tests.java.security;

import junit.framework.TestCase;

/**
 * 
 * 
 */
public class KeyRepTest extends TestCase {
//
//    private static final Set<String> keyFactoryAlgorithm;
//    static {
//        keyFactoryAlgorithm = Security.getAlgorithms("KeyFactory");
//    }
//
    public final void testKeyRep01() {
//        KeyRep kr = new KeyRep(KeyRep.Type.SECRET, "", "", new byte[] {});
//        kr = new KeyRep(KeyRep.Type.PUBLIC, "", "", new byte[] {});
//        kr = new KeyRep(KeyRep.Type.PRIVATE, "", "", new byte[] {});
    }
//
//    public final void testKeyRep02() {
//        try {
//            KeyRep kr = new KeyRep(null, "", "", new byte[] {});
//            fail("NullPointerException has not been thrown (type)");
//        } catch (NullPointerException ok) {
//            System.out.println(getName() + ": " + ok);
//        }
//        try {
//            KeyRep kr = new KeyRep(KeyRep.Type.SECRET, null, "", new byte[] {});
//            fail("NullPointerException has not been thrown (alg)");
//        } catch (NullPointerException ok) {
//            System.out.println(getName() + ": " + ok);
//        }
//        try {
//            KeyRep kr = new KeyRep(KeyRep.Type.PRIVATE, "", null, new byte[] {});
//            fail("NullPointerException has not been thrown (format)");
//        } catch (NullPointerException ok) {
//            System.out.println(getName() + ": " + ok);
//        }
//        try {
//            KeyRep kr = new KeyRep(KeyRep.Type.PUBLIC, "", "", null);
//            fail("NullPointerException has not been thrown (encoding)");
//        } catch (NullPointerException ok) {
//            System.out.println(getName() + ": " + ok);
//        }
//    }
//
//    public final void testReadResolve01() throws ObjectStreamException {
//        KeyRep kr = new KeyRep(KeyRep.Type.SECRET, "", "", new byte[] {});
//        try {
//            kr.readResolve();
//            fail("NotSerializableException has not been thrown (no format)");
//        } catch (NotSerializableException ok) {
//            System.out.println(getName() + ": " + ok);
//        }
//
//        kr = new KeyRep(KeyRep.Type.SECRET, "", "X.509", new byte[] {});
//        try {
//            kr.readResolve();
//            fail("NotSerializableException has not been thrown (unacceptable format)");
//        } catch (NotSerializableException ok) {
//            System.out.println(getName() + ": " + ok);
//        }
//
//        kr = new KeyRep(KeyRep.Type.SECRET, "", "RAW", new byte[] {});
//        try {
//            kr.readResolve();
//            fail("NotSerializableException has not been thrown (empty key)");
//        } catch (NotSerializableException ok) {
//            System.out.println(getName() + ": " + ok);
//        }
//    }
//
//    public final void testReadResolve02() throws ObjectStreamException {
//        KeyRep kr = new KeyRep(KeyRep.Type.PUBLIC, "", "", new byte[] {});
//        try {
//            kr.readResolve();
//            fail("NotSerializableException has not been thrown (no format)");
//        } catch (NotSerializableException ok) {
//            System.out.println(getName() + ": " + ok);
//        }
//
//        kr = new KeyRep(KeyRep.Type.PUBLIC, "", "RAW", new byte[] {});
//        try {
//            kr.readResolve();
//            fail("NotSerializableException has not been thrown (unacceptable format)");
//        } catch (NotSerializableException ok) {
//            System.out.println(getName() + ": " + ok);
//        }
//
//        kr = new KeyRep(KeyRep.Type.PUBLIC, "bla-bla", "X.509", new byte[] {});
//        try {
//            kr.readResolve();
//            fail("NotSerializableException has not been thrown (unknown KeyFactory algorithm)");
//        } catch (NotSerializableException ok) {
//            System.out.println(getName() + ": " + ok);
//        }
//    }
//
//    public final void testReadResolve03() throws ObjectStreamException {
//        KeyRep kr = new KeyRep(KeyRep.Type.PRIVATE, "", "", new byte[] {});
//        try {
//            kr.readResolve();
//            fail("NotSerializableException has not been thrown (no format)");
//        } catch (NotSerializableException ok) {
//            System.out.println(getName() + ": " + ok);
//        }
//
//        kr = new KeyRep(KeyRep.Type.PRIVATE, "", "RAW", new byte[] {});
//        try {
//            kr.readResolve();
//            fail("NotSerializableException has not been thrown (unacceptable format)");
//        } catch (NotSerializableException ok) {
//            System.out.println(getName() + ": " + ok);
//        }
//
//        kr = new KeyRep(KeyRep.Type.PRIVATE, "bla-bla", "PKCS#8", new byte[] {});
//        try {
//            kr.readResolve();
//            fail("NotSerializableException has not been thrown (unknown KeyFactory algorithm)");
//        } catch (NotSerializableException ok) {
//            System.out.println(getName() + ": " + ok);
//        }
//    }
//
//    public final void testReadResolve04() throws ObjectStreamException {
//        if (keyFactoryAlgorithm.isEmpty()) {
//            System.err.println(getName() + ": skipped - no KeyFactory algorithms available");
//            return;
//        } else {
//            System.out.println(getName() + ": available algorithms - " +
//                    keyFactoryAlgorithm);
//        }
//        for (Iterator<String> i = keyFactoryAlgorithm.iterator(); i.hasNext();) {
//            KeyRep kr = new KeyRep(KeyRep.Type.PUBLIC,
//                    i.next(), "X.509", new byte[] {1,2,3});
//            try {
//                kr.readResolve();
//                fail("NotSerializableException has not been thrown (no format)");
//            } catch (NotSerializableException ok) {
//                System.out.println(getName() + ": " + ok);
//            }
//        }
//    }
//
//    public final void testReadResolve05() throws ObjectStreamException {
//        if (keyFactoryAlgorithm.isEmpty()) {
//            System.err.println(getName() + ": skipped - no KeyFactory algorithms available");
//            return;
//        } else {
//            System.out.println(getName() + ": available algorithms - " +
//                    keyFactoryAlgorithm);
//        }
//        for (Iterator<String> i = keyFactoryAlgorithm.iterator(); i.hasNext();) {
//            KeyRep kr = new KeyRep(KeyRep.Type.PRIVATE,
//                    i.next(), "PKCS#8", new byte[] {1,2,3});
//            try {
//                kr.readResolve();
//                fail("NotSerializableException has not been thrown (no format)");
//            } catch (NotSerializableException ok) {
//                System.out.println(getName() + ": " + ok);
//            }
//        }
//    }
}
