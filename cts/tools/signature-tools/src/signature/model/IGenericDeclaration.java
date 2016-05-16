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

/**
 * {@code IGenericDeclaration} is the common base interface for elements which
 * can define type variables.
 */
public interface IGenericDeclaration {

    /**
     * Returns a list of all defined type variables on this generic declaration.
     * 
     * @return a list of all defined type variables
     */
    List<ITypeVariableDefinition> getTypeParameters();

    /**
     * Returns the class definition which declares this element or {@code null}
     * if this declaration is a top level class definition.
     * 
     * @return the class definition which declares this element or {@code null}
     */
    IClassDefinition getDeclaringClass();
}
