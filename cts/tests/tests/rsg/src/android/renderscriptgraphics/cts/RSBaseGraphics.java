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

import android.renderscript.RenderScriptGL;
import android.renderscript.RenderScriptGL.SurfaceConfig;

/**
 * Base RenderScript test class. This class provides a message handler and a
 * convenient way to wait for compute scripts to complete their execution.
 */
class RSBaseGraphics extends RSBase {
    RenderScriptGL mRS;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mRS = new RenderScriptGL(mCtx, new SurfaceConfig());
        mRS.setMessageHandler(mRsMessage);
    }

    @Override
    protected void tearDown() throws Exception {
        if (mRS != null) {
            mRS.destroy();
            mRS = null;
        }
        super.tearDown();
    }

}
