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

import signature.model.ITypeReference;
import signature.model.IWildcardType;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class SigWildcardType implements IWildcardType, Serializable {

    private ITypeReference lowerBound;
    private List<ITypeReference> upperBounds;

    public SigWildcardType(ITypeReference lowerBound,
            List<ITypeReference> upperBounds) {
        this.lowerBound = lowerBound;
        this.upperBounds = upperBounds;
    }

    public ITypeReference getLowerBound() {
        return lowerBound;
    }

    public List<ITypeReference> getUpperBounds() {
        return upperBounds;
    }

    @Override
    public int hashCode() {
        return SigWildcardType.hashCode(this);
    }

    public static int hashCode(IWildcardType type) {
        final int prime = 31;
        int result = 1;
        result = prime
                + ((type.getLowerBound() == null) ? 0 : type.getLowerBound()
                        .hashCode());
        result = prime * result + type.getUpperBounds().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return SigWildcardType.equals(this, obj);
    }

    public static boolean equals(IWildcardType thiz, Object obj) {
        if (thiz == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof IWildcardType)) return false;
        IWildcardType that = (IWildcardType) obj;
        if (thiz.getLowerBound() == null) {
            if (that.getLowerBound() != null) return false;
        } else if (!thiz.getLowerBound().equals(that.getLowerBound()))
            return false;
        if (!thiz.getUpperBounds().equals(that.getUpperBounds())) return false;
        return true;
    }


    @Override
    public String toString() {
        return SigWildcardType.toString(this);
    }

    public static String toString(IWildcardType thiz) {
        StringBuilder builder = new StringBuilder();
        builder.append("?");
        if (thiz.getLowerBound() != null) {
            builder.append(" ");
            builder.append(" super ");
            builder.append(thiz.getLowerBound());
        }
        if (!thiz.getUpperBounds().isEmpty()) {
            builder.append(" extends ");
            builder.append(thiz.getUpperBounds());
        }
        return builder.toString();
    }
}
