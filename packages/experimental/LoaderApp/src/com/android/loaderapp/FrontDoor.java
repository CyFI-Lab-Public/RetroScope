/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.loaderapp;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

public final class FrontDoor extends Activity {
    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        Intent intent = getIntent();

        String componentName = intent.getComponent().getClassName();
        if ("com.android.loaderapp.FrontDoorNormal".equals(componentName)) {
            // The user clicked on the normal front door
            startActivity(new Intent(this, HomeNormal.class));
        } else if ("com.android.loaderapp.FrontDoorXLarge".equals(componentName)) {
            // The user clicked on the large front door
            startActivity(new Intent(this, HomeXLarge.class));
        } else if ("com.android.loaderapp.FrontDoorGroupsXLarge".equals(componentName)) {
            // The user clicked on the groups large front door
            startActivity(new Intent(this, HomeGroupsXLarge.class));
        } else {
            // The user clicked on the config based front door
            Configuration config = getResources().getConfiguration();
            int screenLayoutSize = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
            if (screenLayoutSize == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
                startActivity(new Intent(this, HomeXLarge.class));
            } else {
                // Default to the normal layout
                startActivity(new Intent(this, HomeNormal.class));
            }
        }

        finish();
    }
}