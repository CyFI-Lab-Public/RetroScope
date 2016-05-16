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
 * {@code IAnnotationField} models a field in a annotation definition. The
 * following example shows an annotation field <code>androField</code> of type
 * String and the default value "droid".
 * 
 * <pre>
 * 
 * @interface A { String androField() default "droid"; }
 * 
 *            </pre>
 */
public interface IAnnotationField extends IField {

    /**
     * Returns the default value. If no default value is set then null is
     * returned.
     * 
     * The type of the returned object is one of the following:
     * <ul>
     * <li>a wrapper class for a primitive type
     * <li>String (for String values)
     * <li>IType (representing a class literal)
     * <li>IEnumConstant (representing an enum constant)
     * <li>IAnnotation
     * </ul>
     * and (one-dimensional) arrays of the above types. If an array is returned,
     * then the type of the result is Object[]. The elements of this array are
     * of the above listed types.
     */
    Object getDefaultValue();
}
