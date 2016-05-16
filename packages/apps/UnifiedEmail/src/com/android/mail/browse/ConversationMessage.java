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

package com.android.mail.browse;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.android.emailcommon.internet.MimeMessage;
import com.android.emailcommon.mail.MessagingException;
import com.android.mail.browse.MessageCursor.ConversationController;
import com.android.mail.content.CursorCreator;
import com.android.mail.providers.Attachment;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Message;
import com.android.mail.ui.ConversationUpdater;
import com.google.common.base.Objects;

/**
 * A message created as part of a conversation view. Sometimes, like during star/unstar, it's
 * handy to have the owning {@link com.android.mail.providers.Conversation} for context.
 *
 * <p>This class must remain separate from the {@link MessageCursor} from whence it came,
 * because cursors can be closed by their Loaders at any time. The
 * {@link ConversationController} intermediate is used to obtain the currently opened cursor.
 *
 * <p>(N.B. This is a {@link android.os.Parcelable}, so try not to add non-transient fields here.
 * Parcelable state belongs either in {@link com.android.mail.providers.Message} or
 * {@link com.android.mail.ui.ConversationViewState.MessageViewState}. The
 * assumption is that this class never needs the state of its extra context saved.)
 */
public final class ConversationMessage extends Message {

    private transient ConversationController mController;

    private ConversationMessage(Cursor cursor) {
        super(cursor);
    }

    public ConversationMessage(Context context, MimeMessage mimeMessage, Uri emlFileUri)
            throws MessagingException {
        super(context, mimeMessage, emlFileUri);
    }

    public void setController(ConversationController controller) {
        mController = controller;
    }

    public Conversation getConversation() {
        return mController != null ? mController.getConversation() : null;
    }

    /**
     * Returns a hash code based on this message's identity, contents and current state.
     * This is a separate method from hashCode() to allow for an instance of this class to be
     * a functional key in a hash-based data structure.
     *
     */
    public int getStateHashCode() {
        return Objects.hashCode(uri, read, starred, getAttachmentsStateHashCode());
    }

    private int getAttachmentsStateHashCode() {
        int hash = 0;
        for (Attachment a : getAttachments()) {
            final Uri uri = a.getIdentifierUri();
            hash += (uri != null ? uri.hashCode() : 0);
        }
        return hash;
    }

    public boolean isConversationStarred() {
        final MessageCursor c = mController.getMessageCursor();
        return c != null && c.isConversationStarred();
    }

    public void star(boolean newStarred) {
        final ConversationUpdater listController = mController.getListController();
        if (listController != null) {
            listController.starMessage(this, newStarred);
        }
    }

    /**
     * Public object that knows how to construct Messages given Cursors.
     */
    public static final CursorCreator<ConversationMessage> FACTORY =
            new CursorCreator<ConversationMessage>() {
                @Override
                public ConversationMessage createFromCursor(Cursor c) {
                    return new ConversationMessage(c);
                }

                @Override
                public String toString() {
                    return "ConversationMessage CursorCreator";
                }
            };

}
