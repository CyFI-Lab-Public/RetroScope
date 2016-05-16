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

package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterCropRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageCrop;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

public class EditorCrop extends Editor implements EditorInfo {
    public static final String TAG = EditorCrop.class.getSimpleName();
    public static final int ID = R.id.editorCrop;

    // Holder for an aspect ratio it's string id
    protected static final class AspectInfo {
        int mAspectX;
        int mAspectY;
        int mStringId;
        AspectInfo(int stringID, int x, int y) {
            mStringId = stringID;
            mAspectX = x;
            mAspectY = y;
        }
    };

    // Mapping from menu id to aspect ratio
    protected static final SparseArray<AspectInfo> sAspects;
    static {
        sAspects = new SparseArray<AspectInfo>();
        sAspects.put(R.id.crop_menu_1to1, new AspectInfo(R.string.aspect1to1_effect, 1, 1));
        sAspects.put(R.id.crop_menu_4to3, new AspectInfo(R.string.aspect4to3_effect, 4, 3));
        sAspects.put(R.id.crop_menu_3to4, new AspectInfo(R.string.aspect3to4_effect, 3, 4));
        sAspects.put(R.id.crop_menu_5to7, new AspectInfo(R.string.aspect5to7_effect, 5, 7));
        sAspects.put(R.id.crop_menu_7to5, new AspectInfo(R.string.aspect7to5_effect, 7, 5));
        sAspects.put(R.id.crop_menu_none, new AspectInfo(R.string.aspectNone_effect, 0, 0));
        sAspects.put(R.id.crop_menu_original, new AspectInfo(R.string.aspectOriginal_effect, 0, 0));
    }

    protected ImageCrop mImageCrop;
    private String mAspectString = "";

    public EditorCrop() {
        super(ID);
        mChangesGeometry = true;
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        if (mImageCrop == null) {
            mImageCrop = new ImageCrop(context);
        }
        mView = mImageShow = mImageCrop;
        mImageCrop.setEditor(this);
    }

    @Override
    public void reflectCurrentFilter() {
        MasterImage master = MasterImage.getImage();
        master.setCurrentFilterRepresentation(master.getPreset()
                .getFilterWithSerializationName(FilterCropRepresentation.SERIALIZATION_NAME));
        super.reflectCurrentFilter();
        FilterRepresentation rep = getLocalRepresentation();
        if (rep == null || rep instanceof FilterCropRepresentation) {
            mImageCrop.setFilterCropRepresentation((FilterCropRepresentation) rep);
        } else {
            Log.w(TAG, "Could not reflect current filter, not of type: "
                    + FilterCropRepresentation.class.getSimpleName());
        }
        mImageCrop.invalidate();
    }

    @Override
    public void finalApplyCalled() {
        commitLocalRepresentation(mImageCrop.getFinalRepresentation());
    }

    @Override
    public void openUtilityPanel(final LinearLayout accessoryViewList) {
        Button view = (Button) accessoryViewList.findViewById(R.id.applyEffect);
        view.setText(mContext.getString(R.string.crop));
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                showPopupMenu(accessoryViewList);
            }
        });
    }

    private void changeCropAspect(int itemId) {
        AspectInfo info = sAspects.get(itemId);
        if (info == null) {
            throw new IllegalArgumentException("Invalid resource ID: " + itemId);
        }
        if (itemId == R.id.crop_menu_original) {
            mImageCrop.applyOriginalAspect();
        } else if (itemId == R.id.crop_menu_none) {
            mImageCrop.applyFreeAspect();
        } else {
            mImageCrop.applyAspect(info.mAspectX, info.mAspectY);
        }
        setAspectString(mContext.getString(info.mStringId));
    }

    private void showPopupMenu(LinearLayout accessoryViewList) {
        final Button button = (Button) accessoryViewList.findViewById(R.id.applyEffect);
        final PopupMenu popupMenu = new PopupMenu(mImageShow.getActivity(), button);
        popupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_crop, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                changeCropAspect(item.getItemId());
                return true;
            }
        });
        popupMenu.show();
        ((FilterShowActivity)mContext).onShowMenu(popupMenu);
    }

    @Override
    public void setUtilityPanelUI(View actionButton, View editControl) {
        super.setUtilityPanelUI(actionButton,editControl);
        setMenuIcon(true);
    }

    @Override
    public boolean showsSeekBar() {
        return false;
    }

    @Override
    public int getTextId() {
        return R.string.crop;
    }

    @Override
    public int getOverlayId() {
        return R.drawable.filtershow_button_geometry_crop;
    }

    @Override
    public boolean getOverlayOnly() {
        return true;
    }

    private void setAspectString(String s) {
        mAspectString = s;
    }
}
