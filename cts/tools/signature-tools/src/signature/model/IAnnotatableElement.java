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
 * Common interface for model elements which may be annotated. Annotations can
 * be attached to:
 * <ul>
 * <li>Classes (IClass), including annotations which are also classes
 * <li>Methods and Constructors (IExecutableMember)
 * <li>Fields (IField), including the annotation fields and enum constants
 * <li>Parameters (IParameter)
 * </ul>
 */
public interface IAnnotatableElement {

    /**
     * Returns a set of annotations of a model element.
     * 
     * @return a set of annotations of a model element
     */
    Set<IAnnotation> getAnnotations();
}
