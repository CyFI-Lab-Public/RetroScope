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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.history.HistoryManager;
import com.android.gallery3d.filtershow.category.MainPanel;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.state.StatePanel;

public class EditorPanel extends Fragment {

    private static final String LOGTAG = "EditorPanel";

    private LinearLayout mMainView;
    private Editor mEditor;
    private int mEditorID;

    public void setEditor(int editor) {
        mEditorID = editor;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        FilterShowActivity filterShowActivity = (FilterShowActivity) activity;
        mEditor = filterShowActivity.getEditor(mEditorID);
    }

    public void cancelCurrentFilter() {
        MasterImage masterImage = MasterImage.getImage();
        HistoryManager adapter = masterImage.getHistory();

        int position = adapter.undo();
        masterImage.onHistoryItemClick(position);
        ((FilterShowActivity)getActivity()).invalidateViews();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FilterShowActivity activity = (FilterShowActivity) getActivity();
        if (mMainView != null) {
            if (mMainView.getParent() != null) {
                ViewGroup parent = (ViewGroup) mMainView.getParent();
                parent.removeView(mMainView);
            }
            showImageStatePanel(activity.isShowingImageStatePanel());
            return mMainView;
        }
        mMainView = (LinearLayout) inflater.inflate(R.layout.filtershow_editor_panel, null);

        View actionControl = mMainView.findViewById(R.id.panelAccessoryViewList);
        View editControl = mMainView.findViewById(R.id.controlArea);
        ImageButton cancelButton = (ImageButton) mMainView.findViewById(R.id.cancelFilter);
        ImageButton applyButton = (ImageButton) mMainView.findViewById(R.id.applyFilter);
        Button editTitle = (Button) mMainView.findViewById(R.id.applyEffect);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelCurrentFilter();
                FilterShowActivity activity = (FilterShowActivity) getActivity();
                activity.backToMain();
            }
        });

        Button toggleState = (Button) mMainView.findViewById(R.id.toggle_state);
        mEditor = activity.getEditor(mEditorID);
        if (mEditor != null) {
            mEditor.setUpEditorUI(actionControl, editControl, editTitle, toggleState);
            mEditor.reflectCurrentFilter();
            if (mEditor.useUtilityPanel()) {
                mEditor.openUtilityPanel((LinearLayout) actionControl);
            }
        }
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FilterShowActivity activity = (FilterShowActivity) getActivity();
                mEditor.finalApplyCalled();
                activity.backToMain();
            }
        });

        showImageStatePanel(activity.isShowingImageStatePanel());
        return mMainView;
    }

    @Override
    public void onDetach() {
        if (mEditor != null) {
            mEditor.detach();
        }
        super.onDetach();
    }

    public void showImageStatePanel(boolean show) {
        View container = mMainView.findViewById(R.id.state_panel_container);
        FragmentTransaction transaction = null;
        boolean child = false;
        if (container == null) {
            FilterShowActivity activity = (FilterShowActivity) getActivity();
            container = activity.getMainStatePanelContainer(R.id.state_panel_container);
        } else {
            transaction = getChildFragmentManager().beginTransaction();
            child = true;
        }
        if (container == null) {
            return;
        } else {
            transaction = getFragmentManager().beginTransaction();
        }
        Fragment panel = getActivity().getSupportFragmentManager().findFragmentByTag(
                MainPanel.FRAGMENT_TAG);
        if (panel == null || panel instanceof MainPanel) {
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        if (show) {
            container.setVisibility(View.VISIBLE);
            StatePanel statePanel = new StatePanel();
            transaction.replace(R.id.state_panel_container, statePanel, StatePanel.FRAGMENT_TAG);
        } else {
            Fragment statePanel = getChildFragmentManager().findFragmentByTag(StatePanel.FRAGMENT_TAG);
            if (child) {
                statePanel = getFragmentManager().findFragmentByTag(StatePanel.FRAGMENT_TAG);
            }
            if (statePanel != null) {
                transaction.remove(statePanel);
            }
        }
        transaction.commit();
    }
}
