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

import java.util.Map;

import signature.model.IArrayType;
import signature.model.ITypeReference;
import signature.model.ITypeVariableDefinition;
import signature.model.impl.SigArrayType;

public class ArrayTypeProjection implements IArrayType {

    private final IArrayType original;
    private final Map<ITypeVariableDefinition, ITypeReference> mappings;

    public ArrayTypeProjection(IArrayType original,
            Map<ITypeVariableDefinition, ITypeReference> mappings) {
        this.original = original;
        this.mappings = mappings;
    }

    public ITypeReference getComponentType() {
        return ViewpointAdapter.substitutedTypeReference(original
                .getComponentType(), mappings);
    }

    @Override
    public int hashCode() {
        return SigArrayType.hashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return SigArrayType.equals(this, obj);
    }

    @Override
    public String toString() {
        return "(" + SigArrayType.toString(this) + " : " + mappings + " )";
    }
}
