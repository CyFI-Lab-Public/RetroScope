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
package android.app.cts;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.WindowManager.LayoutParams;

public class TestDialog extends Dialog {
    private static final int OPTIONS_MENU_ITEM_0 = Menu.FIRST;
    private static final int OPTIONS_MENU_ITEM_1 = Menu.FIRST + 1;
    private static final int OPTIONS_MENU_ITEM_2 = Menu.FIRST + 2;
    private static final int OPTIONS_MENU_ITEM_3 = Menu.FIRST + 3;
    private static final int OPTIONS_MENU_ITEM_4 = Menu.FIRST + 4;
    private static final int OPTIONS_MENU_ITEM_5 = Menu.FIRST + 5;
    private static final int OPTIONS_MENU_ITEM_6 = Menu.FIRST + 6;
    private static final int CONTEXT_MENU_ITEM_0 = Menu.FIRST + 7;
    private static final int CONTEXT_MENU_ITEM_1 = Menu.FIRST + 8;
    private static final int CONTEXT_MENU_ITEM_2 = Menu.FIRST + 9;
    public boolean isOnStartCalled;
    public boolean isOnStopCalled;
    public boolean isOnCreateCalled;
    public boolean isRequestWindowFeature;
    public boolean isOnContentChangedCalled;
    public boolean isOnWindowFocusChangedCalled;
    public boolean isOnTouchEventCalled;
    public boolean isOnTrackballEventCalled;
    public boolean isOnKeyDownCalled;
    public boolean isOnKeyUpCalled;
    public boolean isOnKeyMultipleCalled;
    public boolean isOnSaveInstanceStateCalled;
    public static boolean isOnRestoreInstanceStateCalled;
    public boolean isOnWindowAttributesChangedCalled;
    public boolean isOnCreatePanelMenuCalled;
    public boolean isOnCreatePanelViewCalled;
    public boolean isOnPreparePanelCalled;
    public boolean isOnMenuOpenedCalled;
    public boolean isOnMenuItemSelectedCalled;
    public boolean isOnPanelClosedCalled;
    public boolean isOnCreateOptionsMenuCalled;
    public boolean isOnPrepareOptionsMenuCalled;
    public boolean isOnOptionsItemSelectedCalled;
    public boolean isOnOptionsMenuClosedCalled;
    public boolean isOnContextItemSelectedCalled;
    public boolean isOnContextMenuClosedCalled;
    public boolean isOnCreateContextMenuCalled;
    public boolean isOnSearchRequestedCalled;
    public boolean onKeyDownReturn;
    public boolean onKeyMultipleReturn;
    public boolean dispatchTouchEventResult;
    public boolean dispatchKeyEventResult;
    public int keyDownCode = -1;
    public Window window;
    public Bundle saveInstanceState;
    public Bundle savedInstanceState;
    public KeyEvent keyEvent;
    public MotionEvent touchEvent;
    public MotionEvent trackballEvent;
    public MotionEvent onTrackballEvent;
    public MotionEvent onTouchEvent;
    public KeyEvent keyMultipleEvent;

    public TestDialog(Context context) {
        super(context);
    }

    public TestDialog(Context context, int theme) {
        super(context, theme);
    }

    public TestDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        isOnStartCalled = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isOnStopCalled = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isRequestWindowFeature = requestWindowFeature(Window.FEATURE_LEFT_ICON);
        super.onCreate(savedInstanceState);
        isOnCreateCalled = true;
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();

        isOnContentChangedCalled = true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        isOnWindowFocusChangedCalled = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        isOnTouchEventCalled = true;
        onTouchEvent = event;
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        isOnTrackballEventCalled = true;
        onTrackballEvent = event;
        return super.onTrackballEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        isOnKeyDownCalled = true;
        keyDownCode = keyCode;
        onKeyDownReturn = super.onKeyDown(keyCode, event);

        return onKeyDownReturn;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        isOnKeyUpCalled = true;

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        isOnKeyMultipleCalled = true;
        onKeyMultipleReturn = super.onKeyMultiple(keyCode, repeatCount, event);
        keyMultipleEvent = event;
        return onKeyMultipleReturn;
    }

    @Override
    public Bundle onSaveInstanceState() {
        isOnSaveInstanceStateCalled = true;
        saveInstanceState = super.onSaveInstanceState();
        return saveInstanceState;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        isOnRestoreInstanceStateCalled = true;
        this.savedInstanceState = savedInstanceState;

        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onWindowAttributesChanged(LayoutParams params) {
        isOnWindowAttributesChangedCalled = true;
        super.onWindowAttributesChanged(params);
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        isOnCreatePanelMenuCalled = true;
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public View onCreatePanelView(int featureId) {
        isOnCreatePanelViewCalled = true;
        return super.onCreatePanelView(featureId);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        isOnPreparePanelCalled = true;
        return super.onPreparePanel(featureId, view, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        isOnMenuItemSelectedCalled = true;
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        isOnMenuOpenedCalled = true;
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        isOnPanelClosedCalled = true;
        super.onPanelClosed(featureId, menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        isOnPrepareOptionsMenuCalled = true;
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, OPTIONS_MENU_ITEM_0, 0, "OptionsMenuItem0");
        menu.add(0, OPTIONS_MENU_ITEM_1, 0, "OptionsMenuItem1");
        menu.add(0, OPTIONS_MENU_ITEM_2, 0, "OptionsMenuItem2");
        menu.add(0, OPTIONS_MENU_ITEM_3, 0, "OptionsMenuItem3");
        menu.add(0, OPTIONS_MENU_ITEM_4, 0, "OptionsMenuItem4");
        menu.add(0, OPTIONS_MENU_ITEM_5, 0, "OptionsMenuItem5");
        menu.add(0, OPTIONS_MENU_ITEM_6, 0, "OptionsMenuItem6");
        isOnCreateOptionsMenuCalled = true;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        isOnOptionsItemSelectedCalled = true;
        switch (item.getItemId()) {
            case OPTIONS_MENU_ITEM_0:
            case OPTIONS_MENU_ITEM_1:
            case OPTIONS_MENU_ITEM_2:
            case OPTIONS_MENU_ITEM_3:
            case OPTIONS_MENU_ITEM_4:
            case OPTIONS_MENU_ITEM_5:
            case OPTIONS_MENU_ITEM_6:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        isOnOptionsMenuClosedCalled = true;
        super.onOptionsMenuClosed(menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        isOnContextItemSelectedCalled = true;
        switch (item.getItemId()) {
            case CONTEXT_MENU_ITEM_0:
            case CONTEXT_MENU_ITEM_1:
            case CONTEXT_MENU_ITEM_2:
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        isOnContextMenuClosedCalled = true;
        super.onContextMenuClosed(menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(0, CONTEXT_MENU_ITEM_0, 0, "ContextMenuItem0");
        menu.add(0, CONTEXT_MENU_ITEM_1, 0, "ContextMenuItem1");
        menu.add(0, CONTEXT_MENU_ITEM_2, 0, "ContextMenuItem2");
        isOnCreateContextMenuCalled = true;
    }

    @Override
    public boolean onSearchRequested() {
        isOnSearchRequestedCalled = true;
        return super.onSearchRequested();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        keyEvent = event;
        dispatchKeyEventResult = super.dispatchKeyEvent(event);
        return dispatchKeyEventResult;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        trackballEvent = ev;
        return super.dispatchTrackballEvent(ev);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        touchEvent = ev;
        dispatchTouchEventResult = super.dispatchTouchEvent(ev);
        return dispatchTouchEventResult;
    }
}
