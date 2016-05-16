/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

public class IBinderParcelable implements Parcelable {
    public IBinder binder;

    public IBinderParcelable(IBinder source) {
        binder = source;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongBinder(binder);
    }

    public static final Parcelable.Creator<IBinderParcelable>
        CREATOR = new Parcelable.Creator<IBinderParcelable>() {

        public IBinderParcelable createFromParcel(Parcel source) {
            return new IBinderParcelable(source);
        }

        public IBinderParcelable[] newArray(int size) {
            return new IBinderParcelable[size];
        }
    };

    private IBinderParcelable(Parcel source) {
        binder = source.readStrongBinder();
    }
}
