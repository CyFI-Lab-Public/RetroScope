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

import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.android.mail.providers.UIProvider.AccountColumns.SettingsColumns;
import com.android.mail.providers.UIProvider.AutoAdvance;
import com.android.mail.providers.UIProvider.ConversationListIcon;
import com.android.mail.providers.UIProvider.DefaultReplyBehavior;
import com.android.mail.providers.UIProvider.MessageTextSize;
import com.android.mail.providers.UIProvider.SnapHeaderValue;
import com.android.mail.providers.UIProvider.Swipe;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;
import com.google.common.base.Objects;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Model to hold Settings for an account.
 */
public class Settings implements Parcelable {
    private static final String LOG_TAG = LogTag.getLogTag();

    static final Settings EMPTY_SETTINGS = new Settings();

    // Max size for attachments (5 megs). Will be overridden by an account
    // setting, if found.
    private static final int DEFAULT_MAX_ATTACHMENT_SIZE = 5 * 1024 * 1024;

    public static final int SWIPE_SETTING_ARCHIVE = 0;
    public static final int SWIPE_SETTING_DELETE = 1;
    public static final int SWIPE_SETTING_DISABLED = 2;

    private static final int DEFAULT = SWIPE_SETTING_ARCHIVE;

    public final String signature;
    /**
     * Auto advance setting for this account.
     * Integer, one of {@link AutoAdvance#LIST}, {@link AutoAdvance#NEWER},
     * {@link AutoAdvance#OLDER} or  {@link AutoAdvance#UNSET}
     */
    private final int mAutoAdvance;
    private Integer mTransientAutoAdvance = null;
    public final int messageTextSize;
    public final int snapHeaders;
    public final int replyBehavior;
    public final int convListIcon;
    public final boolean  convListAttachmentPreviews;
    public final boolean confirmDelete;
    public final boolean confirmArchive;
    public final boolean confirmSend;
    public final int conversationViewMode;
    public final Uri defaultInbox;
    /**
     * The name of the default inbox: "Inbox" or "Priority Inbox", internationalized...
     */
    public final String defaultInboxName;
    // If you find the need for more default Inbox information: ID or capabilities, then
    // ask viki to replace the above two members with a single JSON object representing the default
    // folder.  That should make all the information about the folder available without an
    // explosion in the number of members.

    public final boolean forceReplyFromDefault;
    public final int maxAttachmentSize;
    public final int swipe;
    /** True if arrows on the priority inbox are enabled. */
    public final boolean priorityArrowsEnabled;
    public final Uri setupIntentUri;
    public final String veiledAddressPattern;

    /**
     * The {@link Uri} to use when moving a conversation to the inbox. May
     * differ from {@link #defaultInbox}.
     */
    public final Uri moveToInbox;

    /** Cached value of hashCode */
    private int mHashCode;

    /** Safe defaults to be used if some values are unspecified. */
    private static final Settings sDefault = EMPTY_SETTINGS;

    private Settings() {
        signature = "";
        mAutoAdvance = AutoAdvance.LIST;
        messageTextSize = MessageTextSize.NORMAL;
        snapHeaders = SnapHeaderValue.ALWAYS;
        replyBehavior = DefaultReplyBehavior.REPLY;
        convListIcon = ConversationListIcon.SENDER_IMAGE;
        convListAttachmentPreviews = true;
        confirmDelete = false;
        confirmArchive = false;
        confirmSend = false;
        defaultInbox = Uri.EMPTY;
        defaultInboxName = "";
        forceReplyFromDefault = false;
        maxAttachmentSize = 0;
        swipe = DEFAULT;
        priorityArrowsEnabled = false;
        setupIntentUri = Uri.EMPTY;
        conversationViewMode = UIProvider.ConversationViewMode.UNDEFINED;
        veiledAddressPattern = null;
        moveToInbox = Uri.EMPTY;
    }

    public Settings(Parcel inParcel) {
        signature = inParcel.readString();
        mAutoAdvance = inParcel.readInt();
        messageTextSize = inParcel.readInt();
        snapHeaders = inParcel.readInt();
        replyBehavior = inParcel.readInt();
        convListIcon = inParcel.readInt();
        convListAttachmentPreviews = inParcel.readInt() != 0;
        confirmDelete = inParcel.readInt() != 0;
        confirmArchive = inParcel.readInt() != 0;
        confirmSend = inParcel.readInt() != 0;
        defaultInbox = Utils.getValidUri(inParcel.readString());
        defaultInboxName = inParcel.readString();
        forceReplyFromDefault = inParcel.readInt() != 0;
        maxAttachmentSize = inParcel.readInt();
        swipe = inParcel.readInt();
        priorityArrowsEnabled = inParcel.readInt() != 0;
        setupIntentUri = Utils.getValidUri(inParcel.readString());
        conversationViewMode = inParcel.readInt();
        veiledAddressPattern = inParcel.readString();
        moveToInbox = Utils.getValidUri(inParcel.readString());
    }

    public Settings(Cursor cursor) {
        signature = cursor.getString(cursor.getColumnIndex(SettingsColumns.SIGNATURE));
        mAutoAdvance = cursor.getInt(cursor.getColumnIndex(SettingsColumns.AUTO_ADVANCE));
        messageTextSize = cursor.getInt(cursor.getColumnIndex(SettingsColumns.MESSAGE_TEXT_SIZE));
        snapHeaders = cursor.getInt(cursor.getColumnIndex(SettingsColumns.SNAP_HEADERS));
        replyBehavior = cursor.getInt(cursor.getColumnIndex(SettingsColumns.REPLY_BEHAVIOR));
        convListIcon = cursor.getInt(cursor.getColumnIndex(SettingsColumns.CONV_LIST_ICON));
        convListAttachmentPreviews = cursor
                .getInt(cursor.getColumnIndex(SettingsColumns.CONV_LIST_ATTACHMENT_PREVIEWS)) != 0;
        confirmDelete = cursor.getInt(cursor.getColumnIndex(SettingsColumns.CONFIRM_DELETE)) != 0;
        confirmArchive = cursor.getInt(cursor.getColumnIndex(SettingsColumns.CONFIRM_ARCHIVE)) != 0;
        confirmSend = cursor.getInt(cursor.getColumnIndex(SettingsColumns.CONFIRM_SEND)) != 0;
        defaultInbox = Utils.getValidUri(
                cursor.getString(cursor.getColumnIndex(SettingsColumns.DEFAULT_INBOX)));
        defaultInboxName =
                cursor.getString(cursor.getColumnIndex(SettingsColumns.DEFAULT_INBOX_NAME));
        forceReplyFromDefault = cursor.getInt(
                cursor.getColumnIndex(SettingsColumns.FORCE_REPLY_FROM_DEFAULT)) != 0;
        maxAttachmentSize =
                cursor.getInt(cursor.getColumnIndex(SettingsColumns.MAX_ATTACHMENT_SIZE));
        swipe = cursor.getInt(cursor.getColumnIndex(SettingsColumns.SWIPE));
        priorityArrowsEnabled = cursor.getInt(
                cursor.getColumnIndex(SettingsColumns.PRIORITY_ARROWS_ENABLED)) != 0;
        setupIntentUri = Utils.getValidUri(
                cursor.getString(cursor.getColumnIndex(SettingsColumns.SETUP_INTENT_URI)));
        conversationViewMode =
                cursor.getInt(cursor.getColumnIndex(SettingsColumns.CONVERSATION_VIEW_MODE));
        veiledAddressPattern =
                cursor.getString(cursor.getColumnIndex(SettingsColumns.VEILED_ADDRESS_PATTERN));
        moveToInbox = Utils.getValidUri(
                cursor.getString(cursor.getColumnIndex(SettingsColumns.MOVE_TO_INBOX)));
    }

    private Settings(JSONObject json) {
        signature = json.optString(SettingsColumns.SIGNATURE, sDefault.signature);
        mAutoAdvance = json.optInt(SettingsColumns.AUTO_ADVANCE, sDefault.getAutoAdvanceSetting());
        messageTextSize = json.optInt(SettingsColumns.MESSAGE_TEXT_SIZE, sDefault.messageTextSize);
        snapHeaders = json.optInt(SettingsColumns.SNAP_HEADERS, sDefault.snapHeaders);
        replyBehavior = json.optInt(SettingsColumns.REPLY_BEHAVIOR, sDefault.replyBehavior);
        convListIcon = json.optInt(SettingsColumns.CONV_LIST_ICON, sDefault.convListIcon);
        convListAttachmentPreviews = json.optBoolean(SettingsColumns.CONV_LIST_ATTACHMENT_PREVIEWS,
                sDefault.convListAttachmentPreviews);
        confirmDelete = json.optBoolean(SettingsColumns.CONFIRM_DELETE, sDefault.confirmDelete);
        confirmArchive = json.optBoolean(SettingsColumns.CONFIRM_ARCHIVE, sDefault.confirmArchive);
        confirmSend = json.optBoolean(SettingsColumns.CONFIRM_SEND, sDefault.confirmSend);
        defaultInbox = Utils.getValidUri( json.optString(SettingsColumns.DEFAULT_INBOX));
        defaultInboxName = json.optString(SettingsColumns.DEFAULT_INBOX_NAME,
                sDefault.defaultInboxName);
        forceReplyFromDefault = json.optBoolean(SettingsColumns.FORCE_REPLY_FROM_DEFAULT,
                sDefault.forceReplyFromDefault);
        maxAttachmentSize =
                json.optInt(SettingsColumns.MAX_ATTACHMENT_SIZE, sDefault.maxAttachmentSize);
        swipe = json.optInt(SettingsColumns.SWIPE, sDefault.swipe);
        priorityArrowsEnabled = json.optBoolean(SettingsColumns.PRIORITY_ARROWS_ENABLED,
                sDefault.priorityArrowsEnabled);
        setupIntentUri = Utils.getValidUri(json.optString(SettingsColumns.SETUP_INTENT_URI));
        conversationViewMode = json.optInt(SettingsColumns.CONVERSATION_VIEW_MODE,
                UIProvider.ConversationViewMode.UNDEFINED);
        veiledAddressPattern = json.optString(SettingsColumns.VEILED_ADDRESS_PATTERN, null);
        moveToInbox = Utils.getValidUri(json.optString(SettingsColumns.MOVE_TO_INBOX));
    }

    /**
     * Return a serialized String for these settings.
     */
    public synchronized String serialize() {
        final JSONObject json = toJSON();
        return json.toString();
    }

    private static Object getNonNull(Object candidate, Object fallback){
        if (candidate == null)
            return fallback;
        return candidate;
    }

    /**
     * Return a JSONObject for these settings.
     */
    public synchronized JSONObject toJSON() {
        final JSONObject json = new JSONObject();
        try {
            json.put(SettingsColumns.SIGNATURE, getNonNull(signature, sDefault.signature));
            json.put(SettingsColumns.AUTO_ADVANCE, getAutoAdvanceSetting());
            json.put(SettingsColumns.MESSAGE_TEXT_SIZE, messageTextSize);
            json.put(SettingsColumns.SNAP_HEADERS, snapHeaders);
            json.put(SettingsColumns.REPLY_BEHAVIOR, replyBehavior);
            json.put(SettingsColumns.CONV_LIST_ICON, convListIcon);
            json.put(SettingsColumns.CONV_LIST_ATTACHMENT_PREVIEWS, convListAttachmentPreviews);
            json.put(SettingsColumns.CONFIRM_DELETE, confirmDelete);
            json.put(SettingsColumns.CONFIRM_ARCHIVE, confirmArchive);
            json.put(SettingsColumns.CONFIRM_SEND, confirmSend);
            json.put(SettingsColumns.DEFAULT_INBOX,
                    getNonNull(defaultInbox, sDefault.defaultInbox));
            json.put(SettingsColumns.DEFAULT_INBOX_NAME,
                    getNonNull(defaultInboxName, sDefault.defaultInboxName));
            json.put(SettingsColumns.FORCE_REPLY_FROM_DEFAULT, forceReplyFromDefault);
            json.put(SettingsColumns.MAX_ATTACHMENT_SIZE,  maxAttachmentSize);
            json.put(SettingsColumns.SWIPE, swipe);
            json.put(SettingsColumns.PRIORITY_ARROWS_ENABLED, priorityArrowsEnabled);
            json.put(SettingsColumns.SETUP_INTENT_URI, setupIntentUri);
            json.put(SettingsColumns.CONVERSATION_VIEW_MODE, conversationViewMode);
            json.put(SettingsColumns.VEILED_ADDRESS_PATTERN, veiledAddressPattern);
            json.put(SettingsColumns.MOVE_TO_INBOX,
                    getNonNull(moveToInbox, sDefault.moveToInbox));
        } catch (JSONException e) {
            LogUtils.wtf(LOG_TAG, e, "Could not serialize settings");
        }
        return json;
    }

    /**
     * Creates a {@link Map} where the column name is the key and the value is the value.
     * @param map map to insert values into or null
     * @return the resulting map
     */
    public Map<String, Object> getValueMap(Map<String, Object> map) {
        if (map == null) {
            map = new HashMap<String, Object>();
        }

        map.put(UIProvider.AccountColumns.SettingsColumns.SIGNATURE, signature);
        map.put(UIProvider.AccountColumns.SettingsColumns.AUTO_ADVANCE, getAutoAdvanceSetting());
        map.put(UIProvider.AccountColumns.SettingsColumns.MESSAGE_TEXT_SIZE, messageTextSize);
        map.put(UIProvider.AccountColumns.SettingsColumns.SNAP_HEADERS, snapHeaders);
        map.put(UIProvider.AccountColumns.SettingsColumns.REPLY_BEHAVIOR, replyBehavior);
        map.put(UIProvider.AccountColumns.SettingsColumns.CONV_LIST_ICON, convListIcon);
        map.put(UIProvider.AccountColumns.SettingsColumns.CONV_LIST_ATTACHMENT_PREVIEWS,
                convListAttachmentPreviews ? 1 : 0);
        map.put(UIProvider.AccountColumns.SettingsColumns.CONFIRM_DELETE, confirmDelete ? 1 : 0);
        map.put(UIProvider.AccountColumns.SettingsColumns.CONFIRM_ARCHIVE, confirmArchive ? 1 : 0);
        map.put(UIProvider.AccountColumns.SettingsColumns.CONFIRM_SEND, confirmSend ? 1 : 0);
        map.put(UIProvider.AccountColumns.SettingsColumns.DEFAULT_INBOX, defaultInbox);
        map.put(UIProvider.AccountColumns.SettingsColumns.DEFAULT_INBOX_NAME, defaultInboxName);
        map.put(UIProvider.AccountColumns.SettingsColumns.FORCE_REPLY_FROM_DEFAULT,
                forceReplyFromDefault ? 1 : 0);
        map.put(UIProvider.AccountColumns.SettingsColumns.MAX_ATTACHMENT_SIZE, maxAttachmentSize);
        map.put(UIProvider.AccountColumns.SettingsColumns.SWIPE, swipe);
        map.put(UIProvider.AccountColumns.SettingsColumns.PRIORITY_ARROWS_ENABLED,
                priorityArrowsEnabled ? 1 : 0);
        map.put(UIProvider.AccountColumns.SettingsColumns.SETUP_INTENT_URI, setupIntentUri);
        map.put(UIProvider.AccountColumns.SettingsColumns.CONVERSATION_VIEW_MODE,
                conversationViewMode);
        map.put(UIProvider.AccountColumns.SettingsColumns.VEILED_ADDRESS_PATTERN,
                veiledAddressPattern);
        map.put(UIProvider.AccountColumns.SettingsColumns.MOVE_TO_INBOX, moveToInbox);

        return map;
    }

    /**
     * Create a new instance of an Settings object using a JSONObject  instance created previously
     * using {@link #toJSON()}. This returns null if the serialized instance was invalid or does
     * not represent a valid account object.
     *
     * @param json Serialized object
     * @return New settings object or null
     */
    public static Settings newInstance(JSONObject json) {
        if (json == null) {
            return null;
        }
        return new Settings(json);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString((String) getNonNull(signature, sDefault.signature));
        dest.writeInt(getAutoAdvanceSetting());
        dest.writeInt(messageTextSize);
        dest.writeInt(snapHeaders);
        dest.writeInt(replyBehavior);
        dest.writeInt(convListIcon);
        dest.writeInt(convListAttachmentPreviews ? 1 : 0);
        dest.writeInt(confirmDelete ? 1 : 0);
        dest.writeInt(confirmArchive? 1 : 0);
        dest.writeInt(confirmSend? 1 : 0);
        dest.writeString(getNonNull(defaultInbox, sDefault.defaultInbox).toString());
        dest.writeString((String) getNonNull(defaultInboxName, sDefault.defaultInboxName));
        dest.writeInt(forceReplyFromDefault ? 1 : 0);
        dest.writeInt(maxAttachmentSize);
        dest.writeInt(swipe);
        dest.writeInt(priorityArrowsEnabled ? 1 : 0);
        dest.writeString(getNonNull(setupIntentUri, sDefault.setupIntentUri).toString());
        dest.writeInt(conversationViewMode);
        dest.writeString(veiledAddressPattern);
        dest.writeString(getNonNull(moveToInbox, sDefault.moveToInbox).toString());
    }

    /**
     * Returns the URI of the current account's default inbox if available, otherwise
     * returns the empty URI {@link Uri#EMPTY}
     * @param settings a settings object, possibly null.
     * @return a valid default Inbox URI, or {@link Uri#EMPTY} if settings are null or no default
     * is specified.
     */
    public static Uri getDefaultInboxUri(Settings settings) {
        if (settings == null) {
            return sDefault.defaultInbox;
        }
        return (Uri) getNonNull(settings.defaultInbox, sDefault.defaultInbox);
    }

    /**
     * Gets the autoadvance setting for this object, which may have changed since the settings were
     * initially loaded.
     */
    public int getAutoAdvanceSetting() {
        if (mTransientAutoAdvance != null) {
            return mTransientAutoAdvance;
        }

        return mAutoAdvance;
    }

    /**
     * Sets the transient autoadvance setting, which will override the initial autoadvance setting.
     */
    public void setAutoAdvanceSetting(final int autoAdvance) {
        mTransientAutoAdvance = autoAdvance;
    }

    /**
     * @return true if {@link UIProvider.ConversationViewMode#OVERVIEW} mode is set. In the event
     * that the setting is not yet set, fall back to
     * {@link UIProvider.ConversationViewMode#DEFAULT}.
     */
    public boolean isOverviewMode() {
        final int val = (conversationViewMode != UIProvider.ConversationViewMode.UNDEFINED) ?
                conversationViewMode : UIProvider.ConversationViewMode.DEFAULT;
        return (val == UIProvider.ConversationViewMode.OVERVIEW);
    }

    /**
     * Return the swipe setting for the settings provided. It is safe to pass this method
     * a null object. It always returns a valid {@link Swipe} setting.
     * @return the auto advance setting, a constant from {@link Swipe}
     */
    public static int getSwipeSetting(Settings settings) {
        return settings != null ? settings.swipe : sDefault.swipe;
    }

    @SuppressWarnings("hiding")
    public static final Creator<Settings> CREATOR = new Creator<Settings>() {
        @Override
        public Settings createFromParcel(Parcel source) {
            return new Settings(source);
        }

        @Override
        public Settings[] newArray(int size) {
            return new Settings[size];
        }
    };

    /**
     *  Get the maximum size in bytes for attachments.
     */
    public int getMaxAttachmentSize() {
        return maxAttachmentSize <= 0 ? DEFAULT_MAX_ATTACHMENT_SIZE : maxAttachmentSize;
    }

    @Override
    public boolean equals(final Object aThat) {
        LogUtils.d(LOG_TAG, "Settings.equals(%s)", aThat);
        if (this == aThat) {
            return true;
        }
        if ((aThat == null) || (aThat.getClass() != this.getClass())) {
            return false;
        }
        final Settings that = (Settings) aThat;
        final boolean autoAdvanceEquals = mTransientAutoAdvance != null
                ? mTransientAutoAdvance.equals(that.mTransientAutoAdvance)
                : that.mTransientAutoAdvance == null;
        return (TextUtils.equals(signature, that.signature)
                && mAutoAdvance == that.mAutoAdvance
                && autoAdvanceEquals
                && messageTextSize == that.messageTextSize
                && snapHeaders == that.snapHeaders
                && replyBehavior == that.replyBehavior
                && convListIcon == that.convListIcon
                && convListAttachmentPreviews == that.convListAttachmentPreviews
                && confirmDelete == that.confirmDelete
                && confirmArchive == that.confirmArchive
                && confirmSend == that.confirmSend
                && Objects.equal(defaultInbox, that.defaultInbox)
                // Not checking default Inbox name, since is is identical to the URI check above.
                && forceReplyFromDefault == that.forceReplyFromDefault
                && maxAttachmentSize == that.maxAttachmentSize
                && swipe == that.swipe
                && priorityArrowsEnabled == that.priorityArrowsEnabled
                && setupIntentUri == that.setupIntentUri
                && conversationViewMode == that.conversationViewMode
                && TextUtils.equals(veiledAddressPattern, that.veiledAddressPattern))
                && Objects.equal(moveToInbox, that.moveToInbox);
    }

    @Override
    public int hashCode() {
        if (mHashCode == 0) {
            mHashCode = super.hashCode()
                    ^ Objects.hashCode(signature, mAutoAdvance, mTransientAutoAdvance,
                    messageTextSize, snapHeaders, replyBehavior, convListIcon,
                    convListAttachmentPreviews, confirmDelete, confirmArchive, confirmSend,
                    defaultInbox, forceReplyFromDefault, maxAttachmentSize, swipe,
                    priorityArrowsEnabled, setupIntentUri, conversationViewMode,
                    veiledAddressPattern, moveToInbox);
        }
        return mHashCode;
    }
}
