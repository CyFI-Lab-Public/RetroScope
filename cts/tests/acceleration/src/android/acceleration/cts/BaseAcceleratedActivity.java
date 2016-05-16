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

package android.acceleration.cts;

import com.android.cts.acceleration.stub.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

abstract class BaseAcceleratedActivity extends Activity {

    private AcceleratedView mHardwareAcceleratedView;
    private AcceleratedView mSoftwareAcceleratedView;

    private AcceleratedView mManualHardwareAcceleratedView;
    private AcceleratedView mManualSoftwareAcceleratedView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acceleration);

        mHardwareAcceleratedView = (AcceleratedView) findViewById(R.id.hardware_accelerated_view);
        mSoftwareAcceleratedView = (AcceleratedView) findViewById(R.id.software_accelerated_view);

        mManualHardwareAcceleratedView =
            (AcceleratedView) findViewById(R.id.manual_hardware_accelerated_view);
        mManualSoftwareAcceleratedView =
            (AcceleratedView) findViewById(R.id.manual_software_accelerated_view);

        mManualHardwareAcceleratedView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mManualSoftwareAcceleratedView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public AcceleratedView getHardwareAcceleratedView() {
        return mHardwareAcceleratedView;
    }

    public AcceleratedView getSoftwareAcceleratedView() {
        return mSoftwareAcceleratedView;
    }

    public AcceleratedView getManualHardwareAcceleratedView() {
        return mManualHardwareAcceleratedView;
    }

    public AcceleratedView getManualSoftwareAcceleratedView() {
        return mManualSoftwareAcceleratedView;
    }
}
