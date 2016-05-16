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

package dex.structure;

/**
 * {@code DexEncodedValueType} represents the type of an {code DexEncodedValue}.
 */
public enum DexEncodedValueType {
    /**
     * <pre>
     * VALUE_BYTE 0x00 (none; must be 0) ubyte[1]
     * </pre>
     * 
     * signed one-byte integer value
     */
    VALUE_BYTE((byte) 0x00),
    /**
     * <pre>
     * VALUE_SHORT 0x02 size - 1 (0...1) ubyte[size]
     * </pre>
     * 
     * signed two-byte integer value, sign-extended
     */
    VALUE_SHORT((byte) 0x02),
    /**
     * <pre>
     * VALUE_CHAR 0x03 size - 1 (0...1) ubyte[size]
     * </pre>
     * 
     * unsigned two-byte integer value, zero-extended
     */
    VALUE_CHAR((byte) 0x03),
    /**
     * <pre>
     * VALUE_INT 0x04 size - 1 (0...3) ubyte[size]
     * </pre>
     * 
     * signed four-byte integer value, sign-extended
     */
    VALUE_INT((byte) 0x04),
    /**
     * <pre>
     * VALUE_LONG 0x06 size - 1 (0...7) ubyte[size]
     * </pre>
     * 
     * signed eight-byte integer value, sign-extended
     */
    VALUE_LONG((byte) 0x06),
    /**
     * <pre>
     * VALUE_FLOAT 0x10 size - 1 (0...3) ubyte[size]
     * </pre>
     * 
     * four-byte bit pattern, zero-extended to the right, and interpreted as an
     * IEEE754 32-bit floating point value
     */
    VALUE_FLOAT((byte) 0x10),
    /**
     * <pre>
     * VALUE_DOUBLE 0x11 size - 1 (0...7) ubyte[size]
     * </pre>
     * 
     * eight-byte bit pattern, zero-extended to the right, and interpreted as an
     * IEEE754 64-bit floating point value
     */
    VALUE_DOUBLE((byte) 0x11),
    /**
     * <pre>
     * VALUE_STRING    0x17     size - 1 (0...3)     ubyte[size]
     * </pre>
     * 
     * unsigned (zero-extended) four-byte integer value, interpreted as an index
     * into the string_ids section and representing a string value
     */
    VALUE_STRING((byte) 0x17),
    /**
     * <pre>
     * VALUE_TYPE 0x18 size - 1 (0...3) ubyte[size]
     * </pre>
     * 
     * unsigned (zero-extended) four-byte integer value, interpreted as an index
     * into the type_ids section and representing a reflective type/class value
     */
    VALUE_TYPE((byte) 0x18),
    /**
     * <pre>
     * VALUE_FIELD 0x19 size - 1 (0...3) ubyte[size]
     * </pre>
     * 
     * unsigned (zero-extended) four-byte integer value, interpreted as an index
     * into the field_ids section and representing a reflective field value
     */
    VALUE_FIELD((byte) 0x19),
    /**
     * <pre>
     * VALUE_METHOD 0x1a size - 1 (0...3) ubyte[size]
     * </pre>
     * 
     * unsigned (zero-extended) four-byte integer value, interpreted as an index
     * into the method_ids section and representing a reflective method value
     */
    VALUE_METHOD((byte) 0x1a),
    /**
     * <pre>
     * VALUE_ENUM 0x1b size - 1 (0...3) ubyte[size]
     * </pre>
     * 
     * unsigned (zero-extended) four-byte integer value, interpreted as an index
     * into the field_ids section and representing the value of an enumerated
     * type constant
     */
    VALUE_ENUM((byte) 0x1b),
    /**
     * <pre>
     * VALUE_ARRAY 0x1c (none; must be 0) encoded_array
     * </pre>
     * 
     * an array of values, in the format specified by "encoded_array Format"
     * below. The size of the value is implicit in the encoding.
     */
    VALUE_ARRAY((byte) 0x1c),
    /**
     * <pre>
     * VALUE_ANNOTATION 0x1d (none; must be 0) encoded_annotation
     * </pre>
     * 
     * a sub-annotation, in the format specified by "encoded_annotation Format"
     * below. The size of the value is implicit in the encoding.
     */
    VALUE_ANNOTATION((byte) 0x1d),
    /**
     * <pre>
     * VALUE_NULL 0x1e (none; must be 0) (none)
     * </pre>
     * 
     * null reference value
     */
    VALUE_NULL((byte) 0x1e),
    /**
     * <pre>
     * VALUE_BOOLEAN 0x1f boolean (0...1) (none)
     * </pre>
     * 
     * one-bit value; 0 for false and 1 for true. The bit is represented in the
     * value_arg.
     */
    VALUE_BOOLEAN((byte) 0x1f);

    private byte value;

    /**
     * Creates a new instance of {@code DexEncodedValueType} using the provided
     * byte.
     * <p>
     * Format: value := (value_arg << 5) | value_type
     * 
     * @param value
     *            the {@code byte} containing the type and the value argument
     */
    private DexEncodedValueType(byte value) {
        this.value = value;
    }

    /**
     * Returns the {@code DexEncodedValueType} for the given {@code byte}.
     * 
     * @param value
     *            the {@code byte} containing the type and the value argument
     * @return the {@code DexEncodedValueType} for the given {@code byte}
     */
    public static DexEncodedValueType get(byte value) {
        // FIXME don't loop -> switch to get performance boost
        for (DexEncodedValueType type : values()) {
            if (type.value == (value & 0x1F)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Type does not exist!");
    }

    /**
     * Returns the value argument of the given {@code byte}.
     * <p>
     * Format: value := (value_arg << 5) | value_type
     * 
     * @param value
     *            the {@code byte} containing the type and the value argument
     * @return the value argument of the given {@code byte}
     */
    public static byte valueArg(byte value) {
        return (byte) (value >>> 5);
    }
}
