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

package com.android.gallery3d.app;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.CacheStorageUsageInfo;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.ManageCacheDrawer;
import com.android.gallery3d.ui.MenuExecutor;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;

import java.util.ArrayList;

public class ManageCachePage extends ActivityState implements
        SelectionManager.SelectionListener, MenuExecutor.ProgressListener,
        EyePosition.EyePositionListener, OnClickListener {
    public static final String KEY_MEDIA_PATH = "media-path";

    @SuppressWarnings("unused")
    private static final String TAG = "ManageCachePage";

    private static final int DATA_CACHE_SIZE = 256;
    private static final int MSG_REFRESH_STORAGE_INFO = 1;
    private static final int MSG_REQUEST_LAYOUT = 2;
    private static final int PROGRESS_BAR_MAX = 10000;

    private SlotView mSlotView;
    private MediaSet mMediaSet;

    protected SelectionManager mSelectionManager;
    protected ManageCacheDrawer mSelectionDrawer;
    private AlbumSetDataLoader mAlbumSetDataAdapter;

    private EyePosition mEyePosition;

    // The eyes' position of the user, the origin is at the center of the
    // device and the unit is in pixels.
    private float mX;
    private float mY;
    private float mZ;

    private int mAlbumCountToMakeAvailableOffline;
    private View mFooterContent;
    private CacheStorageUsageInfo mCacheStorageInfo;
    private Future<Void> mUpdateStorageInfo;
    private Handler mHandler;
    private boolean mLayoutReady = false;

    @Override
    protected int getBackgroundColorId() {
        return R.color.cache_background;
    }

    private GLView mRootPane = new GLView() {
        private float mMatrix[] = new float[16];

        @Override
        protected void renderBackground(GLCanvas view) {
            view.clearBuffer(getBackgroundColor());
        }

        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {
            // Hack: our layout depends on other components on the screen.
            // We assume the other components will complete before we get a change
            // to run a message in main thread.
            if (!mLayoutReady) {
                mHandler.sendEmptyMessage(MSG_REQUEST_LAYOUT);
                return;
            }
            mLayoutReady = false;

            mEyePosition.resetPosition();
            int slotViewTop = mActivity.getGalleryActionBar().getHeight();
            int slotViewBottom = bottom - top;

            View footer = mActivity.findViewById(R.id.footer);
            if (footer != null) {
                int location[] = {0, 0};
                footer.getLocationOnScreen(location);
                slotViewBottom = location[1];
            }

            mSlotView.layout(0, slotViewTop, right - left, slotViewBottom);
        }

        @Override
        protected void render(GLCanvas canvas) {
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            GalleryUtils.setViewPointMatrix(mMatrix,
                        getWidth() / 2 + mX, getHeight() / 2 + mY, mZ);
            canvas.multiplyMatrix(mMatrix, 0);
            super.render(canvas);
            canvas.restore();
        }
    };

    @Override
    public void onEyePositionChanged(float x, float y, float z) {
        mRootPane.lockRendering();
        mX = x;
        mY = y;
        mZ = z;
        mRootPane.unlockRendering();
        mRootPane.invalidate();
    }

    private void onDown(int index) {
        mSelectionDrawer.setPressedIndex(index);
    }

    private void onUp() {
        mSelectionDrawer.setPressedIndex(-1);
    }

    public void onSingleTapUp(int slotIndex) {
        MediaSet targetSet = mAlbumSetDataAdapter.getMediaSet(slotIndex);
        if (targetSet == null) return; // Content is dirty, we shall reload soon

        // ignore selection action if the target set does not support cache
        // operation (like a local album).
        if ((targetSet.getSupportedOperations()
                & MediaSet.SUPPORT_CACHE) == 0) {
            showToastForLocalAlbum();
            return;
        }

        Path path = targetSet.getPath();
        boolean isFullyCached =
                (targetSet.getCacheFlag() == MediaObject.CACHE_FLAG_FULL);
        boolean isSelected = mSelectionManager.isItemSelected(path);

        if (!isFullyCached) {
            // We only count the media sets that will be made available offline
            // in this session.
            if (isSelected) {
                --mAlbumCountToMakeAvailableOffline;
            } else {
                ++mAlbumCountToMakeAvailableOffline;
            }
        }

        long sizeOfTarget = targetSet.getCacheSize();
        mCacheStorageInfo.increaseTargetCacheSize(
                (isFullyCached ^ isSelected) ? -sizeOfTarget : sizeOfTarget);
        refreshCacheStorageInfo();

        mSelectionManager.toggle(path);
        mSlotView.invalidate();
    }

    @Override
    public void onCreate(Bundle data, Bundle restoreState) {
        super.onCreate(data, restoreState);
        mCacheStorageInfo = new CacheStorageUsageInfo(mActivity);
        initializeViews();
        initializeData(data);
        mEyePosition = new EyePosition(mActivity.getAndroidContext(), this);
        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_REFRESH_STORAGE_INFO:
                        refreshCacheStorageInfo();
                        break;
                    case MSG_REQUEST_LAYOUT: {
                        mLayoutReady = true;
                        removeMessages(MSG_REQUEST_LAYOUT);
                        mRootPane.requestLayout();
                        break;
                    }
                }
            }
        };
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        // We use different layout resources for different configs
        initializeFooterViews();
        FrameLayout layout = (FrameLayout) ((Activity) mActivity).findViewById(R.id.footer);
        if (layout.getVisibility() == View.VISIBLE) {
            layout.removeAllViews();
            layout.addView(mFooterContent);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mAlbumSetDataAdapter.pause();
        mSelectionDrawer.pause();
        mEyePosition.pause();

        if (mUpdateStorageInfo != null) {
            mUpdateStorageInfo.cancel();
            mUpdateStorageInfo = null;
        }
        mHandler.removeMessages(MSG_REFRESH_STORAGE_INFO);

        FrameLayout layout = (FrameLayout) ((Activity) mActivity).findViewById(R.id.footer);
        layout.removeAllViews();
        layout.setVisibility(View.INVISIBLE);
    }

    private Job<Void> mUpdateStorageInfoJob = new Job<Void>() {
        @Override
        public Void run(JobContext jc) {
            mCacheStorageInfo.loadStorageInfo(jc);
            if (!jc.isCancelled()) {
                mHandler.sendEmptyMessage(MSG_REFRESH_STORAGE_INFO);
            }
            return null;
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        setContentPane(mRootPane);
        mAlbumSetDataAdapter.resume();
        mSelectionDrawer.resume();
        mEyePosition.resume();
        mUpdateStorageInfo = mActivity.getThreadPool().submit(mUpdateStorageInfoJob);
        FrameLayout layout = (FrameLayout) ((Activity) mActivity).findViewById(R.id.footer);
        layout.addView(mFooterContent);
        layout.setVisibility(View.VISIBLE);
    }

    private void initializeData(Bundle data) {
        String mediaPath = data.getString(ManageCachePage.KEY_MEDIA_PATH);
        mMediaSet = mActivity.getDataManager().getMediaSet(mediaPath);
        mSelectionManager.setSourceMediaSet(mMediaSet);

        // We will always be in selection mode in this page.
        mSelectionManager.setAutoLeaveSelectionMode(false);
        mSelectionManager.enterSelectionMode();

        mAlbumSetDataAdapter = new AlbumSetDataLoader(
                mActivity, mMediaSet, DATA_CACHE_SIZE);
        mSelectionDrawer.setModel(mAlbumSetDataAdapter);
    }

    private void initializeViews() {
        Activity activity = mActivity;

        mSelectionManager = new SelectionManager(mActivity, true);
        mSelectionManager.setSelectionListener(this);

        Config.ManageCachePage config = Config.ManageCachePage.get(activity);
        mSlotView = new SlotView(mActivity, config.slotViewSpec);
        mSelectionDrawer = new ManageCacheDrawer(mActivity, mSelectionManager, mSlotView,
                config.labelSpec, config.cachePinSize, config.cachePinMargin);
        mSlotView.setSlotRenderer(mSelectionDrawer);
        mSlotView.setListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                ManageCachePage.this.onDown(index);
            }

            @Override
            public void onUp(boolean followedByLongPress) {
                ManageCachePage.this.onUp();
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                ManageCachePage.this.onSingleTapUp(slotIndex);
            }
        });
        mRootPane.addComponent(mSlotView);
        initializeFooterViews();
    }

    private void initializeFooterViews() {
        Activity activity = mActivity;

        LayoutInflater inflater = activity.getLayoutInflater();
        mFooterContent = inflater.inflate(R.layout.manage_offline_bar, null);

        mFooterContent.findViewById(R.id.done).setOnClickListener(this);
        refreshCacheStorageInfo();
    }

    @Override
    public void onClick(View view) {
        Utils.assertTrue(view.getId() == R.id.done);
        GLRoot root = mActivity.getGLRoot();
        root.lockRenderThread();
        try {
            ArrayList<Path> ids = mSelectionManager.getSelected(false);
            if (ids.size() == 0) {
                onBackPressed();
                return;
            }
            showToast();

            MenuExecutor menuExecutor = new MenuExecutor(mActivity, mSelectionManager);
            menuExecutor.startAction(R.id.action_toggle_full_caching,
                    R.string.process_caching_requests, this);
        } finally {
            root.unlockRenderThread();
        }
    }

    private void showToast() {
        if (mAlbumCountToMakeAvailableOffline > 0) {
            Activity activity = mActivity;
            Toast.makeText(activity, activity.getResources().getQuantityString(
                    R.plurals.make_albums_available_offline,
                    mAlbumCountToMakeAvailableOffline),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showToastForLocalAlbum() {
        Activity activity = mActivity;
        Toast.makeText(activity, activity.getResources().getString(
            R.string.try_to_set_local_album_available_offline),
            Toast.LENGTH_SHORT).show();
    }

    private void refreshCacheStorageInfo() {
        ProgressBar progressBar = (ProgressBar) mFooterContent.findViewById(R.id.progress);
        TextView status = (TextView) mFooterContent.findViewById(R.id.status);
        progressBar.setMax(PROGRESS_BAR_MAX);
        long totalBytes = mCacheStorageInfo.getTotalBytes();
        long usedBytes = mCacheStorageInfo.getUsedBytes();
        long expectedBytes = mCacheStorageInfo.getExpectedUsedBytes();
        long freeBytes = mCacheStorageInfo.getFreeBytes();

        Activity activity = mActivity;
        if (totalBytes == 0) {
            progressBar.setProgress(0);
            progressBar.setSecondaryProgress(0);

            // TODO: get the string translated
            String label = activity.getString(R.string.free_space_format, "-");
            status.setText(label);
        } else {
            progressBar.setProgress((int) (usedBytes * PROGRESS_BAR_MAX / totalBytes));
            progressBar.setSecondaryProgress(
                    (int) (expectedBytes * PROGRESS_BAR_MAX / totalBytes));
            String label = activity.getString(R.string.free_space_format,
                    Formatter.formatFileSize(activity, freeBytes));
            status.setText(label);
        }
    }

    @Override
    public void onProgressComplete(int result) {
        onBackPressed();
    }

    @Override
    public void onProgressUpdate(int index) {
    }

    @Override
    public void onSelectionModeChange(int mode) {
    }

    @Override
    public void onSelectionChange(Path path, boolean selected) {
    }

    @Override
    public void onConfirmDialogDismissed(boolean confirmed) {
    }

    @Override
    public void onConfirmDialogShown() {
    }

    @Override
    public void onProgressStart() {
    }
}
