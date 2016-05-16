/*
 * Copyright (C) 2013 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.ui;

import com.android.mail.R;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A {@link DialogFragment} that shows multiple values, and lets you select 0 to
 * {@link #MAX_SELECTED_VALUES} of them.
 */
public abstract class LimitedMultiSelectDialogFragment extends DialogFragment {
    public interface LimitedMultiSelectDialogListener {
        void onSelectionChanged(Set<String> selectedValues);
    }

    private static final String ARG_ENTRIES = "entries";
    private static final String ARG_ENTRY_VALUES = "entryValues";
    private static final String ARG_SELECTED_VALUES = "selectedValues";

    private WeakReference<LimitedMultiSelectDialogListener> mListener = null;

    // Public no-args constructor needed for fragment re-instantiation
    public LimitedMultiSelectDialogFragment() {}
    /**
     * Populates the arguments on a {@link LimitedMultiSelectDialogFragment}.
     */
    protected static void populateArguments(final LimitedMultiSelectDialogFragment fragment,
            final ArrayList<String> entries, final ArrayList<String> entryValues,
            final ArrayList<String> selectedValues) {
        final Bundle args = new Bundle(3);
        args.putStringArrayList(ARG_ENTRIES, entries);
        args.putStringArrayList(ARG_ENTRY_VALUES, entryValues);
        args.putStringArrayList(ARG_SELECTED_VALUES, selectedValues);
        fragment.setArguments(args);
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final List<String> selectedValuesList =
                getArguments().getStringArrayList(ARG_SELECTED_VALUES);
        final Set<String> selectedValues = Sets.newHashSet(selectedValuesList);

        final List<String> entryValues = getArguments().getStringArrayList(ARG_ENTRY_VALUES);

        final LimitedMultiSelectAdapter adapter = new LimitedMultiSelectAdapter(
                getActivity(), getArguments().getStringArrayList(ARG_ENTRIES), entryValues,
                getMaxSelectedValues());
        adapter.setSelected(selectedValues);

        final ListView listView = new ListView(getActivity());
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(
                    final AdapterView<?> parent, final View view, final int position,
                    final long id) {
                final String entryValue = (String) parent.getItemAtPosition(position);

                if (selectedValues.contains(entryValue)) {
                    // Remove / uncheck
                    selectedValues.remove(entryValue);
                    adapter.removeSelected(entryValue);
                } else {
                    // Add / check
                    selectedValues.add(entryValue);
                    adapter.addSelected(entryValue);
                }

                getArguments().putStringArrayList(ARG_SELECTED_VALUES,
                        new ArrayList<String>(selectedValues));

                adapter.notifyDataSetChanged();
            }
        });

        // Set initial check states
        for (final String value : selectedValues) {
            for (int j = 0; j < entryValues.size(); j++) {
                if (entryValues.get(j).equals(value)) {
                    listView.setItemChecked(j, true);
                }
            }
        }

        return new AlertDialog.Builder(getActivity()).setTitle(getDialogTitle())
                .setView(listView)
                .setPositiveButton(R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        if (mListener != null) {
                            final LimitedMultiSelectDialogListener listener = mListener.get();
                            if (listener != null) {
                                listener.onSelectionChanged(selectedValues);
                            }
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        dismiss();
                    }
                })
                .create();
    }

    public void setListener(final LimitedMultiSelectDialogListener listener) {
        mListener = new WeakReference<LimitedMultiSelectDialogListener>(listener);
    }

    private static class LimitedMultiSelectAdapter extends BaseAdapter {
        private final Context mContext;

        private final List<String> mEntries;
        private final List<String> mEntryValues;

        private final Set<String> mSelectedValues;

        private final int mMaxSelectedValues;

        public LimitedMultiSelectAdapter(final Context context, final List<String> entries,
                final List<String> entryValues, final int maxSelectedValues) {
            mContext = context;

            mEntries = ImmutableList.copyOf(entries);
            mEntryValues = ImmutableList.copyOf(entryValues);

            mSelectedValues = Sets.newHashSetWithExpectedSize(maxSelectedValues);

            mMaxSelectedValues = maxSelectedValues;

            if (mEntries.size() != mEntryValues.size()) {
                throw new IllegalArgumentException("Each entry must have a corresponding value");
            }
        }

        @Override
        public int getCount() {
            return mEntries.size();
        }

        @Override
        public String getItem(final int position) {
            return mEntryValues.get(position);
        }

        @Override
        public long getItemId(final int position) {
            return getItem(position).hashCode();
        }

        @Override
        public int getItemViewType(final int position) {
            return 0;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final CheckedTextView checkedTextView;

            if (convertView == null) {
                checkedTextView = (CheckedTextView) LayoutInflater.from(mContext)
                        .inflate(R.layout.select_dialog_multichoice_holo, null);
            } else {
                checkedTextView = (CheckedTextView) convertView;
            }

            checkedTextView.setText(mEntries.get(position));
            checkedTextView.setEnabled(isEnabled(position));

            return checkedTextView;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public boolean isEmpty() {
            return mEntries.isEmpty();
        }

        @Override
        public boolean areAllItemsEnabled() {
            // If we have less than the maximum number selected, everything is
            // enabled
            if (mMaxSelectedValues > mSelectedValues.size()) {
                return true;
            }

            return false;
        }

        @Override
        public boolean isEnabled(final int position) {
            // If we have less than the maximum selected, everything is enabled
            if (mMaxSelectedValues > mSelectedValues.size()) {
                return true;
            }

            // If we have the maximum selected, only the selected rows are
            // enabled
            if (mSelectedValues.contains(getItem(position))) {
                return true;
            }

            return false;
        }

        public void setSelected(final Collection<String> selectedValues) {
            mSelectedValues.clear();
            mSelectedValues.addAll(selectedValues);
            notifyDataSetChanged();
        }

        public void addSelected(final String selectedValue) {
            mSelectedValues.add(selectedValue);
            notifyDataSetChanged();
        }

        public void removeSelected(final String selectedValue) {
            mSelectedValues.remove(selectedValue);
            notifyDataSetChanged();
        }
    }

    /**
     * Gets a unique String to be used as a tag for this Fragment.
     */
    protected abstract String getFragmentTag();

    /**
     * Gets the maximum number of values that may be selected.
     */
    protected abstract int getMaxSelectedValues();

    /**
     * Gets the title of the dialog.
     */
    protected abstract String getDialogTitle();
}
