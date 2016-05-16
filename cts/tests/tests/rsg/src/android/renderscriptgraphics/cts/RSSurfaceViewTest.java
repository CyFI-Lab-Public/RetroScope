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

import android.renderscript.RSSurfaceView;
import android.renderscript.RenderScriptGL;
import android.renderscript.RenderScriptGL.SurfaceConfig;
import android.util.AttributeSet;

public class RSSurfaceViewTest extends RSBaseGraphics {

    public void testCreation() {
        RSSurfaceView view = new RSSurfaceView(mCtx);
        view = new RSSurfaceView(mCtx, null);
    }

    public void testCreateRenderScriptGL() {
        RSSurfaceView view = new RSSurfaceView(mCtx);
        RenderScriptGL rs = view.createRenderScriptGL(new SurfaceConfig());
        assertTrue(rs != null);
    }

    public void testGetSetRenderScriptGL() {
        RSSurfaceView view = new RSSurfaceView(mCtx);
        RenderScriptGL rs = view.createRenderScriptGL(new SurfaceConfig());
        assertTrue(rs != null);
        assertEquals(view.getRenderScriptGL(), rs);

        view = new RSSurfaceView(mCtx);
        view.setRenderScriptGL(mRS);
        assertEquals(view.getRenderScriptGL(), mRS);
    }

    public void testDestroyRenderScriptGL() {
        RSSurfaceView view = new RSSurfaceView(mCtx);
        RenderScriptGL rs = view.createRenderScriptGL(new SurfaceConfig());
        assertTrue(rs != null);
        view.destroyRenderScriptGL();
        assertTrue(view.getRenderScriptGL() == null);
    }

    public void testPauseResume() {
        RSSurfaceView view = new RSSurfaceView(mCtx);
        view.pause();
        view.resume();

        view.setRenderScriptGL(mRS);
        view.pause();
        view.resume();
    }
}


