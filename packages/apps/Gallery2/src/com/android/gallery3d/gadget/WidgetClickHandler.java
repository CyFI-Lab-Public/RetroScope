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

package com.android.gallery3d.gadget;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.app.GalleryActivity;
import com.android.gallery3d.app.PhotoPage;
import com.android.gallery3d.common.ApiHelper;

public class WidgetClickHandler extends Activity {
    private static final String TAG = "PhotoAppWidgetClickHandler";

    private boolean isValidDataUri(Uri dataUri) {
        if (dataUri == null) return false;
        try {
            AssetFileDescriptor f = getContentResolver()
                    .openAssetFileDescriptor(dataUri, "r");
            f.close();
            return true;
        } catch (Throwable e) {
            Log.w(TAG, "cannot open uri: " + dataUri, e);
            return false;
        }
    }

    @Override
    @TargetApi(ApiHelper.VERSION_CODES.HONEYCOMB)
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        // The behavior is changed in JB, refer to b/6384492 for more details
        boolean tediousBack = Build.VERSION.SDK_INT >= ApiHelper.VERSION_CODES.JELLY_BEAN;
        Uri uri = getIntent().getData();
        Intent intent;
        if (isValidDataUri(uri)) {
            intent = new Intent(Intent.ACTION_VIEW, uri);
            if (tediousBack) {
                intent.putExtra(PhotoPage.KEY_TREAT_BACK_AS_UP, true);
            }
        } else {
            Toast.makeText(this,
                    R.string.no_such_item, Toast.LENGTH_LONG).show();
            intent = new Intent(this, GalleryActivity.class);
        }
        if (tediousBack) {
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        }
        startActivity(intent);
        finish();
    }
}
