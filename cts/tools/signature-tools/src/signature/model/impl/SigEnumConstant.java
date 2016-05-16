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

package signature.model.impl;

import signature.model.IEnumConstant;

@SuppressWarnings("serial")
public class SigEnumConstant extends SigField implements IEnumConstant {

    private static final int UNKNOWN = -1;

    private int ordinal = UNKNOWN;

    public SigEnumConstant(String name) {
        super(name);
    }

    public int getOrdinal() {
        if (ordinal == UNKNOWN) {
            throw new UnsupportedOperationException();
        }
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }
}
