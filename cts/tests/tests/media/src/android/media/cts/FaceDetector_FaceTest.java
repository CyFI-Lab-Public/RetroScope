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


import android.content.Intent;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.test.InstrumentationTestCase;

import java.util.List;

public class FaceDetector_FaceTest extends InstrumentationTestCase {
    private FaceDetectorStub mActivity;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Intent intent = new Intent();
        intent.setClass(getInstrumentation().getTargetContext(), FaceDetectorStub.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(FaceDetectorStub.IMAGE_ID, R.drawable.single_face);
        mActivity = (FaceDetectorStub) getInstrumentation().startActivitySync(intent);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mActivity.finish();
    }

    public void testFaceProperties() throws Exception {
        long waitMsec = 5000;
        Thread.sleep(waitMsec);
        List<Face> detectedFaces = mActivity.getDetectedFaces();
        assertEquals(1, detectedFaces.size());
        Face face = detectedFaces.get(0);
        PointF eyesMP = new PointF();
        face.getMidPoint(eyesMP);
        float tolerance = 5f;
        float goodConfidence = 0.3f;
        assertTrue(face.confidence() >= goodConfidence);
        float eyesDistance = 20.0f;
        assertEquals(eyesDistance, face.eyesDistance(), tolerance);
        float eyesMidpointX = 60.0f;
        float eyesMidpointY = 60.0f;
        assertEquals(eyesMidpointX, eyesMP.x, tolerance);
        assertEquals(eyesMidpointY, eyesMP.y, tolerance);
        face.pose(FaceDetector.Face.EULER_X);
        face.pose(FaceDetector.Face.EULER_Y);
        face.pose(FaceDetector.Face.EULER_Z);

        int ErrorEuler = 100;
        try {
            face.pose(ErrorEuler);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}

