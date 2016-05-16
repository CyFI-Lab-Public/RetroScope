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

package dex.structure;

/**
 * {@code DexEncodedValue} represents an encoded value. The value of a {@code
 * DexAnnotationValue} is encoded as {@code DexEncodedValue}.
 */
public interface DexEncodedValue {

    /**
     * Returns the type of this {@code DexEncodedValue}.
     * 
     * @return the type of this {@code DexEncodedValue}
     */
    DexEncodedValueType getType();

    /**
     * Returns the value of this {@code DexEncodedValue}.
     * 
     * @return the value of this {@code DexEncodedValue}
     */
    Object getValue();
}
