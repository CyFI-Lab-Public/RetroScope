/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.cts.uiautomator;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class Test6DetailFragment extends Fragment {
    public static final String ARG_ITEM_ID = "item_id";
    private final static String PAGE = "<html><body>"
            + "This is test <b>6</b> for WebView text traversal test."
            + "<p/><a href=\"http://google.com\">This is a link to google</a><br/>"
            + "<h5>This is h5 text</h5>"
            + "<a href=\"http://yahoo.com\">This is a link to yahoo</a>"
            + "<p/><h4>This is h4 text</h4>" + "</body></html>";

    public Test6DetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View rootView = inflater.inflate(R.layout.test6_detail_fragment, container, false);
        WebView wv = (WebView) rootView.findViewById(R.id.test6WebView);
        wv.loadData(PAGE, "text/html", null);
        return rootView;
    }
}
