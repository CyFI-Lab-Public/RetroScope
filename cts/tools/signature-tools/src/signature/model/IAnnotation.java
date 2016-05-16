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

import java.util.Set;

/**
 * {@code IAnnotation} models an <em>instance</em> of an annotation which is
 * attached to a program element. The definition of an annotation type is
 * modeled as an {@link IClassDefinition} with the {@link Kind#ANNOTATION}.
 */
public interface IAnnotation {
    /**
     * Returns the annotation type of this annotation.
     * 
     * @return the annotation type of this annotation
     */
    IClassReference getType();

    /**
     * Returns the elements declared in this annotation. The values which are
     * not declared are not contained in this list. Each element consists of its
     * name and its value.
     * 
     * @return elements declared in this annotation
     */
    Set<IAnnotationElement> getElements();
}
