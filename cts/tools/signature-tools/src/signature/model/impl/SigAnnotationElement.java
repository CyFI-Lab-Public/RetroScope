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

import signature.model.IAnnotationElement;
import signature.model.IAnnotationField;

@SuppressWarnings("serial")
public class SigAnnotationElement implements IAnnotationElement, Serializable {

    private IAnnotationField declaringField;
    private Object value;

    public IAnnotationField getDeclaringField() {
        return declaringField;
    }

    public void setDeclaringField(IAnnotationField declaringField) {
        this.declaringField = declaringField;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getDeclaringField().getName());
        builder.append(" = ");
        builder.append(getValue());
        return builder.toString();
    }
}
