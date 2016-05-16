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
 * {@code IClassDefinition} models a class definition. This is the model
 * equivalent to a class.
 */
public interface IClassDefinition extends ITypeDefinition, IGenericDeclaration,
        IAnnotatableElement {

    /**
     * Returns the kind of this class definition.
     * 
     * @return the kind of this class definition
     */
    Kind getKind();

    /**
     * Returns the name of this class definition.
     * 
     * @return the name of this class definition
     */
    String getName();

    /**
     * Returns the '.' separated package name of this class.
     * 
     * @return the '.' separated package name of this class
     */
    String getPackageName();

    /**
     * Returns a list containing each package fragment.
     * <p>
     * If {@link #getPackageName()} returns : "a.b.c" this method returns a list
     * containing the three elements "a", "b", "c".
     * <p>
     * Note: this method exists only for convenience in output templating.
     * 
     * @return a list containing each package fragment
     */
    List<String> getPackageFragments();

    /**
     * Returns the qualified name of this class definition. The qualified name
     * is composed of {@link #getPackageName()} '.' {@link #getName()}
     * 
     * @return the qualified name of this class definition
     */
    String getQualifiedName();

    /**
     * Returns the super class for this class definition. May return {@code
     * null} if this class definition does not have any superclass. This is the
     * case if the kind of this class definition is {@link Kind#INTERFACE} or
     * this class definition is {@link Object}.
     * 
     * @return the super class for this class definition or {@code null}
     */
    ITypeReference getSuperClass();

    /**
     * Returns the declared interfaces this class definition implements . If no
     * interfaces are declared, an empty set is returned.
     * 
     * @return the declared interfaces for this class definition
     */
    Set<ITypeReference> getInterfaces();

    /**
     * Returns the modifiers for this class definition.
     * 
     * @return the modifiers for this class definition
     */
    Set<Modifier> getModifiers();

    /**
     * Returns all declared methods of this class definition.
     * 
     * @return all declared methods of this class definition
     */
    Set<IMethod> getMethods();

    /**
     * Returns all declared constructors of this class definition.
     * 
     * @return all declared constructors of this class definition
     */
    Set<IConstructor> getConstructors();

    /**
     * Returns all declared fields of this class definition.
     * 
     * @return all declared fields of this class definition
     */
    Set<IField> getFields();

    /**
     * Returns all declared enumeration constant definitions of this class
     * definition. The returned set may only contain elements if the kind of
     * this class definition is {@link Kind#ENUM}.
     * 
     * @return all declared enumeration constants of this class definition
     */
    Set<IEnumConstant> getEnumConstants();

    /**
     * Returns all declared annotation field definitions of this class
     * definition. The returned set may only contain elements if the kind of
     * this class definition is {@link Kind#ANNOTATION}.
     * 
     * @return all declared annotation fields of this class definition
     */
    Set<IAnnotationField> getAnnotationFields();

    /**
     * Returns all classes which where defined in the lexical scope of this
     * class definition. Anonymous classes are never returned.
     * 
     * @return all classes which where defined in the lexical scope of this
     *         class definition
     */
    Set<IClassDefinition> getInnerClasses();
}
