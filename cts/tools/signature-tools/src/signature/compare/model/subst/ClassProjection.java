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

package signature.compare.model.subst;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import signature.model.IAnnotation;
import signature.model.IAnnotationField;
import signature.model.IClassDefinition;
import signature.model.IConstructor;
import signature.model.IEnumConstant;
import signature.model.IField;
import signature.model.IMethod;
import signature.model.ITypeReference;
import signature.model.ITypeVariableDefinition;
import signature.model.Kind;
import signature.model.Modifier;
import signature.model.impl.SigClassDefinition;

public class ClassProjection implements IClassDefinition {

    private final IClassDefinition original;
    private final Map<ITypeVariableDefinition, ITypeReference> substitutions;

    public ClassProjection(IClassDefinition original,
            Map<ITypeVariableDefinition, ITypeReference> mapping) {
        this.original = original;
        this.substitutions = mapping;
    }

    public Set<IAnnotationField> getAnnotationFields() {
        throw new UnsupportedOperationException();
    }

    public Set<IAnnotation> getAnnotations() {
        throw new UnsupportedOperationException();
    }

    public Set<IConstructor> getConstructors() {
        throw new UnsupportedOperationException();
    }

    public IClassDefinition getDeclaringClass() {
        throw new UnsupportedOperationException();
    }

    public Set<IEnumConstant> getEnumConstants() {
        throw new UnsupportedOperationException();
    }

    public Set<IField> getFields() {
        throw new UnsupportedOperationException();
    }

    public Set<IClassDefinition> getInnerClasses() {
        throw new UnsupportedOperationException();
    }

    Set<ITypeReference> interfaces = null;

    public Set<ITypeReference> getInterfaces() {
        if (interfaces == null) {
            Set<ITypeReference> originalInterfaces = original.getInterfaces();
            if (originalInterfaces == null) {
                interfaces = Collections.emptySet();
            } else {
                interfaces = new HashSet<ITypeReference>();
                for (ITypeReference interfaze : originalInterfaces) {
                    interfaces.add(ViewpointAdapter.substitutedTypeReference(
                            interfaze, substitutions));
                }
                interfaces = Collections.unmodifiableSet(interfaces);
            }
        }
        return interfaces;
    }

    public Kind getKind() {
        return original.getKind();
    }


    Set<IMethod> methods = null;

    public Set<IMethod> getMethods() {
        if (methods == null) {
            Set<IMethod> originalMethods = original.getMethods();
            if (originalMethods == null) {
                methods = Collections.emptySet();
            } else {
                methods = new HashSet<IMethod>();
                for (IMethod m : original.getMethods()) {
                    methods.add(new MethodProjection(m, substitutions));
                }
                methods = Collections.unmodifiableSet(methods);
            }
        }
        return methods;
    }

    public Set<Modifier> getModifiers() {
        return original.getModifiers();
    }

    public String getName() {
        return original.getName();
    }

    public List<String> getPackageFragments() {
        return original.getPackageFragments();
    }

    public String getPackageName() {
        return original.getPackageName();
    }

    public String getQualifiedName() {
        return original.getQualifiedName();
    }

    private boolean superClassInit = false;
    private ITypeReference superClass = null;

    public ITypeReference getSuperClass() {
        if (!superClassInit) {
            ITypeReference originalSuperClass = original.getSuperClass();
            if (originalSuperClass != null) {
                superClass = ViewpointAdapter.substitutedTypeReference(original
                        .getSuperClass(), substitutions);
            }
            superClassInit = true;
        }
        return superClass;
    }

    // Definitions of type variables are not substituted
    public List<ITypeVariableDefinition> getTypeParameters() {
        return original.getTypeParameters();
    }

    @Override
    public int hashCode() {
        return SigClassDefinition.hashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return SigClassDefinition.equals(this, obj);
    }

    @Override
    public String toString() {
        return "(" + SigClassDefinition.toString(this) + " : " + substitutions
                + " )";
    }

}
