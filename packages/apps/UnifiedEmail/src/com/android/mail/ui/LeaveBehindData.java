/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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
package com.android.mail.ui;

import android.os.Parcel;
import android.os.Parcelable;

import com.android.mail.providers.Conversation;

public class LeaveBehindData implements Parcelable {
    final Conversation data;
    final ToastBarOperation op;
    final int height;

    public LeaveBehindData(Conversation conv, ToastBarOperation undoOp, int height) {
        data = conv;
        op = undoOp;
        this.height = height;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel arg, int flags) {
        arg.writeParcelable(data, 0);
        arg.writeParcelable(op, 0);
        arg.writeInt(height);
    }

    private LeaveBehindData(Parcel arg, ClassLoader loader) {
        data = arg.readParcelable(loader);
        op = arg.readParcelable(loader);
        height = arg.readInt();
    }

    public static final ClassLoaderCreator<LeaveBehindData> CREATOR =
            new ClassLoaderCreator<LeaveBehindData>() {

        @Override
        public LeaveBehindData createFromParcel(Parcel source) {
            return new LeaveBehindData(source, null);
        }

        @Override
        public LeaveBehindData createFromParcel(Parcel source, ClassLoader loader) {
            return new LeaveBehindData(source, loader);
        }

        @Override
        public LeaveBehindData[] newArray(int size) {
            return new LeaveBehindData[size];
        }

    };
}