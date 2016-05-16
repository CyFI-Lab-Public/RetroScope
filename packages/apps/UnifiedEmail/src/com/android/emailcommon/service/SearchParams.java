/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.emailcommon.service;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import com.google.common.base.Objects;

import java.util.Date;

public class SearchParams implements Parcelable {

    private static final int DEFAULT_LIMIT = 10; // Need input on what this number should be
    private static final int DEFAULT_OFFSET = 0;

    // The id of the mailbox to be searched; if -1, all mailboxes MUST be searched
    public final long mMailboxId;
    // If true, all subfolders of the specified mailbox MUST be searched
    public boolean mIncludeChildren = true;
    // The search terms (the search MUST only select messages whose contents include all of the
    // search terms in the query)
    public final String mFilter;
    // The start date (GreaterThan) for date-windowing results
    public final Date mStartDate;
    // The end date (LessThan) for date-windowing results
    public final Date mEndDate;
    // The maximum number of results to be created by this search
    public int mLimit = DEFAULT_LIMIT;
    // If zero, specifies a "new" search; otherwise, asks for a continuation of the previous
    // query(ies) starting with the mOffset'th match (0 based)
    public int mOffset = DEFAULT_OFFSET;
    // The id of the "search" mailbox being used
    public long mSearchMailboxId;

    /**
     * Error codes returned by the searchMessages API
     */
    public static class SearchParamsError {
        public static final int CANT_SEARCH_ALL_MAILBOXES = -1;
        public static final int CANT_SEARCH_CHILDREN = -2;
    }

    public SearchParams(long mailboxId, String filter) {
        mMailboxId = mailboxId;
        mFilter = filter;
        mStartDate = null;
        mEndDate = null;
    }

    public SearchParams(long mailboxId, String filter, long searchMailboxId) {
        mMailboxId = mailboxId;
        mFilter = filter;
        mStartDate = null;
        mEndDate = null;
        mSearchMailboxId = searchMailboxId;
    }

    public SearchParams(long mailboxId, String filter, long searchMailboxId, Date startDate,
            Date endDate) {
        mMailboxId = mailboxId;
        mFilter = filter;
        mSearchMailboxId = searchMailboxId;
        mStartDate = startDate;
        mEndDate = endDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if ((o == null) || !(o instanceof SearchParams)) {
            return false;
        }

        SearchParams os = (SearchParams) o;
        return mMailboxId == os.mMailboxId
                && mIncludeChildren == os.mIncludeChildren
                && mFilter.equals(os.mFilter)
                && Objects.equal(mStartDate, os.mStartDate)
                && Objects.equal(mEndDate, os.mEndDate)
                && mLimit == os.mLimit
                && mOffset == os.mOffset;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mMailboxId, mFilter, mStartDate, mEndDate, mLimit, mOffset);
    }

    @Override
    public String toString() {
        return "[SearchParams "
                + mMailboxId + ":" + mFilter
                + " (" + mOffset + ", " + mLimit + ")"
                + " {" + mStartDate + ", " + mEndDate + "}"
                + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Supports Parcelable
     */
    public static final Parcelable.Creator<SearchParams> CREATOR
        = new Parcelable.Creator<SearchParams>() {
        @Override
        public SearchParams createFromParcel(Parcel in) {
            return new SearchParams(in);
        }

        @Override
        public SearchParams[] newArray(int size) {
            return new SearchParams[size];
        }
    };

    /**
     * Supports Parcelable
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mMailboxId);
        dest.writeInt(mIncludeChildren ? 1 : 0);
        dest.writeString(mFilter);
        dest.writeInt(mLimit);
        dest.writeInt(mOffset);
        SparseArray<Object> dateWindow = new SparseArray<Object>(2);
        if (mStartDate != null) {
            dateWindow.put(0, mStartDate.getTime());
        }
        if (mEndDate != null) {
            dateWindow.put(1, mEndDate.getTime());
        }
        dest.writeSparseArray(dateWindow);
    }

    /**
     * Supports Parcelable
     */
    public SearchParams(Parcel in) {
        mMailboxId = in.readLong();
        mIncludeChildren = in.readInt() == 1;
        mFilter = in.readString();
        mLimit = in.readInt();
        mOffset = in.readInt();
        SparseArray dateWindow = in.readSparseArray(this.getClass().getClassLoader());
        if (dateWindow.get(0) != null) {
            mStartDate = new Date((Long)dateWindow.get(0));
        } else {
            mStartDate = null;
        }
        if (dateWindow.get(1) != null) {
            mEndDate = new Date((Long)dateWindow.get(1));
        } else {
            mEndDate = null;
        }
    }
}
