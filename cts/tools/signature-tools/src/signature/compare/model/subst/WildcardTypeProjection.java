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

import signature.model.ITypeReference;
import signature.model.ITypeVariableDefinition;
import signature.model.IWildcardType;
import signature.model.impl.SigWildcardType;

import java.util.List;
import java.util.Map;

public class WildcardTypeProjection implements IWildcardType {

    private final IWildcardType original;

    private final Map<ITypeVariableDefinition, ITypeReference> mappings;

    public WildcardTypeProjection(IWildcardType original,
            Map<ITypeVariableDefinition, ITypeReference> mappings) {
        this.original = original;
        this.mappings = mappings;
    }

    public ITypeReference getLowerBound() {
        return ViewpointAdapter.substitutedTypeReference(original
                .getLowerBound(), mappings);
    }

    public List<ITypeReference> getUpperBounds() {
        return ViewpointAdapter.substitutedTypeReferences(original
                .getUpperBounds(), mappings);
    }

    @Override
    public int hashCode() {
        return SigWildcardType.hashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return SigWildcardType.equals(this, obj);
    }

    @Override
    public String toString() {
        return SigWildcardType.toString(this);
    }
}
