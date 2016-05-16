/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.quicksearchbox.util;

/**
 * A {@link NowOrLater} object that is always ready now.
 */
public class Now<C> implements NowOrLater<C> {

    private final C mValue;

    public Now(C value) {
        mValue = value;
    }

    public void getLater(Consumer<? super C> consumer) {
        consumer.consume(getNow());
    }

    public C getNow() {
        return mValue;
    }

    public boolean haveNow() {
        return true;
    }

}
