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

import java.util.ArrayList;

public abstract class Clustering {
    public abstract void run(MediaSet baseSet);
    public abstract int getNumberOfClusters();
    public abstract ArrayList<Path> getCluster(int index);
    public abstract String getClusterName(int index);
    public MediaItem getClusterCover(int index) {
        return null;
    }
}
