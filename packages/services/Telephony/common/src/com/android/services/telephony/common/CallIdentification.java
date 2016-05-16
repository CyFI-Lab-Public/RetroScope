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

package com.android.services.telephony.common;

import android.os.Parcel;
import android.os.Parcelable;

import com.android.internal.telephony.PhoneConstants;
import com.google.common.base.Objects;

/**
 * Class object used across CallHandlerService APIs. Describes a single call and its state.
 */
public final class CallIdentification implements Parcelable {

    // Unique identifier for the call
    private int mCallId;

    // The phone number on the other end of the connection
    private String mNumber = "";

    // Number presentation received from the carrier
    private int mNumberPresentation = Call.PRESENTATION_ALLOWED;

    // Name presentation mode received from the carrier
    private int mCnapNamePresentation = Call.PRESENTATION_ALLOWED;

    // Name associated with the other end of the connection; from the carrier.
    private String mCnapName = "";

    public CallIdentification(int callId) {
        mCallId = callId;
    }

    public CallIdentification(CallIdentification identification) {
        mCallId = identification.mCallId;
        mNumber = identification.mNumber;
        mNumberPresentation = identification.mNumberPresentation;
        mCnapNamePresentation = identification.mCnapNamePresentation;
        mCnapName = identification.mCnapName;
    }

    public int getCallId() {
        return mCallId;
    }

    public String getNumber() {
        return mNumber;
    }

    public void setNumber(String number) {
        mNumber = number;
    }

    public int getNumberPresentation() {
        return mNumberPresentation;
    }

    public void setNumberPresentation(int presentation) {
        mNumberPresentation = presentation;
    }

    public int getCnapNamePresentation() {
        return mCnapNamePresentation;
    }

    public void setCnapNamePresentation(int presentation) {
        mCnapNamePresentation = presentation;
    }

    public String getCnapName() {
        return mCnapName;
    }

    public void setCnapName(String cnapName) {
        mCnapName = cnapName;
    }

    /**
     * Parcelable implementation
     */

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mCallId);
        dest.writeString(mNumber);
        dest.writeInt(mNumberPresentation);
        dest.writeInt(mCnapNamePresentation);
        dest.writeString(mCnapName);
    }

    /**
     * Constructor for Parcelable implementation.
     */
    private CallIdentification(Parcel in) {
        mCallId = in.readInt();
        mNumber = in.readString();
        mNumberPresentation = in.readInt();
        mCnapNamePresentation = in.readInt();
        mCnapName = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Creates Call objects for Parcelable implementation.
     */
    public static final Creator<CallIdentification> CREATOR = new Creator<CallIdentification>() {

        @Override
        public CallIdentification createFromParcel(Parcel in) {
            return new CallIdentification(in);
        }

        @Override
        public CallIdentification[] newArray(int size) {
            return new CallIdentification[size];
        }
    };

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("mCallId", mCallId)
                .add("mNumber", MoreStrings.toSafeString(mNumber))
                .add("mNumberPresentation", mNumberPresentation)
                .add("mCnapName", MoreStrings.toSafeString(mCnapName))
                .add("mCnapNamePresentation", mCnapNamePresentation)
                .toString();
    }
}
