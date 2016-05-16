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
 * Interface for data consumers.
 *
 * @param <A> The type of data to consume.
 */
public interface Consumer<A> {

    /**
     * Consumes a value.
     *
     * @param value The value to consume.
     * @return {@code true} if the value was accepted, {@code false} otherwise.
     */
    boolean consume(A value);

}
