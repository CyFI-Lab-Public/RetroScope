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
import android.graphics.BitmapShader;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.test.AndroidTestCase;

public class ShaderTest extends AndroidTestCase {
    public void testConstructor() {
        new Shader();
    }

    public void testAccessLocalMatrix() {
        int width = 80;
        int height = 120;
        int[] color = new int[width * height];
        Bitmap bitmap = Bitmap.createBitmap(color, width, height, Bitmap.Config.RGB_565);

        Shader shader = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Matrix m = new Matrix();

        shader.setLocalMatrix(m);
        assertFalse(shader.getLocalMatrix(m));

        shader.setLocalMatrix(null);
        assertFalse(shader.getLocalMatrix(m));
    }
}
