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
import android.test.AndroidTestCase;

import android.renderscript.RSIllegalArgumentException;
import android.renderscript.RenderScriptGL.SurfaceConfig;

public class SurfaceConfigTest extends AndroidTestCase {

    public void testSimpleCreate() {
        SurfaceConfig sc = new SurfaceConfig();
    }

    public void testSetColor() {
        SurfaceConfig sc = new SurfaceConfig();
        try {
            sc.setColor(-1, 8);
            fail("should throw RSIllegalArgumentException.");
        } catch (RSIllegalArgumentException e) {
        }
        sc = new SurfaceConfig();
        try {
            sc.setColor(9, 8);
            fail("should throw RSIllegalArgumentException.");
        } catch (RSIllegalArgumentException e) {
        }
        sc = new SurfaceConfig();
        try {
            sc.setColor(5, -1);
            fail("should throw RSIllegalArgumentException.");
        } catch (RSIllegalArgumentException e) {
        }
        sc = new SurfaceConfig();
        sc.setColor(5, 8);
        sc = new SurfaceConfig();
        sc.setColor(8, 8);
    }

    public void testSetAlpha() {
        SurfaceConfig sc = new SurfaceConfig();
        try {
            sc.setAlpha(-1, 8);
            fail("should throw RSIllegalArgumentException.");
        } catch (RSIllegalArgumentException e) {
        }
        sc = new SurfaceConfig();
        try {
            sc.setAlpha(9, 8);
            fail("should throw RSIllegalArgumentException.");
        } catch (RSIllegalArgumentException e) {
        }
        sc = new SurfaceConfig();
        try {
            sc.setAlpha(0, -1);
            fail("should throw RSIllegalArgumentException.");
        } catch (RSIllegalArgumentException e) {
        }
        sc = new SurfaceConfig();
        sc.setAlpha(0, 8);
        sc = new SurfaceConfig();
        sc.setAlpha(8, 8);
    }

    public void testSetDepth() {
        SurfaceConfig sc = new SurfaceConfig();
        try {
            sc.setDepth(-1, 8);
            fail("should throw RSIllegalArgumentException.");
        } catch (RSIllegalArgumentException e) {
        }
        sc = new SurfaceConfig();
        try {
            sc.setDepth(45, 8);
            fail("should throw RSIllegalArgumentException.");
        } catch (RSIllegalArgumentException e) {
        }
        sc = new SurfaceConfig();
        try {
            sc.setDepth(0, -1);
            fail("should throw RSIllegalArgumentException.");
        } catch (RSIllegalArgumentException e) {
        }
        sc = new SurfaceConfig();
        sc.setDepth(0, 16);
        sc = new SurfaceConfig();
        sc.setDepth(16, 24);
        sc = new SurfaceConfig();
        sc.setDepth(24, 24);
    }

    public void testSetSamples() {
        SurfaceConfig sc = new SurfaceConfig();
        try {
            sc.setSamples(-1, 8, 1.0f);
            fail("should throw RSIllegalArgumentException.");
        } catch (RSIllegalArgumentException e) {
        }
        sc = new SurfaceConfig();
        try {
            sc.setSamples(45, 8, 1.0f);
            fail("should throw RSIllegalArgumentException.");
        } catch (RSIllegalArgumentException e) {
        }
        sc = new SurfaceConfig();
        try {
            sc.setSamples(1, -1, 1.0f);
            fail("should throw RSIllegalArgumentException.");
        } catch (RSIllegalArgumentException e) {
        }
        sc = new SurfaceConfig();
        try {
            sc.setSamples(1, 1, -1.0f);
            fail("should throw RSIllegalArgumentException.");
        } catch (RSIllegalArgumentException e) {
        }
        sc = new SurfaceConfig();
        try {
            sc.setSamples(1, 1, 10.0f);
            fail("should throw RSIllegalArgumentException.");
        } catch (RSIllegalArgumentException e) {
        }
        sc = new SurfaceConfig();
        sc.setSamples(1, 4, 1.0f);
        sc = new SurfaceConfig();
        sc.setSamples(4, 32, 1.0f);
        sc = new SurfaceConfig();
        sc.setSamples(4, 64, 0.5f);
    }

    public void testCopyConstructor() {
        SurfaceConfig sc = new SurfaceConfig();
        sc.setAlpha(1, 7);
        sc.setColor(5, 8);
        sc.setDepth(0, 16);
        sc.setSamples(1, 4, 0.71f);
        SurfaceConfig sc2 = new SurfaceConfig(sc);
    }

}
