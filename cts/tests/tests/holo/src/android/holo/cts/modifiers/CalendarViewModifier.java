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

package android.holo.cts.modifiers;

import android.holo.cts.LayoutModifier;
import android.view.View;
import android.widget.CalendarView;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * {@LayoutModifier} to set a precise date on a {@link CalendarView}.
 */
public class CalendarViewModifier implements LayoutModifier {
    /**
     * Long representation of a date that is 30 years in milliseconds from
     * Unix epoch (January 1, 1970 00:00:00).
     */
    private static final long JANUARY_DATE = 946707779241L;

    private static final long FEBRUARY_DATE = 951033600000L;

    private static final TimeZone TZ = TimeZone.getTimeZone("GMT+00:00");

    private final boolean mJanuary;

    public CalendarViewModifier(boolean january) {
        mJanuary = january;
    }

    @Override
    public void prepare() {
        TimeZone.setDefault(TZ);
    }

    @Override
    public View modifyView(View view) {
        ((CalendarView) view).setDate(mJanuary ? JANUARY_DATE : FEBRUARY_DATE);
        return view;
    }

    public static boolean isMonth(int month) {
        Calendar cal = new GregorianCalendar(TZ);
        return cal.get(Calendar.MONTH) == month;
    }
}
