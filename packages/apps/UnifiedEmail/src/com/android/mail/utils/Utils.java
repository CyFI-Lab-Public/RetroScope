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

package com.android.mail.utils;

import com.google.android.mail.common.html.parser.HtmlDocument;
import com.google.android.mail.common.html.parser.HtmlParser;
import com.google.android.mail.common.html.parser.HtmlTree;
import com.google.android.mail.common.html.parser.HtmlTreeBuilder;
import com.google.common.collect.Maps;

import android.app.Fragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Browser;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.android.mail.R;
import com.android.mail.browse.ConversationCursor;
import com.android.mail.compose.ComposeActivity;
import com.android.mail.perf.SimpleTimer;
import com.android.mail.providers.Account;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.providers.UIProvider;
import com.android.mail.providers.UIProvider.EditSettingsExtras;
import com.android.mail.ui.FeedbackEnabledActivity;
import com.android.mail.ui.ViewMode;

import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

public class Utils {
    /**
     * longest extension we recognize is 4 characters (e.g. "html", "docx")
     */
    private static final int FILE_EXTENSION_MAX_CHARS = 4;
    private static final Map<Integer, Integer> sPriorityToLength = Maps.newHashMap();
    public static final String SENDER_LIST_TOKEN_ELIDED = "e";
    public static final String SENDER_LIST_TOKEN_NUM_MESSAGES = "n";
    public static final String SENDER_LIST_TOKEN_NUM_DRAFTS = "d";
    public static final String SENDER_LIST_TOKEN_LITERAL = "l";
    public static final String SENDER_LIST_TOKEN_SENDING = "s";
    public static final String SENDER_LIST_TOKEN_SEND_FAILED = "f";
    public static final Character SENDER_LIST_SEPARATOR = '\n';
    public static final SimpleStringSplitter sSenderListSplitter = new SimpleStringSplitter(
            SENDER_LIST_SEPARATOR);
    public static String[] sSenderFragments = new String[8];

    public static final String EXTRA_ACCOUNT = "account";
    public static final String EXTRA_ACCOUNT_URI = "accountUri";
    public static final String EXTRA_FOLDER_URI = "folderUri";
    public static final String EXTRA_FOLDER = "folder";
    public static final String EXTRA_COMPOSE_URI = "composeUri";
    public static final String EXTRA_CONVERSATION = "conversationUri";
    public static final String EXTRA_FROM_NOTIFICATION = "notification";

    private static final String MAILTO_SCHEME = "mailto";

    /** Extra tag for debugging the blank fragment problem. */
    public static final String VIEW_DEBUGGING_TAG = "MailBlankFragment";

    /*
     * Notifies that changes happened. Certain UI components, e.g., widgets, can
     * register for this {@link Intent} and update accordingly. However, this
     * can be very broad and is NOT the preferred way of getting notification.
     */
    // TODO: UI Provider has this notification URI?
    public static final String ACTION_NOTIFY_DATASET_CHANGED =
            "com.android.mail.ACTION_NOTIFY_DATASET_CHANGED";

    /** Parameter keys for context-aware help. */
    private static final String SMART_HELP_LINK_PARAMETER_NAME = "p";

    private static final String SMART_LINK_APP_VERSION = "version";
    private static int sVersionCode = -1;

    private static final int SCALED_SCREENSHOT_MAX_HEIGHT_WIDTH = 600;

    private static final String APP_VERSION_QUERY_PARAMETER = "appVersion";
    private static final String FOLDER_URI_QUERY_PARAMETER = "folderUri";

    private static final String LOG_TAG = LogTag.getLogTag();

    public static final boolean ENABLE_CONV_LOAD_TIMER = false;
    public static final SimpleTimer sConvLoadTimer =
            new SimpleTimer(ENABLE_CONV_LOAD_TIMER).withSessionName("ConvLoadTimer");

    private static final int[] STYLE_ATTR = new int[] {android.R.attr.background};

    public static boolean isRunningJellybeanOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean isRunningKitkatOrLater() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    /**
     * Sets WebView in a restricted mode suitable for email use.
     *
     * @param webView The WebView to restrict
     */
    public static void restrictWebView(WebView webView) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setJavaScriptEnabled(false);
        webSettings.setSupportZoom(false);
    }

    /**
     * Format a plural string.
     *
     * @param resource The identity of the resource, which must be a R.plurals
     * @param count The number of items.
     */
    public static String formatPlural(Context context, int resource, int count) {
        final CharSequence formatString = context.getResources().getQuantityText(resource, count);
        return String.format(formatString.toString(), count);
    }

    /**
     * @return an ellipsized String that's at most maxCharacters long. If the
     *         text passed is longer, it will be abbreviated. If it contains a
     *         suffix, the ellipses will be inserted in the middle and the
     *         suffix will be preserved.
     */
    public static String ellipsize(String text, int maxCharacters) {
        int length = text.length();
        if (length < maxCharacters)
            return text;

        int realMax = Math.min(maxCharacters, length);
        // Preserve the suffix if any
        int index = text.lastIndexOf(".");
        String extension = "\u2026"; // "...";
        if (index >= 0) {
            // Limit the suffix to dot + four characters
            if (length - index <= FILE_EXTENSION_MAX_CHARS + 1) {
                extension = extension + text.substring(index + 1);
            }
        }
        realMax -= extension.length();
        if (realMax < 0)
            realMax = 0;
        return text.substring(0, realMax) + extension;
    }

    /**
     * Ensures that the given string starts and ends with the double quote
     * character. The string is not modified in any way except to add the double
     * quote character to start and end if it's not already there. sample ->
     * "sample" "sample" -> "sample" ""sample"" -> "sample"
     * "sample"" -> "sample" sa"mp"le -> "sa"mp"le" "sa"mp"le" -> "sa"mp"le"
     * (empty string) -> "" " -> ""
     */
    public static String ensureQuotedString(String s) {
        if (s == null) {
            return null;
        }
        if (!s.matches("^\".*\"$")) {
            return "\"" + s + "\"";
        } else {
            return s;
        }
    }

    // TODO: Move this to the UI Provider.
    private static CharacterStyle sUnreadStyleSpan = null;
    private static CharacterStyle sReadStyleSpan;
    private static CharacterStyle sDraftsStyleSpan;
    private static CharSequence sMeString;
    private static CharSequence sDraftSingularString;
    private static CharSequence sDraftPluralString;
    private static CharSequence sSendingString;
    private static CharSequence sSendFailedString;

    private static int sMaxUnreadCount = -1;
    private static final CharacterStyle ACTION_BAR_UNREAD_STYLE = new StyleSpan(Typeface.BOLD);
    private static String sUnreadText;
    private static int sDefaultFolderBackgroundColor = -1;
    private static int sUseFolderListFragmentTransition = -1;

    public static void getStyledSenderSnippet(Context context, String senderInstructions,
            SpannableStringBuilder senderBuilder, SpannableStringBuilder statusBuilder,
            int maxChars, boolean forceAllUnread, boolean forceAllRead, boolean allowDraft) {
        Resources res = context.getResources();
        if (sUnreadStyleSpan == null) {
            sUnreadStyleSpan = new StyleSpan(Typeface.BOLD);
            sReadStyleSpan = new StyleSpan(Typeface.NORMAL);
            sDraftsStyleSpan = new ForegroundColorSpan(res.getColor(R.color.drafts));

            sMeString = context.getText(R.string.me_subject_pronun);
            sDraftSingularString = res.getQuantityText(R.plurals.draft, 1);
            sDraftPluralString = res.getQuantityText(R.plurals.draft, 2);
            SpannableString sendingString = new SpannableString(context.getText(R.string.sending));
            sendingString.setSpan(CharacterStyle.wrap(sDraftsStyleSpan), 0, sendingString.length(),
                    0);
            sSendingString = sendingString;
            sSendFailedString = context.getText(R.string.send_failed);
        }

        getSenderSnippet(senderInstructions, senderBuilder, statusBuilder, maxChars,
                sUnreadStyleSpan, sReadStyleSpan, sDraftsStyleSpan, sMeString,
                sDraftSingularString, sDraftPluralString, sSendingString, sSendFailedString,
                forceAllUnread, forceAllRead, allowDraft);
    }

    /**
     * Uses sender instructions to build a formatted string.
     * <p>
     * Sender list instructions contain compact information about the sender
     * list. Most work that can be done without knowing how much room will be
     * availble for the sender list is done when creating the instructions.
     * <p>
     * The instructions string consists of tokens separated by
     * SENDER_LIST_SEPARATOR. Here are the tokens, one per line:
     * <ul>
     * <li><tt>n</tt></li>
     * <li><em>int</em>, the number of non-draft messages in the conversation</li>
     * <li><tt>d</tt</li>
     * <li><em>int</em>, the number of drafts in the conversation</li>
     * <li><tt>l</tt></li>
     * <li><em>literal html to be included in the output</em></li>
     * <li><tt>s</tt> indicates that the message is sending (in the outbox
     * without errors)</li>
     * <li><tt>f</tt> indicates that the message failed to send (in the outbox
     * with errors)</li>
     * <li><em>for each message</em>
     * <ul>
     * <li><em>int</em>, 0 for read, 1 for unread</li>
     * <li><em>int</em>, the priority of the message. Zero is the most important
     * </li>
     * <li><em>text</em>, the sender text or blank for messages from 'me'</li>
     * </ul>
     * </li>
     * <li><tt>e</tt> to indicate that one or more messages have been elided</li>
     * <p>
     * The instructions indicate how many messages and drafts are in the
     * conversation and then describe the most important messages in order,
     * indicating the priority of each message and whether the message is
     * unread.
     *
     * @param instructions instructions as described above
     * @param senderBuilder the SpannableStringBuilder to append to for sender
     *            information
     * @param statusBuilder the SpannableStringBuilder to append to for status
     * @param maxChars the number of characters available to display the text
     * @param unreadStyle the CharacterStyle for unread messages, or null
     * @param draftsStyle the CharacterStyle for draft messages, or null
     * @param sendingString the string to use when there are messages scheduled
     *            to be sent
     * @param sendFailedString the string to use when there are messages that
     *            mailed to send
     * @param meString the string to use for messages sent by this user
     * @param draftString the string to use for "Draft"
     * @param draftPluralString the string to use for "Drafts"
     */
    public static synchronized void getSenderSnippet(String instructions,
            SpannableStringBuilder senderBuilder, SpannableStringBuilder statusBuilder,
            int maxChars, CharacterStyle unreadStyle, CharacterStyle readStyle,
            CharacterStyle draftsStyle, CharSequence meString, CharSequence draftString,
            CharSequence draftPluralString, CharSequence sendingString,
            CharSequence sendFailedString, boolean forceAllUnread, boolean forceAllRead,
            boolean allowDraft) {
        assert !(forceAllUnread && forceAllRead);
        boolean unreadStatusIsForced = forceAllUnread || forceAllRead;
        boolean forcedUnreadStatus = forceAllUnread;

        // Measure each fragment. It's ok to iterate over the entire set of
        // fragments because it is
        // never a long list, even if there are many senders.
        final Map<Integer, Integer> priorityToLength = sPriorityToLength;
        priorityToLength.clear();

        int maxFoundPriority = Integer.MIN_VALUE;
        int numMessages = 0;
        int numDrafts = 0;
        CharSequence draftsFragment = "";
        CharSequence sendingFragment = "";
        CharSequence sendFailedFragment = "";

        sSenderListSplitter.setString(instructions);
        int numFragments = 0;
        String[] fragments = sSenderFragments;
        int currentSize = fragments.length;
        while (sSenderListSplitter.hasNext()) {
            fragments[numFragments++] = sSenderListSplitter.next();
            if (numFragments == currentSize) {
                sSenderFragments = new String[2 * currentSize];
                System.arraycopy(fragments, 0, sSenderFragments, 0, currentSize);
                currentSize *= 2;
                fragments = sSenderFragments;
            }
        }

        for (int i = 0; i < numFragments;) {
            String fragment0 = fragments[i++];
            if ("".equals(fragment0)) {
                // This should be the final fragment.
            } else if (SENDER_LIST_TOKEN_ELIDED.equals(fragment0)) {
                // ignore
            } else if (SENDER_LIST_TOKEN_NUM_MESSAGES.equals(fragment0)) {
                numMessages = Integer.valueOf(fragments[i++]);
            } else if (SENDER_LIST_TOKEN_NUM_DRAFTS.equals(fragment0)) {
                String numDraftsString = fragments[i++];
                numDrafts = Integer.parseInt(numDraftsString);
                draftsFragment = numDrafts == 1 ? draftString : draftPluralString + " ("
                        + numDraftsString + ")";
            } else if (SENDER_LIST_TOKEN_LITERAL.equals(fragment0)) {
                senderBuilder.append(Utils.convertHtmlToPlainText(fragments[i++]));
                return;
            } else if (SENDER_LIST_TOKEN_SENDING.equals(fragment0)) {
                sendingFragment = sendingString;
            } else if (SENDER_LIST_TOKEN_SEND_FAILED.equals(fragment0)) {
                sendFailedFragment = sendFailedString;
            } else {
                String priorityString = fragments[i++];
                CharSequence nameString = fragments[i++];
                if (nameString.length() == 0)
                    nameString = meString;
                int priority = Integer.parseInt(priorityString);
                priorityToLength.put(priority, nameString.length());
                maxFoundPriority = Math.max(maxFoundPriority, priority);
            }
        }
        String numMessagesFragment = (numMessages != 0) ? " \u00A0"
                + Integer.toString(numMessages + numDrafts) : "";

        // Don't allocate fixedFragment unless we need it
        SpannableStringBuilder fixedFragment = null;
        int fixedFragmentLength = 0;
        if (draftsFragment.length() != 0 && allowDraft) {
            fixedFragment = new SpannableStringBuilder();
            fixedFragment.append(draftsFragment);
            if (draftsStyle != null) {
                fixedFragment.setSpan(CharacterStyle.wrap(draftsStyle), 0, fixedFragment.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        if (sendingFragment.length() != 0) {
            if (fixedFragment == null) {
                fixedFragment = new SpannableStringBuilder();
            }
            if (fixedFragment.length() != 0)
                fixedFragment.append(", ");
            fixedFragment.append(sendingFragment);
        }
        if (sendFailedFragment.length() != 0) {
            if (fixedFragment == null) {
                fixedFragment = new SpannableStringBuilder();
            }
            if (fixedFragment.length() != 0)
                fixedFragment.append(", ");
            fixedFragment.append(sendFailedFragment);
        }

        if (fixedFragment != null) {
            fixedFragmentLength = fixedFragment.length();
        }
        maxChars -= fixedFragmentLength;

        int maxPriorityToInclude = -1; // inclusive
        int numCharsUsed = numMessagesFragment.length();
        int numSendersUsed = 0;
        while (maxPriorityToInclude < maxFoundPriority) {
            if (priorityToLength.containsKey(maxPriorityToInclude + 1)) {
                int length = numCharsUsed + priorityToLength.get(maxPriorityToInclude + 1);
                if (numCharsUsed > 0)
                    length += 2;
                // We must show at least two senders if they exist. If we don't
                // have space for both
                // then we will truncate names.
                if (length > maxChars && numSendersUsed >= 2) {
                    break;
                }
                numCharsUsed = length;
                numSendersUsed++;
            }
            maxPriorityToInclude++;
        }

        int numCharsToRemovePerWord = 0;
        if (numCharsUsed > maxChars) {
            numCharsToRemovePerWord = (numCharsUsed - maxChars) / numSendersUsed;
        }

        String lastFragment = null;
        CharacterStyle lastStyle = null;
        for (int i = 0; i < numFragments;) {
            String fragment0 = fragments[i++];
            if ("".equals(fragment0)) {
                // This should be the final fragment.
            } else if (SENDER_LIST_TOKEN_ELIDED.equals(fragment0)) {
                if (lastFragment != null) {
                    addStyledFragment(senderBuilder, lastFragment, lastStyle, false);
                    senderBuilder.append(" ");
                    addStyledFragment(senderBuilder, "..", lastStyle, true);
                    senderBuilder.append(" ");
                }
                lastFragment = null;
            } else if (SENDER_LIST_TOKEN_NUM_MESSAGES.equals(fragment0)) {
                i++;
            } else if (SENDER_LIST_TOKEN_NUM_DRAFTS.equals(fragment0)) {
                i++;
            } else if (SENDER_LIST_TOKEN_SENDING.equals(fragment0)) {
            } else if (SENDER_LIST_TOKEN_SEND_FAILED.equals(fragment0)) {
            } else {
                final String unreadString = fragment0;
                final String priorityString = fragments[i++];
                String nameString = fragments[i++];
                if (nameString.length() == 0) {
                    nameString = meString.toString();
                } else {
                    nameString = Utils.convertHtmlToPlainText(nameString).toString();
                }
                if (numCharsToRemovePerWord != 0) {
                    nameString = nameString.substring(0,
                            Math.max(nameString.length() - numCharsToRemovePerWord, 0));
                }
                final boolean unread = unreadStatusIsForced ? forcedUnreadStatus : Integer
                        .parseInt(unreadString) != 0;
                final int priority = Integer.parseInt(priorityString);
                if (priority <= maxPriorityToInclude) {
                    if (lastFragment != null && !lastFragment.equals(nameString)) {
                        addStyledFragment(senderBuilder, lastFragment.concat(","), lastStyle,
                                false);
                        senderBuilder.append(" ");
                    }
                    lastFragment = nameString;
                    lastStyle = unread ? unreadStyle : readStyle;
                } else {
                    if (lastFragment != null) {
                        addStyledFragment(senderBuilder, lastFragment, lastStyle, false);
                        // Adjacent spans can cause the TextView in Gmail widget
                        // confused and leads to weird behavior on scrolling.
                        // Our workaround here is to separate the spans by
                        // spaces.
                        senderBuilder.append(" ");
                        addStyledFragment(senderBuilder, "..", lastStyle, true);
                        senderBuilder.append(" ");
                    }
                    lastFragment = null;
                }
            }
        }
        if (lastFragment != null) {
            addStyledFragment(senderBuilder, lastFragment, lastStyle, false);
        }
        senderBuilder.append(numMessagesFragment);
        if (fixedFragmentLength != 0) {
            statusBuilder.append(fixedFragment);
        }
    }

    /**
     * Adds a fragment with given style to a string builder.
     *
     * @param builder the current string builder
     * @param fragment the fragment to be added
     * @param style the style of the fragment
     * @param withSpaces whether to add the whole fragment or to divide it into
     *            smaller ones
     */
    private static void addStyledFragment(SpannableStringBuilder builder, String fragment,
            CharacterStyle style, boolean withSpaces) {
        if (withSpaces) {
            int pos = builder.length();
            builder.append(fragment);
            builder.setSpan(CharacterStyle.wrap(style), pos, builder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            int start = 0;
            while (true) {
                int pos = fragment.substring(start).indexOf(' ');
                if (pos == -1) {
                    addStyledFragment(builder, fragment.substring(start), style, true);
                    break;
                } else {
                    pos += start;
                    if (start < pos) {
                        addStyledFragment(builder, fragment.substring(start, pos), style, true);
                        builder.append(' ');
                    }
                    start = pos + 1;
                    if (start >= fragment.length()) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Returns a boolean indicating whether the table UI should be shown.
     */
    public static boolean useTabletUI(Resources res) {
        return res.getInteger(R.integer.use_tablet_ui) != 0;
    }

    /**
     * @return <code>true</code> if the right edge effect should be displayed on list items
     */
    public static boolean getDisplayListRightEdgeEffect(final boolean tabletDevice,
            final boolean listCollapsible, final int viewMode) {
        return tabletDevice && !listCollapsible
                && (ViewMode.isConversationMode(viewMode) || ViewMode.isAdMode(viewMode));
    }

    /**
     * Returns a boolean indicating whether or not we should animate in the
     * folder list fragment.
     */
    public static boolean useFolderListFragmentTransition(Context context) {
        if (sUseFolderListFragmentTransition == -1) {
            sUseFolderListFragmentTransition  = context.getResources().getInteger(
                    R.integer.use_folder_list_fragment_transition);
        }
        return sUseFolderListFragmentTransition != 0;
    }

    /**
     * Returns displayable text from the provided HTML string.
     * @param htmlText HTML string
     * @return Plain text string representation of the specified Html string
     */
    public static String convertHtmlToPlainText(String htmlText) {
        if (TextUtils.isEmpty(htmlText)) {
            return "";
        }
        return getHtmlTree(htmlText, new HtmlParser(), new HtmlTreeBuilder()).getPlainText();
    }

    public static String convertHtmlToPlainText(String htmlText, HtmlParser parser,
            HtmlTreeBuilder builder) {
        if (TextUtils.isEmpty(htmlText)) {
            return "";
        }
        return getHtmlTree(htmlText, parser, builder).getPlainText();
    }

    /**
     * Returns a {@link HtmlTree} representation of the specified HTML string.
     */
    public static HtmlTree getHtmlTree(String htmlText) {
        return getHtmlTree(htmlText, new HtmlParser(), new HtmlTreeBuilder());
    }

    /**
     * Returns a {@link HtmlTree} representation of the specified HTML string.
     */
    private static HtmlTree getHtmlTree(String htmlText, HtmlParser parser,
            HtmlTreeBuilder builder) {
        final HtmlDocument doc = parser.parse(htmlText);
        doc.accept(builder);

        return builder.getTree();
    }

    /**
     * Perform a simulated measure pass on the given child view, assuming the
     * child has a ViewGroup parent and that it should be laid out within that
     * parent with a matching width but variable height. Code largely lifted
     * from AnimatedAdapter.measureChildHeight().
     *
     * @param child a child view that has already been placed within its parent
     *            ViewGroup
     * @param parent the parent ViewGroup of child
     * @return measured height of the child in px
     */
    public static int measureViewHeight(View child, ViewGroup parent) {
        final ViewGroup.LayoutParams lp = child.getLayoutParams();
        final int childSideMargin;
        if (lp instanceof MarginLayoutParams) {
            final MarginLayoutParams mlp = (MarginLayoutParams) lp;
            childSideMargin = mlp.leftMargin + mlp.rightMargin;
        } else {
            childSideMargin = 0;
        }

        final int parentWSpec = MeasureSpec.makeMeasureSpec(parent.getWidth(), MeasureSpec.EXACTLY);
        final int wSpec = ViewGroup.getChildMeasureSpec(parentWSpec,
                parent.getPaddingLeft() + parent.getPaddingRight() + childSideMargin,
                ViewGroup.LayoutParams.MATCH_PARENT);
        final int hSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        child.measure(wSpec, hSpec);
        return child.getMeasuredHeight();
    }

    /**
     * Encode the string in HTML.
     *
     * @param removeEmptyDoubleQuotes If true, also remove any occurrence of ""
     *            found in the string
     */
    public static Object cleanUpString(String string, boolean removeEmptyDoubleQuotes) {
        return !TextUtils.isEmpty(string) ? TextUtils.htmlEncode(removeEmptyDoubleQuotes ? string
                .replace("\"\"", "") : string) : "";
    }

    /**
     * Get the correct display string for the unread count of a folder.
     */
    public static String getUnreadCountString(Context context, int unreadCount) {
        final String unreadCountString;
        final Resources resources = context.getResources();
        if (sMaxUnreadCount == -1) {
            sMaxUnreadCount = resources.getInteger(R.integer.maxUnreadCount);
        }
        if (unreadCount > sMaxUnreadCount) {
            if (sUnreadText == null) {
                sUnreadText = resources.getString(R.string.widget_large_unread_count);
            }
            // Localize "999+" according to the device language
            unreadCountString = String.format(sUnreadText, sMaxUnreadCount);
        } else if (unreadCount <= 0) {
            unreadCountString = "";
        } else {
            // Localize unread count according to the device language
            unreadCountString = String.format("%d", unreadCount);
        }
        return unreadCountString;
    }

    /**
     * Get the correct display string for the unread count in the actionbar.
     */
    public static CharSequence getUnreadMessageString(Context context, int unreadCount) {
        final SpannableString message;
        final Resources resources = context.getResources();
        if (sMaxUnreadCount == -1) {
            sMaxUnreadCount = resources.getInteger(R.integer.maxUnreadCount);
        }
        if (unreadCount > sMaxUnreadCount) {
            message = new SpannableString(
                    resources.getString(R.string.actionbar_large_unread_count, sMaxUnreadCount));
        } else {
             message = new SpannableString(resources.getQuantityString(
                     R.plurals.actionbar_unread_messages, unreadCount, unreadCount));
        }

        message.setSpan(CharacterStyle.wrap(ACTION_BAR_UNREAD_STYLE), 0,
                message.toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return message;
    }

    /**
     * Get text matching the last sync status.
     */
    public static CharSequence getSyncStatusText(Context context, int packedStatus) {
        final String[] errors = context.getResources().getStringArray(R.array.sync_status);
        final int status = packedStatus & 0x0f;
        if (status >= errors.length) {
            return "";
        }
        return errors[status];
    }

    /**
     * Create an intent to show a conversation.
     * @param conversation Conversation to open.
     * @param folder
     * @param account
     * @return
     */
    public static Intent createViewConversationIntent(final Context context,
            Conversation conversation, final Uri folderUri, Account account) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        final Uri versionedUri = appendVersionQueryParameter(context, conversation.uri);
        // We need the URI to be unique, even if it's for the same message, so append the folder URI
        final Uri uniqueUri = versionedUri.buildUpon().appendQueryParameter(
                FOLDER_URI_QUERY_PARAMETER, folderUri.toString()).build();
        intent.setDataAndType(uniqueUri, account.mimeType);
        intent.putExtra(Utils.EXTRA_ACCOUNT, account.serialize());
        intent.putExtra(Utils.EXTRA_FOLDER_URI, folderUri);
        intent.putExtra(Utils.EXTRA_CONVERSATION, conversation);
        return intent;
    }

    /**
     * Create an intent to open a folder.
     *
     * @param folder Folder to open.
     * @param account
     * @return
     */
    public static Intent createViewFolderIntent(final Context context, final Uri folderUri,
            Account account) {
        if (folderUri == null || account == null) {
            LogUtils.wtf(LOG_TAG, "Utils.createViewFolderIntent(%s,%s): Bad input", folderUri,
                    account);
            return null;
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        intent.setDataAndType(appendVersionQueryParameter(context, folderUri), account.mimeType);
        intent.putExtra(Utils.EXTRA_ACCOUNT, account.serialize());
        intent.putExtra(Utils.EXTRA_FOLDER_URI, folderUri);
        return intent;
    }

    /**
     * Creates an intent to open the default inbox for the given account.
     *
     * @param account
     * @return
     */
    public static Intent createViewInboxIntent(Account account) {
        if (account == null) {
            LogUtils.wtf(LOG_TAG, "Utils.createViewInboxIntent(%s): Bad input", account);
            return null;
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        intent.setDataAndType(account.settings.defaultInbox, account.mimeType);
        intent.putExtra(Utils.EXTRA_ACCOUNT, account.serialize());
        return intent;
    }

    /**
     * Helper method to show context-aware Gmail help.
     *
     * @param context Context to be used to open the help.
     * @param fromWhere Information about the activity the user was in
     * when they requested help.
     */
    public static void showHelp(Context context, Account account, String fromWhere) {
        final String urlString = (account != null && account.helpIntentUri != null) ?
                account.helpIntentUri.toString() : null;
        if (TextUtils.isEmpty(urlString) ) {
            LogUtils.e(LOG_TAG, "unable to show help for account: %s", account);
            return;
        }
        final Uri uri = addParamsToUrl(context, urlString);
        Uri.Builder builder = uri.buildUpon();
        // Add the activity specific information parameter.
        if (!TextUtils.isEmpty(fromWhere)) {
            builder = builder.appendQueryParameter(SMART_HELP_LINK_PARAMETER_NAME, fromWhere);
        }

        openUrl(context, builder.build(), null);
    }

    /**
     * Helper method to open a link in a browser.
     *
     * @param context Context
     * @param uri Uri to open.
     */
    private static void openUrl(Context context, Uri uri, Bundle optionalExtras) {
        if(uri == null || TextUtils.isEmpty(uri.toString())) {
            LogUtils.wtf(LOG_TAG, "invalid url in Utils.openUrl(): %s", uri);
            return;
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        // Fill in any of extras that have been requested.
        if (optionalExtras != null) {
            intent.putExtras(optionalExtras);
        }
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        context.startActivity(intent);
    }


    private static Uri addParamsToUrl(Context context, String url) {
        url = replaceLocale(url);
        Uri.Builder builder = Uri.parse(url).buildUpon();
        final int versionCode = getVersionCode(context);
        if (versionCode != -1) {
            builder = builder.appendQueryParameter(SMART_LINK_APP_VERSION,
                    String.valueOf(versionCode));
        }

        return builder.build();
    }

    /**
     * Replaces the language/country of the device into the given string.  The pattern "%locale%"
     * will be replaced with the <language_code>_<country_code> value.
     *
     * @param str the string to replace the language/country within
     *
     * @return the string with replacement
     */
    private static String replaceLocale(String str) {
        // Substitute locale if present in string
        if (str.contains("%locale%")) {
            Locale locale = Locale.getDefault();
            String tmp = locale.getLanguage() + "_" + locale.getCountry().toLowerCase();
            str = str.replace("%locale%", tmp);
        }
        return str;
    }

    /**
     * Returns the version code for the package, or -1 if it cannot be retrieved.
     */
    public static int getVersionCode(Context context) {
        if (sVersionCode == -1) {
            try {
                sVersionCode =
                        context.getPackageManager().getPackageInfo(context.getPackageName(),
                                0 /* flags */).versionCode;
            } catch (NameNotFoundException e) {
                LogUtils.e(Utils.LOG_TAG, "Error finding package %s",
                        context.getApplicationInfo().packageName);
            }
        }
        return sVersionCode;
    }

    /**
     * Show the top level settings screen for the supplied account.
     */
    public static void showSettings(Context context, Account account) {
        if (account == null) {
            LogUtils.e(LOG_TAG, "Invalid attempt to show setting screen with null account");
            return;
        }
        final Intent settingsIntent = new Intent(Intent.ACTION_EDIT, account.settingsIntentUri);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        context.startActivity(settingsIntent);
    }

    /**
     * Show the account level settings screen for the supplied account.
     */
    public static void showAccountSettings(Context context, Account account) {
        if (account == null) {
            LogUtils.e(LOG_TAG, "Invalid attempt to show setting screen with null account");
            return;
        }
        final Intent settingsIntent = new Intent(Intent.ACTION_EDIT,
                appendVersionQueryParameter(context, account.settingsIntentUri));

        settingsIntent.putExtra(EditSettingsExtras.EXTRA_ACCOUNT, account);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        context.startActivity(settingsIntent);
    }

    /**
     * Show the settings screen for the supplied account.
     */
     public static void showFolderSettings(Context context, Account account, Folder folder) {
        if (account == null || folder == null) {
            LogUtils.e(LOG_TAG, "Invalid attempt to show folder settings. account: %s folder: %s",
                    account, folder);
            return;
        }
        final Intent settingsIntent = new Intent(Intent.ACTION_EDIT,
                appendVersionQueryParameter(context, account.settingsIntentUri));

        settingsIntent.putExtra(EditSettingsExtras.EXTRA_ACCOUNT, account);
        settingsIntent.putExtra(EditSettingsExtras.EXTRA_FOLDER, folder);
        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        context.startActivity(settingsIntent);
    }

    /**
     * Show the settings screen for managing all folders.
     */
     public static void showManageFolder(Context context, Account account) {
         if (account == null) {
             LogUtils.e(LOG_TAG, "Invalid attempt to the manage folders screen with null account");
             return;
         }
         final Intent settingsIntent = new Intent(Intent.ACTION_EDIT, account.settingsIntentUri);

         settingsIntent.putExtra(EditSettingsExtras.EXTRA_ACCOUNT, account);
         settingsIntent.putExtra(EditSettingsExtras.EXTRA_MANAGE_FOLDERS, true);
         settingsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
         context.startActivity(settingsIntent);
    }

    /**
     * Show the feedback screen for the supplied account.
     */
    public static void sendFeedback(FeedbackEnabledActivity activity, Account account,
                                    boolean reportingProblem) {
        if (activity != null && account != null) {
            sendFeedback(activity, account.sendFeedbackIntentUri, reportingProblem);
        }
    }
    public static void sendFeedback(FeedbackEnabledActivity activity, Uri feedbackIntentUri,
            boolean reportingProblem) {
        if (activity != null &&  !isEmpty(feedbackIntentUri)) {
            final Bundle optionalExtras = new Bundle(2);
            optionalExtras.putBoolean(
                    UIProvider.SendFeedbackExtras.EXTRA_REPORTING_PROBLEM, reportingProblem);
            final Bitmap screenBitmap =  getReducedSizeBitmap(activity);
            if (screenBitmap != null) {
                optionalExtras.putParcelable(
                        UIProvider.SendFeedbackExtras.EXTRA_SCREEN_SHOT, screenBitmap);
            }
            openUrl(activity.getActivityContext(), feedbackIntentUri, optionalExtras);
        }
    }


    public static Bitmap getReducedSizeBitmap(FeedbackEnabledActivity activity) {
        final Window activityWindow = activity.getWindow();
        final View currentView = activityWindow != null ? activityWindow.getDecorView() : null;
        final View rootView = currentView != null ? currentView.getRootView() : null;
        if (rootView != null) {
            rootView.setDrawingCacheEnabled(true);
            final Bitmap drawingCache = rootView.getDrawingCache();
            // Null check to avoid NPE discovered from monkey crash:
            if (drawingCache != null) {
                final Bitmap originalBitmap = drawingCache.copy(Bitmap.Config.RGB_565, false);
                double originalHeight = originalBitmap.getHeight();
                double originalWidth = originalBitmap.getWidth();
                int newHeight = SCALED_SCREENSHOT_MAX_HEIGHT_WIDTH;
                int newWidth = SCALED_SCREENSHOT_MAX_HEIGHT_WIDTH;
                double scaleX, scaleY;
                scaleX = newWidth  / originalWidth;
                scaleY = newHeight / originalHeight;
                final double scale = Math.min(scaleX, scaleY);
                newWidth = (int)Math.round(originalWidth * scale);
                newHeight = (int)Math.round(originalHeight * scale);
                return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
            }
        }
        return null;
    }

    /**
     * Retrieves the mailbox search query associated with an intent (or null if not available),
     * doing proper sanitizing (e.g. trims whitespace).
     */
    public static String mailSearchQueryForIntent(Intent intent) {
        String query = intent.getStringExtra(SearchManager.QUERY);
        return TextUtils.isEmpty(query) ? null : query.trim();
   }

    /**
     * Split out a filename's extension and return it.
     * @param filename a file name
     * @return the file extension (max of 5 chars including period, like ".docx"), or null
     */
    public static String getFileExtension(String filename) {
        String extension = null;
        int index = !TextUtils.isEmpty(filename) ? filename.lastIndexOf('.') : -1;
        // Limit the suffix to dot + four characters
        if (index >= 0 && filename.length() - index <= FILE_EXTENSION_MAX_CHARS + 1) {
            extension = filename.substring(index);
        }
        return extension;
    }

   /**
    * (copied from {@link Intent#normalizeMimeType(String)} for pre-J)
    *
    * Normalize a MIME data type.
    *
    * <p>A normalized MIME type has white-space trimmed,
    * content-type parameters removed, and is lower-case.
    * This aligns the type with Android best practices for
    * intent filtering.
    *
    * <p>For example, "text/plain; charset=utf-8" becomes "text/plain".
    * "text/x-vCard" becomes "text/x-vcard".
    *
    * <p>All MIME types received from outside Android (such as user input,
    * or external sources like Bluetooth, NFC, or the Internet) should
    * be normalized before they are used to create an Intent.
    *
    * @param type MIME data type to normalize
    * @return normalized MIME data type, or null if the input was null
    * @see {@link #setType}
    * @see {@link #setTypeAndNormalize}
    */
   public static String normalizeMimeType(String type) {
       if (type == null) {
           return null;
       }

       type = type.trim().toLowerCase(Locale.US);

       final int semicolonIndex = type.indexOf(';');
       if (semicolonIndex != -1) {
           type = type.substring(0, semicolonIndex);
       }
       return type;
   }

   /**
    * (copied from {@link Uri#normalize()} for pre-J)
    *
    * Return a normalized representation of this Uri.
    *
    * <p>A normalized Uri has a lowercase scheme component.
    * This aligns the Uri with Android best practices for
    * intent filtering.
    *
    * <p>For example, "HTTP://www.android.com" becomes
    * "http://www.android.com"
    *
    * <p>All URIs received from outside Android (such as user input,
    * or external sources like Bluetooth, NFC, or the Internet) should
    * be normalized before they are used to create an Intent.
    *
    * <p class="note">This method does <em>not</em> validate bad URI's,
    * or 'fix' poorly formatted URI's - so do not use it for input validation.
    * A Uri will always be returned, even if the Uri is badly formatted to
    * begin with and a scheme component cannot be found.
    *
    * @return normalized Uri (never null)
    * @see {@link android.content.Intent#setData}
    * @see {@link #setNormalizedData}
    */
   public static Uri normalizeUri(Uri uri) {
       String scheme = uri.getScheme();
       if (scheme == null) return uri;  // give up
       String lowerScheme = scheme.toLowerCase(Locale.US);
       if (scheme.equals(lowerScheme)) return uri;  // no change

       return uri.buildUpon().scheme(lowerScheme).build();
   }

   public static Intent setIntentTypeAndNormalize(Intent intent, String type) {
       return intent.setType(normalizeMimeType(type));
   }

   public static Intent setIntentDataAndTypeAndNormalize(Intent intent, Uri data, String type) {
       return intent.setDataAndType(normalizeUri(data), normalizeMimeType(type));
   }

   public static int getTransparentColor(int color) {
       return 0x00ffffff & color;
   }

    public static void setMenuItemVisibility(Menu menu, int itemId, boolean shouldShow) {
        final MenuItem item = menu.findItem(itemId);
        if (item == null) {
            return;
        }
        item.setVisible(shouldShow);
    }

    /**
     * Parse a string (possibly null or empty) into a URI. If the string is null
     * or empty, null is returned back. Otherwise an empty URI is returned.
     *
     * @param uri
     * @return a valid URI, possibly {@link android.net.Uri#EMPTY}
     */
    public static Uri getValidUri(String uri) {
        if (TextUtils.isEmpty(uri) || uri == JSONObject.NULL)
            return Uri.EMPTY;
        return Uri.parse(uri);
    }

    public static boolean isEmpty(Uri uri) {
        return uri == null || uri.equals(Uri.EMPTY);
    }

    public static String dumpFragment(Fragment f) {
        final StringWriter sw = new StringWriter();
        f.dump("", new FileDescriptor(), new PrintWriter(sw), new String[0]);
        return sw.toString();
    }

    public static void dumpViewTree(ViewGroup root) {
        dumpViewTree(root, "");
    }

    private static void dumpViewTree(ViewGroup g, String prefix) {
        LogUtils.i(LOG_TAG, "%sVIEWGROUP: %s childCount=%s", prefix, g, g.getChildCount());
        final String childPrefix = prefix + "  ";
        for (int i = 0; i < g.getChildCount(); i++) {
            final View child = g.getChildAt(i);
            if (child instanceof ViewGroup) {
                dumpViewTree((ViewGroup) child, childPrefix);
            } else {
                LogUtils.i(LOG_TAG, "%sCHILD #%s: %s", childPrefix, i, child);
            }
        }
    }

    /**
     * Executes an out-of-band command on the cursor.
     * @param cursor
     * @param request Bundle with all keys and values set for the command.
     * @param key The string value against which we will check for success or failure
     * @return true if the operation was a success.
     */
    private static boolean executeConversationCursorCommand(
            Cursor cursor, Bundle request, String key) {
        final Bundle response = cursor.respond(request);
        final String result = response.getString(key,
                UIProvider.ConversationCursorCommand.COMMAND_RESPONSE_FAILED);

        return UIProvider.ConversationCursorCommand.COMMAND_RESPONSE_OK.equals(result);
    }

    /**
     * Commands a cursor representing a set of conversations to indicate that an item is being shown
     * in the UI.
     *
     * @param cursor a conversation cursor
     * @param position position of the item being shown.
     */
    public static boolean notifyCursorUIPositionChange(Cursor cursor, int position) {
        final Bundle request = new Bundle();
        final String key =
                UIProvider.ConversationCursorCommand.COMMAND_NOTIFY_CURSOR_UI_POSITION_CHANGE;
        request.putInt(key, position);
        return executeConversationCursorCommand(cursor, request, key);
    }

    /**
     * Commands a cursor representing a set of conversations to set its visibility state.
     *
     * @param cursor a conversation cursor
     * @param visible true if the conversation list is visible, false otherwise.
     * @param isFirstSeen true if you want to notify the cursor that this conversation list was seen
     *        for the first time: the user launched the app into it, or the user switched from some
     *        other folder into it.
     */
    public static void setConversationCursorVisibility(
            Cursor cursor, boolean visible, boolean isFirstSeen) {
        new MarkConversationCursorVisibleTask(cursor, visible, isFirstSeen).execute();
    }

    /**
     * Async task for  marking conversations "seen" and informing the cursor that the folder was
     * seen for the first time by the UI.
     */
    private static class MarkConversationCursorVisibleTask extends AsyncTask<Void, Void, Void> {
        private final Cursor mCursor;
        private final boolean mVisible;
        private final boolean mIsFirstSeen;

        /**
         * Create a new task with the given cursor, with the given visibility and
         *
         * @param cursor
         * @param isVisible true if the conversation list is visible, false otherwise.
         * @param isFirstSeen true if the folder was shown for the first time: either the user has
         *        just switched to it, or the user started the app in this folder.
         */
        public MarkConversationCursorVisibleTask(
                Cursor cursor, boolean isVisible, boolean isFirstSeen) {
            mCursor = cursor;
            mVisible = isVisible;
            mIsFirstSeen = isFirstSeen;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (mCursor == null) {
                return null;
            }
            final Bundle request = new Bundle();
            if (mIsFirstSeen) {
                request.putBoolean(
                        UIProvider.ConversationCursorCommand.COMMAND_KEY_ENTERED_FOLDER, true);
            }
            final String key = UIProvider.ConversationCursorCommand.COMMAND_KEY_SET_VISIBILITY;
            request.putBoolean(key, mVisible);
            executeConversationCursorCommand(mCursor, request, key);
            return null;
        }
    }


    /**
     * This utility method returns the conversation ID at the current cursor position.
     * @return the conversation id at the cursor.
     */
    public static long getConversationId(ConversationCursor cursor) {
        return cursor.getLong(UIProvider.CONVERSATION_ID_COLUMN);
    }

    /**
     * This utility method returns the conversation Uri at the current cursor position.
     * @return the conversation id at the cursor.
     */
    public static String getConversationUri(ConversationCursor cursor) {
        return cursor.getString(UIProvider.CONVERSATION_URI_COLUMN);
    }

    /**
     * @return whether to show two pane or single pane search results.
     */
    public static boolean showTwoPaneSearchResults(Context context) {
        return context.getResources().getBoolean(R.bool.show_two_pane_search_results);
    }

    /**
     * Sets the layer type of a view to hardware if the view is attached and hardware acceleration
     * is enabled. Does nothing otherwise.
     */
    public static void enableHardwareLayer(View v) {
        if (v != null && v.isHardwareAccelerated()) {
            v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            v.buildLayer();
        }
    }

    /**
     * Return whether menus should show the disabled archive menu item or just
     * remove it when archive is not available.
     */
    public static boolean shouldShowDisabledArchiveIcon(Context context) {
        return context.getResources().getBoolean(R.bool.show_disabled_archive_menu_item);
    }

    public static int getDefaultFolderBackgroundColor(Context context) {
        if (sDefaultFolderBackgroundColor == -1) {
            sDefaultFolderBackgroundColor = context.getResources().getColor(
                    R.color.default_folder_background_color);
        }
        return sDefaultFolderBackgroundColor;
    }

    /**
     * Returns the count that should be shown for the specified folder.  This method should be used
     * when the UI wants to display an "unread" count.  For most labels, the returned value will be
     * the unread count, but for some folder types (outbox, drafts, trash) this will return the
     * total count.
     */
    public static int getFolderUnreadDisplayCount(final Folder folder) {
        if (folder != null) {
            if (folder.isUnreadCountHidden()) {
                return folder.totalCount;
            } else {
                return folder.unreadCount;
            }
        }
        return 0;
    }

    /**
     * @return an intent which, if launched, will reply to the conversation
     */
    public static Intent createReplyIntent(final Context context, final Account account,
            final Uri messageUri, final boolean isReplyAll) {
        final Intent intent =
                ComposeActivity.createReplyIntent(context, account, messageUri, isReplyAll);
        return intent;
    }

    /**
     * @return an intent which, if launched, will forward the conversation
     */
    public static Intent createForwardIntent(
            final Context context, final Account account, final Uri messageUri) {
        final Intent intent = ComposeActivity.createForwardIntent(context, account, messageUri);
        return intent;
    }

    public static Uri appendVersionQueryParameter(final Context context, final Uri uri) {
        int appVersion = 0;

        try {
            final PackageInfo packageInfo =
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            appVersion = packageInfo.versionCode;
        } catch (final NameNotFoundException e) {
            LogUtils.wtf(LOG_TAG, e, "Couldn't find our own PackageInfo");
        }

        return uri.buildUpon().appendQueryParameter(APP_VERSION_QUERY_PARAMETER,
                Integer.toString(appVersion)).build();
    }

    /**
     * Convenience method for diverting mailto: uris directly to our compose activity. Using this
     * method ensures that the Account object is not accidentally sent to a different process.
     *
     * @param context for sending the intent
     * @param uri mailto: or other uri
     * @param account desired account for potential compose activity
     * @return true if a compose activity was started, false if uri should be sent to a view intent
     */
    public static boolean divertMailtoUri(final Context context, final Uri uri,
            final Account account) {
        final String scheme = normalizeUri(uri).getScheme();
        if (TextUtils.equals(MAILTO_SCHEME, scheme)) {
            ComposeActivity.composeToAddress(context, account, uri.getSchemeSpecificPart());
            return true;
        }
        return false;
    }

    /**
     * Gets the specified {@link Folder} object.
     *
     * @param folderUri The {@link Uri} for the folder
     * @param allowHidden <code>true</code> to allow a hidden folder to be returned,
     *        <code>false</code> to return <code>null</code> instead
     * @return the specified {@link Folder} object, or <code>null</code>
     */
    public static Folder getFolder(final Context context, final Uri folderUri,
            final boolean allowHidden) {
        final Uri uri = folderUri
                .buildUpon()
                .appendQueryParameter(UIProvider.ALLOW_HIDDEN_FOLDERS_QUERY_PARAM,
                        Boolean.toString(allowHidden))
                .build();

        final Cursor cursor = context.getContentResolver().query(uri,
                UIProvider.FOLDERS_PROJECTION, null, null, null);

        if (cursor == null) {
            return null;
        }

        try {
            if (cursor.moveToFirst()) {
                return new Folder(cursor);
            } else {
                return null;
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Begins systrace tracing for a given tag. No-op on unsupported platform versions.
     *
     * @param tag systrace tag to use
     *
     * @see android.os.Trace#beginSection(String)
     */
    public static void traceBeginSection(String tag) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            android.os.Trace.beginSection(tag);
        }
    }

    /**
     * Ends systrace tracing for the most recently begun section. No-op on unsupported platform
     * versions.
     *
     * @see android.os.Trace#endSection()
     */
    public static void traceEndSection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            android.os.Trace.endSection();
        }
    }

    /**
     * Get the background color of Gmail's action bar.
     */
    public static int getActionBarBackgroundResource(final Context context) {
        final TypedValue actionBarStyle = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.actionBarStyle, actionBarStyle, true)
                && actionBarStyle.type == TypedValue.TYPE_REFERENCE) {
            final TypedValue backgroundValue = new TypedValue();
            final TypedArray attr = context.obtainStyledAttributes(actionBarStyle.resourceId,
                    STYLE_ATTR);
            attr.getValue(0, backgroundValue);
            attr.recycle();
            return backgroundValue.resourceId;
        } else {
            // Default color
            return context.getResources().getColor(R.color.list_background_color);
        }
    }

    /**
     * Email addresses are supposed to be treated as case-insensitive for the host-part and
     * case-sensitive for the local-part, but nobody really wants email addresses to match
     * case-sensitive on the local-part, so just smash everything to lower case.
     * @param email Hello@Example.COM
     * @return hello@example.com
     */
    public static String normalizeEmailAddress(String email) {
        /*
        // The RFC5321 version
        if (TextUtils.isEmpty(email)) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts.length != 2) {
            LogUtils.d(LOG_TAG, "Tried to normalize a malformed email address: ", email);
            return email;
        }

        return parts[0] + "@" + parts[1].toLowerCase(Locale.US);
        */
        if (TextUtils.isEmpty(email)) {
            return email;
        } else {
            // Doing this for other locales might really screw things up, so do US-version only
            return email.toLowerCase(Locale.US);
        }
    }
}
