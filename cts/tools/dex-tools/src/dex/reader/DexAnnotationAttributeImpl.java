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

package dex.reader;

import dex.reader.DexFileReader.FieldIdItem;
import dex.structure.DexAnnotation;
import dex.structure.DexAnnotationAttribute;
import dex.structure.DexEncodedValue;

/* package */final class DexAnnotationAttributeImpl implements
        DexAnnotationAttribute {
    int nameIdx; // uleb128
    DexEncodedValue value;// encoded_value
    private String[] stringPool;
    private DexBuffer buffer;
    private final int[] typeIds;
    private final FieldIdItem[] fieldIdItems;
    private final DexAnnotation annotation;

    public DexAnnotationAttributeImpl(DexBuffer buffer,
            DexAnnotation annotation, int[] typeIds, String[] stringPool,
            FieldIdItem[] fieldIdItems) {
        this.buffer = buffer;
        this.annotation = annotation;
        this.typeIds = typeIds;
        this.stringPool = stringPool;
        this.fieldIdItems = fieldIdItems;
        parseValue();
    }

    private void parseValue() {
        nameIdx = buffer.readUleb128();
        value = new DexEncodedValueImpl(buffer, annotation, typeIds,
                stringPool, fieldIdItems);
    }

    public String getName() {
        return stringPool[nameIdx];
    }

    public DexEncodedValue getEncodedValue() {
        return value;
    }

    @Override
    public String toString() {
        return getName() + " " + getEncodedValue();
    }

    public DexAnnotation getAnnotation() {
        return annotation;
    }

}
