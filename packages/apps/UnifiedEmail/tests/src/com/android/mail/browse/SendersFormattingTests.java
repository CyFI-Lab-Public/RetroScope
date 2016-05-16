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

package com.android.mail.browse;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.SpannableString;

import com.android.mail.providers.ConversationInfo;
import com.android.mail.providers.MessageInfo;

import java.util.ArrayList;

@SmallTest
public class SendersFormattingTests extends AndroidTestCase {

    private static ConversationInfo createConversationInfo(int count) {
        int draftCount = 5;
        String first = "snippet", firstUnread = first, last = first;
        return new ConversationInfo(count, draftCount, first, firstUnread, last);
    }

    public void testMe() {
        // Blank sender == from "me"
        ConversationInfo conv = createConversationInfo(1);
        boolean read = false, starred = false;
        MessageInfo info = new MessageInfo(read, starred, null, -1, null);
        conv.addMessage(info);
        ArrayList<SpannableString> strings = new ArrayList<SpannableString>();
        ArrayList<String> emailDisplays = null;
        SendersView.format(getContext(), conv, "", 100, strings, emailDisplays, emailDisplays,
                null, false);
        assertEquals(1, strings.size());
        assertEquals(strings.get(0).toString(), "me");

        ConversationInfo conv2 = createConversationInfo(1);
        MessageInfo info2 = new MessageInfo(read, starred, "", -1, null);
        strings.clear();
        conv2.addMessage(info2);
        SendersView.format(getContext(), conv, "", 100, strings, emailDisplays, emailDisplays,
                null, false);
        assertEquals(1, strings.size());
        assertEquals(strings.get(0).toString(), "me");

        ConversationInfo conv3 = createConversationInfo(2);
        MessageInfo info3 = new MessageInfo(read, starred, "", -1, null);
        conv3.addMessage(info3);
        MessageInfo info4 = new MessageInfo(read, starred, "", -1, null);
        conv3.addMessage(info4);
        strings.clear();
        SendersView.format(getContext(), conv, "", 100, strings, emailDisplays, emailDisplays,
                null, false);
        assertEquals(1, strings.size());
        assertEquals(strings.get(0).toString(), "me");
    }

    public void testDupes() {
        // Duplicate sender; should only return 1
        ArrayList<SpannableString> strings = new ArrayList<SpannableString>();
        ArrayList<String> emailDisplays = null;
        ConversationInfo conv = createConversationInfo(2);
        boolean read = false, starred = false;
        String sender = "sender@sender.com";
        MessageInfo info = new MessageInfo(read, starred, sender, -1, null);
        conv.addMessage(info);
        MessageInfo info2 = new MessageInfo(read, starred, sender, -1, null);
        conv.addMessage(info2);
        SendersView.format(getContext(), conv, "", 100, strings, emailDisplays, emailDisplays,
                null, false);
        // We actually don't remove the item, we just set it to null, so count
        // just the non-null items.
        int count = 0;
        for (int i = 0; i < strings.size(); i++) {
            if (strings.get(i) != null) {
                count++;
                assertEquals(strings.get(i).toString(), sender);
            }
        }
        assertEquals(1, count);
    }

    public void testSenderNameBadInput() {
        final ConversationInfo conv = createConversationInfo(1);
        final MessageInfo msg = new MessageInfo(false, false, "****^****", 0, null);
        conv.addMessage(msg);

        final byte[] serialized = conv.toBlob();

        ConversationInfo conv2 = ConversationInfo.fromBlob(serialized);
        assertEquals(1, conv2.messageInfos.size());
        assertEquals(msg.sender, conv2.messageInfos.get(0).sender);
    }

    public void testConversationSnippetsBadInput() {
        final String firstSnippet = "*^*";
        final String firstUnreadSnippet = "*^*^*";
        final String lastSnippet = "*^*^*^*";

        final ConversationInfo conv = new ConversationInfo(42, 49, firstSnippet,
                firstUnreadSnippet, lastSnippet);
        final MessageInfo msg = new MessageInfo(false, false, "Foo Bar", 0, null);
        conv.addMessage(msg);

        assertEquals(firstSnippet, conv.firstSnippet);
        assertEquals(firstUnreadSnippet, conv.firstUnreadSnippet);
        assertEquals(lastSnippet, conv.lastSnippet);

        final byte[] serialized = conv.toBlob();

        ConversationInfo conv2 = ConversationInfo.fromBlob(serialized);

        assertEquals(conv.firstSnippet, conv2.firstSnippet);
        assertEquals(conv.firstUnreadSnippet, conv2.firstUnreadSnippet);
        assertEquals(conv.lastSnippet, conv2.lastSnippet);
    }

}
