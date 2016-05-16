/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.renderscript.Type;

public class TypeTest extends RSBaseCompute {

    void testBuilderSizes(Type.Builder b, int min, int max) {
        for (int x = min; x < max; x ++) {
            for (int y = min; y < max; y ++) {
                b.setX(x).setY(y);
                b.create();
            }
        }
    }

    void testTypeBuilderHelper(Element e) {
        Type.Builder b = new Type.Builder(mRS, e);
        for (int mips = 0; mips <= 1; mips ++) {
            boolean useMips = (mips == 1);

            for (int faces = 0; faces <= 1; faces++) {
                boolean useFaces = (faces == 1);

                b.setMipmaps(useMips);
                b.setFaces(useFaces);
                testBuilderSizes(b, 1, 8);
            }
        }
    }

    public void testTypeBuilder() {
        testTypeBuilderHelper(Element.A_8(mRS));
        testTypeBuilderHelper(Element.RGB_565(mRS));
        testTypeBuilderHelper(Element.RGB_888(mRS));
        testTypeBuilderHelper(Element.RGBA_8888(mRS));
        testTypeBuilderHelper(Element.F32(mRS));
        testTypeBuilderHelper(Element.F32_2(mRS));
        testTypeBuilderHelper(Element.F32_3(mRS));
        testTypeBuilderHelper(Element.F32_4(mRS));
        testTypeBuilderHelper(Element.BOOLEAN(mRS));
        testTypeBuilderHelper(Element.F64(mRS));
        testTypeBuilderHelper(Element.I8(mRS));
        testTypeBuilderHelper(Element.I16(mRS));
        testTypeBuilderHelper(Element.I32(mRS));
        testTypeBuilderHelper(Element.I64(mRS));
        testTypeBuilderHelper(Element.U8(mRS));
        testTypeBuilderHelper(Element.U8_4(mRS));
        testTypeBuilderHelper(Element.U16(mRS));
        testTypeBuilderHelper(Element.U32(mRS));
        testTypeBuilderHelper(Element.U64(mRS));
        testTypeBuilderHelper(Element.MATRIX_2X2(mRS));
        testTypeBuilderHelper(Element.MATRIX_3X3(mRS));
        testTypeBuilderHelper(Element.MATRIX_4X4(mRS));
        testTypeBuilderHelper(Element.MESH(mRS));
        testTypeBuilderHelper(Element.PROGRAM_FRAGMENT(mRS));
        testTypeBuilderHelper(Element.PROGRAM_RASTER(mRS));
        testTypeBuilderHelper(Element.PROGRAM_STORE(mRS));
        testTypeBuilderHelper(Element.PROGRAM_VERTEX(mRS));
        testTypeBuilderHelper(Element.ALLOCATION(mRS));
        testTypeBuilderHelper(Element.SAMPLER(mRS));
        testTypeBuilderHelper(Element.SCRIPT(mRS));
        testTypeBuilderHelper(Element.TYPE(mRS));

        // Add some complex and struct types to test here
    }

    public void testGetCount() {
        Type.Builder b = new Type.Builder(mRS, Element.F32(mRS));
        for (int faces = 0; faces <= 1; faces++) {
            boolean useFaces = faces == 1;
            int faceMultiplier = useFaces ? 6 : 1;
            for (int x = 1; x < 8; x ++) {
                for (int y = 1; y < 8; y ++) {
                    b.setFaces(useFaces);
                    b.setX(x).setY(y);
                    Type t = b.create();
                    assertTrue(t.getCount() == x * y * faceMultiplier);
                }
            }
        }

        // Test mipmaps
        b.setFaces(false);
        b.setMipmaps(true);
        Type t = b.setX(8).setY(1).create();
        int expectedCount = 8 + 4 + 2 + 1;
        assertTrue(t.getCount() == expectedCount);

        t = b.setX(8).setY(8).create();
        expectedCount = 8*8 + 4*4 + 2*2 + 1;
        assertTrue(t.getCount() == expectedCount);

        t = b.setX(8).setY(4).create();
        expectedCount = 8*4 + 4*2 + 2*1 + 1;
        assertTrue(t.getCount() == expectedCount);

        t = b.setX(4).setY(8).create();
        assertTrue(t.getCount() == expectedCount);

        t = b.setX(7).setY(1).create();
        expectedCount = 7 + 3 + 1;
        assertTrue(t.getCount() == expectedCount);

        t = b.setX(7).setY(3).create();
        expectedCount = 7*3 + 3*1 + 1;
        assertTrue(t.getCount() == expectedCount);
    }

    public void testGetElement() {
        Type.Builder b = new Type.Builder(mRS, Element.F32(mRS));
        b.setX(1);
        assertTrue(b.create().getElement() == Element.F32(mRS));
    }

    public void testGetX() {
        Type.Builder b = new Type.Builder(mRS, Element.F32(mRS));
        b.setX(3);
        assertTrue(b.create().getX() == 3);
    }

    public void testGetY() {
        Type.Builder b = new Type.Builder(mRS, Element.F32(mRS));
        b.setX(3).setY(4);
        Type t = b.create();
        assertTrue(t.getX() == 3);
        assertTrue(t.getY() == 4);
    }

    public void testGetZ() {
        Type.Builder b = new Type.Builder(mRS, Element.F32(mRS));
        b.setX(3).setY(4);
        assertTrue(b.create().getZ() == 0);
    }

    public void testHasFaces() {
        Type.Builder b = new Type.Builder(mRS, Element.F32(mRS));
        b.setX(4).setY(4).setFaces(true);
        assertTrue(b.create().hasFaces());
        b.setFaces(false);
        assertFalse(b.create().hasFaces());
    }

    public void testGetMipmaps() {
        Type.Builder b = new Type.Builder(mRS, Element.F32(mRS));
        b.setX(4).setY(4).setMipmaps(true);
        assertTrue(b.create().hasMipmaps());
        b.setMipmaps(false);
        assertFalse(b.create().hasMipmaps());
    }

    public void testTypeCubemapFace() {
        assertEquals(Type.CubemapFace.NEGATIVE_X, Type.CubemapFace.valueOf("NEGATIVE_X"));
        assertEquals(Type.CubemapFace.NEGATIVE_Y, Type.CubemapFace.valueOf("NEGATIVE_Y"));
        assertEquals(Type.CubemapFace.NEGATIVE_Z, Type.CubemapFace.valueOf("NEGATIVE_Z"));
        assertEquals(Type.CubemapFace.POSITIVE_X, Type.CubemapFace.valueOf("POSITIVE_X"));
        assertEquals(Type.CubemapFace.POSITIVE_Y, Type.CubemapFace.valueOf("POSITIVE_Y"));
        assertEquals(Type.CubemapFace.POSITIVE_Z, Type.CubemapFace.valueOf("POSITIVE_Z"));
        // Legacy typo enums
        assertEquals(Type.CubemapFace.POSITVE_X, Type.CubemapFace.valueOf("POSITVE_X"));
        assertEquals(Type.CubemapFace.POSITVE_Y, Type.CubemapFace.valueOf("POSITVE_Y"));
        assertEquals(Type.CubemapFace.POSITVE_Z, Type.CubemapFace.valueOf("POSITVE_Z"));
        // Make sure no new enums are added
        assertEquals(9, Type.CubemapFace.values().length);
    }

    public void testEquals() {
        Type.Builder b1 = new Type.Builder(mRS, Element.F32(mRS));
        Type t1 = b1.setX(5).setY(5).create();

        Type.Builder b2 = new Type.Builder(mRS, Element.F32(mRS));
        Type t2 = b2.setX(5).setY(5).create();

        assertTrue(t1.equals(t2));
        assertTrue(t2.equals(t1));
        assertTrue(t1.hashCode() == t2.hashCode());

        t2 = b2.setX(4).create();
        assertFalse(t1.equals(t2));
        assertFalse(t2.equals(t1));
    }
}


