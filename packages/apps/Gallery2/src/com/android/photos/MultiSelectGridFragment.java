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
 * limitations under the License.
 */

package com.android.photos;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.android.gallery3d.R;

public abstract class MultiSelectGridFragment extends Fragment
        implements MultiChoiceManager.Delegate, AdapterView.OnItemClickListener {

    final private Handler mHandler = new Handler();

    final private Runnable mRequestFocus = new Runnable() {
        @Override
        public void run() {
            mGrid.focusableViewAvailable(mGrid);
        }
    };

    ListAdapter mAdapter;
    GridView mGrid;
    TextView mEmptyView;
    View mProgressContainer;
    View mGridContainer;
    CharSequence mEmptyText;
    boolean mGridShown;
    MultiChoiceManager.Provider mHost;

    public MultiSelectGridFragment() {
    }

    /**
     * Provide default implementation to return a simple grid view. Subclasses
     * can override to replace with their own layout. If doing so, the returned
     * view hierarchy <em>must</em> have a GridView whose id is
     * {@link android.R.id#grid android.R.id.list} and can optionally have a
     * sibling text view id {@link android.R.id#empty android.R.id.empty} that
     * is to be shown when the grid is empty.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.multigrid_content, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mHost = (MultiChoiceManager.Provider) activity;
        if (mGrid != null) {
            mGrid.setMultiChoiceModeListener(mHost.getMultiChoiceManager());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mHost = null;
    }

    /**
     * Attach to grid view once the view hierarchy has been created.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureGrid();
    }

    /**
     * Detach from grid view.
     */
    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRequestFocus);
        mGrid = null;
        mGridShown = false;
        mEmptyView = null;
        mProgressContainer = mGridContainer = null;
        super.onDestroyView();
    }

    /**
     * This method will be called when an item in the grid is selected.
     * Subclasses should override. Subclasses can call
     * getGridView().getItemAtPosition(position) if they need to access the data
     * associated with the selected item.
     *
     * @param g The GridView where the click happened
     * @param v The view that was clicked within the GridView
     * @param position The position of the view in the grid
     * @param id The id of the item that was clicked
     */
    public void onGridItemClick(GridView g, View v, int position, long id) {
    }

    /**
     * Provide the cursor for the grid view.
     */
    public void setAdapter(ListAdapter adapter) {
        boolean hadAdapter = mAdapter != null;
        mAdapter = adapter;
        if (mGrid != null) {
            mGrid.setAdapter(adapter);
            if (!mGridShown && !hadAdapter) {
                // The grid was hidden, and previously didn't have an
                // adapter. It is now time to show it.
                setGridShown(true, getView().getWindowToken() != null);
            }
        }
    }

    /**
     * Set the currently selected grid item to the specified position with the
     * adapter's data
     *
     * @param position
     */
    public void setSelection(int position) {
        ensureGrid();
        mGrid.setSelection(position);
    }

    /**
     * Get the position of the currently selected grid item.
     */
    public int getSelectedItemPosition() {
        ensureGrid();
        return mGrid.getSelectedItemPosition();
    }

    /**
     * Get the cursor row ID of the currently selected grid item.
     */
    public long getSelectedItemId() {
        ensureGrid();
        return mGrid.getSelectedItemId();
    }

    /**
     * Get the activity's grid view widget.
     */
    public GridView getGridView() {
        ensureGrid();
        return mGrid;
    }

    /**
     * The default content for a MultiSelectGridFragment has a TextView that can
     * be shown when the grid is empty. If you would like to have it shown, call
     * this method to supply the text it should use.
     */
    public void setEmptyText(CharSequence text) {
        ensureGrid();
        if (mEmptyView == null) {
            return;
        }
        mEmptyView.setText(text);
        if (mEmptyText == null) {
            mGrid.setEmptyView(mEmptyView);
        }
        mEmptyText = text;
    }

    /**
     * Control whether the grid is being displayed. You can make it not
     * displayed if you are waiting for the initial data to show in it. During
     * this time an indeterminate progress indicator will be shown instead.
     * <p>
     * Applications do not normally need to use this themselves. The default
     * behavior of MultiSelectGridFragment is to start with the grid not being
     * shown, only showing it once an adapter is given with
     * {@link #setAdapter(ListAdapter)}. If the grid at that point had not been
     * shown, when it does get shown it will be do without the user ever seeing
     * the hidden state.
     *
     * @param shown If true, the grid view is shown; if false, the progress
     *            indicator. The initial value is true.
     */
    public void setGridShown(boolean shown) {
        setGridShown(shown, true);
    }

    /**
     * Like {@link #setGridShown(boolean)}, but no animation is used when
     * transitioning from the previous state.
     */
    public void setGridShownNoAnimation(boolean shown) {
        setGridShown(shown, false);
    }

    /**
     * Control whether the grid is being displayed. You can make it not
     * displayed if you are waiting for the initial data to show in it. During
     * this time an indeterminate progress indicator will be shown instead.
     *
     * @param shown If true, the grid view is shown; if false, the progress
     *            indicator. The initial value is true.
     * @param animate If true, an animation will be used to transition to the
     *            new state.
     */
    private void setGridShown(boolean shown, boolean animate) {
        ensureGrid();
        if (mProgressContainer == null) {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
        if (mGridShown == shown) {
            return;
        }
        mGridShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mGridContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            } else {
                mProgressContainer.clearAnimation();
                mGridContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.GONE);
            mGridContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mGridContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            } else {
                mProgressContainer.clearAnimation();
                mGridContainer.clearAnimation();
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mGridContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Get the ListAdapter associated with this activity's GridView.
     */
    public ListAdapter getAdapter() {
        return mGrid.getAdapter();
    }

    private void ensureGrid() {
        if (mGrid != null) {
            return;
        }
        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }
        if (root instanceof GridView) {
            mGrid = (GridView) root;
        } else {
            View empty = root.findViewById(android.R.id.empty);
            if (empty != null && empty instanceof TextView) {
                mEmptyView = (TextView) empty;
            }
            mProgressContainer = root.findViewById(R.id.progressContainer);
            mGridContainer = root.findViewById(R.id.gridContainer);
            View rawGridView = root.findViewById(android.R.id.list);
            if (!(rawGridView instanceof GridView)) {
                throw new RuntimeException(
                        "Content has view with id attribute 'android.R.id.list' "
                                + "that is not a GridView class");
            }
            mGrid = (GridView) rawGridView;
            if (mGrid == null) {
                throw new RuntimeException(
                        "Your content must have a GridView whose id attribute is " +
                                "'android.R.id.list'");
            }
            if (mEmptyView != null) {
                mGrid.setEmptyView(mEmptyView);
            }
        }
        mGridShown = true;
        mGrid.setOnItemClickListener(this);
        mGrid.setMultiChoiceModeListener(mHost.getMultiChoiceManager());
        if (mAdapter != null) {
            ListAdapter adapter = mAdapter;
            mAdapter = null;
            setAdapter(adapter);
        } else {
            // We are starting without an adapter, so assume we won't
            // have our data right away and start with the progress indicator.
            if (mProgressContainer != null) {
                setGridShown(false, false);
            }
        }
        mHandler.post(mRequestFocus);
    }

    @Override
    public Object getItemAtPosition(int position) {
        return getAdapter().getItem(position);
    }

    @Override
    public Object getPathForItemAtPosition(int position) {
        return getPathForItem(getItemAtPosition(position));
    }

    @Override
    public SparseBooleanArray getSelectedItemPositions() {
        return mGrid.getCheckedItemPositions();
    }

    @Override
    public int getSelectedItemCount() {
        return mGrid.getCheckedItemCount();
    }

    public abstract Object getPathForItem(Object item);

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        onGridItemClick((GridView) parent, v, position, id);
    }
}
