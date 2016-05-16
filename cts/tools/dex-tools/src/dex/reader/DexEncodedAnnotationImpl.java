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
import dex.structure.DexEncodedAnnotation;
import dex.structure.DexEncodedValueType;

import java.util.ArrayList;
import java.util.List;

/* package */final class DexEncodedAnnotationImpl implements
        DexEncodedAnnotation {

    private List<DexAnnotationAttribute> values;
    private final DexBuffer buffer;
    private final int[] typeIds;
    private final String[] stringPool;
    private int typeIdx;
    private final FieldIdItem[] fieldIdItems;
    private final DexAnnotation annotation;

    public DexEncodedAnnotationImpl(DexBuffer buffer, DexAnnotation annotation,
            int[] typeIds, String[] stringPool, FieldIdItem[] fieldIdItems) {
        this.buffer = buffer;
        this.annotation = annotation;
        this.typeIds = typeIds;
        this.stringPool = stringPool;
        this.fieldIdItems = fieldIdItems;
        parseEncodedAnnotation();
    }

    private void parseEncodedAnnotation() {
        typeIdx = buffer.readUleb128();
        int size = buffer.readUleb128();
        values = new ArrayList<DexAnnotationAttribute>(size);
        for (int j = 0; j < size; j++) {
            values.add(new DexAnnotationAttributeImpl(buffer, annotation,
                    typeIds, stringPool, fieldIdItems));
        }
    }

    public DexEncodedValueType getType() {
        return DexEncodedValueType.VALUE_ANNOTATION;
    }

    public List<DexAnnotationAttribute> getValue() {
        return values;
    }

    public String getTypeName() {
        return stringPool[typeIds[typeIdx]];
    }

    @Override
    public String toString() {
        return getTypeName() + ":" + getValue();
    }
}
