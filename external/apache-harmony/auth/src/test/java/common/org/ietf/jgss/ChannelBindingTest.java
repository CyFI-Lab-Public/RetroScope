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
 * @author Alexander V. Esin
 */
package org.ietf.jgss;

import java.net.InetAddress;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Tests ChannelBinding class
 */
public class ChannelBindingTest extends TestCase {

    public void testGetApplicationData() {
        byte data [] = { 0, 1, 2, 10, 20 };
        ChannelBinding cb = new ChannelBinding(data);
        assertTrue(Arrays.equals( new byte [] { 0, 1, 2, 10, 20 },
                cb.getApplicationData()));
    }
    
    public void testGetApplicationData_0() throws Exception {
        byte data [] = { 0, 10, 20, 40, 50 };
        InetAddress addr1= InetAddress.getByAddress(new byte [] {127, 0, 0, 1});
        InetAddress addr2= InetAddress.getByAddress(new byte [] {127, 0, 0, 2});
        ChannelBinding cb = new ChannelBinding(addr1, addr2, data);
        assertTrue(Arrays.equals( new byte [] { 0, 10, 20, 40, 50 },
                cb.getApplicationData()));
    }
    
    public void testGetApplicationData_1() {
        ChannelBinding cb = new ChannelBinding(null);
        assertNull(cb.getApplicationData());
    }
    
    public void testGetInitiatorAddress() throws Exception {
        InetAddress addr= InetAddress.getByAddress(new byte [] {127, 0, 0, 1});
        ChannelBinding cb = new ChannelBinding(addr, null, null);
        assertEquals(InetAddress.getByAddress(new byte [] {127, 0, 0, 1}),
                cb.getInitiatorAddress());
    }
    
    public void testGetInitiatorAddress_0() throws Exception {
        byte data [] = { 0, 10, 20, 40, 50 };
        InetAddress addr1= InetAddress.getByAddress(new byte [] {127, 0, 0, 1});
        InetAddress addr2= InetAddress.getByAddress(new byte [] {127, 0, 0, 2});
        ChannelBinding cb = new ChannelBinding(addr1, addr2, data);
        assertEquals(InetAddress.getByAddress(new byte [] {127, 0, 0, 1}),
                cb.getInitiatorAddress());
    }
    
    public void testGetInitiatorAddress_1() {
        ChannelBinding cb = new ChannelBinding(null);
        assertNull(cb.getInitiatorAddress());
    }
    
    public void testGetAcceptorAddress() throws Exception {
        InetAddress addr= InetAddress.getByAddress(new byte [] {127, 0, 0, 1});
        ChannelBinding cb = new ChannelBinding(null, addr, null);
        assertEquals(InetAddress.getByAddress(new byte [] {127, 0, 0, 1}),
                cb.getAcceptorAddress());
    }
    public void testGetAcceptorAddress_0() throws Exception {
        byte data [] = { 0, 10, 20, 40, 50 };
        InetAddress addr1= InetAddress.getByAddress(new byte [] {127, 0, 0, 1});
        InetAddress addr2= InetAddress.getByAddress(new byte [] {127, 0, 0, 2});
        ChannelBinding cb = new ChannelBinding(addr1, addr2, data);
        assertEquals(InetAddress.getByAddress(new byte [] {127, 0, 0, 2}),
                cb.getAcceptorAddress());
    }
    public void testGetAcceptorAddress_1() {
        ChannelBinding cb = new ChannelBinding(null);
        assertNull(cb.getAcceptorAddress());
    }
    
    public void testEquals() {
        ChannelBinding cb = new ChannelBinding(null);
        assertTrue((new ChannelBinding(null, null, null)
                .equals(cb)));
    }
    
    public void testEquals_0_0_0() {
        ChannelBinding cb = new ChannelBinding(new byte [] { 0, 10, 20, 40, 50 });
        assertTrue(cb
                .equals(cb));
    }
    
    public void testEquals_0() throws Exception {
        byte data [] = { 0, 10, 20, 40, 50 };
        InetAddress addr1= InetAddress.getByAddress(new byte [] {127, 0, 0, 1});
        InetAddress addr2= InetAddress.getByAddress(new byte [] {127, 0, 0, 2});
        ChannelBinding cb = new ChannelBinding(addr1, addr2, data);
        
        assertTrue((new ChannelBinding(InetAddress.getByAddress(new byte [] {127, 0, 0, 1}), 
                InetAddress.getByAddress(new byte [] {127, 0, 0, 2}), 
                new byte [] { 0, 10, 20, 40, 50 }))
                .equals(cb));
    }
    
    public void testEquals_0_0() throws Exception {
        byte data [] = { 0, 10, 20, 40, 50 };
        InetAddress addr1= InetAddress.getByAddress(new byte [] {127, 0, 0, 1});
        InetAddress addr2= InetAddress.getByAddress(new byte [] {127, 0, 0, 2});
        ChannelBinding cb = new ChannelBinding(addr1, addr2, data);
        
        assertTrue((new ChannelBinding(addr1, 
                addr2, 
                new byte [] { 0, 10, 20, 40, 50 }))
                .equals(cb));
    }
    
    public void testEquals_1() throws Exception {
        byte data [] = { 0, 10, 20, 40, 5 };
        ChannelBinding cb = new ChannelBinding(data);
        assertTrue((new ChannelBinding(new byte [] { 0, 10, 20, 40, 5 }))
                .equals(cb));
    }
    
    public void testEquals_2() throws Exception {
        byte data [] = null;
        InetAddress addr1= InetAddress.getByAddress(new byte [] {127, 0, 0, 1});
        InetAddress addr2= InetAddress.getByAddress(new byte [] {127, 0, 0, 2});
        ChannelBinding cb = new ChannelBinding(addr1, addr2, data);
        
        assertTrue((new ChannelBinding(InetAddress.getByAddress(new byte [] {127, 0, 0, 1}), 
                InetAddress.getByAddress(new byte [] {127, 0, 0, 2}), null))
                .equals(cb));
    }
    
    public void testEquals_2_0() throws Exception {
        byte data [] = null;
        InetAddress addr1= null;
        InetAddress addr2= InetAddress.getByAddress(new byte [] {127, 0, 0, 2});
        ChannelBinding cb = new ChannelBinding(addr1, addr2, data);
        
        assertTrue((new ChannelBinding(null, 
                InetAddress.getByAddress(new byte [] {127, 0, 0, 2}), null))
                .equals(cb));
    }
    
    //Negative
    public void testEquals_Negative() throws Exception {
        byte data [] = { 0, 10, 20};
        ChannelBinding cb = new ChannelBinding(data);
        assertTrue(!(new ChannelBinding(null))
                .equals(cb));
    }
    
    public void testEquals_Negative_0() throws Exception {
        byte data [] = { 0, 10, 20};
        ChannelBinding cb = new ChannelBinding(null);
        assertTrue(!(new ChannelBinding(data))
                .equals(cb));
    }
    
    public void testEquals_Negative_0_0() throws Exception {
        byte data [] = { 0, 10, 20};
        ChannelBinding cb = new ChannelBinding(data);
        assertTrue(!cb
                .equals(new Object()));
    }
    
    public void testEquals_Negative_1() throws Exception {
        byte data [] = { 0, 10, 20};
        ChannelBinding cb = new ChannelBinding(data);
        assertTrue(!(new ChannelBinding(new byte [] { 0, 10, 20, 30, 40 }))
                .equals(cb));
    }
    
    public void testEquals_Negative_2() throws Exception {
        byte data [] = { 0, 10, 20};
        InetAddress addr1= InetAddress.getByAddress(new byte [] {127, 0, 0, 1});
        InetAddress addr2= InetAddress.getByAddress(new byte [] {127, 0, 0, 2});
        ChannelBinding cb = new ChannelBinding(addr1, addr2, data);
        assertTrue(!(new ChannelBinding(new byte [] { 0, 10, 20 }))
                .equals(cb));
    }
    
    public void testEquals_Negative_3() throws Exception {
        byte data [] = { 0, 10, 20, 30};
        InetAddress addr1= InetAddress.getByAddress(new byte [] {127, 0, 0, 1});
        InetAddress addr2= InetAddress.getByAddress(new byte [] {127, 0, 0, 2});
        ChannelBinding cb = new ChannelBinding(addr1, addr2, data);
        assertTrue(!(new ChannelBinding( addr1, addr2, 
                new byte [] { 0, 10, 20 }))
                .equals(cb));
    }
    
    public void testEquals_Negative_3_0() throws Exception {
        byte data [] = { 0, 10, 20, 30};
        InetAddress addr1= InetAddress.getByAddress(new byte [] {127, 0, 0, 1});
        InetAddress addr2= InetAddress.getByAddress(new byte [] {127, 0, 0, 2});
        ChannelBinding cb = new ChannelBinding(addr1, addr2, data);
        assertTrue(!(new ChannelBinding( InetAddress.getByAddress(new byte [] {127, 0, 0, 1}), 
                InetAddress.getByAddress(new byte [] {127, 0, 0, 2}), 
                new byte [] { 0, 10, 20 }))
                .equals(cb));
    }
    
    public void testEquals_Negative_4() throws Exception {
        byte data [] = { 0, 10, 20};
        InetAddress addr1= InetAddress.getByAddress(new byte [] {127, 0, 0, 1});
        InetAddress addr2= InetAddress.getByAddress(new byte [] {127, 0, 0, 2});
        ChannelBinding cb = new ChannelBinding(addr1, addr2, data);
        assertTrue(!(new ChannelBinding( InetAddress.getByAddress(new byte [] {127, 0, 0, 1}), 
                InetAddress.getByAddress(new byte [] {127, 0, 0, 1}), 
                new byte [] { 0, 10, 20 }))
                .equals(cb));
    }
    
    public void testEquals_Negative_5() throws Exception {
        byte data [] = { 0, 10, 20};
        InetAddress addr1= InetAddress.getByAddress(new byte [] {127, 0, 0, 2});
        InetAddress addr2= InetAddress.getByAddress(new byte [] {127, 0, 0, 1});
        ChannelBinding cb = new ChannelBinding(addr1, addr2, data);
        assertTrue(!(new ChannelBinding( InetAddress.getByAddress(new byte [] {127, 0, 0, 1}), 
                InetAddress.getByAddress(new byte [] {127, 0, 0, 1}), 
                new byte [] { 0, 10, 20 }))
                .equals(cb));
    }
    
    public void testEquals_Negative_6() throws Exception {
        byte data [] = { 0, 10, 20};
        InetAddress addr1= InetAddress.getByAddress(new byte [] {127, 0, 0, 2});
        InetAddress addr2= InetAddress.getByAddress(new byte [] {127, 0, 0, 1});
        ChannelBinding cb = new ChannelBinding(addr1, addr2, data);
        assertTrue(!(new ChannelBinding( null, 
                InetAddress.getByAddress(new byte [] {127, 0, 0, 1}), 
                new byte [] { 0, 10, 20 }))
                .equals(cb));
    }
    
    public void testHashCode() throws Exception {
        byte data [] = { 0, 10, 20, 40, 50 };
        InetAddress addr1= InetAddress.getByAddress(new byte [] {127, 0, 0, 1});
        InetAddress addr2= InetAddress.getByAddress(new byte [] {127, 0, 0, 2});
        ChannelBinding cb = new ChannelBinding(addr1, addr2, data);
        assertEquals(addr1.hashCode(), cb.hashCode());
    }
    public void testHashCode_0() throws Exception {
        byte data [] = { 0, 10, 20, 40, 50 };
        InetAddress addr= InetAddress.getByAddress(new byte [] {127, 0, 0, 1});
        ChannelBinding cb = new ChannelBinding(null, addr, data);
        assertEquals(addr.hashCode(), cb.hashCode());
    }
    public void testHashCode_1() throws Exception {
        byte data [] = { 1, 2 };
        ChannelBinding cb = new ChannelBinding(null, null, data);
        assertEquals(33, cb.hashCode());
    }
    
    public void testHashCode_2() throws Exception {
        byte data [] = { 10, 2 };
        ChannelBinding cb = new ChannelBinding(null, null, data);
        assertEquals(312, cb.hashCode());
    }
}
