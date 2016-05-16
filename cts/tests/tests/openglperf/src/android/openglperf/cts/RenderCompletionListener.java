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

package android.openglperf.cts;

/**
 * Interface used to notify the completion of requested rendering.
 */
public interface RenderCompletionListener {
    /**
     * @param averageFps average of total frames
     * @param numTriangles Number of triangles in geometric model
     * @param frameInterval interval for each frame in ms. Do not use the first one and the last one.
     */
    void onRenderCompletion(float averageFps, int numTriangles, int[] frameInterval);

}
