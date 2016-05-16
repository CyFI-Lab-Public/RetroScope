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


import android.graphics.MaskFilter;
import android.text.TextPaint;
import android.text.style.MaskFilterSpan;

import junit.framework.TestCase;

public class MaskFilterSpanTest extends TestCase {
    public void testConstructor() {
        MaskFilter mf = new MaskFilter();
        new MaskFilterSpan(mf);
        new MaskFilterSpan(null);
    }

    public void testUpdateDrawState() {
        MaskFilter mf = new MaskFilter();
        MaskFilterSpan maskFilterSpan = new MaskFilterSpan(mf);

        TextPaint tp = new TextPaint();
        assertNull(tp.getMaskFilter());

        maskFilterSpan.updateDrawState(tp);
        assertSame(mf, tp.getMaskFilter());

        try {
            maskFilterSpan.updateDrawState(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    public void testGetMaskFilter() {
        MaskFilter expected = new MaskFilter();

        MaskFilterSpan maskFilterSpan = new MaskFilterSpan(expected);
        assertSame(expected, maskFilterSpan.getMaskFilter());

        maskFilterSpan = new MaskFilterSpan(null);
        assertNull(maskFilterSpan.getMaskFilter());
    }
}
