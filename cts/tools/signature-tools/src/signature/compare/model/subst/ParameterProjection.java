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

import signature.model.IAnnotation;
import signature.model.IParameter;
import signature.model.ITypeReference;
import signature.model.ITypeVariableDefinition;

import java.util.Map;
import java.util.Set;

public class ParameterProjection implements IParameter {
    private IParameter original;
    private Map<ITypeVariableDefinition, ITypeReference> mappings;

    public ParameterProjection(IParameter original,
            Map<ITypeVariableDefinition, ITypeReference> mappings) {
        this.original = original;
        this.mappings = mappings;
    }

    public Set<IAnnotation> getAnnotations() {
        return original.getAnnotations();
    }

    public ITypeReference getType() {
        return ViewpointAdapter.substitutedTypeReference(original.getType(),
                mappings);
    }

    @Override
    public String toString() {
        return getType().toString();
    }
}
