/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.security.cts;

import android.os.Binder;
import android.os.IBinder;

import junit.framework.TestCase;

import java.lang.Class;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class NetdTest extends TestCase {

    /**
     * Detect if netd has unsanitized system call in Throttle API.
     * Based on jsharkey PoC in b/5758556
     *    serv.setInterfaceThrottle("foo; reboot; echo ", -1, -1);
     */
    public void testThrottleSanitization() {
        try {

            /* get hidden classes */
            Class mSMClass = Class.forName("android.os.ServiceManager");
            Class mINMSClass = Class.forName("android.os.INetworkManagementService");
            Class<?>[] mDeclaredClasses = mINMSClass.getDeclaredClasses();
            if (!mDeclaredClasses[0].getName().equals("android.os.INetworkManagementService$Stub")) {

                /* INetworkManagementService fundamentally changed from original vuln */
                return;
            }

            /* get methods */
            Method mSMMethod = mSMClass.getDeclaredMethod("getService", String.class);
            Method mStubMethod = mDeclaredClasses[0].getDeclaredMethod("asInterface", IBinder.class);
            Method mINMSMethod = mINMSClass.getDeclaredMethod("setInterfaceThrottle", String.class, int.class, int.class);

            /* invoke methods */
            IBinder iB = (IBinder) mSMMethod.invoke(null, "network_management");
            Object INMSObj = mStubMethod.invoke(null, iB);
            if (INMSObj == null) {

                /* Unable to vulnerable service */
                return;
            }
            mINMSMethod.invoke(mINMSClass.cast(INMSObj), "foo;reboot;", -1, -1);
        } catch (IllegalAccessException e) {

            /* Java language access prevents exploitation. */
            return;
        } catch (InvocationTargetException e) {

            /* Underlying method has been changed. */
            return;
        } catch (ClassNotFoundException e) {

            /* not vulnerable if hidden API no longer available */
            return;
        } catch (NoSuchMethodException e) {

            /* not vulnerable if hidden API no longer available */
            return;
        } catch (SecurityException e) {

            /* Security manager blocked operation. */
            return;
        }

        /* should not reach here if vulnerable */
    }
}
