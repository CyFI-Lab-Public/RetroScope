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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.controller.BitmapCaller;
import com.android.gallery3d.filtershow.controller.ColorChooser;
import com.android.gallery3d.filtershow.controller.FilterView;
import com.android.gallery3d.filtershow.filters.FilterColorBorderRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.ImageFilterColorBorder;
import com.android.gallery3d.filtershow.imageshow.ImageShow;

public class EditorColorBorder extends ParametricEditor  {
    private static final String LOGTAG = "EditorColorBorder";
    public static final int ID = R.id.editorColorBorder;

    int[] mBasColors = {
            FilterColorBorderRepresentation.DEFAULT_MENU_COLOR1,
            FilterColorBorderRepresentation.DEFAULT_MENU_COLOR2,
            FilterColorBorderRepresentation.DEFAULT_MENU_COLOR3,
            FilterColorBorderRepresentation.DEFAULT_MENU_COLOR4,
            FilterColorBorderRepresentation.DEFAULT_MENU_COLOR5,
    };
    private EditorColorBorderTabletUI mTabletUI;
    private String mParameterString;
    private int mSelectedColorButton;

    public EditorColorBorder() {
        super(ID);
    }

    @Override
    public String calculateUserMessage(Context context, String effectName, Object parameterValue) {
        FilterColorBorderRepresentation rep = getColorBorderRep();
        if (rep == null) {
            return "";
        }
        if (mParameterString == null) {
            mParameterString = "";
        }
        String paramString;
        String val = rep.getValueString();
        return mParameterString + val;
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        mView = mImageShow = new ImageShow(context);
        super.createEditor(context, frameLayout);
    }

    @Override
    public void reflectCurrentFilter() {
        super.reflectCurrentFilter();
        FilterRepresentation rep = getLocalRepresentation();
        if (rep != null && getLocalRepresentation() instanceof FilterColorBorderRepresentation) {
            FilterColorBorderRepresentation cbRep =
                    (FilterColorBorderRepresentation) getLocalRepresentation();
            if (!ParametricEditor.useCompact(mContext)) {
                if (mTabletUI != null) {
                    mTabletUI.setColorBorderRepresentation(cbRep);
                }

            }
            cbRep.setPramMode(FilterColorBorderRepresentation.PARAM_SIZE);
            mParameterString = mContext.getString(R.string.color_border_size);
            if (mEditControl != null) {
                control(cbRep.getCurrentParam(), mEditControl);
            }
        }
    }

    @Override
    public void openUtilityPanel(final LinearLayout accessoryViewList) {
        Button view = (Button) accessoryViewList.findViewById(R.id.applyEffect);
        view.setText(mContext.getString(R.string.color_border_size));
        view.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                showPopupMenu(accessoryViewList);
            }
        });
    }

    @Override
    public boolean showsSeekBar() {
        return false;
    }

    private void showPopupMenu(LinearLayout accessoryViewList) {
        final Button button = (Button) accessoryViewList.findViewById(
                R.id.applyEffect);
        if (button == null) {
            return;
        }
        final PopupMenu popupMenu = new PopupMenu(mImageShow.getActivity(), button);
        popupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_color_border,
                popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                selectMenuItem(item);
                return true;
            }
        });
        popupMenu.show();
        ((FilterShowActivity)mContext).onShowMenu(popupMenu);
    }

    protected void selectMenuItem(MenuItem item) {
        ImageFilterColorBorder filter = (ImageFilterColorBorder) mImageShow.getCurrentFilter();
        FilterColorBorderRepresentation rep = getColorBorderRep();
        if (rep == null) {
            return;
        }
        switch (item.getItemId()) {
            case R.id.color_border_menu_clear:
                clearFrame();
                break;
            case R.id.color_border_menu_size:
                rep.setPramMode(FilterColorBorderRepresentation.PARAM_SIZE);
                break;
            case R.id.color_border_menu_corner_size:
                rep.setPramMode(FilterColorBorderRepresentation.PARAM_RADIUS);
                break;
            case R.id.color_border_menu_color:
                rep.setPramMode(FilterColorBorderRepresentation.PARAM_COLOR);
                break;
        }
        if (item.getItemId() != R.id.color_border_menu_clear) {
            mParameterString = item.getTitle().toString();
        }
        if (mControl instanceof ColorChooser) {
            ColorChooser c = (ColorChooser) mControl;
            mBasColors = c.getColorSet();
        }
        if (mEditControl != null) {
            control(rep.getCurrentParam(), mEditControl);
        }
        if (mControl instanceof ColorChooser) {
            ColorChooser c = (ColorChooser) mControl;
            c.setColorSet(mBasColors);
        }
        updateText();
        mControl.updateUI();
        mView.invalidate();
    }

    public void clearFrame() {
        commitLocalRepresentation();
    }

    @Override
    public void setUtilityPanelUI(View actionButton, View editControl) {
        if (ParametricEditor.useCompact(mContext)) {
            super.setUtilityPanelUI(actionButton, editControl);
            return;
        }
        mSeekBar = (SeekBar) editControl.findViewById(R.id.primarySeekBar);
        if (mSeekBar != null) {
            mSeekBar.setVisibility(View.GONE);
        }

        mTabletUI = new EditorColorBorderTabletUI(this, mContext, editControl);

    }

    FilterColorBorderRepresentation getColorBorderRep() {
        FilterRepresentation rep = getLocalRepresentation();
        if (rep instanceof FilterColorBorderRepresentation) {
            return (FilterColorBorderRepresentation) rep;
        }
        return null;
    }


}
