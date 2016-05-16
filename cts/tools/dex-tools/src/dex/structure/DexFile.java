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


import java.util.List;

/**
 * {@code DexFile} represents a whole dex file, containing multiple classes.
 */
public interface DexFile extends NamedElement {

    /**
     * Returns a list of {@code DexClass} elements that are part of this {@code
     * DexFile}.
     * 
     * @return a list of {@code DexClass} elements that are part of this {@code
     *         DexFile}
     */
    public List<DexClass> getDefinedClasses();

}
