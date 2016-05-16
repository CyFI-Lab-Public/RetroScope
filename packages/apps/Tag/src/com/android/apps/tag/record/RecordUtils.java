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
 * limitations under the License
 */

package com.android.apps.tag.record;

import com.android.apps.tag.R;
import com.google.common.collect.Lists;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utilities for parsed records to use.
 */
public class RecordUtils {
    /**
     * Contains info for click events that happen on views created via {@link #getViewsForIntent}.
     */
    public static final class ClickInfo {
        public Activity activity;
        public Intent intent;

        public ClickInfo(Activity activity, Intent intent) {
            this.activity = activity;
            this.intent = intent;
        }
    }

    /**
     * Creates one or more views for a parsed record that wants to display an actionable intent.
     * The views will have a {@link ClickInfo} set as their tag.
     */
    public static View getViewsForIntent(Activity activity, LayoutInflater inflater,
            ViewGroup parent, OnClickListener listener, Intent intent, String description) {
        // Lookup which packages can handle this intent.
        PackageManager pm = activity.getPackageManager();
        int flags = PackageManager.GET_RESOLVED_FILTER | PackageManager.MATCH_DEFAULT_ONLY;
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, flags);
        int numActivities = activities.size();
        if (numActivities == 0 || (numActivities == 1 && !activities.get(0).activityInfo.enabled)) {
            TextView text = (TextView) inflater.inflate(R.layout.tag_text, parent, false);
            text.setText(description);
            return text;
        } else if (numActivities == 1) {
            return buildActivityView(activity, activities.get(0), pm, inflater, parent, listener,
                    intent, description);
        } else {
            // Build a container to hold the multiple entries
            LinearLayout container = new LinearLayout(activity);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            // Create an entry for each activity that can handle the URI
            for (ResolveInfo resolveInfo : activities) {
                if (!resolveInfo.activityInfo.enabled) {
                    continue;
                }

                if (container.getChildCount() > 0) {
                    inflater.inflate(R.layout.tag_divider, container);
                }
                // Clone the intent for each view so they can each have their own components setup
                Intent clone = new Intent(intent);
                container.addView(buildActivityView(activity, resolveInfo, pm, inflater, container,
                        listener, clone, description));
            }
            return container;
        }
    }

    /**
     * Build a view to display a single activity that can handle this URI.
     */
    private static View buildActivityView(Activity activity, ResolveInfo resolveInfo, PackageManager pm,
            LayoutInflater inflater, ViewGroup parent, OnClickListener listener, Intent intent,
            String defaultText) {
        ActivityInfo activityInfo = resolveInfo.activityInfo;

        intent.setAction(resolveInfo.filter.getAction(0));
        intent.setComponent(new ComponentName(activityInfo.packageName, activityInfo.name));

        View item = inflater.inflate(R.layout.tag_uri, parent, false);
        item.setOnClickListener(listener);
        item.setTag(new ClickInfo(activity, intent));

        ImageView icon = (ImageView) item.findViewById(R.id.icon);
        icon.setImageDrawable(resolveInfo.loadIcon(pm));

        TextView text = (TextView) item.findViewById(R.id.secondary);
        text.setText(resolveInfo.loadLabel(pm));

        text = (TextView) item.findViewById(R.id.primary);
        text.setText(defaultText);

        return item;
    }
}
