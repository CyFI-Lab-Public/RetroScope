/*******************************************************************************
 *      Copyright (C) 2011 Google Inc.
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
import android.os.Parcel;
import android.os.Parcelable;

import com.android.mail.R;
import com.android.mail.providers.Folder;

/**
 * A simple holder class that stores the information to undo the application of a folder.
 */
public class ToastBarOperation implements Parcelable,
        ActionableToastBar.ActionClickedListener {
    public static final int UNDO = 0;
    public static final int ERROR = 1;
    private final int mAction;
    private final int mCount;
    private final boolean mBatch;
    private final int mType;
    private final Folder mFolder;

    /**
     * Create a ToastBarOperation
     *
     * @param count Number of conversations this action would be applied to.
     * @param menuId res id identifying the menu item tapped; used to determine what action was
     *        performed
     * @param operationFolder The {@link Folder} upon which the operation was run. This may be
     *        <code>null</code>, but is required in {@link #getDescription(Context)} for certain
     *        actions.
     */
    public ToastBarOperation(int count, int menuId, int type, boolean batch,
            final Folder operationFolder) {
        mCount = count;
        mAction = menuId;
        mBatch = batch;
        mType = type;
        mFolder = operationFolder;
    }

    public int getType() {
        return mType;
    }

    public boolean isBatchUndo() {
        return mBatch;
    }

    public ToastBarOperation(final Parcel in, final ClassLoader loader) {
        mCount = in.readInt();
        mAction = in.readInt();
        mBatch = in.readInt() != 0;
        mType = in.readInt();
        mFolder = in.readParcelable(loader);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append(super.toString());
        sb.append(" mAction=");
        sb.append(mAction);
        sb.append(" mCount=");
        sb.append(mCount);
        sb.append(" mBatch=");
        sb.append(mBatch);
        sb.append(" mType=");
        sb.append(mType);
        sb.append(" mFolder=");
        sb.append(mFolder);
        sb.append("}");
        return sb.toString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mCount);
        dest.writeInt(mAction);
        dest.writeInt(mBatch ? 1 : 0);
        dest.writeInt(mType);
        dest.writeParcelable(mFolder, 0);
    }

    public static final ClassLoaderCreator<ToastBarOperation> CREATOR =
            new ClassLoaderCreator<ToastBarOperation>() {
        @Override
        public ToastBarOperation createFromParcel(final Parcel source) {
            return createFromParcel(source, null);
        }

        @Override
        public ToastBarOperation[] newArray(final int size) {
            return new ToastBarOperation[size];
        }

        @Override
        public ToastBarOperation createFromParcel(final Parcel source, final ClassLoader loader) {
            return new ToastBarOperation(source, loader);
        }
    };

    /**
     * Get a string description of the operation that will be performed
     * when the user taps the undo bar.
     */
    public String getDescription(Context context) {
        final int resId;
        if (mAction == R.id.delete) {
            resId = R.plurals.conversation_deleted;
        } else if (mAction == R.id.remove_folder) {
            return context.getString(R.string.folder_removed, mFolder.name);
        } else if (mAction == R.id.change_folders) {
            resId = R.plurals.conversation_folder_changed;
        } else if (mAction == R.id.move_folder) {
            return context.getString(R.string.conversation_folder_moved, mFolder.name);
        } else if (mAction == R.id.archive) {
            resId = R.plurals.conversation_archived;
        } else if (mAction == R.id.report_spam) {
            resId = R.plurals.conversation_spammed;
        } else if (mAction == R.id.mark_not_spam) {
            resId = R.plurals.conversation_not_spam;
        } else if (mAction == R.id.mark_not_important) {
            resId = R.plurals.conversation_not_important;
        } else if (mAction == R.id.mute) {
            resId = R.plurals.conversation_muted;
        } else if (mAction == R.id.remove_star) {
            resId = R.plurals.conversation_unstarred;
        } else if (mAction == R.id.report_phishing) {
            resId = R.plurals.conversation_phished;
        } else {
            resId = -1;
        }
        final String desc = (resId == -1) ? "" :
                String.format(context.getResources().getQuantityString(resId, mCount), mCount);
        return desc;
    }

    public String getSingularDescription(Context context, Folder folder) {
        if (mAction == R.id.remove_folder) {
            return context.getString(R.string.folder_removed, folder.name);
        }
        final int resId;
        if (mAction ==  R.id.delete) {
            resId = R.string.deleted;
        } else if (mAction == R.id.archive) {
            resId = R.string.archived;
        } else {
            resId = -1;
        }
        return (resId == -1) ? "" : context.getString(resId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Returns true if this object should take precedence
     * when the {@link ActionableToastBar}'s action button is clicked.
     * If <code>true</code>, the listener passed in {@link ActionableToastBar#show}
     * will not be used. The default implementation returns false. Derived
     * classes should override if this behavior is desired.
     */
    public boolean shouldTakeOnActionClickedPrecedence() {
        return false;
    }

    @Override
    public void onActionClicked(Context context) {
        // DO NOTHING
    }

    public void onToastBarTimeout(Context context) {
        // DO NOTHING
    }
}
