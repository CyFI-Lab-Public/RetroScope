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

package com.android.gallery3d.data;

import android.content.Context;
import android.graphics.Rect;

import com.android.gallery3d.R;
import com.android.gallery3d.picasasource.PicasaSource;

import java.util.ArrayList;
import java.util.TreeMap;

public class FaceClustering extends Clustering {
    @SuppressWarnings("unused")
    private static final String TAG = "FaceClustering";

    private FaceCluster[] mClusters;
    private String mUntaggedString;
    private Context mContext;

    private class FaceCluster {
        ArrayList<Path> mPaths = new ArrayList<Path>();
        String mName;
        MediaItem mCoverItem;
        Rect mCoverRegion;
        int mCoverFaceIndex;

        public FaceCluster(String name) {
            mName = name;
        }

        public void add(MediaItem item, int faceIndex) {
            Path path = item.getPath();
            mPaths.add(path);
            Face[] faces = item.getFaces();
            if (faces != null) {
                Face face = faces[faceIndex];
                if (mCoverItem == null) {
                    mCoverItem = item;
                    mCoverRegion = face.getPosition();
                    mCoverFaceIndex = faceIndex;
                } else {
                    Rect region = face.getPosition();
                    if (mCoverRegion.width() < region.width() &&
                            mCoverRegion.height() < region.height()) {
                        mCoverItem = item;
                        mCoverRegion = face.getPosition();
                        mCoverFaceIndex = faceIndex;
                    }
                }
            }
        }

        public int size() {
            return mPaths.size();
        }

        public MediaItem getCover() {
            if (mCoverItem != null) {
                if (PicasaSource.isPicasaImage(mCoverItem)) {
                    return PicasaSource.getFaceItem(mContext, mCoverItem, mCoverFaceIndex);
                } else {
                    return mCoverItem;
                }
            }
            return null;
        }
    }

    public FaceClustering(Context context) {
        mUntaggedString = context.getResources().getString(R.string.untagged);
        mContext = context;
    }

    @Override
    public void run(MediaSet baseSet) {
        final TreeMap<Face, FaceCluster> map =
                new TreeMap<Face, FaceCluster>();
        final FaceCluster untagged = new FaceCluster(mUntaggedString);

        baseSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                Face[] faces = item.getFaces();
                if (faces == null || faces.length == 0) {
                    untagged.add(item, -1);
                    return;
                }
                for (int j = 0; j < faces.length; j++) {
                    Face face = faces[j];
                    FaceCluster cluster = map.get(face);
                    if (cluster == null) {
                        cluster = new FaceCluster(face.getName());
                        map.put(face, cluster);
                    }
                    cluster.add(item, j);
                }
            }
        });

        int m = map.size();
        mClusters = map.values().toArray(new FaceCluster[m + ((untagged.size() > 0) ? 1 : 0)]);
        if (untagged.size() > 0) {
            mClusters[m] = untagged;
        }
    }

    @Override
    public int getNumberOfClusters() {
        return mClusters.length;
    }

    @Override
    public ArrayList<Path> getCluster(int index) {
        return mClusters[index].mPaths;
    }

    @Override
    public String getClusterName(int index) {
        return mClusters[index].mName;
    }

    @Override
    public MediaItem getClusterCover(int index) {
        return mClusters[index].getCover();
    }
}
