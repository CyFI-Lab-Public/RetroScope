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

package com.android.wallpaper.livepicker;

import java.io.IOException;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.service.wallpaper.WallpaperService;
import android.util.Log;

public class LiveWallpaperChange extends Activity {
    private static final String TAG = "CHANGE_LIVE_WALLPAPER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Parcelable obj = getIntent().getParcelableExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT);
        if (obj == null || !(obj instanceof ComponentName)) {
            Log.w(TAG, "No LIVE_WALLPAPER_COMPONENT extra supplied");
            finish();
            return;
        }

        ComponentName comp = (ComponentName)obj;

        // Get the information about this component.  Implemented this way
        // to not allow us to direct the caller to a service that is not a
        // live wallpaper.
        Intent queryIntent = new Intent(WallpaperService.SERVICE_INTERFACE);
        queryIntent.setPackage(comp.getPackageName());
        List<ResolveInfo> list = getPackageManager().queryIntentServices(
                queryIntent, PackageManager.GET_META_DATA);
        if (list != null) {
            for (int i=0; i<list.size(); i++) {
                ResolveInfo ri = list.get(i);
                if (ri.serviceInfo.name.equals(comp.getClassName())) {
                    WallpaperInfo info = null;
                    try {
                        info = new WallpaperInfo(this, ri);
                    } catch (XmlPullParserException e) {
                        Log.w(TAG, "Bad wallpaper " + ri.serviceInfo, e);
                        finish();
                        return;
                    } catch (IOException e) {
                        Log.w(TAG, "Bad wallpaper " + ri.serviceInfo, e);
                        finish();
                        return;
                    }
                    Intent intent = new Intent(WallpaperService.SERVICE_INTERFACE);
                    intent.setClassName(info.getPackageName(), info.getServiceName());
                    LiveWallpaperPreview.showPreview(this, 0, intent, info);
                    finish();
                    return;
                }
            }
        }

        Log.w(TAG, "Not a live wallpaper: " + comp);
        finish();
    }
}
