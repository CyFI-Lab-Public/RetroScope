/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.animation.cts;

import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.graphics.Paint;
import android.graphics.RadialGradient;

/**
 * A data structure that holds a Shape and various properties that can be used to define
 * how the shape is drawn.
 */
public class ShapeHolder {
    private float mX = 0, mY = 0;
    private ShapeDrawable mShape;
    private int mColor;
    private RadialGradient mGradient;
    private float mAlpha = 1f;
    private Paint mPaint;

    public void setPaint(Paint value) {
        mPaint = value;
    }

    public Paint getPaint() {
        return mPaint;
    }

    public void setX(float value) {
        mX = value;
    }

    public float getX() {
        return mX;
    }

    public void setY(float value) {
        mY = value;
    }

    public float getY() {
        return mY;
    }

    public void setShape(ShapeDrawable value) {
        mShape = value;
    }

    public ShapeDrawable getShape() {
        return mShape;
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int value) {
        mShape.getPaint().setColor(value);
        mColor = value;
    }

    public void setGradient(RadialGradient value) {
        mGradient = value;
    }
    public RadialGradient getGradient() {
        return mGradient;
    }

    public void setAlpha(float alpha) {
        this.mAlpha = alpha;
        mShape.setAlpha((int)((alpha * 255f) + .5f));
    }

    public float getWidth() {
        return mShape.getShape().getWidth();
    }

    public void setWidth(float width) {
        Shape s = mShape.getShape();
        s.resize(width, s.getHeight());
    }

    public float getHeight() {
        return mShape.getShape().getHeight();
    }

    public void setHeight(float height) {
        Shape s = mShape.getShape();
        s.resize(s.getWidth(), height);
    }

    public ShapeHolder(ShapeDrawable s) {
        mShape = s;
    }
}

