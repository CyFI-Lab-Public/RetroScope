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

/**
 * This class defines constants for webkit test assets.
 */
public class TestHtmlConstants {
    public static final String BR_TAG_URL = "webkit/test_br_tag.html";
    public static final String BR_TAG_TITLE = "test br tag";

    public static final String HELLO_WORLD_URL = "webkit/test_hello_world.html";
    public static final String HELLO_WORLD_TITLE = "test hello world";

    public static final String TEST_FAVICON_URL = "webkit/test_favicon.html";

    public static final String LARGE_IMG_URL = "images/robot.png";
    public static final String SMALL_IMG_URL = "images/tomato.png";

    public static final String EMBEDDED_IMG_URL = "webkit/embedded_image.html";
    public static final String POPUP_URL = "webkit/popup_base.html";
    public static final String JAVASCRIPT_URL = "webkit/javascript.html";
    public static final String JS_ALERT_URL = "webkit/jsalert.html";
    public static final String JS_CONFIRM_URL = "webkit/jsconfirm.html";
    public static final String JS_PROMPT_URL = "webkit/jsprompt.html";
    public static final String JS_UNLOAD_URL = "webkit/jsunload.html";
    public static final String JS_WINDOW_URL = "webkit/jswindow.html";
    public static final String JS_TIMEOUT_URL = "webkit/jstimeout.html";
    public static final String JS_FORM_URL = "webkit/jsform.html";

    public static final String FONT_URL = "webkit/fonts.html";

    public static final String NETWORK_STATE_URL = "webkit/network_state.html";
    public static final String TEST_TIMER_URL = "webkit/test_timer.html";

    public static final String HTML_URL1 = "webkit/test_firstPage.html";
    public static final String HTML_URL2 = "webkit/test_secondPage.html";
    public static final String HTML_URL3 = "webkit/test_thirdPage.html";

    public static final String BLANK_PAGE_URL = "webkit/test_blankPage.html";
    public static final String ADD_JAVA_SCRIPT_INTERFACE_URL = "webkit/test_jsInterface.html";

    public static final String EXT_WEB_URL1 = "http://www.example.com/";

    public static final String LOCAL_FILESYSTEM_URL = "file:///etc/hosts";

    public static final String PARAM_ASSET_URL = "webkit/test_queryparam.html";
    public static final String ANCHOR_ASSET_URL = "webkit/test_anchor.html";
    public static final String IMAGE_ACCESS_URL = "webkit/test_imageaccess.html";
    public static final String IFRAME_ACCESS_URL = "webkit/test_iframeaccess.html";
    public static final String DATABASE_ACCESS_URL = "webkit/test_databaseaccess.html";
    public static final String STOP_LOADING_URL = "webkit/test_stop_loading.html";
    public static final String BLANK_TAG_URL = "webkit/blank_tag.html";

    // Must match the title of the page at
    // android/frameworks/base/core/res/res/raw/loaderror.html
    public static final String WEBPAGE_NOT_AVAILABLE_TITLE = "Webpage not available";

    public static final String getFileUrl(String assetName) {
        if (assetName.contains(":") || assetName.startsWith("/")) {
            throw new IllegalArgumentException();
        }
        return "file:///android_asset/" + assetName;
    }
}
