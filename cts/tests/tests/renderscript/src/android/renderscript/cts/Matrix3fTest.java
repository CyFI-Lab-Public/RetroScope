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
import android.renderscript.Matrix3f;

public class Matrix3fTest extends RSBaseCompute {

    final float delta = 0.00001f;
    final float[] setData = {
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        };

    void checkIdentity(Matrix3f m, float delta) {
        for (int i = 0; i < 3; i ++) {
            for (int j = 0; j < 3; j ++) {
                if (i == j) {
                    assertEquals(1.0f, m.getArray()[i*3 + j], delta);
                } else {
                    assertEquals(0.0f, m.getArray()[i*3 + j], delta);
                }
            }
        }
    }

    String getString(float[] array) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 3; i ++) {
            builder.append("[");
            for (int j = 0; j < 3; j ++) {
                builder.append(array[i*3 + j]);
                builder.append(" ");
            }
            builder.append("]");
        }
        return builder.toString();
    }

    void checkData(Matrix3f m, float[] data, float delta) {
        for (int i = 0; i < data.length; i ++) {
            assertEquals(data[i], m.getArray()[i], delta);
        }
    }

    void checkData(Matrix3f m, float[] data) {
        String s1 = getString(m.getArray());
        String s2 = getString(data);
        assertEquals(s2, s1);
    }

    public void testCreation() {
        Matrix3f m = new Matrix3f();
        assertTrue(m.getArray() != null);
        checkIdentity(m, 0.0f);

        m = new Matrix3f(setData);
        checkData(m, setData);
    }

    public void testGet() {

        Matrix3f m = new Matrix3f(setData);

        for (int i = 0; i < 3; i ++) {
            for (int j = 0; j < 3; j ++) {
                assertEquals(setData[i*3 + j], m.get(i, j), 0.0f);
            }
        }
    }

    public void testSet() {
        Matrix3f m = new Matrix3f();
        for (int i = 0; i < 3; i ++) {
            for (int j = 0; j < 3; j ++) {
                float valToSet = setData[i*3 + j];
                m.set(i, j, valToSet);
                assertEquals(valToSet, m.get(i, j), 0.0f);
            }
        }
    }

    public void testLoadIdentity() {
        Matrix3f m = new Matrix3f(setData);
        m.loadIdentity();
        checkIdentity(m, 0.0f);
    }

    public void testLoad() {
        Matrix3f m1 = new Matrix3f();
        Matrix3f m2 = new Matrix3f(setData);

        m1.load(m2);
        checkData(m1, setData);
    }

    public void testLoadScale() {
        float[] expectedData = {
            2.0f, 0.0f, 0.0f,
            0.0f, 3.0f, 0.0f,
            0.0f, 0.0f, 4.0f,
        };
        float[] expectedData2 = {
            2.0f, 0.0f, 0.0f,
            0.0f, 3.0f, 0.0f,
            0.0f, 0.0f, 1.0f,
        };

        Matrix3f m = new Matrix3f(setData);
        m.loadScale(2.0f, 3.0f, 4.0f);
        checkData(m, expectedData);

        m = new Matrix3f(setData);
        m.loadScale(2.0f, 3.0f);
        checkData(m, expectedData2);
    }

    public void testLoadTranslate() {
        float[] expectedData = {
            1.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            2.0f, 3.0f, 1.0f,
        };

        Matrix3f m = new Matrix3f(setData);
        m.loadTranslate(2.0f, 3.0f);
        checkData(m, expectedData);
    }

    public void testScale() {
        float[] expectedData = new float[setData.length];
        System.arraycopy(setData, 0, expectedData, 0, setData.length);
        float scaleX = 2.0f;
        float scaleY = 3.0f;
        float scaleZ = 4.0f;
        expectedData[0] *= scaleX;
        expectedData[1] *= scaleX;
        expectedData[2] *= scaleX;
        expectedData[3] *= scaleY;
        expectedData[4] *= scaleY;
        expectedData[5] *= scaleY;
        expectedData[6] *= scaleZ;
        expectedData[7] *= scaleZ;
        expectedData[8] *= scaleZ;

        Matrix3f m = new Matrix3f(setData);
        m.scale(scaleX, scaleY, scaleZ);
        checkData(m, expectedData);

        System.arraycopy(setData, 0, expectedData, 0, setData.length);
        expectedData[0] *= scaleX;
        expectedData[1] *= scaleX;
        expectedData[2] *= scaleX;
        expectedData[3] *= scaleY;
        expectedData[4] *= scaleY;
        expectedData[5] *= scaleY;

        m = new Matrix3f(setData);
        m.scale(scaleX, scaleY);
        checkData(m, expectedData);
    }

    public void testMultiply() {
        float[] lhsData = {
            1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f
        };

        float[] rhsData = {
            2.0f, 2.0f, 2.0f,
            3.0f, 3.0f, 3.0f,
            4.0f, 4.0f, 4.0f
        };

        float[] expected = {
            3.0f*2.0f, 3.0f*2.0f, 3.0f*2.0f,
            3.0f*3.0f, 3.0f*3.0f, 3.0f*3.0f,
            3.0f*4.0f, 3.0f*4.0f, 3.0f*4.0f
        };

        // Due to column major nature of OpenGL,
        // left hand side and right hand side
        // are reversed. Premultiplying by row major
        // and post multiplying by column major
        // are the same. So lhs and rhs get reversed here.
        Matrix3f lhs = new Matrix3f(lhsData);
        Matrix3f rhs = new Matrix3f(rhsData);
        Matrix3f loadMul = new Matrix3f();

        loadMul.loadMultiply(lhs, rhs);
        checkData(loadMul, expected);

        lhs.multiply(rhs);
        checkData(lhs, expected);
    }

    public void testTranspose() {
        float[] expectedData = new float[setData.length];
        System.arraycopy(setData, 0, expectedData, 0, setData.length);

        for(int i = 0; i < 2; i++) {
            for(int j = i + 1; j < 3; j++) {
                float temp = expectedData[i*3 + j];
                expectedData[i*3 + j] = expectedData[j*3 + i];
                expectedData[j*3 + i] = temp;
            }
        }

        Matrix3f m = new Matrix3f(setData);
        m.transpose();

        checkData(m, expectedData);
    }
}
