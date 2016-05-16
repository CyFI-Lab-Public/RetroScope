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

package signature.model.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import signature.model.IAnnotatableElement;
import signature.model.IAnnotation;

@SuppressWarnings("serial")
public abstract class SigAnnotatableElement implements IAnnotatableElement,
        Serializable {
    private Set<IAnnotation> annotations;

    public SigAnnotatableElement() {
        annotations = Collections.emptySet();
    }

    public Set<IAnnotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Set<IAnnotation> annotations) {
        this.annotations = annotations;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (IAnnotation annotation : getAnnotations()) {
            builder.append(annotation);
        }
        return builder.toString();
    }
}
