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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

import signature.model.IAnnotationField;
import signature.model.IClassDefinition;
import signature.model.IClassReference;
import signature.model.IPackage;
import signature.model.ITypeReference;
import signature.model.ITypeVariableDefinition;
import signature.model.ITypeVariableReference;
import signature.model.Kind;

/**
 *
 */
public class Uninitialized {

    private static final Object UNINITIALIZED;

    static {
        UNINITIALIZED = Proxy.newProxyInstance(Uninitialized.class
                .getClassLoader(), new Class[] {
                ITypeReference.class, IPackage.class, IClassDefinition.class,
                IClassReference.class, ITypeVariableReference.class,
                ITypeVariableDefinition.class, IAnnotationField.class,
                Set.class, List.class}, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {
                if (method.getName().equals("toString")) {
                    return "Uninitialized";
                }

                throw new UnsupportedOperationException();
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> T unset() {
        return (T) UNINITIALIZED;
    }

    public static boolean isInitialized(Object o) {
        return o != UNINITIALIZED && o != Kind.UNINITIALIZED;
    }
}
