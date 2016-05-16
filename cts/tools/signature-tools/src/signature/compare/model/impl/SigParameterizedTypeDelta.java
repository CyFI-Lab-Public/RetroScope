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

package signature.compare.model.impl;

import java.util.Set;

import signature.compare.model.IParameterizedTypeDelta;
import signature.compare.model.ITypeReferenceDelta;
import signature.model.IParameterizedType;

public class SigParameterizedTypeDelta extends SigTypeDelta<IParameterizedType>
        implements IParameterizedTypeDelta {

    private ITypeReferenceDelta<?> ownerTypeDelta;
    private ITypeReferenceDelta<?> rawTypeDelta;
    private Set<ITypeReferenceDelta<?>> argumentTypeDeltas;

    public SigParameterizedTypeDelta(IParameterizedType from,
            IParameterizedType to) {
        super(from, to);
    }

    public ITypeReferenceDelta<?> getOwnerTypeDelta() {
        return ownerTypeDelta;
    }

    public void setOwnerTypeDelta(ITypeReferenceDelta<?> ownerTypeDelta) {
        this.ownerTypeDelta = ownerTypeDelta;
    }

    public ITypeReferenceDelta<?> getRawTypeDelta() {
        return rawTypeDelta;
    }

    public void setRawTypeDelta(ITypeReferenceDelta<?> rawTypeDelta) {
        this.rawTypeDelta = rawTypeDelta;
    }

    public Set<ITypeReferenceDelta<?>> getArgumentTypeDeltas() {
        return argumentTypeDeltas;
    }

    public void setArgumentTypeDeltas(
            Set<ITypeReferenceDelta<?>> argumentTypeDeltas) {
        this.argumentTypeDeltas = argumentTypeDeltas;
    }
}
