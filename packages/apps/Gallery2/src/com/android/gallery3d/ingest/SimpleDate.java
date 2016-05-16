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

package com.android.gallery3d.ingest;

import java.text.DateFormat;
import java.util.Calendar;

/**
 * Represents a date (year, month, day)
 */
public class SimpleDate implements Comparable<SimpleDate> {
    public int month; // MM
    public int day; // DD
    public int year; // YYYY
    private long timestamp;
    private String mCachedStringRepresentation;

    public SimpleDate() {
    }

    public SimpleDate(long timestamp) {
        setTimestamp(timestamp);
    }

    private static Calendar sCalendarInstance = Calendar.getInstance();

    public void setTimestamp(long timestamp) {
        synchronized (sCalendarInstance) {
            // TODO find a more efficient way to convert a timestamp to a date?
            sCalendarInstance.setTimeInMillis(timestamp);
            this.day = sCalendarInstance.get(Calendar.DATE);
            this.month = sCalendarInstance.get(Calendar.MONTH);
            this.year = sCalendarInstance.get(Calendar.YEAR);
            this.timestamp = timestamp;
            mCachedStringRepresentation = DateFormat.getDateInstance(DateFormat.SHORT).format(timestamp);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + day;
        result = prime * result + month;
        result = prime * result + year;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof SimpleDate))
            return false;
        SimpleDate other = (SimpleDate) obj;
        if (year != other.year)
            return false;
        if (month != other.month)
            return false;
        if (day != other.day)
            return false;
        return true;
    }

    @Override
    public int compareTo(SimpleDate other) {
        int yearDiff = this.year - other.getYear();
        if (yearDiff != 0)
            return yearDiff;
        else {
            int monthDiff = this.month - other.getMonth();
            if (monthDiff != 0)
                return monthDiff;
            else
                return this.day - other.getDay();
        }
    }

    public int getDay() {
        return day;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    @Override
    public String toString() {
        if (mCachedStringRepresentation == null) {
            mCachedStringRepresentation = DateFormat.getDateInstance(DateFormat.SHORT).format(timestamp);
        }
        return mCachedStringRepresentation;
    }
}
