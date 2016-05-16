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
import android.renderscript.Matrix4f;

public class Matrix4fTest extends RSBaseCompute {

    final float delta = 0.00001f;
    final float[] setData = {
            1.0f, 2.0f, 3.0f, 4.0f,
            5.0f, 6.0f, 7.0f, 8.0f,
            9.0f, 10.0f, 11.0f, 12.0f,
            13.0f, 14.0f, 15.0f, 16.0f
        };

    void checkIdentity(Matrix4f m, float delta) {
        for (int i = 0; i < 4; i ++) {
            for (int j = 0; j < 4; j ++) {
                if (i == j) {
                    assertEquals(1.0f, m.getArray()[i*4 + j], delta);
                } else {
                    assertEquals(0.0f, m.getArray()[i*4 + j], delta);
                }
            }
        }
    }

    String getString(float[] array) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4; i ++) {
            builder.append("[");
            for (int j = 0; j < 4; j ++) {
                builder.append(array[i*4 + j]);
                builder.append(" ");
            }
            builder.append("]");
        }
        return builder.toString();
    }

    void checkData(Matrix4f m, float[] data, float delta) {
        for (int i = 0; i < data.length; i ++) {
            assertEquals(data[i], m.getArray()[i], delta);
        }
    }

    void checkData(Matrix4f m, float[] data) {
        String s1 = getString(m.getArray());
        String s2 = getString(data);
        assertEquals(s2, s1);
    }

    public void testCreation() {
        Matrix4f m = new Matrix4f();
        assertTrue(m.getArray() != null);
        checkIdentity(m, 0.0f);


        m = new Matrix4f(setData);
        checkData(m, setData);
    }

    public void testGet() {

        Matrix4f m = new Matrix4f(setData);

        for (int i = 0; i < 4; i ++) {
            for (int j = 0; j < 4; j ++) {
                assertEquals(setData[i*4 + j], m.get(i, j), 0.0f);
            }
        }
    }

    public void testSet() {
        Matrix4f m = new Matrix4f();
        for (int i = 0; i < 4; i ++) {
            for (int j = 0; j < 4; j ++) {
                float valToSet = setData[i*4 + j];
                m.set(i, j, valToSet);
                assertEquals(valToSet, m.get(i, j), 0.0f);
            }
        }
    }

    public void testLoadIdentity() {
        Matrix4f m = new Matrix4f(setData);
        m.loadIdentity();
        checkIdentity(m, 0.0f);
    }

    public void testLoad() {
        Matrix4f m1 = new Matrix4f();
        Matrix4f m2 = new Matrix4f(setData);

        m1.load(m2);
        checkData(m1, setData);
    }

    public void testLoadScale() {
        float[] expectedData = {
            2.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 3.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 4.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };

        Matrix4f m = new Matrix4f(setData);
        m.loadScale(2.0f, 3.0f, 4.0f);
        checkData(m, expectedData);
    }

    public void testLoadTranslate() {
        float[] expectedData = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            2.0f, 3.0f, 4.0f, 1.0f
        };

        Matrix4f m = new Matrix4f(setData);
        m.loadTranslate(2.0f, 3.0f, 4.0f);
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
        expectedData[3] *= scaleX;
        expectedData[4] *= scaleY;
        expectedData[5] *= scaleY;
        expectedData[6] *= scaleY;
        expectedData[7] *= scaleY;
        expectedData[8] *= scaleZ;
        expectedData[9] *= scaleZ;
        expectedData[10] *= scaleZ;
        expectedData[11] *= scaleZ;

        Matrix4f m = new Matrix4f(setData);
        m.scale(scaleX, scaleY, scaleZ);
        checkData(m, expectedData);
    }

    public void testTranspose() {
        float[] expectedData = new float[setData.length];
        System.arraycopy(setData, 0, expectedData, 0, setData.length);

        for(int i = 0; i < 3; i++) {
            for(int j = i + 1; j < 4; j++) {
                float temp = expectedData[i*4 + j];
                expectedData[i*4 + j] = expectedData[j*4 + i];
                expectedData[j*4 + i] = temp;
            }
        }

        Matrix4f m = new Matrix4f(setData);
        m.transpose();

        checkData(m, expectedData);
    }

    public void testMultiply() {
        float[] lhsData = {
            1.0f, 1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f
        };

        float[] rhsData = {
            2.0f, 2.0f, 2.0f, 2.0f,
            3.0f, 3.0f, 3.0f, 3.0f,
            4.0f, 4.0f, 4.0f, 4.0f,
            5.0f, 5.0f, 5.0f, 5.0f
        };

        float[] expected = {
            4.0f*2.0f, 4.0f*2.0f, 4.0f*2.0f, 4.0f*2.0f,
            4.0f*3.0f, 4.0f*3.0f, 4.0f*3.0f, 4.0f*3.0f,
            4.0f*4.0f, 4.0f*4.0f, 4.0f*4.0f, 4.0f*4.0f,
            4.0f*5.0f, 4.0f*5.0f, 4.0f*5.0f, 4.0f*5.0f
        };

        // Due to column major nature of OpenGL,
        // left hand side and right hand side
        // are reversed. Premultiplying by row major
        // and post multiplying by column major
        // are the same. So lhs and rhs get reversed here.
        Matrix4f lhs = new Matrix4f(lhsData);
        Matrix4f rhs = new Matrix4f(rhsData);
        Matrix4f loadMul = new Matrix4f();

        loadMul.loadMultiply(lhs, rhs);
        checkData(loadMul, expected);

        lhs.multiply(rhs);
        checkData(lhs, expected);
    }

    public void testInverse() {
        Matrix4f m = new Matrix4f();
        assertTrue(m.inverse());
        checkIdentity(m, delta);

        m = new Matrix4f();
        m.scale(2.0f, 2.0f, 2.0f);
        m.translate(5.0f, 6.0f, 7.0f);

        Matrix4f m2 = new Matrix4f(m.getArray());
        assertTrue(m2.inverse());
        m.multiply(m2);
        checkIdentity(m, delta);
    }
}
