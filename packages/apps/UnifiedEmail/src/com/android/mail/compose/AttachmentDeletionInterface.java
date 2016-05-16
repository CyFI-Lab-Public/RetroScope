
package com.android.mail.compose;

import android.view.View.OnClickListener;

/**
 * Interface for views in the compose layout that represent
 * attachments so that the larger attachments view can set
 * the onClickListener for the close button to remove the attachment
 * from the draft.
 */
interface AttachmentDeletionInterface {
    /**
     * Sets the onClickListener for the close button on the attachment view.
     * @param clickListener the listener to which the close button should be set
     */
    public void addDeleteListener(OnClickListener clickListener);
}
