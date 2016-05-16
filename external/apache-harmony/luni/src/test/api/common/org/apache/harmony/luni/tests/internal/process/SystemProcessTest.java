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

package org.apache.harmony.luni.tests.internal.process;

import java.io.OutputStream;

import junit.framework.TestCase;
import tests.support.Support_Exec;

public class SystemProcessTest extends TestCase {

    public void test_interrupt() throws Exception {
        Object[] execArgs = null;
        Process process = null;
        try {
            Thread.currentThread().interrupt();
            execArgs = Support_Exec.execJava2(
                    new String[] { "tests.support.Support_AvailTest" }, null,
                    true);
            process = (Process) execArgs[0];
            OutputStream os = process.getOutputStream();
            os.write("10 5 abcde".getBytes());
            os.close();
            process.waitFor();
            fail("Should throw InterruptedException");
        } catch (InterruptedException e) {
            // Expected
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignored
        }
        process.waitFor();
        Support_Exec.checkStderr(execArgs);
        process.destroy();
    }
}
