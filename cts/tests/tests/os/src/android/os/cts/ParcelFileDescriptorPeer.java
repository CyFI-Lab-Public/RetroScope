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

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.ParcelFileDescriptor.OnCloseListener;
import android.os.ParcelFileDescriptor.FileDescriptorDetachedException;
import android.os.RemoteException;
import android.os.SystemClock;

import com.google.common.util.concurrent.AbstractFuture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Sits around in a remote process doing whatever the CTS test says.
 */
public class ParcelFileDescriptorPeer extends IParcelFileDescriptorPeer.Stub {
    private IParcelFileDescriptorPeer mPeer;

    private ParcelFileDescriptor mLocal;
    private ParcelFileDescriptor mRemote;

    private FutureCloseListener mListener;

    @Override
    public void setPeer(IParcelFileDescriptorPeer peer) throws RemoteException {
        mPeer = peer;
    }

    @Override
    public void setupReadPipe() throws RemoteException {
        try {
            ParcelFileDescriptor[] pfds = ParcelFileDescriptor.createReliablePipe();
            mLocal = pfds[0];
            mRemote = pfds[1];
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setupWritePipe() throws RemoteException {
        try {
            ParcelFileDescriptor[] pfds = ParcelFileDescriptor.createReliablePipe();
            mLocal = pfds[1];
            mRemote = pfds[0];
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setupSocket() throws RemoteException {
        try {
            ParcelFileDescriptor[] pfds = ParcelFileDescriptor.createReliableSocketPair();
            mLocal = pfds[0];
            mRemote = pfds[1];
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setupFile() throws RemoteException {
        final Handler handler = new Handler(Looper.getMainLooper());
        mListener = new FutureCloseListener();
        try {
            mLocal = null;
            mRemote = ParcelFileDescriptor.open(File.createTempFile("pfd", "tmp"),
                    ParcelFileDescriptor.MODE_READ_WRITE, handler, mListener);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ParcelFileDescriptor get() throws RemoteException {
        return mRemote;
    }

    @Override
    public void set(ParcelFileDescriptor pfd) throws RemoteException {
        mLocal = pfd;
    }

    @Override
    public void doGet() throws RemoteException {
        mLocal = mPeer.get();
    }

    @Override
    public void doSet() throws RemoteException {
        mPeer.set(mRemote);
    }

    @Override
    public int read() throws RemoteException {
        try {
            return new FileInputStream(mLocal.getFileDescriptor()).read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(int oneByte) throws RemoteException {
        try {
            new FileOutputStream(mLocal.getFileDescriptor()).write(oneByte);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws RemoteException {
        try {
            mLocal.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void closeWithError(String msg) throws RemoteException {
        try {
            mLocal.closeWithError(msg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void detachFd() throws RemoteException {
        mLocal.detachFd();
    }

    @Override
    public void leak() throws RemoteException {
        mLocal = null;

        // Try really hard to finalize
        for (int i = 0; i < 5; i++) {
            System.gc();
            System.runFinalization();
            SystemClock.sleep(100);
        }
    }

    @Override
    public void crash() throws RemoteException {
        Process.killProcess(Process.myPid());
        System.exit(42);
    }

    @Override
    public String checkError() throws RemoteException {
        try {
            mLocal.checkError();
            return null;
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    @Override
    public String checkListener() throws RemoteException {
        try {
            return String.valueOf(mListener.get());
        } catch (InterruptedException e1) {
            return null;
        } catch (ExecutionException e1) {
            return null;
        }
    }

    public static class FutureCloseListener extends AbstractFuture<IOException>
            implements OnCloseListener {
        @Override
        public void onClose(IOException e) {
            if (e instanceof FileDescriptorDetachedException) {
                set(new IOException("DETACHED"));
            } else {
                set(e);
            }
        }

        @Override
        public IOException get() throws InterruptedException, ExecutionException {
            try {
                return get(5, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Red extends Service {
        @Override
        public IBinder onBind(Intent intent) {
            return new ParcelFileDescriptorPeer();
        }
    }

    public static class Blue extends Service {
        @Override
        public IBinder onBind(Intent intent) {
            return new ParcelFileDescriptorPeer();
        }
    }
}
