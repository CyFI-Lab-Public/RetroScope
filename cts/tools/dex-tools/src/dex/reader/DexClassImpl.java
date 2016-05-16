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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dex.reader.DexFileReader.ClassDefItem;
import dex.reader.DexFileReader.FieldIdItem;
import dex.reader.DexFileReader.MethodsIdItem;
import dex.reader.DexFileReader.ProtIdItem;
import dex.structure.DexAnnotation;
import dex.structure.DexClass;
import dex.structure.DexField;
import dex.structure.DexMethod;

/* package */final class DexClassImpl implements DexClass {
    // constant
    private final int NO_INDEX = -1;
    // dex bytes
    private final DexBuffer buffer;
    // allready parsed
    private final ClassDefItem classDefItem;
    private final int[] typeIds;
    private final String[] stringPool;
    private ProtIdItem[] protoIdItems;
    private FieldIdItem[] fieldIdItems;
    private MethodsIdItem[] methodIdItems;

    //
    private List<DexField> fields;
    private List<DexMethod> methods;
    private List<String> interfaces;
    private ClassDataItem classDataItem;
    private AnnotationsDirectoryItem annotationDir;
    private Map<Integer, FieldAnnotation> idToFieldAnnotation =
            new HashMap<Integer, FieldAnnotation>();
    private Map<Integer, MethodAnnotation> idToMethodAnnotation =
            new HashMap<Integer, MethodAnnotation>();
    private Map<Integer, ParameterAnnotation> idToParameterAnnotation =
            new HashMap<Integer, ParameterAnnotation>();

    private Set<DexAnnotation> annotations;
    private TypeFormatter formatter = new TypeFormatter();

    private boolean hasClassData;


    public DexClassImpl(DexBuffer buffer, ClassDefItem classDefItem,
            String[] stringPool, int[] typeIds, ProtIdItem[] protoIdItems,
            FieldIdItem[] fieldIdItems, MethodsIdItem[] methodIdItems) {
        this.buffer = buffer;
        this.classDefItem = classDefItem;
        this.stringPool = stringPool;
        this.typeIds = typeIds;
        this.protoIdItems = protoIdItems;
        this.fieldIdItems = fieldIdItems;
        this.methodIdItems = methodIdItems;
        hasClassData = classDefItem.class_data_off != 0;
        parseClassData();
        parseAnnotationDirectory();
        parseClassAnnotations();
    }

    static class AnnotationsDirectoryItem {
        int class_annotations_off; // uint
        int fields_size; // uint
        int methods_size; // uint
        int annotated_params_size; // uint
        FieldAnnotation[] fieldAnnotations;
        MethodAnnotation[] methodAnnotations;
        ParameterAnnotation[] parameterAnnotations;
    }

    static class AnnotationSetItem {
        int size;// uint
        int[] annotationOffItem;
    }

    static class FieldAnnotation {
        int fieldIdx;// uint
        int annotationsOff;// uint
        AnnotationSetItem[] annotationSetItems;
    }

    static class MethodAnnotation {
        int methodIdx;// uint
        int annotationsOff;// uint
        AnnotationSetItem[] annotationSetItems;
    }

    static class ParameterAnnotation {
        int methodIdx;// uint
        int annotationsOff;// uint
        // AnnotationSetRefListItem[] annotationSetRefListItems;
    }

    private void parseAnnotationDirectory() {
        if (classDefItem.annotations_off != 0) {
            buffer.setPosition(classDefItem.annotations_off);
            annotationDir = new AnnotationsDirectoryItem();
            annotationDir.class_annotations_off = buffer.readUInt();
            annotationDir.fields_size = buffer.readUInt();
            annotationDir.methods_size = buffer.readUInt();
            annotationDir.annotated_params_size = buffer.readUInt();

            if (annotationDir.fields_size != 0) {
                annotationDir.fieldAnnotations =
                        new FieldAnnotation[annotationDir.fields_size];
                for (int i = 0; i < annotationDir.fields_size; i++) {
                    annotationDir.fieldAnnotations[i] = new FieldAnnotation();
                    annotationDir.fieldAnnotations[i].fieldIdx = buffer
                            .readUInt();
                    annotationDir.fieldAnnotations[i].annotationsOff = buffer
                            .readUInt();
                    idToFieldAnnotation.put(
                            annotationDir.fieldAnnotations[i].fieldIdx,
                            annotationDir.fieldAnnotations[i]);
                }
            }
            if (annotationDir.methods_size != 0) {
                annotationDir.methodAnnotations =
                        new MethodAnnotation[annotationDir.methods_size];
                for (int i = 0; i < annotationDir.methods_size; i++) {
                    annotationDir.methodAnnotations[i] = new MethodAnnotation();
                    annotationDir.methodAnnotations[i].methodIdx = buffer
                            .readUInt();
                    annotationDir.methodAnnotations[i].annotationsOff = buffer
                            .readUInt();
                    idToMethodAnnotation.put(
                            annotationDir.methodAnnotations[i].methodIdx,
                            annotationDir.methodAnnotations[i]);
                }
            }
            if (annotationDir.annotated_params_size != 0) {
                annotationDir.parameterAnnotations =
                        new ParameterAnnotation[annotationDir
                                .annotated_params_size];
                for (int i = 0; i < annotationDir.annotated_params_size; i++) {
                    annotationDir.parameterAnnotations[i] =
                            new ParameterAnnotation();
                    annotationDir.parameterAnnotations[i].methodIdx = buffer
                            .readUInt();
                    annotationDir.parameterAnnotations[i].annotationsOff =
                            buffer.readUInt();
                    idToParameterAnnotation.put(
                            annotationDir.parameterAnnotations[i].methodIdx,
                            annotationDir.parameterAnnotations[i]);
                }
            }
        }
    }

    static class ClassDataItem {
        int static_fields_size;// uleb128
        int instance_fields_size;// uleb128
        int direct_methods_size;// uleb128
        int virtual_methods_size;// uleb128
        EncodedField[] staticFields;
        EncodedField[] instanceFields;
        EncodedMethod[] directMethods;
        EncodedMethod[] virtualMethods;
    }

    static class EncodedField {
        int field_idx_diff; // uleb128
        int access_flags; // uleb128
    }

    static class EncodedMethod {
        int method_idx_diff;// uleb128
        int access_flags;// uleb128
        int code_off; // uleb128
    }

    private void parseClassData() {
        if (hasClassData) {
            buffer.setPosition(classDefItem.class_data_off);
            classDataItem = new ClassDataItem();
            classDataItem.static_fields_size = buffer.readUleb128();
            classDataItem.instance_fields_size = buffer.readUleb128();
            classDataItem.direct_methods_size = buffer.readUleb128();
            classDataItem.virtual_methods_size = buffer.readUleb128();
            classDataItem.staticFields = parseFields(
                    classDataItem.static_fields_size);
            classDataItem.instanceFields = parseFields(
                    classDataItem.instance_fields_size);
            classDataItem.directMethods = parseMethods(
                    classDataItem.direct_methods_size);
            classDataItem.virtualMethods = parseMethods(
                    classDataItem.virtual_methods_size);
        }
    }

    private EncodedField[] parseFields(int size) {
        EncodedField[] fields = new EncodedField[size];
        for (int i = 0; i < fields.length; i++) {
            fields[i] = new EncodedField();
            fields[i].field_idx_diff = buffer.readUleb128();
            fields[i].access_flags = buffer.readUleb128();
        }
        return fields;
    }

    private EncodedMethod[] parseMethods(int size) {
        EncodedMethod[] methods = new EncodedMethod[size];
        for (int i = 0; i < methods.length; i++) {
            methods[i] = new EncodedMethod();
            methods[i].method_idx_diff = buffer.readUleb128();
            methods[i].access_flags = buffer.readUleb128();
            methods[i].code_off = buffer.readUleb128();
        }
        return methods;
    }

    private void parseClassAnnotations() {
        annotations = new HashSet<DexAnnotation>();
        if (annotationDir != null && annotationDir.class_annotations_off != 0) {
            buffer.setPosition(annotationDir.class_annotations_off);
            final int size = buffer.readUInt();
            for (int i = 0; i < size; i++) {
                annotations.add(new DexAnnotationImpl(buffer.createCopy(),
                        buffer.readUInt(), typeIds, stringPool, fieldIdItems));
            }
        }
    }

    public synchronized List<DexField> getFields() {
        if (fields == null) {
            fields = new ArrayList<DexField>();
            if (hasClassData) {
                fields.addAll(getDexFields(classDataItem.staticFields));
                fields.addAll(getDexFields(classDataItem.instanceFields));
            }
        }
        return fields;
    }

    private List<DexField> getDexFields(EncodedField[] fields) {
        List<DexField> dexFields = new ArrayList<DexField>(fields.length);
        if (fields.length != 0) {
            int fieldIdIdx = 0;
            for (int i = 0; i < fields.length; i++) {
                int accessFlags = fields[i].access_flags;
                fieldIdIdx = (i == 0) ? fields[i].field_idx_diff : fieldIdIdx
                        + fields[i].field_idx_diff;
                dexFields.add(new DexFieldImpl(buffer.createCopy(), this,
                        fieldIdItems[fieldIdIdx], accessFlags,
                        idToFieldAnnotation.get(fieldIdIdx), stringPool,
                        typeIds, fieldIdItems));
            }
        }
        return dexFields;
    }

    public synchronized List<DexMethod> getMethods() {
        if (methods == null) {
            methods = new ArrayList<DexMethod>();
            if (hasClassData) {
                methods.addAll(getDexMethods(classDataItem.directMethods));
                methods.addAll(getDexMethods(classDataItem.virtualMethods));
            }
        }
        return methods;
    }

    private List<DexMethod> getDexMethods(EncodedMethod[] methods) {
        List<DexMethod> dexMethods = new ArrayList<DexMethod>(methods.length);
        if (methods.length != 0) {
            int methodIdIdx = 0;
            EncodedMethod method = null;
            for (int i = 0; i < methods.length; i++) {
                method = methods[i];
                methodIdIdx = (i == 0) ? method.method_idx_diff : methodIdIdx
                        + method.method_idx_diff;
                dexMethods.add(new DexMethodImpl(buffer, this,
                        methodIdItems[methodIdIdx],
                        protoIdItems[methodIdItems[methodIdIdx].proto_idx],
                        method.access_flags, idToMethodAnnotation
                                .get(methodIdIdx), idToParameterAnnotation
                                .get(methodIdIdx), stringPool, typeIds,
                        fieldIdItems));
            }
        }
        return dexMethods;
    }



    public synchronized List<String> getInterfaces() {
        if (interfaces == null) {
            interfaces = new LinkedList<String>();
            if (classDefItem.interfaces_off != 0) {
                buffer.setPosition(classDefItem.interfaces_off);
                int size = buffer.readUInt();
                for (int i = 0; i < size; i++) {
                    interfaces.add(stringPool[typeIds[buffer.readUShort()]]);
                }
            }
        }
        return interfaces;
    }

    // returns null if no super class is present
    public String getSuperClass() {
        return classDefItem.superclass_idx == NO_INDEX ? null
                : stringPool[typeIds[classDefItem.superclass_idx]];
    }

    public Set<DexAnnotation> getAnnotations() {
        return annotations;
    }

    public String getName() {
        return stringPool[typeIds[classDefItem.class_idx]];
    }

    public int getModifiers() {
        return classDefItem.access_flags;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(formatter.formatAnnotations(getAnnotations()));
        builder.append(Modifier.toString(getModifiers()));
        builder.append(" class ");
        builder.append(formatter.format(getName()));
        if (getSuperClass() != null) {
            builder.append(" extends ");
            builder.append(formatter.format(getSuperClass()));
        }
        if (!getInterfaces().isEmpty()) {
            builder.append(" implements ");
            builder.append(formatter.format(getInterfaces()));
        }
        return builder.toString();
    }

}
