/*
 * Copyright (C) 2011-2012 The Android Open Source Project
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

package android.renderscript.cts;

import android.renderscript.Element;
import android.renderscript.Element.DataType;
import android.renderscript.Element.DataKind;
import android.renderscript.RSIllegalArgumentException;

public class ElementTest extends RSBaseCompute {

    public void testCreatePixel() {
        assertTrue(Element.createPixel(mRS,
                                       DataType.UNSIGNED_8,
                                       DataKind.PIXEL_A) != null);
        assertTrue(Element.createPixel(mRS,
                                       DataType.UNSIGNED_5_6_5,
                                       DataKind.PIXEL_RGB) != null);
        assertTrue(Element.createPixel(mRS,
                                       DataType.UNSIGNED_8,
                                       DataKind.PIXEL_RGB) != null);
        assertTrue(Element.createPixel(mRS,
                                       DataType.UNSIGNED_5_5_5_1,
                                       DataKind.PIXEL_RGBA) != null);
        assertTrue(Element.createPixel(mRS,
                                       DataType.UNSIGNED_4_4_4_4,
                                       DataKind.PIXEL_RGBA) != null);
        assertTrue(Element.createPixel(mRS,
                                       DataType.UNSIGNED_8,
                                       DataKind.PIXEL_RGBA) != null);
    }

    public void testCreateVector() {

        Element e = null;
        for (int len = 2; len <= 4; len ++) {
            assertTrue(Element.createVector(mRS, DataType.FLOAT_32, len) != null);
            assertTrue(Element.createVector(mRS, DataType.FLOAT_64, len) != null);
            assertTrue(Element.createVector(mRS, DataType.SIGNED_8, len) != null);
            assertTrue(Element.createVector(mRS, DataType.SIGNED_16, len) != null);
            assertTrue(Element.createVector(mRS, DataType.SIGNED_32, len) != null);
            assertTrue(Element.createVector(mRS, DataType.SIGNED_64, len) != null);
            assertTrue(Element.createVector(mRS, DataType.UNSIGNED_8, len) != null);
            assertTrue(Element.createVector(mRS, DataType.UNSIGNED_16, len) != null);
            assertTrue(Element.createVector(mRS, DataType.UNSIGNED_32, len) != null);
            assertTrue(Element.createVector(mRS, DataType.UNSIGNED_64, len) != null);
        }
    }

    public void testPrebuiltElements() {
        assertTrue(Element.A_8(mRS) != null);
        assertTrue(Element.RGBA_4444(mRS) != null);
        assertTrue(Element.RGBA_5551(mRS) != null);
        assertTrue(Element.RGB_565(mRS) != null);
        assertTrue(Element.RGB_888(mRS) != null);
        assertTrue(Element.RGBA_8888(mRS) != null);
        assertTrue(Element.F32(mRS) != null);
        assertTrue(Element.F32_2(mRS) != null);
        assertTrue(Element.F32_3(mRS) != null);
        assertTrue(Element.F32_4(mRS) != null);
        assertTrue(Element.F64(mRS) != null);
        assertTrue(Element.F64_2(mRS) != null);
        assertTrue(Element.F64_3(mRS) != null);
        assertTrue(Element.F64_4(mRS) != null);
        assertTrue(Element.I8(mRS) != null);
        assertTrue(Element.I8_2(mRS) != null);
        assertTrue(Element.I8_3(mRS) != null);
        assertTrue(Element.I8_4(mRS) != null);
        assertTrue(Element.I16(mRS) != null);
        assertTrue(Element.I16_2(mRS) != null);
        assertTrue(Element.I16_3(mRS) != null);
        assertTrue(Element.I16_4(mRS) != null);
        assertTrue(Element.I32(mRS) != null);
        assertTrue(Element.I32_2(mRS) != null);
        assertTrue(Element.I32_3(mRS) != null);
        assertTrue(Element.I32_4(mRS) != null);
        assertTrue(Element.I64(mRS) != null);
        assertTrue(Element.I64_2(mRS) != null);
        assertTrue(Element.I64_3(mRS) != null);
        assertTrue(Element.I64_4(mRS) != null);
        assertTrue(Element.U8(mRS) != null);
        assertTrue(Element.U8_2(mRS) != null);
        assertTrue(Element.U8_3(mRS) != null);
        assertTrue(Element.U8_4(mRS) != null);
        assertTrue(Element.U16(mRS) != null);
        assertTrue(Element.U16_2(mRS) != null);
        assertTrue(Element.U16_3(mRS) != null);
        assertTrue(Element.U16_4(mRS) != null);
        assertTrue(Element.U32(mRS) != null);
        assertTrue(Element.U32_2(mRS) != null);
        assertTrue(Element.U32_3(mRS) != null);
        assertTrue(Element.U32_4(mRS) != null);
        assertTrue(Element.U64(mRS) != null);
        assertTrue(Element.U64_2(mRS) != null);
        assertTrue(Element.U64_3(mRS) != null);
        assertTrue(Element.U64_4(mRS) != null);
        assertTrue(Element.MATRIX_2X2(mRS) != null);
        assertTrue(Element.MATRIX_3X3(mRS) != null);
        assertTrue(Element.MATRIX_4X4(mRS) != null);
        assertTrue(Element.MATRIX4X4(mRS) != null);
        assertTrue(Element.MESH(mRS) != null);
        assertTrue(Element.PROGRAM_FRAGMENT(mRS) != null);
        assertTrue(Element.PROGRAM_RASTER(mRS) != null);
        assertTrue(Element.PROGRAM_STORE(mRS) != null);
        assertTrue(Element.PROGRAM_VERTEX(mRS) != null);
        assertTrue(Element.ALLOCATION(mRS) != null);
        assertTrue(Element.SAMPLER(mRS) != null);
        assertTrue(Element.SCRIPT(mRS) != null);
        assertTrue(Element.TYPE(mRS) != null);
        assertTrue(Element.BOOLEAN(mRS) != null);
        assertTrue(Element.ELEMENT(mRS) != null);
    }

    public void testEquals() {
        assertTrue(Element.A_8(mRS).equals(Element.A_8(mRS)));
        assertFalse(Element.A_8(mRS).equals(Element.U8(mRS)));

        // Compatible elements
        assertTrue(Element.A_8(mRS).isCompatible(Element.U8(mRS)));
        assertTrue(Element.U8(mRS).isCompatible(Element.A_8(mRS)));

        // Verify equivalence of user-built Elements
        Element.Builder eb1 = new Element.Builder(mRS);
        eb1.add(Element.I8(mRS), "Test");
        Element e1 = eb1.add(Element.U8(mRS), "UTest").create();

        assertTrue(e1.equals(e1));

        Element.Builder eb2 = new Element.Builder(mRS);
        eb2.add(Element.I8(mRS), "Test");
        Element e2 = eb2.add(Element.U8(mRS), "UTest").create();

        assertTrue(e1.equals(e2));
        assertTrue(e2.equals(e1));
        assertTrue(e1.hashCode() == e2.hashCode());
        assertTrue(e1.isCompatible(e2));
        assertTrue(e2.isCompatible(e1));

        // Check name differences
        Element.Builder eb3 = new Element.Builder(mRS);
        eb3.add(Element.I8(mRS), "Test");
        Element e3 = eb3.add(Element.U8(mRS), "NotUTest").create();

        assertFalse(e1.equals(e3));
        assertFalse(e3.equals(e1));
        assertFalse(e1.isCompatible(e3));
        assertFalse(e3.isCompatible(e1));

        eb1.add(Element.I8(mRS), "Another");
        assertFalse(e1.equals(eb1.create()));
        assertFalse(eb1.create().equals(e1));

        // Compatible sub-elements but neither equal nor compatible
        Element.Builder eb4 = new Element.Builder(mRS);
        eb4.add(Element.I8(mRS), "Test");
        Element e4 = eb4.add(Element.A_8(mRS), "UTest").create();

        assertFalse(e1.equals(e4));
        assertFalse(e4.equals(e1));
        assertFalse(e1.isCompatible(e4));
        assertFalse(e4.isCompatible(e1));

        // Check identity from same builder
        assertTrue(eb1.create().isCompatible(eb1.create()));
    }

    public void testIsCompatible() {
        Element[] ALLOCATION = { Element.ALLOCATION(mRS) };
        // A_8 is in U8
        Element[] BOOLEAN = { Element.BOOLEAN(mRS) };
        Element[] ELEMENT = { Element.ELEMENT(mRS) };
        Element[] F32 = { Element.F32(mRS) };
        Element[] F32_2 = { Element.F32_2(mRS),
            Element.createVector(mRS, Element.DataType.FLOAT_32, 2) };
        Element[] F32_3 = { Element.F32_3(mRS),
            Element.createVector(mRS, Element.DataType.FLOAT_32, 3) };
        Element[] F32_4 = { Element.F32_4(mRS),
            Element.createVector(mRS, Element.DataType.FLOAT_32, 4) };
        Element[] F64 = { Element.F64(mRS) };
        Element[] I16 = { Element.I16(mRS) };
        Element[] I32 = { Element.I32(mRS) };
        Element[] I64 = { Element.I64(mRS) };
        Element[] I8 = { Element.I8(mRS) };
        // MATRIX4X4 is in MATRIX_4X4
        Element[] MATRIX_2X2 = { Element.MATRIX_2X2(mRS) };
        Element[] MATRIX_3X3 = { Element.MATRIX_3X3(mRS) };
        Element[] MATRIX_4X4 = { Element.MATRIX4X4(mRS),
                                 Element.MATRIX_4X4(mRS) };
        Element[] MESH = { Element.MESH(mRS) };
        Element[] PROGRAM_FRAGMENT = { Element.PROGRAM_FRAGMENT(mRS) };
        Element[] PROGRAM_RASTER = { Element.PROGRAM_RASTER(mRS) };
        Element[] PROGRAM_STORE = { Element.PROGRAM_STORE(mRS) };
        Element[] PROGRAM_VERTEX = { Element.PROGRAM_VERTEX(mRS) };
        Element[] RGBA_4444 = { Element.RGBA_4444(mRS),
            Element.createPixel(mRS, Element.DataType.UNSIGNED_4_4_4_4,
                                Element.DataKind.PIXEL_RGBA) };
        Element[] RGBA_5551 = { Element.RGBA_5551(mRS),
            Element.createPixel(mRS, Element.DataType.UNSIGNED_5_5_5_1,
                                Element.DataKind.PIXEL_RGBA) };
        // RGBA_8888 is in U8_4
        Element[] RGB_565 = { Element.RGB_565(mRS),
            Element.createPixel(mRS, Element.DataType.UNSIGNED_5_6_5,
                                Element.DataKind.PIXEL_RGB) };
        // RGB_888 is in U8_3
        Element[] SAMPLER = { Element.SAMPLER(mRS) };
        Element[] SCRIPT = { Element.SCRIPT(mRS) };
        Element[] TYPE = { Element.TYPE(mRS) };
        Element[] U16 = { Element.U16(mRS) };
        Element[] U32 = { Element.U32(mRS) };
        Element[] U64 = { Element.U64(mRS) };
        Element[] U8 = { Element.A_8(mRS),
                         Element.U8(mRS),
            Element.createPixel(mRS, Element.DataType.UNSIGNED_8,
                                Element.DataKind.PIXEL_A) };
        Element[] U8_3 = { Element.RGB_888(mRS),
            Element.createVector(mRS, Element.DataType.UNSIGNED_8, 3),
            Element.createPixel(mRS, Element.DataType.UNSIGNED_8,
                                Element.DataKind.PIXEL_RGB) };
        Element[] U8_4 = { Element.U8_4(mRS),
                           Element.RGBA_8888(mRS),
            Element.createVector(mRS, Element.DataType.UNSIGNED_8, 4),
            Element.createPixel(mRS, Element.DataType.UNSIGNED_8,
                                Element.DataKind.PIXEL_RGBA) };

        Element[][] ElementArrs = { ALLOCATION, BOOLEAN, ELEMENT, F32, F32_2,
                                    F32_3, F32_4, F64, I16, I32, I64, I8,
                                    MATRIX_2X2, MATRIX_3X3, MATRIX_4X4, MESH,
                                    PROGRAM_FRAGMENT, PROGRAM_RASTER,
                                    PROGRAM_STORE, PROGRAM_VERTEX, RGBA_4444,
                                    RGBA_5551, RGB_565, SAMPLER, SCRIPT, TYPE,
                                    U16, U32, U64, U8, U8_3, U8_4 };

        int ElementArrsLen = ElementArrs.length;
        for (int i = 0; i < ElementArrsLen; i++) {
            for (int j = 0; j < ElementArrsLen; j++) {
                for (Element el : ElementArrs[i]) {
                    for (Element er : ElementArrs[j]) {
                        if (i == j) {
                            // Elements within a group are compatible
                            assertTrue(el.isCompatible(er));
                        } else {
                            // Elements from different groups are incompatible
                            assertFalse(el.isCompatible(er));
                        }
                    }
                }
            }
        }
    }

    public void testElementBuilder() {
        for (int arraySize = 1; arraySize <= 3; arraySize++) {
            // Now test array size
            Element.Builder eb = new Element.Builder(mRS);
            eb.add(Element.A_8(mRS), "A_8", arraySize);
            eb.add(Element.RGBA_4444(mRS), "RGBA_4444", arraySize);
            eb.add(Element.RGBA_5551(mRS), "RGBA_5551", arraySize);
            eb.add(Element.RGB_565(mRS), "RGB_565", arraySize);
            eb.add(Element.RGB_888(mRS), "RGB_888", arraySize);
            eb.add(Element.RGBA_8888(mRS), "RGBA_8888", arraySize);
            eb.add(Element.F32(mRS), "F32", arraySize);
            eb.add(Element.F32_2(mRS), "F32_2", arraySize);
            eb.add(Element.F32_3(mRS), "F32_3", arraySize);
            eb.add(Element.F32_4(mRS), "F32_4", arraySize);
            eb.add(Element.F64(mRS), "F64", arraySize);
            eb.add(Element.F64_2(mRS), "F64_2", arraySize);
            eb.add(Element.F64_3(mRS), "F64_3", arraySize);
            eb.add(Element.F64_4(mRS), "F64_4", arraySize);
            eb.add(Element.I8(mRS), "I8", arraySize);
            eb.add(Element.I8_2(mRS), "I8_2", arraySize);
            eb.add(Element.I8_3(mRS), "I8_3", arraySize);
            eb.add(Element.I8_4(mRS), "I8_4", arraySize);
            eb.add(Element.I16(mRS), "I16", arraySize);
            eb.add(Element.I16_2(mRS), "I16_2", arraySize);
            eb.add(Element.I16_3(mRS), "I16_3", arraySize);
            eb.add(Element.I16_4(mRS), "I16_4", arraySize);
            eb.add(Element.I32(mRS), "I32", arraySize);
            eb.add(Element.I32_2(mRS), "I32_2", arraySize);
            eb.add(Element.I32_3(mRS), "I32_3", arraySize);
            eb.add(Element.I32_4(mRS), "I32_4", arraySize);
            eb.add(Element.I64(mRS), "I64", arraySize);
            eb.add(Element.I64_2(mRS), "I64_2", arraySize);
            eb.add(Element.I64_3(mRS), "I64_3", arraySize);
            eb.add(Element.I64_4(mRS), "I64_4", arraySize);
            eb.add(Element.U8(mRS), "U8", arraySize);
            eb.add(Element.U8_2(mRS), "U8_2", arraySize);
            eb.add(Element.U8_3(mRS), "U8_3", arraySize);
            eb.add(Element.U8_4(mRS), "U8_4", arraySize);
            eb.add(Element.U16(mRS), "U16", arraySize);
            eb.add(Element.U16_2(mRS), "U16_2", arraySize);
            eb.add(Element.U16_3(mRS), "U16_3", arraySize);
            eb.add(Element.U16_4(mRS), "U16_4", arraySize);
            eb.add(Element.U32(mRS), "U32", arraySize);
            eb.add(Element.U32_2(mRS), "U32_2", arraySize);
            eb.add(Element.U32_3(mRS), "U32_3", arraySize);
            eb.add(Element.U32_4(mRS), "U32_4", arraySize);
            eb.add(Element.U64(mRS), "U64", arraySize);
            eb.add(Element.U64_2(mRS), "U64_2", arraySize);
            eb.add(Element.U64_3(mRS), "U64_3", arraySize);
            eb.add(Element.U64_4(mRS), "U64_4", arraySize);
            eb.add(Element.MATRIX_2X2(mRS), "MATRIX_2X2", arraySize);
            eb.add(Element.MATRIX_3X3(mRS), "MATRIX_3X3", arraySize);
            eb.add(Element.MATRIX_4X4(mRS), "MATRIX_4X4", arraySize);
            eb.add(Element.MATRIX4X4(mRS), "MATRIX4X4", arraySize);
            eb.add(Element.MESH(mRS), "MESH", arraySize);
            eb.add(Element.PROGRAM_FRAGMENT(mRS), "PROGRAM_FRAGMENT", arraySize);
            eb.add(Element.PROGRAM_RASTER(mRS), "PROGRAM_RASTER", arraySize);
            eb.add(Element.PROGRAM_STORE(mRS), "PROGRAM_STORE", arraySize);
            eb.add(Element.PROGRAM_VERTEX(mRS), "PROGRAM_VERTEX", arraySize);
            eb.add(Element.ALLOCATION(mRS), "ALLOCATION", arraySize);
            eb.add(Element.SAMPLER(mRS), "SAMPLER", arraySize);
            eb.add(Element.SCRIPT(mRS), "SCRIPT", arraySize);
            eb.add(Element.TYPE(mRS), "TYPE", arraySize);
            eb.add(Element.BOOLEAN(mRS), "BOOLEAN", arraySize);
            eb.add(Element.ELEMENT(mRS), "ELEMENT", arraySize);
            assertTrue(eb.create() != null);
        }
    }

    public void testIsComplex() {
        assertFalse(Element.A_8(mRS).isComplex());
        assertFalse(Element.RGBA_4444(mRS).isComplex());
        assertFalse(Element.RGBA_5551(mRS).isComplex());
        assertFalse(Element.RGB_565(mRS).isComplex());
        assertFalse(Element.RGB_888(mRS).isComplex());
        assertFalse(Element.RGBA_8888(mRS).isComplex());
        assertFalse(Element.F32(mRS).isComplex());
        assertFalse(Element.F32_2(mRS).isComplex());
        assertFalse(Element.F32_3(mRS).isComplex());
        assertFalse(Element.F32_4(mRS).isComplex());
        assertFalse(Element.F64(mRS).isComplex());
        assertFalse(Element.F64_2(mRS).isComplex());
        assertFalse(Element.F64_3(mRS).isComplex());
        assertFalse(Element.F64_4(mRS).isComplex());
        assertFalse(Element.I8(mRS).isComplex());
        assertFalse(Element.I8_2(mRS).isComplex());
        assertFalse(Element.I8_3(mRS).isComplex());
        assertFalse(Element.I8_4(mRS).isComplex());
        assertFalse(Element.I16(mRS).isComplex());
        assertFalse(Element.I16_2(mRS).isComplex());
        assertFalse(Element.I16_3(mRS).isComplex());
        assertFalse(Element.I16_4(mRS).isComplex());
        assertFalse(Element.I32(mRS).isComplex());
        assertFalse(Element.I32_2(mRS).isComplex());
        assertFalse(Element.I32_3(mRS).isComplex());
        assertFalse(Element.I32_4(mRS).isComplex());
        assertFalse(Element.I64(mRS).isComplex());
        assertFalse(Element.I64_2(mRS).isComplex());
        assertFalse(Element.I64_3(mRS).isComplex());
        assertFalse(Element.I64_4(mRS).isComplex());
        assertFalse(Element.U8(mRS).isComplex());
        assertFalse(Element.U8_2(mRS).isComplex());
        assertFalse(Element.U8_3(mRS).isComplex());
        assertFalse(Element.U8_4(mRS).isComplex());
        assertFalse(Element.U16(mRS).isComplex());
        assertFalse(Element.U16_2(mRS).isComplex());
        assertFalse(Element.U16_3(mRS).isComplex());
        assertFalse(Element.U16_4(mRS).isComplex());
        assertFalse(Element.U32(mRS).isComplex());
        assertFalse(Element.U32_2(mRS).isComplex());
        assertFalse(Element.U32_3(mRS).isComplex());
        assertFalse(Element.U32_4(mRS).isComplex());
        assertFalse(Element.U64(mRS).isComplex());
        assertFalse(Element.U64_2(mRS).isComplex());
        assertFalse(Element.U64_3(mRS).isComplex());
        assertFalse(Element.U64_4(mRS).isComplex());
        assertFalse(Element.MATRIX_2X2(mRS).isComplex());
        assertFalse(Element.MATRIX_3X3(mRS).isComplex());
        assertFalse(Element.MATRIX_4X4(mRS).isComplex());
        assertFalse(Element.MATRIX4X4(mRS).isComplex());
        assertFalse(Element.MESH(mRS).isComplex());
        assertFalse(Element.PROGRAM_FRAGMENT(mRS).isComplex());
        assertFalse(Element.PROGRAM_RASTER(mRS).isComplex());
        assertFalse(Element.PROGRAM_STORE(mRS).isComplex());
        assertFalse(Element.PROGRAM_VERTEX(mRS).isComplex());
        assertFalse(Element.ALLOCATION(mRS).isComplex());
        assertFalse(Element.SAMPLER(mRS).isComplex());
        assertFalse(Element.SCRIPT(mRS).isComplex());
        assertFalse(Element.TYPE(mRS).isComplex());
        assertFalse(Element.BOOLEAN(mRS).isComplex());
        assertFalse(Element.ELEMENT(mRS).isComplex());

        Element.Builder eb = new Element.Builder(mRS);
        eb.add(Element.F32(mRS), "FLOAT");
        eb.add(Element.SAMPLER(mRS), "SAMPLER");
        Element e1 = eb.create();
        assertFalse(e1.isComplex());

        eb = new Element.Builder(mRS);
        eb.add(Element.A_8(mRS), "A_8");
        eb.add(Element.RGBA_4444(mRS), "RGBA_4444");
        Element e2 = eb.create();
        assertFalse(e2.isComplex());

        eb = new Element.Builder(mRS);
        eb.add(e1, "e1");
        eb.add(e2, "e2");
        assertTrue(eb.create().isComplex());
    }

    public void testDataType() {
        // Uncomment when NONE is no longer hidden.
        //assertEquals(DataType.NONE, DataType.valueOf("NONE"));

        assertEquals(DataType.FLOAT_32, DataType.valueOf("FLOAT_32"));
        assertEquals(DataType.FLOAT_64, DataType.valueOf("FLOAT_64"));
        assertEquals(DataType.SIGNED_8, DataType.valueOf("SIGNED_8"));
        assertEquals(DataType.SIGNED_16, DataType.valueOf("SIGNED_16"));
        assertEquals(DataType.SIGNED_32, DataType.valueOf("SIGNED_32"));
        assertEquals(DataType.SIGNED_64, DataType.valueOf("SIGNED_64"));
        assertEquals(DataType.UNSIGNED_8, DataType.valueOf("UNSIGNED_8"));
        assertEquals(DataType.UNSIGNED_16, DataType.valueOf("UNSIGNED_16"));
        assertEquals(DataType.UNSIGNED_32, DataType.valueOf("UNSIGNED_32"));
        assertEquals(DataType.UNSIGNED_64, DataType.valueOf("UNSIGNED_64"));

        assertEquals(DataType.BOOLEAN, DataType.valueOf("BOOLEAN"));

        assertEquals(DataType.UNSIGNED_5_6_5, DataType.valueOf("UNSIGNED_5_6_5"));
        assertEquals(DataType.UNSIGNED_5_5_5_1, DataType.valueOf("UNSIGNED_5_5_5_1"));
        assertEquals(DataType.UNSIGNED_4_4_4_4, DataType.valueOf("UNSIGNED_4_4_4_4"));

        assertEquals(DataType.MATRIX_4X4, DataType.valueOf("MATRIX_4X4"));
        assertEquals(DataType.MATRIX_3X3, DataType.valueOf("MATRIX_3X3"));
        assertEquals(DataType.MATRIX_2X2, DataType.valueOf("MATRIX_2X2"));

        assertEquals(DataType.RS_ELEMENT, DataType.valueOf("RS_ELEMENT"));
        assertEquals(DataType.RS_TYPE, DataType.valueOf("RS_TYPE"));
        assertEquals(DataType.RS_ALLOCATION, DataType.valueOf("RS_ALLOCATION"));
        assertEquals(DataType.RS_SAMPLER, DataType.valueOf("RS_SAMPLER"));
        assertEquals(DataType.RS_SCRIPT, DataType.valueOf("RS_SCRIPT"));
        assertEquals(DataType.RS_MESH, DataType.valueOf("RS_MESH"));
        assertEquals(DataType.RS_PROGRAM_FRAGMENT, DataType.valueOf("RS_PROGRAM_FRAGMENT"));
        assertEquals(DataType.RS_PROGRAM_VERTEX, DataType.valueOf("RS_PROGRAM_VERTEX"));
        assertEquals(DataType.RS_PROGRAM_RASTER, DataType.valueOf("RS_PROGRAM_RASTER"));
        assertEquals(DataType.RS_PROGRAM_STORE, DataType.valueOf("RS_PROGRAM_STORE"));
        // Make sure no new enums are added
        assertEquals(29, DataType.values().length);

        for (DataType dt : DataType.values()) {
            switch (dt) {
            case FLOAT_32:
            case FLOAT_64:
            case SIGNED_8:
            case SIGNED_16:
            case SIGNED_32:
            case SIGNED_64:
            case UNSIGNED_8:
            case UNSIGNED_16:
            case UNSIGNED_32:
            case UNSIGNED_64:
            case BOOLEAN:
                Element.createVector(mRS, dt, 2);
                break;

            default: {
                try {
                    Element.createVector(mRS, dt, 2);
                    fail("should throw RSIllegalArgumentException");
                } catch (RSIllegalArgumentException e) {
                }
                break;
            }
            }
        }
    }

    public void testDataKind() {
        assertEquals(DataKind.USER, DataKind.valueOf("USER"));

        assertEquals(DataKind.PIXEL_L, DataKind.valueOf("PIXEL_L"));
        assertEquals(DataKind.PIXEL_A, DataKind.valueOf("PIXEL_A"));
        assertEquals(DataKind.PIXEL_LA, DataKind.valueOf("PIXEL_LA"));
        assertEquals(DataKind.PIXEL_RGB, DataKind.valueOf("PIXEL_RGB"));
        assertEquals(DataKind.PIXEL_RGBA, DataKind.valueOf("PIXEL_RGBA"));
        assertEquals(DataKind.PIXEL_DEPTH, DataKind.valueOf("PIXEL_DEPTH"));
        assertEquals(DataKind.PIXEL_YUV, DataKind.valueOf("PIXEL_YUV"));
        // Make sure no new enums are added
        assertEquals(8, DataKind.values().length);

        for (DataKind dk : DataKind.values()) {
            if (dk != DataKind.USER && dk != DataKind.PIXEL_YUV) {
                Element.createPixel(mRS, DataType.UNSIGNED_8, dk);
            }
        }
    }
}


