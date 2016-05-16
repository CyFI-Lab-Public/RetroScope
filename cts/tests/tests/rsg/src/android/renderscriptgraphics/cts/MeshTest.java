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

package android.renderscriptgraphics.cts;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.Type;
import android.renderscript.Mesh;
import android.renderscript.Mesh.Primitive;
import android.renderscript.Mesh.AllocationBuilder;
import android.renderscript.Mesh.Builder;
import android.renderscript.Mesh.TriangleMeshBuilder;

public class MeshTest extends RSBaseGraphics {

    Allocation mAttrAlloc;
    Allocation mIndexAlloc;
    Element mPosElem;
    Type mPosType;
    Type mIndexType;
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Element.Builder eb = new Element.Builder(mRS);
        eb.add(Element.F32_4(mRS), "position");
        mPosElem = eb.create();
        Type.Builder typeB = new Type.Builder(mRS, mPosElem);
        mPosType = typeB.setX(3).create();
        typeB = new Type.Builder(mRS, Element.U16(mRS));
        mIndexType = typeB.setX(3).create();

        mAttrAlloc = Allocation.createSized(mRS, mPosElem, 3);
        mIndexAlloc = Allocation.createSized(mRS, Element.U16(mRS), 3);
    }

    public void testMeshAllocationBuilder() {
        Mesh.AllocationBuilder mab;
        for(Primitive prim : Primitive.values()) {
            mab = new Mesh.AllocationBuilder(mRS);
            mab.addVertexAllocation(mAttrAlloc);
            mab.getCurrentVertexTypeIndex();
            mab.addIndexSetType(prim);
            assertTrue(mab.create() != null);

            mab = new Mesh.AllocationBuilder(mRS);
            mab.addVertexAllocation(mAttrAlloc);
            mab.getCurrentVertexTypeIndex();
            mab.addIndexSetAllocation(mIndexAlloc, prim);
            mab.getCurrentIndexSetIndex();
            mab.addIndexSetType(prim);

            Mesh mesh = mab.create();
            assertTrue(mesh != null);
            assertTrue(mesh.getVertexAllocationCount() == 1);
            assertTrue(mesh.getVertexAllocation(0) == mAttrAlloc);
            assertTrue(mesh.getPrimitiveCount() == 2);
            assertTrue(mesh.getPrimitive(0) == prim);
            assertTrue(mesh.getPrimitive(1) == prim);
            assertTrue(mesh.getIndexSetAllocation(0) == mIndexAlloc);
        }
    }

    public void testMeshBuilder() {
        Mesh.Builder mb;
        for(Primitive prim : Primitive.values()) {
            mb = new Mesh.Builder(mRS,
                                  Allocation.USAGE_SCRIPT |
                                  Allocation.USAGE_GRAPHICS_VERTEX);
            mb.addVertexType(mPosElem, 3);
            mb.getCurrentVertexTypeIndex();
            mb.addIndexSetType(prim);
            Mesh mesh = mb.create();
            assertTrue(mesh != null);
            assertTrue(mesh.getVertexAllocationCount() != 0);

            mb = new Mesh.Builder(mRS,
                                  Allocation.USAGE_SCRIPT |
                                  Allocation.USAGE_GRAPHICS_VERTEX);
            mb.addVertexType(mPosElem, 3);
            mb.getCurrentVertexTypeIndex();
            mb.addIndexSetType(Element.U16(mRS), 3, prim);
            mb.getCurrentIndexSetIndex();
            mesh = mb.create();
            assertTrue(mesh != null);
            assertTrue(mesh.getVertexAllocationCount() != 0);
            assertTrue(mesh.getPrimitiveCount() != 0);

            mb = new Mesh.Builder(mRS,
                                  Allocation.USAGE_SCRIPT |
                                  Allocation.USAGE_GRAPHICS_VERTEX);
            mb.addVertexType(mPosElem, 3);
            mb.getCurrentVertexTypeIndex();
            mb.addIndexSetType(mIndexType, prim);
            mb.getCurrentIndexSetIndex();
            assertTrue(mb.create() != null);

            mb = new Mesh.Builder(mRS,
                                  Allocation.USAGE_SCRIPT |
                                  Allocation.USAGE_GRAPHICS_VERTEX);
            mb.addVertexType(mPosType);
            mb.getCurrentVertexTypeIndex();
            mb.addIndexSetType(prim);
            assertTrue(mb.create() != null);

            mb = new Mesh.Builder(mRS,
                                  Allocation.USAGE_SCRIPT |
                                  Allocation.USAGE_GRAPHICS_VERTEX);
            mb.addVertexType(mPosType);
            mb.getCurrentVertexTypeIndex();
            mb.addIndexSetType(Element.U16(mRS), 3, prim);
            mb.getCurrentIndexSetIndex();
            assertTrue(mb.create() != null);

            mb = new Mesh.Builder(mRS,
                                  Allocation.USAGE_SCRIPT |
                                  Allocation.USAGE_GRAPHICS_VERTEX);
            mb.addVertexType(mPosType);
            mb.getCurrentVertexTypeIndex();
            mb.addIndexSetType(mIndexType, prim);
            mb.getCurrentIndexSetIndex();
            assertTrue(mb.create() != null);
        }
    }

    void triangleMeshBuilderHelper(int size, int flags) {
        // Test various num vertices and triangles
        for (int numVerts = 3; numVerts < 100; numVerts += 15) {
            for (int numTries = 1; numTries < 100; numTries += 15) {
                Mesh.TriangleMeshBuilder tmb = new Mesh.TriangleMeshBuilder(mRS, size, flags);
                // Append all the vertices
                for (int numVertsI = 0; numVertsI < numVerts; numVertsI++) {
                    if (size == 2) {
                        tmb.addVertex(1.0f, 1.0f);
                    } else {
                        tmb.addVertex(1.0f, 1.0f, 1.0f);
                    }
                    if ((flags & TriangleMeshBuilder.COLOR) != 0) {
                        tmb.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                    }
                    if ((flags & TriangleMeshBuilder.NORMAL) != 0) {
                        tmb.setNormal(1.0f, 1.0f, 1.0f);
                    }
                    if ((flags & TriangleMeshBuilder.TEXTURE_0) != 0) {
                        tmb.setTexture(1.0f, 1.0f);
                    }
                }
                // Add triangles to index them
                for (int numTriesI = 0; numTriesI < numTries; numTriesI ++) {
                    tmb.addTriangle(0, 1, 2);
                }
                assertTrue(tmb.create(false) != null);
                assertTrue(tmb.create(true) != null);
            }
        }
    }

    public void testMeshTriangleMeshBuilder() {
        for (int size = 2; size <= 3; size ++) {
            triangleMeshBuilderHelper(size, 0);
            triangleMeshBuilderHelper(size, TriangleMeshBuilder.COLOR);
            triangleMeshBuilderHelper(size, TriangleMeshBuilder.COLOR |
                                            TriangleMeshBuilder.NORMAL);
            triangleMeshBuilderHelper(size, TriangleMeshBuilder.COLOR |
                                            TriangleMeshBuilder.TEXTURE_0);
            triangleMeshBuilderHelper(size, TriangleMeshBuilder.COLOR |
                                            TriangleMeshBuilder.NORMAL |
                                            TriangleMeshBuilder.TEXTURE_0);
            triangleMeshBuilderHelper(size, TriangleMeshBuilder.NORMAL);
            triangleMeshBuilderHelper(size, TriangleMeshBuilder.NORMAL|
                                            TriangleMeshBuilder.TEXTURE_0);
            triangleMeshBuilderHelper(size, TriangleMeshBuilder.TEXTURE_0);
        }
    }

    public void testMeshPrimitive() {
        assertEquals(Mesh.Primitive.POINT, Mesh.Primitive.valueOf("POINT"));
        assertEquals(Mesh.Primitive.LINE, Mesh.Primitive.valueOf("LINE"));
        assertEquals(Mesh.Primitive.LINE_STRIP, Mesh.Primitive.valueOf("LINE_STRIP"));
        assertEquals(Mesh.Primitive.TRIANGLE, Mesh.Primitive.valueOf("TRIANGLE"));
        assertEquals(Mesh.Primitive.TRIANGLE_STRIP, Mesh.Primitive.valueOf("TRIANGLE_STRIP"));
        assertEquals(Mesh.Primitive.TRIANGLE_FAN, Mesh.Primitive.valueOf("TRIANGLE_FAN"));
        // Make sure no new enums are added
        assertEquals(6, Mesh.Primitive.values().length);
    }
}


