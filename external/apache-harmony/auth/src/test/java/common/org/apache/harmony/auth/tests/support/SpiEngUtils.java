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
* @author Vera Y. Petrashkova
*/

package org.apache.harmony.auth.tests.support;

import java.io.File;
import java.security.Security;
import java.security.Provider;
import java.util.StringTokenizer;

/**
 * Additional class for verification spi-engine classes
 * 
 */

public class SpiEngUtils {

    public static final String[] invalidValues = {
            "",
            "BadAlgorithm",
            "Long message Long message Long message Long message Long message Long message Long message Long message Long message Long message Long message Long message Long message" };

    /**
     * Verification: is algorithm supported or not
     * 
     * @param algorithm
     * @param service
     * @return
     */
    public static Provider isSupport(String algorithm, String service) {
        try {
            Provider[] provs = Security.getProviders(service.concat(".")
                    .concat(algorithm));
            if (provs == null) {
                return null;
            }
            return (provs.length == 0 ? null : provs[0]);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getFileName(String dir, String name) {
        String res = dir;
        if (res.charAt(res.length() - 1) == '/') {
            res = res.substring(0, res.length() - 1);
        }
        char[] mm = { File.separatorChar };
        String sp = String.copyValueOf(mm);
        StringTokenizer st = new StringTokenizer(name, "/");
        while (st.hasMoreElements()) {
            res = res.concat(sp).concat((String) st.nextElement());
        }
        return res;
    }

    public class MyProvider extends Provider {
        private static final long serialVersionUID = 1L;

        public MyProvider(String name, String info, String key, String clName) {
            super(name, 1.0, info);
            put(key, clName);
        }

    }

}