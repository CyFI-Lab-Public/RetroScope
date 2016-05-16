/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.ui;

import android.view.View;

/**
 * Represents an item that can be dismissed by the SwipeableListView.
 */
public interface SwipeableItemView {
    public SwipeableView getSwipeableView();

    public boolean canChildBeDismissed();

    public void dismiss();

    /**
     * Returns the minimum allowed displacement in the Y axis that is considered a scroll. After
     * this displacement, all future events are considered scroll events rather than swipes.
     * @return
     */
    public float getMinAllowScrollDistance();

    public static class SwipeableView {
        public static SwipeableView from(View view) {
            view.setClickable(true);
            return new SwipeableView(view);
        }

        private final View mView;
        private SwipeableView(View view) {
            mView = view;
        }

        public View getView() {
            return mView;
        }
    }
}
