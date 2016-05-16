/* Licensed to the Apache Software Foundation (ASF) under one or more
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

package org.apache.harmony.prefs.tests.java.util.prefs;

import java.io.FilePermission;
import java.io.IOException;
import java.security.Permission;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import junit.framework.TestCase;

public class FilePreferencesImplTest extends TestCase {

    private String prevFactory;
    private Preferences uroot;
    private Preferences sroot;

    public FilePreferencesImplTest() {
        super();
    }

    @Override
    protected void setUp() throws Exception {
        prevFactory = System.getProperty("java.util.prefs.PreferencesFactory");
        System.setProperty("java.util.prefs.PreferencesFactory", "java.util.prefs.FilePreferencesFactoryImpl");

        uroot = Preferences.userRoot().node("harmony_test");
        sroot = Preferences.systemRoot().node("harmony_test");
    }

    @Override
    protected void tearDown() throws Exception {
        if (prevFactory != null)
            System.setProperty("java.util.prefs.PreferencesFactory", prevFactory);

        uroot.removeNode();
        sroot.removeNode();
        uroot = null;
        sroot = null;
    }

    public void testPutGet() throws IOException, BackingStoreException {
        uroot.put("ukey1", "value1");
        assertEquals("value1", uroot.get("ukey1", null));
        String[] names = uroot.keys();
        assertEquals(1, names.length);

        uroot.put("ukey2", "value3");
        assertEquals("value3", uroot.get("ukey2", null));
        uroot.put("\u4e2d key1", "\u4e2d value1");
        assertEquals("\u4e2d value1", uroot.get("\u4e2d key1", null));
        names = uroot.keys();
        assertEquals(3, names.length);

        uroot.flush();
        uroot.clear();
        names = uroot.keys();
        assertEquals(0, names.length);

        sroot.put("skey1", "value1");
        assertEquals("value1", sroot.get("skey1", null));
        sroot.put("\u4e2d key1", "\u4e2d value1");
        assertEquals("\u4e2d value1", sroot.get("\u4e2d key1", null));
    }

    public void testChildNodes() throws Exception {
        Preferences child1 = uroot.node("child1");
        Preferences child2 = uroot.node("\u4e2d child2");
        Preferences grandchild = child1.node("grand");
        assertNotNull(grandchild);

        String[] childNames = uroot.childrenNames();
        assertEquals(2, childNames.length);

        childNames = child1.childrenNames();
        assertEquals(1, childNames.length);

        childNames = child2.childrenNames();
        assertEquals(0, childNames.length);

        child1.removeNode();
        childNames = uroot.childrenNames();
        assertEquals(1, childNames.length);

        child2.removeNode();
        childNames = uroot.childrenNames();
        assertEquals(0, childNames.length);

        child1 = sroot.node("child1");
        child2 = sroot.node("child2");
        grandchild = child1.node("grand");

        childNames = sroot.childrenNames();

        assertEquals(2, childNames.length);

        childNames = child1.childrenNames();
        assertEquals(1, childNames.length);

        childNames = child2.childrenNames();
        assertEquals(0, childNames.length);

        child1.removeNode();
        assertNotSame(child1, sroot.node("child1"));
        assertSame(sroot.node("child1"), sroot.node("child1"));
        sroot.node("child1").removeNode();
        childNames = sroot.childrenNames();
        assertEquals(1, childNames.length);
        child2.removeNode();
        childNames = sroot.childrenNames();
        assertEquals(0, childNames.length);
    }
}
