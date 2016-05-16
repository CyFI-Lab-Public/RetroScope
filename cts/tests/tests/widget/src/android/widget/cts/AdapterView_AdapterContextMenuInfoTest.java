/*
 * Copyright (C) 2009 The Android Open Source Project
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


import android.test.AndroidTestCase;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class AdapterView_AdapterContextMenuInfoTest extends AndroidTestCase {
    public void testConstructor() {
        AdapterView.AdapterContextMenuInfo menuInfo;
        View testView = new View(getContext());
        int position = 1;
        long id = 0xffL;
        menuInfo = new AdapterView.AdapterContextMenuInfo(testView, position, id);

        assertEquals(position, menuInfo.position);
        assertEquals(id, menuInfo.id);
        assertEquals(testView, menuInfo.targetView);
    }
}
