/**
 * Copyright (c) 2012, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.mail.ui;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.text.BidiFormatter;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.mail.R;
import com.android.mail.providers.Folder;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;

/**
 * The view for each folder in the folder list.
 */
public class FolderItemView extends RelativeLayout {
    private final String LOG_TAG = LogTag.getLogTag();

    private static final int[] STATE_DRAG_MODE = {R.attr.state_drag_mode};

    private Folder mFolder;
    private TextView mFolderTextView;
    private TextView mUnreadCountTextView;
    private TextView mUnseenCountTextView;
    private DropHandler mDropHandler;
    private ImageView mFolderParentIcon;

    private boolean mIsDragMode;

    /**
     * A delegate for a handler to handle a drop of an item.
     */
    public interface DropHandler {
        /**
         * Return whether or not the drag event is supported by the drop handler. The
         *     {@code FolderItemView} will present appropriate visual affordances if the drag is
         *     supported.
         */
        boolean supportsDrag(DragEvent event, Folder folder);

        /**
         * Handles a drop event, applying the appropriate logic.
         */
        void handleDrop(DragEvent event, Folder folder);
    }

    public FolderItemView(Context context) {
        super(context);
    }

    public FolderItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FolderItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mIsDragMode = false;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mFolderTextView = (TextView)findViewById(R.id.name);
        mUnreadCountTextView = (TextView)findViewById(R.id.unread);
        mUnseenCountTextView = (TextView)findViewById(R.id.unseen);
        mFolderParentIcon = (ImageView) findViewById(R.id.folder_parent_icon);
    }

    /**
     * Returns true if the two folders lead to identical {@link FolderItemView} objects.
     * @param a
     * @param b
     * @return true if the two folders would still lead to the same {@link FolderItemView}.
     */
    public static boolean areSameViews(final Folder a, final Folder b) {
        if (a == null) {
            return b == null;
        }
        if (b == null) {
            // a is not null because it would have returned above.
            return false;
        }
        return (a == b || (a.folderUri.equals(b.folderUri)
                && a.name.equals(b.name)
                && a.hasChildren == b.hasChildren
                && a.unseenCount == b.unseenCount
                && a.unreadCount == b.unreadCount));
    }

    public void bind(final Folder folder, final DropHandler dropHandler,
            final BidiFormatter bidiFormatter) {
        mFolder = folder;
        mDropHandler = dropHandler;

        mFolderTextView.setText(bidiFormatter.unicodeWrap(folder.name));

        mFolderParentIcon.setVisibility(mFolder.hasChildren ? View.VISIBLE : View.GONE);
        if (mFolder.isInbox() && mFolder.unseenCount > 0) {
            mUnreadCountTextView.setVisibility(View.GONE);
            setUnseenCount(mFolder.getBackgroundColor(Color.BLACK), mFolder.unseenCount);
        } else {
            mUnseenCountTextView.setVisibility(View.GONE);
            setUnreadCount(Utils.getFolderUnreadDisplayCount(mFolder));
        }
    }

    /**
     * Sets the icon, if any. If the image view's visibility is set to gone, the text view will
     * be moved over to account for the change.
     */
    public void setIcon(final Folder folder) {
        final ImageView folderIconView = (ImageView) findViewById(R.id.folder_icon);
        Folder.setIcon(folder, folderIconView);
        if (folderIconView.getVisibility() == View.GONE) {
            mFolderTextView.setPadding(getContext()
                    .getResources().getDimensionPixelSize(R.dimen.folder_list_item_left_offset),
                    0, 0, 0 /* No top, right, bottom padding needed */);
        } else {
            // View recycling case
            mFolderTextView.setPadding(0, 0, 0, 0);
        }
    }

    /**
     * Sets the unread count, taking care to hide/show the textview if the count is zero/non-zero.
     */
    private void setUnreadCount(int count) {
        mUnreadCountTextView.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        if (count > 0) {
            mUnreadCountTextView.setText(Utils.getUnreadCountString(getContext(), count));
        }
    }

    /**
     * Sets the unseen count, taking care to hide/show the textview if the count is zero/non-zero.
     */
    private void setUnseenCount(final int color, final int count) {
        mUnseenCountTextView.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        if (count > 0) {
            mUnseenCountTextView.setBackgroundColor(color);
            mUnseenCountTextView.setText(Utils.getUnreadCountString(getContext(), count));
        }
    }

    /**
     * Used if we detect a problem with the unread count and want to force an override.
     * @param count
     */
    public final void overrideUnreadCount(int count) {
        LogUtils.e(LOG_TAG, "FLF->FolderItem.getFolderView: unread count mismatch found (%s vs %d)",
                mUnreadCountTextView.getText(), count);
        setUnreadCount(count);
    }

    private boolean isDroppableTarget(DragEvent event) {
        return (mDropHandler != null && mDropHandler.supportsDrag(event, mFolder));
    }

    /**
     * Handles the drag event.
     *
     * @param event the drag event to be handled
     */
    @Override
    public boolean onDragEvent(DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                // Set drag mode state to true now that we have entered drag mode.
                // This change updates the states of icons and text colors.
                // Additional drawable states are updated by the framework
                // based on the DragEvent.
                setDragMode(true);
            case DragEvent.ACTION_DRAG_ENTERED:
            case DragEvent.ACTION_DRAG_EXITED:
                // All of these states return based on isDroppableTarget's return value.
                // If modifying, watch the switch's drop-through effects.
                return isDroppableTarget(event);
            case DragEvent.ACTION_DRAG_ENDED:
                // Set drag mode to false since we're leaving drag mode.
                // Updates all the states of icons and text colors back to non-drag values.
                setDragMode(false);
                return true;

            case DragEvent.ACTION_DRAG_LOCATION:
                return true;

            case DragEvent.ACTION_DROP:
                if (mDropHandler == null) {
                    return false;
                }

                mDropHandler.handleDrop(event, mFolder);
                return true;
        }
        return false;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (mIsDragMode) {
            mergeDrawableStates(drawableState, STATE_DRAG_MODE);
        }
        return drawableState;
    }

    private void setDragMode(boolean isDragMode) {
        mIsDragMode = isDragMode;
        refreshDrawableState();
    }
}
