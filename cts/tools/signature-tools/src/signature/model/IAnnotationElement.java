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

/**
 * {@code IAnnotationElement} models an annotation element which consist of a
 * name and a value.
 */
public interface IAnnotationElement {
    /**
     * Returns the value of this annotation element. The type of this value is
     * restricted to the possible value types for annotation elements.
     * <p>
     * The following types are possible:
     * <ul>
     * <li>a wrapper class for a primitive type
     * <li>String (for String values)
     * <li>IType (representing a class literal) FIXME Reference? Def?
     * <li>IEnumConstant (representing an enum constant)
     * <li>IAnnotation
     * </ul>
     * and (one-dimensional) arrays of the above types.
     * 
     * @return the value of this annotation element
     */
    Object getValue();

    /**
     * Returns the corresponding annotation field declaration. This declaration
     * contains e.g. the name of this element, its type and its modifiers. The
     * declaration also contains the default value of this element which is
     * overwritten by this annotation element.
     * 
     * @return the corresponding annotation field declaration
     */
    IAnnotationField getDeclaringField();

}
