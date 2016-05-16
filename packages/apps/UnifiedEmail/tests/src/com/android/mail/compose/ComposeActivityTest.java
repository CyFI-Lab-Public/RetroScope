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

package com.android.mail.compose;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.Html;
import android.text.TextUtils;
import android.text.util.Rfc822Tokenizer;

import com.android.mail.compose.ComposeActivity;
import com.android.mail.providers.Account;
import com.android.mail.providers.Attachment;
import com.android.mail.providers.MailAppProvider;
import com.android.mail.providers.Message;
import com.android.mail.providers.ReplyFromAccount;
import com.android.mail.providers.UIProvider;
import com.android.mail.utils.AccountUtils;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.MatrixCursorWithCachedColumns;
import com.android.mail.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.Deprecated;
import java.lang.Throwable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

@SmallTest
public class ComposeActivityTest extends ActivityInstrumentationTestCase2<ComposeActivity> {
    // TODO: Remove usages of FromAddressSpinner#initialize and ComposeActivity#initReplyRecipients.
    // The internal state of the activity instance may have the wrong mReplyFromAccount as
    // this is set when handling the initial intent.  Theses tests should
    // instantiate the ComposeActivity with the correct reply all intent

    // This varible shouldn't be used, as it may not match the state of the ComposeActivity
    // TODO: remove usage of this variable
    @Deprecated
    private Account mAccount;

    private static final Account[] EMPTY_ACCOUNT_LIST = new Account[0];

    public ComposeActivityTest() {
        super(ComposeActivity.class);
    }

    private Message getRefMessage(ContentResolver resolver, Uri folderListUri) {
        Cursor foldersCursor = resolver.query(folderListUri,
                UIProvider.FOLDERS_PROJECTION, null, null, null);
        Uri convUri = null;
        if (foldersCursor != null) {
            foldersCursor.moveToFirst();
            convUri = Uri.parse(foldersCursor
                    .getString(UIProvider.FOLDER_CONVERSATION_LIST_URI_COLUMN));
            foldersCursor.close();
        }

        Cursor convCursor = resolver.query(convUri,
                UIProvider.CONVERSATION_PROJECTION, null, null, null);
        Uri messagesUri = null;
        if (convCursor != null) {
            convCursor.moveToFirst();
            messagesUri = Uri.parse(convCursor
                    .getString(UIProvider.CONVERSATION_MESSAGE_LIST_URI_COLUMN));
            convCursor.close();
        }

        Cursor msgCursor = resolver.query(messagesUri,
                UIProvider.MESSAGE_PROJECTION, null, null, null);
        if (msgCursor != null) {
            msgCursor.moveToFirst();
        }
        return new Message(msgCursor);
    }


    private Message getRefMessage(ContentResolver resolver) {
        return getRefMessage(resolver, mAccount.folderListUri);
    }

    public void setAccount(ComposeActivity activity, String accountName) {
        // Get a mock account.
        final Account account = getAccountForName(activity, accountName);
        if (account != null) {
            mAccount = account;
            activity.setAccount(mAccount);
        }
    }

    private Account[] getAccounts(Context context) {
        return AccountUtils.getSyncingAccounts(context);
    }

    private Account getAccountForName(Context context, String accountName) {
        Account[] results = getAccounts(context);
        for (Account account : results) {
            if (account.name.equals(accountName)) {
                return account;
            }
        }
        return null;
    }

    /**
     * Test the cases where: The user's reply-to is one of their custom from's
     * and they are replying all to a message where their custom from was a
     * recipient. TODO: verify web behavior
     */
    public void testRecipientsRefReplyAllCustomFromReplyTo() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account3@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        final String customFrom = "CUSTOMaccount3@mockuiprovider.com";
        refMessage.setFrom("account3@mockuiprovider.com");
        refMessage.setTo("someotheraccount1@mockuiprovider.com, "
                + "someotheraccount2@mockuiprovider.com, someotheraccount3@mockuiprovider.com, "
                + customFrom);
        refMessage.setReplyTo(customFrom);
        activity.mFromSpinner = new FromAddressSpinner(activity);
        ReplyFromAccount a = new ReplyFromAccount(mAccount, mAccount.uri, customFrom,
                customFrom, customFrom, true, true);
        JSONArray array = new JSONArray();
        array.put(a.serialize());
        mAccount.accountFromAddresses = array.toString();
        ReplyFromAccount currentAccount = new ReplyFromAccount(mAccount, mAccount.uri,
                mAccount.name, mAccount.name, customFrom, true, false);
        activity.mFromSpinner.setCurrentAccount(currentAccount);

        activity.mFromSpinner.initialize(ComposeActivity.REPLY_ALL,
                currentAccount.account, EMPTY_ACCOUNT_LIST, null);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY_ALL);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                String toAsString = TextUtils.join(",", to);
                assertEquals(3, to.length);
                assertFalse(toAsString.contains(customFrom));
                assertEquals(0, cc.length);
                assertEquals(0, bcc.length);
            }
        });
    }

    /**
     * Test the cases where: The user sent a message to one of
     * their custom froms and just replied to that message
     */
    public void testRecipientsRefReplyAllOnlyAccount() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account3@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        refMessage.setFrom("account3@mockuiprovider.com");
        refMessage.setTo("account3@mockuiprovider.com");
        final Account account = mAccount;
        activity.mFromSpinner = new FromAddressSpinner(activity);
        ReplyFromAccount currentAccount = new ReplyFromAccount(mAccount, mAccount.uri,
                mAccount.name, mAccount.name, mAccount.name, true, false);
        activity.mFromSpinner.setCurrentAccount(currentAccount);

        activity.mFromSpinner.initialize(ComposeActivity.REPLY_ALL,
                currentAccount.account, EMPTY_ACCOUNT_LIST, null);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY_ALL);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                String toAsString = TextUtils.join(",", to);
                assertEquals(1, to.length);
                assertTrue(toAsString.contains(account.getEmailAddress()));
                assertEquals(0, cc.length);
                assertEquals(0, bcc.length);
            }
        });
    }

    /**
     * Test the cases where: The user sent a message to one of
     * their custom froms and just replied to that message
     */
    public void testRecipientsRefReplyAllOnlyCustomFrom() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account3@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        final String customFrom = "CUSTOMaccount3@mockuiprovider.com";
        refMessage.setFrom("account3@mockuiprovider.com");
        refMessage.setTo(customFrom);
        activity.mFromSpinner = new FromAddressSpinner(activity);
        ReplyFromAccount a = new ReplyFromAccount(mAccount, mAccount.uri, customFrom,
                customFrom, customFrom, true, true);
        JSONArray array = new JSONArray();
        array.put(a.serialize());
        mAccount.accountFromAddresses = array.toString();
        ReplyFromAccount currentAccount = new ReplyFromAccount(mAccount, mAccount.uri,
                mAccount.name, mAccount.name, customFrom, true, false);
        activity.mFromSpinner.setCurrentAccount(currentAccount);

        activity.mFromSpinner.initialize(ComposeActivity.REPLY_ALL,
                currentAccount.account, EMPTY_ACCOUNT_LIST, null);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY_ALL);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                String toAsString = TextUtils.join(",", to);
                assertEquals(1, to.length);
                assertTrue(toAsString.contains(customFrom));
                assertEquals(0, cc.length);
                assertEquals(0, bcc.length);
            }
        });
    }

    public void testReply() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account0@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        final String refMessageFromAccount = refMessage.getFrom();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                assertTrue(to.length == 1);
                assertEquals(refMessageFromAccount,
                        Rfc822Tokenizer.tokenize(to[0])[0].getAddress());
                assertTrue(cc.length == 0);
                assertTrue(bcc.length == 0);
            }
        });
    }

    public void testReplyWithReplyTo() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account1@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        refMessage.setReplyTo("replytofromaccount1@mock.com");
        final String refReplyToAccount = refMessage.getReplyTo();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                assertTrue(to.length == 1);
                assertEquals(refReplyToAccount,
                        Rfc822Tokenizer.tokenize(to[0])[0].getAddress());
                assertTrue(cc.length == 0);
                assertTrue(bcc.length == 0);
            }
        });
    }

    /**
     * Reply to a message you sent yourself to some recipients in the to field.
     */
    public void testReplyToSelf() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account1@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        refMessage.setFrom("Account Test <account1@mockuiprovider.com>");
        refMessage.setTo("test1@gmail.com");
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                assertTrue(to.length == 1);
                String toAsString = TextUtils.join(",", to);
                assertTrue(toAsString.contains("test1@gmail.com"));
                assertTrue(cc.length == 0);
                assertTrue(bcc.length == 0);
            }
        });
    }

    /**
     * Reply-all to a message you sent.
     */
    public void testReplyAllToSelf() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account1@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        refMessage.setFrom("Account Test <account1@mockuiprovider.com>");
        refMessage.setTo("test1@gmail.com, test2@gmail.com");
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY_ALL);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                assertEquals(to.length, 2);
                String toAsString = TextUtils.join(",", to);
                assertTrue(toAsString.contains("test1@gmail.com"));
                assertTrue(toAsString.contains("test2@gmail.com"));
                assertTrue(cc.length == 0);
                assertTrue(bcc.length == 0);
            }
        });
    }

    /**
     * Reply-all to a message you sent with some to and some CC recips.
     */
    public void testReplyAllToSelfWithCc() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account1@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        refMessage.setFrom("Account Test <account1@mockuiprovider.com>");
        refMessage.setTo("test1@gmail.com, test2@gmail.com");
        refMessage.setCc("testcc@gmail.com");
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY_ALL);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                assertEquals(to.length, 2);
                String toAsString = TextUtils.join(",", to);
                assertTrue(toAsString.contains("test1@gmail.com"));
                assertTrue(toAsString.contains("test2@gmail.com"));
                String ccAsString = TextUtils.join(",", cc);
                assertTrue(ccAsString.contains("testcc@gmail.com"));
                assertTrue(cc.length == 1);
                assertTrue(bcc.length == 0);
            }
        });
    }

    public void testReplyAll() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account0@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        final String[] refMessageTo = TextUtils.split(refMessage.getTo(), ",");
        final String refMessageFromAccount = refMessage.getFrom();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY_ALL);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                assertTrue(to.length == 1);
                assertEquals(refMessageFromAccount,
                        Rfc822Tokenizer.tokenize(to[0])[0].getAddress());
                assertEquals(cc.length, refMessageTo.length);
                assertTrue(bcc.length == 0);
            }
        });
    }

    public void testReplyAllWithReplyTo() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account1@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        refMessage.setReplyTo("replytofromaccount1@mock.com");
        final String[] refMessageTo = TextUtils.split(refMessage.getTo(), ",");
        final String refReplyToAccount = refMessage.getReplyTo();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY_ALL);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                assertTrue(to.length == 1);
                assertEquals(refReplyToAccount, Rfc822Tokenizer.tokenize(to[0])[0].getAddress());
                assertEquals(cc.length, refMessageTo.length);
                assertTrue(bcc.length == 0);
            }
        });
    }

    private static Message getRefMessageWithCc(long messageId, boolean hasAttachments) {
        MatrixCursor cursor = new MatrixCursorWithCachedColumns(UIProvider.MESSAGE_PROJECTION);
        final String messageUri = "content://xxx/message/" + messageId;
        Object[] messageValues = new Object[UIProvider.MESSAGE_PROJECTION.length];
        messageValues[UIProvider.MESSAGE_ID_COLUMN] = Long.valueOf(messageId);
        messageValues[UIProvider.MESSAGE_URI_COLUMN] = messageUri;
        messageValues[UIProvider.MESSAGE_SUBJECT_COLUMN] = "Message subject";
        messageValues[UIProvider.MESSAGE_SNIPPET_COLUMN] = "SNIPPET";
        String html = "<html><body><b><i>This is some html!!!</i></b></body></html>";
        messageValues[UIProvider.MESSAGE_BODY_HTML_COLUMN] = html;
        messageValues[UIProvider.MESSAGE_BODY_TEXT_COLUMN] = Html.fromHtml(html);
        messageValues[UIProvider.MESSAGE_HAS_ATTACHMENTS_COLUMN] = hasAttachments ? 1 : 0;
        messageValues[UIProvider.MESSAGE_DATE_RECEIVED_MS_COLUMN] = new Date().getTime();
        messageValues[UIProvider.MESSAGE_ATTACHMENT_LIST_URI_COLUMN] = messageUri
                + "/getAttachments";
        messageValues[UIProvider.MESSAGE_TO_COLUMN] = "account1@mock.com, account2@mock.com";
        messageValues[UIProvider.MESSAGE_FROM_COLUMN] = "fromaccount1@mock.com";
        messageValues[UIProvider.MESSAGE_CC_COLUMN] = "accountcc1@mock.com, accountcc2@mock.com";
        cursor.addRow(messageValues);
        cursor.moveToFirst();
        return new Message(cursor);
    }

    public void testReplyAllWithCc() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account1@mockuiprovider.com");
        final Message refMessage = getRefMessageWithCc(0, false);
        final String[] refMessageTo = TextUtils.split(refMessage.getTo(), ",");
        final String[] refMessageCc = TextUtils.split(refMessage.getCc(), ",");
        final String refMessageFromAccount = refMessage.getFrom();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY_ALL);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                assertTrue(to.length == 1);
                assertEquals(refMessageFromAccount,
                        Rfc822Tokenizer.tokenize(to[0])[0].getAddress());
                assertEquals(cc.length, refMessageTo.length + refMessageCc.length);
                HashSet<String> ccMap = new HashSet<String>();
                for (String recip : cc) {
                    ccMap.add(Rfc822Tokenizer.tokenize(recip.trim())[0].getAddress());
                }
                for (String toRecip : refMessageTo) {
                    assertTrue(ccMap.contains(toRecip.trim()));
                }
                for (String ccRecip : refMessageCc) {
                    assertTrue(ccMap.contains(ccRecip.trim()));
                }
                assertTrue(bcc.length == 0);
            }
        });
    }

    public void testForward() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account0@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.FORWARD);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                assertEquals(to.length, 0);
                assertEquals(cc.length, 0);
                assertEquals(bcc.length, 0);
            }
        });
    }

    public void testCompose() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account0@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.COMPOSE);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                assertEquals(to.length, 0);
                assertEquals(cc.length, 0);
                assertEquals(bcc.length, 0);
            }
        });
    }

    /**
     * Test the cases where: The user is replying to a message they sent
     */
    public void testRecipientsRefMessageReplyToSelf() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account0@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        refMessage.setFrom("account0@mockuiprovider.com");
        refMessage.setTo("someotheraccount@mockuiprovider.com");

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                assertEquals(to.length, 1);
                assertTrue(to[0].contains(refMessage.getTo()));
                assertEquals(cc.length, 0);
                assertEquals(bcc.length, 0);
            }
        });
    }

    /**
     * Test the cases where:
     * The user is replying to a message sent from one of their custom froms
     */
    public void testRecipientsRefMessageReplyToCustomFrom() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account1@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        refMessage.setFrom("CUSTOMaccount1@mockuiprovider.com");
        refMessage.setTo("someotheraccount@mockuiprovider.com");
        activity.mFromSpinner = new FromAddressSpinner(activity);
        ReplyFromAccount a = new ReplyFromAccount(mAccount, mAccount.uri, refMessage.getFrom(),
                refMessage.getFrom(), refMessage.getFrom(), true, true);
        JSONArray array = new JSONArray();
        array.put(a.serialize());
        mAccount.accountFromAddresses = array.toString();
        ReplyFromAccount currentAccount = new ReplyFromAccount(mAccount, mAccount.uri,
                mAccount.name, mAccount.name, mAccount.name, true, false);
        activity.mFromSpinner.setCurrentAccount(currentAccount);
        activity.mFromSpinner.initialize(ComposeActivity.REPLY, currentAccount.account,
                EMPTY_ACCOUNT_LIST, null);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                assertEquals(to.length, 1);
                assertTrue(to[0].contains(refMessage.getTo()));
                assertEquals(cc.length, 0);
                assertEquals(bcc.length, 0);
            }
        });
    }

    /**
     * Test the cases where:
     * The user is replying to a message sent from one of their custom froms
     */
    public void testRecipientsRefMessageReplyAllCustomFrom() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account1@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        final String customFrom = "CUSTOMaccount1@mockuiprovider.com";
        refMessage.setFrom("senderaccount@mockuiprovider.com");
        refMessage.setTo("someotheraccount@mockuiprovider.com, "
                + "someotheraccount2@mockuiprovider.com, someotheraccount4@mockuiprovider.com, "
                + customFrom);
        activity.mFromSpinner = new FromAddressSpinner(activity);
        ReplyFromAccount a = new ReplyFromAccount(mAccount, mAccount.uri, customFrom,
                customFrom, customFrom, true, true);
        JSONArray array = new JSONArray();
        array.put(a.serialize());
        mAccount.accountFromAddresses = array.toString();
        ReplyFromAccount currentAccount = new ReplyFromAccount(mAccount, mAccount.uri,
                mAccount.name, mAccount.name, mAccount.name, true, false);
        activity.mFromSpinner.setCurrentAccount(currentAccount);
        activity.mFromSpinner.initialize(ComposeActivity.REPLY_ALL,
                currentAccount.account, EMPTY_ACCOUNT_LIST, null);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY_ALL);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                String toAsString = TextUtils.join(",", to);
                String ccAsString = TextUtils.join(",", cc);
                String bccAsString = TextUtils.join(",", bcc);
                assertEquals(to.length, 1);
                assertFalse(toAsString.contains(customFrom));
                assertFalse(ccAsString.contains(customFrom));
                assertFalse(bccAsString.contains(customFrom));
            }
        });
    }

    /**
     * Test the cases where:
     * The user is replying to a message sent from one of their custom froms
     */
    public void testRecipientsRefMessageReplyAllCustomFromThisAccount() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account1@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        final String customFrom = "CUSTOMaccount1@mockuiprovider.com";
        refMessage.setFrom("account1@mockuiprovider.com");
        refMessage.setTo("someotheraccount@mockuiprovider.com, "
                + "someotheraccount2@mockuiprovider.com, someotheraccount4@mockuiprovider.com, "
                + customFrom);
        activity.mFromSpinner = new FromAddressSpinner(activity);
        ReplyFromAccount a = new ReplyFromAccount(mAccount, mAccount.uri, customFrom,
                customFrom, customFrom, true, true);
        JSONArray array = new JSONArray();
        array.put(a.serialize());
        mAccount.accountFromAddresses = array.toString();
        ReplyFromAccount currentAccount = new ReplyFromAccount(mAccount, mAccount.uri,
                mAccount.name, mAccount.name, mAccount.name, true, false);
        activity.mFromSpinner.setCurrentAccount(currentAccount);
        activity.mFromSpinner.initialize(ComposeActivity.REPLY_ALL,
                currentAccount.account, EMPTY_ACCOUNT_LIST, null);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY_ALL);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                String toAsString = TextUtils.join(",", to);
                String ccAsString = TextUtils.join(",", cc);
                String bccAsString = TextUtils.join(",", bcc);
                // Should have the same count as the original message.
                assertEquals(to.length, 3);
                assertFalse(toAsString.contains(customFrom));
                assertFalse(ccAsString.contains(customFrom));
                assertFalse(bccAsString.contains(customFrom));
            }
        });
    }

    // Test replying to a message in the first account in the list, and verify that
    // the Compose Activity's from account for the reply is correct
    public void testReplySendingAccount0() throws Throwable {
        final Context context = getInstrumentation().getContext();
        // Get the test account
        final Account currentAccount = getAccountForName(context, "account0@mockuiprovider.com");

        // Get the message to be replied to
        final Message refMessage =
                getRefMessage(context.getContentResolver(), currentAccount.folderListUri);

        // Create the reply intent
        final Intent replyIntent =
                ComposeActivity.updateActionIntent(currentAccount, refMessage.uri,
                        ComposeActivity.REPLY, new Intent());

        setActivityIntent(replyIntent);

        final ComposeActivity activity = getActivity();
        final String refMessageFromAccount = refMessage.getFrom();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                Account fromAccount = activity.getFromAccount();
                assertEquals(1, to.length);
                assertEquals(refMessageFromAccount,
                        Rfc822Tokenizer.tokenize(to[0])[0].getAddress());
                assertEquals(0, cc.length);
                assertEquals(0, bcc.length);
                assertEquals("account0@mockuiprovider.com", fromAccount.getEmailAddress());
            }
        });
    }

    // Test replying to a message in the third account in the list, and verify that
    // the Compose Activity's from account for the reply is correct
    public void testReplySendingAccount1() throws Throwable {
        final Context context = getInstrumentation().getContext();
        // Get the test account
        final Account currentAccount = getAccountForName(context, "account2@mockuiprovider.com");

        // Get the message to be replied to
        final Message refMessage =
                getRefMessage(context.getContentResolver(), currentAccount.folderListUri);

        // Create the reply intent
        final Intent replyIntent =
                ComposeActivity.updateActionIntent(currentAccount, refMessage.uri,
                        ComposeActivity.REPLY, new Intent());

        setActivityIntent(replyIntent);

        final ComposeActivity activity = getActivity();
        Account fromAccount = activity.getFromAccount();

        final String refMessageFromAccount = refMessage.getFrom();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                Account fromAccount = activity.getFromAccount();
                assertEquals(1, to.length);
                assertEquals(refMessageFromAccount,
                        Rfc822Tokenizer.tokenize(to[0])[0].getAddress());
                assertEquals(0, cc.length);
                assertEquals(0, bcc.length);
                assertEquals("account2@mockuiprovider.com", fromAccount.getEmailAddress());
            }
        });
    }

    // Test a mailto VIEW Intent, with an account specified in JSON format
    public void testMailToAccountJSON() throws Throwable {
        final Context context = getInstrumentation().getContext();
        // Get the test account
        final Account currentAccount = getAccountForName(context, "account2@mockuiprovider.com");

        // Create the mailto intent
        final Intent mailtoIntent =
                new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:test@localhost.com"));
        mailtoIntent.putExtra(Utils.EXTRA_ACCOUNT, currentAccount.serialize());

        setActivityIntent(mailtoIntent);

        final ComposeActivity activity = getActivity();
        Account fromAccount = activity.getFromAccount();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                Account fromAccount = activity.getFromAccount();
                assertEquals( 1, to.length);
                assertEquals("test@localhost.com",
                        Rfc822Tokenizer.tokenize(to[0])[0].getAddress());
                assertEquals(0, cc.length);
                assertEquals(0, bcc.length);
                assertEquals("account2@mockuiprovider.com", fromAccount.getEmailAddress());
            }
        });
    }

    // Test a COMPOSE Intent, with an account specified in parcel format
    public void testMailToAccount() throws Throwable {
        final Context context = getInstrumentation().getContext();
        // Get the test account
        final Account currentAccount = getAccountForName(context, "account2@mockuiprovider.com");

        // Create the mailto intent
        Intent intent = new Intent(context, ComposeActivity.class);
        intent.putExtra(ComposeActivity.EXTRA_FROM_EMAIL_TASK, true);
        intent.putExtra(ComposeActivity.EXTRA_ACTION, ComposeActivity.COMPOSE);
        intent.putExtra(Utils.EXTRA_ACCOUNT, currentAccount);
        intent.putExtra(ComposeActivity.EXTRA_TO, "test@localhost.com");

        setActivityIntent(intent);

        final ComposeActivity activity = getActivity();
        Account fromAccount = activity.getFromAccount();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                Account fromAccount = activity.getFromAccount();
                assertEquals( 1, to.length);
                assertEquals("test@localhost.com",
                        Rfc822Tokenizer.tokenize(to[0])[0].getAddress());
                assertEquals(0, cc.length);
                assertEquals(0, bcc.length);
                assertEquals("account2@mockuiprovider.com", fromAccount.getEmailAddress());
            }
        });
    }

    // Test a mailto VIEW Intent, with no account specified.  The fromAccount should default to the
    // last sent account.
    public void testMailToAccountWithLastSentAccount() throws Throwable {
        final Context context = getInstrumentation().getContext();

        // Set the last sent account to account0
        final Account lastSentAccount = getAccountForName(context, "account1@mockuiprovider.com");
        MailAppProvider appProvider = MailAppProvider.getInstance();
        appProvider.setLastSentFromAccount(lastSentAccount.uri.toString());

        // Create the mailto intent
        final Intent mailtoIntent =
                new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:test@localhost.com"));

        setActivityIntent(mailtoIntent);

        final ComposeActivity activity = getActivity();
        Account fromAccount = activity.getFromAccount();

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                Account fromAccount = activity.getFromAccount();
                assertEquals( 1, to.length);
                assertEquals("test@localhost.com",
                        Rfc822Tokenizer.tokenize(to[0])[0].getAddress());
                assertEquals(0, cc.length);
                assertEquals(0, bcc.length);
                assertEquals("account1@mockuiprovider.com", fromAccount.getEmailAddress());
            }
        });
    }

    private static String createAttachmentsJson() {
        Attachment attachment1 = new Attachment();
        attachment1.contentUri = Uri.parse("www.google.com");
        attachment1.setContentType("img/jpeg");
        attachment1.setName("attachment1");
        Attachment attachment2 = new Attachment();
        attachment2.contentUri = Uri.parse("www.google.com");
        attachment2.setContentType("img/jpeg");
        attachment2.setName("attachment2");
        JSONArray attachments = new JSONArray();
        try {
            attachments.put(attachment1.toJSON());
            attachments.put(attachment2.toJSON());
        } catch (JSONException e) {
            assertTrue(false);
        }
        return attachments.toString();
    }

    // First test: switch reply to reply all to fwd, 1 to recipient, 1 cc recipient.
    public void testChangeModes0() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account0@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        refMessage.setFrom("fromaccount@mockuiprovider.com");
        refMessage.setTo("account0@mockuiprovider.com");
        refMessage.setCc("ccaccount@mockuiprovider.com");
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.mRefMessage = refMessage;
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                assertEquals(1, to.length);
                assertTrue(to[0].contains(refMessage.getFrom()));
                assertEquals(cc.length, 0);
                assertEquals(bcc.length, 0);
                activity.onNavigationItemSelected(1, ComposeActivity.REPLY_ALL);
                assertEquals(activity.getToAddresses().length, 1);
                assertTrue(activity.getToAddresses()[0].contains(refMessage.getFrom()));
                assertEquals(activity.getCcAddresses().length, 1);
                assertTrue(activity.getCcAddresses()[0].contains(refMessage.getCc()));
                assertEquals(activity.getBccAddresses().length, 0);
                activity.onNavigationItemSelected(2, ComposeActivity.FORWARD);
                assertEquals(activity.getToAddresses().length, 0);
                assertEquals(activity.getCcAddresses().length, 0);
                assertEquals(activity.getBccAddresses().length, 0);
            }
        });
    }

    // Switch reply to reply all to fwd, 2 to recipients, 1 cc recipient.
    public void testChangeModes1() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account0@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        refMessage.setFrom("fromaccount@mockuiprovider.com");
        refMessage.setTo("account0@mockuiprovider.com, toaccount0@mockuiprovider.com");
        refMessage.setCc("ccaccount@mockuiprovider.com");
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.mRefMessage = refMessage;
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                assertEquals(to.length, 1);
                assertTrue(to[0].contains(refMessage.getFrom()));
                assertEquals(cc.length, 0);
                assertEquals(bcc.length, 0);
                activity.onNavigationItemSelected(1, ComposeActivity.REPLY_ALL);
                assertEquals(activity.getToAddresses().length, 1);
                assertTrue(activity.getToAddresses()[0].contains(refMessage.getFrom()));
                assertEquals(activity.getCcAddresses().length, 2);
                assertTrue(activity.getCcAddresses()[0].contains(refMessage.getCc())
                        || activity.getCcAddresses()[1].contains(refMessage.getCc()));
                assertTrue(activity.getCcAddresses()[0].contains("toaccount0@mockuiprovider.com")
                        || activity.getCcAddresses()[1]
                        .contains("toaccount0@mockuiprovider.com"));
                assertEquals(activity.getBccAddresses().length, 0);
                activity.onNavigationItemSelected(2, ComposeActivity.FORWARD);
                assertEquals(activity.getToAddresses().length, 0);
                assertEquals(activity.getCcAddresses().length, 0);
                assertEquals(activity.getBccAddresses().length, 0);
            }
        });
    }

    // Switch reply to reply all to fwd, 2 to recipients, 2 cc recipients.
    public void testChangeModes2() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account0@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        refMessage.setFrom("fromaccount@mockuiprovider.com");
        refMessage.setTo("account0@mockuiprovider.com, toaccount0@mockuiprovider.com");
        refMessage.setCc("ccaccount@mockuiprovider.com, ccaccount2@mockuiprovider.com");
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.mRefMessage = refMessage;
                activity.initReplyRecipients(refMessage, ComposeActivity.REPLY);
                String[] to = activity.getToAddresses();
                String[] cc = activity.getCcAddresses();
                String[] bcc = activity.getBccAddresses();
                assertEquals(to.length, 1);
                assertTrue(to[0].contains(refMessage.getFrom()));
                assertEquals(cc.length, 0);
                assertEquals(bcc.length, 0);
                activity.onNavigationItemSelected(1, ComposeActivity.REPLY_ALL);
                assertEquals(activity.getToAddresses().length, 1);
                assertTrue(activity.getToAddresses()[0].contains(refMessage.getFrom()));
                assertEquals(activity.getCcAddresses().length, 3);
                assertTrue(activity.getCcAddresses()[0].contains("ccaccount@mockuiprovider.com")
                        || activity.getCcAddresses()[1].contains("ccaccount@mockuiprovider.com")
                        || activity.getCcAddresses()[2].contains("ccaccount@mockuiprovider.com"));
                assertTrue(activity.getCcAddresses()[0].contains("ccaccount2@mockuiprovider.com")
                        || activity.getCcAddresses()[1].contains("ccaccount2@mockuiprovider.com")
                        || activity.getCcAddresses()[2].contains("ccaccount2@mockuiprovider.com"));
                assertTrue(activity.getCcAddresses()[0].contains("toaccount0@mockuiprovider.com")
                        || activity.getCcAddresses()[1].contains("toaccount0@mockuiprovider.com")
                        || activity.getCcAddresses()[2].contains("toaccount0@mockuiprovider.com"));
                assertTrue(activity.getCcAddresses()[0].contains("toaccount0@mockuiprovider.com")
                        || activity.getCcAddresses()[1]
                        .contains("toaccount0@mockuiprovider.com")
                        || activity.getCcAddresses()[2]
                        .contains("toaccount0@mockuiprovider.com"));
                assertEquals(activity.getBccAddresses().length, 0);
                activity.onNavigationItemSelected(2, ComposeActivity.FORWARD);
                assertEquals(activity.getToAddresses().length, 0);
                assertEquals(activity.getCcAddresses().length, 0);
                assertEquals(activity.getBccAddresses().length, 0);
            }
        });
    }

    // Switch reply to reply all to fwd, 2 attachments.
    public void testChangeModes3() throws Throwable {
        final ComposeActivity activity = getActivity();
        setAccount(activity, "account0@mockuiprovider.com");
        final Message refMessage = getRefMessage(activity.getContentResolver());
        refMessage.hasAttachments = true;
        refMessage.attachmentsJson = createAttachmentsJson();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.mRefMessage = refMessage;
                activity.initAttachments(refMessage);
                assertEquals(activity.getAttachments().size(), 2);
                activity.onNavigationItemSelected(1, ComposeActivity.REPLY);
                assertEquals(activity.getAttachments().size(), 0);
                activity.onNavigationItemSelected(1, ComposeActivity.REPLY_ALL);
                assertEquals(activity.getAttachments().size(), 0);
                activity.onNavigationItemSelected(2, ComposeActivity.FORWARD);
                assertEquals(activity.getAttachments().size(), 2);
            }
        });
    }
}
