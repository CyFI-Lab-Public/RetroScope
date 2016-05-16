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

package signature.compare.model;

/**
 * {@code IDelta} is the common base interface for all delta model elements. It
 * describes a delta from a "from" element to a "to" element.
 * 
 * @param <T>
 *            the type of the compared elements
 */
public interface IDelta<T> {

    /**
     * Returns the type of this delta.
     * 
     * @return the type of this delta
     */
    DeltaType getType();

    /**
     * Returns the "from" element. Is null if type is {@link DeltaType#ADDED}
     * 
     * @return the "from" element
     */
    T getFrom();

    /**
     * Returns the "to" element. Is null if type is {@link DeltaType#REMOVED}
     * 
     * @return the "to" element
     */
    T getTo();
}
