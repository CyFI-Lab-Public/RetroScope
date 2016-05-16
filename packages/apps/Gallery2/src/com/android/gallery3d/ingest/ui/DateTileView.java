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

package com.android.gallery3d.ingest.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.gallery3d.R;
import com.android.gallery3d.ingest.SimpleDate;

import java.text.DateFormatSymbols;
import java.util.Locale;

public class DateTileView extends FrameLayout {
    private static String[] sMonthNames = DateFormatSymbols.getInstance().getShortMonths();
    private static Locale sLocale;

    static {
        refreshLocale();
    }

    public static boolean refreshLocale() {
        Locale currentLocale = Locale.getDefault();
        if (!currentLocale.equals(sLocale)) {
            sLocale = currentLocale;
            sMonthNames = DateFormatSymbols.getInstance(sLocale).getShortMonths();
            return true;
        } else {
            return false;
        }
    }

    private TextView mDateTextView;
    private TextView mMonthTextView;
    private TextView mYearTextView;
    private int mMonth = -1;
    private int mYear = -1;
    private int mDate = -1;
    private String[] mMonthNames = sMonthNames;

    public DateTileView(Context context) {
        super(context);
    }

    public DateTileView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DateTileView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Force this to be square
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mDateTextView = (TextView) findViewById(R.id.date_tile_day);
        mMonthTextView = (TextView) findViewById(R.id.date_tile_month);
        mYearTextView = (TextView) findViewById(R.id.date_tile_year);
    }

    public void setDate(SimpleDate date) {
        setDate(date.getDay(), date.getMonth(), date.getYear());
    }

    public void setDate(int date, int month, int year) {
        if (date != mDate) {
            mDate = date;
            mDateTextView.setText(mDate > 9 ? Integer.toString(mDate) : "0" + mDate);
        }
        if (mMonthNames != sMonthNames) {
            mMonthNames = sMonthNames;
            if (month == mMonth) {
                mMonthTextView.setText(mMonthNames[mMonth]);
            }
        }
        if (month != mMonth) {
            mMonth = month;
            mMonthTextView.setText(mMonthNames[mMonth]);
        }
        if (year != mYear) {
            mYear = year;
            mYearTextView.setText(Integer.toString(mYear));
        }
    }
}
