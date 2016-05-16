/*******************************************************************************
 *      Copyright (C) 2012 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail.ui;

import android.content.Context;
import android.database.Cursor;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.android.mail.R;
import com.android.mail.providers.Folder;
import com.google.common.annotations.VisibleForTesting;

import java.util.Set;

public class HierarchicalFolderSelectorAdapter extends FolderSelectorAdapter {

    private Context mContext;

    public HierarchicalFolderSelectorAdapter(Context context, Cursor folders,
            Set<String> initiallySelected, int layout, String header) {
        super(context, folders, initiallySelected, layout, header);
        mContext = context;
    }

    public HierarchicalFolderSelectorAdapter(Context context, Cursor folders, int layout,
            String header, Folder excludedFolder) {
        super(context, folders, layout, header, excludedFolder);
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);
        if (isHeader(position)) {
            return view;
        }
        final FolderRow row = (FolderRow) getItem(position);
        final Folder folder = row.getFolder();
        final CompoundButton checkBox = (CompoundButton) view.findViewById(R.id.checkbox);
        final TextView display = (TextView) view.findViewById(R.id.folder_name);
        final CharSequence displayText = TextUtils.isEmpty(row.mPathName) ? folder.name
                : truncateHierarchy(row.mPathName);
        if (checkBox != null) {
            checkBox.setText(TextUtils.isEmpty(row.mPathName) ? folder.name
                : truncateHierarchy(row.mPathName), TextView.BufferType.SPANNABLE);
        } else {
            display.setText(displayText, TextView.BufferType.SPANNABLE);
        }
        return view;
    }

    /**
     * Truncation of a hierarchy works as follows:
     * 1) If there is just a folder name, return that.
     * 2) If there is a single parent and a folder name, return parent/folder.
     * 3) If there is > 1 but < 3 ancestors, return ancestor/ancestor2/folder
     * 4) If there are > 3 ancestors, return the top most ancestor, and direct parent
     * of the folder, and the folder: ancestor/.../directParent/folder
     */
    @VisibleForTesting
    protected SpannableStringBuilder truncateHierarchy(String hierarchy) {
        if (TextUtils.isEmpty(hierarchy)) {
            return null;
        }
        final String[] splitHierarchy = hierarchy.split("/");
        // We want to keep the last part of the hierachy, as that is the name of
        // the folder.
        final String folderName;
        final String topParentName;
        final String directParentName;
        final SpannableStringBuilder display = new SpannableStringBuilder();
        if (splitHierarchy != null && splitHierarchy.length > 0) {
            final int length = splitHierarchy.length;
            if (length > 2) {
                topParentName = splitHierarchy[0];
                directParentName = splitHierarchy[length - 2];
                folderName = splitHierarchy[length - 1];
            } else if (length > 1) {
                topParentName = splitHierarchy[0];
                directParentName = null;
                folderName = splitHierarchy[length - 1];
            } else {
                topParentName = null;
                directParentName = null;
                folderName = splitHierarchy[0];
            }
            if (!TextUtils.isEmpty(directParentName)) {
                final int formatString;
                if (length > 3) {
                    formatString = R.string.hierarchical_folder_parent_top_ellip;
                } else {
                    formatString = R.string.hierarchical_folder_parent_top;
                }
                display.append(mContext.getResources().getString(formatString, topParentName,
                        directParentName));
            } else if (!TextUtils.isEmpty(topParentName)) {
                display.append(mContext.getResources().getString(R.string.hierarchical_folder_top,
                        topParentName, directParentName));
            }
            display.setSpan(new ForegroundColorSpan(R.color.hierarchical_folder_parent_color), 0,
                    display.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            display.append(folderName);
        }
        return display;
    }
}
