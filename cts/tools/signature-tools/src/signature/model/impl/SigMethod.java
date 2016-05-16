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

import java.io.Serializable;

import signature.model.IMethod;
import signature.model.ITypeReference;
import signature.model.Modifier;
import signature.model.util.ModelUtil;

@SuppressWarnings("serial")
public class SigMethod extends SigExecutableMember implements IMethod,
        Serializable {

    private ITypeReference returnType = Uninitialized.unset();

    public SigMethod(String name) {
        super(name);
    }


    public ITypeReference getReturnType() {
        return returnType;
    }

    public void setReturnType(ITypeReference returnType) {
        this.returnType = returnType;
    }

    @Override
    public String toString() {
        return SigMethod.toString(this);
    }

    public static String toString(IMethod method) {
        StringBuilder builder = new StringBuilder();
        builder.append(Modifier.toString(method.getModifiers()));
        builder.append(method.getReturnType());
        builder.append(" ");
        if (method.getTypeParameters() != null
                && !method.getTypeParameters().isEmpty()) {
            builder.append("<");
            builder
                    .append(ModelUtil
                            .separate(method.getTypeParameters(), ", "));
            builder.append("> ");
        }
        builder.append(method.getName());
        builder.append("(");
        builder.append(method.getParameters().isEmpty() ? "" : ModelUtil
                .separate(method.getParameters(), ", "));
        builder.append(")");
        if (method.getExceptions() != null
                && !method.getExceptions().isEmpty()) {
            builder.append(" throws ");
            builder.append(ModelUtil.separate(method.getExceptions(), " "));
        }
        return builder.toString();
    }
}
