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
package com.android.mail.content;

import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;

import com.android.mail.providers.UIProvider;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

public class ThreadSafeCursorWrapper extends CursorWrapper {
    private static final String LOG_TAG = LogTag.getLogTag();

    private final ThreadLocal<Integer> mPosition;
    private final Object mLock = new Object();

    public ThreadSafeCursorWrapper(Cursor cursor) {
        super(cursor);

        mPosition = new ThreadLocal<Integer>() {
            @Override
            protected Integer initialValue() {
                return Integer.valueOf(-1);
            }
        };
    }

    @Override
    public String getString(int column) {
        synchronized (mLock) {
            moveToCurrent();
            return super.getString(column);
        }
    }

    @Override
    public short getShort(int column) {
        synchronized (mLock) {
            moveToCurrent();
            return super.getShort(column);
        }
    }

    @Override
    public int getInt(int column) {
        synchronized (mLock) {
            moveToCurrent();
            return super.getInt(column);
        }
    }

    @Override
    public long getLong(int column) {
        synchronized (mLock) {
            moveToCurrent();
            return super.getLong(column);
        }
    }

    @Override
    public float getFloat(int column) {
        synchronized (mLock) {
            moveToCurrent();
            return super.getFloat(column);
        }
    }

    @Override
    public double getDouble(int column) {
        synchronized (mLock) {
            moveToCurrent();
            return super.getDouble(column);
        }
    }

    @Override
    public byte[] getBlob(int column) {
        synchronized (mLock) {
            moveToCurrent();
            return super.getBlob(column);
        }
    }

    @Override
    public Bundle respond(Bundle extras) {
        final int opts = extras.getInt(UIProvider.ConversationCursorCommand.COMMAND_KEY_OPTIONS);
        if ((opts & UIProvider.ConversationCursorCommand.OPTION_MOVE_POSITION) != 0) {
            synchronized (mLock) {
                moveToCurrent();
                return super.respond(extras);
            }
        } else {
            return super.respond(extras);
        }
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        synchronized (mLock) {
            moveToCurrent();
            super.copyStringToBuffer(columnIndex, buffer);
        }
    }

    @Override
    public boolean isNull(int column){
        synchronized (mLock) {
            moveToCurrent();
            return super.isNull(column);
        }
    }

    private void moveToCurrent() {
        final int pos = mPosition.get();
        final boolean result = super.moveToPosition(pos);

        // AbstractCursor returns false on negative positions, although Cursor documentation
        // states that -1 is a valid input. Let's just log positive values as failures.
        if (!result && pos >= 0) {
            LogUtils.e(LOG_TAG, "Unexpected failure to move to current position, pos=%d", pos);
        }
    }

    @Override
    public boolean move(int offset) {
        final int curPos = mPosition.get();
        return moveToPosition(curPos + offset);
    }

    @Override
    public boolean moveToFirst() {
        return moveToPosition(0);
    }

    @Override
    public boolean moveToLast() {
        return moveToPosition(getCount() - 1);
    }

    @Override
    public boolean moveToNext() {
        final int curPos = mPosition.get();
        return moveToPosition(curPos + 1);
    }

    @Override
    public boolean moveToPosition(int position) {
        // Make sure position isn't past the end of the cursor
        final int count = getCount();
        if (position >= count) {
            mPosition.set(count);
            return false;
        }

        // Make sure position isn't before the beginning of the cursor
        if (position < 0) {
            mPosition.set(-1);
            return false;
        }

        final int curPos = mPosition.get();
        // Check for no-op moves, and skip the rest of the work for them
        if (position == curPos) {
            return true;
        }

        // Save this thread's current position.
        mPosition.set(position);
        return true;
    }

    @Override
    public boolean moveToPrevious() {
        final int curPos = mPosition.get();
        return moveToPosition(curPos - 1);
    }

    @Override
    public int getPosition() {
        return mPosition.get();
    }
}
