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

package android.app.cts;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import com.android.cts.stub.R;

public class InstrumentationTestActivity extends Activity {

    private boolean mOnCreateCalled;
    private boolean mOnDestroyCalled ;
    private boolean mOnNewIntentCalled;
    private boolean mOnPauseCalled;
    private boolean mOnPostCreate;
    private boolean mOnRestart;
    private boolean mOnRestoreInstanceState;
    private boolean mOnResume;
    private boolean mOnSaveInstanceState;
    private boolean mOnStart;
    private boolean mOnStop;
    private boolean mOnMenuOpened;
    private boolean mOnLeave;
    private int mMenuID;
    private boolean mOnTouchEventCalled;
    private int mKeyDownCode;
    private int mKeyUpCode;
    private MotionEvent mMotionEvent;
    private Bundle mBundle;
    private MockTextView mTextView;
    private List<KeyEvent> mKeyDownList = new ArrayList<KeyEvent>();
    private List<KeyEvent> mKeyUpList = new ArrayList<KeyEvent>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTextView = new MockTextView(this);
        setContentView(mTextView);
        mOnCreateCalled = true;
    }

    class MockTextView extends TextView {

        public MockTextView(Context context) {
            super(context);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return super.onTouchEvent(event);
        }

        @Override
        public boolean onTrackballEvent(MotionEvent event) {
            return super.onTrackballEvent(event);
        }

        @Override
        public void getLocationOnScreen(int[] location) {
            super.getLocationOnScreen(location);
            location[0] = location[1] = 10;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOnDestroyCalled = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mOnNewIntentCalled = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOnPauseCalled = true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mOnPostCreate = true;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mOnRestart = true;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mOnRestoreInstanceState = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mOnTouchEventCalled = true;
        mMotionEvent = event;
        return super.onTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mOnResume = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mOnSaveInstanceState = true;
        mBundle = outState;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mOnStart = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mOnStop = true;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        mOnLeave = true;
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        mOnMenuOpened = true;
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.browser, menu);

        menu.add("title");
        mOnMenuOpened = true;
        return true;
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        mMenuID = item.getItemId();
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void openContextMenu(View view) {
        super.openContextMenu(view);
    }

    @Override
    public void openOptionsMenu() {
        super.openOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        mKeyDownList.add(event);
        mKeyDownCode = keyCode;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        mMotionEvent = event;
        return super.onTrackballEvent(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        mKeyUpList.add(event);
        mKeyUpCode = keyCode;
        return super.onKeyUp(keyCode, event);
    }

    public boolean isOnCreateCalled() {
        return mOnCreateCalled;
    }

    public void setOnCreateCalled(boolean onCreateCalled) {
        mOnCreateCalled = onCreateCalled;
    }

    public boolean isOnDestroyCalled() {
        return mOnDestroyCalled;
    }

    public void setOnDestroyCalled(boolean onDestroyCalled) {
        mOnDestroyCalled = onDestroyCalled;
    }

    public boolean isOnNewIntentCalled() {
        return mOnNewIntentCalled;
    }

    public void setOnNewIntentCalled(boolean onNewIntentCalled) {
        mOnNewIntentCalled = onNewIntentCalled;
    }

    public boolean isOnPauseCalled() {
        return mOnPauseCalled;
    }

    public void setOnPauseCalled(boolean onPauseCalled) {
        mOnPauseCalled = onPauseCalled;
    }

    public boolean isOnPostCreate() {
        return mOnPostCreate;
    }

    public void setOnPostCreate(boolean onPostCreate) {
        mOnPostCreate = onPostCreate;
    }

    public boolean isOnRestart() {
        return mOnRestart;
    }

    public void setOnRestart(boolean onRestart) {
        mOnRestart = onRestart;
    }

    public boolean isOnRestoreInstanceState() {
        return mOnRestoreInstanceState;
    }

    public void setOnRestoreInstanceState(boolean onRestoreInstanceState) {
        mOnRestoreInstanceState = onRestoreInstanceState;
    }

    public boolean isOnResume() {
        return mOnResume;
    }

    public void setOnResume(boolean onResume) {
        mOnResume = onResume;
    }

    public boolean isOnSaveInstanceState() {
        return mOnSaveInstanceState;
    }

    public void setOnSaveInstanceState(boolean onSaveInstanceState) {
        mOnSaveInstanceState = onSaveInstanceState;
    }

    public boolean isOnStart() {
        return mOnStart;
    }

    public void setOnStart(boolean onStart) {
        mOnStart = onStart;
    }

    public boolean isOnStop() {
        return mOnStop;
    }

    public boolean isOnLeave() {
        return mOnLeave;
    }

    public void setOnStop(boolean onStop) {
        mOnStop = onStop;
    }

    public boolean isMOnMenuOpened() {
        return mOnMenuOpened;
    }

    public void setOnMenuOpened(boolean onMenuOpened) {
        mOnMenuOpened = onMenuOpened;
    }

    public int getMenuID() {
        return mMenuID;
    }

    public void setMenuID(int menuID) {
        mMenuID = menuID;
    }

    public MotionEvent getMotionEvent() {
        return mMotionEvent;
    }

    public Bundle getBundle() {
        return mBundle;
    }

    public boolean isOnTouchEventCalled() {
        return mOnTouchEventCalled;
    }

    public void setOnTouchEventCalled(boolean onTouchEventCalled) {
        mOnTouchEventCalled = onTouchEventCalled;
    }

    public int getKeyUpCode() {
        return mKeyUpCode;
    }

    public int getKeyDownCode() {
        return mKeyDownCode;
    }

    public List<KeyEvent> getKeyUpList() {
        return mKeyUpList;
    }

    public List<KeyEvent> getKeyDownList() {
        return mKeyDownList;
    }
}
