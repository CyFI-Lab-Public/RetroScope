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

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.res.Resources;
import android.renderscript.*;

import com.android.cts.stub.R;

// Renderscript activity
public class RenderscriptGLStubActivity extends Activity {
    class StubActivityRS {
        private Resources mRes;
        private RenderScriptGL mRS;

        private ScriptC mScript;

        public StubActivityRS() {
        }

        // This provides us with the renderscript context and resources that
        // allow us to create the script that does rendering
        public void init(RenderScriptGL rs, Resources res) {
            mRS = rs;
            mRes = res;
            initRS();
        }

        private void initRS() {
            mScript = new ScriptC_stub_activity(mRS, mRes, R.raw.stub_activity);
            mRS.bindRootScript(mScript);
        }
    }

    class HelloWorldView extends RSSurfaceView {
        // Renderscipt context
        private RenderScriptGL mRS;
        // Script that does the rendering
        private StubActivityRS mRender;

        public HelloWorldView(Context context) {
            super(context);
            ensureRenderScript();
        }

        private void ensureRenderScript() {
            if (mRS == null) {
                // Initialize renderscript with desired surface characteristics.
                // In this case, just use the defaults
                RenderScriptGL.SurfaceConfig sc = new RenderScriptGL.SurfaceConfig();
                mRS = createRenderScriptGL(sc);
                // Create an instance of the script that does the rendering
                mRender = new StubActivityRS();
                mRender.init(mRS, getResources());
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            ensureRenderScript();
        }

        @Override
        protected void onDetachedFromWindow() {
            // Handle the system event and clean up
            mRender = null;
            if (mRS != null) {
                mRS = null;
                destroyRenderScriptGL();
            }
        }

        public void forceDestroy() {
            onDetachedFromWindow();
        }
    }

    // Custom view to use with RenderScript
    private HelloWorldView mView;
    private HelloWorldView mView2;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Create our view and set it as the content of our Activity
        mView = new HelloWorldView(this);
        setContentView(mView);
    }

    public void recreateView() {
        HelloWorldView oldView = mView;
        mView = new HelloWorldView(this);
        setContentView(mView);
        oldView.forceDestroy();
    }

    public void destroyAll() {
        if (mView != null) {
            mView.forceDestroy();
        }
        if (mView2 != null) {
            mView2.forceDestroy();
        }
    }

    public void recreateMultiView() {
        HelloWorldView oldView = mView;
        mView = new HelloWorldView(this);
        mView2 = new HelloWorldView(this);
        setContentView(mView);
        setContentView(mView2);
        oldView.forceDestroy();
    }

    @Override
    protected void onResume() {
        // Ideally an app should implement onResume() and onPause()
        // to take appropriate action when the activity loses focus
        super.onResume();
        mView.resume();
    }

    @Override
    protected void onPause() {
        // Ideally an app should implement onResume() and onPause()
        // to take appropriate action when the activity loses focus
        super.onPause();
        mView.pause();
    }

}

