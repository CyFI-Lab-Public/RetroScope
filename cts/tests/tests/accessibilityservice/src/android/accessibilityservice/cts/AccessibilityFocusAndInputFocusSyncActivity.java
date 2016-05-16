/**
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.accessibilityservice.cts;

import android.app.Activity;
import android.os.Bundle;

import com.android.cts.accessibilityservice.R;

/**
 * Activity for testing the accessibility focus APIs exposed to
 * accessibility services. These APIs allow moving accessibility
 * focus in the view tree from an AccessiiblityService. Specifically,
 * this activity is for verifying the the sync between accessibility
 * and input focus.
 */
public class AccessibilityFocusAndInputFocusSyncActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accessibility_focus_and_input_focus_sync_test);
    }
}
