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


import android.graphics.Color;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import junit.framework.TestCase;

public class ClickableSpanTest extends TestCase {
    public void testUpdateDrawState() {
        ClickableSpan clickableSpan = new MyClickableSpan();

        TextPaint tp = new TextPaint();
        tp.linkColor = Color.RED;
        tp.setUnderlineText(false);
        assertFalse(tp.isUnderlineText());

        clickableSpan.updateDrawState(tp);
        assertEquals(Color.RED, tp.getColor());
        assertTrue(tp.isUnderlineText());

        tp.linkColor = Color.BLUE;
        clickableSpan.updateDrawState(tp);
        assertEquals(Color.BLUE, tp.getColor());
        assertTrue(tp.isUnderlineText());

        try {
            clickableSpan.updateDrawState(null);
            fail("should throw NullPointerException when TextPaint is null.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    /**
     * MyClickableSpan for test.
     */
    private class MyClickableSpan extends ClickableSpan {
        @Override
        public void onClick(View widget) {
            // implement abstract method.
        }
    }
}
