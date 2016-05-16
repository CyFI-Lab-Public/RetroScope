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

import dex.reader.DexClassImpl.FieldAnnotation;
import dex.reader.DexFileReader.FieldIdItem;
import dex.structure.DexAnnotation;
import dex.structure.DexClass;
import dex.structure.DexField;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/* package */final class DexFieldImpl implements DexField {

    private DexBuffer buffer;
    private String[] stringPool;
    private FieldIdItem fieldIdItem;
    private int[] typeIds;
    private final int accessFlags;
    private Set<DexAnnotation> annotations;
    private FieldAnnotation fieldAnnotation;
    private TypeFormatter formatter = new TypeFormatter();
    private final DexClass declaringClass;
    private final FieldIdItem[] fieldIdItems;

    public DexFieldImpl(DexBuffer buffer, DexClass declaringClass,
            FieldIdItem fieldIdItem, int accessFlags,
            FieldAnnotation fieldAnnotation, String[] stringPool,
            int[] typeIds, FieldIdItem[] fieldIdItems) {
        this.buffer = buffer;
        this.declaringClass = declaringClass;
        this.fieldIdItem = fieldIdItem;
        this.accessFlags = accessFlags;
        this.fieldAnnotation = fieldAnnotation;
        this.stringPool = stringPool;
        this.typeIds = typeIds;
        this.fieldIdItems = fieldIdItems;
        parseAnnotations();
    }

    private void parseAnnotations() {
        annotations = new HashSet<DexAnnotation>();
        if (fieldAnnotation != null) {
            buffer.setPosition(fieldAnnotation.annotationsOff);
            final int size = buffer.readUInt();
            for (int i = 0; i < size; i++) {
                annotations.add(new DexAnnotationImpl(buffer.createCopy(),
                        buffer.readUInt(), typeIds, stringPool, fieldIdItems));
            }
        }
    }

    public String getName() {
        return stringPool[fieldIdItem.name_idx];
    }

    public String getType() {
        return stringPool[typeIds[fieldIdItem.type_idx]];
    }

    public int getModifiers() {
        return accessFlags;
    }

    public synchronized Set<DexAnnotation> getAnnotations() {
        return annotations;
    }

    public DexClass getDeclaringClass() {
        return declaringClass;
    }

    public boolean isEnumConstant() {
        return (getModifiers() & 0x4000) > 0;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(formatter.formatAnnotations(getAnnotations()));
        builder.append(Modifier.toString(getModifiers()));
        builder.append(" ");
        builder.append(formatter.format(getType()));
        builder.append(" ");
        builder.append(getName());
        return builder.toString();
    }
}
