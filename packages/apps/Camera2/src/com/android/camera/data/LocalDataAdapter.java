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

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import static com.android.camera.ui.FilmStripView.DataAdapter;

/**
 * An interface which extends {@link DataAdapter} and defines operations on
 * the data in the local camera folder.
 */
public interface LocalDataAdapter extends DataAdapter {

    /**
     * Request for loading the local data.
     *
     * @param resolver  {@link ContentResolver} used for data loading.
     */
    public void requestLoad(ContentResolver resolver);

    /**
     * Returns the specified {@link LocalData}.
     *
     * @param dataID The ID of the {@link LocalData} to get.
     * @return The {@link LocalData} to get. {@code null} if not available.
     */
    public LocalData getLocalData(int dataID);

    /**
     * Remove the data in the local camera folder.
     *
     * @param context       {@link Context} used to remove the data.
     * @param dataID  ID of data to be deleted.
     */
    public void removeData(Context context, int dataID);

    /**
     * Add new local video data.
     *
     * @param resolver  {@link ContentResolver} used to add the data.
     * @param uri       {@link Uri} of the video.
     */
    public void addNewVideo(ContentResolver resolver, Uri uri);

    /**
     * Adds new local photo data.
     *
     * @param resolver  {@link ContentResolver} used to add the data.
     * @param uri       {@link Uri} of the photo.
     */
    public void addNewPhoto(ContentResolver resolver, Uri uri);

    /**
     * Refresh the data by {@link Uri}.
     *
     * @param resolver {@link ContentResolver} used to refresh the data.
     * @param uri The {@link Uri} of the data to refresh.
     */
    public void refresh(ContentResolver resolver, Uri uri);

    /**
     * Finds the {@link LocalData} of the specified content Uri.
     *
     * @param Uri  The content Uri of the {@link LocalData}.
     * @return     The index of the data. {@code -1} if not found.
     */
    public int findDataByContentUri(Uri uri);

    /**
     * Clears all the data currently loaded.
     */
    public void flush();

    /**
     * Executes the deletion task. Delete the data waiting in the deletion queue.
     *
     * @param context The {@link Context} from the caller.
     * @return        {@code true} if task has been executed, {@code false}
     *                otherwise.
     */
    public boolean executeDeletion(Context context);

    /**
     * Undo a deletion. If there is any data waiting to be deleted in the queue,
     * move it out of the deletion queue.
     *
     * @return {@code true} if there are items in the queue, {@code false} otherwise.
     */
    public boolean undoDataRemoval();

    /**
     * Update the data in a specific position.
     *
     * @param pos The position of the data to be updated.
     * @param data The new data.
     */
    public void updateData(int pos, LocalData data);

    /** Insert a data. */
    public void insertData(LocalData data);
}
