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

package com.android.mail.providers;

import android.content.ContentValues;
import android.text.TextUtils;

import com.android.mail.providers.UIProvider.DraftType;
import com.android.mail.providers.UIProvider.MessageColumns;

import java.util.List;

/**
 * A helper class for creating or updating messages. Use the putXxx methods to
 * provide initial or new values for the message. Then save or send the message.
 * To save or send an existing message without making other changes to it simply
 * provide an empty ContentValues.
 */
public class MessageModification {
    /**
     * Sets the message's subject. Only valid for drafts.
     * @param values the ContentValues that will be used to create or update the
     *            message
     * @param subject the new subject
     */
    public static void putSubject(ContentValues values, String subject) {
        values.put(MessageColumns.SUBJECT, subject);
    }

    /**
     * Sets the message's to address. Only valid for drafts.
     * @param values the ContentValues that will be used to create or update the
     *            message
     * @param toAddresses the new to addresses
     */
    public static void putToAddresses(ContentValues values, String[] toAddresses) {
        values.put(MessageColumns.TO, TextUtils.join(UIProvider.EMAIL_SEPARATOR, toAddresses));
    }

    /**
     * Sets the message's cc address. Only valid for drafts.
     * @param values the ContentValues that will be used to create or update the
     *            message
     * @param ccAddresses the new cc addresses
     */
    public static void putCcAddresses(ContentValues values, String[] ccAddresses) {
        values.put(MessageColumns.CC, TextUtils.join(UIProvider.EMAIL_SEPARATOR, ccAddresses));
    }

    /**
     * Sets the message's bcc address. Only valid for drafts.
     * @param values the ContentValues that will be used to create or update the
     *            message
     * @param bccAddresses the new bcc addresses
     */
    public static void putBccAddresses(ContentValues values, String[] bccAddresses) {
        values.put(MessageColumns.BCC, TextUtils.join(UIProvider.EMAIL_SEPARATOR, bccAddresses));
    }

    /**
     * Sets the custom from address for a message, we only set this if its different than the
     * default adress for the account.
     *
     * @param values the ContentValues that will be used to create or update the message
     * @param customFromAddress from address
     */
     public static void putCustomFromAddress(ContentValues values, String customFromAddress) {
        values.put(MessageColumns.CUSTOM_FROM_ADDRESS, customFromAddress);
     }

    /**
     * Saves a flag indicating the message is forwarded. Only valid for drafts
     * not yet sent to / retrieved from server.
     * @param values the ContentValues that will be used to create or update the
     *            message
     * @param forward true if the message is forwarded
     */
    public static void putForward(ContentValues values, boolean forward) {
        values.put(MessageColumns.DRAFT_TYPE, DraftType.FORWARD);
    }

    /**
     * Saves an include quoted text flag. Only valid for drafts not yet sent to
     * / retrieved from server.
     * @param values the ContentValues that will be used to create or update the
     *            message
     * @param includeQuotedText the include quoted text flag
     */
    public static void putAppendRefMessageContent(ContentValues values, boolean includeQuotedText) {
        values.put(MessageColumns.APPEND_REF_MESSAGE_CONTENT, includeQuotedText ? 1 : 0);
    }

    /**
     * Saves a new body for the message. Only valid for drafts.
     * @param values the ContentValues that will be used to create or update the
     *            message
     * @param body the new body of the message
     */
    public static void putBody(ContentValues values, String body) {
        values.put(MessageColumns.BODY_TEXT, body);
    }

    /**
     * Saves a new body for the message. Only valid for drafts.
     * @param values the ContentValues that will be used to create or update the
     *            message
     * @param body the new body of the message
     */
    public static void putBodyHtml(ContentValues values, String body) {
        values.put(MessageColumns.BODY_HTML, body);
    }

    /**
     * Saves the type of the conversation.
     *
     * @param values the ContentValues that will be used to create or update the message
     * @param draftType
     */
    public static void putDraftType(ContentValues values, int draftType) {
        values.put(MessageColumns.DRAFT_TYPE, draftType);
    }

    /**
     * Saves the ref message id for the conversation. It will be a uri.
     *
     * @param values the ContentValues that will be used to create or update the message
     * @param uri of the reference message
     */
    public static void putRefMessageId(ContentValues values, String uri) {
        values.put(MessageColumns.REF_MESSAGE_ID, uri);
    }

    /**
     * Saves a quoted text starting position. Only valid for drafts not yet sent to /
     * retrieved from server.
     *
     * @param values the ContentValues that will be used to create or update the message
     * @param quoteStartPos the starting position for quoted text
     */
    public static void putQuoteStartPos(ContentValues values, int quoteStartPos) {
        values.put(MessageColumns.QUOTE_START_POS, quoteStartPos);
    }

    public static void putAttachments(ContentValues values, List<Attachment> attachments) {
        values.put(MessageColumns.ATTACHMENTS,  Attachment.toJSONArray(attachments));
    }
}
