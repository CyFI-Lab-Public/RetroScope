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
 * {@code DexClass} represents a class.
 */
public interface DexClass extends DexAnnotatedElement, WithModifiers,
        NamedElement {
    /**
     * Returns a list containing the names of all implemented interfaces.
     * 
     * @return a list containing the names of all implemented interfaces
     */
    List<String> getInterfaces();

    /**
     * Returns the name of the super class.
     * 
     * @return the name of the super class, maybe {@code null}
     */
    String getSuperClass();

    /**
     * Returns a list containing all fields declared by this {@code DexClass}.
     * 
     * @return a list containing all fields declared by this {@code DexClass}
     */
    List<DexField> getFields();

    /**
     * Returns a list containing all methods declared by this {@code DexClass}.
     * 
     * @return a list containing all methods declared by this {@code DexClass}
     */
    List<DexMethod> getMethods();
}
