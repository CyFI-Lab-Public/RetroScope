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
package com.android.quicksearchbox.ui;

import android.view.View;
import android.widget.AbsListView;

/**
 * Interface for suggestions list UI views.
 */
public interface SuggestionsListView<A> {

    /**
     * See {@link View#setOnKeyListener}.
     */
    void setOnKeyListener(View.OnKeyListener l);

    /**
     * See {@link AbsListView#setOnScrollListener}.
     */
    void setOnScrollListener(AbsListView.OnScrollListener l);

    /**
     * See {@link View#setOnFocusChangeListener}.
     */
    void setOnFocusChangeListener(View.OnFocusChangeListener l);

    /**
     * See {@link View#setVisibility}.
     */
    void setVisibility(int visibility);

    /**
     * Sets the adapter for the list. See {@link AbsListView#setAdapter}
     */
    void setSuggestionsAdapter(SuggestionsAdapter<A> adapter);

    /**
     * Gets the adapter for the list.
     */
    SuggestionsAdapter<A> getSuggestionsAdapter();

    /**
     * Gets the ID of the currently selected item.
     */
    long getSelectedItemId();

}
