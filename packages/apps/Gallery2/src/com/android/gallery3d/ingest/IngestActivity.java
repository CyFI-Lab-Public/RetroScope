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

package com.android.gallery3d.ingest;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.mtp.MtpObjectInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.android.gallery3d.R;
import com.android.gallery3d.ingest.adapter.CheckBroker;
import com.android.gallery3d.ingest.adapter.MtpAdapter;
import com.android.gallery3d.ingest.adapter.MtpPagerAdapter;
import com.android.gallery3d.ingest.data.MtpBitmapFetch;
import com.android.gallery3d.ingest.ui.DateTileView;
import com.android.gallery3d.ingest.ui.IngestGridView;
import com.android.gallery3d.ingest.ui.IngestGridView.OnClearChoicesListener;

import java.lang.ref.WeakReference;
import java.util.Collection;

public class IngestActivity extends Activity implements
        MtpDeviceIndex.ProgressListener, ImportTask.Listener {

    private IngestService mHelperService;
    private boolean mActive = false;
    private IngestGridView mGridView;
    private MtpAdapter mAdapter;
    private Handler mHandler;
    private ProgressDialog mProgressDialog;
    private ActionMode mActiveActionMode;

    private View mWarningView;
    private TextView mWarningText;
    private int mLastCheckedPosition = 0;

    private ViewPager mFullscreenPager;
    private MtpPagerAdapter mPagerAdapter;
    private boolean mFullscreenPagerVisible = false;

    private MenuItem mMenuSwitcherItem;
    private MenuItem mActionMenuSwitcherItem;

    // The MTP framework components don't give us fine-grained file copy
    // progress updates, so for large photos and videos, we will be stuck
    // with a dialog not updating for a long time. To give the user feedback,
    // we switch to the animated indeterminate progress bar after the timeout
    // specified by INDETERMINATE_SWITCH_TIMEOUT_MS. On the next update from
    // the framework, we switch back to the normal progress bar.
    private static final int INDETERMINATE_SWITCH_TIMEOUT_MS = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        doBindHelperService();

        setContentView(R.layout.ingest_activity_item_list);
        mGridView = (IngestGridView) findViewById(R.id.ingest_gridview);
        mAdapter = new MtpAdapter(this);
        mAdapter.registerDataSetObserver(mMasterObserver);
        mGridView.setAdapter(mAdapter);
        mGridView.setMultiChoiceModeListener(mMultiChoiceModeListener);
        mGridView.setOnItemClickListener(mOnItemClickListener);
        mGridView.setOnClearChoicesListener(mPositionMappingCheckBroker);

        mFullscreenPager = (ViewPager) findViewById(R.id.ingest_view_pager);

        mHandler = new ItemListHandler(this);

        MtpBitmapFetch.configureForContext(this);
    }

    private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View itemView, int position, long arg3) {
            mLastCheckedPosition = position;
            mGridView.setItemChecked(position, !mGridView.getCheckedItemPositions().get(position));
        }
    };

    private MultiChoiceModeListener mMultiChoiceModeListener = new MultiChoiceModeListener() {
        private boolean mIgnoreItemCheckedStateChanges = false;

        private void updateSelectedTitle(ActionMode mode) {
            int count = mGridView.getCheckedItemCount();
            mode.setTitle(getResources().getQuantityString(
                    R.plurals.number_of_items_selected, count, count));
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                boolean checked) {
            if (mIgnoreItemCheckedStateChanges) return;
            if (mAdapter.itemAtPositionIsBucket(position)) {
                SparseBooleanArray checkedItems = mGridView.getCheckedItemPositions();
                mIgnoreItemCheckedStateChanges = true;
                mGridView.setItemChecked(position, false);

                // Takes advantage of the fact that SectionIndexer imposes the
                // need to clamp to the valid range
                int nextSectionStart = mAdapter.getPositionForSection(
                        mAdapter.getSectionForPosition(position) + 1);
                if (nextSectionStart == position)
                    nextSectionStart = mAdapter.getCount();

                boolean rangeValue = false; // Value we want to set all of the bucket items to

                // Determine if all the items in the bucket are currently checked, so that we
                // can uncheck them, otherwise we will check all items in the bucket.
                for (int i = position + 1; i < nextSectionStart; i++) {
                    if (checkedItems.get(i) == false) {
                        rangeValue = true;
                        break;
                    }
                }

                // Set all items in the bucket to the desired state
                for (int i = position + 1; i < nextSectionStart; i++) {
                    if (checkedItems.get(i) != rangeValue)
                        mGridView.setItemChecked(i, rangeValue);
                }

                mPositionMappingCheckBroker.onBulkCheckedChange();
                mIgnoreItemCheckedStateChanges = false;
            } else {
                mPositionMappingCheckBroker.onCheckedChange(position, checked);
            }
            mLastCheckedPosition = position;
            updateSelectedTitle(mode);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return onOptionsItemSelected(item);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.ingest_menu_item_list_selection, menu);
            updateSelectedTitle(mode);
            mActiveActionMode = mode;
            mActionMenuSwitcherItem = menu.findItem(R.id.ingest_switch_view);
            setSwitcherMenuState(mActionMenuSwitcherItem, mFullscreenPagerVisible);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActiveActionMode = null;
            mActionMenuSwitcherItem = null;
            mHandler.sendEmptyMessage(ItemListHandler.MSG_BULK_CHECKED_CHANGE);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            updateSelectedTitle(mode);
            return false;
        }
    };

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.import_items:
                if (mActiveActionMode != null) {
                    mHelperService.importSelectedItems(
                            mGridView.getCheckedItemPositions(),
                            mAdapter);
                    mActiveActionMode.finish();
                }
                return true;
            case R.id.ingest_switch_view:
                setFullscreenPagerVisibility(!mFullscreenPagerVisible);
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.ingest_menu_item_list_selection, menu);
        mMenuSwitcherItem = menu.findItem(R.id.ingest_switch_view);
        menu.findItem(R.id.import_items).setVisible(false);
        setSwitcherMenuState(mMenuSwitcherItem, mFullscreenPagerVisible);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindHelperService();
    }

    @Override
    protected void onResume() {
        DateTileView.refreshLocale();
        mActive = true;
        if (mHelperService != null) mHelperService.setClientActivity(this);
        updateWarningView();
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mHelperService != null) mHelperService.setClientActivity(null);
        mActive = false;
        cleanupProgressDialog();
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        MtpBitmapFetch.configureForContext(this);
    }

    private void showWarningView(int textResId) {
        if (mWarningView == null) {
            mWarningView = findViewById(R.id.ingest_warning_view);
            mWarningText =
                    (TextView)mWarningView.findViewById(R.id.ingest_warning_view_text);
        }
        mWarningText.setText(textResId);
        mWarningView.setVisibility(View.VISIBLE);
        setFullscreenPagerVisibility(false);
        mGridView.setVisibility(View.GONE);
    }

    private void hideWarningView() {
        if (mWarningView != null) {
            mWarningView.setVisibility(View.GONE);
            setFullscreenPagerVisibility(false);
        }
    }

    private PositionMappingCheckBroker mPositionMappingCheckBroker = new PositionMappingCheckBroker();

    private class PositionMappingCheckBroker extends CheckBroker
        implements OnClearChoicesListener {
        private int mLastMappingPager = -1;
        private int mLastMappingGrid = -1;

        private int mapPagerToGridPosition(int position) {
            if (position != mLastMappingPager) {
               mLastMappingPager = position;
               mLastMappingGrid = mAdapter.translatePositionWithoutLabels(position);
            }
            return mLastMappingGrid;
        }

        private int mapGridToPagerPosition(int position) {
            if (position != mLastMappingGrid) {
                mLastMappingGrid = position;
                mLastMappingPager = mPagerAdapter.translatePositionWithLabels(position);
            }
            return mLastMappingPager;
        }

        @Override
        public void setItemChecked(int position, boolean checked) {
            mGridView.setItemChecked(mapPagerToGridPosition(position), checked);
        }

        @Override
        public void onCheckedChange(int position, boolean checked) {
            if (mPagerAdapter != null) {
                super.onCheckedChange(mapGridToPagerPosition(position), checked);
            }
        }

        @Override
        public boolean isItemChecked(int position) {
            return mGridView.getCheckedItemPositions().get(mapPagerToGridPosition(position));
        }

        @Override
        public void onClearChoices() {
            onBulkCheckedChange();
        }
    };

    private DataSetObserver mMasterObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            if (mPagerAdapter != null) mPagerAdapter.notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            if (mPagerAdapter != null) mPagerAdapter.notifyDataSetChanged();
        }
    };

    private int pickFullscreenStartingPosition() {
        int firstVisiblePosition = mGridView.getFirstVisiblePosition();
        if (mLastCheckedPosition <= firstVisiblePosition
                || mLastCheckedPosition > mGridView.getLastVisiblePosition()) {
            return firstVisiblePosition;
        } else {
            return mLastCheckedPosition;
        }
    }

    private void setSwitcherMenuState(MenuItem menuItem, boolean inFullscreenMode) {
        if (menuItem == null) return;
        if (!inFullscreenMode) {
            menuItem.setIcon(android.R.drawable.ic_menu_zoom);
            menuItem.setTitle(R.string.switch_photo_fullscreen);
        } else {
            menuItem.setIcon(android.R.drawable.ic_dialog_dialer);
            menuItem.setTitle(R.string.switch_photo_grid);
        }
    }

    private void setFullscreenPagerVisibility(boolean visible) {
        mFullscreenPagerVisible = visible;
        if (visible) {
            if (mPagerAdapter == null) {
                mPagerAdapter = new MtpPagerAdapter(this, mPositionMappingCheckBroker);
                mPagerAdapter.setMtpDeviceIndex(mAdapter.getMtpDeviceIndex());
            }
            mFullscreenPager.setAdapter(mPagerAdapter);
            mFullscreenPager.setCurrentItem(mPagerAdapter.translatePositionWithLabels(
                    pickFullscreenStartingPosition()), false);
        } else if (mPagerAdapter != null) {
            mGridView.setSelection(mAdapter.translatePositionWithoutLabels(
                    mFullscreenPager.getCurrentItem()));
            mFullscreenPager.setAdapter(null);
        }
        mGridView.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
        mFullscreenPager.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        if (mActionMenuSwitcherItem != null) {
            setSwitcherMenuState(mActionMenuSwitcherItem, visible);
        }
        setSwitcherMenuState(mMenuSwitcherItem, visible);
    }

    private void updateWarningView() {
        if (!mAdapter.deviceConnected()) {
            showWarningView(R.string.ingest_no_device);
        } else if (mAdapter.indexReady() && mAdapter.getCount() == 0) {
            showWarningView(R.string.ingest_empty_device);
        } else {
            hideWarningView();
        }
    }

    private void UiThreadNotifyIndexChanged() {
        mAdapter.notifyDataSetChanged();
        if (mActiveActionMode != null) {
            mActiveActionMode.finish();
            mActiveActionMode = null;
        }
        updateWarningView();
    }

    protected void notifyIndexChanged() {
        mHandler.sendEmptyMessage(ItemListHandler.MSG_NOTIFY_CHANGED);
    }

    private static class ProgressState {
        String message;
        String title;
        int current;
        int max;

        public void reset() {
            title = null;
            message = null;
            current = 0;
            max = 0;
        }
    }

    private ProgressState mProgressState = new ProgressState();

    @Override
    public void onObjectIndexed(MtpObjectInfo object, int numVisited) {
        // Not guaranteed to be called on the UI thread
        mProgressState.reset();
        mProgressState.max = 0;
        mProgressState.message = getResources().getQuantityString(
                R.plurals.ingest_number_of_items_scanned, numVisited, numVisited);
        mHandler.sendEmptyMessage(ItemListHandler.MSG_PROGRESS_UPDATE);
    }

    @Override
    public void onSorting() {
        // Not guaranteed to be called on the UI thread
        mProgressState.reset();
        mProgressState.max = 0;
        mProgressState.message = getResources().getString(R.string.ingest_sorting);
        mHandler.sendEmptyMessage(ItemListHandler.MSG_PROGRESS_UPDATE);
    }

    @Override
    public void onIndexFinish() {
        // Not guaranteed to be called on the UI thread
        mHandler.sendEmptyMessage(ItemListHandler.MSG_PROGRESS_HIDE);
        mHandler.sendEmptyMessage(ItemListHandler.MSG_NOTIFY_CHANGED);
    }

    @Override
    public void onImportProgress(final int visitedCount, final int totalCount,
            String pathIfSuccessful) {
        // Not guaranteed to be called on the UI thread
        mProgressState.reset();
        mProgressState.max = totalCount;
        mProgressState.current = visitedCount;
        mProgressState.title = getResources().getString(R.string.ingest_importing);
        mHandler.sendEmptyMessage(ItemListHandler.MSG_PROGRESS_UPDATE);
        mHandler.removeMessages(ItemListHandler.MSG_PROGRESS_INDETERMINATE);
        mHandler.sendEmptyMessageDelayed(ItemListHandler.MSG_PROGRESS_INDETERMINATE,
                INDETERMINATE_SWITCH_TIMEOUT_MS);
    }

    @Override
    public void onImportFinish(Collection<MtpObjectInfo> objectsNotImported,
            int numVisited) {
        // Not guaranteed to be called on the UI thread
        mHandler.sendEmptyMessage(ItemListHandler.MSG_PROGRESS_HIDE);
        mHandler.removeMessages(ItemListHandler.MSG_PROGRESS_INDETERMINATE);
        // TODO: maybe show an extra dialog listing the ones that failed
        // importing, if any?
    }

    private ProgressDialog getProgressDialog() {
        if (mProgressDialog == null || !mProgressDialog.isShowing()) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCancelable(false);
        }
        return mProgressDialog;
    }

    private void updateProgressDialog() {
        ProgressDialog dialog = getProgressDialog();
        boolean indeterminate = (mProgressState.max == 0);
        dialog.setIndeterminate(indeterminate);
        dialog.setProgressStyle(indeterminate ? ProgressDialog.STYLE_SPINNER
                : ProgressDialog.STYLE_HORIZONTAL);
        if (mProgressState.title != null) {
            dialog.setTitle(mProgressState.title);
        }
        if (mProgressState.message != null) {
            dialog.setMessage(mProgressState.message);
        }
        if (!indeterminate) {
            dialog.setProgress(mProgressState.current);
            dialog.setMax(mProgressState.max);
        }
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }

    private void makeProgressDialogIndeterminate() {
        ProgressDialog dialog = getProgressDialog();
        dialog.setIndeterminate(true);
    }

    private void cleanupProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.hide();
            mProgressDialog = null;
        }
    }

    // This is static and uses a WeakReference in order to avoid leaking the Activity
    private static class ItemListHandler extends Handler {
        public static final int MSG_PROGRESS_UPDATE = 0;
        public static final int MSG_PROGRESS_HIDE = 1;
        public static final int MSG_NOTIFY_CHANGED = 2;
        public static final int MSG_BULK_CHECKED_CHANGE = 3;
        public static final int MSG_PROGRESS_INDETERMINATE = 4;

        WeakReference<IngestActivity> mParentReference;

        public ItemListHandler(IngestActivity parent) {
            super();
            mParentReference = new WeakReference<IngestActivity>(parent);
        }

        public void handleMessage(Message message) {
            IngestActivity parent = mParentReference.get();
            if (parent == null || !parent.mActive)
                return;
            switch (message.what) {
                case MSG_PROGRESS_HIDE:
                    parent.cleanupProgressDialog();
                    break;
                case MSG_PROGRESS_UPDATE:
                    parent.updateProgressDialog();
                    break;
                case MSG_NOTIFY_CHANGED:
                    parent.UiThreadNotifyIndexChanged();
                    break;
                case MSG_BULK_CHECKED_CHANGE:
                    parent.mPositionMappingCheckBroker.onBulkCheckedChange();
                    break;
                case MSG_PROGRESS_INDETERMINATE:
                    parent.makeProgressDialogIndeterminate();
                    break;
                default:
                    break;
            }
        }
    }

    private ServiceConnection mHelperServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mHelperService = ((IngestService.LocalBinder) service).getService();
            mHelperService.setClientActivity(IngestActivity.this);
            MtpDeviceIndex index = mHelperService.getIndex();
            mAdapter.setMtpDeviceIndex(index);
            if (mPagerAdapter != null) mPagerAdapter.setMtpDeviceIndex(index);
        }

        public void onServiceDisconnected(ComponentName className) {
            mHelperService = null;
        }
    };

    private void doBindHelperService() {
        bindService(new Intent(getApplicationContext(), IngestService.class),
                mHelperServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void doUnbindHelperService() {
        if (mHelperService != null) {
            mHelperService.setClientActivity(null);
            unbindService(mHelperServiceConnection);
        }
    }
}
