/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.rpc_performance;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Debug;
import android.os.IBinder;
import android.util.Log;
import android.net.LocalServerSocket;
import android.net.LocalSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MiscService extends Service {

    public static final String SOCKET_NAME = "play-misc-service";
    private static final String TAG = "MiscService";

    private LocalServerSocket mServerSocket = null;

    @Override public void onCreate() {
        Log.v(TAG, "onCreate");
        try {
            mServerSocket = new LocalServerSocket(SOCKET_NAME);
        } catch (IOException e) {
            Log.v(TAG, "in onCreate, making server socket: " + e);
            return;
        }

        Thread t = new Thread() {
                @Override public void run() {
                    LocalSocket socket = null;
                    while (true) {
                        try {
                            Log.v(TAG, "Waiting for connection...");
                            socket = mServerSocket.accept();
                            Log.v(TAG, "Got socket: " + socket);
                            if (socket != null) {
                                startEchoThread(socket);
                            } else {
                                return;  // socket shutdown?
                            }
                        } catch (IOException e) {
                            Log.v(TAG, "in accept: " + e);
                        }
                    }
                }
            };
        t.start();
    }

    private void startEchoThread(final LocalSocket socket) {
        Thread t = new Thread() {
                @Override public void run() {
                    try {
                        InputStream is = socket.getInputStream();
                        OutputStream os = socket.getOutputStream();
                        while (true) {
                            int byteRead = is.read();
                            if (byteRead < 0) {
                                return;  // EOF.
                            }
                            os.write(byteRead);
                        }
                    } catch (IOException e) {
                        Log.v(TAG, "in echo thread loop: " + e);
                    }
                }
            };
        t.start();
    }

    @Override public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        return mBinder;
    }

    private final IService.Stub mBinder = new IService.Stub() {
        public String pingString(String v) {
            return v;
        }
        public void pingVoid() {
        }
        public void startTracing(String name) {
            Debug.startMethodTracing(name);
        }
        public void stopTracing() {
            Debug.stopMethodTracing();
        }
    };
}
