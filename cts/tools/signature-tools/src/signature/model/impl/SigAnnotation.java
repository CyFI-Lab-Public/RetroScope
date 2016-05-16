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

import signature.model.IAnnotation;
import signature.model.IAnnotationElement;
import signature.model.IClassReference;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

@SuppressWarnings("serial")
public class SigAnnotation implements IAnnotation, Serializable {

    private Set<IAnnotationElement> elements;
    private IClassReference type;

    public SigAnnotation() {
        elements = Collections.emptySet();
    }

    public IClassReference getType() {
        return type;
    }

    public void setType(IClassReference type) {
        this.type = type;
    }

    public Set<IAnnotationElement> getElements() {
        return elements;
    }

    public void setElements(Set<IAnnotationElement> elements) {
        this.elements = elements;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("@");
        builder.append(getType());
        if (!getElements().isEmpty()) {
            builder.append("{");
            for (IAnnotationElement element : getElements()) {
                builder.append("\n");
                builder.append(element.toString());
            }
            builder.append("}");
        }
        return builder.toString();
    }
}
