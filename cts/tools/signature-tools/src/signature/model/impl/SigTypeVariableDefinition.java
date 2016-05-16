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

import signature.model.IGenericDeclaration;
import signature.model.ITypeReference;
import signature.model.ITypeVariableDefinition;
import signature.model.util.ModelUtil;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class SigTypeVariableDefinition implements ITypeVariableDefinition,
        Serializable {

    private String name;
    private IGenericDeclaration genericDeclaration;
    private List<ITypeReference> upperBounds = Uninitialized.unset();

    public SigTypeVariableDefinition(String name,
            IGenericDeclaration genericDeclaration) {
        this.name = name;
        this.genericDeclaration = genericDeclaration;
    }

    public String getName() {
        return name;
    }

    public IGenericDeclaration getGenericDeclaration() {
        return genericDeclaration;
    }

    public List<ITypeReference> getUpperBounds() {
        return upperBounds;
    }

    public void setUpperBounds(List<ITypeReference> upperBounds) {
        this.upperBounds = upperBounds;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name);
        if (getUpperBounds().size() != 1) {
            builder.append(ModelUtil.separate(getUpperBounds(), ", "));
        } else {
            if (!ModelUtil.isJavaLangObject(getUpperBounds().get(0))) {
                builder.append(getUpperBounds().get(0));
            }
        }
        return builder.toString();
    }

}
