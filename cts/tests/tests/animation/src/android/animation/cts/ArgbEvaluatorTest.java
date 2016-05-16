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

import android.animation.ArgbEvaluator;
import android.graphics.Color;
import android.test.InstrumentationTestCase;

public class ArgbEvaluatorTest extends InstrumentationTestCase {
    public void testEvaluate() throws Throwable {
        final int RED =  0xffFF8080;
        final int BLUE = 0xff8080FF;
        int aRED = Color.alpha(RED);
        int rRED = Color.red(RED);
        int gRED = Color.green(RED);
        int bRED = Color.blue(RED);
        int aBLUE = Color.alpha(BLUE);
        int rBLUE = Color.red(BLUE);
        int gBLUE = Color.green(BLUE);
        int bBLUE = Color.blue(BLUE);

        final ArgbEvaluator evaluator = new ArgbEvaluator();
        class AnimationRunnable implements Runnable{
            int result;
            public void run() {
                result = (Integer) evaluator.evaluate(0.5f, RED, BLUE);
            }
        }
        AnimationRunnable aRunnable = new AnimationRunnable();
        this.runTestOnUiThread(aRunnable);
        Thread.sleep(100);
        int result = aRunnable.result;

        int aResult = Color.alpha(result);
        int rResult = Color.red(result);
        int gResult = Color.green(result);
        int bResult = Color.blue(result);
        assertTrue(aResult >= aRED);
        assertTrue(aResult <= aBLUE);
        assertTrue(rResult <= rRED);
        assertTrue(rResult >= rBLUE);
        assertTrue(gResult >= gRED);
        assertTrue(gResult <= gBLUE);
        assertTrue(bResult >= bRED);
        assertTrue(bResult <= bBLUE);
    }
}

