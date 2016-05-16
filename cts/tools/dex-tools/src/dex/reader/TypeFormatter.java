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

import dex.structure.DexAnnotation;
import dex.structure.DexClass;
import dex.structure.DexField;
import dex.structure.DexFile;
import dex.structure.DexMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <pre>
 * TypeDescriptor := 'V' | FieldTypeDescriptor
 * FieldTypeDescriptor := NonArrayFieldTypeDescriptor | ('[' * 1...255) NonArrayFieldTypeDescriptor
 * NonArrayFieldTypeDescriptor := 'Z' | 'B' | 'S' | 'C' | 'I' | 'J' | 'F' | 'D' | 'L' FullClassName ';'
 * </pre>
 */
public final class TypeFormatter {

    /**
     * V void; only valid for return types Z boolean B byte S short C char I int
     * J long F float D double Lfully/qualified/Name; the class
     * fully.qualified.Name [descriptor array of descriptor, usable recursively
     * for arrays-of-arrays, though it is invalid to have more than 255
     * dimensions.
     */
    public String format(String typeName) {
        if (typeName.length() == 1) {
            switch (typeName.charAt(0)) {
            case 'V':
                return "void";
            case 'Z':
                return "boolean";
            case 'B':
                return "byte";
            case 'S':
                return "short";
            case 'C':
                return "char";
            case 'I':
                return "int";
            case 'J':
                return "long";
            case 'F':
                return "float";
            case 'D':
                return "double";
            }
        } else {
            if (typeName.startsWith("L")) {
                return typeName.substring(1, typeName.length() - 1).replace(
                        "/", "."); // remove 'L' and ';', replace '/' with '.'
            } else if (typeName.startsWith("[")) {
                return format(typeName.substring(1)) + "[]";
            }
        }
        System.err.println("Strange type in formatter: " + typeName);
        return typeName;
    }

    public String format(List<String> typeNames) {
        List<String> types = new ArrayList<String>(typeNames.size());
        for (String type : typeNames) {
            types.add(format(type));
        }
        return format(types, ", ");
    }

    public String formatAnnotations(Set<DexAnnotation> annotations) {
        return format(new ArrayList<DexAnnotation>(annotations), "\n") + "\n";
    }

    private String format(List<?> elements, String separator) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Object element : elements) {
            if (!first) {
                builder.append(separator);
            }
            builder.append(element.toString());
            first = false;
        }
        return builder.toString();
    }


    public String formatDexFile(DexFile file) {
        StringBuilder builder = new StringBuilder();
        builder.append("----------------DEX_FILE--------------\n\n");
        builder.append("Filename: ").append(file.getName());
        builder.append("\n-----------DEFINED_CLASSES------------\n\n");
        for (DexClass dexClass : file.getDefinedClasses()) {
            builder.append("\n________________CLASS________________\n\n");
            builder.append(dexClass);
            builder.append("\n\n----------------FIELDS----------------\n");
            for (DexField field : dexClass.getFields()) {
                builder.append(field).append("\n");
            }
            builder.append("----------------METHODS----------------\n");
            for (DexMethod method : dexClass.getMethods()) {
                builder.append(method).append("\n");
            }
        }
        return builder.toString();
    }
}
