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

package com.android.gallery3d.app;

import com.android.gallery3d.R;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;

// This class handles filtering and clustering.
//
// We allow at most only one filter operation at a time (Currently it
// doesn't make sense to use more than one). Also each clustering operation
// can be applied at most once. In addition, there is one more constraint
// ("fixed set constraint") described below.
//
// A clustered album (not including album set) and its base sets are fixed.
// For example,
//
// /cluster/{base_set}/time/7
//
// This set and all sets inside base_set (recursively) are fixed because
// 1. We can not change this set to use another clustering condition (like
//    changing "time" to "location").
// 2. Neither can we change any set in the base_set.
// The reason is in both cases the 7th set may not exist in the new clustering.
// ---------------------
// newPath operation: create a new path based on a source path and put an extra
// condition on top of it:
//
// T = newFilterPath(S, filterType);
// T = newClusterPath(S, clusterType);
//
// Similar functions can be used to replace the current condition (if there is one).
//
// T = switchFilterPath(S, filterType);
// T = switchClusterPath(S, clusterType);
//
// For all fixed set in the path defined above, if some clusterType and
// filterType are already used, they cannot not be used as parameter for these
// functions. setupMenuItems() makes sure those types cannot be selected.
//
public class FilterUtils {
    @SuppressWarnings("unused")
    private static final String TAG = "FilterUtils";

    public static final int CLUSTER_BY_ALBUM = 1;
    public static final int CLUSTER_BY_TIME = 2;
    public static final int CLUSTER_BY_LOCATION = 4;
    public static final int CLUSTER_BY_TAG = 8;
    public static final int CLUSTER_BY_SIZE = 16;
    public static final int CLUSTER_BY_FACE = 32;

    public static final int FILTER_IMAGE_ONLY = 1;
    public static final int FILTER_VIDEO_ONLY = 2;
    public static final int FILTER_ALL = 4;

    // These are indices of the return values of getAppliedFilters().
    // The _F suffix means "fixed".
    private static final int CLUSTER_TYPE = 0;
    private static final int FILTER_TYPE = 1;
    private static final int CLUSTER_TYPE_F = 2;
    private static final int FILTER_TYPE_F = 3;
    private static final int CLUSTER_CURRENT_TYPE = 4;
    private static final int FILTER_CURRENT_TYPE = 5;

    public static void setupMenuItems(GalleryActionBar actionBar, Path path, boolean inAlbum) {
        int[] result = new int[6];
        getAppliedFilters(path, result);
        int ctype = result[CLUSTER_TYPE];
        int ftype = result[FILTER_TYPE];
        int ftypef = result[FILTER_TYPE_F];
        int ccurrent = result[CLUSTER_CURRENT_TYPE];
        int fcurrent = result[FILTER_CURRENT_TYPE];

        setMenuItemApplied(actionBar, CLUSTER_BY_TIME,
                (ctype & CLUSTER_BY_TIME) != 0, (ccurrent & CLUSTER_BY_TIME) != 0);
        setMenuItemApplied(actionBar, CLUSTER_BY_LOCATION,
                (ctype & CLUSTER_BY_LOCATION) != 0, (ccurrent & CLUSTER_BY_LOCATION) != 0);
        setMenuItemApplied(actionBar, CLUSTER_BY_TAG,
                (ctype & CLUSTER_BY_TAG) != 0, (ccurrent & CLUSTER_BY_TAG) != 0);
        setMenuItemApplied(actionBar, CLUSTER_BY_FACE,
                (ctype & CLUSTER_BY_FACE) != 0, (ccurrent & CLUSTER_BY_FACE) != 0);

        actionBar.setClusterItemVisibility(CLUSTER_BY_ALBUM, !inAlbum || ctype == 0);

        setMenuItemApplied(actionBar, R.id.action_cluster_album, ctype == 0,
                ccurrent == 0);

        // A filtering is available if it's not applied, and the old filtering
        // (if any) is not fixed.
        setMenuItemAppliedEnabled(actionBar, R.string.show_images_only,
                (ftype & FILTER_IMAGE_ONLY) != 0,
                (ftype & FILTER_IMAGE_ONLY) == 0 && ftypef == 0,
                (fcurrent & FILTER_IMAGE_ONLY) != 0);
        setMenuItemAppliedEnabled(actionBar, R.string.show_videos_only,
                (ftype & FILTER_VIDEO_ONLY) != 0,
                (ftype & FILTER_VIDEO_ONLY) == 0 && ftypef == 0,
                (fcurrent & FILTER_VIDEO_ONLY) != 0);
        setMenuItemAppliedEnabled(actionBar, R.string.show_all,
                ftype == 0, ftype != 0 && ftypef == 0, fcurrent == 0);
    }

    // Gets the filters applied in the path.
    private static void getAppliedFilters(Path path, int[] result) {
        getAppliedFilters(path, result, false);
    }

    private static void getAppliedFilters(Path path, int[] result, boolean underCluster) {
        String[] segments = path.split();
        // Recurse into sub media sets.
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].startsWith("{")) {
                String[] sets = Path.splitSequence(segments[i]);
                for (int j = 0; j < sets.length; j++) {
                    Path sub = Path.fromString(sets[j]);
                    getAppliedFilters(sub, result, underCluster);
                }
            }
        }

        // update current selection
        if (segments[0].equals("cluster")) {
            // if this is a clustered album, set underCluster to true.
            if (segments.length == 4) {
                underCluster = true;
            }

            int ctype = toClusterType(segments[2]);
            result[CLUSTER_TYPE] |= ctype;
            result[CLUSTER_CURRENT_TYPE] = ctype;
            if (underCluster) {
                result[CLUSTER_TYPE_F] |= ctype;
            }
        }
    }

    private static int toClusterType(String s) {
        if (s.equals("time")) {
            return CLUSTER_BY_TIME;
        } else if (s.equals("location")) {
            return CLUSTER_BY_LOCATION;
        } else if (s.equals("tag")) {
            return CLUSTER_BY_TAG;
        } else if (s.equals("size")) {
            return CLUSTER_BY_SIZE;
        } else if (s.equals("face")) {
            return CLUSTER_BY_FACE;
        }
        return 0;
    }

    private static void setMenuItemApplied(
            GalleryActionBar model, int id, boolean applied, boolean updateTitle) {
        model.setClusterItemEnabled(id, !applied);
    }

    private static void setMenuItemAppliedEnabled(GalleryActionBar model, int id, boolean applied, boolean enabled, boolean updateTitle) {
        model.setClusterItemEnabled(id, enabled);
    }

    // Add a specified filter to the path.
    public static String newFilterPath(String base, int filterType) {
        int mediaType;
        switch (filterType) {
            case FILTER_IMAGE_ONLY:
                mediaType = MediaObject.MEDIA_TYPE_IMAGE;
                break;
            case FILTER_VIDEO_ONLY:
                mediaType = MediaObject.MEDIA_TYPE_VIDEO;
                break;
            default:  /* FILTER_ALL */
                return base;
        }

        return "/filter/mediatype/" + mediaType + "/{" + base + "}";
    }

    // Add a specified clustering to the path.
    public static String newClusterPath(String base, int clusterType) {
        String kind;
        switch (clusterType) {
            case CLUSTER_BY_TIME:
                kind = "time";
                break;
            case CLUSTER_BY_LOCATION:
                kind = "location";
                break;
            case CLUSTER_BY_TAG:
                kind = "tag";
                break;
            case CLUSTER_BY_SIZE:
                kind = "size";
                break;
            case CLUSTER_BY_FACE:
                kind = "face";
                break;
            default: /* CLUSTER_BY_ALBUM */
                return base;
        }

        return "/cluster/{" + base + "}/" + kind;
    }

    // Change the topmost clustering to the specified type.
    public static String switchClusterPath(String base, int clusterType) {
        return newClusterPath(removeOneClusterFromPath(base), clusterType);
    }

    // Remove the topmost clustering (if any) from the path.
    private static String removeOneClusterFromPath(String base) {
        boolean[] done = new boolean[1];
        return removeOneClusterFromPath(base, done);
    }

    private static String removeOneClusterFromPath(String base, boolean[] done) {
        if (done[0]) return base;

        String[] segments = Path.split(base);
        if (segments[0].equals("cluster")) {
            done[0] = true;
            return Path.splitSequence(segments[1])[0];
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            sb.append("/");
            if (segments[i].startsWith("{")) {
                sb.append("{");
                String[] sets = Path.splitSequence(segments[i]);
                for (int j = 0; j < sets.length; j++) {
                    if (j > 0) {
                        sb.append(",");
                    }
                    sb.append(removeOneClusterFromPath(sets[j], done));
                }
                sb.append("}");
            } else {
                sb.append(segments[i]);
            }
        }
        return sb.toString();
    }
}
