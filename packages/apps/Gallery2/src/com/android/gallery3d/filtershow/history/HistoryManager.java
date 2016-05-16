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

package com.android.gallery3d.filtershow.history;

import android.graphics.drawable.Drawable;
import android.view.MenuItem;

import java.util.Vector;

public class HistoryManager {
    private static final String LOGTAG = "HistoryManager";

    private Vector<HistoryItem> mHistoryItems = new Vector<HistoryItem>();
    private int mCurrentPresetPosition = 0;
    private MenuItem mUndoMenuItem = null;
    private MenuItem mRedoMenuItem = null;
    private MenuItem mResetMenuItem = null;

    public void setMenuItems(MenuItem undoItem, MenuItem redoItem, MenuItem resetItem) {
        mUndoMenuItem = undoItem;
        mRedoMenuItem = redoItem;
        mResetMenuItem = resetItem;
        updateMenuItems();
    }

    private int getCount() {
        return mHistoryItems.size();
    }

    public HistoryItem getItem(int position) {
        if (position > mHistoryItems.size() - 1) {
            return null;
        }
        return mHistoryItems.elementAt(position);
    }

    private void clear() {
        mHistoryItems.clear();
    }

    private void add(HistoryItem item) {
        mHistoryItems.add(item);
    }

    private void notifyDataSetChanged() {
        // TODO
    }

    public boolean canReset() {
        if (getCount() <= 0) {
            return false;
        }
        return true;
    }

    public boolean canUndo() {
        if (mCurrentPresetPosition == getCount() - 1) {
            return false;
        }
        return true;
    }

    public boolean canRedo() {
        if (mCurrentPresetPosition == 0) {
            return false;
        }
        return true;
    }

    public void updateMenuItems() {
        if (mUndoMenuItem != null) {
            setEnabled(mUndoMenuItem, canUndo());
        }
        if (mRedoMenuItem != null) {
            setEnabled(mRedoMenuItem, canRedo());
        }
        if (mResetMenuItem != null) {
            setEnabled(mResetMenuItem, canReset());
        }
    }

    private void setEnabled(MenuItem item, boolean enabled) {
        item.setEnabled(enabled);
        Drawable drawable = item.getIcon();
        if (drawable != null) {
            drawable.setAlpha(enabled ? 255 : 80);
        }
    }

    public void setCurrentPreset(int n) {
        mCurrentPresetPosition = n;
        updateMenuItems();
        notifyDataSetChanged();
    }

    public void reset() {
        if (getCount() == 0) {
            return;
        }
        clear();
        updateMenuItems();
    }

    public HistoryItem getLast() {
        if (getCount() == 0) {
            return null;
        }
        return getItem(0);
    }

    public HistoryItem getCurrent() {
        return getItem(mCurrentPresetPosition);
    }

    public void addHistoryItem(HistoryItem preset) {
        insert(preset, 0);
        updateMenuItems();
    }

    private void insert(HistoryItem preset, int position) {
        if (mCurrentPresetPosition != 0) {
            // in this case, let's discount the presets before the current one
            Vector<HistoryItem> oldItems = new Vector<HistoryItem>();
            for (int i = mCurrentPresetPosition; i < getCount(); i++) {
                oldItems.add(getItem(i));
            }
            clear();
            for (int i = 0; i < oldItems.size(); i++) {
                add(oldItems.elementAt(i));
            }
            mCurrentPresetPosition = position;
            notifyDataSetChanged();
        }
        mHistoryItems.insertElementAt(preset, position);
        mCurrentPresetPosition = position;
        notifyDataSetChanged();
    }

    public int redo() {
        mCurrentPresetPosition--;
        if (mCurrentPresetPosition < 0) {
            mCurrentPresetPosition = 0;
        }
        notifyDataSetChanged();
        updateMenuItems();
        return mCurrentPresetPosition;
    }

    public int undo() {
        mCurrentPresetPosition++;
        if (mCurrentPresetPosition >= getCount()) {
            mCurrentPresetPosition = getCount() - 1;
        }
        notifyDataSetChanged();
        updateMenuItems();
        return mCurrentPresetPosition;
    }

}
