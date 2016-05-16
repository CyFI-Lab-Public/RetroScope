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

import signature.model.IPrimitiveType;

public enum SigPrimitiveType implements IPrimitiveType {
    VOID_TYPE("void"), BOOLEAN_TYPE("boolean"), BYTE_TYPE("byte"), CHAR_TYPE(
            "char"), SHORT_TYPE("short"), INT_TYPE("int"), LONG_TYPE("long"),
            FLOAT_TYPE("float"), DOUBLE_TYPE("double");

    private String name;

    private SigPrimitiveType(String name) {
        this.name = name;
    }

    public static SigPrimitiveType valueOfTypeName(String name) {
        for (SigPrimitiveType primitive : values()) {
            if (primitive.name.equals(name)) {
                return primitive;
            }
        }
        throw new IllegalArgumentException(name + " is not a primitive type");
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
