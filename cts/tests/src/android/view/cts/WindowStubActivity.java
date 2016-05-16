/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.view.cts;

import com.android.cts.stub.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.Window;

public class WindowStubActivity extends Activity {

    private static boolean mIsOnCreateOptionsMenuCalled;
    private static boolean mIsOnOptionsMenuClosedCalled;
    private static boolean mIsOnKeyDownCalled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.windowstub_layout);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Quit").setShortcut('1', 'q');
        menu.add(Menu.NONE, Menu.NONE, Menu.NONE, "Action").setShortcut('2', 'a');
        mIsOnCreateOptionsMenuCalled = true;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        mIsOnOptionsMenuClosedCalled = true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        mIsOnKeyDownCalled = true;
        return super.onKeyDown(keyCode, event);
    }

    public boolean isOnCreateOptionsMenuCalled() {
        return mIsOnCreateOptionsMenuCalled;
    }

    public boolean isOnOptionsMenuClosedCalled() {
        return mIsOnOptionsMenuClosedCalled;
    }

    public boolean isOnKeyDownCalled() {
        return mIsOnKeyDownCalled;
    }

    public void setFlagFalse() {
        mIsOnCreateOptionsMenuCalled = false;
        mIsOnOptionsMenuClosedCalled = false;
    }
}
