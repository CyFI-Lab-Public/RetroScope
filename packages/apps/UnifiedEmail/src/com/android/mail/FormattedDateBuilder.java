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
package com.android.mail;

import android.content.Context;
import android.content.res.Resources;
import android.text.format.DateUtils;
import android.text.format.Time;

import java.util.Formatter;

/**
 * Convenience class to efficiently make multiple short date strings. Instantiating and reusing
 * one of these builders is faster than repeatedly bringing up all the locale stuff.
 *
 */
public class FormattedDateBuilder {

    private StringBuilder sb;
    private Formatter dateFormatter;
    private Context mContext;

    public FormattedDateBuilder(Context context) {
        mContext = context;
        sb = new StringBuilder();
        dateFormatter = new Formatter(sb);
    }

    public CharSequence formatShortDate(long when) {
        return DateUtils.getRelativeTimeSpanString(mContext, when);
    }

    public CharSequence formatLongDateTime(long when) {
        final Resources resources = mContext.getResources();

        if (DateUtils.isToday(when)) {
            return resources.getString(R.string.date_message_received_today, formatLongTime(when));
        } else if (isYesterday(when)) {
            return resources.getString(
                    R.string.date_message_received_yesterday, formatLongTime(when));
        } else {
            return resources.getString(R.string.date_message_received,
                    formatLongDayAndDate(when), formatLongTime(when));
        }
    }

    private CharSequence formatLongDayAndDate(long when) {
        sb.setLength(0);
        DateUtils.formatDateRange(mContext, dateFormatter, when, when,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY
                        | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_ALL);
        return sb.toString();
    }

    private CharSequence formatLongTime(long when) {
        sb.setLength(0);
        DateUtils.formatDateRange(mContext, dateFormatter, when, when,
                DateUtils.FORMAT_SHOW_TIME);
        return sb.toString();
    }

    /**
     * @return true if the supplied when is today else false
     */
    private static boolean isYesterday(long when) {
        final Time time = new Time();
        time.set(when);

        final int thenYear = time.year;
        final int thenMonth = time.month;
        final int thenMonthDay = time.monthDay;

        time.set(System.currentTimeMillis());
        return (thenYear == time.year)
                && (thenMonth == time.month)
                && (thenMonthDay == (time.monthDay-1));
    }
}
