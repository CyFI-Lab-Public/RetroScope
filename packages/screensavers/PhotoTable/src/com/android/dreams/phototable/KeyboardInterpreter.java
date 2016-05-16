/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.dreams.phototable;

import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

/**
 * Keyboard event dispatcher for Photo Table.
 */
public class KeyboardInterpreter {
    private static final String TAG = "DPadInterpreter";
    private static final boolean DEBUG = false;

    private final PhotoTable mTable;
    private final long mBounce;
    private long mLastDeckNavigation;

    public KeyboardInterpreter(PhotoTable table) {
        mBounce = 2000; // TODO: remove this once latencies in lower layers are removed.
        mTable = table;
    }
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        final View focus = mTable.getFocus();
        boolean consumed = true;
        if (mTable.hasSelection()) {
            switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ESCAPE:
                mTable.setFocus(mTable.getSelection());
                mTable.clearSelection();
                break;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_L:
                if ((System.currentTimeMillis() - mLastDeckNavigation) > mBounce) {
                    mLastDeckNavigation = System.currentTimeMillis();
                    mTable.selectPrevious();
                }
                break;

            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_H:
                if ((System.currentTimeMillis() - mLastDeckNavigation) > mBounce) {
                    mLastDeckNavigation = System.currentTimeMillis();
                    mTable.selectNext();
                }
                break;

            default:
                if (DEBUG) Log.d(TAG, "dropped unexpected: " + keyCode);
                consumed = false;
                // give the user some more time to figure it out
                mTable.refreshSelection();
                break;
            }
        } else {
            switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (mTable.hasFocus()) {
                    mTable.setSelection(mTable.getFocus());
                    mTable.clearFocus();
                } else {
                    mTable.setDefaultFocus();
                }
                break;

            case KeyEvent.KEYCODE_DEL:
            case KeyEvent.KEYCODE_X:
                if (mTable.hasFocus()) {
                    mTable.fling(mTable.getFocus());
                }
                break;

            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_K:
                mTable.moveFocus(focus, 0f);
                break;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_L:
                mTable.moveFocus(focus, 90f);
                break;

            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_J:
                mTable.moveFocus(focus, 180f);
                break;

            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_H:
                mTable.moveFocus(focus, 270f);
                break;

            default:
                if (DEBUG) Log.d(TAG, "dropped unexpected: " + keyCode);
                consumed = false;
                // give the user some more time to figure it out
                mTable.refreshFocus();
                break;
            }
        }

        return consumed;
    }
}
