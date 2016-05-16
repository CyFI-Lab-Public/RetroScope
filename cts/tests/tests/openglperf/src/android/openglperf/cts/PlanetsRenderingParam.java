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

public class PlanetsRenderingParam {
    /** whether to use VBO for vertices data */
    public boolean mUseVboForVertices = true;
    /** whether to use VBO for indices data */
    public boolean mUseVboForIndices = true;
    /** number of indices buffer per one vertices buffer */
    public int mNumIndicesPerVertex = 1;
    /** number of planets to render. There is always a sun */
    public int mNumPlanets = 0;
    /** number of frames to render to calculate FPS and finish */
    public int mNumFrames = 100;
}
