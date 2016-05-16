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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.view.View;

import java.util.ArrayList;

public class FaceView extends View {
    private static final int NUM_FACES = 10;
    private FaceDetector mFaceDetector;
    private Face[] mAllFaces = new Face[NUM_FACES];
    private Bitmap mSourceImage;
    private Paint mTmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mPOuterBullsEye = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mPInnerBullsEye = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int mPicWidth;
    private int mPicHeight;

    public ArrayList<Face> detectedFaces = new ArrayList<Face>();

    public FaceView(Context context, int resId) {
        super(context);

        mPInnerBullsEye.setStyle(Paint.Style.FILL);
        mPInnerBullsEye.setColor(Color.RED);

        mPOuterBullsEye.setStyle(Paint.Style.STROKE);
        mPOuterBullsEye.setColor(Color.RED);

        mTmpPaint.setStyle(Paint.Style.STROKE);
        mTmpPaint.setTextAlign(Paint.Align.CENTER);

        BitmapFactory.Options bfo = new BitmapFactory.Options();
        bfo.inPreferredConfig = Bitmap.Config.RGB_565;
        bfo.inScaled = false;
        bfo.inDither = false;

        mSourceImage = BitmapFactory.decodeResource(getResources(), resId, bfo);

        mPicWidth = mSourceImage.getWidth();
        mPicHeight = mSourceImage.getHeight();

        mFaceDetector = new FaceDetector(mPicWidth, mPicHeight, NUM_FACES);
        int numFaces = mFaceDetector.findFaces(mSourceImage, mAllFaces);

        for (int i = 0; i < numFaces; i++) {
            detectedFaces.add(mAllFaces[i]);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float scale = Math.min((float) getWidth() / mPicWidth, (float) getHeight() / mPicHeight);
        Rect scaledRect = new Rect(0, 0, mPicWidth, mPicHeight);
        scaledRect.scale(scale);
        canvas.drawBitmap(mSourceImage, null, scaledRect, mTmpPaint);
        for (Face face : detectedFaces) {
            PointF eyesMP = new PointF();
            face.getMidPoint(eyesMP);
            float centerX = eyesMP.x * scale;
            float centerY = eyesMP.y * scale;
            float eyesDistance = face.eyesDistance() * scale;
            mPOuterBullsEye.setStrokeWidth(eyesDistance / 6);
            canvas.drawCircle(centerX, centerY, eyesDistance / 2, mPOuterBullsEye);
            canvas.drawCircle(centerX, centerY, eyesDistance / 6, mPInnerBullsEye);
        }
    }
}
