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
package android.view.inputmethod.cts;


import android.os.Parcel;
import android.test.AndroidTestCase;
import android.view.inputmethod.ExtractedText;

public class ExtractedTextTest extends AndroidTestCase {

    public void testWriteToParcel() {

        ExtractedText extractedText = new ExtractedText();
        extractedText.flags = 1;
        extractedText.selectionEnd = 11;
        extractedText.selectionStart = 2;
        extractedText.startOffset = 1;
        CharSequence text = "test";
        extractedText.text = text;
        Parcel p = Parcel.obtain();
        extractedText.writeToParcel(p, 0);
        p.setDataPosition(0);
        ExtractedText target = ExtractedText.CREATOR.createFromParcel(p);
        assertEquals(extractedText.flags, target.flags);
        assertEquals(extractedText.selectionEnd, target.selectionEnd);
        assertEquals(extractedText.selectionStart, target.selectionStart);
        assertEquals(extractedText.startOffset, target.startOffset);
        assertEquals(extractedText.partialStartOffset, target.partialStartOffset);
        assertEquals(extractedText.partialEndOffset, target.partialEndOffset);
        assertEquals(extractedText.text.toString(), target.text.toString());

        assertEquals(0, extractedText.describeContents());
    }
}
