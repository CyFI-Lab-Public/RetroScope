/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.gallery3d.filtershow.tools;

import android.util.Log;

public class MatrixFit {
    // Simple implementation of a matrix fit in N dimensions.

    private static final String LOGTAG = "MatrixFit";

    private double[][] mMatrix;
    private int mDimension;
    private boolean mValid = false;
    private static double sEPS = 1.0f/10000000000.0f;

    public MatrixFit(double[][] from, double[][] to) {
        mValid = fit(from, to);
    }

    public int getDimension() {
        return mDimension;
    }

    public boolean isValid() {
        return mValid;
    }

    public double[][] getMatrix() {
        return mMatrix;
    }

    public boolean fit(double[][] from, double[][] to) {
        if ((from.length != to.length) || (from.length < 1)) {
            Log.e(LOGTAG, "from and to must be of same size");
            return false;
        }

        mDimension = from[0].length;
        mMatrix = new double[mDimension +1][mDimension + mDimension +1];

        if (from.length < mDimension) {
            Log.e(LOGTAG, "Too few points => under-determined system");
            return false;
        }

        double[][] q = new double[from.length][mDimension];
        for (int i = 0; i < from.length; i++) {
            for (int j = 0; j < mDimension; j++) {
                q[i][j] = from[i][j];
            }
        }

        double[][] p = new double[to.length][mDimension];
        for (int i = 0; i < to.length; i++) {
            for (int j = 0; j < mDimension; j++) {
                p[i][j] = to[i][j];
            }
        }

        // Make an empty (dim) x (dim + 1) matrix and fill it
        double[][] c = new double[mDimension+1][mDimension];
        for (int j = 0; j < mDimension; j++) {
            for (int k = 0; k < mDimension + 1; k++) {
                for (int i = 0; i < q.length; i++) {
                    double qt = 1;
                    if (k < mDimension) {
                        qt = q[i][k];
                    }
                    c[k][j] += qt * p[i][j];
                }
            }
        }

        // Make an empty (dim+1) x (dim+1) matrix and fill it
        double[][] Q = new double[mDimension+1][mDimension+1];
        for (int qi = 0; qi < q.length; qi++) {
            double[] qt = new double[mDimension + 1];
            for (int i = 0; i < mDimension; i++) {
                qt[i] = q[qi][i];
            }
            qt[mDimension] = 1;
            for (int i = 0; i < mDimension + 1; i++) {
                for (int j = 0; j < mDimension + 1; j++) {
                    Q[i][j] += qt[i] * qt[j];
                }
            }
        }

        // Use a gaussian elimination to solve the linear system
        for (int i = 0; i < mDimension + 1; i++) {
            for (int j = 0; j < mDimension + 1; j++) {
                mMatrix[i][j] = Q[i][j];
            }
            for (int j = 0; j < mDimension; j++) {
                mMatrix[i][mDimension + 1 + j] = c[i][j];
            }
        }
        if (!gaussianElimination(mMatrix)) {
            return false;
        }
        return true;
    }

    public double[] apply(double[] point) {
        if (mDimension != point.length) {
            return null;
        }
        double[] res = new double[mDimension];
        for (int j = 0; j < mDimension; j++) {
            for (int i = 0; i < mDimension; i++) {
                res[j] += point[i] * mMatrix[i][j+ mDimension +1];
            }
            res[j] += mMatrix[mDimension][j+ mDimension +1];
        }
        return res;
    }

    public void printEquation() {
        for (int j = 0; j < mDimension; j++) {
            String str = "x" + j + "' = ";
            for (int i = 0; i < mDimension; i++) {
                str += "x" + i + " * " + mMatrix[i][j+mDimension+1] + " + ";
            }
            str += mMatrix[mDimension][j+mDimension+1];
            Log.v(LOGTAG, str);
        }
    }

    private void printMatrix(String name, double[][] matrix) {
        Log.v(LOGTAG, "name: " + name);
        for (int i = 0; i < matrix.length; i++) {
            String str = "";
            for (int j = 0; j < matrix[0].length; j++) {
                str += "" + matrix[i][j] + " ";
            }
            Log.v(LOGTAG, str);
        }
    }

    /*
     * Transforms the given matrix into a row echelon matrix
     */
    private boolean gaussianElimination(double[][] m) {
        int h = m.length;
        int w = m[0].length;

        for (int y = 0; y < h; y++) {
            int maxrow = y;
            for (int y2 = y + 1; y2 < h; y2++) { // Find max pivot
                if (Math.abs(m[y2][y]) > Math.abs(m[maxrow][y])) {
                    maxrow = y2;
                }
            }
            // swap
            for (int i = 0; i < mDimension; i++) {
                double t = m[y][i];
                m[y][i] = m[maxrow][i];
                m[maxrow][i] = t;
            }

            if (Math.abs(m[y][y]) <= sEPS) { // Singular Matrix
                return false;
            }
            for (int y2 = y + 1; y2 < h; y2++) { // Eliminate column y
                double c = m[y2][y] / m[y][y];
                for (int x = y; x < w; x++) {
                    m[y2][x] -= m[y][x] * c;
                }
            }
        }
        for (int y = h -1; y > -1; y--) { // Back substitution
            double c = m[y][y];
            for (int y2 = 0; y2 < y; y2++) {
                for (int x = w - 1; x > y - 1; x--) {
                    m[y2][x] -= m[y][x] * m[y2][y] / c;
                }
            }
            m[y][y] /= c;
            for (int x = h; x < w; x++) { // Normalize row y
                m[y][x] /= c;
            }
        }
        return true;
    }
}
