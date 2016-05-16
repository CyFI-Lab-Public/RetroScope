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

package com.android.gallery3d.filtershow.filters;

import java.util.Arrays;

public class ColorSpaceMatrix {
    private final float[] mMatrix = new float[16];
    private static final float RLUM = 0.3086f;
    private static final float GLUM = 0.6094f;
    private static final float BLUM = 0.0820f;

    public ColorSpaceMatrix() {
        identity();
    }

    /**
     * Copy constructor
     *
     * @param matrix
     */
    public ColorSpaceMatrix(ColorSpaceMatrix matrix) {
        System.arraycopy(matrix.mMatrix, 0, mMatrix, 0, matrix.mMatrix.length);
    }

    /**
     * get the matrix
     *
     * @return the internal matrix
     */
    public float[] getMatrix() {
        return mMatrix;
    }

    /**
     * set matrix to identity
     */
    public void identity() {
        Arrays.fill(mMatrix, 0);
        mMatrix[0] = mMatrix[5] = mMatrix[10] = mMatrix[15] = 1;
    }

    public void convertToLuminance() {
        mMatrix[0] = mMatrix[1] = mMatrix[2] = 0.3086f;
        mMatrix[4] = mMatrix[5] = mMatrix[6] = 0.6094f;
        mMatrix[8] = mMatrix[9] = mMatrix[10] = 0.0820f;
    }

    private void multiply(float[] a)
    {
        int x, y;
        float[] temp = new float[16];

        for (y = 0; y < 4; y++) {
            int y4 = y * 4;
            for (x = 0; x < 4; x++) {
                temp[y4 + x] = mMatrix[y4 + 0] * a[x]
                        + mMatrix[y4 + 1] * a[4 + x]
                        + mMatrix[y4 + 2] * a[8 + x]
                        + mMatrix[y4 + 3] * a[12 + x];
            }
        }
        for (int i = 0; i < 16; i++)
            mMatrix[i] = temp[i];
    }

    private void xRotateMatrix(float rs, float rc)
    {
        ColorSpaceMatrix c = new ColorSpaceMatrix();
        float[] tmp = c.mMatrix;

        tmp[5] = rc;
        tmp[6] = rs;
        tmp[9] = -rs;
        tmp[10] = rc;

        multiply(tmp);
    }

    private void yRotateMatrix(float rs, float rc)
    {
        ColorSpaceMatrix c = new ColorSpaceMatrix();
        float[] tmp = c.mMatrix;

        tmp[0] = rc;
        tmp[2] = -rs;
        tmp[8] = rs;
        tmp[10] = rc;

        multiply(tmp);
    }

    private void zRotateMatrix(float rs, float rc)
    {
        ColorSpaceMatrix c = new ColorSpaceMatrix();
        float[] tmp = c.mMatrix;

        tmp[0] = rc;
        tmp[1] = rs;
        tmp[4] = -rs;
        tmp[5] = rc;
        multiply(tmp);
    }

    private void zShearMatrix(float dx, float dy)
    {
        ColorSpaceMatrix c = new ColorSpaceMatrix();
        float[] tmp = c.mMatrix;

        tmp[2] = dx;
        tmp[6] = dy;
        multiply(tmp);
    }

    /**
     * sets the transform to a shift in Hue
     *
     * @param rot rotation in degrees
     */
    public void setHue(float rot)
    {
        float mag = (float) Math.sqrt(2.0);
        float xrs = 1 / mag;
        float xrc = 1 / mag;
        xRotateMatrix(xrs, xrc);
        mag = (float) Math.sqrt(3.0);
        float yrs = -1 / mag;
        float yrc = (float) Math.sqrt(2.0) / mag;
        yRotateMatrix(yrs, yrc);

        float lx = getRedf(RLUM, GLUM, BLUM);
        float ly = getGreenf(RLUM, GLUM, BLUM);
        float lz = getBluef(RLUM, GLUM, BLUM);
        float zsx = lx / lz;
        float zsy = ly / lz;
        zShearMatrix(zsx, zsy);

        float zrs = (float) Math.sin(rot * Math.PI / 180.0);
        float zrc = (float) Math.cos(rot * Math.PI / 180.0);
        zRotateMatrix(zrs, zrc);
        zShearMatrix(-zsx, -zsy);
        yRotateMatrix(-yrs, yrc);
        xRotateMatrix(-xrs, xrc);
    }

    /**
     * set it to a saturation matrix
     *
     * @param s
     */
    public void changeSaturation(float s) {
        mMatrix[0] = (1 - s) * RLUM + s;
        mMatrix[1] = (1 - s) * RLUM;
        mMatrix[2] = (1 - s) * RLUM;
        mMatrix[4] = (1 - s) * GLUM;
        mMatrix[5] = (1 - s) * GLUM + s;
        mMatrix[6] = (1 - s) * GLUM;
        mMatrix[8] = (1 - s) * BLUM;
        mMatrix[9] = (1 - s) * BLUM;
        mMatrix[10] = (1 - s) * BLUM + s;
    }

    /**
     * Transform RGB value
     *
     * @param r red pixel value
     * @param g green pixel value
     * @param b blue pixel value
     * @return computed red pixel value
     */
    public float getRed(int r, int g, int b) {
        return r * mMatrix[0] + g * mMatrix[4] + b * mMatrix[8] + mMatrix[12];
    }

    /**
     * Transform RGB value
     *
     * @param r red pixel value
     * @param g green pixel value
     * @param b blue pixel value
     * @return computed green pixel value
     */
    public float getGreen(int r, int g, int b) {
        return r * mMatrix[1] + g * mMatrix[5] + b * mMatrix[9] + mMatrix[13];
    }

    /**
     * Transform RGB value
     *
     * @param r red pixel value
     * @param g green pixel value
     * @param b blue pixel value
     * @return computed blue pixel value
     */
    public float getBlue(int r, int g, int b) {
        return r * mMatrix[2] + g * mMatrix[6] + b * mMatrix[10] + mMatrix[14];
    }

    private float getRedf(float r, float g, float b) {
        return r * mMatrix[0] + g * mMatrix[4] + b * mMatrix[8] + mMatrix[12];
    }

    private float getGreenf(float r, float g, float b) {
        return r * mMatrix[1] + g * mMatrix[5] + b * mMatrix[9] + mMatrix[13];
    }

    private float getBluef(float r, float g, float b) {
        return r * mMatrix[2] + g * mMatrix[6] + b * mMatrix[10] + mMatrix[14];
    }

}
