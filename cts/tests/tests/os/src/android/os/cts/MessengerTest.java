/*
 * Copyright (C) 2008 The Android Open Source Project
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


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;
import android.test.AndroidTestCase;

import java.io.FileDescriptor;

public class MessengerTest extends AndroidTestCase {

    private Messenger mMessenger;
    private Message mMessage;
    private boolean mResult;
    private Messenger mServiceMessenger;
    private static final int MSG_ARG1 = 100;
    private static final int MSG_ARG2 = 1000;
    private static final int WHAT = 2008;
    private Handler mHandler = new Handler() {
        @Override
        public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
            mResult = true;
            mMessage = msg;
            return super.sendMessageAtTime(msg, uptimeMillis);
        }
    };

    private final IBinder mIBinder = new IBinder() {

        public String getInterfaceDescriptor() throws RemoteException {
            return null;
        }

        public boolean isBinderAlive() {
            return false;
        }

        public void linkToDeath(DeathRecipient recipient, int flags) throws RemoteException {
        }

        public boolean pingBinder() {
            return false;
        }

        public IInterface queryLocalInterface(String descriptor) {
            return null;
        }

        public boolean transact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            return false;
        }

        public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
            return false;
        }

        public void dump(FileDescriptor fd, String[] args) throws RemoteException {
        }

        public void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {
        }

    };

    // Create another messenger to send msg.
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (MessengerTest.this) {
                mServiceMessenger = new Messenger(service);
                MessengerTest.this.notifyAll();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            mServiceMessenger = null;
        }
    };

    @Override
    protected void setUp() throws Exception {

        super.setUp();
        mMessenger = new Messenger(mHandler);
        getContext().bindService(new Intent(mContext, MessengerService.class), mConnection,
                Context.BIND_AUTO_CREATE);
        synchronized (this) {
            while (mServiceMessenger == null) {
                wait();
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        getContext().unbindService(mConnection);
    }

    public void testConstructorAndEquals() {
        Messenger messenger = new Messenger(mHandler);
        Messenger objMessenger = new Messenger(mHandler);
        assertTrue(messenger.equals(objMessenger));
        messenger = new Messenger(mIBinder);
        assertFalse(messenger.equals(objMessenger));
    }

    public void testSend() throws RemoteException, InterruptedException {
        // messenger used by its own thread.
        Message message = Message.obtain(mHandler, WHAT, MSG_ARG1, MSG_ARG2);
        mMessenger.send(message);
        assertTrue(mResult);
        assertNotNull(mMessage);
        assertEquals(mMessage.what, message.what);
        assertEquals(mMessage.arg1, message.arg1);
        assertEquals(mMessage.arg2, message.arg2);

        // Used in other process. If the sent msg does not equal to expected, it will throw failure
        // and the test will fail
        (new MessengerTestHelper()).doTest(1000, 50);
    }

    public void testHashCode() {
        assertEquals(mMessenger.getBinder().hashCode(), mMessenger.hashCode());
    }

    public void testGetBinder() {
        Messenger messenger = new Messenger(mIBinder);
        assertSame(mIBinder, messenger.getBinder());
        assertNotNull(mMessenger.getBinder());
    }

    public void testWriteToParcel() {
        Parcel parcel = Parcel.obtain();
        mMessenger.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Messenger messenger = Messenger.CREATOR.createFromParcel(parcel);
        assertTrue(messenger.equals(mMessenger));
        parcel.recycle();
    }

    public void testDescribeContents() {
        assertEquals(0, mMessenger.describeContents());
    }

    public void testWriteMessengerOrNullToParcel() {
        Parcel parcelWithMessenger = Parcel.obtain();
        Messenger.writeMessengerOrNullToParcel(mMessenger, parcelWithMessenger);
        parcelWithMessenger.setDataPosition(0);
        Messenger messenger = Messenger.readMessengerOrNullFromParcel(parcelWithMessenger);
        assertNotNull(messenger);
        assertTrue(messenger.equals(mMessenger));
        parcelWithMessenger.recycle();

        Parcel parcelWithNull = Parcel.obtain();
        Messenger.writeMessengerOrNullToParcel(null, parcelWithNull);
        parcelWithNull.setDataPosition(0);
        messenger = Messenger.readMessengerOrNullFromParcel(parcelWithNull);
        assertNull(messenger);
        parcelWithNull.recycle();
    }

    /**
     * This helper class is used for test of MessengerTest. Mainly on control of the message looper.
     */
    private class MessengerTestHelper {
        private boolean mDone = false;
        private boolean mSuccess = false;
        private RuntimeException mFailure = null;
        private Looper mLooper;

        private Handler mTestHandler;
        private Messenger mTestMessenger;

        public void init() {
            synchronized (MessengerTest.this) {
                mTestHandler = new Handler() {
                    public void handleMessage(Message msg) {
                        MessengerTestHelper.this.handleMessage(msg);
                    }
                };
                mTestMessenger = new Messenger(mTestHandler);
                try {
                    MessengerTestHelper.this.executeTest();
                } catch (RemoteException e) {
                    fail(e.getMessage());
                }
            }
        }

        public MessengerTestHelper() {
        }

        public void executeTest() throws RemoteException {
            Message msg = Message.obtain();
            msg.arg1 = MSG_ARG1;
            msg.arg2 = MSG_ARG2;
            msg.replyTo = mTestMessenger;
            // Use another messenger to send msg.
            mServiceMessenger.send(msg);
        }

        /**
         * This method is used to check if the message sent by another messenger is correctly
         * handled by this thread. If not equals to expected, there will be a failure thrown.
         */
        public void handleMessage(Message msg) {
            if (msg.arg1 != MSG_ARG1) {
                failure(new RuntimeException("Message.arg1 is not " + MSG_ARG1 + ", it's "
                        + msg.arg1));
                return;
            }
            if (msg.arg2 != MSG_ARG2) {
                failure(new RuntimeException("Message.arg2 is not " + MSG_ARG2 + ", it's "
                        + msg.arg2));
                return;
            }
            if (!mTestMessenger.equals(msg.replyTo)) {
                failure(new RuntimeException("Message.replyTo is not me, it's " + msg.replyTo));
                return;
            }
            success();
        }

        public void doTest(long timeout, long interval) throws InterruptedException {
            (new LooperThread()).start();

            synchronized (this) {
                long now = System.currentTimeMillis();
                long endTime = now + timeout;
                // wait and frequently check if mDone is set.
                while (!mDone && now < endTime) {
                    wait(interval);
                    now = System.currentTimeMillis();
                }
            }

            mLooper.quit();

            if (!mDone) {
                throw new RuntimeException("test timed out");
            }
            if (!mSuccess) {
                throw mFailure;
            }
        }

        public Looper getLooper() {
            return mLooper;
        }

        public void success() {
            synchronized (this) {
                mSuccess = true;
                quit();
            }
        }

        public void failure(RuntimeException failure) {
            synchronized (this) {
                mSuccess = false;
                mFailure = failure;
                quit();
            }
        }

        class LooperThread extends HandlerThread {

            public LooperThread() {
                super("MessengerLooperThread");
            }

            public void onLooperPrepared() {
                init();
                mLooper = getLooper();
            }

            @Override
            public void run() {
                super.run();
                synchronized (MessengerTestHelper.this) {
                    mDone = true;
                    if (!mSuccess && mFailure == null) {
                        mFailure = new RuntimeException("no failure exception set");
                    }
                    MessengerTestHelper.this.notifyAll();
                }
            }
        }

        private void quit() {
            synchronized (this) {
                mDone = true;
                notifyAll();
            }
        }
    }
}
