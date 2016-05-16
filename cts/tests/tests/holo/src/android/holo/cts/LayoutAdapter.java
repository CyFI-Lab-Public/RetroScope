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

package android.holo.cts;

import com.android.cts.holo.R;

import android.holo.cts.modifiers.CalendarViewModifier;
import android.holo.cts.modifiers.ProgressBarModifier;
import android.holo.cts.modifiers.SearchViewModifier;
import android.holo.cts.modifiers.TabHostModifier;
import android.holo.cts.modifiers.TimePickerModifier;
import android.holo.cts.modifiers.ViewPressedModifier;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * {@link BaseAdapter} for all the layouts used to test the Holo theme.
 */
class LayoutAdapter extends BaseAdapter {

    /** Mode where we are just viewing all the layouts. */
    static final int MODE_VIEWING = 0;

    /** Mode where we are testing and may not include some layouts based on the device state. */
    static final int MODE_TESTING = 1;

    /** No timeout for widgets in layouts that aren't changed after inflation.  */
    private static final int NO_TIMEOUT_MS = 0;

    /** Small timeout for letting things like button presses to be handled. */
    private static final int SHORT_TIMEOUT_MS = 500;

    /** Longer timeout for widgets that have one-time animations. */
    private static final int LONG_TIMEOUT_MS = 6000;

    static class LayoutInfo {
        private final int mDisplayName;
        private final String mFileName;
        private final int mLayout;
        private final LayoutModifier mModifier;
        private final long mTimeoutMs;

        private LayoutInfo(int displayName, String fileName, int layout, LayoutModifier modifier,
                long timeoutMs) {
            mDisplayName = displayName;
            mFileName = fileName;
            mLayout = layout;
            mModifier = modifier;
            mTimeoutMs = timeoutMs;
        }

        /** @return file name to use for creating reference and failure bitmaps. */
        public String getBitmapName() {
            return mFileName;
        }

        /** @return layout that will be inflated and captured to a bitmap */
        public int getLayout() {
            return mLayout;
        }

        /** @return whether or non the layout has a layout modifier */
        public boolean hasModifier() {
            return mModifier != null;
        }

        /** @return null or modifier that will manipulate the layout after it is inflated */
        public LayoutModifier getModifier() {
            return mModifier;
        }

        /** @return timeout before taking a snapshot of the layout */
        public long getTimeoutMs() {
            return mTimeoutMs;
        }
    }

    private final List<LayoutInfo> mLayoutInfos = new ArrayList<LayoutInfo>();

    private final LayoutInflater mInflater;

    LayoutAdapter(LayoutInflater inflater, int adapterMode) {
        mInflater = inflater;

        // Widgets

        addLayout(R.string.button, "button",
                R.layout.button, null, NO_TIMEOUT_MS);

        addLayout(R.string.button_pressed, "button_pressed",
                R.layout.button, new ViewPressedModifier(), LONG_TIMEOUT_MS);

        addCalendarLayouts(adapterMode);

        addLayout(R.string.checkbox, "checkbox",
                R.layout.checkbox, null, NO_TIMEOUT_MS);

        addLayout(R.string.checkbox_checked, "checkbox_checked",
                R.layout.checkbox_checked, null, SHORT_TIMEOUT_MS);

        addLayout(R.string.chronometer, "chronometer",
                R.layout.chronometer, null, NO_TIMEOUT_MS);

        // TODO: DatePicker has a blinking cursor that can be captured in the bitmap...
        // addLayout(R.string.datepicker, "datepicker",
        //        R.layout.datepicker, new DatePickerModifier(), LONG_TIMEOUT_MS);

        addLayout(R.string.edittext, "edittext",
                R.layout.edittext, null, NO_TIMEOUT_MS);

        addLayout(R.string.progressbar, "progressbar",
                R.layout.progressbar, new ProgressBarModifier(), NO_TIMEOUT_MS);

        addLayout(R.string.progressbar_small, "progressbar_small",
                R.layout.progressbar_small, new ProgressBarModifier(), NO_TIMEOUT_MS);

        addLayout(R.string.progressbar_large, "progressbar_large",
                R.layout.progressbar_large, new ProgressBarModifier(), NO_TIMEOUT_MS);

        addLayout(R.string.progressbar_horizontal_0, "progressbar_horizontal_0",
                R.layout.progressbar_horizontal_0, null, NO_TIMEOUT_MS);

        addLayout(R.string.progressbar_horizontal_50, "progressbar_horizontal_50",
                R.layout.progressbar_horizontal_50, null, NO_TIMEOUT_MS);

        addLayout(R.string.progressbar_horizontal_100, "progressbar_horizontal_100",
                R.layout.progressbar_horizontal_100, null, NO_TIMEOUT_MS);

        addLayout(R.string.radiobutton, "radio_button",
                R.layout.radiobutton, null, NO_TIMEOUT_MS);

        addLayout(R.string.radiobutton_checked, "radio_button_checked",
                R.layout.radiobutton_checked, null, NO_TIMEOUT_MS);

        addLayout(R.string.radiogroup_horizontal, "radiogroup_horizontal",
                R.layout.radiogroup_horizontal, null, NO_TIMEOUT_MS);

        addLayout(R.string.radiogroup_vertical, "radiogroup_vertical",
                R.layout.radiogroup_vertical, null, NO_TIMEOUT_MS);

        addLayout(R.string.ratingbar_0, "ratingbar_0",
                R.layout.ratingbar_0, null, NO_TIMEOUT_MS);

        addLayout(R.string.ratingbar_2point5, "ratingbar_2point5",
                R.layout.ratingbar_2point5, null, NO_TIMEOUT_MS);

        addLayout(R.string.ratingbar_5, "ratingbar_5",
                R.layout.ratingbar_5, null, NO_TIMEOUT_MS);

        addLayout(R.string.ratingbar_0_pressed, "ratingbar_0_pressed",
                R.layout.ratingbar_0, new ViewPressedModifier(), LONG_TIMEOUT_MS);

        addLayout(R.string.ratingbar_2point5_pressed, "ratingbar_2point5_pressed",
                R.layout.ratingbar_2point5, new ViewPressedModifier(), LONG_TIMEOUT_MS);

        addLayout(R.string.ratingbar_5_pressed, "ratingbar_5_pressed",
                R.layout.ratingbar_5, new ViewPressedModifier(), LONG_TIMEOUT_MS);

        addLayout(R.string.searchview, "searchview",
                R.layout.searchview, null, NO_TIMEOUT_MS);

        addLayout(R.string.searchview_query, "searchview_query",
                R.layout.searchview, new SearchViewModifier(SearchViewModifier.QUERY),
                SHORT_TIMEOUT_MS);

        addLayout(R.string.searchview_query_hint, "searchview_query_hint",
                R.layout.searchview, new SearchViewModifier(SearchViewModifier.QUERY_HINT),
                SHORT_TIMEOUT_MS);

        addLayout(R.string.seekbar_0, "seekbar_0",
                R.layout.seekbar_0, null, NO_TIMEOUT_MS);

        addLayout(R.string.seekbar_50, "seekbar_50",
                R.layout.seekbar_50, null, NO_TIMEOUT_MS);

        addLayout(R.string.seekbar_100, "seekbar_100",
                R.layout.seekbar_100, null, NO_TIMEOUT_MS);

        addLayout(R.string.spinner, "spinner",
                R.layout.spinner, null, NO_TIMEOUT_MS);

        addLayout(R.string.switch_button, "switch",
                R.layout.switch_button, null, NO_TIMEOUT_MS);

        addLayout(R.string.switch_button_checked, "switch_checked",
                R.layout.switch_button_checked, null, NO_TIMEOUT_MS);

        addLayout(R.string.tabhost, "tabhost",
                R.layout.tabhost, new TabHostModifier(), SHORT_TIMEOUT_MS);

        addLayout(R.string.textview, "textview",
                R.layout.textview, null, NO_TIMEOUT_MS);

        addLayout(R.string.timepicker, "timepicker",
                R.layout.timepicker, new TimePickerModifier(), LONG_TIMEOUT_MS);

        addLayout(R.string.togglebutton, "toggle_button",
                R.layout.togglebutton, null, NO_TIMEOUT_MS);

        addLayout(R.string.togglebutton_checked, "toggle_button_checked",
                R.layout.togglebutton_checked, null, NO_TIMEOUT_MS);


        // TODO: Zoom control hasn't been styled for Holo so don't test them.

//        addLayout(R.string.zoomcontrols, "zoomcontrols",
//                R.layout.zoomcontrols, null, NO_TIMEOUT_MS);

        // Dialogs


        // TODO: Dialogs are changing sizes depending on screen sizes, so we can't test these.

//        addLayout(R.string.alertdialog_onebutton, "alertdialog_onebutton", R.layout.empty,
//                new DialogModifier(new AlertDialogBuilder(AlertDialogBuilder.ONE_BUTTON)),
//                SHORT_TIMEOUT_MS);
//
//        addLayout(R.string.alertdialog_twobuttons, "alertdialog_twobuttons", R.layout.empty,
//                new DialogModifier(new AlertDialogBuilder(AlertDialogBuilder.TWO_BUTTONS)),
//                SHORT_TIMEOUT_MS);
//
//        addLayout(R.string.alertdialog_threebuttons, "alertdialog_threebuttons", R.layout.empty,
//                new DialogModifier(new AlertDialogBuilder(AlertDialogBuilder.THREE_BUTTONS)),
//                SHORT_TIMEOUT_MS);
//
//        addLayout(R.string.alertdialog_list, "alertdialog_list", R.layout.empty,
//                new DialogModifier(new AlertDialogBuilder(AlertDialogBuilder.LIST)),
//                SHORT_TIMEOUT_MS);
//
//        addLayout(R.string.alertdialog_singlechoice, "alertdialog_singlechoice", R.layout.empty,
//                new DialogModifier(new AlertDialogBuilder(AlertDialogBuilder.SINGLE_CHOICE)),
//                SHORT_TIMEOUT_MS);
//
//        addLayout(R.string.alertdialog_multichoice, "alertdialog_multichoice", R.layout.empty,
//                new DialogModifier(new AlertDialogBuilder(AlertDialogBuilder.MULTI_CHOICE)),
//                SHORT_TIMEOUT_MS);

        // TODO: We can't test the spinner, because there is no way to halt the animation.
        // addLayout(R.string.progressdialog_spinner, "progressdialog_spinner", R.layout.empty,
        //      new DialogModifier(new ProgressDialogBuilder(ProgressDialog.STYLE_SPINNER)));

//        addLayout(R.string.progressdialog_horizontal, "progressdialog_horizontal", R.layout.empty,
//                new DialogModifier(new ProgressDialogBuilder(ProgressDialog.STYLE_HORIZONTAL)),
//                SHORT_TIMEOUT_MS);

        // Colors

        addLayout(R.string.color_blue_bright, "color_blue_bright",
                R.layout.color_blue_bright, null, NO_TIMEOUT_MS);

        addLayout(R.string.color_blue_dark, "color_blue_dark",
                R.layout.color_blue_dark, null, NO_TIMEOUT_MS);

        addLayout(R.string.color_blue_light, "color_blue_light",
                R.layout.color_blue_light, null, NO_TIMEOUT_MS);

        addLayout(R.string.color_green_dark, "color_green_dark",
                R.layout.color_green_dark, null, NO_TIMEOUT_MS);

        addLayout(R.string.color_green_light, "color_green_light",
                R.layout.color_green_light, null, NO_TIMEOUT_MS);

        addLayout(R.string.color_orange_dark, "color_orange_dark",
                R.layout.color_orange_dark, null, NO_TIMEOUT_MS);

        addLayout(R.string.color_orange_light, "color_orange_light",
                R.layout.color_orange_light, null, NO_TIMEOUT_MS);

        addLayout(R.string.color_purple, "color_purple",
                R.layout.color_purple, null, NO_TIMEOUT_MS);

        addLayout(R.string.color_red_dark, "color_red_dark",
                R.layout.color_red_dark, null, NO_TIMEOUT_MS);

        addLayout(R.string.color_red_light, "color_red_light",
                R.layout.color_red_light, null, NO_TIMEOUT_MS);
    }

    private void addLayout(int displayName, String fileName, int layout, LayoutModifier modifier,
            long timeoutMs) {
        addLayout(new LayoutInfo(displayName, fileName, layout, modifier, timeoutMs));
    }

    private void addLayout(LayoutInfo info) {
        mLayoutInfos.add(info);
    }

    private void addCalendarLayouts(int adapterMode) {
        if (adapterMode == MODE_VIEWING || !CalendarViewModifier.isMonth(Calendar.JANUARY)) {
            addLayout(getCalendarLayoutInfo());
        }

        if (adapterMode == MODE_VIEWING || !CalendarViewModifier.isMonth(Calendar.FEBRUARY)) {
            addLayout(getFebruaryCalendarLayoutInfo());
        }
    }

    private LayoutInfo getCalendarLayoutInfo() {
        return new LayoutInfo(R.string.calendarview_jan, "calendar_view",
            R.layout.calendarview, new CalendarViewModifier(true), SHORT_TIMEOUT_MS);
    }

    private LayoutInfo getFebruaryCalendarLayoutInfo() {
        return new LayoutInfo(R.string.calendarview_feb, "calendar_view_feb",
            R.layout.calendarview, new CalendarViewModifier(false), SHORT_TIMEOUT_MS);
    }

    @Override
    public int getCount() {
        return mLayoutInfos.size();
    }

    @Override
    public LayoutInfo getItem(int position) {
        return mLayoutInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).mLayout;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if (convertView != null) {
            textView = (TextView) convertView;
        } else {
            textView = (TextView) mInflater.inflate(android.R.layout.simple_list_item_1,
                    parent, false);
        }
        LayoutInfo layoutInfo = getItem(position);
        textView.setText(layoutInfo.mDisplayName);
        return textView;
    }
}
