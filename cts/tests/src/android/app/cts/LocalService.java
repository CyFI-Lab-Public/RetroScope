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

package android.app.cts;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

public class LocalService extends Service {
    public static final String SERVICE_LOCAL =
            "android.app.cts.activity.SERVICE_LOCAL";
    public static final String SERVICE_LOCAL_GRANTED =
            "android.app.cts.activity.SERVICE_LOCAL_GRANTED";
    public static final String SERVICE_LOCAL_DENIED =
            "android.app.cts.activity.SERVICE_LOCAL_DENIED";

    public static final String REPORT_OBJ_NAME = "report";

    public static final int STARTED_CODE = 1;
    public static final int DESTROYED_CODE = 2;
    public static final int SET_REPORTER_CODE = 3;
    public static final int UNBIND_CODE = 4;
    public static final int REBIND_CODE = 5;

    private IBinder mReportObject;
    private int mStartCount = 1;

    private final IBinder mBinder = new Binder() {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply,
                int flags) throws RemoteException {
            if (code == SET_REPORTER_CODE) {
                data.enforceInterface(SERVICE_LOCAL);
                mReportObject = data.readStrongBinder();
                return true;
            } else {
                return super.onTransact(code, data, reply, flags);
            }
        }
    };


    public LocalService() {
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (intent.getExtras() != null) {
            IBinderParcelable parcelable
                    = (IBinderParcelable) intent.getExtras().getParcelable(REPORT_OBJ_NAME);
            mReportObject = parcelable.binder;
            if (mReportObject != null) {
                bindAction(STARTED_CODE);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mReportObject != null) {
            bindAction(DESTROYED_CODE);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mReportObject != null) {
            bindAction(UNBIND_CODE);
        }
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        if (mReportObject != null) {
            bindAction(REBIND_CODE);
        }
    }

    private void bindAction(final int bindCode) {
        try {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken(SERVICE_LOCAL);
            if (bindCode == STARTED_CODE) {
                data.writeInt(mStartCount);
                mStartCount++;
            }
            mReportObject.transact(
                    bindCode, data, null, 0);
            data.recycle();
        } catch (RemoteException e) {
            // fail
        }
    }
}
