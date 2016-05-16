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


import android.os.Parcel;
import android.test.InstrumentationTestCase;
import android.text.TextUtils;
import android.view.AbsSavedState;
import android.widget.TextView;

/**
 * Test {@link TextView.SavedState}.
 */
public class TextView_SaveStateTest extends InstrumentationTestCase {
    public void testToString() {
        Parcel source = creatTestParcel(0, 0, true, "This is content");
        TextView.SavedState state = TextView.SavedState.CREATOR.createFromParcel(source);

        assertNotNull(state.toString());

        source = creatTestParcel(5, 10, false, "This is another content");
        state = TextView.SavedState.CREATOR.createFromParcel(source);

        assertNotNull(state.toString());
    }

    public void testWriteToParcel() {
        Parcel source = creatTestParcel(0, 0, true, "This is content");
        TextView.SavedState state = TextView.SavedState.CREATOR.createFromParcel(source);
        assertNotNull(state);
    }

    /**
     * Gets the parcel.
     *
     * @param start the start
     * @param end the end
     * @param frozenWithFocus the frozen with focus
     * @param text the text
     * @return the parcel
     */
    private Parcel creatTestParcel(int start, int end, boolean frozenWithFocus, String text) {
        Parcel source = Parcel.obtain();

        source.writeParcelable(AbsSavedState.EMPTY_STATE, 0);
        source.writeInt(start);
        source.writeInt(end);
        source.writeInt(frozenWithFocus ? 1 : 0);
        TextView textView = new TextView(getInstrumentation().getTargetContext());
        textView.setText(text);
        TextUtils.writeToParcel(textView.getText(), source, 0);
        source.setDataPosition(0);
        return source;
    }
}
