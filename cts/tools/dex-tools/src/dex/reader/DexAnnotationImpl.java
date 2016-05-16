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

import java.util.List;

// FIXME provide special type for Signature annotation
/* package */final class DexAnnotationImpl implements DexAnnotation {

    private int offset;
    private DexBuffer buffer;
    private int[] typeIds;
    private String[] stringPool;
    private Visibility visibility;
    private DexEncodedAnnotationImpl encodedAnnotation;

    private TypeFormatter formatter = new TypeFormatter();
    private final FieldIdItem[] fieldIdItems;

    public DexAnnotationImpl(DexBuffer buffer, int offset, int[] typeIds,
            String[] stringPool, FieldIdItem[] fieldIdItems) {
        this.buffer = buffer;
        this.offset = offset;
        this.typeIds = typeIds;
        this.stringPool = stringPool;
        this.fieldIdItems = fieldIdItems;
        parseAnnotations();
    }

    private void parseAnnotations() {
        buffer.setPosition(offset);
        visibility = Visibility.get(buffer.readUByte());
        encodedAnnotation = new DexEncodedAnnotationImpl(buffer, this, typeIds,
                stringPool, fieldIdItems);
    }

    public List<DexAnnotationAttribute> getAttributes() {
        return encodedAnnotation.getValue();
    }

    public String getTypeName() {
        return encodedAnnotation.getTypeName();
    }

    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("@");
        builder.append(formatter.format(encodedAnnotation.getTypeName()));
        if (!getAttributes().isEmpty()) {
            builder.append(" (");
            for (DexAnnotationAttribute value : getAttributes()) {
                builder.append(value.toString());
                builder.append(" ");
            }
            builder.append(")");
        }
        return builder.toString();
    }

}
