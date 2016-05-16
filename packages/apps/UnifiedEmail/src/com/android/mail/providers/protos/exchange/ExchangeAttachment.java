/**
 * Copyright (c) 2011, Google Inc.
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
package com.android.mail.providers.protos.exchange;

import android.os.Parcel;

import com.android.mail.providers.Attachment;

public class ExchangeAttachment extends Attachment {
    public String contentId;
    public long messageKey;
    public String location;
    public String encoding;
    public String content; // Not currently used
    public int flags;
    public long accountKey;

    public ExchangeAttachment(Parcel in) {
        super(in);
        contentId = in.readString();
        messageKey = in.readLong();
        location = in.readString();
        encoding = in.readString();
        content = in.readString();
        flags = in.readInt();
        accountKey = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(contentId);
        dest.writeLong(messageKey);
        dest.writeString(location);
        dest.writeString(encoding);
        dest.writeString(content);
        dest.writeInt(flags);
        dest.writeLong(accountKey);
    }

    public static final Creator<ExchangeAttachment> CREATOR = new Creator<ExchangeAttachment>() {
        @Override
        public ExchangeAttachment createFromParcel(Parcel source) {
            return new ExchangeAttachment(source);
        }

        @Override
        public ExchangeAttachment[] newArray(int size) {
            return new ExchangeAttachment[size];
        }
    };
}
