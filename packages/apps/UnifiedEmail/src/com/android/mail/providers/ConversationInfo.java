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

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Objects;

import java.util.ArrayList;

public class ConversationInfo implements Parcelable {

    final public ArrayList<MessageInfo> messageInfos;
    public int messageCount;
    public int draftCount;
    public String firstSnippet;
    public String firstUnreadSnippet;
    public String lastSnippet;

    public ConversationInfo() {
        messageInfos = new ArrayList<MessageInfo>();
    }

    /**
     * Alternate constructor that allows clients to specify the intended number of messages to
     * reduce garbage from resizing.
     */
    public ConversationInfo(int count) {
        messageInfos = new ArrayList<MessageInfo>(count);
    }

    public ConversationInfo(int count, int draft, String first, String firstUnread, String last) {
        messageInfos = new ArrayList<MessageInfo>(count);
        set(count, draft, first, firstUnread, last);
    }

    private ConversationInfo(Parcel in) {
        messageCount = in.readInt();
        draftCount = in.readInt();
        firstSnippet = in.readString();
        firstUnreadSnippet = in.readString();
        lastSnippet = in.readString();
        messageInfos = in.createTypedArrayList(MessageInfo.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(messageCount);
        dest.writeInt(draftCount);
        dest.writeString(firstSnippet);
        dest.writeString(firstUnreadSnippet);
        dest.writeString(lastSnippet);
        dest.writeTypedList(messageInfos);
    }

    public static ConversationInfo fromBlob(byte[] blob) {
        if (blob == null) {
            return null;
        }
        final Parcel p = Parcel.obtain();
        p.unmarshall(blob, 0, blob.length);
        p.setDataPosition(0);
        final ConversationInfo result = CREATOR.createFromParcel(p);
        p.recycle();
        return result;
    }

    public byte[] toBlob() {
        final Parcel p = Parcel.obtain();
        writeToParcel(p, 0);
        final byte[] result = p.marshall();
        p.recycle();
        return result;
    }

    public void set(int count, int draft, String first, String firstUnread, String last) {
        messageInfos.clear();
        messageCount = count;
        draftCount = draft;
        firstSnippet = first;
        firstUnreadSnippet = firstUnread;
        lastSnippet = last;
    }

    public void reset() {
        messageInfos.clear();
        messageCount = 0;
        draftCount = 0;
        firstSnippet = null;
        firstUnreadSnippet = null;
        lastSnippet = null;
    }

    public void addMessage(MessageInfo info) {
        messageInfos.add(info);
    }

    public boolean markRead(boolean read) {
        boolean changed = false;
        for (MessageInfo msg : messageInfos) {
            changed |= msg.markRead(read);
        }
        if (read) {
            firstSnippet = lastSnippet;
        } else {
            firstSnippet = firstUnreadSnippet;
        }
        return changed;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(messageCount, draftCount, messageInfos, firstSnippet,
                lastSnippet, firstUnreadSnippet);
    }

    public static final Creator<ConversationInfo> CREATOR = new Creator<ConversationInfo>() {

        @Override
        public ConversationInfo createFromParcel(Parcel source) {
            return new ConversationInfo(source);
        }

        @Override
        public ConversationInfo[] newArray(int size) {
            return new ConversationInfo[size];
        }

    };

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[ConversationInfo object: messageCount = ");
        builder.append(messageCount);
        builder.append(", draftCount = ");
        builder.append(draftCount);
        builder.append(", firstSnippet= ");
        builder.append(firstSnippet);
        builder.append(", firstUnreadSnippet = ");
        builder.append(firstUnreadSnippet);
        builder.append(", messageInfos = ");
        builder.append(messageInfos.toString());
        builder.append("]");
        return builder.toString();
    }
}
