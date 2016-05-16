/**
 * Copyright (c) 2012, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mail.providers;

import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

public class ListParams implements Parcelable {
    private static final String LIMIT_KEY = "limit";
    private static final String USE_NETWORK_KEY = "use-network";

    public static final int NO_LIMIT = -1;

    private static final String LOG_TAG = LogTag.getLogTag();

    // The maximum number of results to be created by this search
    public final int mLimit;

    public final boolean mUseNetwork;

    public ListParams(int limit, boolean useNetwork) {
        mLimit = limit;
        mUseNetwork = useNetwork;
    }

    /**
     * Supports Parcelable
     */
    public static final Parcelable.Creator<ListParams> CREATOR
        = new Parcelable.Creator<ListParams>() {
        @Override
        public ListParams createFromParcel(Parcel in) {
            return new ListParams(in);
        }

        @Override
        public ListParams[] newArray(int size) {
            return new ListParams[size];
        }
    };

    /**
     * Supports Parcelable
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mLimit);
        dest.writeInt(mUseNetwork ? 1 : 0);
    }

    /**
     * Supports Parcelable
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Supports Parcelable
     */
    public ListParams(Parcel in) {
        mLimit = in.readInt();
        mUseNetwork = in.readInt() != 0;
    }

    /**
     * Return a serialized String for this ListParams.
     */
    public synchronized String serialize() {
        JSONObject json = new JSONObject();
        try {
            json.put(LIMIT_KEY, mLimit);
            json.put(USE_NETWORK_KEY, mUseNetwork);
        } catch (JSONException e) {
            LogUtils.wtf(LOG_TAG, e, "Could not serialize ListParams");
        }
        return json.toString();
    }

    /**
     * Create a new instance of an ListParams object using a serialized instance created previously
     * using {@link #serialize()}. This returns null if the serialized instance was invalid or does
     * not represent a valid parameter object.
     *
     * @param serializedParams
     * @return
     */
    public static ListParams newinstance(String serializedParams) {
        // This method is a wrapper to check for errors and exceptions and return back a null
        // in cases something breaks.
        JSONObject json = null;
        try {
            json = new JSONObject(serializedParams);
            final int limit = json.getInt(LIMIT_KEY);
            final boolean useNetwork = json.getBoolean(USE_NETWORK_KEY);
            return new ListParams(limit, useNetwork);
        } catch (JSONException e) {
            LogUtils.wtf(LOG_TAG, e, "Could not create an params object from this input: \""
                    + serializedParams);
            return null;
        }
    }



}