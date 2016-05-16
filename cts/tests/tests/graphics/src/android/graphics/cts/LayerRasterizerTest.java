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

import junit.framework.TestCase;
import android.graphics.LayerRasterizer;
import android.graphics.Paint;

public class LayerRasterizerTest extends TestCase {

    public void testConstructor() {

        // new the LayerRasterizer instance
        new LayerRasterizer();
    }

    public void testAddLayer1() {
        // new the LayerRasterizer instance
        LayerRasterizer layerRasterizer = new LayerRasterizer();
        Paint p = new Paint();
        layerRasterizer.addLayer(p);
        // this function called a native function and this test just make sure
        // it doesn't throw out any exception.
    }

    public void testAddLayer2() {
        // new the LayerRasterizer instance
        LayerRasterizer layerRasterizer = new LayerRasterizer();
        layerRasterizer.addLayer(new Paint(), 1.0f, 1.0f);
        // this function called a native function and this test just make sure
        // it doesn't throw out any exception.
    }

}
