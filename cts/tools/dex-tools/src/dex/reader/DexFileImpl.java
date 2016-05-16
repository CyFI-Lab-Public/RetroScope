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

import dex.reader.DexFileReader.ClassDefItem;
import dex.reader.DexFileReader.FieldIdItem;
import dex.reader.DexFileReader.MethodsIdItem;
import dex.reader.DexFileReader.ProtIdItem;
import dex.structure.DexClass;
import dex.structure.DexFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* package */final class DexFileImpl implements DexFile {

    private final String[] stringPool;
    private final int[] typeIds;
    private ProtIdItem[] protoIdItems;
    private FieldIdItem[] fieldIdItems;
    private MethodsIdItem[] methodIdItems;
    private ClassDefItem[] classDefItems;
    private final DexBuffer buffer;

    private List<DexClass> classes = null;

    public DexFileImpl(DexBuffer buffer, String[] stringPool, int[] typeIds,
            ProtIdItem[] protoIds, FieldIdItem[] fieldIdItems,
            MethodsIdItem[] methodIdItems, ClassDefItem[] classDefItems) {
        this.buffer = buffer;
        this.stringPool = stringPool;
        this.typeIds = typeIds;
        this.protoIdItems = protoIds;
        this.fieldIdItems = fieldIdItems;
        this.methodIdItems = methodIdItems;
        this.classDefItems = classDefItems;
    }

    /*
     * (non-Javadoc)
     * 
     * @see dex.reader.DexFile#getDefinedClasses()
     */
    public synchronized List<DexClass> getDefinedClasses() {
        if (classes == null) {
            classes = new ArrayList<DexClass>(classDefItems.length);
            for (int i = 0; i < classDefItems.length; i++) {
                classes.add(new DexClassImpl(buffer.createCopy(),
                        classDefItems[i], stringPool, typeIds, protoIdItems,
                        fieldIdItems, methodIdItems));
            }
        }
        return classes;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("StringPool:\n").append(Arrays.toString(stringPool));
        b.append("\nTypes:\n");
        for (int i = 0; i < typeIds.length; i++) {
            b.append(stringPool[typeIds[i]] + "\n");
        }
        b.append("\nProtos:\n").append(Arrays.toString(protoIdItems));
        b.append("\nFields:\n").append(Arrays.toString(fieldIdItems));
        b.append("\nMethods:\n").append(Arrays.toString(methodIdItems));
        b.append("\nClasses:\n").append(Arrays.toString(classDefItems));
        return b.toString();
    }

    public String getName() {
        return "DexFile";
    }
}
