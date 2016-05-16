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

package com.android.gallery3d.filtershow.category;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterDrawRepresentation;
import com.android.gallery3d.filtershow.filters.FilterUserPresetRepresentation;
import com.android.gallery3d.filtershow.pipeline.RenderingRequest;
import com.android.gallery3d.filtershow.pipeline.RenderingRequestCaller;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;

public class Action implements RenderingRequestCaller {

    private static final String LOGTAG = "Action";
    private FilterRepresentation mRepresentation;
    private String mName;
    private Rect mImageFrame;
    private Bitmap mImage;
    private ArrayAdapter mAdapter;
    public static final int FULL_VIEW = 0;
    public static final int CROP_VIEW = 1;
    public static final int ADD_ACTION = 2;
    public static final int SPACER = 3;
    private int mType = CROP_VIEW;
    private Bitmap mPortraitImage;
    private Bitmap mOverlayBitmap;
    private FilterShowActivity mContext;
    private boolean mCanBeRemoved = false;
    private int mTextSize = 32;
    private boolean mIsDoubleAction = false;

    public Action(FilterShowActivity context, FilterRepresentation representation, int type,
                  boolean canBeRemoved) {
        this(context, representation, type);
        mCanBeRemoved = canBeRemoved;
        mTextSize = context.getResources().getDimensionPixelSize(
                R.dimen.category_panel_text_size);
    }

    public Action(FilterShowActivity context, FilterRepresentation representation, int type) {
        this(context, type);
        setRepresentation(representation);
    }

    public Action(FilterShowActivity context, int type) {
        mContext = context;
        setType(type);
        mContext.registerAction(this);
    }

    public Action(FilterShowActivity context, FilterRepresentation representation) {
        this(context, representation, CROP_VIEW);
    }

    public boolean isDoubleAction() {
        return mIsDoubleAction;
    }

    public void setIsDoubleAction(boolean value) {
        mIsDoubleAction = value;
    }

    public boolean canBeRemoved() {
        return mCanBeRemoved;
    }

    public int getType() {
        return mType;
    }

    public FilterRepresentation getRepresentation() {
        return mRepresentation;
    }

    public void setRepresentation(FilterRepresentation representation) {
        mRepresentation = representation;
        mName = representation.getName();
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setImageFrame(Rect imageFrame, int orientation) {
        if (mImageFrame != null && mImageFrame.equals(imageFrame)) {
            return;
        }
        if (getType() == Action.ADD_ACTION) {
            return;
        }
        Bitmap temp = MasterImage.getImage().getTemporaryThumbnailBitmap();
        if (temp != null) {
            mImage = temp;
        }
        Bitmap bitmap = MasterImage.getImage().getThumbnailBitmap();
        if (bitmap != null) {
            mImageFrame = imageFrame;
            int w = mImageFrame.width();
            int h = mImageFrame.height();
            postNewIconRenderRequest(w, h);
        }
    }

    public Bitmap getImage() {
        return mImage;
    }

    public void setImage(Bitmap image) {
        mImage = image;
    }

    public void setAdapter(ArrayAdapter adapter) {
        mAdapter = adapter;
    }

    public void setType(int type) {
        mType = type;
    }

    private void postNewIconRenderRequest(int w, int h) {
        if (mRepresentation != null) {
            ImagePreset preset = new ImagePreset();
            preset.addFilter(mRepresentation);
            RenderingRequest.postIconRequest(mContext, w, h, preset, this);
        }
    }

    private void drawCenteredImage(Bitmap source, Bitmap destination, boolean scale) {
        int minSide = Math.min(destination.getWidth(), destination.getHeight());
        Matrix m = new Matrix();
        float scaleFactor = minSide / (float) Math.min(source.getWidth(), source.getHeight());

        float dx = (destination.getWidth() - source.getWidth() * scaleFactor) / 2.0f;
        float dy = (destination.getHeight() - source.getHeight() * scaleFactor) / 2.0f;
        if (mImageFrame.height() > mImageFrame.width()) {
            // if portrait
            dy -= mTextSize;
        }
        m.setScale(scaleFactor, scaleFactor);
        m.postTranslate(dx, dy);
        Canvas canvas = new Canvas(destination);
        canvas.drawBitmap(source, m, new Paint(Paint.FILTER_BITMAP_FLAG));
    }

    @Override
    public void available(RenderingRequest request) {
        clearBitmap();
        mImage = request.getBitmap();
        if (mImage == null) {
            mImageFrame = null;
            return;
        }
        if (mRepresentation.getOverlayId() != 0 && mOverlayBitmap == null) {
            mOverlayBitmap = BitmapFactory.decodeResource(
                    mContext.getResources(),
                    mRepresentation.getOverlayId());
        }
        if (mOverlayBitmap != null) {
            if (getRepresentation().getFilterType() == FilterRepresentation.TYPE_BORDER) {
                Canvas canvas = new Canvas(mImage);
                canvas.drawBitmap(mOverlayBitmap, new Rect(0, 0, mOverlayBitmap.getWidth(), mOverlayBitmap.getHeight()),
                        new Rect(0, 0, mImage.getWidth(), mImage.getHeight()), new Paint());
            } else {
                Canvas canvas = new Canvas(mImage);
                canvas.drawARGB(128, 0, 0, 0);
                drawCenteredImage(mOverlayBitmap, mImage, false);
            }
        }
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    public void setPortraitImage(Bitmap portraitImage) {
        mPortraitImage = portraitImage;
    }

    public Bitmap getPortraitImage() {
        return mPortraitImage;
    }

    public Bitmap getOverlayBitmap() {
        return mOverlayBitmap;
    }

    public void setOverlayBitmap(Bitmap overlayBitmap) {
        mOverlayBitmap = overlayBitmap;
    }

    public void clearBitmap() {
        if (mImage != null
                && mImage != MasterImage.getImage().getTemporaryThumbnailBitmap()) {
            MasterImage.getImage().getBitmapCache().cache(mImage);
        }
        mImage = null;
    }
}
