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

import android.database.Cursor;

/**
 * An object that knows how to create its implementing class using a single row of a cursor alone.
 * @param <T>
 */
public interface CursorCreator<T> {

    /**
     * Creates an object using the current row of the cursor given here. The implementation should
     * not advance/rewind the cursor, and is only allowed to read the existing row.
     * @param c
     * @return a real object of the implementing class.
     */
    T createFromCursor(Cursor c);
}
