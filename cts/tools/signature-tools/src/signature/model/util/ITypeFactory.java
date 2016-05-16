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

package signature.model.util;

import signature.model.IClassReference;
import signature.model.IGenericDeclaration;
import signature.model.ITypeReference;
import signature.model.ITypeVariableReference;
import signature.model.impl.SigArrayType;
import signature.model.impl.SigClassDefinition;
import signature.model.impl.SigParameterizedType;
import signature.model.impl.SigTypeVariableDefinition;
import signature.model.impl.SigWildcardType;

import java.util.List;

public interface ITypeFactory {

    public static final String JAVA_LANG_OBJECT = "java.lang.Object";

    /**
     * Returns the existing type or creates a new one.<br>
     * Format: java.lang.Object
     */
    public SigClassDefinition getClass(String packageName, String className);

    public IClassReference getClassReference(String packageName,
            String className);

    /**
     * Returns the existing array type or creates a new one.
     * 
     * @param componentType
     *            the component type of the array
     * @return the array type
     */
    public SigArrayType getArrayType(ITypeReference componentType);

    /**
     * Returns the existing parameterized type or creates a new one.
     * 
     * @param ownerType
     *            the owner of the parameterized type
     * @param rawType
     *            the type which is parameterized
     * @param typeArguments
     *            the type arguments
     * @return the parameterized type
     */
    public SigParameterizedType getParameterizedType(ITypeReference ownerType,
            IClassReference rawType, List<ITypeReference> typeArguments);


    public boolean containsTypeVariableDefinition(String name,
            IGenericDeclaration genericDeclaration);

    /**
     * Returns the existing type variable or creates a new one.
     * 
     * @param genericDeclaration
     *            the declaration site of the variable
     * @param name
     *            the name of the type variable
     * @return the type variable
     */
    public SigTypeVariableDefinition getTypeVariable(String name,
            IGenericDeclaration genericDeclaration);

    public ITypeVariableReference getTypeVariableReference(String name,
            IGenericDeclaration genericDeclaration);

    /**
     * Returns the existing wildcard type or creates a new one. Wildcard types
     * are equal if they have the same lower bound and have the same upper
     * bounds. The order of the upper bounds is irrelevant except for the first
     * element. <br>
     * Note: This does not mean that two values with equal wildcard type can be
     * assigned to each other!
     * 
     * @param lowerBound
     *            the lower bound
     * @param upperBounds
     *            the upper bounds
     * @return the wildcard type
     */
    public SigWildcardType getWildcardType(ITypeReference lowerBound,
            List<ITypeReference> upperBounds);

}
