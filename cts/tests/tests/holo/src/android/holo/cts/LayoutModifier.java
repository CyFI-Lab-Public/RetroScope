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

package android.holo.cts;

import android.view.View;

/**
 * Interface used to do further setup on a view after it has been inflated.
 */
public interface LayoutModifier {

    /** Actions to take before inflating the view. */
    void prepare();

    /**
     * @param view inflated by the test activity
     * @return the same view or another view that will be snapshotted by the test
     */
    View modifyView(View view);
}
