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
import android.os.Handler;
import android.os.Looper;
import android.util.FloatMath;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ReverseGeocoder;

import java.util.ArrayList;

class LocationClustering extends Clustering {
    @SuppressWarnings("unused")
    private static final String TAG = "LocationClustering";

    private static final int MIN_GROUPS = 1;
    private static final int MAX_GROUPS = 20;
    private static final int MAX_ITERATIONS = 30;

    // If the total distance change is less than this ratio, stop iterating.
    private static final float STOP_CHANGE_RATIO = 0.01f;
    private Context mContext;
    private ArrayList<ArrayList<SmallItem>> mClusters;
    private ArrayList<String> mNames;
    private String mNoLocationString;
    private Handler mHandler;

    private static class Point {
        public Point(double lat, double lng) {
            latRad = Math.toRadians(lat);
            lngRad = Math.toRadians(lng);
        }
        public Point() {}
        public double latRad, lngRad;
    }

    private static class SmallItem {
        Path path;
        double lat, lng;
    }

    public LocationClustering(Context context) {
        mContext = context;
        mNoLocationString = mContext.getResources().getString(R.string.no_location);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void run(MediaSet baseSet) {
        final int total = baseSet.getTotalMediaItemCount();
        final SmallItem[] buf = new SmallItem[total];
        // Separate items to two sets: with or without lat-long.
        final double[] latLong = new double[2];
        baseSet.enumerateTotalMediaItems(new MediaSet.ItemConsumer() {
            @Override
            public void consume(int index, MediaItem item) {
                if (index < 0 || index >= total) return;
                SmallItem s = new SmallItem();
                s.path = item.getPath();
                item.getLatLong(latLong);
                s.lat = latLong[0];
                s.lng = latLong[1];
                buf[index] = s;
            }
        });

        final ArrayList<SmallItem> withLatLong = new ArrayList<SmallItem>();
        final ArrayList<SmallItem> withoutLatLong = new ArrayList<SmallItem>();
        final ArrayList<Point> points = new ArrayList<Point>();
        for (int i = 0; i < total; i++) {
            SmallItem s = buf[i];
            if (s == null) continue;
            if (GalleryUtils.isValidLocation(s.lat, s.lng)) {
                withLatLong.add(s);
                points.add(new Point(s.lat, s.lng));
            } else {
                withoutLatLong.add(s);
            }
        }

        ArrayList<ArrayList<SmallItem>> clusters = new ArrayList<ArrayList<SmallItem>>();

        int m = withLatLong.size();
        if (m > 0) {
            // cluster the items with lat-long
            Point[] pointsArray = new Point[m];
            pointsArray = points.toArray(pointsArray);
            int[] bestK = new int[1];
            int[] index = kMeans(pointsArray, bestK);

            for (int i = 0; i < bestK[0]; i++) {
                clusters.add(new ArrayList<SmallItem>());
            }

            for (int i = 0; i < m; i++) {
                clusters.get(index[i]).add(withLatLong.get(i));
            }
        }

        ReverseGeocoder geocoder = new ReverseGeocoder(mContext);
        mNames = new ArrayList<String>();
        boolean hasUnresolvedAddress = false;
        mClusters = new ArrayList<ArrayList<SmallItem>>();
        for (ArrayList<SmallItem> cluster : clusters) {
            String name = generateName(cluster, geocoder);
            if (name != null) {
                mNames.add(name);
                mClusters.add(cluster);
            } else {
                // move cluster-i to no location cluster
                withoutLatLong.addAll(cluster);
                hasUnresolvedAddress = true;
            }
        }

        if (withoutLatLong.size() > 0) {
            mNames.add(mNoLocationString);
            mClusters.add(withoutLatLong);
        }

        if (hasUnresolvedAddress) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, R.string.no_connectivity,
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private static String generateName(ArrayList<SmallItem> items,
            ReverseGeocoder geocoder) {
        ReverseGeocoder.SetLatLong set = new ReverseGeocoder.SetLatLong();

        int n = items.size();
        for (int i = 0; i < n; i++) {
            SmallItem item = items.get(i);
            double itemLatitude = item.lat;
            double itemLongitude = item.lng;

            if (set.mMinLatLatitude > itemLatitude) {
                set.mMinLatLatitude = itemLatitude;
                set.mMinLatLongitude = itemLongitude;
            }
            if (set.mMaxLatLatitude < itemLatitude) {
                set.mMaxLatLatitude = itemLatitude;
                set.mMaxLatLongitude = itemLongitude;
            }
            if (set.mMinLonLongitude > itemLongitude) {
                set.mMinLonLatitude = itemLatitude;
                set.mMinLonLongitude = itemLongitude;
            }
            if (set.mMaxLonLongitude < itemLongitude) {
                set.mMaxLonLatitude = itemLatitude;
                set.mMaxLonLongitude = itemLongitude;
            }
        }

        return geocoder.computeAddress(set);
    }

    @Override
    public int getNumberOfClusters() {
        return mClusters.size();
    }

    @Override
    public ArrayList<Path> getCluster(int index) {
        ArrayList<SmallItem> items = mClusters.get(index);
        ArrayList<Path> result = new ArrayList<Path>(items.size());
        for (int i = 0, n = items.size(); i < n; i++) {
            result.add(items.get(i).path);
        }
        return result;
    }

    @Override
    public String getClusterName(int index) {
        return mNames.get(index);
    }

    // Input: n points
    // Output: the best k is stored in bestK[0], and the return value is the
    // an array which specifies the group that each point belongs (0 to k - 1).
    private static int[] kMeans(Point points[], int[] bestK) {
        int n = points.length;

        // min and max number of groups wanted
        int minK = Math.min(n, MIN_GROUPS);
        int maxK = Math.min(n, MAX_GROUPS);

        Point[] center = new Point[maxK];  // center of each group.
        Point[] groupSum = new Point[maxK];  // sum of points in each group.
        int[] groupCount = new int[maxK];  // number of points in each group.
        int[] grouping = new int[n]; // The group assignment for each point.

        for (int i = 0; i < maxK; i++) {
            center[i] = new Point();
            groupSum[i] = new Point();
        }

        // The score we want to minimize is:
        //   (sum of distance from each point to its group center) * sqrt(k).
        float bestScore = Float.MAX_VALUE;
        // The best group assignment up to now.
        int[] bestGrouping = new int[n];
        // The best K up to now.
        bestK[0] = 1;

        float lastDistance = 0;
        float totalDistance = 0;

        for (int k = minK; k <= maxK; k++) {
            // step 1: (arbitrarily) pick k points as the initial centers.
            int delta = n / k;
            for (int i = 0; i < k; i++) {
                Point p = points[i * delta];
                center[i].latRad = p.latRad;
                center[i].lngRad = p.lngRad;
            }

            for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
                // step 2: assign each point to the nearest center.
                for (int i = 0; i < k; i++) {
                    groupSum[i].latRad = 0;
                    groupSum[i].lngRad = 0;
                    groupCount[i] = 0;
                }
                totalDistance = 0;

                for (int i = 0; i < n; i++) {
                    Point p = points[i];
                    float bestDistance = Float.MAX_VALUE;
                    int bestIndex = 0;
                    for (int j = 0; j < k; j++) {
                        float distance = (float) GalleryUtils.fastDistanceMeters(
                                p.latRad, p.lngRad, center[j].latRad, center[j].lngRad);
                        // We may have small non-zero distance introduced by
                        // floating point calculation, so zero out small
                        // distances less than 1 meter.
                        if (distance < 1) {
                            distance = 0;
                        }
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestIndex = j;
                        }
                    }
                    grouping[i] = bestIndex;
                    groupCount[bestIndex]++;
                    groupSum[bestIndex].latRad += p.latRad;
                    groupSum[bestIndex].lngRad += p.lngRad;
                    totalDistance += bestDistance;
                }

                // step 3: calculate new centers
                for (int i = 0; i < k; i++) {
                    if (groupCount[i] > 0) {
                        center[i].latRad = groupSum[i].latRad / groupCount[i];
                        center[i].lngRad = groupSum[i].lngRad / groupCount[i];
                    }
                }

                if (totalDistance == 0 || (Math.abs(lastDistance - totalDistance)
                        / totalDistance) < STOP_CHANGE_RATIO) {
                    break;
                }
                lastDistance = totalDistance;
            }

            // step 4: remove empty groups and reassign group number
            int reassign[] = new int[k];
            int realK = 0;
            for (int i = 0; i < k; i++) {
                if (groupCount[i] > 0) {
                    reassign[i] = realK++;
                }
            }

            // step 5: calculate the final score
            float score = totalDistance * FloatMath.sqrt(realK);

            if (score < bestScore) {
                bestScore = score;
                bestK[0] = realK;
                for (int i = 0; i < n; i++) {
                    bestGrouping[i] = reassign[grouping[i]];
                }
                if (score == 0) {
                    break;
                }
            }
        }
        return bestGrouping;
    }
}
