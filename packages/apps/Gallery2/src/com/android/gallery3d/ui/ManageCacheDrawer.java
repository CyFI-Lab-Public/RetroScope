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

package com.android.gallery3d.ui;

import android.content.Context;

import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.DataSourceType;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.ResourceTexture;
import com.android.gallery3d.glrenderer.StringTexture;
import com.android.gallery3d.ui.AlbumSetSlidingWindow.AlbumSetEntry;

public class ManageCacheDrawer extends AlbumSetSlotRenderer {
    private final ResourceTexture mCheckedItem;
    private final ResourceTexture mUnCheckedItem;
    private final SelectionManager mSelectionManager;

    private final ResourceTexture mLocalAlbumIcon;
    private final StringTexture mCachingText;

    private final int mCachePinSize;
    private final int mCachePinMargin;

    public ManageCacheDrawer(AbstractGalleryActivity activity, SelectionManager selectionManager,
            SlotView slotView, LabelSpec labelSpec, int cachePinSize, int cachePinMargin) {
        super(activity, selectionManager, slotView, labelSpec,
                activity.getResources().getColor(R.color.cache_placeholder));
        Context context = activity;
        mCheckedItem = new ResourceTexture(
                context, R.drawable.btn_make_offline_normal_on_holo_dark);
        mUnCheckedItem = new ResourceTexture(
                context, R.drawable.btn_make_offline_normal_off_holo_dark);
        mLocalAlbumIcon = new ResourceTexture(
                context, R.drawable.btn_make_offline_disabled_on_holo_dark);
        String cachingLabel = context.getString(R.string.caching_label);
        mCachingText = StringTexture.newInstance(cachingLabel, 12, 0xffffffff);
        mSelectionManager = selectionManager;
        mCachePinSize = cachePinSize;
        mCachePinMargin = cachePinMargin;
    }

    private static boolean isLocal(int dataSourceType) {
        return dataSourceType != DataSourceType.TYPE_PICASA;
    }

    @Override
    public int renderSlot(GLCanvas canvas, int index, int pass, int width, int height) {
        AlbumSetEntry entry = mDataWindow.get(index);

        boolean wantCache = entry.cacheFlag == MediaSet.CACHE_FLAG_FULL;
        boolean isCaching = wantCache && (
                entry.cacheStatus != MediaSet.CACHE_STATUS_CACHED_FULL);
        boolean selected = mSelectionManager.isItemSelected(entry.setPath);
        boolean chooseToCache = wantCache ^ selected;
        boolean available = isLocal(entry.sourceType) || chooseToCache;

        int renderRequestFlags = 0;

        if (!available) {
            canvas.save(GLCanvas.SAVE_FLAG_ALPHA);
            canvas.multiplyAlpha(0.6f);
        }
        renderRequestFlags |= renderContent(canvas, entry, width, height);
        if (!available) canvas.restore();

        renderRequestFlags |= renderLabel(canvas, entry, width, height);

        drawCachingPin(canvas, entry.setPath,
                entry.sourceType, isCaching, chooseToCache, width, height);

        renderRequestFlags |= renderOverlay(canvas, index, entry, width, height);
        return renderRequestFlags;
    }

    private void drawCachingPin(GLCanvas canvas, Path path, int dataSourceType,
            boolean isCaching, boolean chooseToCache, int width, int height) {
        ResourceTexture icon;
        if (isLocal(dataSourceType)) {
            icon = mLocalAlbumIcon;
        } else if (chooseToCache) {
            icon = mCheckedItem;
        } else {
            icon = mUnCheckedItem;
        }

        // show the icon in right bottom
        int s = mCachePinSize;
        int m = mCachePinMargin;
        icon.draw(canvas, width - m - s, height - s, s, s);

        if (isCaching) {
            int w = mCachingText.getWidth();
            int h = mCachingText.getHeight();
            // Show the caching text in bottom center
            mCachingText.draw(canvas, (width - w) / 2, height - h);
        }
    }
}
