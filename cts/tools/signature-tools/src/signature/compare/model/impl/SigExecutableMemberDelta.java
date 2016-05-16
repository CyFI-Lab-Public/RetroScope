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

package signature.compare.model.impl;

import signature.compare.model.IAnnotationDelta;
import signature.compare.model.IExecutableMemberDelta;
import signature.compare.model.IModifierDelta;
import signature.compare.model.IParameterDelta;
import signature.compare.model.ITypeReferenceDelta;
import signature.compare.model.ITypeVariableDefinitionDelta;
import signature.model.IExecutableMember;

import java.util.Set;

public abstract class SigExecutableMemberDelta<T extends IExecutableMember>
        extends SigDelta<T> implements IExecutableMemberDelta<T> {

    private Set<ITypeReferenceDelta<?>> exceptionDeltas;
    private Set<IModifierDelta> modifierDeltas;
    private Set<ITypeVariableDefinitionDelta> typeVariableDeltas;
    private Set<IAnnotationDelta> annotationDeltas;
    private Set<IParameterDelta> parameterDeltas;

    public SigExecutableMemberDelta(T from, T to) {
        super(from, to);
    }

    public Set<ITypeReferenceDelta<?>> getExceptionDeltas() {
        return exceptionDeltas;
    }

    public void setExceptionDeltas(
            Set<ITypeReferenceDelta<?>> exceptionDeltas) {
        this.exceptionDeltas = exceptionDeltas;
    }

    public Set<IModifierDelta> getModifierDeltas() {
        return modifierDeltas;
    }

    public void setModifierDeltas(Set<IModifierDelta> modifierDeltas) {
        this.modifierDeltas = modifierDeltas;
    }

    public Set<ITypeVariableDefinitionDelta> getTypeVariableDeltas() {
        return typeVariableDeltas;
    }

    public void setTypeVariableDeltas(
            Set<ITypeVariableDefinitionDelta> typeVariableDeltas) {
        this.typeVariableDeltas = typeVariableDeltas;
    }

    public Set<IAnnotationDelta> getAnnotationDeltas() {
        return annotationDeltas;
    }

    public void setAnnotationDeltas(Set<IAnnotationDelta> annotationDeltas) {
        this.annotationDeltas = annotationDeltas;
    }

    public Set<IParameterDelta> getParameterDeltas() {
        return parameterDeltas;
    }

    public void setParameterDeltas(Set<IParameterDelta> parameterDeltas) {
        this.parameterDeltas = parameterDeltas;
    }
}
