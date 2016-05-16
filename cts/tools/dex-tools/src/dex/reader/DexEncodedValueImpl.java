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

import static dex.structure.DexEncodedValueType.*;
import dex.reader.DexFileReader.FieldIdItem;
import dex.structure.DexAnnotation;
import dex.structure.DexEncodedValue;
import dex.structure.DexEncodedValueType;

import java.util.ArrayList;
import java.util.List;

/* package */final class DexEncodedValueImpl implements DexEncodedValue {

    private final DexBuffer buffer;
    private byte typeAndValueArg;
    private DexEncodedValueType type;
    private String[] stringPool;
    private Object value;
    private int[] typeIds;
    private final FieldIdItem[] fieldIdItems;
    private final DexAnnotation annotation;

    /**
     * 
     * @param buffer
     *            the buffer with the correct position
     * @param annotation
     * @param stringPool
     * @param fieldIdItems
     */
    public DexEncodedValueImpl(DexBuffer buffer, DexAnnotation annotation,
            int[] typeIds, String[] stringPool, FieldIdItem[] fieldIdItems) {
        this.buffer = buffer;
        this.annotation = annotation;
        this.typeIds = typeIds;
        this.stringPool = stringPool;
        this.fieldIdItems = fieldIdItems;
        parseValue();
    }

    private void parseValue() {
        typeAndValueArg = buffer.readUByte();
        type = DexEncodedValueType.get(typeAndValueArg);
        int valueArg = DexEncodedValueType.valueArg(typeAndValueArg);
        switch (type) {
        case VALUE_BYTE:
            value = getByteValue(valueArg);
            break;
        case VALUE_SHORT:
            value = getShortValue(valueArg);
            break;
        case VALUE_CHAR:
            value = getCharValue(valueArg);
            break;
        case VALUE_INT:
            value = getIntValue(valueArg);
            break;
        case VALUE_LONG:
            value = getLongValue(valueArg);
            break;
        case VALUE_FLOAT:
            value = getFloatValue(valueArg);
            break;
        case VALUE_DOUBLE:
            value = getDoubleValue(valueArg);
            break;
        case VALUE_STRING:
            value = getStringValue(valueArg);
            break;
        case VALUE_TYPE:
            value = getTypeValue(valueArg);
            break;
        case VALUE_FIELD:
            value = getFieldValue(valueArg);
            break;
        case VALUE_METHOD:
            value = getMethodValue(valueArg);
            break;
        case VALUE_ENUM:
            value = getEnumValue(valueArg);
            break;
        case VALUE_ARRAY:
            value = getArrayValue(valueArg);
            break;
        case VALUE_ANNOTATION:
            value = getAnnotationValue(valueArg);
            break;
        case VALUE_NULL:
            value = getNullValue(valueArg);
            break;
        case VALUE_BOOLEAN:
            value = getBooleanValue(valueArg);
            break;
        default:
            throw new IllegalArgumentException("DexEncodedValueType " + type
                    + " not recognized");
        }
    }

    /**
     * VALUE_BOOLEAN 0x1f boolean (0...1) (none) one-bit value; 0 for false and
     * 1 for true. The bit is represented in the value_arg.
     */
    private Boolean getBooleanValue(int valueArg) {
        return valueArg == 1;
    }

    /** VALUE_NULL 0x1e (none; must be 0) (none) null reference value */
    private Object getNullValue(int valueArg) {
        return null; // must be like that!
    }

    /**
     * VALUE_ANNOTATION 0x1d (none; must be 0) encoded_annotation a
     * sub-annotation, in the format specified by "encoded_annotation Format"
     * below. The size of the value is implicit in the encoding.
     */
    private Object getAnnotationValue(int valueArg) {
        // use the buffer directly to get adjusted offset
        return new DexEncodedAnnotationImpl(buffer, annotation, typeIds,
                stringPool, fieldIdItems);
    }

    /**
     * VALUE_ARRAY 0x1c (none; must be 0) encoded_array an array of values, in
     * the format specified by "encoded_array Format" below. The size of the
     * value is implicit in the encoding.
     */
    private List<DexEncodedValue> getArrayValue(int valueArg) {
        int size = buffer.readUleb128();
        List<DexEncodedValue> values = new ArrayList<DexEncodedValue>(size);
        for (int i = 0; i < size; i++) {
            values.add(new DexEncodedValueImpl(buffer, annotation, typeIds,
                    stringPool, fieldIdItems));
        }
        return values;
    }

    /**
     * VALUE_ENUM 0x1b size - 1 (0...3) ubyte[size] unsigned (zero-extended)
     * four-byte integer value, interpreted as an index into the field_ids
     * section and representing the value of an enumerated type constant
     */
    private Object getEnumValue(int valueArg) {
        int fieldOffset = buffer.readInt(valueArg + 1);
        FieldIdItem fieldIdItem = fieldIdItems[fieldOffset];
        // FORMAT La/b/E;!CONSTANT
        String constantName = stringPool[fieldIdItem.name_idx];
        String typeName = stringPool[typeIds[fieldIdItem.type_idx]];
        return typeName + "!" + constantName;
    }

    /**
     * VALUE_METHOD 0x1a size - 1 (0...3) ubyte[size] unsigned (zero-extended)
     * four-byte integer value, interpreted as an index into the method_ids
     * section and representing a reflective method value
     */
    private Object getMethodValue(int valueArg) {
        // FIXME lookup value
        buffer.skip(valueArg + 1);
        return null;
    }

    /**
     * VALUE_FIELD 0x19 size - 1 (0...3) ubyte[size] unsigned (zero-extended)
     * four-byte integer value, interpreted as an index into the field_ids
     * section and representing a reflective field value
     */
    private Object getFieldValue(int valueArg) {
        int fieldOffset = buffer.readInt(valueArg + 1);
        FieldIdItem fieldIdItem = fieldIdItems[fieldOffset];
        // FORMAT La/b/E;!CONSTANT
        String fieldName = stringPool[fieldIdItem.name_idx];
        String typeName = stringPool[typeIds[fieldIdItem.type_idx]];
        return typeName + "!" + fieldName;
    }

    /**
     * VALUE_TYPE 0x18 size - 1 (0...3) ubyte[size] unsigned (zero-extended)
     * four-byte integer value, interpreted as an index into the type_ids
     * section and representing a reflective type/class value
     */
    private Object getTypeValue(int valueArg) {
        valueArg++; // size - 1 (0...3)
        // FIXME SPEC!! states: unsigned (zero-extended) four-byte integer value
        return stringPool[typeIds[buffer.readInt(valueArg)]];
    }

    /**
     * VALUE_STRING 0x17 size - 1 (0...3) ubyte[size] unsigned (zero-extended)
     * four-byte integer value, interpreted as an index into the string_ids
     * section and representing a string value
     */
    private Object getStringValue(int valueArg) {
        valueArg++;
        return stringPool[buffer.readInt(valueArg)];
    }

    /**
     * VALUE_DOUBLE 0x11 size - 1 (0...7) ubyte[size] eight-byte bit pattern,
     * zero-extended to the right, and interpreted as an IEEE754 64-bit floating
     * point value
     */
    private Object getDoubleValue(int valueArg) {
        return buffer.readDouble(valueArg + 1);
    }

    /**
     * VALUE_FLOAT 0x10 size - 1 (0...3) ubyte[size] four-byte bit pattern,
     * zero-extended to the right, and interpreted as an IEEE754 32-bit floating
     * point value
     */
    private Float getFloatValue(int valueArg) {
        return buffer.readFloat(valueArg + 1);
    }

    /**
     * VALUE_LONG 0x06 size - 1 (0...7) ubyte[size] signed eight-byte integer
     * value, sign-extended
     */
    private Long getLongValue(int valueArg) {
        return buffer.readLong(valueArg + 1);
    }

    /**
     * VALUE_INT 0x04 size - 1 (0...3) ubyte[size] signed four-byte integer
     * value, sign-extended
     */
    private Integer getIntValue(int valueArg) {
        return buffer.readInt(valueArg + 1);
    }

    /**
     * VALUE_CHAR 0x03 size - 1 (0...1) ubyte[size] unsigned two-byte integer
     * value, zero-extended
     */
    private Character getCharValue(int valueArg) {
        return buffer.readChar(valueArg + 1);
    }

    /**
     * VALUE_SHORT 0x02 size - 1 (0...1) ubyte[size] signed two-byte integer
     * value, sign-extended
     */
    private Short getShortValue(int valueArg) {
        return buffer.readShort(valueArg + 1);
    }

    /**
     * VALUE_BYTE 0x00 (none; must be 0) ubyte[1] signed one-byte integer value
     */
    private Byte getByteValue(int valueArg) {
        assert valueArg == 0 : "Illegal valueArg for VALUE_BYTE: " + valueArg;
        return null;
    }

    public DexEncodedValueType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("=");
        if (type == VALUE_ARRAY) {
            if (getValue() instanceof List<?>) {
                List<?> values = (List<?>) getValue();
                for (Object object : values) {
                    DexEncodedValue val = (DexEncodedValue) object;
                    builder.append(val.getValue());
                }
            }
        } else {
            builder.append(getValue());
        }
        return builder.toString();
    }
}
