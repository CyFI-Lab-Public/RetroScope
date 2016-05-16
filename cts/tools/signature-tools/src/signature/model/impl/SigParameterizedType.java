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

import signature.model.IClassReference;
import signature.model.IParameterizedType;
import signature.model.ITypeReference;
import signature.model.util.ModelUtil;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class SigParameterizedType implements IParameterizedType, Serializable {

    private ITypeReference ownerType;
    private IClassReference rawType;
    private List<ITypeReference> typeArguments;

    public SigParameterizedType(ITypeReference ownerType,
            IClassReference rawType, List<ITypeReference> typeArguments) {
        this.ownerType = ownerType;
        this.rawType = rawType;
        this.typeArguments = typeArguments;
    }

    public ITypeReference getOwnerType() {
        ITypeReference returnValue = ownerType;
        if (returnValue == null) {
            if (rawType.getClassDefinition().getDeclaringClass() != null) {
                returnValue = new SigClassReference(rawType
                        .getClassDefinition().getDeclaringClass());
            }
        }
        return returnValue;
    }

    public IClassReference getRawType() {
        return rawType;
    }

    public List<ITypeReference> getTypeArguments() {
        return typeArguments;
    }

    @Override
    public int hashCode() {
        return hashCode(this);
    }

    public static int hashCode(IParameterizedType type) {
        final int prime = 31;
        int result = 1;
        result = prime * type.getRawType().hashCode();
        result = prime * result + type.getTypeArguments().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return equals(this, obj);
    }

    public static boolean equals(IParameterizedType thiz, Object that) {
        if (!(that instanceof IParameterizedType)) {
            return false;
        }
        IParameterizedType other = (IParameterizedType) that;
        if (thiz.getOwnerType() == null) {
            if (other.getOwnerType() != null) {
                return false;
            }
        } else if (Uninitialized.isInitialized(thiz.getOwnerType())) {
            if (!Uninitialized.isInitialized(other.getOwnerType())) {
                return false;
            }
        } else if (!thiz.getOwnerType().equals(other.getOwnerType())) {
            return false;
        }
        if (!thiz.getRawType().equals(other.getRawType())) {
            return false;
        }
        if (!thiz.getTypeArguments().equals(other.getTypeArguments())) {
            return false;
        }
        return true;
    }


    @Override
    public String toString() {
        return SigParameterizedType.toString(this);
    }

    public static String toString(IParameterizedType type) {
        StringBuilder builder = new StringBuilder();
        if (type.getOwnerType() != null) {
            builder.append(type.getOwnerType().toString());
            builder.append("::");
        }
        builder.append(type.getRawType());
        builder.append("<");
        builder.append(ModelUtil.separate(type.getTypeArguments(), ", "));
        builder.append(">");
        return builder.toString();
    }
}
