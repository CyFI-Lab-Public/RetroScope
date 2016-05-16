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

import signature.model.IClassDefinition;
import signature.model.IClassReference;

import java.io.Serializable;

@SuppressWarnings("serial")
public class SigClassReference implements IClassReference, Serializable {

    private final IClassDefinition definition;

    public SigClassReference(IClassDefinition definition) {
        this.definition = definition;
    }

    public IClassDefinition getClassDefinition() {
        return definition;
    }

    @Override
    public boolean equals(Object obj) {
        return SigClassReference.equals(this, obj);
    }

    public static boolean equals(IClassReference thiz, Object that) {
        if (that instanceof IClassReference) {
            return thiz.getClassDefinition().equals(
                    ((IClassReference) that).getClassDefinition());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return SigClassReference.hashCode(this);
    }

    public static int hashCode(IClassReference thiz) {
        return thiz.getClassDefinition().hashCode();
    }

    @Override
    public String toString() {
        return SigClassReference.toString(this);
    }

    public static String toString(IClassReference thiz) {
        return "-> " + thiz.getClassDefinition().getQualifiedName();
    }
}
