/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.media.cts;

import com.android.cts.media.R;

import android.app.Activity;
import android.media.FaceDetector.Face;
import android.os.Bundle;

import java.util.List;

public class FaceDetectorStub extends Activity {

    public static final String IMAGE_ID = "imageId";

    private FaceView mFaceView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int imageId = getIntent().getIntExtra(IMAGE_ID, R.drawable.single_face);
        mFaceView = new FaceView(this, imageId);
        setContentView(mFaceView);
    }

    public List<Face> getDetectedFaces() {
        return mFaceView.detectedFaces;
    }
}