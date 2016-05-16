/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.incallui;

/**
 * Base class for Presenters.
 */
public abstract class Presenter<U extends Ui> {

    private U mUi;

    /**
     * Called after the UI view has been created.  That is when fragment.onViewCreated() is called.
     *
     * @param ui The Ui implementation that is now ready to be used.
     */
    public void onUiReady(U ui) {
        mUi = ui;
    }

    /**
     * Called when the UI view is destroyed in Fragment.onDestroyView().
     */
    public final void onUiDestroy(U ui) {
        onUiUnready(ui);
        mUi = null;
    }

    /**
     * To be overriden by Presenter implementations.  Called when the fragment is being
     * destroyed but before ui is set to null.
     */
    public void onUiUnready(U ui) {
    }

    public U getUi() {
        return mUi;
    }
}
