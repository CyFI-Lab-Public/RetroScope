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

package android.view.cts;

import com.android.cts.stub.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.Button;

public class FocusFinderStubActivity extends Activity {

    public ViewGroup layout;

    public Button topLeftButton;

    public Button topRightButton;

    public Button bottomLeftButton;

    public Button bottomRightButton;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.focus_finder_layout);
        layout = (ViewGroup) findViewById(R.id.layout);
        topLeftButton = (Button) findViewById(R.id.top_left_button);
        topRightButton = (Button) findViewById(R.id.top_right_button);
        bottomLeftButton = (Button) findViewById(R.id.bottom_left_button);
        bottomRightButton = (Button) findViewById(R.id.bottom_right_button);
    }
}

