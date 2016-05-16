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

package com.android.gallery3d.filtershow.state;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.editors.ImageOnlyEditor;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

import java.util.Vector;

public class StateAdapter extends ArrayAdapter<State> {

    private static final String LOGTAG = "StateAdapter";
    private int mOrientation;
    private String mOriginalText;
    private String mResultText;

    public StateAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        mOriginalText = context.getString(R.string.state_panel_original);
        mResultText = context.getString(R.string.state_panel_result);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        StateView view = null;
        if (convertView == null) {
            convertView = new StateView(getContext());
        }
        view = (StateView) convertView;
        State state = getItem(position);
        view.setState(state);
        view.setOrientation(mOrientation);
        FilterRepresentation currentRep = MasterImage.getImage().getCurrentFilterRepresentation();
        FilterRepresentation stateRep = state.getFilterRepresentation();
        if (currentRep != null && stateRep != null
            && currentRep.getFilterClass() == stateRep.getFilterClass()
            && currentRep.getEditorId() != ImageOnlyEditor.ID) {
            view.setSelected(true);
        } else {
            view.setSelected(false);
        }
        return view;
    }

    public boolean contains(State state) {
        for (int i = 0; i < getCount(); i++) {
            if (state == getItem(i)) {
                return true;
            }
        }
        return false;
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    public void addOriginal() {
        add(new State(mOriginalText));
    }

    public boolean same(Vector<State> states) {
        // we have the original state in addition
        if (states.size() + 1 != getCount()) {
            return false;
        }
        for (int i = 1; i < getCount(); i++) {
            State state = getItem(i);
            if (!state.equals(states.elementAt(i-1))) {
                return false;
            }
        }
        return true;
    }

    public void fill(Vector<State> states) {
        if (same(states)) {
            return;
        }
        clear();
        addOriginal();
        addAll(states);
        notifyDataSetChanged();
    }

    @Override
    public void remove(State state) {
        super.remove(state);
        FilterRepresentation filterRepresentation = state.getFilterRepresentation();
        FilterShowActivity activity = (FilterShowActivity) getContext();
        activity.removeFilterRepresentation(filterRepresentation);
    }
}
