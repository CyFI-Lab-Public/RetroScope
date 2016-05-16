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
import signature.compare.model.IAnnotationFieldDelta;
import signature.compare.model.IClassDefinitionDelta;
import signature.compare.model.IConstructorDelta;
import signature.compare.model.IEnumConstantDelta;
import signature.compare.model.IFieldDelta;
import signature.compare.model.IMethodDelta;
import signature.compare.model.IModifierDelta;
import signature.compare.model.ITypeReferenceDelta;
import signature.compare.model.ITypeVariableDefinitionDelta;
import signature.model.IClassDefinition;

import java.util.Set;

public class SigClassDefinitionDelta extends
        SigTypeDefinitionDelta<IClassDefinition> implements
        IClassDefinitionDelta {

    public SigClassDefinitionDelta(IClassDefinition from, IClassDefinition to) {
        super(from, to);
    }

    private Set<IAnnotationFieldDelta> annotationFieldDeltas;
    private Set<IConstructorDelta> constructorDeltas;
    private Set<IEnumConstantDelta> enumConstantDeltas;
    private Set<IFieldDelta> fieldDeltas;
    private Set<ITypeReferenceDelta<?>> interfaceDeltas;
    private Set<IMethodDelta> methodDeltas;
    private Set<IModifierDelta> modifierDeltas;
    private ITypeReferenceDelta<?> superClassDelta;
    private Set<IAnnotationDelta> annotationDeltas;
    private Set<ITypeVariableDefinitionDelta> typeVariableDeltas;

    public Set<IAnnotationFieldDelta> getAnnotationFieldDeltas() {
        return annotationFieldDeltas;
    }

    public void setAnnotationFieldDeltas(
            Set<IAnnotationFieldDelta> annotationFieldDeltas) {
        this.annotationFieldDeltas = annotationFieldDeltas;
    }

    public Set<IConstructorDelta> getConstructorDeltas() {
        return constructorDeltas;
    }

    public void setConstructorDeltas(
            Set<IConstructorDelta> constructorDeltas) {
        this.constructorDeltas = constructorDeltas;
    }

    public Set<IEnumConstantDelta> getEnumConstantDeltas() {
        return enumConstantDeltas;
    }

    public void setEnumConstantDeltas(
            Set<IEnumConstantDelta> enumConstantDeltas) {
        this.enumConstantDeltas = enumConstantDeltas;
    }

    public Set<IFieldDelta> getFieldDeltas() {
        return fieldDeltas;
    }

    public void setFieldDeltas(Set<IFieldDelta> fieldDeltas) {
        this.fieldDeltas = fieldDeltas;
    }

    public Set<ITypeReferenceDelta<?>> getInterfaceDeltas() {
        return interfaceDeltas;
    }

    public void setInterfaceDeltas(
            Set<ITypeReferenceDelta<?>> interfaceDeltas) {
        this.interfaceDeltas = interfaceDeltas;
    }

    public Set<IMethodDelta> getMethodDeltas() {
        return methodDeltas;
    }

    public void setMethodDeltas(Set<IMethodDelta> methodDeltas) {
        this.methodDeltas = methodDeltas;
    }

    public Set<IModifierDelta> getModifierDeltas() {
        return modifierDeltas;
    }

    public void setModifierDeltas(Set<IModifierDelta> modifierDeltas) {
        this.modifierDeltas = modifierDeltas;
    }

    public ITypeReferenceDelta<?> getSuperClassDelta() {
        return superClassDelta;
    }

    public void setSuperClassDelta(ITypeReferenceDelta<?> superClassDelta) {
        this.superClassDelta = superClassDelta;
    }

    public Set<IAnnotationDelta> getAnnotationDeltas() {
        return annotationDeltas;
    }

    public void setAnnotationDeltas(Set<IAnnotationDelta> annotationDeltas) {
        this.annotationDeltas = annotationDeltas;
    }

    public Set<ITypeVariableDefinitionDelta> getTypeVariableDeltas() {
        return typeVariableDeltas;
    }

    public void setTypeVariableDeltas(
            Set<ITypeVariableDefinitionDelta> typeVariableDeltas) {
        this.typeVariableDeltas = typeVariableDeltas;
    }
}
