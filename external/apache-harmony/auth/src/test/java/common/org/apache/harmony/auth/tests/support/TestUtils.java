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

package org.apache.harmony.auth.tests.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Security;
import java.util.Properties;

/**
 * Test utility class
 */
public class TestUtils {

    /**
     * No need to instantiate
     */
    private TestUtils() {
    }

    /**
     * Prints byte array <code>data</code> as hex to the
     * <code>System.out</code> in the customizable form.
     *
     * @param perLine how many numbers put on single line
     * @param prefix custom output number prefix
     * @param delimiter custom output number delimiter
     * @param data data to be printed
     */
    public static void printAsHex(int perLine,
                                  String prefix,
                                  String delimiter,
                                  byte[] data) {
        for (int i=0; i<data.length; i++) {
            String tail = Integer.toHexString(0x000000ff & data[i]);
            if (tail.length() == 1) {
                tail = "0" + tail; 
            }
            System.out.print(prefix + "0x" + tail + delimiter);

            if (((i+1)%perLine) == 0) {
                System.out.println("");
            }
        }
        System.out.println("");
    }
    
    /**
     * Sets system property
     *
     * @param key - the name of the system property.
     * @param value - the value to be set
     */
    public static void setSystemProperty(String key, String value) {
        Properties properties = System.getProperties();
        if (value == null) {
            properties.remove(key);
        } else {
            properties.setProperty(key, value);
        }
        System.setProperties(properties);
    }
    
    /**
     * Creates security properties file.
     * 
     * The method puts to the file all initial security providers and adds
     * passed custom properties.
     * 
     * @param props -
     *            security properties to be added to properties file
     * @return path to created file.
     * @throws IOException
     *             if failed to create the file
     */
    public static String createJavaPropertiesFile(Properties props)
            throws IOException {

        File f = File.createTempFile("java", "security");
        f.deleteOnExit();

        FileOutputStream out = new FileOutputStream(f);

        Properties propsToFlush = new Properties();
        
        int i = 1;
        String provider = "security.provider.1";
        while (Security.getProperty(provider) != null) {
            propsToFlush.setProperty(provider, Security.getProperty(provider));
            provider = "security.provider." + i++;
        }

        props.store(out, null);
        propsToFlush.store(out, null);

        out.close();

        return f.getAbsolutePath();
    }
}
