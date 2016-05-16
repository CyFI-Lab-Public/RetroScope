/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.text.style.cts;

import junit.framework.TestCase;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.text.TextPaint;
import android.text.style.ReplacementSpan;

public class ReplacementSpanTest extends TestCase {
    public void testUpdateMeasureState() {
        ReplacementSpan replacementSpan = new MockReplacementSpan();
        replacementSpan.updateMeasureState(null);
    }

    public void testUpdateDrawState() {
        ReplacementSpan replacementSpan = new MockReplacementSpan();
        replacementSpan.updateDrawState(null);
    }

    /**
     * MockReplacementSpan for test.
     */
    private class MockReplacementSpan extends ReplacementSpan {
        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end,
                float x, int top, int y, int bottom, Paint paint) {
            // implement abstract method
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end,
                FontMetricsInt fm) {
            // implement abstract method
            return 0;
        }
    }
}
