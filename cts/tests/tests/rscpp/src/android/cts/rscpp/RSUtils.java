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

package android.cts.rscpp;

import android.content.res.Resources;
import java.util.Random;
import android.renderscript.Allocation;
import android.renderscript.RSRuntimeException;
import com.android.cts.stub.R;

/**
 * This class supplies some utils for renderscript tests
 */
public class RSUtils {

    public static void genRandom(long seed, int factor, int offset, float array[]) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length; i++) {
            array[i] = r.nextFloat() * factor + offset;
        }
    }

    public static void genRandom(long seed, int factor, int offset, float array[],
            int stride, int skip) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length / stride; i++) {
            for (int j = 0; j < stride; j++) {
                if (j >= stride - skip)
                    array[i * stride + j] = 0;
                else
                    array[i * stride + j] = r.nextFloat() * factor + offset;
            }
        }
    }

    public static void genRandom(long seed, int max, int factor, int offset, int array[]) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length; i++) {
            array[i] = (r.nextInt(max) * factor + offset);
        }
    }

    public static void genRandom(long seed, int factor, int offset, int array[],
            int stride, int skip) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length / stride; i++) {
            for (int j = 0; j < stride; j++) {
                if (j >= stride - skip)
                    array[i * stride + j] = 0;
                else
                    array[i * stride + j] = r.nextInt() * factor + offset;
            }
        }
    }

    public static void genRandom(long seed, int max, int factor, int offset, int array[],
            int stride, int skip) {
        Random r = new Random(seed);
        for (int i = 0; i < array.length / stride; i++) {
            for (int j = 0; j < stride; j++) {
                if (j >= stride - skip)
                    array[i * stride + j] = 0;
                else
                    array[i * stride + j] = r.nextInt(max) * factor + offset;
            }
        }
    }
}
