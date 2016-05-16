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

package signature.converter.dex;

import java.util.HashMap;
import java.util.Map;

import signature.model.impl.SigEnumConstant;
import signature.model.impl.SigField;

public class FieldPool {

    private Map<FieldKey, SigField> fieldStore;
    private Map<FieldKey, SigEnumConstant> constantStore;

    public FieldPool() {
        fieldStore = new HashMap<FieldKey, SigField>();
        constantStore = new HashMap<FieldKey, SigEnumConstant>();
    }

    private static class FieldKey {
        private final String qualifiedClassName;
        private final String fieldName;

        public FieldKey(String qualifiedClassName, String fieldName) {
            this.qualifiedClassName = qualifiedClassName;
            this.fieldName = fieldName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = prime * fieldName.hashCode();
            result = prime * result + qualifiedClassName.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            FieldKey other = (FieldKey) obj;
            if (!fieldName.equals(other.fieldName)) {
                return false;
            }
            if (!qualifiedClassName.equals(other.qualifiedClassName)) {
                return false;
            }
            return true;
        }
    }

    public SigField getField(String qualifiedClassName, String fieldName) {
        FieldKey key = new FieldKey(qualifiedClassName, fieldName);
        SigField sigField = fieldStore.get(key);
        if (sigField == null) {
            sigField = new SigField(fieldName);
            fieldStore.put(key, sigField);
        }
        return sigField;
    }

    public SigEnumConstant getEnumConstant(String qualifiedName,
            String fieldName) {
        FieldKey key = new FieldKey(qualifiedName, fieldName);
        SigEnumConstant sigField = constantStore.get(key);
        if (sigField == null) {
            sigField = new SigEnumConstant(fieldName);
            constantStore.put(key, sigField);
        }
        return sigField;
    }

}
