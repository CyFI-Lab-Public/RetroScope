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

import signature.model.IClassDefinition;
import signature.model.IExecutableMember;
import signature.model.IParameter;
import signature.model.ITypeReference;
import signature.model.ITypeVariableDefinition;
import signature.model.Modifier;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@SuppressWarnings("serial")
public abstract class SigExecutableMember extends SigAnnotatableElement
        implements IExecutableMember, Serializable {

    private String name;
    private List<IParameter> parameters = Uninitialized.unset();
    private Set<ITypeReference> exceptions = Uninitialized.unset();
    private Set<Modifier> modifiers = Uninitialized.unset();
    private List<ITypeVariableDefinition> typeParameters = Uninitialized
            .unset();
    private IClassDefinition declaringClass = Uninitialized.unset();

    public SigExecutableMember(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<IParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<IParameter> parameters) {
        this.parameters = parameters;
    }

    public Set<ITypeReference> getExceptions() {
        return exceptions;
    }

    public void setExceptions(Set<ITypeReference> exceptions) {
        this.exceptions = exceptions;
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    public void setModifiers(Set<Modifier> modifiers) {
        this.modifiers = modifiers;
    }

    public List<ITypeVariableDefinition> getTypeParameters() {
        return typeParameters;
    }

    public void setTypeParameters(
            List<ITypeVariableDefinition> typeParameters) {
        this.typeParameters = typeParameters;
    }

    public IClassDefinition getDeclaringClass() {
        return declaringClass;
    }

    public void setDeclaringClass(IClassDefinition declaringClass) {
        this.declaringClass = declaringClass;
    }
}
