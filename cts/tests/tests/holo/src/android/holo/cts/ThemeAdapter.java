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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link BaseAdapter} containing all the themes.
 */
class ThemeAdapter extends BaseAdapter  {

    static class ThemeInfo {
        private final String mName;
        private final int mTheme;
        private final String mFileName;

        private ThemeInfo(String name, int theme, String fileName) {
            mName = name;
            mTheme = theme;
            mFileName = fileName;
        }

        public String getName() {
            return mName;
        }

        public int getTheme() {
            return mTheme;
        }

        public String getBitmapName() {
            return mFileName;
        }
    }

    private final List<ThemeInfo> mThemeInfos = new ArrayList<ThemeInfo>();

    private final LayoutInflater mInflater;

    ThemeAdapter(LayoutInflater inflater) {
        mInflater = inflater;

        addTheme("Holo",
                android.R.style.Theme_Holo,
                "holo");

        addTheme("Holo Dialog",
                android.R.style.Theme_Holo_Dialog,
                "holo_dialog");

        addTheme("Holo Dialog Minimum Width",
                android.R.style.Theme_Holo_Dialog_MinWidth,
                "holo_dialog_minwidth");

        addTheme("Holo Dialog No Action Bar",
                android.R.style.Theme_Holo_Dialog_NoActionBar,
                "holo_dialog_noactionbar");

        addTheme("Holo Dialog No Action Bar Minimum Width",
                android.R.style.Theme_Holo_Dialog_NoActionBar_MinWidth,
                "holo_dialog_noactionbar_minwidth");

        addTheme("Holo Dialog When Large",
                android.R.style.Theme_Holo_DialogWhenLarge,
                "holo_dialogwhenlarge");

        addTheme("Holo Dialog When Large No Action Bar",
                android.R.style.Theme_Holo_DialogWhenLarge_NoActionBar,
                "holo_dialogwhenlarge_noactionbar");

        addTheme("Holo Input Method",
                android.R.style.Theme_Holo_InputMethod,
                "holo_inputmethod");

        addTheme("Holo Light",
                android.R.style.Theme_Holo_Light,
                "holo_light");

        addTheme("Holo Light Dark Action Bar",
                android.R.style.Theme_Holo_Light_DarkActionBar,
                "holo_light_darkactionbar");

        addTheme("Holo Light Dialog",
                android.R.style.Theme_Holo_Light_Dialog,
                "holo_light_dialog");

        addTheme("Holo Light Dialog Minimum Width",
                android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                "holo_light_dialog_minwidth");

        addTheme("Holo Light Dialog No Action Bar",
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                "holo_light_dialog_noactionbar");

        addTheme("Holo Light Dialog No Action Bar Minimum Width",
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth,
                "holo_light_dialog_noactionbar_minwidth");

        addTheme("Holo Light Dialog When Large",
                android.R.style.Theme_Holo_Light_DialogWhenLarge,
                "holo_light_dialogwhenlarge");

        addTheme("Holo Light Dialog When Large No Action Bar",
                android.R.style.Theme_Holo_Light_DialogWhenLarge_NoActionBar,
                "holo_light_dialogwhenlarge_noactionbar");

        addTheme("Holo Light No Action Bar",
                android.R.style.Theme_Holo_Light_NoActionBar,
                "holo_light_noactionbar");

        addTheme("Holo Light No Action Bar Fullscreen",
                android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen,
                "holo_light_noactionbar_fullscreen");

        addTheme("Holo Light Panel",
                android.R.style.Theme_Holo_Light_Panel,
                "holo_light_panel");

        addTheme("Holo No Action Bar",
                android.R.style.Theme_Holo_NoActionBar,
                "holo_noactionbar");

        addTheme("Holo No Action Bar Fullscreen",
                android.R.style.Theme_Holo_NoActionBar_Fullscreen,
                "holo_noactionbar_fullscreen");

        addTheme("Holo Panel",
                android.R.style.Theme_Holo_Panel,
                "holo_panel");

        addTheme("Holo Wallpaper",
                android.R.style.Theme_Holo_Wallpaper,
                "holo_wallpaper");

        addTheme("Holo Wallpaper No Title Bar",
                android.R.style.Theme_Holo_Wallpaper_NoTitleBar,
                "holo_wallpaper_notitlebar");

        // NOTE: Adding a theme doesn't mean it will be tested. You have to add an explicit
        //       test in HoloTest for it!
    }

    private void addTheme(String name, int theme, String fileNamePrefix) {
        mThemeInfos.add(new ThemeInfo(name, theme, fileNamePrefix));
    }

    @Override
    public int getCount() {
        return mThemeInfos.size();
    }

    @Override
    public ThemeInfo getItem(int position) {
        return mThemeInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).mTheme;
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
        ThemeInfo themeInfo = getItem(position);
        textView.setText(themeInfo.mName);
        return textView;
    }
}
