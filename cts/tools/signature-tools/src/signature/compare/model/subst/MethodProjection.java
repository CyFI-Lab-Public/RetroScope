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

import signature.model.IMethod;
import signature.model.ITypeReference;
import signature.model.ITypeVariableDefinition;
import signature.model.impl.SigMethod;

import java.util.Map;

public class MethodProjection extends ExecutableMemberProjection implements
        IMethod {

    private final IMethod original;
    private Map<ITypeVariableDefinition, ITypeReference> mappings;

    public MethodProjection(IMethod original,
            Map<ITypeVariableDefinition, ITypeReference> mappings) {
        super(original, mappings);
        this.mappings = mappings;
        this.original = original;
    }

    public ITypeReference getReturnType() {
        return ViewpointAdapter.substitutedTypeReference(original
                .getReturnType(), mappings);
    }

    @Override
    public String toString() {
        return "(" + SigMethod.toString(this) + " : " + mappings + " )";
    }
}
