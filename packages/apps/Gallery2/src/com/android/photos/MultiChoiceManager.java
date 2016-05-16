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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore.Files.FileColumns;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ShareActionProvider;
import android.widget.ShareActionProvider.OnShareTargetSelectedListener;

import com.android.gallery3d.R;
import com.android.gallery3d.app.TrimVideo;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.crop.CropActivity;
import com.android.gallery3d.util.GalleryUtils;

import java.util.ArrayList;
import java.util.List;

public class MultiChoiceManager implements MultiChoiceModeListener,
    OnShareTargetSelectedListener, SelectionManager.SelectedUriSource {

    public interface Provider {
        public MultiChoiceManager getMultiChoiceManager();
    }

    public interface Delegate {
        public SparseBooleanArray getSelectedItemPositions();
        public int getSelectedItemCount();
        public int getItemMediaType(Object item);
        public int getItemSupportedOperations(Object item);
        public ArrayList<Uri> getSubItemUrisForItem(Object item);
        public Uri getItemUri(Object item);
        public Object getItemAtPosition(int position);
        public Object getPathForItemAtPosition(int position);
        public void deleteItemWithPath(Object itemPath);
    }

    private SelectionManager mSelectionManager;
    private ShareActionProvider mShareActionProvider;
    private ActionMode mActionMode;
    private Context mContext;
    private Delegate mDelegate;

    private ArrayList<Uri> mSelectedShareableUrisArray = new ArrayList<Uri>();

    public MultiChoiceManager(Activity activity) {
        mContext = activity;
        mSelectionManager = new SelectionManager(activity);
    }

    public void setDelegate(Delegate delegate) {
        if (mDelegate == delegate) {
            return;
        }
        if (mActionMode != null) {
            mActionMode.finish();
        }
        mDelegate = delegate;
    }

    @Override
    public ArrayList<Uri> getSelectedShareableUris() {
        return mSelectedShareableUrisArray;
    }

    private void updateSelectedTitle(ActionMode mode) {
        int count = mDelegate.getSelectedItemCount();
        mode.setTitle(mContext.getResources().getQuantityString(
                R.plurals.number_of_items_selected, count, count));
    }

    private String getItemMimetype(Object item) {
        int type = mDelegate.getItemMediaType(item);
        if (type == FileColumns.MEDIA_TYPE_IMAGE) {
            return GalleryUtils.MIME_TYPE_IMAGE;
        } else if (type == FileColumns.MEDIA_TYPE_VIDEO) {
            return GalleryUtils.MIME_TYPE_VIDEO;
        } else {
            return GalleryUtils.MIME_TYPE_ALL;
        }
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
            boolean checked) {
        updateSelectedTitle(mode);
        Object item = mDelegate.getItemAtPosition(position);

        int supported = mDelegate.getItemSupportedOperations(item);

        if ((supported & MediaObject.SUPPORT_SHARE) > 0) {
            ArrayList<Uri> subItems = mDelegate.getSubItemUrisForItem(item);
            if (checked) {
                mSelectedShareableUrisArray.addAll(subItems);
            } else {
                mSelectedShareableUrisArray.removeAll(subItems);
            }
        }

        mSelectionManager.onItemSelectedStateChanged(mShareActionProvider,
                mDelegate.getItemMediaType(item),
                supported,
                checked);
        updateActionItemVisibilities(mode.getMenu(),
                mSelectionManager.getSupportedOperations());
    }

    private void updateActionItemVisibilities(Menu menu, int supportedOperations) {
        MenuItem editItem = menu.findItem(R.id.menu_edit);
        MenuItem deleteItem = menu.findItem(R.id.menu_delete);
        MenuItem shareItem = menu.findItem(R.id.menu_share);
        MenuItem cropItem = menu.findItem(R.id.menu_crop);
        MenuItem trimItem = menu.findItem(R.id.menu_trim);
        MenuItem muteItem = menu.findItem(R.id.menu_mute);
        MenuItem setAsItem = menu.findItem(R.id.menu_set_as);

        editItem.setVisible((supportedOperations & MediaObject.SUPPORT_EDIT) > 0);
        deleteItem.setVisible((supportedOperations & MediaObject.SUPPORT_DELETE) > 0);
        shareItem.setVisible((supportedOperations & MediaObject.SUPPORT_SHARE) > 0);
        cropItem.setVisible((supportedOperations & MediaObject.SUPPORT_CROP) > 0);
        trimItem.setVisible((supportedOperations & MediaObject.SUPPORT_TRIM) > 0);
        muteItem.setVisible((supportedOperations & MediaObject.SUPPORT_MUTE) > 0);
        setAsItem.setVisible((supportedOperations & MediaObject.SUPPORT_SETAS) > 0);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mSelectionManager.setSelectedUriSource(this);
        mActionMode = mode;
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.gallery_multiselect, menu);
        MenuItem menuItem = menu.findItem(R.id.menu_share);
        mShareActionProvider = (ShareActionProvider) menuItem.getActionProvider();
        mShareActionProvider.setOnShareTargetSelectedListener(this);
        updateSelectedTitle(mode);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        // onDestroyActionMode gets called when the share target was selected,
        // but apparently before the ArrayList is serialized in the intent
        // so we can't clear the old one here.
        mSelectedShareableUrisArray = new ArrayList<Uri>();
        mSelectionManager.onClearSelection();
        mSelectionManager.setSelectedUriSource(null);
        mShareActionProvider = null;
        mActionMode = null;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        updateSelectedTitle(mode);
        return false;
    }

    @Override
    public boolean onShareTargetSelected(ShareActionProvider provider, Intent intent) {
        mActionMode.finish();
        return false;
    }

    private static class BulkDeleteTask extends AsyncTask<Void, Void, Void> {
        private Delegate mDelegate;
        private List<Object> mPaths;

        public BulkDeleteTask(Delegate delegate, List<Object> paths) {
            mDelegate = delegate;
            mPaths = paths;
        }

        @Override
        protected Void doInBackground(Void... ignored) {
            for (Object path : mPaths) {
                mDelegate.deleteItemWithPath(path);
            }
            return null;
        }
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        int actionItemId = item.getItemId();
        switch (actionItemId) {
            case R.id.menu_delete:
                BulkDeleteTask deleteTask = new BulkDeleteTask(mDelegate,
                        getPathsForSelectedItems());
                deleteTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                mode.finish();
                return true;
            case R.id.menu_edit:
            case R.id.menu_crop:
            case R.id.menu_trim:
            case R.id.menu_mute:
            case R.id.menu_set_as:
                singleItemAction(getSelectedItem(), actionItemId);
                mode.finish();
                return true;
            default:
                return false;
        }
    }

    private void singleItemAction(Object item, int actionItemId) {
        Intent intent = new Intent();
        String mime = getItemMimetype(item);
        Uri uri = mDelegate.getItemUri(item);
        switch (actionItemId) {
            case R.id.menu_edit:
                intent.setDataAndType(uri, mime)
                      .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                      .setAction(Intent.ACTION_EDIT);
                mContext.startActivity(Intent.createChooser(intent, null));
                return;
            case R.id.menu_crop:
                intent.setDataAndType(uri, mime)
                      .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                      .setAction(CropActivity.CROP_ACTION)
                      .setClass(mContext, FilterShowActivity.class);
                mContext.startActivity(intent);
                return;
            case R.id.menu_trim:
                intent.setData(uri)
                      .setClass(mContext, TrimVideo.class);
                mContext.startActivity(intent);
                return;
            case R.id.menu_mute:
                /* TODO need a way to get the file path of an item
                MuteVideo muteVideo = new MuteVideo(filePath,
                        uri, (Activity) mContext);
                muteVideo.muteInBackground();
                */
                return;
            case R.id.menu_set_as:
                intent.setDataAndType(uri, mime)
                      .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                      .setAction(Intent.ACTION_ATTACH_DATA)
                      .putExtra("mimeType", mime);
                mContext.startActivity(Intent.createChooser(
                        intent, mContext.getString(R.string.set_as)));
                return;
            default:
                return;
        }
    }

    private List<Object> getPathsForSelectedItems() {
        List<Object> paths = new ArrayList<Object>();
        SparseBooleanArray selected = mDelegate.getSelectedItemPositions();
        for (int i = 0; i < selected.size(); i++) {
            if (selected.valueAt(i)) {
                paths.add(mDelegate.getPathForItemAtPosition(i));
            }
        }
        return paths;
    }

    public Object getSelectedItem() {
        if (mDelegate.getSelectedItemCount() != 1) {
            return null;
        }
        SparseBooleanArray selected = mDelegate.getSelectedItemPositions();
        for (int i = 0; i < selected.size(); i++) {
            if (selected.valueAt(i)) {
                return mDelegate.getItemAtPosition(selected.keyAt(i));
            }
        }
        return null;
    }
}
