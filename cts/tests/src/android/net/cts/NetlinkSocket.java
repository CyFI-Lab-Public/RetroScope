/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.net.cts;

import java.io.FileDescriptor;
import java.io.IOException;

public class NetlinkSocket {

    static {
        System.loadLibrary("cts_jni");
    }

    private static native void create_native(FileDescriptor fd);
    private static native int sendmsg(FileDescriptor fd, int pid, byte[] bytes);

    private FileDescriptor fd = new FileDescriptor();

    /** no public constructors */
    private NetlinkSocket() { }

    public static NetlinkSocket create() {
        NetlinkSocket retval = new NetlinkSocket();
        create_native(retval.fd);
        return retval;
    }

    public boolean valid() {
        return fd.valid();
    }

    public int sendmsg(int pid, byte[] bytes) throws IOException {
        int retval = sendmsg(fd, pid, bytes);
        if (retval == -1) {
            throw new IOException("Unable to send message to PID=" + pid);
        }
        return retval;
    }
}
