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
import com.android.internal.util.XmlUtils;


import org.xmlpull.v1.XmlPullParserException;

import android.content.res.XmlResourceParser;
import android.test.AndroidTestCase;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;

import java.io.IOException;

public class LinearLayout_LayoutParamsTest extends AndroidTestCase {
    public void testConstructor() throws XmlPullParserException, IOException {
        XmlResourceParser p = mContext.getResources().getLayout(R.layout.linearlayout_layout);

        XmlUtils.beginDocument(p, "LinearLayout");
        new LinearLayout.LayoutParams(getContext(), p);

        new LinearLayout.LayoutParams(320, 240);

        new LinearLayout.LayoutParams(320, 240, 0);

        LayoutParams layoutParams = new LayoutParams(320, 480);
        new LinearLayout.LayoutParams(layoutParams);

        MarginLayoutParams marginLayoutParams = new MarginLayoutParams(320, 480);
        new LinearLayout.LayoutParams(marginLayoutParams);
    }

    public void testDebug() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(320, 240);
        assertNotNull(layoutParams.debug("test: "));
    }
}
