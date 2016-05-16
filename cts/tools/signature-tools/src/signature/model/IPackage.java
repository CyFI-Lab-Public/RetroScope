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

import java.util.List;
import java.util.Set;

/**
 * {@code IPackage} models a package.
 */
public interface IPackage extends IAnnotatableElement {

    /**
     * Returns the name of this package.
     * 
     * @return the name of this package
     */
    String getName();

    /**
     * Returns a list containing each package fragment.
     * <p>
     * If {@link #getName()} returns : "a.b.c" this method returns a list
     * containing the three elements "a", "b", "c".
     * <p>
     * Note: this method exists only for convenience in output templating.
     * 
     * @return a list containing each package fragment
     */
    List<String> getPackageFragments();

    /**
     * Returns all classes declared in this package, including ordinary classes,
     * interfaces, enum types and annotation types. Nested classes are returned
     * as well.
     * 
     * @return all classes declared in this package
     */
    Set<IClassDefinition> getClasses();
}
