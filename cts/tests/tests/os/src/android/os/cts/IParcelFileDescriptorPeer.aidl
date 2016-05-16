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

package android.os.cts;

import android.os.ParcelFileDescriptor;

interface IParcelFileDescriptorPeer {

    void setPeer(in IParcelFileDescriptorPeer peer);

    /* Setup internal local and remote FDs */
    void setupReadPipe();
    void setupWritePipe();
    void setupSocket();
    void setupFile();

    ParcelFileDescriptor get();
    void set(in ParcelFileDescriptor pfd);

    /* Ask this peer to get their remote FD from another */
    void doGet();
    /* Ask this peer to set their remote FD to another */
    void doSet();

    int read();
    void write(int oneByte);

    void close();
    void closeWithError(String msg);
    void detachFd();
    void leak();
    void crash();

    String checkError();
    String checkListener();

}
