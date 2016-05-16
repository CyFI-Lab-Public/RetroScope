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

import com.android.mail.R;
import com.android.mail.providers.Folder;
import com.android.mail.providers.UIProvider.FolderCapabilities;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * An adapter for translating a cursor of {@link Folder} to a set of selectable views to be used for
 * applying folders to one or more conversations.
 */
public class FolderSelectorAdapter extends BaseAdapter {

    public static class FolderRow implements Comparable<FolderRow> {
        private final Folder mFolder;
        private boolean mIsPresent;
        // Filled in during folderSort
        public String mPathName;

        public FolderRow(Folder folder, boolean isPresent) {
            mFolder = folder;
            mIsPresent = isPresent;
        }

        public Folder getFolder() {
            return mFolder;
        }

        public boolean isPresent() {
            return mIsPresent;
        }

        public void setIsPresent(boolean isPresent) {
            mIsPresent = isPresent;
        }

        @Override
        public int compareTo(FolderRow another) {
            // TODO: this should sort the system folders in the appropriate order
            if (equals(another)) {
                return 0;
            } else if (mIsPresent != another.mIsPresent) {
                return mIsPresent ? -1 : 1;
            } else {
                return mFolder.name.compareToIgnoreCase(another.mFolder.name);
            }
        }

    }

    protected final List<FolderRow> mFolderRows = Lists.newArrayList();
    private final LayoutInflater mInflater;
    private final int mLayout;
    private final String mHeader;
    private Folder mExcludedFolder;


    public FolderSelectorAdapter(Context context, Cursor folders,
            Set<String> initiallySelected, int layout, String header) {
        mInflater = LayoutInflater.from(context);
        mLayout = layout;
        mHeader = header;
        createFolderRows(folders, initiallySelected);
    }

    public FolderSelectorAdapter(Context context, Cursor folders, int layout, String header,
            Folder excludedFolder) {
        mInflater = LayoutInflater.from(context);
        mLayout = layout;
        mHeader = header;
        mExcludedFolder = excludedFolder;
        createFolderRows(folders, null);
    }

    protected void createFolderRows(Cursor folders, Set<String> initiallySelected) {
        if (folders == null) {
            return;
        }
        final List<FolderRow> allFolders = new ArrayList<FolderRow>(folders.getCount());

        if (folders.moveToFirst()) {
            do {
                final Folder folder = new Folder(folders);
                final boolean isSelected = initiallySelected != null
                        && initiallySelected.contains(
                        folder.folderUri.getComparisonUri().toString());
                final FolderRow row = new FolderRow(folder, isSelected);
                allFolders.add(row);
            } while (folders.moveToNext());
        }
        // Need to do the foldersort first with all folders present to avoid dropping orphans
        folderSort(allFolders);

        // Rows corresponding to user created, unchecked folders.
        final List<FolderRow> userUnselected = new ArrayList<FolderRow>();
        // Rows corresponding to system created, unchecked folders.
        final List<FolderRow> systemUnselected = new ArrayList<FolderRow>();

        // Divert the folders to the appropriate sections
        for (final FolderRow row : allFolders) {
            final Folder folder = row.getFolder();
            if (meetsRequirements(folder) && !Objects.equal(folder, mExcludedFolder)) {
                // Add the currently selected first.
                if (row.isPresent()) {
                    mFolderRows.add(row);
                } else if (folder.isProviderFolder()) {
                    systemUnselected.add(row);
                } else {
                    userUnselected.add(row);
                }
            }
        }
        mFolderRows.addAll(systemUnselected);
        mFolderRows.addAll(userUnselected);
    }

    /**
     * Wrapper class to construct a hierarchy tree of FolderRow objects for sorting
     */
    private static class TreeNode implements Comparable<TreeNode> {
        public FolderRow mWrappedObject;
        final public PriorityQueue<TreeNode> mChildren = new PriorityQueue<TreeNode>();
        public boolean mAddedToList = false;

        TreeNode(FolderRow wrappedObject) {
            mWrappedObject = wrappedObject;
        }

        void addChild(final TreeNode child) {
            mChildren.add(child);
        }

        TreeNode pollChild() {
            return mChildren.poll();
        }

        @Override
        public int compareTo(TreeNode o) {
            // mWrappedObject is always non-null here because we set it before we add this object
            // to a sorted collection, otherwise we wouldn't have known what collection to add it to
            return mWrappedObject.compareTo(o.mWrappedObject);
        }
    }

    /**
     * Sorts the folder list according to hierarchy.
     * If no parent information exists this basically just turns into a heap sort
     *
     * How this works:
     * When the first part of this algorithm completes, we want to have a tree of TreeNode objects
     * mirroring the hierarchy of mailboxes/folders in the user's account, but we don't have any
     * guarantee that we'll see the parents before their respective children.
     * First we check the nodeMap to see if we've already pre-created (see below) a TreeNode for
     * the current FolderRow, and if not then we create one now.
     * Then for each folder, we check to see if the parent TreeNode has already been created. We
     * special case the root node. If we don't find the parent node, then we pre-create one to fill
     * in later (see above) when we eventually find the parent's entry.
     * Whenever we create a new TreeNode we add it to the nodeMap keyed on the folder's provider
     * Uri, so that we can find it later either to add children or to retrieve a half-created node.
     * It should be noted that it is only valid to add a child node after the mWrappedObject
     * member variable has been set.
     * Finally we do a depth-first traversal of the constructed tree to re-fill the folderList in
     * hierarchical order.
     * @param folderList List of {@link Folder} objects to sort
     */
    private void folderSort(final List<FolderRow> folderList) {
        final TreeNode root = new TreeNode(null);
        // Make double-sure we don't accidentally add the root node to the final list
        root.mAddedToList = true;
        // Map from folder Uri to TreeNode containing said folder
        final Map<Uri, TreeNode> nodeMap = new HashMap<Uri, TreeNode>(folderList.size());
        nodeMap.put(Uri.EMPTY, root);

        for (final FolderRow folderRow : folderList) {
            final Folder folder = folderRow.mFolder;
            // Find-and-complete or create the TreeNode wrapper
            TreeNode node = nodeMap.get(folder.folderUri.getComparisonUri());
            if (node == null) {
                node = new TreeNode(folderRow);
                nodeMap.put(folder.folderUri.getComparisonUri(), node);
            } else {
                node.mWrappedObject = folderRow;
            }
            // Special case the top level folders
            if (folderRow.mFolder.parent == null || folderRow.mFolder.parent.equals(Uri.EMPTY)) {
                root.addChild(node);
            } else {
                // Find or half-create the parent TreeNode wrapper
                TreeNode parentNode = nodeMap.get(folder.parent);
                if (parentNode == null) {
                    parentNode = new TreeNode(null);
                    nodeMap.put(folder.parent, parentNode);
                }
                parentNode.addChild(node);
            }
        }

        folderList.clear();

        // Depth-first traversal of the constructed tree. Flattens the tree back into the
        // folderList list and sets mPathName in the FolderRow objects
        final Deque<TreeNode> stack = new ArrayDeque<TreeNode>(10);
        stack.push(root);
        TreeNode currentNode;
        while ((currentNode = stack.poll()) != null) {
            final TreeNode parentNode = stack.peek();
            // If parentNode is null then currentNode is the root node (not a real folder)
            // If mAddedToList is true it means we've seen this node before and just want to
            // iterate the children.
            if (parentNode != null && !currentNode.mAddedToList) {
                final String pathName;
                // If the wrapped object is null then the parent is the root
                if (parentNode.mWrappedObject == null ||
                        TextUtils.isEmpty(parentNode.mWrappedObject.mPathName)) {
                    pathName = currentNode.mWrappedObject.mFolder.name;
                } else {
                    /**
                     * This path name is re-split at / characters in
                     * {@link HierarchicalFolderSelectorAdapter#truncateHierarchy}
                     */
                    pathName = parentNode.mWrappedObject.mPathName + "/"
                            + currentNode.mWrappedObject.mFolder.name;
                }
                currentNode.mWrappedObject.mPathName = pathName;
                folderList.add(currentNode.mWrappedObject);
                // Mark this node as done so we don't re-add it
                currentNode.mAddedToList = true;
            }
            final TreeNode childNode = currentNode.pollChild();
            if (childNode != null) {
                // If we have children to deal with, re-push the current node as the parent...
                stack.push(currentNode);
                // ... then add the child node and loop around to deal with it...
                stack.push(childNode);
            }
            // ... otherwise we're done with currentNode
        }
    }

    /**
     * Return whether the supplied folder meets the requirements to be displayed
     * in the folder list.
     */
    protected boolean meetsRequirements(Folder folder) {
        // We only want to show the non-Trash folders that can accept moved messages
        return folder.supportsCapability(FolderCapabilities.CAN_ACCEPT_MOVED_MESSAGES) &&
                !folder.isTrash() && !Objects.equal(folder, mExcludedFolder);
    }

    @Override
    public int getCount() {
        return mFolderRows.size() + (hasHeader() ? 1 : 0);
    }

    @Override
    public Object getItem(int position) {
        if (isHeader(position)) {
            return mHeader;
        }
        return mFolderRows.get(correctPosition(position));
    }

    @Override
    public long getItemId(int position) {
        if (isHeader(position)) {
            return -1;
        }
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeader(position)) {
            return SeparatedFolderListAdapter.TYPE_SECTION_HEADER;
        } else {
            return SeparatedFolderListAdapter.TYPE_ITEM;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    /**
     * Returns true if this position represents the header.
     */
    protected final boolean isHeader(int position) {
        return position == 0 && hasHeader();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // The header is at the top
        if (isHeader(position)) {
            final TextView view = convertView != null ? (TextView) convertView :
                (TextView) mInflater.inflate(R.layout.folder_header, parent, false);
            view.setText(mHeader);
            return view;
        }
        final View view;

        if (convertView == null) {
            view = mInflater.inflate(mLayout, parent, false);
        } else {
            view = convertView;
        }
        final FolderRow row = (FolderRow) getItem(position);
        final Folder folder = row.getFolder();
        final String folderDisplay = !TextUtils.isEmpty(row.mPathName) ?
                row.mPathName : folder.name;
        final CompoundButton checkBox = (CompoundButton) view.findViewById(R.id.checkbox);
        if (checkBox != null) {
            // Suppress the checkbox selection, and handle the toggling of the
            // folder on the parent list item's click handler.
            checkBox.setClickable(false);
            checkBox.setText(folderDisplay);
            checkBox.setChecked(row.isPresent());
        }
        final TextView display = (TextView) view.findViewById(R.id.folder_name);
        if (display != null) {
            display.setText(folderDisplay);
        }
        final View colorBlock = view.findViewById(R.id.color_block);
        final ImageView iconView = (ImageView) view.findViewById(R.id.folder_icon);
        Folder.setFolderBlockColor(folder, colorBlock);
        Folder.setIcon(folder, iconView);
        return view;
    }

    private boolean hasHeader() {
        return mHeader != null;
    }

    /**
     * Since this adapter may contain 2 types of data, make sure that we offset
     * the position being asked for correctly.
     */
    public int correctPosition(int position) {
        return hasHeader() ? position-1 : position;
    }
}
