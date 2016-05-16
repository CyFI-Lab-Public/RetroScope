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

package com.android.quicksearchbox.benchmarks;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;

/*

To build and run:

mmm packages/apps/QuickSearchBox/benchmarks \
&& adb install -r $OUT/data/app/QuickSearchBoxBenchmarks.apk \
&& sleep 10 \
&& adb shell am start -a android.intent.action.MAIN \
        -n com.android.quicksearchbox.benchmarks/.WebLatency \
&& adb logcat

*/

public class WebLatency extends SourceLatency {

    private static final String[] queries = {
        "", "a", "s", "e", "r", "pub", "taxi", "kilt hire", "pizza",
             "weather london uk", "terminator showtimes", "obama news",
             "12 USD in GBP", "how to pass a drug test", "goog stock",
             "76 Bucking",
             "sanxjkashasrxae"
    };

    @Override
    protected void onResume() {
        super.onResume();
        testWebSuggestions();
        finish();
    }

    private ComponentName getWebSearchComponent() {
        Intent webSearchIntent = new Intent(Intent.ACTION_WEB_SEARCH);
        PackageManager pm = getPackageManager();
        return webSearchIntent.resolveActivity(pm);
    }

    private void testWebSuggestions() {
        ComponentName webComponent = getWebSearchComponent();
        checkSource("WEB", webComponent, queries);
    }

}
