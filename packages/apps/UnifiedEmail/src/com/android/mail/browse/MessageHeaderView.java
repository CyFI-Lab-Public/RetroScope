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

package com.android.mail.browse;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mail.ContactInfo;
import com.android.mail.ContactInfoSource;
import com.android.mail.R;
import com.android.mail.browse.ConversationViewAdapter.BorderItem;
import com.android.mail.browse.ConversationViewAdapter.MessageHeaderItem;
import com.android.mail.compose.ComposeActivity;
import com.android.mail.perf.Timer;
import com.android.mail.photomanager.LetterTileProvider;
import com.android.mail.providers.Account;
import com.android.mail.providers.Address;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.providers.Message;
import com.android.mail.providers.UIProvider;
import com.android.mail.ui.ImageCanvas;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;
import com.android.mail.utils.VeiledAddressMatcher;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

public class MessageHeaderView extends LinearLayout implements OnClickListener,
        OnMenuItemClickListener, ConversationContainer.DetachListener {

    /**
     * Cap very long recipient lists during summary construction for efficiency.
     */
    private static final int SUMMARY_MAX_RECIPIENTS = 50;

    private static final int MAX_SNIPPET_LENGTH = 100;

    private static final int SHOW_IMAGE_PROMPT_ONCE = 1;
    private static final int SHOW_IMAGE_PROMPT_ALWAYS = 2;

    private static final String HEADER_INFLATE_TAG = "message header inflate";
    private static final String HEADER_ADDVIEW_TAG = "message header addView";
    private static final String HEADER_RENDER_TAG = "message header render";
    private static final String PREMEASURE_TAG = "message header pre-measure";
    private static final String LAYOUT_TAG = "message header layout";
    private static final String MEASURE_TAG = "message header measure";

    private static final String RECIPIENT_HEADING_DELIMITER = "   ";

    private static final String LOG_TAG = LogTag.getLogTag();

    public static final int DEFAULT_MODE = 0;
    public static final int POPUP_MODE = 1;

    // This is a debug only feature
    public static final boolean ENABLE_REPORT_RENDERING_PROBLEM = false;

    private static final String DETAILS_DIALOG_TAG = "details-dialog";

    private MessageHeaderViewCallbacks mCallbacks;

    private ViewGroup mUpperHeaderView;
    private View mSnapHeaderBottomBorder;
    private TextView mSenderNameView;
    private TextView mSenderEmailView;
    private TextView mDateView;
    private TextView mSnippetView;
    private QuickContactBadge mPhotoView;
    private ImageView mStarView;
    private ViewGroup mTitleContainerView;
    private ViewGroup mExtraContentView;
    private ViewGroup mCollapsedDetailsView;
    private ViewGroup mExpandedDetailsView;
    private SpamWarningView mSpamWarningView;
    private TextView mImagePromptView;
    private MessageInviteView mInviteView;
    private View mForwardButton;
    private View mOverflowButton;
    private View mDraftIcon;
    private View mEditDraftButton;
    private TextView mUpperDateView;
    private View mReplyButton;
    private View mReplyAllButton;
    private View mAttachmentIcon;
    private final EmailCopyContextMenu mEmailCopyMenu;

    // temporary fields to reference raw data between initial render and details
    // expansion
    private String[] mFrom;
    private String[] mTo;
    private String[] mCc;
    private String[] mBcc;
    private String[] mReplyTo;

    private boolean mIsDraft = false;

    private boolean mIsSending;

    /**
     * The snappy header has special visibility rules (i.e. no details header,
     * even though it has an expanded appearance)
     */
    private boolean mIsSnappy;

    private String mSnippet;

    private Address mSender;

    private ContactInfoSource mContactInfoSource;

    private boolean mPreMeasuring;

    private ConversationAccountController mAccountController;

    private Map<String, Address> mAddressCache;

    private boolean mShowImagePrompt;

    /**
     * Take the initial visibility of the star view to mean its collapsed
     * visibility. Star is always visible when expanded, but sometimes, like on
     * phones, there isn't enough room to warrant showing star when collapsed.
     */
    private boolean mCollapsedStarVisible;
    private boolean mStarShown;

    /**
     * End margin of the text when collapsed. When expanded, the margin is 0.
     */
    private int mTitleContainerCollapsedMarginEnd;

    private PopupMenu mPopup;

    private MessageHeaderItem mMessageHeaderItem;
    private ConversationMessage mMessage;

    private boolean mCollapsedDetailsValid;
    private boolean mExpandedDetailsValid;

    private final LayoutInflater mInflater;

    private AsyncQueryHandler mQueryHandler;

    private boolean mObservingContactInfo;

    /**
     * What I call myself? "me" in English, and internationalized correctly.
     */
    private final String mMyName;

    private final DataSetObserver mContactInfoObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            updateContactInfo();
        }
    };

    private boolean mExpandable = true;

    private int mExpandMode = DEFAULT_MODE;

    private DialogFragment mDetailsPopup;

    private VeiledAddressMatcher mVeiledMatcher;

    private boolean mIsViewOnlyMode = false;

    private LetterTileProvider mLetterTileProvider;
    private final int mContactPhotoWidth;
    private final int mContactPhotoHeight;

    public interface MessageHeaderViewCallbacks {
        void setMessageSpacerHeight(MessageHeaderItem item, int newSpacerHeight);

        void setMessageExpanded(MessageHeaderItem item, int newSpacerHeight,
                int topBorderHeight, int bottomBorderHeight);

        void setMessageDetailsExpanded(MessageHeaderItem messageHeaderItem, boolean expanded,
                int previousMessageHeaderItemHeight);

        void showExternalResources(Message msg);

        void showExternalResources(String senderRawAddress);

        boolean supportsMessageTransforms();

        String getMessageTransforms(Message msg);

        FragmentManager getFragmentManager();
    }

    public MessageHeaderView(Context context) {
        this(context, null);
    }

    public MessageHeaderView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public MessageHeaderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mEmailCopyMenu = new EmailCopyContextMenu(getContext());
        mInflater = LayoutInflater.from(context);
        mMyName = context.getString(R.string.me_object_pronun);

        final Resources resources = getResources();
        mContactPhotoWidth = resources.getDimensionPixelSize(
                R.dimen.message_header_contact_photo_width);
        mContactPhotoHeight = resources.getDimensionPixelSize(
                R.dimen.message_header_contact_photo_height);
    }

    /**
     * Expand mode is DEFAULT_MODE by default.
     */
    public void setExpandMode(int mode) {
        mExpandMode = mode;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mUpperHeaderView = (ViewGroup) findViewById(R.id.upper_header);
        mSnapHeaderBottomBorder = findViewById(R.id.snap_header_bottom_border);
        mSenderNameView = (TextView) findViewById(R.id.sender_name);
        mSenderEmailView = (TextView) findViewById(R.id.sender_email);
        mDateView = (TextView) findViewById(R.id.send_date);
        mSnippetView = (TextView) findViewById(R.id.email_snippet);
        mPhotoView = (QuickContactBadge) findViewById(R.id.photo);
        mReplyButton = findViewById(R.id.reply);
        mReplyAllButton = findViewById(R.id.reply_all);
        mForwardButton = findViewById(R.id.forward);
        mStarView = (ImageView) findViewById(R.id.star);
        mTitleContainerView = (ViewGroup) findViewById(R.id.title_container);
        mOverflowButton = findViewById(R.id.overflow);
        mDraftIcon = findViewById(R.id.draft);
        mEditDraftButton = findViewById(R.id.edit_draft);
        mUpperDateView = (TextView) findViewById(R.id.upper_date);
        mAttachmentIcon = findViewById(R.id.attachment);
        mExtraContentView = (ViewGroup) findViewById(R.id.header_extra_content);

        mCollapsedStarVisible = mStarView.getVisibility() == VISIBLE;
        final Resources resources = getResources();
        mTitleContainerCollapsedMarginEnd = resources.getDimensionPixelSize(
                R.dimen.message_header_title_container_margin_end_collapsed);

        setExpanded(true);

        registerMessageClickTargets(R.id.reply, R.id.reply_all, R.id.forward, R.id.star,
                R.id.edit_draft, R.id.overflow, R.id.upper_header);

        mUpperHeaderView.setOnCreateContextMenuListener(mEmailCopyMenu);
    }

    private void registerMessageClickTargets(int... ids) {
        for (int id : ids) {
            View v = findViewById(id);
            if (v != null) {
                v.setOnClickListener(this);
            }
        }
    }

    /**
     * Associate the header with a contact info source for later contact
     * presence/photo lookup.
     */
    public void setContactInfoSource(ContactInfoSource contactInfoSource) {
        mContactInfoSource = contactInfoSource;
    }

    public void setCallbacks(MessageHeaderViewCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    public void setVeiledMatcher(VeiledAddressMatcher matcher) {
        mVeiledMatcher = matcher;
    }

    public boolean isExpanded() {
        // (let's just arbitrarily say that unbound views are expanded by default)
        return mMessageHeaderItem == null || mMessageHeaderItem.isExpanded();
    }

    public void setSnappy(boolean snappy) {
        mIsSnappy = snappy;
        hideMessageDetails();
    }

    @Override
    public void onDetachedFromParent() {
        unbind();
    }

    /**
     * Headers that are unbound will not match any rendered header (matches()
     * will return false). Unbinding is not guaranteed to *hide* the view's old
     * data, though. To re-bind this header to message data, call render() or
     * renderUpperHeaderFrom().
     */
    public void unbind() {
        mMessageHeaderItem = null;
        mMessage = null;

        if (mObservingContactInfo) {
            mContactInfoSource.unregisterObserver(mContactInfoObserver);
            mObservingContactInfo = false;
        }
    }

    public void initialize(ConversationAccountController accountController,
            Map<String, Address> addressCache) {
        mAccountController = accountController;
        mAddressCache = addressCache;
    }

    private Account getAccount() {
        return mAccountController != null ? mAccountController.getAccount() : null;
    }

    public void bind(MessageHeaderItem headerItem, boolean measureOnly) {
        if (mMessageHeaderItem != null && mMessageHeaderItem == headerItem) {
            return;
        }

        mMessageHeaderItem = headerItem;
        render(measureOnly);
    }

    /**
     * Rebinds the view to its data. This will only update the view
     * if the {@link MessageHeaderItem} sent as a parameter is the
     * same as the view's current {@link MessageHeaderItem} and the
     * view's expanded state differs from the item's expanded state.
     */
    public void rebind(MessageHeaderItem headerItem) {
        if (mMessageHeaderItem == null || mMessageHeaderItem != headerItem ||
                isActivated() == isExpanded()) {
            return;
        }

        render(false /* measureOnly */);
    }

    public void refresh() {
        render(false);
    }

    private void render(boolean measureOnly) {
        if (mMessageHeaderItem == null) {
            return;
        }

        Timer t = new Timer();
        t.start(HEADER_RENDER_TAG);

        mCollapsedDetailsValid = false;
        mExpandedDetailsValid = false;

        mMessage = mMessageHeaderItem.getMessage();
        mShowImagePrompt = mMessage.shouldShowImagePrompt();
        setExpanded(mMessageHeaderItem.isExpanded());

        mFrom = mMessage.getFromAddresses();
        mTo = mMessage.getToAddresses();
        mCc = mMessage.getCcAddresses();
        mBcc = mMessage.getBccAddresses();
        mReplyTo = mMessage.getReplyToAddresses();

        /**
         * Turns draft mode on or off. Draft mode hides message operations other
         * than "edit", hides contact photo, hides presence, and changes the
         * sender name to "Draft".
         */
        mIsDraft = mMessage.draftType != UIProvider.DraftType.NOT_A_DRAFT;
        mIsSending = mMessage.isSending;

        // If this was a sent message AND:
        // 1. the account has a custom from, the cursor will populate the
        // selected custom from as the fromAddress when a message is sent but
        // not yet synced.
        // 2. the account has no custom froms, fromAddress will be empty, and we
        // can safely fall back and show the account name as sender since it's
        // the only possible fromAddress.
        String from = mMessage.getFrom();
        if (TextUtils.isEmpty(from)) {
            from = getAccount().name;
        }
        mSender = getAddress(from);

        mStarView.setSelected(mMessage.starred);
        mStarView.setContentDescription(getResources().getString(
                mStarView.isSelected() ? R.string.remove_star : R.string.add_star));
        mStarShown = true;

        final Conversation conversation = mMessage.getConversation();
        if (conversation != null) {
            for (Folder folder : conversation.getRawFolders()) {
                if (folder.isTrash()) {
                    mStarShown = false;
                    break;
                }
            }
        }

        updateChildVisibility();

        if (mIsDraft || mIsSending) {
            mSnippet = makeSnippet(mMessage.snippet);
        } else {
            mSnippet = mMessage.snippet;
        }

        mSenderNameView.setText(getHeaderTitle());
        mSenderEmailView.setText(getHeaderSubtitle());
        mDateView.setText(mMessageHeaderItem.getTimestampLong());
        mSnippetView.setText(mSnippet);
        setAddressOnContextMenu();

        if (mUpperDateView != null) {
            mUpperDateView.setText(mMessageHeaderItem.getTimestampShort());
        }

        if (measureOnly) {
            // avoid leaving any state around that would interfere with future regular bind() calls
            unbind();
        } else {
            updateContactInfo();
            if (!mObservingContactInfo) {
                mContactInfoSource.registerObserver(mContactInfoObserver);
                mObservingContactInfo = true;
            }
        }

        t.pause(HEADER_RENDER_TAG);
    }

    /**
     * Update context menu's address field for when the user long presses
     * on the message header and attempts to copy/send email.
     */
    private void setAddressOnContextMenu() {
        mEmailCopyMenu.setAddress(mSender.getAddress());
    }

    public boolean isBoundTo(ConversationOverlayItem item) {
        return item == mMessageHeaderItem;
    }

    public Address getAddress(String emailStr) {
        return getAddress(mAddressCache, emailStr);
    }

    public static Address getAddress(Map<String, Address> cache, String emailStr) {
        Address addr = null;
        synchronized (cache) {
            if (cache != null) {
                addr = cache.get(emailStr);
            }
            if (addr == null) {
                addr = Address.getEmailAddress(emailStr);
                if (cache != null) {
                    cache.put(emailStr, addr);
                }
            }
        }
        return addr;
    }

    private void updateSpacerHeight() {
        final int h = measureHeight();

        mMessageHeaderItem.setHeight(h);
        if (mCallbacks != null) {
            mCallbacks.setMessageSpacerHeight(mMessageHeaderItem, h);
        }
    }

    private int measureHeight() {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null) {
            LogUtils.e(LOG_TAG, new Error(), "Unable to measure height of detached header");
            return getHeight();
        }
        mPreMeasuring = true;
        final int h = Utils.measureViewHeight(this, parent);
        mPreMeasuring = false;
        return h;
    }

    private CharSequence getHeaderTitle() {
        CharSequence title;

        if (mIsDraft) {
            title = getResources().getQuantityText(R.plurals.draft, 1);
        } else if (mIsSending) {
            title = getResources().getString(R.string.sending);
        } else {
            title = getSenderName(mSender);
        }

        return title;
    }

    private CharSequence getHeaderSubtitle() {
        CharSequence sub;
        if (mIsSending) {
            sub = null;
        } else {
            if (isExpanded()) {
                if (mMessage.viaDomain != null) {
                    sub = getResources().getString(
                            R.string.via_domain, mMessage.viaDomain);
                } else {
                    sub = getSenderAddress(mSender);
                }
            } else {
                sub = mSnippet;
            }
        }
        return sub;
    }

    /**
     * Return the name, if known, or just the address.
     */
    private static CharSequence getSenderName(Address sender) {
        final String displayName = sender.getName();
        return TextUtils.isEmpty(displayName) ? sender.getAddress() : displayName;
    }

    /**
     * Return the address, if a name is present, or null if not.
     */
    private static CharSequence getSenderAddress(Address sender) {
        return sender.getAddress();
    }

    private static void setChildVisibility(int visibility, View... children) {
        for (View v : children) {
            if (v != null) {
                v.setVisibility(visibility);
            }
        }
    }

    private void setExpanded(final boolean expanded) {
        // use View's 'activated' flag to store expanded state
        // child view state lists can use this to toggle drawables
        setActivated(expanded);
        if (mMessageHeaderItem != null) {
            mMessageHeaderItem.setExpanded(expanded);
        }
    }

    /**
     * Update the visibility of the many child views based on expanded/collapsed
     * and draft/normal state.
     */
    private void updateChildVisibility() {
        // Too bad this can't be done with an XML state list...

        if (mIsViewOnlyMode) {
            setMessageDetailsVisibility(VISIBLE);
            setChildVisibility(GONE, mSnapHeaderBottomBorder);

            setChildVisibility(GONE, mReplyButton, mReplyAllButton, mForwardButton,
                    mOverflowButton, mDraftIcon, mEditDraftButton, mStarView,
                    mAttachmentIcon, mUpperDateView, mSnippetView);
            setChildVisibility(VISIBLE, mPhotoView, mSenderEmailView, mDateView);

            setChildMarginEnd(mTitleContainerView, 0);
        } else if (isExpanded()) {
            int normalVis, draftVis;

            setMessageDetailsVisibility((mIsSnappy) ? GONE : VISIBLE);
            setChildVisibility(mIsSnappy ? VISIBLE : GONE, mSnapHeaderBottomBorder);

            if (mIsDraft) {
                normalVis = GONE;
                draftVis = VISIBLE;
            } else {
                normalVis = VISIBLE;
                draftVis = GONE;
            }

            setReplyOrReplyAllVisible();
            setChildVisibility(normalVis, mPhotoView, mForwardButton, mOverflowButton);
            setChildVisibility(draftVis, mDraftIcon, mEditDraftButton);
            setChildVisibility(VISIBLE, mSenderEmailView, mDateView);
            setChildVisibility(GONE, mAttachmentIcon, mUpperDateView, mSnippetView);
            setChildVisibility(mStarShown ? VISIBLE : GONE, mStarView);

            setChildMarginEnd(mTitleContainerView, 0);

        } else {

            setMessageDetailsVisibility(GONE);
            setChildVisibility(GONE, mSnapHeaderBottomBorder);
            setChildVisibility(VISIBLE, mSnippetView, mUpperDateView);

            setChildVisibility(GONE, mEditDraftButton, mReplyButton, mReplyAllButton,
                    mForwardButton, mOverflowButton, mSenderEmailView, mDateView);

            setChildVisibility(mMessage.hasAttachments ? VISIBLE : GONE,
                    mAttachmentIcon);

            setChildVisibility(mCollapsedStarVisible && mStarShown ? VISIBLE : GONE, mStarView);

            setChildMarginEnd(mTitleContainerView, mTitleContainerCollapsedMarginEnd);

            if (mIsDraft) {

                setChildVisibility(VISIBLE, mDraftIcon);
                setChildVisibility(GONE, mPhotoView);

            } else {

                setChildVisibility(GONE, mDraftIcon);
                setChildVisibility(VISIBLE, mPhotoView);

            }
        }
    }

    /**
     * If an overflow menu is present in this header's layout, set the
     * visibility of "Reply" and "Reply All" actions based on a user preference.
     * Only one of those actions will be visible when an overflow is present. If
     * no overflow is present (e.g. big phone or tablet), it's assumed we have
     * plenty of screen real estate and can show both.
     */
    private void setReplyOrReplyAllVisible() {
        if (mIsDraft) {
            setChildVisibility(GONE, mReplyButton, mReplyAllButton);
            return;
        } else if (mOverflowButton == null) {
            setChildVisibility(VISIBLE, mReplyButton, mReplyAllButton);
            return;
        }

        final Account account = getAccount();
        final boolean defaultReplyAll = (account != null) ? account.settings.replyBehavior
                == UIProvider.DefaultReplyBehavior.REPLY_ALL : false;
        setChildVisibility(defaultReplyAll ? GONE : VISIBLE, mReplyButton);
        setChildVisibility(defaultReplyAll ? VISIBLE : GONE, mReplyAllButton);
    }

    private static void setChildMarginEnd(View childView, int marginEnd) {
        MarginLayoutParams mlp = (MarginLayoutParams) childView.getLayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mlp.setMarginEnd(marginEnd);
        } else {
            mlp.rightMargin = marginEnd;
        }
        childView.setLayoutParams(mlp);
    }

    /**
     * Utility class to build a list of recipient lists.
     */
    private static class RecipientListsBuilder {
        private final Context mContext;
        private final String mMe;
        private final String mMyName;
        private final SpannableStringBuilder mBuilder = new SpannableStringBuilder();
        private final CharSequence mComma;
        private final Map<String, Address> mAddressCache;
        private final VeiledAddressMatcher mMatcher;

        int mRecipientCount = 0;
        boolean mFirst = true;

        public RecipientListsBuilder(Context context, String me, String myName,
                Map<String, Address> addressCache, VeiledAddressMatcher matcher) {
            mContext = context;
            mMe = me;
            mMyName = myName;
            mComma = mContext.getText(R.string.enumeration_comma);
            mAddressCache = addressCache;
            mMatcher = matcher;
        }

        public void append(String[] recipients, int headingRes) {
            int addLimit = SUMMARY_MAX_RECIPIENTS - mRecipientCount;
            CharSequence recipientList = getSummaryTextForHeading(headingRes, recipients, addLimit);
            if (recipientList != null) {
                // duplicate TextUtils.join() logic to minimize temporary
                // allocations, and because we need to support spans
                if (mFirst) {
                    mFirst = false;
                } else {
                    mBuilder.append(RECIPIENT_HEADING_DELIMITER);
                }
                mBuilder.append(recipientList);
                mRecipientCount += Math.min(addLimit, recipients.length);
            }
        }

        private CharSequence getSummaryTextForHeading(int headingStrRes, String[] rawAddrs,
                int maxToCopy) {
            if (rawAddrs == null || rawAddrs.length == 0 || maxToCopy == 0) {
                return null;
            }

            SpannableStringBuilder ssb = new SpannableStringBuilder(
                    mContext.getString(headingStrRes));
            ssb.setSpan(new StyleSpan(Typeface.NORMAL), 0, ssb.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            final int len = Math.min(maxToCopy, rawAddrs.length);
            boolean first = true;
            for (int i = 0; i < len; i++) {
                final Address email = getAddress(mAddressCache, rawAddrs[i]);
                final String emailAddress = email.getAddress();
                final String name;
                if (mMatcher != null && mMatcher.isVeiledAddress(emailAddress)) {
                    if (TextUtils.isEmpty(email.getName())) {
                        // Let's write something more readable.
                        name = mContext.getString(VeiledAddressMatcher.VEILED_SUMMARY_UNKNOWN);
                    } else {
                        name = email.getSimplifiedName();
                    }
                } else {
                    // Not a veiled address, show first part of email, or "me".
                    name = mMe.equals(emailAddress) ? mMyName : email.getSimplifiedName();
                }

                // duplicate TextUtils.join() logic to minimize temporary
                // allocations, and because we need to support spans
                if (first) {
                    first = false;
                } else {
                    ssb.append(mComma);
                }
                ssb.append(name);
            }

            return ssb;
        }

        public CharSequence build() {
            return mBuilder;
        }
    }

    @VisibleForTesting
    static CharSequence getRecipientSummaryText(Context context, String me, String myName,
            String[] to, String[] cc, String[] bcc, Map<String, Address> addressCache,
            VeiledAddressMatcher matcher) {

        final RecipientListsBuilder builder =
                new RecipientListsBuilder(context, me, myName, addressCache, matcher);

        builder.append(to, R.string.to_heading);
        builder.append(cc, R.string.cc_heading);
        builder.append(bcc, R.string.bcc_heading);

        return builder.build();
    }

    private void updateContactInfo() {
        if (mContactInfoSource == null || mSender == null) {
            mPhotoView.setImageToDefault();
            mPhotoView.setContentDescription(getResources().getString(
                    R.string.contact_info_string_default));
            return;
        }

        // Set the photo to either a found Bitmap or the default
        // and ensure either the contact URI or email is set so the click
        // handling works
        String contentDesc = getResources().getString(R.string.contact_info_string,
                !TextUtils.isEmpty(mSender.getName()) ? mSender.getName() : mSender.getAddress());
        mPhotoView.setContentDescription(contentDesc);
        boolean photoSet = false;
        final String email = mSender.getAddress();
        final ContactInfo info = mContactInfoSource.getContactInfo(email);
        if (info != null) {
            mPhotoView.assignContactUri(info.contactUri);
            if (info.photo != null) {
                mPhotoView.setImageBitmap(info.photo);
                photoSet = true;
            }
        } else {
            mPhotoView.assignContactFromEmail(email, true /* lazyLookup */);
        }

        if (!photoSet) {
            mPhotoView.setImageBitmap(makeLetterTile(mSender.getName(), email));
        }
    }

    private Bitmap makeLetterTile(
            String displayName, String senderAddress) {
        if (mLetterTileProvider == null) {
            mLetterTileProvider = new LetterTileProvider(getContext());
        }

        final ImageCanvas.Dimensions dimensions = new ImageCanvas.Dimensions(
                mContactPhotoWidth, mContactPhotoHeight, ImageCanvas.Dimensions.SCALE_ONE);
        return mLetterTileProvider.getLetterTile(dimensions, displayName, senderAddress);
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        mPopup.dismiss();
        return onClick(null, item.getItemId());
    }

    @Override
    public void onClick(View v) {
        onClick(v, v.getId());
    }

    /**
     * Handles clicks on either views or menu items. View parameter can be null
     * for menu item clicks.
     */
    public boolean onClick(final View v, final int id) {
        if (mMessage == null) {
            LogUtils.i(LOG_TAG, "ignoring message header tap on unbound view");
            return false;
        }

        boolean handled = true;

        if (id == R.id.reply) {
            ComposeActivity.reply(getContext(), getAccount(), mMessage);
        } else if (id == R.id.reply_all) {
            ComposeActivity.replyAll(getContext(), getAccount(), mMessage);
        } else if (id == R.id.forward) {
            ComposeActivity.forward(getContext(), getAccount(), mMessage);
        } else if (id == R.id.report_rendering_problem) {
            final String text = getContext().getString(R.string.report_rendering_problem_desc);
            ComposeActivity.reportRenderingFeedback(getContext(), getAccount(), mMessage,
                text + "\n\n" + mCallbacks.getMessageTransforms(mMessage));
        } else if (id == R.id.report_rendering_improvement) {
            final String text = getContext().getString(R.string.report_rendering_improvement_desc);
            ComposeActivity.reportRenderingFeedback(getContext(), getAccount(), mMessage,
                text + "\n\n" + mCallbacks.getMessageTransforms(mMessage));
        } else if (id == R.id.star) {
            final boolean newValue = !v.isSelected();
            v.setSelected(newValue);
            mMessage.star(newValue);
        } else if (id == R.id.edit_draft) {
            ComposeActivity.editDraft(getContext(), getAccount(), mMessage);
        } else if (id == R.id.overflow) {
            if (mPopup == null) {
                mPopup = new PopupMenu(getContext(), v);
                mPopup.getMenuInflater().inflate(R.menu.message_header_overflow_menu,
                        mPopup.getMenu());
                mPopup.setOnMenuItemClickListener(this);
            }
            final boolean defaultReplyAll = getAccount().settings.replyBehavior
                    == UIProvider.DefaultReplyBehavior.REPLY_ALL;
            final Menu m = mPopup.getMenu();
            m.findItem(R.id.reply).setVisible(defaultReplyAll);
            m.findItem(R.id.reply_all).setVisible(!defaultReplyAll);

            final boolean reportRendering = ENABLE_REPORT_RENDERING_PROBLEM
                && mCallbacks.supportsMessageTransforms();
            m.findItem(R.id.report_rendering_improvement).setVisible(reportRendering);
            m.findItem(R.id.report_rendering_problem).setVisible(reportRendering);

            mPopup.show();
        } else if (id == R.id.details_collapsed_content
                || id == R.id.details_expanded_content) {
            toggleMessageDetails(v);
        } else if (id == R.id.upper_header) {
            toggleExpanded();
        } else if (id == R.id.show_pictures_text) {
            handleShowImagePromptClick(v);
        } else {
            LogUtils.i(LOG_TAG, "unrecognized header tap: %d", id);
            handled = false;
        }
        return handled;
    }

    /**
     * Set to true if the user should not be able to perfrom message actions
     * on the message such as reply/reply all/forward/star/etc.
     *
     * Default is false.
     */
    public void setViewOnlyMode(boolean isViewOnlyMode) {
        mIsViewOnlyMode = isViewOnlyMode;
    }

    public void setExpandable(boolean expandable) {
        mExpandable = expandable;
    }

    public void toggleExpanded() {
        if (!mExpandable) {
            return;
        }
        setExpanded(!isExpanded());

        // The snappy header will disappear; no reason to update text.
        if (!mIsSnappy) {
            mSenderNameView.setText(getHeaderTitle());
            mSenderEmailView.setText(getHeaderSubtitle());
            mDateView.setText(mMessageHeaderItem.getTimestampLong());
            mSnippetView.setText(mSnippet);
        }

        updateChildVisibility();

        final BorderHeights borderHeights = updateBorderExpandedState();

        // Force-measure the new header height so we can set the spacer size and
        // reveal the message div in one pass. Force-measuring makes it unnecessary to set
        // mSizeChanged.
        int h = measureHeight();
        mMessageHeaderItem.setHeight(h);
        if (mCallbacks != null) {
            mCallbacks.setMessageExpanded(mMessageHeaderItem, h,
                    borderHeights.topHeight, borderHeights.bottomHeight);
        }
    }

    /**
     * Checks the neighboring messages to this message and
     * updates the {@link BorderItem}s of the borders of this message
     * in case they should be collapsed or expanded.
     * @return a {@link BorderHeights} object containing
     * the new heights of the top and bottom borders.
     */
    private BorderHeights updateBorderExpandedState() {
        final int position = mMessageHeaderItem.getPosition();
        final boolean isExpanded = mMessageHeaderItem.isExpanded();
        final int abovePosition = position - 2; // position of MessageFooterItem above header
        final int belowPosition = position + 3; // position of next MessageHeaderItem
        final ConversationViewAdapter adapter = mMessageHeaderItem.getAdapter();
        final int size = adapter.getCount();
        final BorderHeights borderHeights = new BorderHeights();

        // if an above message exists, update the border above this message
        if (isValidPosition(abovePosition, size)) {
            final ConversationOverlayItem item = adapter.getItem(abovePosition);
            final int type = item.getType();
            if (type == ConversationViewAdapter.VIEW_TYPE_MESSAGE_FOOTER ||
                    type == ConversationViewAdapter.VIEW_TYPE_SUPER_COLLAPSED_BLOCK) {
                final BorderItem borderItem = (BorderItem) adapter.getItem(abovePosition + 1);
                final boolean borderIsExpanded = isExpanded || item.isExpanded();
                borderItem.setExpanded(borderIsExpanded);
                borderHeights.topHeight = borderIsExpanded ?
                        BorderView.getExpandedHeight() : BorderView.getCollapsedHeight();
                borderItem.setHeight(borderHeights.topHeight);
            }
        }


        // if a below message exists, update the border below this message
        if (isValidPosition(belowPosition, size)) {
            final ConversationOverlayItem item = adapter.getItem(belowPosition);
            if (item.getType() == ConversationViewAdapter.VIEW_TYPE_MESSAGE_HEADER) {
                final BorderItem borderItem = (BorderItem) adapter.getItem(belowPosition - 1);
                final boolean borderIsExpanded = isExpanded || item.isExpanded();
                borderItem.setExpanded(borderIsExpanded);
                borderHeights.bottomHeight = borderIsExpanded ?
                        BorderView.getExpandedHeight() : BorderView.getCollapsedHeight();
                borderItem.setHeight(borderHeights.bottomHeight);
            }
        }

        return borderHeights;
    }

    /**
     * A plain-old-data class used to return the new heights of the top and bottom borders
     * in {@link #updateBorderExpandedState()}.
     * If {@link #topHeight} or {@link #bottomHeight} are -1 after returning,
     * do not update the heights of the spacer for their respective borders
     * as their state has not changed.
     */
    private class BorderHeights {
        public int topHeight = -1;
        public int bottomHeight = -1;
    }

    private boolean isValidPosition(int position, int size) {
        return position >= 0 && position < size;
    }

    private void toggleMessageDetails(View visibleDetailsView) {
        int heightBefore = measureHeight();
        final boolean detailsExpanded = (visibleDetailsView == mCollapsedDetailsView);
        setMessageDetailsExpanded(detailsExpanded);
        updateSpacerHeight();
        if (mCallbacks != null) {
            mCallbacks.setMessageDetailsExpanded(mMessageHeaderItem, detailsExpanded, heightBefore);
        }
    }

    private void setMessageDetailsExpanded(boolean expand) {
        if (mExpandMode == DEFAULT_MODE) {
            if (expand) {
                showExpandedDetails();
                hideCollapsedDetails();
            } else {
                hideExpandedDetails();
                showCollapsedDetails();
            }
        } else if (mExpandMode == POPUP_MODE) {
            if (expand) {
                showDetailsPopup();
            } else {
                hideDetailsPopup();
                showCollapsedDetails();
            }
        }
        if (mMessageHeaderItem != null) {
            mMessageHeaderItem.detailsExpanded = expand;
        }
    }

    public void setMessageDetailsVisibility(int vis) {
        if (vis == GONE) {
            hideCollapsedDetails();
            hideExpandedDetails();
            hideSpamWarning();
            hideShowImagePrompt();
            hideInvite();
            mUpperHeaderView.setOnCreateContextMenuListener(null);
        } else {
            setMessageDetailsExpanded(mMessageHeaderItem.detailsExpanded);
            if (mMessage.spamWarningString == null) {
                hideSpamWarning();
            } else {
                showSpamWarning();
            }
            if (mShowImagePrompt) {
                if (mMessageHeaderItem.getShowImages()) {
                    showImagePromptAlways(true);
                } else {
                    showImagePromptOnce();
                }
            } else {
                hideShowImagePrompt();
            }
            if (mMessage.isFlaggedCalendarInvite()) {
                showInvite();
            } else {
                hideInvite();
            }
            mUpperHeaderView.setOnCreateContextMenuListener(mEmailCopyMenu);
        }
    }

    public void hideMessageDetails() {
        setMessageDetailsVisibility(GONE);
    }

    private void hideCollapsedDetails() {
        if (mCollapsedDetailsView != null) {
            mCollapsedDetailsView.setVisibility(GONE);
        }
    }

    private void hideExpandedDetails() {
        if (mExpandedDetailsView != null) {
            mExpandedDetailsView.setVisibility(GONE);
        }
    }

    private void hideInvite() {
        if (mInviteView != null) {
            mInviteView.setVisibility(GONE);
        }
    }

    private void showInvite() {
        if (mInviteView == null) {
            mInviteView = (MessageInviteView) mInflater.inflate(
                    R.layout.conversation_message_invite, this, false);
            mExtraContentView.addView(mInviteView);
        }
        mInviteView.bind(mMessage);
        mInviteView.setVisibility(VISIBLE);
    }

    private void hideShowImagePrompt() {
        if (mImagePromptView != null) {
            mImagePromptView.setVisibility(GONE);
        }
    }

    private void showImagePromptOnce() {
        if (mImagePromptView == null) {
            mImagePromptView = (TextView) mInflater.inflate(
                    R.layout.conversation_message_show_pics, this, false);
            mExtraContentView.addView(mImagePromptView);
            mImagePromptView.setOnClickListener(this);
        }
        mImagePromptView.setVisibility(VISIBLE);
        mImagePromptView.setText(R.string.show_images);
        mImagePromptView.setTag(SHOW_IMAGE_PROMPT_ONCE);
    }

    /**
     * Shows the "Always show pictures" message
     *
     * @param initialShowing <code>true</code> if this is the first time we are showing the prompt
     *        for "show images", <code>false</code> if we are transitioning from "Show pictures"
     */
    private void showImagePromptAlways(final boolean initialShowing) {
        if (initialShowing) {
            // Initialize the view
            showImagePromptOnce();
        }

        mImagePromptView.setText(R.string.always_show_images);
        mImagePromptView.setTag(SHOW_IMAGE_PROMPT_ALWAYS);

        if (!initialShowing) {
            // the new text's line count may differ, so update the spacer height
            updateSpacerHeight();
        }
    }

    private void hideSpamWarning() {
        if (mSpamWarningView != null) {
            mSpamWarningView.setVisibility(GONE);
        }
    }

    private void showSpamWarning() {
        if (mSpamWarningView == null) {
            mSpamWarningView = (SpamWarningView)
                    mInflater.inflate(R.layout.conversation_message_spam_warning, this, false);
            mExtraContentView.addView(mSpamWarningView);
        }

        mSpamWarningView.showSpamWarning(mMessage, mSender);
    }

    private void handleShowImagePromptClick(View v) {
        Integer state = (Integer) v.getTag();
        if (state == null) {
            return;
        }
        switch (state) {
            case SHOW_IMAGE_PROMPT_ONCE:
                if (mCallbacks != null) {
                    mCallbacks.showExternalResources(mMessage);
                }
                if (mMessageHeaderItem != null) {
                    mMessageHeaderItem.setShowImages(true);
                }
                if (mIsViewOnlyMode) {
                    hideShowImagePrompt();
                } else {
                    showImagePromptAlways(false);
                }
                break;
            case SHOW_IMAGE_PROMPT_ALWAYS:
                mMessage.markAlwaysShowImages(getQueryHandler(), 0 /* token */, null /* cookie */);

                if (mCallbacks != null) {
                    mCallbacks.showExternalResources(mMessage.getFrom());
                }

                mShowImagePrompt = false;
                v.setTag(null);
                v.setVisibility(GONE);
                updateSpacerHeight();
                Toast.makeText(getContext(), R.string.always_show_images_toast, Toast.LENGTH_SHORT)
                        .show();
                break;
        }
    }

    private AsyncQueryHandler getQueryHandler() {
        if (mQueryHandler == null) {
            mQueryHandler = new AsyncQueryHandler(getContext().getContentResolver()) {};
        }
        return mQueryHandler;
    }

    /**
     * Makes collapsed details visible. If necessary, will inflate details
     * layout and render using saved-off state (senders, timestamp, etc).
     */
    private void showCollapsedDetails() {
        if (mCollapsedDetailsView == null) {
            mCollapsedDetailsView = (ViewGroup) mInflater.inflate(
                    R.layout.conversation_message_details_header, this, false);
            mExtraContentView.addView(mCollapsedDetailsView, 0);
            mCollapsedDetailsView.setOnClickListener(this);
        }
        if (!mCollapsedDetailsValid) {
            if (mMessageHeaderItem.recipientSummaryText == null) {
                final Account account = getAccount();
                final String name = (account != null) ? account.name : "";
                mMessageHeaderItem.recipientSummaryText = getRecipientSummaryText(getContext(),
                        name, mMyName, mTo, mCc, mBcc, mAddressCache, mVeiledMatcher);
            }
            ((TextView) findViewById(R.id.recipients_summary))
                    .setText(mMessageHeaderItem.recipientSummaryText);

            mCollapsedDetailsValid = true;
        }
        mCollapsedDetailsView.setVisibility(VISIBLE);
    }

    /**
     * Makes expanded details visible. If necessary, will inflate expanded
     * details layout and render using saved-off state (senders, timestamp,
     * etc).
     */
    private void showExpandedDetails() {
        // lazily create expanded details view
        final boolean expandedViewCreated = ensureExpandedDetailsView();
        if (expandedViewCreated) {
            mExtraContentView.addView(mExpandedDetailsView, 0);
        }
        mExpandedDetailsView.setVisibility(VISIBLE);
    }

    private boolean ensureExpandedDetailsView() {
        boolean viewCreated = false;
        if (mExpandedDetailsView == null) {
            View v = inflateExpandedDetails(mInflater);
            v.setOnClickListener(this);

            mExpandedDetailsView = (ViewGroup) v;
            viewCreated = true;
        }
        if (!mExpandedDetailsValid) {
            renderExpandedDetails(getResources(), mExpandedDetailsView, mMessage.viaDomain,
                    mAddressCache, getAccount(), mVeiledMatcher, mFrom, mReplyTo, mTo, mCc, mBcc,
                    mMessageHeaderItem.getTimestampLong());

            mExpandedDetailsValid = true;
        }
        return viewCreated;
    }

    public static View inflateExpandedDetails(LayoutInflater inflater) {
        return inflater.inflate(R.layout.conversation_message_details_header_expanded, null,
                false);
    }

    public static void renderExpandedDetails(Resources res, View detailsView,
            String viaDomain, Map<String, Address> addressCache, Account account,
            VeiledAddressMatcher veiledMatcher, String[] from, String[] replyTo,
            String[] to, String[] cc, String[] bcc, CharSequence receivedTimestamp) {
        renderEmailList(res, R.id.from_heading, R.id.from_details, from, viaDomain,
                detailsView, addressCache, account, veiledMatcher);
        renderEmailList(res, R.id.replyto_heading, R.id.replyto_details, replyTo, viaDomain,
                detailsView, addressCache, account, veiledMatcher);
        renderEmailList(res, R.id.to_heading, R.id.to_details, to, viaDomain,
                detailsView, addressCache, account, veiledMatcher);
        renderEmailList(res, R.id.cc_heading, R.id.cc_details, cc, viaDomain,
                detailsView, addressCache, account, veiledMatcher);
        renderEmailList(res, R.id.bcc_heading, R.id.bcc_details, bcc, viaDomain,
                detailsView, addressCache, account, veiledMatcher);

        // Render date
        detailsView.findViewById(R.id.date_heading).setVisibility(VISIBLE);
        final TextView date = (TextView) detailsView.findViewById(R.id.date_details);
        date.setText(receivedTimestamp);
        date.setVisibility(VISIBLE);
    }

    /**
     * Render an email list for the expanded message details view.
     */
    private static void renderEmailList(Resources res, int headerId, int detailsId,
            String[] emails, String viaDomain, View rootView,
            Map<String, Address> addressCache, Account account,
            VeiledAddressMatcher veiledMatcher) {
        if (emails == null || emails.length == 0) {
            return;
        }
        final String[] formattedEmails = new String[emails.length];
        for (int i = 0; i < emails.length; i++) {
            final Address email = getAddress(addressCache, emails[i]);
            String name = email.getName();
            final String address = email.getAddress();
            // Check if the address here is a veiled address.  If it is, we need to display an
            // alternate layout
            final boolean isVeiledAddress = veiledMatcher != null &&
                    veiledMatcher.isVeiledAddress(address);
            final String addressShown;
            if (isVeiledAddress) {
                // Add the warning at the end of the name, and remove the address.  The alternate
                // text cannot be put in the address part, because the address is made into a link,
                // and the alternate human-readable text is not a link.
                addressShown = "";
                if (TextUtils.isEmpty(name)) {
                    // Empty name and we will block out the address. Let's write something more
                    // readable.
                    name = res.getString(VeiledAddressMatcher.VEILED_ALTERNATE_TEXT_UNKNOWN_PERSON);
                } else {
                    name = name + res.getString(VeiledAddressMatcher.VEILED_ALTERNATE_TEXT);
                }
            } else {
                addressShown = address;
            }
            if (name == null || name.length() == 0) {
                formattedEmails[i] = addressShown;
            } else {
                // The one downside to having the showViaDomain here is that
                // if the sender does not have a name, it will not show the via info
                if (viaDomain != null) {
                    formattedEmails[i] = res.getString(
                            R.string.address_display_format_with_via_domain,
                            name, addressShown, viaDomain);
                } else {
                    formattedEmails[i] = res.getString(R.string.address_display_format,
                            name, addressShown);
                }
            }
        }

        rootView.findViewById(headerId).setVisibility(VISIBLE);
        final TextView detailsText = (TextView) rootView.findViewById(detailsId);
        detailsText.setText(TextUtils.join("\n", formattedEmails));
        stripUnderlines(detailsText, account);
        detailsText.setVisibility(VISIBLE);
    }

    private static void stripUnderlines(TextView textView, Account account) {
        final Spannable spannable = (Spannable) textView.getText();
        final URLSpan[] urls = textView.getUrls();

        for (URLSpan span : urls) {
            final int start = spannable.getSpanStart(span);
            final int end = spannable.getSpanEnd(span);
            spannable.removeSpan(span);
            span = new EmailAddressSpan(account, span.getURL().substring(7));
            spannable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void showDetailsPopup() {
        final FragmentManager manager = mCallbacks.getFragmentManager();
        mDetailsPopup = (DialogFragment) manager.findFragmentByTag(DETAILS_DIALOG_TAG);
        if (mDetailsPopup == null) {
            mDetailsPopup = MessageHeaderDetailsDialogFragment.newInstance(
                    mAddressCache, getAccount(), mFrom, mReplyTo, mTo, mCc, mBcc,
                    mMessageHeaderItem.getTimestampLong());
            mDetailsPopup.show(manager, DETAILS_DIALOG_TAG);
        }
    }

    private void hideDetailsPopup() {
        if (mDetailsPopup != null) {
            mDetailsPopup.dismiss();
            mDetailsPopup = null;
        }
    }

    /**
     * Returns a short plaintext snippet generated from the given HTML message
     * body. Collapses whitespace, ignores '&lt;' and '&gt;' characters and
     * everything in between, and truncates the snippet to no more than 100
     * characters.
     *
     * @return Short plaintext snippet
     */
    @VisibleForTesting
    static String makeSnippet(final String messageBody) {
        if (TextUtils.isEmpty(messageBody)) {
            return null;
        }

        final StringBuilder snippet = new StringBuilder(MAX_SNIPPET_LENGTH);

        final StringReader reader = new StringReader(messageBody);
        try {
            int c;
            while ((c = reader.read()) != -1 && snippet.length() < MAX_SNIPPET_LENGTH) {
                // Collapse whitespace.
                if (Character.isWhitespace(c)) {
                    snippet.append(' ');
                    do {
                        c = reader.read();
                    } while (Character.isWhitespace(c));
                    if (c == -1) {
                        break;
                    }
                }

                if (c == '<') {
                    // Ignore everything up to and including the next '>'
                    // character.
                    while ((c = reader.read()) != -1) {
                        if (c == '>') {
                            break;
                        }
                    }

                    // If we reached the end of the message body, exit.
                    if (c == -1) {
                        break;
                    }
                } else if (c == '&') {
                    // Read HTML entity.
                    StringBuilder sb = new StringBuilder();

                    while ((c = reader.read()) != -1) {
                        if (c == ';') {
                            break;
                        }
                        sb.append((char) c);
                    }

                    String entity = sb.toString();
                    if ("nbsp".equals(entity)) {
                        snippet.append(' ');
                    } else if ("lt".equals(entity)) {
                        snippet.append('<');
                    } else if ("gt".equals(entity)) {
                        snippet.append('>');
                    } else if ("amp".equals(entity)) {
                        snippet.append('&');
                    } else if ("quot".equals(entity)) {
                        snippet.append('"');
                    } else if ("apos".equals(entity) || "#39".equals(entity)) {
                        snippet.append('\'');
                    } else {
                        // Unknown entity; just append the literal string.
                        snippet.append('&').append(entity);
                        if (c == ';') {
                            snippet.append(';');
                        }
                    }

                    // If we reached the end of the message body, exit.
                    if (c == -1) {
                        break;
                    }
                } else {
                    // The current character is a non-whitespace character that
                    // isn't inside some
                    // HTML tag and is not part of an HTML entity.
                    snippet.append((char) c);
                }
            }
        } catch (IOException e) {
            LogUtils.wtf(LOG_TAG, e, "Really? IOException while reading a freaking string?!? ");
        }

        return snippet.toString();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Timer perf = new Timer();
        perf.start(LAYOUT_TAG);
        super.onLayout(changed, l, t, r, b);
        perf.pause(LAYOUT_TAG);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Timer t = new Timer();
        if (Timer.ENABLE_TIMER && !mPreMeasuring) {
            t.count("header measure id=" + mMessage.id);
            t.start(MEASURE_TAG);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!mPreMeasuring) {
            t.pause(MEASURE_TAG);
        }
    }
}
