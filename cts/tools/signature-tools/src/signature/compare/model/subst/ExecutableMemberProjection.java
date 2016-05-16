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
import signature.model.IClassDefinition;
import signature.model.IExecutableMember;
import signature.model.IParameter;
import signature.model.ITypeReference;
import signature.model.ITypeVariableDefinition;
import signature.model.Modifier;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ExecutableMemberProjection implements IExecutableMember {

    private final IExecutableMember original;
    private final Map<ITypeVariableDefinition, ITypeReference> mappings;

    public ExecutableMemberProjection(IExecutableMember original,
            Map<ITypeVariableDefinition, ITypeReference> mappings) {
        this.original = original;
        this.mappings = mappings;
    }

    public Set<IAnnotation> getAnnotations() {
        return original.getAnnotations();
    }

    public IClassDefinition getDeclaringClass() {
        throw new UnsupportedOperationException();
    }

    public Set<ITypeReference> getExceptions() {
        return ViewpointAdapter.substitutedTypeReferences(original
                .getExceptions(), mappings);
    }

    public Set<Modifier> getModifiers() {
        return original.getModifiers();
    }

    public String getName() {
        return original.getName();
    }

    public List<IParameter> getParameters() {
        List<IParameter> result = new LinkedList<IParameter>();
        for (IParameter parameter : original.getParameters()) {
            result.add(new ParameterProjection(parameter, mappings));
        }
        return result;
    }

    public List<ITypeVariableDefinition> getTypeParameters() {
        // FIXME bounds need to be substituted ?
        return original.getTypeParameters();
    }
}
