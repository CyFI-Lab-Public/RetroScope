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

import android.webkit.ConsoleMessage;
import android.webkit.cts.WebViewOnUiThread.WaitForProgressClient;

// A chrome client for listening webview chrome events.
class ChromeClient extends WaitForProgressClient {

    private boolean mIsMessageLevelAvailable;
    private ConsoleMessage.MessageLevel mMessageLevel;

    public ChromeClient(WebViewOnUiThread onUiThread) {
        super(onUiThread);
    }

    @Override
    public synchronized boolean onConsoleMessage(ConsoleMessage message) {
        mMessageLevel = message.messageLevel();
        mIsMessageLevelAvailable = true;
        notify();
        // return false for default handling; i.e. printing the message.
        return false;
    }

    public synchronized ConsoleMessage.MessageLevel getMessageLevel(int timeout) {
        for(; timeout > 0; timeout -= 1000) {
            if( mIsMessageLevelAvailable ) break;
            try {
                wait(1000);
            } catch (InterruptedException e) {
            }
        }
        return mMessageLevel;
    }
}
