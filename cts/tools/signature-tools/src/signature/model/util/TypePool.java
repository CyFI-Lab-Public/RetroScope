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

package signature.model.util;

import static signature.model.impl.Uninitialized.isInitialized;
import signature.model.IClassReference;
import signature.model.IGenericDeclaration;
import signature.model.ITypeReference;
import signature.model.ITypeVariableReference;
import signature.model.impl.SigArrayType;
import signature.model.impl.SigClassDefinition;
import signature.model.impl.SigClassReference;
import signature.model.impl.SigParameterizedType;
import signature.model.impl.SigTypeVariableDefinition;
import signature.model.impl.SigTypeVariableReference;
import signature.model.impl.SigWildcardType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pool and factory for all {@link ITypeReference} instances.<br>
 * Note: This class is not thread save
 */
public class TypePool implements ITypeFactory {

    /**
     * Pool for all SigClass objects. Key format: "java.lang.Object", "a.b.C$D
     */
    private Map<String, SigClassDefinition> classPool;
    /** Pool for all SigTypeVariable objects */
    private Map<TypeVariableKey, SigTypeVariableDefinition> typeVariablePool;

    public TypePool() {
        classPool = new HashMap<String, SigClassDefinition>();
        typeVariablePool =
                new HashMap<TypeVariableKey, SigTypeVariableDefinition>();
    }

    public SigClassDefinition getClass(String packageName, String className) {
        String key = packageName + "<>" + className;
        SigClassDefinition clazz = classPool.get(key);
        if (clazz == null) {
            clazz = new SigClassDefinition(packageName, className);
            classPool.put(key, clazz);
        }
        return clazz;
    }

    public IClassReference getClassReference(String packageName,
            String className) {
        return new SigClassReference(getClass(packageName, className));
    }

    public SigArrayType getArrayType(ITypeReference componentType) {
        assert componentType != null;
        return new SigArrayType(componentType);
    }

    public SigParameterizedType getParameterizedType(ITypeReference ownerType,
            IClassReference rawType, List<ITypeReference> typeArguments) {
        assert rawType != null;
        assert typeArguments != null;
        return new SigParameterizedType(ownerType, rawType, typeArguments);
    }

    private static class TypeVariableKey {
        private String name;
        private IGenericDeclaration genericDeclaration;

        public TypeVariableKey(String name,
                IGenericDeclaration genericDeclaration) {
            this.genericDeclaration = genericDeclaration;
            this.name = name;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + genericDeclaration.hashCode();
            result = prime * result + name.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            TypeVariableKey other = (TypeVariableKey) obj;
            if (genericDeclaration != other.genericDeclaration) {
                return false;
            }
            if (!name.equals(other.name)) {
                return false;
            }
            return true;
        }
    }

    public boolean containsTypeVariableDefinition(String name,
            IGenericDeclaration genericDeclaration) {
        TypeVariableKey key = new TypeVariableKey(name, genericDeclaration);
        return typeVariablePool.get(key) != null;
    }

    public SigTypeVariableDefinition getTypeVariable(String name,
            IGenericDeclaration genericDeclaration) {
        assert name != null;
        assert genericDeclaration != null;

        TypeVariableKey key = new TypeVariableKey(name, genericDeclaration);
        SigTypeVariableDefinition sigTypeVariable = typeVariablePool.get(key);
        if (sigTypeVariable == null) {
            sigTypeVariable = new SigTypeVariableDefinition(name,
                    genericDeclaration);
            typeVariablePool.put(key, sigTypeVariable);
        }
        return sigTypeVariable;
    }

    public ITypeVariableReference getTypeVariableReference(String name,
            IGenericDeclaration genericDeclaration) {
        return new SigTypeVariableReference(getTypeVariable(name,
                genericDeclaration));
    }

    public SigWildcardType getWildcardType(ITypeReference lowerBound,
            List<ITypeReference> upperBounds) {
        assert upperBounds != null;

        SigWildcardType sigWildcardType = new SigWildcardType(lowerBound,
                upperBounds);
        return sigWildcardType;
    }

    public void replaceAllUninitialiezWithNull() {
        for (SigClassDefinition clazz : classPool.values()) {
            replaceUninitializedWithNull(clazz);
        }
    }

    private static void replaceUninitializedWithNull(
            SigClassDefinition clazz) {
        if (clazz == null) {
            return;
        }
        if (!isInitialized(clazz.getAnnotationFields())) {
            clazz.setAnnotationFields(null);
        }
        if (!isInitialized(clazz.getAnnotations())) {
            clazz.setAnnotations(null);
        }
        if (!isInitialized(clazz.getAnnotations())) {
            clazz.setAnnotations(null);
        }
        if (!isInitialized(clazz.getConstructors())) {
            clazz.setConstructors(null);
        }
        if (!isInitialized(clazz.getDeclaringClass())) {
            clazz.setDeclaringClass(null);
        }

        if (!isInitialized(clazz.getEnumConstants())) {
            clazz.setEnumConstants(null);
        }
        if (!isInitialized(clazz.getFields())) {
            clazz.setFields(null);
        }
        if (!isInitialized(clazz.getInnerClasses())) {
            clazz.setInnerClasses(null);
        }
        if (!isInitialized(clazz.getInterfaces())) {
            clazz.setInterfaces(null);
        }
        if (!isInitialized(clazz.getKind())) {
            clazz.setKind(null);
        }
        if (!isInitialized(clazz.getMethods())) {
            clazz.setMethods(null);
        }
        if (!isInitialized(clazz.getModifiers())) {
            clazz.setModifiers(null);
        }
        if (!isInitialized(clazz.getSuperClass())) {
            clazz.setSuperClass(null);
        }
        if (!isInitialized(clazz.getTypeParameters())) {
            clazz.setTypeParameters(null);
        }
    }
}
