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

package android.widget.cts;

import com.android.cts.stub.R;


import android.content.Context;
import android.test.InstrumentationTestCase;
import android.util.AttributeSet;
import android.widget.RadioButton;

/**
 * Test {@link RadioButton}.
 */
public class RadioButtonTest extends InstrumentationTestCase {
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();
    }

    public void testConstructor() {
        AttributeSet attrs = mContext.getResources().getLayout(R.layout.radiogroup_1);
        assertNotNull(attrs);

        new RadioButton(mContext);
        try {
            new RadioButton(null);
            fail("The constructor should throw NullPointerException when param Context is null.");
        } catch (NullPointerException e) {
        }

        new RadioButton(mContext, attrs);
        try {
            new RadioButton(null, attrs);
            fail("The constructor should throw NullPointerException when param Context is null.");
        } catch (NullPointerException e) {
        }
        new RadioButton(mContext, null);

        new RadioButton(mContext, attrs, 0);
        try {
            new RadioButton(null, attrs, 0);
            fail("The constructor should throw NullPointerException when param Context is null.");
        } catch (NullPointerException e) {
        }
        new RadioButton(mContext, null, 0);
        new RadioButton(mContext, attrs, Integer.MAX_VALUE);
        new RadioButton(mContext, attrs, Integer.MIN_VALUE);
    }

    public void testToggle() {
        RadioButton button = new RadioButton(mContext);
        assertFalse(button.isChecked());

        button.toggle();
        assertTrue(button.isChecked());

        // Can't be toggled if it is checked
        button.toggle();
        assertTrue(button.isChecked());
    }
}
