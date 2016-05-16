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

package android.hardware.cts;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import com.android.cts.stub.R;

public class CameraStubActivity extends Activity {
    private SurfaceView mSurfaceView;
    private final int LAYOUT_WIDTH = 480;
    private final int LAYOUT_HEIGHT = 320;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.surface_view);
        mSurfaceView = (SurfaceView)findViewById(R.id.surface_view);
        ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
        lp.width = LAYOUT_WIDTH;
        lp.height = LAYOUT_HEIGHT;
        mSurfaceView.setLayoutParams(lp);
        mSurfaceView.getHolder().setFixedSize(LAYOUT_WIDTH, LAYOUT_HEIGHT);
        mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public SurfaceView getSurfaceView() {
        return mSurfaceView;
    }
}
