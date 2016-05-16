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

import signature.compare.model.IEnumConstantDelta;
import signature.compare.model.IValueDelta;
import signature.model.IEnumConstant;

public class SigEnumConstantDelta extends SigMemberDelta<IEnumConstant>
        implements IEnumConstantDelta {

    private IValueDelta ordinalDelta;

    public SigEnumConstantDelta(IEnumConstant from, IEnumConstant to) {
        super(from, to);
    }

    public IValueDelta getOrdinalDelta() {
        return ordinalDelta;
    }

    public void setOrdinalDelta(IValueDelta ordinalDelta) {
        this.ordinalDelta = ordinalDelta;
    }
}
