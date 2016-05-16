/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.webkit.cts;

import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.webkit.WebView;
import android.webkit.WebView.WebViewTransport;


public class WebView_WebViewTransportTest
        extends ActivityInstrumentationTestCase2<WebViewStubActivity> {

    public WebView_WebViewTransportTest() {
        super("com.android.cts.stub", WebViewStubActivity.class);
    }

    @UiThreadTest
    public void testAccessWebView() {
        WebView webView = getActivity().getWebView();
        WebViewTransport transport = webView.new WebViewTransport();

        assertNull(transport.getWebView());

        transport.setWebView(webView);
        assertSame(webView, transport.getWebView());
    }
}
