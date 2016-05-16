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

package com.android.providers.downloads.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.provider.DocumentsContract;
import android.provider.Downloads;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.android.providers.downloads.Constants;
import com.android.providers.downloads.OpenHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *  View showing a list of all downloads the Download Manager knows about.
 */
public class DownloadList extends Activity {
    static final String LOG_TAG = "DownloadList";

    private ExpandableListView mDateOrderedListView;
    private ListView mSizeOrderedListView;
    private View mEmptyView;

    private DownloadManager mDownloadManager;
    private Cursor mDateSortedCursor;
    private DateSortedDownloadAdapter mDateSortedAdapter;
    private Cursor mSizeSortedCursor;
    private DownloadAdapter mSizeSortedAdapter;
    private ActionMode mActionMode;
    private MyContentObserver mContentObserver = new MyContentObserver();
    private MyDataSetObserver mDataSetObserver = new MyDataSetObserver();

    private int mStatusColumnId;
    private int mIdColumnId;
    private int mLocalUriColumnId;
    private int mMediaTypeColumnId;
    private int mReasonColumndId;

    // TODO this shouldn't be necessary
    private final Map<Long, SelectionObjAttrs> mSelectedIds =
            new HashMap<Long, SelectionObjAttrs>();
    private static class SelectionObjAttrs {
        private String mFileName;
        private String mMimeType;
        SelectionObjAttrs(String fileName, String mimeType) {
            mFileName = fileName;
            mMimeType = mimeType;
        }
        String getFileName() {
            return mFileName;
        }
        String getMimeType() {
            return mMimeType;
        }
    }
    private ListView mCurrentView;
    private Cursor mCurrentCursor;
    private boolean mCurrentViewIsExpandableListView = false;
    private boolean mIsSortedBySize = false;

    /**
     * We keep track of when a dialog is being displayed for a pending download, because if that
     * download starts running, we want to immediately hide the dialog.
     */
    private Long mQueuedDownloadId = null;
    private AlertDialog mQueuedDialog;
    String mSelectedCountFormat;

    private Button mSortOption;

    private class MyContentObserver extends ContentObserver {
        public MyContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            handleDownloadsChanged();
        }
    }

    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            // ignore change notification if there are selections
            if (mSelectedIds.size() > 0) {
                return;
            }
            // may need to switch to or from the empty view
            chooseListToShow();
            ensureSomeGroupIsExpanded();
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Trampoline over to new management UI
        final Intent intent = new Intent(DocumentsContract.ACTION_MANAGE_ROOT);
        intent.setData(DocumentsContract.buildRootUri(
                Constants.STORAGE_AUTHORITY, Constants.STORAGE_ROOT_ID));
        startActivity(intent);
        finish();
    }

    public void onCreateLegacy(Bundle icicle) {
        super.onCreate(icicle);
        setFinishOnTouchOutside(true);
        setupViews();

        mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        mDownloadManager.setAccessAllDownloads(true);
        DownloadManager.Query baseQuery = new DownloadManager.Query()
                .setOnlyIncludeVisibleInDownloadsUi(true);
        //TODO don't do both queries - do them as needed
        mDateSortedCursor = mDownloadManager.query(baseQuery);
        mSizeSortedCursor = mDownloadManager.query(baseQuery
                                                  .orderBy(DownloadManager.COLUMN_TOTAL_SIZE_BYTES,
                                                          DownloadManager.Query.ORDER_DESCENDING));

        // only attach everything to the listbox if we can access the download database. Otherwise,
        // just show it empty
        if (haveCursors()) {
            startManagingCursor(mDateSortedCursor);
            startManagingCursor(mSizeSortedCursor);

            mStatusColumnId =
                    mDateSortedCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS);
            mIdColumnId =
                    mDateSortedCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID);
            mLocalUriColumnId =
                    mDateSortedCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI);
            mMediaTypeColumnId =
                    mDateSortedCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE);
            mReasonColumndId =
                    mDateSortedCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON);

            mDateSortedAdapter = new DateSortedDownloadAdapter(this, mDateSortedCursor);
            mDateOrderedListView.setAdapter(mDateSortedAdapter);
            mSizeSortedAdapter = new DownloadAdapter(this, mSizeSortedCursor);
            mSizeOrderedListView.setAdapter(mSizeSortedAdapter);

            ensureSomeGroupIsExpanded();
        }

        // did the caller want  to display the data sorted by size?
        Bundle extras = getIntent().getExtras();
        if (extras != null &&
                extras.getBoolean(DownloadManager.INTENT_EXTRAS_SORT_BY_SIZE, false)) {
            mIsSortedBySize = true;
        }
        mSortOption = (Button) findViewById(R.id.sort_button);
        mSortOption.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // flip the view
                mIsSortedBySize = !mIsSortedBySize;
                // clear all selections
                mSelectedIds.clear();
                chooseListToShow();
            }
        });

        chooseListToShow();
        mSelectedCountFormat = getString(R.string.selected_count);
    }

    /**
     * If no group is expanded in the date-sorted list, expand the first one.
     */
    private void ensureSomeGroupIsExpanded() {
        mDateOrderedListView.post(new Runnable() {
            public void run() {
                if (mDateSortedAdapter.getGroupCount() == 0) {
                    return;
                }
                for (int group = 0; group < mDateSortedAdapter.getGroupCount(); group++) {
                    if (mDateOrderedListView.isGroupExpanded(group)) {
                        return;
                    }
                }
                mDateOrderedListView.expandGroup(0);
            }
        });
    }

    private void setupViews() {
        setContentView(R.layout.download_list);
        ModeCallback modeCallback = new ModeCallback(this);

        //TODO don't create both views. create only the one needed.
        mDateOrderedListView = (ExpandableListView) findViewById(R.id.date_ordered_list);
        mDateOrderedListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mDateOrderedListView.setMultiChoiceModeListener(modeCallback);
        mDateOrderedListView.setOnChildClickListener(new OnChildClickListener() {
            // called when a child is clicked on (this is NOT the checkbox click)
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                    int groupPosition, int childPosition, long id) {
                if (!(v instanceof DownloadItem)) {
                    // can this even happen?
                    return false;
                }
                if (mSelectedIds.size() > 0) {
                    ((DownloadItem)v).setChecked(true);
                } else {
                    mDateSortedAdapter.moveCursorToChildPosition(groupPosition, childPosition);
                    handleItemClick(mDateSortedCursor);
                }
                return true;
            }
        });
        mSizeOrderedListView = (ListView) findViewById(R.id.size_ordered_list);
        mSizeOrderedListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mSizeOrderedListView.setMultiChoiceModeListener(modeCallback);
        mSizeOrderedListView.setOnItemClickListener(new OnItemClickListener() {
            // handle a click from the size-sorted list. (this is NOT the checkbox click)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSizeSortedCursor.moveToPosition(position);
                handleItemClick(mSizeSortedCursor);
            }
        });
        mEmptyView = findViewById(R.id.empty);
    }

    private static class ModeCallback implements MultiChoiceModeListener {
        private final DownloadList mDownloadList;

        public ModeCallback(DownloadList downloadList) {
            mDownloadList = downloadList;
        }

        @Override public void onDestroyActionMode(ActionMode mode) {
            mDownloadList.mSelectedIds.clear();
            mDownloadList.mActionMode = null;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (mDownloadList.haveCursors()) {
                final MenuInflater inflater = mDownloadList.getMenuInflater();
                inflater.inflate(R.menu.download_menu, menu);
            }
            mDownloadList.mActionMode = mode;
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (mDownloadList.mSelectedIds.size() == 0) {
                // nothing selected.
                return true;
            }
            switch (item.getItemId()) {
                case R.id.delete_download:
                    for (Long downloadId : mDownloadList.mSelectedIds.keySet()) {
                        mDownloadList.deleteDownload(downloadId);
                    }
                    // uncheck all checked items
                    ListView lv = mDownloadList.getCurrentView();
                    SparseBooleanArray checkedPositionList = lv.getCheckedItemPositions();
                    int checkedPositionListSize = checkedPositionList.size();
                    ArrayList<DownloadItem> sharedFiles = null;
                    for (int i = 0; i < checkedPositionListSize; i++) {
                        int position = checkedPositionList.keyAt(i);
                        if (checkedPositionList.get(position, false)) {
                            lv.setItemChecked(position, false);
                            onItemCheckedStateChanged(mode, position, 0, false);
                        }
                    }
                    mDownloadList.mSelectedIds.clear();
                    // update the subtitle
                    onItemCheckedStateChanged(mode, 1, 0, false);
                    break;
                case R.id.share_download:
                    mDownloadList.shareDownloadedFiles();
                    break;
            }
            return true;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                boolean checked) {
            // ignore long clicks on groups
            if (mDownloadList.isCurrentViewExpandableListView()) {
                ExpandableListView ev = mDownloadList.getExpandableListView();
                long pos = ev.getExpandableListPosition(position);
                if (checked && (ExpandableListView.getPackedPositionType(pos) ==
                        ExpandableListView.PACKED_POSITION_TYPE_GROUP)) {
                    // ignore this click
                    ev.setItemChecked(position, false);
                    return;
                }
            }
            mDownloadList.setActionModeTitle(mode);
        }
    }

    void setActionModeTitle(ActionMode mode) {
        int numSelected = mSelectedIds.size();
        if (numSelected > 0) {
            mode.setTitle(String.format(mSelectedCountFormat, numSelected,
                    mCurrentCursor.getCount()));
        } else {
            mode.setTitle("");
        }
    }

    private boolean haveCursors() {
        return mDateSortedCursor != null && mSizeSortedCursor != null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (haveCursors()) {
            mDateSortedCursor.registerContentObserver(mContentObserver);
            mDateSortedCursor.registerDataSetObserver(mDataSetObserver);
            refresh();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (haveCursors()) {
            mDateSortedCursor.unregisterContentObserver(mContentObserver);
            mDateSortedCursor.unregisterDataSetObserver(mDataSetObserver);
        }
    }

    private static final String BUNDLE_SAVED_DOWNLOAD_IDS = "download_ids";
    private static final String BUNDLE_SAVED_FILENAMES = "filenames";
    private static final String BUNDLE_SAVED_MIMETYPES = "mimetypes";
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isSortedBySize", mIsSortedBySize);
        int len = mSelectedIds.size();
        if (len == 0) {
            return;
        }
        long[] selectedIds = new long[len];
        String[] fileNames = new String[len];
        String[] mimeTypes = new String[len];
        int i = 0;
        for (long id : mSelectedIds.keySet()) {
            selectedIds[i] = id;
            SelectionObjAttrs obj = mSelectedIds.get(id);
            fileNames[i] = obj.getFileName();
            mimeTypes[i] = obj.getMimeType();
            i++;
        }
        outState.putLongArray(BUNDLE_SAVED_DOWNLOAD_IDS, selectedIds);
        outState.putStringArray(BUNDLE_SAVED_FILENAMES, fileNames);
        outState.putStringArray(BUNDLE_SAVED_MIMETYPES, mimeTypes);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mIsSortedBySize = savedInstanceState.getBoolean("isSortedBySize");
        mSelectedIds.clear();
        long[] selectedIds = savedInstanceState.getLongArray(BUNDLE_SAVED_DOWNLOAD_IDS);
        String[] fileNames = savedInstanceState.getStringArray(BUNDLE_SAVED_FILENAMES);
        String[] mimeTypes = savedInstanceState.getStringArray(BUNDLE_SAVED_MIMETYPES);
        if (selectedIds != null && selectedIds.length > 0) {
            for (int i = 0; i < selectedIds.length; i++) {
                mSelectedIds.put(selectedIds[i], new SelectionObjAttrs(fileNames[i], mimeTypes[i]));
            }
        }
        chooseListToShow();
    }

    /**
     * Show the correct ListView and hide the other, or hide both and show the empty view.
     */
    private void chooseListToShow() {
        mDateOrderedListView.setVisibility(View.GONE);
        mSizeOrderedListView.setVisibility(View.GONE);

        if (mDateSortedCursor == null || mDateSortedCursor.getCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
            mSortOption.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mSortOption.setVisibility(View.VISIBLE);
            ListView lv = activeListView();
            lv.setVisibility(View.VISIBLE);
            lv.invalidateViews(); // ensure checkboxes get updated
        }
        // restore the ActionMode title if there are selections
        if (mActionMode != null) {
            setActionModeTitle(mActionMode);
        }
    }

    ListView getCurrentView() {
        return mCurrentView;
    }

    ExpandableListView getExpandableListView() {
        return mDateOrderedListView;
    }

    boolean isCurrentViewExpandableListView() {
        return mCurrentViewIsExpandableListView;
    }

    private ListView activeListView() {
        if (mIsSortedBySize) {
            mCurrentCursor = mSizeSortedCursor;
            mCurrentView = mSizeOrderedListView;
            setTitle(R.string.download_title_sorted_by_size);
            mSortOption.setText(R.string.button_sort_by_date);
            mCurrentViewIsExpandableListView = false;
        } else {
            mCurrentCursor = mDateSortedCursor;
            mCurrentView = mDateOrderedListView;
            setTitle(R.string.download_title_sorted_by_date);
            mSortOption.setText(R.string.button_sort_by_size);
            mCurrentViewIsExpandableListView = true;
        }
        if (mActionMode != null) {
            mActionMode.finish();
        }
        return mCurrentView;
    }

    /**
     * @return an OnClickListener to delete the given downloadId from the Download Manager
     */
    private DialogInterface.OnClickListener getDeleteClickHandler(final long downloadId) {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteDownload(downloadId);
            }
        };
    }

    /**
     * @return an OnClickListener to restart the given downloadId in the Download Manager
     */
    private DialogInterface.OnClickListener getRestartClickHandler(final long downloadId) {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDownloadManager.restartDownload(downloadId);
            }
        };
    }

    /**
     * Send an Intent to open the download currently pointed to by the given cursor.
     */
    private void openCurrentDownload(Cursor cursor) {
        final Uri localUri = Uri.parse(cursor.getString(mLocalUriColumnId));
        try {
            getContentResolver().openFileDescriptor(localUri, "r").close();
        } catch (FileNotFoundException exc) {
            Log.d(LOG_TAG, "Failed to open download " + cursor.getLong(mIdColumnId), exc);
            showFailedDialog(cursor.getLong(mIdColumnId),
                    getString(R.string.dialog_file_missing_body));
            return;
        } catch (IOException exc) {
            // close() failed, not a problem
        }

        final long id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
        if (!OpenHelper.startViewIntent(this, id, 0)) {
            Toast.makeText(this, R.string.download_no_application_title, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleItemClick(Cursor cursor) {
        long id = cursor.getInt(mIdColumnId);
        switch (cursor.getInt(mStatusColumnId)) {
            case DownloadManager.STATUS_PENDING:
            case DownloadManager.STATUS_RUNNING:
                sendRunningDownloadClickedBroadcast(id);
                break;

            case DownloadManager.STATUS_PAUSED:
                if (isPausedForWifi(cursor)) {
                    mQueuedDownloadId = id;
                    mQueuedDialog = new AlertDialog.Builder(this)
                            .setTitle(R.string.dialog_title_queued_body)
                            .setMessage(R.string.dialog_queued_body)
                            .setPositiveButton(R.string.keep_queued_download, null)
                            .setNegativeButton(R.string.remove_download, getDeleteClickHandler(id))
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                /**
                                 * Called when a dialog for a pending download is canceled.
                                 */
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    mQueuedDownloadId = null;
                                    mQueuedDialog = null;
                                }
                            })
                            .show();
                } else {
                    sendRunningDownloadClickedBroadcast(id);
                }
                break;

            case DownloadManager.STATUS_SUCCESSFUL:
                openCurrentDownload(cursor);
                break;

            case DownloadManager.STATUS_FAILED:
                showFailedDialog(id, getErrorMessage(cursor));
                break;
        }
    }

    /**
     * @return the appropriate error message for the failed download pointed to by cursor
     */
    private String getErrorMessage(Cursor cursor) {
        switch (cursor.getInt(mReasonColumndId)) {
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                if (isOnExternalStorage(cursor)) {
                    return getString(R.string.dialog_file_already_exists);
                } else {
                    // the download manager should always find a free filename for cache downloads,
                    // so this indicates a strange internal error
                    return getUnknownErrorMessage();
                }

            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                if (isOnExternalStorage(cursor)) {
                    return getString(R.string.dialog_insufficient_space_on_external);
                } else {
                    return getString(R.string.dialog_insufficient_space_on_cache);
                }

            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                return getString(R.string.dialog_media_not_found);

            case DownloadManager.ERROR_CANNOT_RESUME:
                return getString(R.string.dialog_cannot_resume);

            default:
                return getUnknownErrorMessage();
        }
    }

    private boolean isOnExternalStorage(Cursor cursor) {
        String localUriString = cursor.getString(mLocalUriColumnId);
        if (localUriString == null) {
            return false;
        }
        Uri localUri = Uri.parse(localUriString);
        if (!localUri.getScheme().equals("file")) {
            return false;
        }
        String path = localUri.getPath();
        String externalRoot = Environment.getExternalStorageDirectory().getPath();
        return path.startsWith(externalRoot);
    }

    private String getUnknownErrorMessage() {
        return getString(R.string.dialog_failed_body);
    }

    private void showFailedDialog(long downloadId, String dialogBody) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_not_available)
                .setMessage(dialogBody)
                .setNegativeButton(R.string.delete_download, getDeleteClickHandler(downloadId))
                .setPositiveButton(R.string.retry_download, getRestartClickHandler(downloadId))
                .show();
    }

    private void sendRunningDownloadClickedBroadcast(long id) {
        final Intent intent = new Intent(Constants.ACTION_LIST);
        intent.setPackage(Constants.PROVIDER_PACKAGE_NAME);
        intent.putExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS,
                new long[] { id });
        sendBroadcast(intent);
    }

    // handle a click on one of the download item checkboxes
    public void onDownloadSelectionChanged(long downloadId, boolean isSelected,
            String fileName, String mimeType) {
        if (isSelected) {
            mSelectedIds.put(downloadId, new SelectionObjAttrs(fileName, mimeType));
        } else {
            mSelectedIds.remove(downloadId);
        }
    }

    /**
     * Requery the database and update the UI.
     */
    private void refresh() {
        mDateSortedCursor.requery();
        mSizeSortedCursor.requery();
        // Adapters get notification of changes and update automatically
    }

    /**
     * Delete a download from the Download Manager.
     */
    private void deleteDownload(long downloadId) {
        // let DownloadService do the job of cleaning up the downloads db, mediaprovider db,
        // and removal of file from sdcard
        // TODO do the following in asynctask - not on main thread.
        mDownloadManager.markRowDeleted(downloadId);
    }

    public boolean isDownloadSelected(long id) {
        return mSelectedIds.containsKey(id);
    }

    /**
     * Called when there's a change to the downloads database.
     */
    void handleDownloadsChanged() {
        checkSelectionForDeletedEntries();

        if (mQueuedDownloadId != null && moveToDownload(mQueuedDownloadId)) {
            if (mDateSortedCursor.getInt(mStatusColumnId) != DownloadManager.STATUS_PAUSED
                    || !isPausedForWifi(mDateSortedCursor)) {
                mQueuedDialog.cancel();
            }
        }
    }

    private boolean isPausedForWifi(Cursor cursor) {
        return cursor.getInt(mReasonColumndId) == DownloadManager.PAUSED_QUEUED_FOR_WIFI;
    }

    /**
     * Check if any of the selected downloads have been deleted from the downloads database, and
     * remove such downloads from the selection.
     */
    private void checkSelectionForDeletedEntries() {
        // gather all existing IDs...
        Set<Long> allIds = new HashSet<Long>();
        for (mDateSortedCursor.moveToFirst(); !mDateSortedCursor.isAfterLast();
                mDateSortedCursor.moveToNext()) {
            allIds.add(mDateSortedCursor.getLong(mIdColumnId));
        }

        // ...and check if any selected IDs are now missing
        for (Iterator<Long> iterator = mSelectedIds.keySet().iterator(); iterator.hasNext(); ) {
            if (!allIds.contains(iterator.next())) {
                iterator.remove();
            }
        }
    }

    /**
     * Move {@link #mDateSortedCursor} to the download with the given ID.
     * @return true if the specified download ID was found; false otherwise
     */
    private boolean moveToDownload(long downloadId) {
        for (mDateSortedCursor.moveToFirst(); !mDateSortedCursor.isAfterLast();
                mDateSortedCursor.moveToNext()) {
            if (mDateSortedCursor.getLong(mIdColumnId) == downloadId) {
                return true;
            }
        }
        return false;
    }

    /**
     * handle share menu button click when one more files are selected for sharing
     */
    public boolean shareDownloadedFiles() {
        Intent intent = new Intent();
        if (mSelectedIds.size() > 1) {
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            ArrayList<Parcelable> attachments = new ArrayList<Parcelable>();
            ArrayList<String> mimeTypes = new ArrayList<String>();
            for (Map.Entry<Long, SelectionObjAttrs> item : mSelectedIds.entrySet()) {
                final Uri uri = ContentUris.withAppendedId(
                        Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, item.getKey());
                final String mimeType = item.getValue().getMimeType();
                attachments.add(uri);
                if (mimeType != null) {
                    mimeTypes.add(mimeType);
                }
            }
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments);
            intent.setType(findCommonMimeType(mimeTypes));
        } else {
            // get the entry
            // since there is ONLY one entry in this, we can do the following
            for (Map.Entry<Long, SelectionObjAttrs> item : mSelectedIds.entrySet()) {
                final Uri uri = ContentUris.withAppendedId(
                        Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, item.getKey());
                final String mimeType = item.getValue().getMimeType();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.setType(mimeType);
            }
        }
        intent = Intent.createChooser(intent, getText(R.string.download_share_dialog));
        startActivity(intent);
        return true;
    }

    private String findCommonMimeType(ArrayList<String> mimeTypes) {
        // are all mimeypes the same?
        String str = findCommonString(mimeTypes);
        if (str != null) {
            return str;
        }

        // are all prefixes of the given mimetypes the same?
        ArrayList<String> mimeTypePrefixes = new ArrayList<String>();
        for (String s : mimeTypes) {
            if (s != null) {
                mimeTypePrefixes.add(s.substring(0, s.indexOf('/')));
            }
        }
        str = findCommonString(mimeTypePrefixes);
        if (str != null) {
            return str + "/*";
        }

        // return generic mimetype
        return "*/*";
    }
    private String findCommonString(Collection<String> set) {
        String str = null;
        boolean found = true;
        for (String s : set) {
            if (str == null) {
                str = s;
            } else if (!str.equals(s)) {
                found = false;
                break;
            }
        }
        return (found) ? str : null;
    }
}
