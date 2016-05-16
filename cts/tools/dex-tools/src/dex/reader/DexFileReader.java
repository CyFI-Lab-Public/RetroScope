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

import dex.structure.DexFile;

import java.util.Arrays;

public final class DexFileReader {

    // DEX constants
    private int ENDIAN_CONSTANT = 0x12345678;
    @SuppressWarnings("unused")
    private int REVERSE_ENDIAN_CONSTANT = 0x78563412;
    private final byte[] REF_MAGIC = new byte[] {
            0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x35, 0x00};

    // Header values
    private DexBuffer b;
    private byte[] magic = new byte[8];
    @SuppressWarnings("unused")
    private int checksum = 0;
    private byte[] signature = new byte[20];
    @SuppressWarnings("unused")
    private int fileSize = 0;
    @SuppressWarnings("unused")
    private int headerSize = 0;
    private int endianTag = 0;

    // Indices of offset and size items
    private static final int LINK = 0;
    private static final int MAP = 1; // no size!
    private static final int STRING_IDS = 2;
    private static final int TYPE_IDS = 3;
    private static final int PROTO_IDS = 4;
    private static final int FIELD_IDS = 5;
    private static final int METHOD_IDS = 6;
    private static final int CLASS_DEFS = 7;
    private static final int DATA = 8;

    private int[] size = new int[9];
    private int[] off = new int[9];


    //
    private String[] stringPool;
    private int[] typeIds; // values are index of stringPool
    private ProtIdItem[] protoIdItems;
    private FieldIdItem[] fieldIdItems;
    private MethodsIdItem[] methodIdItems;
    private ClassDefItem[] classDefItems;

    // starting buffer at zero
    public DexFile read(DexBuffer buffer) {
        this.b = buffer;
        readMagic();
        readChecksum();
        readSignature();
        readFileSize();
        readHeaderSize();
        readEndianTag();
        readSize(LINK);
        readOffset(LINK);
        readOffset(MAP);
        readSize(STRING_IDS);
        readOffset(STRING_IDS);
        readSize(TYPE_IDS);
        readOffset(TYPE_IDS);
        readSize(PROTO_IDS);
        readOffset(PROTO_IDS);
        readSize(FIELD_IDS);
        readOffset(FIELD_IDS);
        readSize(METHOD_IDS);
        readOffset(METHOD_IDS);
        readSize(CLASS_DEFS);
        readOffset(CLASS_DEFS);
        readSize(DATA);
        readOffset(DATA);
        // from now on, index is not automatically on the desired position
        readStrings();
        readTypeIds();
        readProtos();
        readFields();
        readMethods();
        readClasses();

        return new DexFileImpl(b.createCopy(), stringPool, typeIds,
                protoIdItems, fieldIdItems, methodIdItems, classDefItems);
    }

    // MAGIC (8, U_BYTE)
    // "dex\n035\0"
    private void readMagic() {
        b.readBytes(magic);
        assert Arrays.equals(magic, REF_MAGIC) : "Not a DEX file";
    }

    // CHECKSUM (1, U_INT)
    private void readChecksum() {
        checksum = b.readUInt();
    }

    // SIGNATURE (20, U_BYTE)
    private void readSignature() {
        b.readBytes(signature);
    }

    // FILE_SIZE (1, U_INT)
    private void readFileSize() {
        fileSize = b.readUInt();
    }

    // HEADER_SIZE (1, U_INT), //0x70
    private void readHeaderSize() {
        headerSize = b.readUInt();
    }

    // ENDIAN_TAG (1, U_INT), //ENDIAN_CONSTANT
    private void readEndianTag() {
        endianTag = b.readUInt();
        // FIXME Support for big endian encoded dex files
        assert endianTag == ENDIAN_CONSTANT : "Byteorder NOT in little endian";
    }

    private void readSize(int attribute) {
        size[attribute] = b.readUInt();
    }

    private void readOffset(int attribute) {
        off[attribute] = b.readUInt();
    }

    // reads the string pool
    private void readStrings() {
        int nStrings = size[STRING_IDS];
        b.setPosition(off[STRING_IDS]); // the first string offset is here
        int[] stringDataOffsets = new int[nStrings];

        for (int i = 0; i < stringDataOffsets.length; i++) {
            stringDataOffsets[i] = b.readUInt();
        }

        stringPool = new String[nStrings];
        for (int i = 0; i < stringDataOffsets.length; i++) {
            b.setPosition(stringDataOffsets[i]); // set buffer to offset
            // Position
            int lenght = b.readUleb128(); // read uleb128
            byte[] values = new byte[lenght];
            b.readBytes(values);
            stringPool[i] = new String(values);
        }
    }

    private void readTypeIds() {
        int nTypes = size[TYPE_IDS];
        b.setPosition(off[TYPE_IDS]); // the first element is here
        typeIds = new int[nTypes];

        for (int i = 0; i < typeIds.length; i++) {
            typeIds[i] = b.readUInt();
        }
    }

    static class ProtIdItem {
        public int shorty_idx;
        public int return_type_idx;
        public int parameter_off;
    }

    private void readProtos() {
        int nProtos = size[PROTO_IDS];
        b.setPosition(off[PROTO_IDS]);
        protoIdItems = new ProtIdItem[nProtos];

        ProtIdItem item = null;
        for (int i = 0; i < protoIdItems.length; i++) {
            item = new ProtIdItem();
            item.shorty_idx = b.readUInt();
            item.return_type_idx = b.readUInt();
            item.parameter_off = b.readUInt();
            protoIdItems[i] = item;
        }
    }

    static class FieldIdItem {
        public int class_idx; // defining class : index of type_ids
        public int type_idx; // type of field : index of type_ids
        public int name_idx; // name of field : index into string id (or
        // directly stringpool)
    }

    private void readFields() {
        int nFields = size[FIELD_IDS];
        b.setPosition(off[FIELD_IDS]);
        fieldIdItems = new FieldIdItem[nFields];

        FieldIdItem item = null;
        for (int i = 0; i < fieldIdItems.length; i++) {
            item = new FieldIdItem();
            item.class_idx = b.readUShort();
            item.type_idx = b.readUShort();
            item.name_idx = b.readUInt();
            fieldIdItems[i] = item;
        }
    }

    static class MethodsIdItem {
        public int class_idx; // defining class : index of typeIds
        public int proto_idx; // proto of method : index of protoIdItems
        public int name_idx; // name of method : index into string id (or
        // directly stringpool)
    }

    private void readMethods() {
        int nMethods = size[METHOD_IDS];
        b.setPosition(off[METHOD_IDS]);
        methodIdItems = new MethodsIdItem[nMethods];

        MethodsIdItem item = null;
        for (int i = 0; i < methodIdItems.length; i++) {
            item = new MethodsIdItem();
            item.class_idx = b.readUShort();
            item.proto_idx = b.readUShort();
            item.name_idx = b.readUInt();
            methodIdItems[i] = item;
        }
    }

    public static class ClassDefItem {
        public int class_idx;
        public int access_flags;
        public int superclass_idx;
        public int interfaces_off;
        public int source_file_idx;
        public int annotations_off;
        public int class_data_off;
        public int static_values_off;
    }

    private void readClasses() {
        int nClassDefs = size[CLASS_DEFS];
        b.setPosition(off[CLASS_DEFS]);
        classDefItems = new ClassDefItem[nClassDefs];

        ClassDefItem item = null;
        for (int i = 0; i < classDefItems.length; i++) {
            item = new ClassDefItem();
            item.class_idx = b.readUInt();
            item.access_flags = b.readUInt();
            item.superclass_idx = b.readUInt();
            item.interfaces_off = b.readUInt();
            item.source_file_idx = b.readUInt();
            item.annotations_off = b.readUInt();
            item.class_data_off = b.readUInt();
            item.static_values_off = b.readUInt();
            classDefItems[i] = item;
        }
    }
}
