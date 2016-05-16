/*
 * Copyright (C) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package android.view.cts;

import android.test.AndroidTestCase;
import android.widget.NumberPicker;

public class NumberPickerTest extends AndroidTestCase {

    public void testSetDisplayedValues1() throws Exception {
        NumberPicker numberPicker = new NumberPicker(getContext());
        numberPicker.setMinValue(10);
        numberPicker.setMaxValue(12);
        numberPicker.setDisplayedValues(new String[]{"One", "Two", "Three"});
    }

    public void testSetDisplayedValues2() throws Exception {
        NumberPicker numberPicker = new NumberPicker(getContext());
        numberPicker.setMinValue(10);
        numberPicker.setMaxValue(14);
        try {
            numberPicker.setDisplayedValues(new String[]{"One", "Two", "Three"});
            fail("The size of the displayed values array must be equal to the selectable numbers!");
        } catch (Exception e) {
            /* expected */
        }
    }
}
