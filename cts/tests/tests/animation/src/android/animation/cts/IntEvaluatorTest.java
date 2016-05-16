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

import android.animation.IntEvaluator;
import android.test.InstrumentationTestCase;

public class IntEvaluatorTest extends InstrumentationTestCase {
    public void testEvaluate() throws Throwable {
        final int start = 0;
        final int end = 100;
        final float fraction = 0.5f;
        final IntEvaluator intEvaluator = new IntEvaluator();
        class AnimationRunnable implements Runnable{
            int result;
            public void run() {
                result = intEvaluator.evaluate(fraction, start, end);
            }
        }
        AnimationRunnable aRunnable = new AnimationRunnable();
        this.runTestOnUiThread(aRunnable);
        Thread.sleep(1);
        int result = aRunnable.result;
        assertTrue(result >= (fraction*start));
        assertTrue(result <= (fraction*end));
    }
}

