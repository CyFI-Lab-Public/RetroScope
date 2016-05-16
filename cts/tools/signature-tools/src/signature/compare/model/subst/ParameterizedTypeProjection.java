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

import signature.model.IClassReference;
import signature.model.IParameterizedType;
import signature.model.ITypeReference;
import signature.model.ITypeVariableDefinition;
import signature.model.impl.SigParameterizedType;

import java.util.List;
import java.util.Map;

public class ParameterizedTypeProjection implements IParameterizedType {

    private final IParameterizedType original;

    private final Map<ITypeVariableDefinition, ITypeReference> mappings;

    public ParameterizedTypeProjection(IParameterizedType original,
            Map<ITypeVariableDefinition, ITypeReference> mappings) {
        this.original = original;
        this.mappings = mappings;
    }

    public ITypeReference getOwnerType() {
        ITypeReference ownerType = original.getOwnerType();
        if (ownerType == null) {
            return null;
        }
        return ViewpointAdapter.substitutedTypeReference(ownerType, mappings);
    }

    private IClassReference rawType = null;

    /**
     * Returns the raw type with substituted type variables.
     * 
     * @return the raw type with substituted type variables
     */
    public IClassReference getRawType() {
        if (rawType == null) {
            rawType = (IClassReference) ViewpointAdapter
                    .substitutedTypeReference(original.getRawType(),
                            ViewpointAdapter.createTypeMapping(this, original
                                    .getRawType().getClassDefinition()));
        }
        return rawType;
    }

    private List<ITypeReference> arguments = null;

    public List<ITypeReference> getTypeArguments() {
        if (arguments == null) {
            arguments = ViewpointAdapter.substitutedTypeReferences(original
                    .getTypeArguments(), mappings);
        }
        return arguments;
    }

    @Override
    public int hashCode() {
        return SigParameterizedType.hashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return SigParameterizedType.equals(this, obj);
    }

    @Override
    public String toString() {
        return SigParameterizedType.toString(this);
    }
}
