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
 * {@code ITypeVariableDefinition} models a type variable definition.
 */
public interface ITypeVariableDefinition extends ITypeDefinition {

    /**
     * Returns the upper bounds for this type variable as specified by the
     * extends clause. If no upper bounds are explicitly specified then
     * java.lang.Object is returned as upper bound.
     * 
     * @return the upper bounds for this type variable
     */
    List<ITypeReference> getUpperBounds();

    /**
     * Returns the name of this type variable.
     * 
     * @return the name of this type variable
     */
    String getName();

    /**
     * Returns the element on which this type variable is declared.
     * 
     * @return the element on which this type variable is declared
     */
    IGenericDeclaration getGenericDeclaration();
}
