/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.holo.cts;

import com.android.cts.holo.R;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;

public class DisplayInfoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_info);

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = Math.round(dm.widthPixels / dm.density);
        int height = Math.round(dm.heightPixels / dm.density);

        TextView text = (TextView) findViewById(R.id.text);
        text.setText(getString(R.string.display_info_text, metrics.densityDpi,
                getScreenDensityBucket(metrics), width, height));
    }

    private String getScreenDensityBucket(DisplayMetrics metrics) {
        switch (metrics.densityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                return "ldpi";

            case DisplayMetrics.DENSITY_MEDIUM:
                return "mdpi";

            case DisplayMetrics.DENSITY_HIGH:
                return "hdpi";

            case DisplayMetrics.DENSITY_XHIGH:
                return "xdpi";

            case DisplayMetrics.DENSITY_XXHIGH:
                return "xxdpi";

            case DisplayMetrics.DENSITY_XXXHIGH:
                return "xxxdpi";

            case DisplayMetrics.DENSITY_TV:
                return "tvdpi";

            default:
                return "" + metrics.densityDpi;
        }
    }
}
