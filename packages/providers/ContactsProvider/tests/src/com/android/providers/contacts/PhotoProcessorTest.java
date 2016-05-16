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

package com.android.providers.contacts;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.providers.contacts.tests.R;


/**
 * Tests for {@link PhotoProcessor}.
 *
 * Most of tests are covered by {@link PhotoStoreTest}.
 */
@SmallTest
public class PhotoProcessorTest extends AndroidTestCase {

    public void testTransparency() {
        final Drawable source = getTestContext().getResources().getDrawable(
                R.drawable.transparent_10x10);
        final Bitmap sourceBitmap = ((BitmapDrawable) source).getBitmap();

        final Bitmap normalized = PhotoProcessor.getNormalizedBitmap(sourceBitmap, 50, false);

        // Make sure it's not scaled up.
        assertEquals(10, normalized.getWidth());
        assertEquals(10, normalized.getHeight());

        // Make sure the transparent pixel is now 100% white.
        assertEquals(Color.argb(255, 255, 255, 255), normalized.getPixel(0, 0));
    }
}
