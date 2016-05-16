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

package android.renderscript.cts;
import android.renderscript.Matrix2f;

public class Matrix2fTest extends RSBaseCompute {

    final float delta = 0.00001f;
    final float[] setData = {
            1.0f, 2.0f,
            3.0f, 4.0f
        };

    void checkIdentity(Matrix2f m, float delta) {
        for (int i = 0; i < 2; i ++) {
            for (int j = 0; j < 2; j ++) {
                if (i == j) {
                    assertEquals(1.0f, m.getArray()[i*2 + j], delta);
                } else {
                    assertEquals(0.0f, m.getArray()[i*2 + j], delta);
                }
            }
        }
    }

    String getString(float[] array) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 2; i ++) {
            builder.append("[");
            for (int j = 0; j < 2; j ++) {
                builder.append(array[i*2 + j]);
                builder.append(" ");
            }
            builder.append("]");
        }
        return builder.toString();
    }

    void checkData(Matrix2f m, float[] data, float delta) {
        for (int i = 0; i < data.length; i ++) {
            assertEquals(data[i], m.getArray()[i], delta);
        }
    }

    void checkData(Matrix2f m, float[] data) {
        String s1 = getString(m.getArray());
        String s2 = getString(data);
        assertEquals(s2, s1);
    }

    public void testCreation() {
        Matrix2f m = new Matrix2f();
        assertTrue(m.getArray() != null);
        checkIdentity(m, 0.0f);

        m = new Matrix2f(setData);
        checkData(m, setData);
    }

    public void testGet() {

        Matrix2f m = new Matrix2f(setData);

        for (int i = 0; i < 2; i ++) {
            for (int j = 0; j < 2; j ++) {
                assertEquals(setData[i*2 + j], m.get(i, j), 0.0f);
            }
        }
    }

    public void testSet() {
        Matrix2f m = new Matrix2f();
        for (int i = 0; i < 2; i ++) {
            for (int j = 0; j < 2; j ++) {
                float valToSet = setData[i*2 + j];
                m.set(i, j, valToSet);
                assertEquals(valToSet, m.get(i, j), 0.0f);
            }
        }
    }

    public void testLoadIdentity() {
        Matrix2f m = new Matrix2f(setData);
        m.loadIdentity();
        checkIdentity(m, 0.0f);
    }

    public void testLoad() {
        Matrix2f m1 = new Matrix2f();
        Matrix2f m2 = new Matrix2f(setData);

        m1.load(m2);
        checkData(m1, setData);
    }

    public void testLoadScale() {
        float[] expectedData = {
            2.0f, 0.0f,
            0.0f, 3.0f
        };

        Matrix2f m = new Matrix2f(setData);
        m.loadScale(2.0f, 3.0f);
        checkData(m, expectedData);
    }

    public void testMultiply() {
        float[] lhsData = {
            1.0f, 1.0f,
            1.0f, 1.0f
        };

        float[] rhsData = {
            2.0f, 2.0f,
            3.0f, 3.0f
        };

        float[] expected = {
            2.0f*2.0f, 2.0f*2.0f,
            2.0f*3.0f, 2.0f*3.0f
        };

        // Due to column major nature of OpenGL,
        // left hand side and right hand side
        // are reversed. Premultiplying by row major
        // and post multiplying by column major
        // are the same. So lhs and rhs get reversed here.
        Matrix2f lhs = new Matrix2f(lhsData);
        Matrix2f rhs = new Matrix2f(rhsData);
        Matrix2f loadMul = new Matrix2f();

        loadMul.loadMultiply(lhs, rhs);
        checkData(loadMul, expected);

        lhs.multiply(rhs);
        checkData(lhs, expected);
    }

    public void testScale() {
        float[] expectedData = new float[setData.length];
        System.arraycopy(setData, 0, expectedData, 0, setData.length);
        float scaleX = 2.0f;
        float scaleY = 3.0f;
        expectedData[0] *= scaleX;
        expectedData[1] *= scaleX;
        expectedData[2] *= scaleY;
        expectedData[3] *= scaleY;

        Matrix2f m = new Matrix2f(setData);
        m.scale(scaleX, scaleY);
        checkData(m, expectedData);
    }

    public void testTranspose() {
        float[] expectedData = new float[setData.length];
        System.arraycopy(setData, 0, expectedData, 0, setData.length);
        float temp = expectedData[1];
        expectedData[1] = expectedData[2];
        expectedData[2] = temp;

        Matrix2f m = new Matrix2f(setData);
        m.transpose();

        checkData(m, expectedData);
    }

    public void testRotateLoadRotate() {

        float theta = 30.0f * (float)(java.lang.Math.PI / 180.0f);
        float cosTheta = (float)Math.cos((float)theta);
        float sinTheta = (float)Math.sin((float)theta);
        float[] rotate = new float[4];
        rotate[0] = cosTheta;
        rotate[1] = -sinTheta;
        rotate[2] = sinTheta;
        rotate[3] = cosTheta;

        Matrix2f m = new Matrix2f();
        m.loadRotate(30.0f);
        checkData(m, rotate);

        m = new Matrix2f();
        m.rotate(30.0f);
        checkData(m, rotate);

        float[] sourceData = {
            2.0f, 3.0f,
            4.0f, 5.0f
        };
        float[] rotated = new float[4];
        rotated[0] = rotate[0] * sourceData[0] + rotate[1] * sourceData[2];
        rotated[1] = rotate[0] * sourceData[1] + rotate[1] * sourceData[3];
        rotated[2] = rotate[2] * sourceData[0] + rotate[3] * sourceData[2];
        rotated[3] = rotate[2] * sourceData[1] + rotate[3] * sourceData[3];
        m = new Matrix2f(sourceData);
        m.rotate(30.0f);

        checkData(m, rotated);
    }
}
