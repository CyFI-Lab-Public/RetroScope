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

import signature.compare.model.ITypeReferenceDelta;
import signature.compare.model.IUpperBoundsDelta;
import signature.compare.model.IWildcardTypeDelta;
import signature.model.IWildcardType;

public class SigWildcardTypeDelta extends SigTypeDelta<IWildcardType>
        implements IWildcardTypeDelta {

    private ITypeReferenceDelta<?> lowerBoundDelta;
    private IUpperBoundsDelta upperBoundDelta;

    public SigWildcardTypeDelta(IWildcardType from, IWildcardType to) {
        super(from, to);
    }

    public ITypeReferenceDelta<?> getLowerBoundDelta() {
        return lowerBoundDelta;
    }

    public void setLowerBoundDelta(ITypeReferenceDelta<?> lowerBoundDelta) {
        this.lowerBoundDelta = lowerBoundDelta;
    }

    public IUpperBoundsDelta getUpperBoundDelta() {
        return upperBoundDelta;
    }

    public void setUpperBoundDelta(IUpperBoundsDelta upperBoundDelta) {
        this.upperBoundDelta = upperBoundDelta;
    }
}
