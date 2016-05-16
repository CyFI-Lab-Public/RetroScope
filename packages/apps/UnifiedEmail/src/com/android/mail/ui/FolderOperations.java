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

import com.android.mail.providers.Folder;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Object that contains a list of folder operations (application/removals of folders)
 *
 */
// This was earlier called FolderOperations
public class FolderOperations {
    /**
     * Map of the Folders that that either need to be applied or removed
     * The key is the canonical name of the Folder, and the value is a boolean,
     * when true, the Folder should be added, and when false, the Folder
     * should be removed
     */
    private final Map<String, Operation> mOperations;

    private static final String LOG_TAG = LogTag.getLogTag();

    public FolderOperations() {
        mOperations = Maps.newHashMap();
    }

    public FolderOperations(Folder folder, boolean add) {
        this();
        if (folder != null) {
            add(folder, add);
        } else {
            LogUtils.e(LOG_TAG, "FolderOperation created with null Folder object");
        }
    }

    /**
     * Adds an operation to the list of folder operations to be applied. The last
     * operation for a folder will be retained in the list of operations.
     * @param folder Folder to be applied
     * @param add True if the folder should be applied, or false if the folder should be removed
     */
    public void add(Folder folder, boolean add) {
        Operation operation = new Operation(folder, add);

        mOperations.put(folder.name, operation);
    }

    /**
     * Returns true if there is an operation for the specified folder
     * @param folder Folder
     * @return Returns true if there is a add or remove operation for
     * the specified folder
     */
    public boolean hasOperation(Folder folder) {
        return hasOperation(folder.name);
    }

    /**
     * Returns true if there is an operation for the specified folder
     * @param canonicalName Canonical name of the folder
     * @return Returns true if there is a add or remove operation for
     * the specified folder
     */
    public boolean hasOperation(String canonicalName) {
        return mOperations.containsKey(canonicalName);
    }

    /**
     * Returns true if the specified folder will be applied
     * @param folder Folder
     * @return Returns true if there is an operation that will apply the folder
     */
    public boolean hasApplyOperation(Folder folder) {
        return hasApplyOperation(folder.name);
    }

    /**
     * Returns true if the specified folder will be applied
     * @param canonicalName Canonical name of the folder
     * @return Returns true if there is an operation that will apply the folder
     */
    public boolean hasApplyOperation(String canonicalName) {
        if (hasOperation(canonicalName)) {
            Operation operation = mOperations.get(canonicalName);
            return operation.mAdd;
        }
        return false;
    }

    /**
     * Returns true if the specified folder will be removed
     * @param folder folder
     * @return Returns true if there is an operation that will remove the folder
     */
    public boolean hasRemoveOperation(Folder folder) {
        return hasRemoveOperation(folder.name);
    }

    /**
     * Returns true if the specified folder will be removed
     * @param canonicalName Canonical name of the folder
     * @return Returns true if there is an operation that will remove the folder
     */
    public boolean hasRemoveOperation(String canonicalName) {
        if (hasOperation(canonicalName)) {
            Operation operation = mOperations.get(canonicalName);
            return !operation.mAdd;
        }
        return false;
    }

    public void clear() {
        mOperations.clear();
    }

    /**
     * Return the number of folder operations
     */
    public int count() {
        return mOperations.size();
    }

    /**
     * Returns a FolderOperations object that will revert the operations described in
     * this FolderOperations instance
     * @return FolderOperations object that will revert
     */
    public FolderOperations undoOperation() {
        FolderOperations undoOperations = new FolderOperations();
        Set<Map.Entry<String, Operation>> operationSet = mOperations.entrySet();
        for (Map.Entry<String, Operation> operationItem : operationSet) {
            Operation operationToUndo = operationItem.getValue();
            undoOperations.add(operationToUndo.mFolder, !operationToUndo.mAdd);
        }
        return undoOperations;
    }

    /**
     * Returns an array of the folder operations
     * @return Array of the folder operations to perform
     */
    public List<Operation> getOperationList() {
        List<Operation> results = Lists.newArrayList();
        Set<Map.Entry<String, Operation>> operationSet = mOperations.entrySet();
        for (Map.Entry<String, Operation> operationItem : operationSet) {
            results.add(operationItem.getValue());
        }
        return results;
    }

    /**
     * Serialize the FolderOperations
     * Not implemented!!
     * TODO(viki): Copy over from Gmail Labels#serialize(FolderOperations)
     * @return Serialized representation of the folder operations
     */
    public static String serialize(FolderOperations operations) {
        return "";
    }

    /**
     * Deserialize a encoded string and instantiates a FolderOperations object
     * Not implemented!!
     * TODO(viki): Copy over from Gmail Labels#deSerialize(String)
     * @param encodedFolderOperations Encode FolderOperations string
     * @return FolderOperations object
     */
    public static FolderOperations deserialize(String encodedFolderOperations) {
        return null;
    }

    /**
     * An operation that can be performed on the folder.
     *
     */
    private class Operation {
        /**
         * True if the action is to include in the folder, false if the action is to remove it
         * from the folder.
         */
        public final boolean mAdd;

        /**
         * The  name of the folder for which the operation is performed.
         */
        public final Folder mFolder;

        /**
         * Create a new operation, which is to add the message to the given folder
         * @param folder Name of the folder.
         * @param add true if message has to be added to the folder. False if it has to be removed
         * from the existing folder.
         */
        private Operation(Folder folder, boolean add) {
            mFolder = folder;
            mAdd = add;
        }
    }

}
