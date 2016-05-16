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

package signature.io.html;

import signature.compare.model.IExecutableMemberDelta;
import signature.model.IArrayType;
import signature.model.IClassDefinition;
import signature.model.IClassReference;
import signature.model.IExecutableMember;
import signature.model.IParameter;
import signature.model.IParameterizedType;
import signature.model.IPrimitiveType;
import signature.model.ITypeDefinition;
import signature.model.ITypeReference;
import signature.model.ITypeVariableDefinition;
import signature.model.ITypeVariableReference;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class ExecutableMemberComparator implements
        Comparator<IExecutableMemberDelta<? extends IExecutableMember>> {

    public int compare(IExecutableMemberDelta<? extends IExecutableMember> a,
            IExecutableMemberDelta<? extends IExecutableMember> b) {
        assert a.getType() == b.getType();
        IExecutableMember aMember = null;
        IExecutableMember bMember = null;

        if (a.getFrom() != null) {
            aMember = a.getFrom();
            bMember = b.getFrom();
        } else {
            aMember = a.getTo();
            bMember = b.getTo();
        }

        int returnValue = aMember.getName().compareTo(bMember.getName());
        return returnValue != 0 ? returnValue : compareParameterLists(aMember
                .getParameters(), bMember.getParameters());
    }

    private int compareParameterLists(List<IParameter> a, List<IParameter> b) {
        if (a.size() != b.size()) {
            return a.size() - b.size();
        }
        Iterator<IParameter> aIt = a.iterator();
        Iterator<IParameter> bIt = b.iterator();
        int returnValue = 0;
        while (aIt.hasNext() && bIt.hasNext()) {
            returnValue += getTypeName(aIt.next().getType()).compareTo(
                    getTypeName(bIt.next().getType()));
        }
        return returnValue;
    }

    private String getTypeName(ITypeReference type) {
        if (type instanceof IClassReference) {
            return getTypeName(((IClassReference) type).getClassDefinition());
        }
        if (type instanceof ITypeVariableReference) {
            return getTypeName(((ITypeVariableReference) type)
                    .getTypeVariableDefinition());
        }
        if (type instanceof IParameterizedType) {
            return getTypeName(((IParameterizedType) type).getRawType());
        }
        if (type instanceof IArrayType) {
            return getTypeName(((IArrayType) type).getComponentType());
        }
        if (type instanceof IPrimitiveType) {
            return ((IPrimitiveType) type).getName();
        }
        return type.toString();
    }

    private String getTypeName(ITypeDefinition type) {
        if (type instanceof IClassDefinition) {
            return ((IClassDefinition) type).getName();
        }
        if (type instanceof ITypeVariableDefinition) {
            return ((ITypeVariableDefinition) type).getName();
        }
        return type.toString();
    }

}
