/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright (C) 2012 The Android Open Source Project
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

package java.lang.reflect;

import com.android.dex.Dex;
import java.lang.annotation.Annotation;
import libcore.reflect.AnnotationAccess;
import libcore.util.EmptyArray;

/**
 * This class represents methods and constructors.
 * @hide
 */
public final class ArtMethod {

    /** Method's declaring class */
    private Class<?> declaringClass;
    /** Method access flags (modifiers) */
    private int accessFlags;
    /** DexFile index */
    private int methodDexIndex;
    /** Dispatch table entry */
    private int methodIndex;
    /** DexFile offset of CodeItem for this Method */
    private int codeItemOffset;
    /* ART compiler meta-data */
    private int frameSizeInBytes;
    private int coreSpillMask;
    private int fpSpillMask;
    private int mappingTable;
    private int gcMap;
    private int vmapTable;
    /** ART: compiled managed code associated with this Method */
    private int entryPointFromCompiledCode;
    /** ART: entry point from interpreter associated with this Method */
    private int entryPointFromInterpreter;
    /** ART: if this is a native method, the native code that will be invoked */
    private int nativeMethod;
    /* ART: dex cache fast access */
    private String[] dexCacheStrings;
    Class<?>[] dexCacheResolvedTypes;
    private ArtMethod[] dexCacheResolvedMethods;
    private Object[] dexCacheInitializedStaticStorage;

    /**
     * Only created by art directly.
     */
    private ArtMethod() {}

    Class getDeclaringClass() {
        return declaringClass;
    }

    public int getAccessFlags() {
        return accessFlags;
    }

    int getDexMethodIndex() {
        return methodDexIndex;
    }

    public static String getMethodName(ArtMethod artMethod) {
        artMethod = artMethod.findOverriddenMethodIfProxy();
        Dex dex = artMethod.getDeclaringClass().getDex();
        int nameIndex = dex.nameIndexFromMethodIndex(artMethod.getDexMethodIndex());
        // Note, in the case of a Proxy the dex cache strings are equal.
        return artMethod.getDexCacheString(dex, nameIndex);
    }

    /**
     * Returns true if the given parameters match those of the method in the given order.
     *
     * @hide
     */
    public static boolean equalConstructorParameters(ArtMethod artMethod, Class<?>[] params) {
        Dex dex = artMethod.getDeclaringClass().getDex();
        short[] types = dex.parameterTypeIndicesFromMethodIndex(artMethod.getDexMethodIndex());
        if (types.length != params.length) {
            return false;
        }
        for (int i = 0; i < types.length; i++) {
            if (artMethod.getDexCacheType(dex, types[i]) != params[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the given parameters match those of this method in the given order.
     *
     * @hide
     */
    public static boolean equalMethodParameters(ArtMethod artMethod, Class<?>[] params) {
        return equalConstructorParameters(artMethod.findOverriddenMethodIfProxy(), params);
    }

    Class<?>[] getParameterTypes() {
        Dex dex = getDeclaringClass().getDex();
        short[] types = dex.parameterTypeIndicesFromMethodIndex(methodDexIndex);
        if (types.length == 0) {
            return EmptyArray.CLASS;
        }
        Class<?>[] parametersArray = new Class[types.length];
        for (int i = 0; i < types.length; i++) {
            // Note, in the case of a Proxy the dex cache types are equal.
            parametersArray[i] = getDexCacheType(dex, types[i]);
        }
        return parametersArray;
    }

    Class<?> getReturnType() {
        Dex dex = declaringClass.getDex();
        int returnTypeIndex = dex.returnTypeIndexFromMethodIndex(methodDexIndex);
        // Note, in the case of a Proxy the dex cache types are equal.
        return getDexCacheType(dex, returnTypeIndex);
    }

    /**
     * Performs a comparison of the parameters to this method with the given parameters.
     *
     * @hide
     */
    int compareParameters(Class<?>[] params) {
        Dex dex = getDeclaringClass().getDex();
        short[] types = dex.parameterTypeIndicesFromMethodIndex(methodDexIndex);
        int length = Math.min(types.length, params.length);
        for (int i = 0; i < length; i++) {
            Class<?> aType = getDexCacheType(dex, types[i]);
            Class<?> bType = params[i];
            if (aType != bType) {
                int comparison = aType.getName().compareTo(bType.getName());
                if (comparison != 0) {
                    return comparison;
                }
            }
        }
        return types.length - params.length;
    }

    Annotation[][] getParameterAnnotations() {
        return AnnotationAccess.getParameterAnnotations(declaringClass, methodDexIndex);
    }

    /**
     * Returns a string from the dex cache, computing the string from the dex file if necessary.
     * Note this method replicates {@link java.lang.Class#getDexCacheString(Dex, int)}, but in
     * Method we can avoid one indirection.
     */
    private String getDexCacheString(Dex dex, int dexStringIndex) {
        String s = (String) dexCacheStrings[dexStringIndex];
        if (s == null) {
            s = dex.strings().get(dexStringIndex).intern();
            dexCacheStrings[dexStringIndex] = s;
        }
        return s;
    }

    /**
     * Returns a resolved type from the dex cache, computing the string from the dex file if
     * necessary. Note this method delegates to {@link java.lang.Class#getDexCacheType(Dex, int)},
     * but in Method we can avoid one indirection.
     */
    private Class<?> getDexCacheType(Dex dex, int dexTypeIndex) {
        Class<?> resolvedType = dexCacheResolvedTypes[dexTypeIndex];
        if (resolvedType == null) {
            resolvedType = declaringClass.getDexCacheType(dex, dexTypeIndex);
        }
        return resolvedType;
    }

    /**
     * Returns the {@code ArtMethod} that this method overrides for
     * proxy methods, otherwise returns this method. Used to determine
     * the interface method overridden by a proxy method (as the proxy
     * method doesn't directly support operations such as {@link
     * Method#getName}).
     */
    ArtMethod findOverriddenMethodIfProxy() {
        if (declaringClass.isProxy()) {
            // Proxy method's declaring class' dex cache refers to that of Proxy. The local cache in
            // Method refers to the original interface's dex cache and is ensured to be resolved by
            // proxy generation.
            return dexCacheResolvedMethods[methodDexIndex];
        }
        return this;
    }
}
