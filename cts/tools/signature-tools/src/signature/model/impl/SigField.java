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

import signature.model.IField;
import signature.model.ITypeReference;
import signature.model.Modifier;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

@SuppressWarnings("serial")
public class SigField extends SigAnnotatableElement implements IField,
        Serializable {

    private String name;
    private ITypeReference type = Uninitialized.unset();
    private Set<Modifier> modifiers = Uninitialized.unset();

    public SigField(String name) {
        this.name = name;
        modifiers = Collections.emptySet();
    }

    public String getName() {
        return name;
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    public void setModifiers(Set<Modifier> modifiers) {
        this.modifiers = modifiers;
    }

    public ITypeReference getType() {
        return type;
    }

    public void setType(ITypeReference type) {
        this.type = type;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (getAnnotations() != null && !getAnnotations().isEmpty()) {
            builder.append(super.toString());
            builder.append("\n");
        }
        builder.append(Modifier.toString(getModifiers()));
        builder.append(getType().toString());
        builder.append(" ");
        builder.append(getName());
        return builder.toString();
    }
}
