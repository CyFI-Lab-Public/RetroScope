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

import java.util.List;
import java.util.Set;

import signature.compare.model.ITypeReferenceDelta;
import signature.compare.model.IUpperBoundsDelta;
import signature.model.ITypeReference;

public class SigUpperBoundsDelta extends SigDelta<List<ITypeReference>>
        implements IUpperBoundsDelta {

    private ITypeReferenceDelta<?> firstUpperBoundDelta;
    private Set<ITypeReferenceDelta<?>> remainingUpperBoundDeltas;

    public SigUpperBoundsDelta(List<ITypeReference> from,
            List<ITypeReference> to) {
        super(from, to);
    }


    public ITypeReferenceDelta<?> getFirstUpperBoundDelta() {
        return firstUpperBoundDelta;
    }

    public void setFirstUpperBoundDelta(
            ITypeReferenceDelta<?> firstUpperBoundDelta) {
        this.firstUpperBoundDelta = firstUpperBoundDelta;
    }

    public Set<ITypeReferenceDelta<?>> getRemainingUpperBoundDeltas() {
        return remainingUpperBoundDeltas;
    }

    public void setRemainingUpperBoundDeltas(
            Set<ITypeReferenceDelta<?>> remainingUpperBoundDeltas) {
        this.remainingUpperBoundDeltas = remainingUpperBoundDeltas;
    }
}
