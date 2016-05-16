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

import signature.model.IArrayType;
import signature.model.ITypeReference;

@SuppressWarnings("serial")
public class SigArrayType implements IArrayType, Serializable {
    private ITypeReference componentType;

    public SigArrayType(ITypeReference componentType) {
        this.componentType = componentType;
    }

    public ITypeReference getComponentType() {
        return componentType;
    }

    @Override
    public int hashCode() {
        return SigArrayType.hashCode(this);
    }

    public static int hashCode(IArrayType type) {
        return type.getComponentType().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return SigArrayType.equals(this, obj);
    }

    public static boolean equals(IArrayType thiz, Object that) {
        if (!(that instanceof IArrayType)) {
            return false;
        }
        IArrayType other = (IArrayType) that;
        return thiz.getComponentType().equals(other.getComponentType());
    }

    @Override
    public String toString() {
        return SigArrayType.toString(this);
    }

    public static String toString(IArrayType type) {
        StringBuilder builder = new StringBuilder();
        builder.append(type.getComponentType().toString());
        builder.append("[]");
        return builder.toString();
    }
}
