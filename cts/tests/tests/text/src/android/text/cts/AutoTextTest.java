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

package android.text.cts;

import java.util.Locale;
import android.test.AndroidTestCase;
import android.text.AutoText;
import android.view.View;
import android.content.res.Configuration;

public class AutoTextTest extends AndroidTestCase {

    public void testGet() {
        // Define the necessary sources.
        CharSequence src;
        String actual;

        // set local as English.
        Locale.setDefault(Locale.ENGLISH);
        Configuration config = getContext().getResources().getConfiguration();
        if (!config.locale.equals(Locale.getDefault())) {
                config.locale = Locale.getDefault();
                getContext().getResources().updateConfiguration(config, null);
        }
        // New a View instance.
        View view = new View(getContext());

        // Test a word key not in the autotext.xml.
        src = "can";
        actual = AutoText.get(src, 0, src.length(), view);
        assertNull(actual);

        // get possible spelling correction in the scope of current
        // local/language
        src = "acn";
        actual = AutoText.get(src, 0, src.length(), view);
        assertNotNull(actual);
        assertEquals("can", actual);

        /*
         * get possible spelling correction in the scope of current
         * local/language, with end bigger than end
         */
        src = "acn";
        actual = AutoText.get(src, 0, src.length() + 1, view);
        assertNull(actual);

        /*
         * get possible spelling correction in the scope of current
         * local/language, with end smaller than end
         */
        src = "acn";
        actual = AutoText.get(src, 0, src.length() - 1, view);
        assertNull(actual);

        // get possible spelling correction outside of the scope of current
        // local/language
        src = "acnh";
        actual = AutoText.get(src, 0, src.length() - 1, view);
        assertNotNull(actual);
        assertEquals("can", actual);
    }

    public void testGetSize() {
        Locale.setDefault(Locale.ENGLISH);
        Configuration config = getContext().getResources().getConfiguration();
        if (!config.locale.equals(Locale.getDefault())) {
                config.locale = Locale.getDefault();
                getContext().getResources().updateConfiguration(config, null);
        }
        View view = new View(getContext());
        // Returns the size of the auto text dictionary. Just make sure it is bigger than 0.
        assertTrue(AutoText.getSize(view) > 0);
    }
}

