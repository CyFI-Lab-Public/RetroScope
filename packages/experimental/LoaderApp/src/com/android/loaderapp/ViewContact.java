/*
 * Copyright (C) 2010 Google Inc.
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
 * limitations under the License
 */

package com.android.loaderapp;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

public class ViewContact extends Activity {
    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        Intent intent = new Intent(getIntent());
        Configuration config = getResources().getConfiguration();
        int screenLayoutSize = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (screenLayoutSize == Configuration.SCREENLAYOUT_SIZE_LARGE) {
            intent.setClass(this, HomeXLarge.class);
        } else {
            intent.setClass(this, DetailsNormal.class);
        }
        startActivity(intent);

        finish();
    }
}
