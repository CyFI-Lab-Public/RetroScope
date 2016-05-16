/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.nfc.handover;

import android.bluetooth.BluetoothDevice;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class PendingHandoverTransfer implements Parcelable {
    public int id;
    public boolean incoming;
    public BluetoothDevice remoteDevice;
    public boolean remoteActivating;
    public Uri[] uris;

    PendingHandoverTransfer(int id, boolean incoming, BluetoothDevice remoteDevice,
            boolean remoteActivating, Uri[] uris) {
        this.id = id;
        this.incoming = incoming;
        this.remoteDevice = remoteDevice;
        this.remoteActivating = remoteActivating;
        this.uris = uris;
    }

    public static final Parcelable.Creator<PendingHandoverTransfer> CREATOR
            = new Parcelable.Creator<PendingHandoverTransfer>() {
        public PendingHandoverTransfer createFromParcel(Parcel in) {
            int id = in.readInt();
            boolean incoming = (in.readInt() == 1) ? true : false;
            BluetoothDevice remoteDevice = in.readParcelable(getClass().getClassLoader());
            boolean remoteActivating = (in.readInt() == 1) ? true : false;
            int numUris = in.readInt();
            Uri[] uris = null;
            if (numUris > 0) {
                uris = new Uri[numUris];
                in.readTypedArray(uris, Uri.CREATOR);
            }
            return new PendingHandoverTransfer(id, incoming, remoteDevice,
                    remoteActivating, uris);
        }

        @Override
        public PendingHandoverTransfer[] newArray(int size) {
            return new PendingHandoverTransfer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(incoming ? 1 : 0);
        dest.writeParcelable(remoteDevice, 0);
        dest.writeInt(remoteActivating ? 1 : 0);
        dest.writeInt(uris != null ? uris.length : 0);
        if (uris != null && uris.length > 0) {
            dest.writeTypedArray(uris, 0);
        }
    }
}
