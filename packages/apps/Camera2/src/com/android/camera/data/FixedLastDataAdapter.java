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

package com.android.camera.data;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.View;

import com.android.camera.ui.FilmStripView;

/**
 * A {@link LocalDataAdapter} which puts a {@link LocalData} fixed at the last
 * position. It's done by combining a {@link LocalData} and another
 * {@link LocalDataAdapter}.
 */
public class FixedLastDataAdapter extends AbstractLocalDataAdapterWrapper {

    private LocalData mLastData;
    private Listener mListener;

    /**
     * Constructor.
     *
     * @param wrappedAdapter  The {@link LocalDataAdapter} to be wrapped.
     * @param lastData       The {@link LocalData} to be placed at the last position.
     */
    public FixedLastDataAdapter(
            LocalDataAdapter wrappedAdapter,
            LocalData lastData) {
        super(wrappedAdapter);
        if (lastData == null) {
            throw new AssertionError("data is null");
        }
        mLastData = lastData;
    }

    @Override
    public void setListener(Listener listener) {
        super.setListener(listener);
        mListener = listener;
    }

    @Override
    public LocalData getLocalData(int dataID) {
        int totalNumber = mAdapter.getTotalNumber();

        if (dataID < totalNumber) {
            return mAdapter.getLocalData(dataID);
        } else if (dataID == totalNumber) {
            return mLastData;
        }

        return null;
    }

    @Override
    public void removeData(Context context, int dataID) {
        if (dataID < mAdapter.getTotalNumber()) {
            mAdapter.removeData(context, dataID);
        }
    }

    @Override
    public int findDataByContentUri(Uri uri) {
        return mAdapter.findDataByContentUri(uri);
    }

    @Override
    public void updateData(final int pos, LocalData data) {
        int totalNumber = mAdapter.getTotalNumber();

        if (pos < totalNumber) {
            mAdapter.updateData(pos, data);
        } else if (pos == totalNumber) {
            mLastData = data;
            if (mListener != null) {
                mListener.onDataUpdated(new UpdateReporter() {
                    @Override
                    public boolean isDataRemoved(int dataID) {
                        return false;
                    }

                    @Override
                    public boolean isDataUpdated(int dataID) {
                        return (dataID == pos);
                    }
                });
            }
        }
    }

    @Override
    public int getTotalNumber() {
        return mAdapter.getTotalNumber() + 1;
    }

    @Override
    public View getView(Activity activity, int dataID) {
        int totalNumber = mAdapter.getTotalNumber();

        if (dataID < totalNumber) {
            return mAdapter.getView(activity, dataID);
        } else if (dataID == totalNumber) {
            return mLastData.getView(activity,
                    mSuggestedWidth, mSuggestedHeight, null, null);
        }

        return null;
    }

    @Override
    public FilmStripView.ImageData getImageData(int dataID) {
        int totalNumber = mAdapter.getTotalNumber();

        if (dataID < totalNumber) {
            return mAdapter.getImageData(dataID);
        } else if (dataID == totalNumber) {
            return mLastData;
        }
        return null;
    }

    @Override
    public boolean canSwipeInFullScreen(int dataID) {
        int totalNumber = mAdapter.getTotalNumber();

        if (dataID < totalNumber) {
            return mAdapter.canSwipeInFullScreen(dataID);
        } else if (dataID == totalNumber) {
            return mLastData.canSwipeInFullScreen();
        }
        return false;
    }
}

