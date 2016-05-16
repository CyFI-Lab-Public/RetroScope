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

package signature.model;

import signature.converter.Visibility;

import java.util.Set;

/**
 * {@code IApi} models the root of an api.
 */
public interface IApi {
    /**
     * Returns the name of this api.
     * 
     * @return the name of this api
     */
    String getName();

    /**
     * Sets the name of this api.
     * 
     * @param name
     *            the name of this api
     */
    void setName(String name);

    /**
     * Returns the set of packages which constitute this api.
     * 
     * @return the set of packages which constitute this api
     */
    Set<IPackage> getPackages();

    /**
     * Returns the visibility of this api. The visibility describes which
     * elements are visible. Only elements with a visibility greater or equal
     * this visibility are contained in this api model.
     * 
     * @return the visibility of this api
     * @see Visibility
     */
    Visibility getVisibility();
}
