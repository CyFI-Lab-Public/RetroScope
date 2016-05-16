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

import java.util.HashSet;
import java.util.Set;

import dex.reader.DexFileReader.FieldIdItem;
import dex.structure.DexAnnotation;
import dex.structure.DexParameter;

public class DexParameterImpl implements DexParameter {

    private final String typeName;
    private final Integer annotationOffset;
    private Set<DexAnnotation> annotations;
    private final DexBuffer buffer;
    private final int[] typeIds;
    private final String[] stringPool;
    private final FieldIdItem[] fieldIdItems;

    public DexParameterImpl(DexBuffer buffer, String typeName,
            Integer annotationOffset, int[] typeIds, String[] stringPool,
            FieldIdItem[] fieldIdItems) {
        this.buffer = buffer;
        this.typeName = typeName;
        this.annotationOffset = annotationOffset;
        this.typeIds = typeIds;
        this.stringPool = stringPool;
        this.fieldIdItems = fieldIdItems;
        parseAnnotations();
    }

    private void parseAnnotations() {
        annotations = new HashSet<DexAnnotation>();
        if (annotationOffset != null) {
            buffer.setPosition(annotationOffset);
            final int size = buffer.readUInt();
            for (int i = 0; i < size; i++) {
                annotations.add(new DexAnnotationImpl(buffer.createCopy(),
                        buffer.readUInt(), typeIds, stringPool, fieldIdItems));
            }
        }
    }

    public String getTypeName() {
        return typeName;
    }

    public Set<DexAnnotation> getAnnotations() {
        return annotations;
    }

    @Override
    public String toString() {
        return getTypeName();
    }
}
