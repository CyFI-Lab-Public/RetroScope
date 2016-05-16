package com.xtremelabs.robolectric.shadows;

import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

@Implements(android.os.Process.class)
public class ShadowProcess {

    private static Integer pid;

    @Implementation
    public static final int myPid() {
        if (pid != null) {
           return pid;
        }
        return 0;
    }

    public static void setPid(int pid) {
        ShadowProcess.pid = pid;
    }

    public static void reset() {
        ShadowProcess.pid = null;
    }
}
