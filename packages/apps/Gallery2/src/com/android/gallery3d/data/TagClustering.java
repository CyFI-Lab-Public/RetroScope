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

package com.android.gallery3d.data;

import android.content.Context;

import com.android.gallery3d.R;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class TagClustering extends Clustering {
    @SuppressWarnings("unused")
    private static final String TAG = "TagClustering";

    private ArrayList<ArrayList<Path>> mClusters;
    private String[] mNames;
    private String mUntaggedString;

    public TagClustering(Context context) {
        mUntaggedString = context.getResources().getString(R.string.untagged);
    }

    @Override
    public void run(MediaSet baseSet) {
        final TreeMap<String, ArrayList<Path>> map =
                new TreeMap<String, ArrayList<Path>>();
        final ArrayList<Path> untagged = new ArrayList<Path>();

        baseSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                Path path = item.getPath();

                String[] tags = item.getTags();
                if (tags == null || tags.length == 0) {
                    untagged.add(path);
                    return;
                }
                for (int j = 0; j < tags.length; j++) {
                    String key = tags[j];
                    ArrayList<Path> list = map.get(key);
                    if (list == null) {
                        list = new ArrayList<Path>();
                        map.put(key, list);
                    }
                    list.add(path);
                }
            }
        });

        int m = map.size();
        mClusters = new ArrayList<ArrayList<Path>>();
        mNames = new String[m + ((untagged.size() > 0) ? 1 : 0)];
        int i = 0;
        for (Map.Entry<String, ArrayList<Path>> entry : map.entrySet()) {
            mNames[i++] = entry.getKey();
            mClusters.add(entry.getValue());
        }
        if (untagged.size() > 0) {
            mNames[i++] = mUntaggedString;
            mClusters.add(untagged);
        }
    }

    @Override
    public int getNumberOfClusters() {
        return mClusters.size();
    }

    @Override
    public ArrayList<Path> getCluster(int index) {
        return mClusters.get(index);
    }

    @Override
    public String getClusterName(int index) {
        return mNames[index];
    }
}
