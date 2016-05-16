/*
 * Copyright (C) 2008 The Android Open Source Project
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
package android.graphics.cts;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas.VertexMode;
import android.test.AndroidTestCase;

public class Canvas_VertexModeTest extends AndroidTestCase {

    public void testValueOf(){
        assertEquals(VertexMode.TRIANGLES, VertexMode.valueOf("TRIANGLES"));
        assertEquals(VertexMode.TRIANGLE_STRIP, VertexMode.valueOf("TRIANGLE_STRIP"));
        assertEquals(VertexMode.TRIANGLE_FAN, VertexMode.valueOf("TRIANGLE_FAN"));
    }

    public void testValues(){
        VertexMode[] verMode = VertexMode.values();

        assertEquals(3, verMode.length);
        assertEquals(VertexMode.TRIANGLES, verMode[0]);
        assertEquals(VertexMode.TRIANGLE_STRIP, verMode[1]);
        assertEquals(VertexMode.TRIANGLE_FAN, verMode[2]);

        float[] verts = new float[10];
        float[] texs = new float[10];
        short[] indices = { 0, 1, 2, 3, 4, 1 };

        Bitmap bitmap = Bitmap.createBitmap(10, 27, Config.RGB_565);
        Canvas c = new Canvas(bitmap);

        //VertexMode is used as a argument here for all the methods that use it
        c.drawVertices( VertexMode.TRIANGLES,
                        2,
                        verts,
                        0,
                        texs,
                        0,
                        new int[]{10, 24},
                        0,
                        indices,
                        0,
                        4,
                        new Paint());

        c.drawVertices( VertexMode.TRIANGLE_STRIP,
                        2,
                        verts,
                        0,
                        texs,
                        0,
                        new int[]{10, 24},
                        0,
                        indices,
                        0,
                        4,
                        new Paint());

        c.drawVertices( VertexMode.TRIANGLE_FAN,
                        10,
                        verts,
                        0,
                        texs,
                        0,
                        null,
                        0,
                        null,
                        0,
                        0,
                        new Paint());
    }
}
