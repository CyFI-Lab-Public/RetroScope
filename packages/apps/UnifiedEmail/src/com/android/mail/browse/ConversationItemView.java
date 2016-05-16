/*
 * Copyright (C) 2012 Google Inc.
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

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout.Alignment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.format.DateUtils;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.TextView;

import com.android.mail.R;
import com.android.mail.R.drawable;
import com.android.mail.R.integer;
import com.android.mail.analytics.Analytics;
import com.android.mail.bitmap.AttachmentDrawable;
import com.android.mail.bitmap.AttachmentGridDrawable;
import com.android.mail.browse.ConversationItemViewModel.SenderFragment;
import com.android.mail.perf.Timer;
import com.android.mail.photomanager.ContactPhotoManager;
import com.android.mail.photomanager.ContactPhotoManager.ContactIdentifier;
import com.android.mail.photomanager.PhotoManager.PhotoIdentifier;
import com.android.mail.providers.Address;
import com.android.mail.providers.Attachment;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.providers.UIProvider;
import com.android.mail.providers.UIProvider.AttachmentRendition;
import com.android.mail.providers.UIProvider.ConversationColumns;
import com.android.mail.providers.UIProvider.ConversationListIcon;
import com.android.mail.providers.UIProvider.FolderType;
import com.android.mail.ui.AnimatedAdapter;
import com.android.mail.ui.AnimatedAdapter.ConversationListListener;
import com.android.mail.ui.ControllableActivity;
import com.android.mail.ui.ConversationSelectionSet;
import com.android.mail.ui.DividedImageCanvas;
import com.android.mail.ui.DividedImageCanvas.InvalidateCallback;
import com.android.mail.ui.FolderDisplayer;
import com.android.mail.ui.SwipeableItemView;
import com.android.mail.ui.SwipeableListView;
import com.android.mail.ui.ViewMode;
import com.android.mail.utils.FolderUri;
import com.android.mail.utils.HardwareLayerEnabler;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

public class ConversationItemView extends View
        implements SwipeableItemView, ToggleableItem, InvalidateCallback, OnScrollListener {

    // Timer.
    private static int sLayoutCount = 0;
    private static Timer sTimer; // Create the sTimer here if you need to do
                                 // perf analysis.
    private static final int PERF_LAYOUT_ITERATIONS = 50;
    private static final String PERF_TAG_LAYOUT = "CCHV.layout";
    private static final String PERF_TAG_CALCULATE_TEXTS_BITMAPS = "CCHV.txtsbmps";
    private static final String PERF_TAG_CALCULATE_SENDER_SUBJECT = "CCHV.sendersubj";
    private static final String PERF_TAG_CALCULATE_FOLDERS = "CCHV.folders";
    private static final String PERF_TAG_CALCULATE_COORDINATES = "CCHV.coordinates";
    private static final String LOG_TAG = LogTag.getLogTag();

    // Static bitmaps.
    private static Bitmap STAR_OFF;
    private static Bitmap STAR_ON;
    private static Bitmap CHECK;
    private static Bitmap ATTACHMENT;
    private static Bitmap ONLY_TO_ME;
    private static Bitmap TO_ME_AND_OTHERS;
    private static Bitmap IMPORTANT_ONLY_TO_ME;
    private static Bitmap IMPORTANT_TO_ME_AND_OTHERS;
    private static Bitmap IMPORTANT_TO_OTHERS;
    private static Bitmap STATE_REPLIED;
    private static Bitmap STATE_FORWARDED;
    private static Bitmap STATE_REPLIED_AND_FORWARDED;
    private static Bitmap STATE_CALENDAR_INVITE;
    private static Bitmap VISIBLE_CONVERSATION_CARET;
    private static Drawable RIGHT_EDGE_TABLET;
    private static Drawable PLACEHOLDER;
    private static Drawable PROGRESS_BAR;

    private static String sSendersSplitToken;
    private static String sElidedPaddingToken;

    // Static colors.
    private static int sSendersTextColorRead;
    private static int sSendersTextColorUnread;
    private static int sDateTextColor;
    private static int sStarTouchSlop;
    private static int sSenderImageTouchSlop;
    private static int sShrinkAnimationDuration;
    private static int sSlideAnimationDuration;
    private static int sOverflowCountMax;
    private static int sCabAnimationDuration;

    // Static paints.
    private static final TextPaint sPaint = new TextPaint();
    private static final TextPaint sFoldersPaint = new TextPaint();
    private static final Paint sCheckBackgroundPaint = new Paint();

    // Backgrounds for different states.
    private final SparseArray<Drawable> mBackgrounds = new SparseArray<Drawable>();

    // Dimensions and coordinates.
    private int mViewWidth = -1;
    /** The view mode at which we calculated mViewWidth previously. */
    private int mPreviousMode;

    private int mInfoIconX;
    private int mDateX;
    private int mPaperclipX;
    private int mSendersWidth;

    /** Whether we are on a tablet device or not */
    private final boolean mTabletDevice;
    /** Whether we are on an expansive tablet */
    private final boolean mIsExpansiveTablet;
    /** When in conversation mode, true if the list is hidden */
    private final boolean mListCollapsible;

    @VisibleForTesting
    ConversationItemViewCoordinates mCoordinates;

    private ConversationItemViewCoordinates.Config mConfig;

    private final Context mContext;

    public ConversationItemViewModel mHeader;
    private boolean mDownEvent;
    private boolean mSelected = false;
    private ConversationSelectionSet mSelectedConversationSet;
    private Folder mDisplayedFolder;
    private boolean mStarEnabled;
    private boolean mSwipeEnabled;
    private int mLastTouchX;
    private int mLastTouchY;
    private AnimatedAdapter mAdapter;
    private float mAnimatedHeightFraction = 1.0f;
    private final String mAccount;
    private ControllableActivity mActivity;
    private ConversationListListener mConversationListListener;
    private final TextView mSubjectTextView;
    private final TextView mSendersTextView;
    private int mGadgetMode;
    private boolean mAttachmentPreviewsEnabled;
    private boolean mParallaxSpeedAlternative;
    private boolean mParallaxDirectionAlternative;
    private final DividedImageCanvas mContactImagesHolder;
    private static ContactPhotoManager sContactPhotoManager;

    private static int sFoldersLeftPadding;
    private static TextAppearanceSpan sSubjectTextUnreadSpan;
    private static TextAppearanceSpan sSubjectTextReadSpan;
    private static ForegroundColorSpan sSnippetTextUnreadSpan;
    private static ForegroundColorSpan sSnippetTextReadSpan;
    private static int sScrollSlop;
    private static CharacterStyle sActivatedTextSpan;

    private final AttachmentGridDrawable mAttachmentsView;

    private final Matrix mPhotoFlipMatrix = new Matrix();
    private final Matrix mCheckMatrix = new Matrix();

    private final CabAnimator mPhotoFlipAnimator;

    /**
     * The conversation id, if this conversation was selected the last time we were in a selection
     * mode. This is reset after any animations complete upon exiting the selection mode.
     */
    private long mLastSelectedId = -1;

    /** The resource id of the color to use to override the background. */
    private int mBackgroundOverrideResId = -1;
    /** The bitmap to use, or <code>null</code> for the default */
    private Bitmap mPhotoBitmap = null;
    private Rect mPhotoRect = null;

    /**
     * A listener for clicks on the various areas of a conversation item.
     */
    public interface ConversationItemAreaClickListener {
        /** Called when the info icon is clicked. */
        void onInfoIconClicked();

        /** Called when the star is clicked. */
        void onStarClicked();
    }

    /** If set, it will steal all clicks for which the interface has a click method. */
    private ConversationItemAreaClickListener mConversationItemAreaClickListener = null;

    static {
        sPaint.setAntiAlias(true);
        sFoldersPaint.setAntiAlias(true);

        sCheckBackgroundPaint.setColor(Color.GRAY);
    }

    public static void setScrollStateChanged(final int scrollState) {
        if (sContactPhotoManager == null) {
            return;
        }
        final boolean flinging = scrollState == OnScrollListener.SCROLL_STATE_FLING;

        if (flinging) {
            sContactPhotoManager.pause();
        } else {
            sContactPhotoManager.resume();
        }
    }

    /**
     * Handles displaying folders in a conversation header view.
     */
    static class ConversationItemFolderDisplayer extends FolderDisplayer {

        private int mFoldersCount;

        public ConversationItemFolderDisplayer(Context context) {
            super(context);
        }

        @Override
        public void loadConversationFolders(Conversation conv, final FolderUri ignoreFolderUri,
                final int ignoreFolderType) {
            super.loadConversationFolders(conv, ignoreFolderUri, ignoreFolderType);
            mFoldersCount = mFoldersSortedSet.size();
        }

        @Override
        public void reset() {
            super.reset();
            mFoldersCount = 0;
        }

        public boolean hasVisibleFolders() {
            return mFoldersCount > 0;
        }

        private int measureFolders(int availableSpace, int cellSize) {
            int totalWidth = 0;
            boolean firstTime = true;
            for (Folder f : mFoldersSortedSet) {
                final String folderString = f.name;
                int width = (int) sFoldersPaint.measureText(folderString) + cellSize;
                if (firstTime) {
                    firstTime = false;
                } else {
                    width += sFoldersLeftPadding;
                }
                totalWidth += width;
                if (totalWidth > availableSpace) {
                    break;
                }
            }

            return totalWidth;
        }

        public void drawFolders(Canvas canvas, ConversationItemViewCoordinates coordinates) {
            if (mFoldersCount == 0) {
                return;
            }
            final int xMinStart = coordinates.foldersX;
            final int xEnd = coordinates.foldersXEnd;
            final int y = coordinates.foldersY;
            final int height = coordinates.foldersHeight;
            int textBottomPadding = coordinates.foldersTextBottomPadding;

            sFoldersPaint.setTextSize(coordinates.foldersFontSize);
            sFoldersPaint.setTypeface(coordinates.foldersTypeface);

            // Initialize space and cell size based on the current mode.
            int availableSpace = xEnd - xMinStart;
            int maxFoldersCount = availableSpace / coordinates.getFolderMinimumWidth();
            int foldersCount = Math.min(mFoldersCount, maxFoldersCount);
            int averageWidth = availableSpace / foldersCount;
            int cellSize = coordinates.getFolderCellWidth();

            // TODO(ath): sFoldersPaint.measureText() is done 3x in this method. stop that.
            // Extra credit: maybe cache results across items as long as font size doesn't change.

            final int totalWidth = measureFolders(availableSpace, cellSize);
            int xStart = xEnd - Math.min(availableSpace, totalWidth);
            final boolean overflow = totalWidth > availableSpace;

            // Second pass to draw folders.
            int i = 0;
            for (Folder f : mFoldersSortedSet) {
                if (availableSpace <= 0) {
                    break;
                }
                final String folderString = f.name;
                final int fgColor = f.getForegroundColor(mDefaultFgColor);
                final int bgColor = f.getBackgroundColor(mDefaultBgColor);
                boolean labelTooLong = false;
                final int textW = (int) sFoldersPaint.measureText(folderString);
                int width = textW + cellSize + sFoldersLeftPadding;

                if (overflow && width > averageWidth) {
                    if (i < foldersCount - 1) {
                        width = averageWidth;
                    } else {
                        // allow the last label to take all remaining space
                        // (and don't let it make room for padding)
                        width = availableSpace + sFoldersLeftPadding;
                    }
                    labelTooLong = true;
                }

                // TODO (mindyp): how to we get this?
                final boolean isMuted = false;
                // labelValues.folderId ==
                // sGmail.getFolderMap(mAccount).getFolderIdIgnored();

                // Draw the box.
                sFoldersPaint.setColor(bgColor);
                sFoldersPaint.setStyle(Paint.Style.FILL);
                canvas.drawRect(xStart, y, xStart + width - sFoldersLeftPadding,
                        y + height, sFoldersPaint);

                // Draw the text.
                final int padding = cellSize / 2;
                sFoldersPaint.setColor(fgColor);
                sFoldersPaint.setStyle(Paint.Style.FILL);
                if (labelTooLong) {
                    final int rightBorder = xStart + width - sFoldersLeftPadding - padding;
                    final Shader shader = new LinearGradient(rightBorder - padding, y, rightBorder,
                            y, fgColor, Utils.getTransparentColor(fgColor), Shader.TileMode.CLAMP);
                    sFoldersPaint.setShader(shader);
                }
                canvas.drawText(folderString, xStart + padding, y + height - textBottomPadding,
                        sFoldersPaint);
                if (labelTooLong) {
                    sFoldersPaint.setShader(null);
                }

                availableSpace -= width;
                xStart += width;
                i++;
            }
        }
    }

    public ConversationItemView(Context context, String account) {
        super(context);
        Utils.traceBeginSection("CIVC constructor");
        setClickable(true);
        setLongClickable(true);
        mContext = context.getApplicationContext();
        final Resources res = mContext.getResources();
        mTabletDevice = Utils.useTabletUI(res);
        mIsExpansiveTablet =
                mTabletDevice ? res.getBoolean(R.bool.use_expansive_tablet_ui) : false;
        mListCollapsible = res.getBoolean(R.bool.list_collapsible);
        mAccount = account;

        if (STAR_OFF == null) {
            // Initialize static bitmaps.
            STAR_OFF = BitmapFactory.decodeResource(res, R.drawable.ic_btn_star_off);
            STAR_ON = BitmapFactory.decodeResource(res, R.drawable.ic_btn_star_on);
            CHECK = BitmapFactory.decodeResource(res, R.drawable.ic_avatar_check);
            ATTACHMENT = BitmapFactory.decodeResource(res, R.drawable.ic_attachment_holo_light);
            ONLY_TO_ME = BitmapFactory.decodeResource(res, R.drawable.ic_email_caret_double);
            TO_ME_AND_OTHERS = BitmapFactory.decodeResource(res, R.drawable.ic_email_caret_single);
            IMPORTANT_ONLY_TO_ME = BitmapFactory.decodeResource(res,
                    R.drawable.ic_email_caret_double_important_unread);
            IMPORTANT_TO_ME_AND_OTHERS = BitmapFactory.decodeResource(res,
                    R.drawable.ic_email_caret_single_important_unread);
            IMPORTANT_TO_OTHERS = BitmapFactory.decodeResource(res,
                    R.drawable.ic_email_caret_none_important_unread);
            STATE_REPLIED =
                    BitmapFactory.decodeResource(res, R.drawable.ic_badge_reply_holo_light);
            STATE_FORWARDED =
                    BitmapFactory.decodeResource(res, R.drawable.ic_badge_forward_holo_light);
            STATE_REPLIED_AND_FORWARDED =
                    BitmapFactory.decodeResource(res, R.drawable.ic_badge_reply_forward_holo_light);
            STATE_CALENDAR_INVITE =
                    BitmapFactory.decodeResource(res, R.drawable.ic_badge_invite_holo_light);
            VISIBLE_CONVERSATION_CARET = BitmapFactory.decodeResource(res, R.drawable.caret_grey);
            RIGHT_EDGE_TABLET = res.getDrawable(R.drawable.list_edge_tablet);
            PLACEHOLDER = res.getDrawable(drawable.ic_attachment_load);
            PROGRESS_BAR = res.getDrawable(drawable.progress_holo);

            // Initialize colors.
            sActivatedTextSpan = CharacterStyle.wrap(new ForegroundColorSpan(
                    res.getColor(R.color.senders_text_color_read)));
            sSendersTextColorRead = res.getColor(R.color.senders_text_color_read);
            sSendersTextColorUnread = res.getColor(R.color.senders_text_color_unread);
            sSubjectTextUnreadSpan = new TextAppearanceSpan(mContext,
                    R.style.SubjectAppearanceUnreadStyle);
            sSubjectTextReadSpan = new TextAppearanceSpan(mContext,
                    R.style.SubjectAppearanceReadStyle);
            sSnippetTextUnreadSpan =
                    new ForegroundColorSpan(res.getColor(R.color.snippet_text_color_unread));
            sSnippetTextReadSpan =
                    new ForegroundColorSpan(res.getColor(R.color.snippet_text_color_read));
            sDateTextColor = res.getColor(R.color.date_text_color);
            sStarTouchSlop = res.getDimensionPixelSize(R.dimen.star_touch_slop);
            sSenderImageTouchSlop = res.getDimensionPixelSize(R.dimen.sender_image_touch_slop);
            sShrinkAnimationDuration = res.getInteger(R.integer.shrink_animation_duration);
            sSlideAnimationDuration = res.getInteger(R.integer.slide_animation_duration);
            // Initialize static color.
            sSendersSplitToken = res.getString(R.string.senders_split_token);
            sElidedPaddingToken = res.getString(R.string.elided_padding_token);
            sScrollSlop = res.getInteger(R.integer.swipeScrollSlop);
            sFoldersLeftPadding = res.getDimensionPixelOffset(R.dimen.folders_left_padding);
            sContactPhotoManager = ContactPhotoManager.createContactPhotoManager(context);
            sOverflowCountMax = res.getInteger(integer.ap_overflow_max_count);
            sCabAnimationDuration =
                    res.getInteger(R.integer.conv_item_view_cab_anim_duration);
        }

        mPhotoFlipAnimator = new CabAnimator("photoFlipFraction", 0, 2,
                sCabAnimationDuration) {
            @Override
            public void invalidateArea() {
                final int left = mCoordinates.contactImagesX;
                final int right = left + mContactImagesHolder.getWidth();
                final int top = mCoordinates.contactImagesY;
                final int bottom = top + mContactImagesHolder.getHeight();
                invalidate(left, top, right, bottom);
            }
        };

        mSendersTextView = new TextView(mContext);
        mSendersTextView.setIncludeFontPadding(false);

        mSubjectTextView = new TextView(mContext);
        mSubjectTextView.setEllipsize(TextUtils.TruncateAt.END);
        mSubjectTextView.setIncludeFontPadding(false);

        mContactImagesHolder = new DividedImageCanvas(context, new InvalidateCallback() {
            @Override
            public void invalidate() {
                if (mCoordinates == null) {
                    return;
                }
                ConversationItemView.this.invalidate(mCoordinates.contactImagesX,
                        mCoordinates.contactImagesY,
                        mCoordinates.contactImagesX + mCoordinates.contactImagesWidth,
                        mCoordinates.contactImagesY + mCoordinates.contactImagesHeight);
            }
        });

        mAttachmentsView = new AttachmentGridDrawable(res, PLACEHOLDER, PROGRESS_BAR);
        mAttachmentsView.setCallback(this);

        Utils.traceEndSection();
    }

    public void bind(final Conversation conversation, final ControllableActivity activity,
            final ConversationListListener conversationListListener,
            final ConversationSelectionSet set, final Folder folder,
            final int checkboxOrSenderImage, final boolean showAttachmentPreviews,
            final boolean parallaxSpeedAlternative, final boolean parallaxDirectionAlternative,
            final boolean swipeEnabled, final boolean priorityArrowEnabled,
            final AnimatedAdapter adapter) {
        Utils.traceBeginSection("CIVC.bind");
        bind(ConversationItemViewModel.forConversation(mAccount, conversation), activity,
                conversationListListener, null /* conversationItemAreaClickListener */, set, folder,
                checkboxOrSenderImage, showAttachmentPreviews, parallaxSpeedAlternative,
                parallaxDirectionAlternative, swipeEnabled, priorityArrowEnabled, adapter,
                -1 /* backgroundOverrideResId */,
                null /* photoBitmap */);
        Utils.traceEndSection();
    }

    public void bindAd(final ConversationItemViewModel conversationItemViewModel,
            final ControllableActivity activity,
            final ConversationListListener conversationListListener,
            final ConversationItemAreaClickListener conversationItemAreaClickListener,
            final Folder folder, final int checkboxOrSenderImage, final AnimatedAdapter adapter,
            final int backgroundOverrideResId, final Bitmap photoBitmap) {
        Utils.traceBeginSection("CIVC.bindAd");
        bind(conversationItemViewModel, activity, conversationListListener,
                conversationItemAreaClickListener, null /* set */, folder, checkboxOrSenderImage,
                false /* attachment previews */, false /* parallax */, false /* parallax */,
                true /* swipeEnabled */, false /* priorityArrowEnabled */, adapter,
                backgroundOverrideResId, photoBitmap);
        Utils.traceEndSection();
    }

    private void bind(final ConversationItemViewModel header, final ControllableActivity activity,
            final ConversationListListener conversationListListener,
            final ConversationItemAreaClickListener conversationItemAreaClickListener,
            final ConversationSelectionSet set, final Folder folder,
            final int checkboxOrSenderImage, final boolean showAttachmentPreviews,
            final boolean parallaxSpeedAlternative, final boolean parallaxDirectionAlternative,
            boolean swipeEnabled, final boolean priorityArrowEnabled, final AnimatedAdapter adapter,
            final int backgroundOverrideResId, final Bitmap photoBitmap) {
        mBackgroundOverrideResId = backgroundOverrideResId;
        mPhotoBitmap = photoBitmap;
        mConversationItemAreaClickListener = conversationItemAreaClickListener;

        if (mHeader != null) {
            // If this was previously bound to a different conversation, remove any contact photo
            // manager requests.
            if (header.conversation.id != mHeader.conversation.id ||
                    (mHeader.displayableSenderNames != null && !mHeader.displayableSenderNames
                    .equals(header.displayableSenderNames))) {
                ArrayList<String> divisionIds = mContactImagesHolder.getDivisionIds();
                if (divisionIds != null) {
                    mContactImagesHolder.reset();
                    for (int pos = 0; pos < divisionIds.size(); pos++) {
                        sContactPhotoManager.removePhoto(ContactPhotoManager.generateHash(
                                mContactImagesHolder, pos, divisionIds.get(pos)));
                    }
                }
            }

            // If this was previously bound to a different conversation,
            // remove any attachment preview manager requests.
            if (header.conversation.id != mHeader.conversation.id
                    || header.conversation.attachmentPreviewsCount
                            != mHeader.conversation.attachmentPreviewsCount
                    || !header.conversation.getAttachmentPreviewUris()
                            .equals(mHeader.conversation.getAttachmentPreviewUris())) {

                // unbind the attachments view (releasing bitmap references)
                // (this also cancels all async tasks)
                for (int i = 0, len = mAttachmentsView.getCount(); i < len; i++) {
                    mAttachmentsView.getOrCreateDrawable(i).unbind();
                }
                // reset the grid, as the newly bound item may have a different attachment count
                mAttachmentsView.setCount(0);
            }

            if (header.conversation.id != mHeader.conversation.id) {
                // Stop the photo flip animation
                mPhotoFlipAnimator.stopAnimation();
            }
        }
        mCoordinates = null;
        mHeader = header;
        mActivity = activity;
        mConversationListListener = conversationListListener;
        mSelectedConversationSet = set;
        mDisplayedFolder = folder;
        mStarEnabled = folder != null && !folder.isTrash();
        mSwipeEnabled = swipeEnabled;
        mAdapter = adapter;
        mAttachmentsView.setBitmapCache(mAdapter.getBitmapCache());
        mAttachmentsView.setDecodeAggregator(mAdapter.getDecodeAggregator());

        if (checkboxOrSenderImage == ConversationListIcon.SENDER_IMAGE) {
            mGadgetMode = ConversationItemViewCoordinates.GADGET_CONTACT_PHOTO;
        } else {
            mGadgetMode = ConversationItemViewCoordinates.GADGET_NONE;
        }

        mAttachmentPreviewsEnabled = showAttachmentPreviews;
        mParallaxSpeedAlternative = parallaxSpeedAlternative;
        mParallaxDirectionAlternative = parallaxDirectionAlternative;

        // Initialize folder displayer.
        if (mHeader.folderDisplayer == null) {
            mHeader.folderDisplayer = new ConversationItemFolderDisplayer(mContext);
        } else {
            mHeader.folderDisplayer.reset();
        }

        final int ignoreFolderType;
        if (mDisplayedFolder.isInbox()) {
            ignoreFolderType = FolderType.INBOX;
        } else {
            ignoreFolderType = -1;
        }

        mHeader.folderDisplayer.loadConversationFolders(mHeader.conversation,
                mDisplayedFolder.folderUri, ignoreFolderType);

        if (mHeader.dateOverrideText == null) {
            mHeader.dateText = DateUtils.getRelativeTimeSpanString(mContext,
                    mHeader.conversation.dateMs);
        } else {
            mHeader.dateText = mHeader.dateOverrideText;
        }

        mConfig = new ConversationItemViewCoordinates.Config()
            .withGadget(mGadgetMode)
            .withAttachmentPreviews(getAttachmentPreviewsMode());
        if (header.folderDisplayer.hasVisibleFolders()) {
            mConfig.showFolders();
        }
        if (header.hasBeenForwarded || header.hasBeenRepliedTo || header.isInvite) {
            mConfig.showReplyState();
        }
        if (mHeader.conversation.color != 0) {
            mConfig.showColorBlock();
        }
        // Personal level.
        mHeader.personalLevelBitmap = null;
        if (true) { // TODO: hook this up to a setting
            final int personalLevel = mHeader.conversation.personalLevel;
            final boolean isImportant =
                    mHeader.conversation.priority == UIProvider.ConversationPriority.IMPORTANT;
            final boolean useImportantMarkers = isImportant && priorityArrowEnabled;

            if (personalLevel == UIProvider.ConversationPersonalLevel.ONLY_TO_ME) {
                mHeader.personalLevelBitmap = useImportantMarkers ? IMPORTANT_ONLY_TO_ME
                        : ONLY_TO_ME;
            } else if (personalLevel == UIProvider.ConversationPersonalLevel.TO_ME_AND_OTHERS) {
                mHeader.personalLevelBitmap = useImportantMarkers ? IMPORTANT_TO_ME_AND_OTHERS
                        : TO_ME_AND_OTHERS;
            } else if (useImportantMarkers) {
                mHeader.personalLevelBitmap = IMPORTANT_TO_OTHERS;
            }
        }
        if (mHeader.personalLevelBitmap != null) {
            mConfig.showPersonalIndicator();
        }

        mAttachmentsView.setOverflowText(null);

        setContentDescription();
        requestLayout();
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        boolean handled = false;
        if (mCoordinates != null) {
            if (mAttachmentsView.equals(who)) {
                final Rect r = new Rect(who.getBounds());
                r.offset(mCoordinates.attachmentPreviewsX, mCoordinates.attachmentPreviewsY);
                ConversationItemView.this.invalidate(r.left, r.top, r.right, r.bottom);
                handled = true;
            }
        }
        if (!handled) {
            super.invalidateDrawable(who);
        }
    }

    /**
     * Get the Conversation object associated with this view.
     */
    public Conversation getConversation() {
        return mHeader.conversation;
    }

    private static void startTimer(String tag) {
        if (sTimer != null) {
            sTimer.start(tag);
        }
    }

    private static void pauseTimer(String tag) {
        if (sTimer != null) {
            sTimer.pause(tag);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Utils.traceBeginSection("CIVC.measure");
        final int wSize = MeasureSpec.getSize(widthMeasureSpec);

        final int currentMode = mActivity.getViewMode().getMode();
        if (wSize != mViewWidth || mPreviousMode != currentMode) {
            mViewWidth = wSize;
            mPreviousMode = currentMode;
        }
        mHeader.viewWidth = mViewWidth;

        mConfig.updateWidth(wSize).setViewMode(currentMode);

        Resources res = getResources();
        mHeader.standardScaledDimen = res.getDimensionPixelOffset(R.dimen.standard_scaled_dimen);

        mCoordinates = ConversationItemViewCoordinates.forConfig(mContext, mConfig,
                mAdapter.getCoordinatesCache());

        if (mPhotoBitmap != null) {
            mPhotoRect = new Rect(0, 0, mCoordinates.contactImagesWidth,
                    mCoordinates.contactImagesHeight);
        }

        final int h = (mAnimatedHeightFraction != 1.0f) ?
                Math.round(mAnimatedHeightFraction * mCoordinates.height) : mCoordinates.height;
        setMeasuredDimension(mConfig.getWidth(), h);
        Utils.traceEndSection();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        startTimer(PERF_TAG_LAYOUT);
        Utils.traceBeginSection("CIVC.layout");

        super.onLayout(changed, left, top, right, bottom);

        Utils.traceBeginSection("text and bitmaps");
        calculateTextsAndBitmaps();
        Utils.traceEndSection();

        Utils.traceBeginSection("coordinates");
        calculateCoordinates();
        Utils.traceEndSection();

        // Subject.
        createSubject(mHeader.unread);

        if (!mHeader.isLayoutValid()) {
            setContentDescription();
        }
        mHeader.validate();

        pauseTimer(PERF_TAG_LAYOUT);
        if (sTimer != null && ++sLayoutCount >= PERF_LAYOUT_ITERATIONS) {
            sTimer.dumpResults();
            sTimer = new Timer();
            sLayoutCount = 0;
        }
        Utils.traceEndSection();
    }

    private void setContentDescription() {
        if (mActivity.isAccessibilityEnabled()) {
            mHeader.resetContentDescription();
            setContentDescription(mHeader.getContentDescription(mContext));
        }
    }

    @Override
    public void setBackgroundResource(int resourceId) {
        Utils.traceBeginSection("set background resource");
        Drawable drawable = mBackgrounds.get(resourceId);
        if (drawable == null) {
            drawable = getResources().getDrawable(resourceId);
            mBackgrounds.put(resourceId, drawable);
        }
        if (getBackground() != drawable) {
            super.setBackgroundDrawable(drawable);
        }
        Utils.traceEndSection();
    }

    private void calculateTextsAndBitmaps() {
        startTimer(PERF_TAG_CALCULATE_TEXTS_BITMAPS);

        if (mSelectedConversationSet != null) {
            mSelected = mSelectedConversationSet.contains(mHeader.conversation);
        }
        setSelected(mSelected);
        mHeader.gadgetMode = mGadgetMode;

        final boolean isUnread = mHeader.unread;
        updateBackground(isUnread);

        mHeader.sendersDisplayText = new SpannableStringBuilder();
        mHeader.styledSendersString = null;

        mHeader.hasDraftMessage = mHeader.conversation.numDrafts() > 0;

        // Parse senders fragments.
        if (mHeader.preserveSendersText) {
            // This is a special view that doesn't need special sender formatting
            mHeader.sendersDisplayText = new SpannableStringBuilder(mHeader.sendersText);
            loadSenderImages();
        } else if (mHeader.conversation.conversationInfo != null) {
            // This is Gmail
            Context context = getContext();
            mHeader.messageInfoString = SendersView
                    .createMessageInfo(context, mHeader.conversation, true);
            int maxChars = ConversationItemViewCoordinates.getSendersLength(context,
                    mCoordinates.getMode(), mHeader.conversation.hasAttachments);
            mHeader.displayableSenderEmails = new ArrayList<String>();
            mHeader.displayableSenderNames = new ArrayList<String>();
            mHeader.styledSenders = new ArrayList<SpannableString>();
            SendersView.format(context, mHeader.conversation.conversationInfo,
                    mHeader.messageInfoString.toString(), maxChars, mHeader.styledSenders,
                    mHeader.displayableSenderNames, mHeader.displayableSenderEmails, mAccount,
                    true);

            if (mHeader.displayableSenderEmails.isEmpty() && mHeader.hasDraftMessage) {
                mHeader.displayableSenderEmails.add(mAccount);
                mHeader.displayableSenderNames.add(mAccount);
            }

            // If we have displayable senders, load their thumbnails
            loadSenderImages();
        } else {
            // This is Email
            SendersView.formatSenders(mHeader, getContext(), true);
            if (!TextUtils.isEmpty(mHeader.conversation.senders)) {
                mHeader.displayableSenderEmails = new ArrayList<String>();
                mHeader.displayableSenderNames = new ArrayList<String>();

                final Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(mHeader.conversation.senders);
                for (int i = 0; i < tokens.length;i++) {
                    final Rfc822Token token = tokens[i];
                    final String senderName = Address.decodeAddressName(token.getName());
                    final String senderAddress = token.getAddress();
                    mHeader.displayableSenderEmails.add(senderAddress);
                    mHeader.displayableSenderNames.add(
                            !TextUtils.isEmpty(senderName) ? senderName : senderAddress);
                }
                loadSenderImages();
            }
        }

        if (isAttachmentPreviewsEnabled()) {
            loadAttachmentPreviews();
        }

        if (mHeader.isLayoutValid()) {
            pauseTimer(PERF_TAG_CALCULATE_TEXTS_BITMAPS);
            return;
        }
        startTimer(PERF_TAG_CALCULATE_FOLDERS);


        pauseTimer(PERF_TAG_CALCULATE_FOLDERS);

        // Paper clip icon.
        mHeader.paperclip = null;
        if (mHeader.conversation.hasAttachments) {
            mHeader.paperclip = ATTACHMENT;
        }

        startTimer(PERF_TAG_CALCULATE_SENDER_SUBJECT);

        pauseTimer(PERF_TAG_CALCULATE_SENDER_SUBJECT);
        pauseTimer(PERF_TAG_CALCULATE_TEXTS_BITMAPS);
    }

    private boolean isAttachmentPreviewsEnabled() {
        return mAttachmentPreviewsEnabled && !mHeader.conversation.getAttachmentPreviewUris()
                .isEmpty();
    }

    private int getOverflowCount() {
        return mHeader.conversation.attachmentPreviewsCount - mHeader.conversation
                .getAttachmentPreviewUris().size();
    }

    private int getAttachmentPreviewsMode() {
        if (isAttachmentPreviewsEnabled()) {
            return mHeader.conversation.read
                    ? ConversationItemViewCoordinates.ATTACHMENT_PREVIEW_READ
                    : ConversationItemViewCoordinates.ATTACHMENT_PREVIEW_UNREAD;
        } else {
            return ConversationItemViewCoordinates.ATTACHMENT_PREVIEW_NONE;
        }
    }

    private float getParallaxSpeedMultiplier() {
        return mParallaxSpeedAlternative
                ? SwipeableListView.ATTACHMENT_PARALLAX_MULTIPLIER_ALTERNATIVE
                : SwipeableListView.ATTACHMENT_PARALLAX_MULTIPLIER_NORMAL;
    }

    // FIXME(ath): maybe move this to bind(). the only dependency on layout is on tile W/H, which
    // is immutable.
    private void loadSenderImages() {
        if (mGadgetMode == ConversationItemViewCoordinates.GADGET_CONTACT_PHOTO
                && mHeader.displayableSenderEmails != null
                && mHeader.displayableSenderEmails.size() > 0) {
            if (mCoordinates.contactImagesWidth <= 0 || mCoordinates.contactImagesHeight <= 0) {
                LogUtils.w(LOG_TAG,
                        "Contact image width(%d) or height(%d) is 0 for mode: (%d).",
                        mCoordinates.contactImagesWidth, mCoordinates.contactImagesHeight,
                        mCoordinates.getMode());
                return;
            }

            int size = mHeader.displayableSenderEmails.size();
            final List<Object> keys = Lists.newArrayListWithCapacity(size);
            for (int i = 0; i < DividedImageCanvas.MAX_DIVISIONS && i < size; i++) {
                keys.add(mHeader.displayableSenderEmails.get(i));
            }

            mContactImagesHolder.setDimensions(mCoordinates.contactImagesWidth,
                    mCoordinates.contactImagesHeight);
            mContactImagesHolder.setDivisionIds(keys);
            String emailAddress;
            for (int i = 0; i < DividedImageCanvas.MAX_DIVISIONS && i < size; i++) {
                emailAddress = mHeader.displayableSenderEmails.get(i);
                PhotoIdentifier photoIdentifier = new ContactIdentifier(
                        mHeader.displayableSenderNames.get(i), emailAddress, i);
                sContactPhotoManager.loadThumbnail(photoIdentifier, mContactImagesHolder);
            }
        }
    }

    private void loadAttachmentPreviews() {
        if (mCoordinates.attachmentPreviewsWidth <= 0
                || mCoordinates.attachmentPreviewsHeight <= 0) {
            LogUtils.w(LOG_TAG,
                    "Attachment preview width(%d) or height(%d) is 0 for mode: (%d,%d).",
                    mCoordinates.attachmentPreviewsWidth, mCoordinates.attachmentPreviewsHeight,
                    mCoordinates.getMode(), getAttachmentPreviewsMode());
            return;
        }
        Utils.traceBeginSection("attachment previews");

        Utils.traceBeginSection("Setup load attachment previews");

        LogUtils.d(LOG_TAG,
                "loadAttachmentPreviews: Loading attachment previews for conversation %s",
                mHeader.conversation);

        // Get list of attachments and states from conversation
        final ArrayList<String> attachmentUris = mHeader.conversation.getAttachmentPreviewUris();
        final int previewStates = mHeader.conversation.attachmentPreviewStates;
        final int displayCount = Math.min(
                attachmentUris.size(), AttachmentGridDrawable.MAX_VISIBLE_ATTACHMENT_COUNT);
        Utils.traceEndSection();

        mAttachmentsView.setCoordinates(mCoordinates);
        mAttachmentsView.setCount(displayCount);

        final int decodeHeight;
        // if parallax is enabled, increase the desired vertical size of attachment bitmaps
        // so we have extra pixels to scroll within
        if (SwipeableListView.ENABLE_ATTACHMENT_PARALLAX) {
            decodeHeight = Math.round(mCoordinates.attachmentPreviewsDecodeHeight
                    * getParallaxSpeedMultiplier());
        } else {
            decodeHeight = mCoordinates.attachmentPreviewsDecodeHeight;
        }

        // set the bounds before binding inner drawables so they can decode right away
        // (they need the their bounds set to know whether to decode to 1x1 or 2x1 dimens)
        mAttachmentsView.setBounds(0, 0, mCoordinates.attachmentPreviewsWidth,
                mCoordinates.attachmentPreviewsHeight);

        for (int i = 0; i < displayCount; i++) {
            Utils.traceBeginSection("setup single attachment preview");
            final String uri = attachmentUris.get(i);

            // Find the rendition to load based on availability.
            LogUtils.v(LOG_TAG, "loadAttachmentPreviews: state [BEST, SIMPLE] is [%s, %s] for %s ",
                    Attachment.getPreviewState(previewStates, i, AttachmentRendition.BEST),
                    Attachment.getPreviewState(previewStates, i, AttachmentRendition.SIMPLE),
                    uri);
            int bestAvailableRendition = -1;
            // BEST first, else use less preferred renditions
            for (final int rendition : AttachmentRendition.PREFERRED_RENDITIONS) {
                if (Attachment.getPreviewState(previewStates, i, rendition)) {
                    bestAvailableRendition = rendition;
                    break;
                }
            }

            LogUtils.d(LOG_TAG,
                    "creating/setting drawable region in CIV=%s canvas=%s rend=%s uri=%s",
                    this, mAttachmentsView, bestAvailableRendition, uri);
            final AttachmentDrawable drawable = mAttachmentsView.getOrCreateDrawable(i);
            drawable.setDecodeDimensions(mCoordinates.attachmentPreviewsWidth, decodeHeight);
            drawable.setParallaxSpeedMultiplier(getParallaxSpeedMultiplier());
            if (bestAvailableRendition != -1) {
                drawable.bind(getContext(), uri, bestAvailableRendition);
            } else {
                drawable.showStaticPlaceholder();
            }

            Utils.traceEndSection();
        }

        Utils.traceEndSection();
    }

    private static int makeExactSpecForSize(int size) {
        return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
    }

    private static void layoutViewExactly(View v, int w, int h) {
        v.measure(makeExactSpecForSize(w), makeExactSpecForSize(h));
        v.layout(0, 0, w, h);
    }

    private void layoutSenders() {
        if (mHeader.styledSendersString != null) {
            if (isActivated() && showActivatedText()) {
                mHeader.styledSendersString.setSpan(sActivatedTextSpan, 0,
                        mHeader.styledMessageInfoStringOffset, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                mHeader.styledSendersString.removeSpan(sActivatedTextSpan);
            }

            final int w = mSendersWidth;
            final int h = mCoordinates.sendersHeight;
            mSendersTextView.setLayoutParams(new ViewGroup.LayoutParams(w, h));
            mSendersTextView.setMaxLines(mCoordinates.sendersLineCount);
            mSendersTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mCoordinates.sendersFontSize);
            layoutViewExactly(mSendersTextView, w, h);

            mSendersTextView.setText(mHeader.styledSendersString);
        }
    }

    private void createSubject(final boolean isUnread) {
        final String subject = filterTag(mHeader.conversation.subject);
        final String snippet = mHeader.conversation.getSnippet();
        final Spannable displayedStringBuilder = new SpannableString(
                Conversation.getSubjectAndSnippetForDisplay(mContext, subject, snippet));

        // since spans affect text metrics, add spans to the string before measure/layout or fancy
        // ellipsizing
        final int subjectTextLength = (subject != null) ? subject.length() : 0;
        if (!TextUtils.isEmpty(subject)) {
            displayedStringBuilder.setSpan(TextAppearanceSpan.wrap(
                    isUnread ? sSubjectTextUnreadSpan : sSubjectTextReadSpan), 0, subjectTextLength,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (!TextUtils.isEmpty(snippet)) {
            final int startOffset = subjectTextLength;
            // Start after the end of the subject text; since the subject may be
            // "" or null, this could start at the 0th character in the subjectText string
            displayedStringBuilder.setSpan(ForegroundColorSpan.wrap(
                    isUnread ? sSnippetTextUnreadSpan : sSnippetTextReadSpan), startOffset,
                    displayedStringBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (isActivated() && showActivatedText()) {
            displayedStringBuilder.setSpan(sActivatedTextSpan, 0, displayedStringBuilder.length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }

        final int subjectWidth = mCoordinates.subjectWidth;
        final int subjectHeight = mCoordinates.subjectHeight;
        mSubjectTextView.setLayoutParams(new ViewGroup.LayoutParams(subjectWidth, subjectHeight));
        mSubjectTextView.setMaxLines(mCoordinates.subjectLineCount);
        mSubjectTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mCoordinates.subjectFontSize);
        layoutViewExactly(mSubjectTextView, subjectWidth, subjectHeight);

        mSubjectTextView.setText(displayedStringBuilder);
    }

    private boolean showActivatedText() {
        // For activated elements in tablet in conversation mode, we show an activated color, since
        // the background is dark blue for activated versus gray for non-activated.
        return mTabletDevice && !mListCollapsible;
    }

    private boolean canFitFragment(int width, int line, int fixedWidth) {
        if (line == mCoordinates.sendersLineCount) {
            return width + fixedWidth <= mSendersWidth;
        } else {
            return width <= mSendersWidth;
        }
    }

    private void calculateCoordinates() {
        startTimer(PERF_TAG_CALCULATE_COORDINATES);

        sPaint.setTextSize(mCoordinates.dateFontSize);
        sPaint.setTypeface(Typeface.DEFAULT);

        if (mHeader.infoIcon != null) {
            mInfoIconX = mCoordinates.infoIconXEnd - mHeader.infoIcon.getWidth();

            // If we have an info icon, we start drawing the date text:
            // At the end of the date TextView minus the width of the date text
            mDateX = mCoordinates.dateXEnd - (int) sPaint.measureText(
                    mHeader.dateText != null ? mHeader.dateText.toString() : "");
        } else {
            // If there is no info icon, we start drawing the date text:
            // At the end of the info icon ImageView minus the width of the date text
            // We use the info icon ImageView for positioning, since we want the date text to be
            // at the right, since there is no info icon
            mDateX = mCoordinates.infoIconXEnd - (int) sPaint.measureText(
                    mHeader.dateText != null ? mHeader.dateText.toString() : "");
        }

        mPaperclipX = mDateX - ATTACHMENT.getWidth() - mCoordinates.datePaddingLeft;

        if (mCoordinates.isWide()) {
            // In wide mode, the end of the senders should align with
            // the start of the subject and is based on a max width.
            mSendersWidth = mCoordinates.sendersWidth;
        } else {
            // In normal mode, the width is based on where the date/attachment icon start.
            final int dateAttachmentStart;
            // Have this end near the paperclip or date, not the folders.
            if (mHeader.paperclip != null) {
                dateAttachmentStart = mPaperclipX - mCoordinates.paperclipPaddingLeft;
            } else {
                dateAttachmentStart = mDateX - mCoordinates.datePaddingLeft;
            }
            mSendersWidth = dateAttachmentStart - mCoordinates.sendersX;
        }

        // Second pass to layout each fragment.
        sPaint.setTextSize(mCoordinates.sendersFontSize);
        sPaint.setTypeface(Typeface.DEFAULT);

        if (mHeader.styledSenders != null) {
            ellipsizeStyledSenders();
            layoutSenders();
        } else {
            // First pass to calculate width of each fragment.
            int totalWidth = 0;
            int fixedWidth = 0;
            for (SenderFragment senderFragment : mHeader.senderFragments) {
                CharacterStyle style = senderFragment.style;
                int start = senderFragment.start;
                int end = senderFragment.end;
                style.updateDrawState(sPaint);
                senderFragment.width = (int) sPaint.measureText(mHeader.sendersText, start, end);
                boolean isFixed = senderFragment.isFixed;
                if (isFixed) {
                    fixedWidth += senderFragment.width;
                }
                totalWidth += senderFragment.width;
            }

            if (mSendersWidth < 0) {
                mSendersWidth = 0;
            }
            totalWidth = ellipsize(fixedWidth);
            mHeader.sendersDisplayLayout = new StaticLayout(mHeader.sendersDisplayText, sPaint,
                    mSendersWidth, Alignment.ALIGN_NORMAL, 1, 0, true);
        }

        if (mSendersWidth < 0) {
            mSendersWidth = 0;
        }

        pauseTimer(PERF_TAG_CALCULATE_COORDINATES);
    }

    // The rules for displaying ellipsized senders are as follows:
    // 1) If there is message info (either a COUNT or DRAFT info to display), it MUST be shown
    // 2) If senders do not fit, ellipsize the last one that does fit, and stop
    // appending new senders
    private int ellipsizeStyledSenders() {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        float totalWidth = 0;
        boolean ellipsize = false;
        float width;
        SpannableStringBuilder messageInfoString =  mHeader.messageInfoString;
        if (messageInfoString.length() > 0) {
            CharacterStyle[] spans = messageInfoString.getSpans(0, messageInfoString.length(),
                    CharacterStyle.class);
            // There is only 1 character style span; make sure we apply all the
            // styles to the paint object before measuring.
            if (spans.length > 0) {
                spans[0].updateDrawState(sPaint);
            }
            // Paint the message info string to see if we lose space.
            float messageInfoWidth = sPaint.measureText(messageInfoString.toString());
            totalWidth += messageInfoWidth;
        }
       SpannableString prevSender = null;
       SpannableString ellipsizedText;
        for (SpannableString sender : mHeader.styledSenders) {
            // There may be null sender strings if there were dupes we had to remove.
            if (sender == null) {
                continue;
            }
            // No more width available, we'll only show fixed fragments.
            if (ellipsize) {
                break;
            }
            CharacterStyle[] spans = sender.getSpans(0, sender.length(), CharacterStyle.class);
            // There is only 1 character style span.
            if (spans.length > 0) {
                spans[0].updateDrawState(sPaint);
            }
            // If there are already senders present in this string, we need to
            // make sure we prepend the dividing token
            if (SendersView.sElidedString.equals(sender.toString())) {
                prevSender = sender;
                sender = copyStyles(spans, sElidedPaddingToken + sender + sElidedPaddingToken);
            } else if (builder.length() > 0
                    && (prevSender == null || !SendersView.sElidedString.equals(prevSender
                            .toString()))) {
                prevSender = sender;
                sender = copyStyles(spans, sSendersSplitToken + sender);
            } else {
                prevSender = sender;
            }
            if (spans.length > 0) {
                spans[0].updateDrawState(sPaint);
            }
            // Measure the width of the current sender and make sure we have space
            width = (int) sPaint.measureText(sender.toString());
            if (width + totalWidth > mSendersWidth) {
                // The text is too long, new line won't help. We have to
                // ellipsize text.
                ellipsize = true;
                width = mSendersWidth - totalWidth; // ellipsis width?
                ellipsizedText = copyStyles(spans,
                        TextUtils.ellipsize(sender, sPaint, width, TruncateAt.END));
                width = (int) sPaint.measureText(ellipsizedText.toString());
            } else {
                ellipsizedText = null;
            }
            totalWidth += width;

            final CharSequence fragmentDisplayText;
            if (ellipsizedText != null) {
                fragmentDisplayText = ellipsizedText;
            } else {
                fragmentDisplayText = sender;
            }
            builder.append(fragmentDisplayText);
        }
        mHeader.styledMessageInfoStringOffset = builder.length();
        builder.append(messageInfoString);
        mHeader.styledSendersString = builder;
        return (int)totalWidth;
    }

    private static SpannableString copyStyles(CharacterStyle[] spans, CharSequence newText) {
        SpannableString s = new SpannableString(newText);
        if (spans != null && spans.length > 0) {
            s.setSpan(spans[0], 0, s.length(), 0);
        }
        return s;
    }

    private int ellipsize(int fixedWidth) {
        int totalWidth = 0;
        int currentLine = 1;
        boolean ellipsize = false;
        for (SenderFragment senderFragment : mHeader.senderFragments) {
            CharacterStyle style = senderFragment.style;
            int start = senderFragment.start;
            int end = senderFragment.end;
            int width = senderFragment.width;
            boolean isFixed = senderFragment.isFixed;
            style.updateDrawState(sPaint);

            // No more width available, we'll only show fixed fragments.
            if (ellipsize && !isFixed) {
                senderFragment.shouldDisplay = false;
                continue;
            }

            // New line and ellipsize text if needed.
            senderFragment.ellipsizedText = null;
            if (isFixed) {
                fixedWidth -= width;
            }
            if (!canFitFragment(totalWidth + width, currentLine, fixedWidth)) {
                // The text is too long, new line won't help. We have to
                // ellipsize text.
                if (totalWidth == 0) {
                    ellipsize = true;
                } else {
                    // New line.
                    if (currentLine < mCoordinates.sendersLineCount) {
                        currentLine++;
                        totalWidth = 0;
                        // The text is still too long, we have to ellipsize
                        // text.
                        if (totalWidth + width > mSendersWidth) {
                            ellipsize = true;
                        }
                    } else {
                        ellipsize = true;
                    }
                }

                if (ellipsize) {
                    width = mSendersWidth - totalWidth;
                    // No more new line, we have to reserve width for fixed
                    // fragments.
                    if (currentLine == mCoordinates.sendersLineCount) {
                        width -= fixedWidth;
                    }
                    senderFragment.ellipsizedText = TextUtils.ellipsize(
                            mHeader.sendersText.substring(start, end), sPaint, width,
                            TruncateAt.END).toString();
                    width = (int) sPaint.measureText(senderFragment.ellipsizedText);
                }
            }
            senderFragment.shouldDisplay = true;
            totalWidth += width;

            final CharSequence fragmentDisplayText;
            if (senderFragment.ellipsizedText != null) {
                fragmentDisplayText = senderFragment.ellipsizedText;
            } else {
                fragmentDisplayText = mHeader.sendersText.substring(start, end);
            }
            final int spanStart = mHeader.sendersDisplayText.length();
            mHeader.sendersDisplayText.append(fragmentDisplayText);
            mHeader.sendersDisplayText.setSpan(senderFragment.style, spanStart,
                    mHeader.sendersDisplayText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return totalWidth;
    }

    /**
     * If the subject contains the tag of a mailing-list (text surrounded with
     * []), return the subject with that tag ellipsized, e.g.
     * "[android-gmail-team] Hello" -> "[andr...] Hello"
     */
    private String filterTag(String subject) {
        String result = subject;
        String formatString = getContext().getResources().getString(R.string.filtered_tag);
        if (!TextUtils.isEmpty(subject) && subject.charAt(0) == '[') {
            int end = subject.indexOf(']');
            if (end > 0) {
                String tag = subject.substring(1, end);
                result = String.format(formatString, Utils.ellipsize(tag, 7),
                        subject.substring(end + 1));
            }
        }
        return result;
    }

    @Override
    public final void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (SwipeableListView.ENABLE_ATTACHMENT_PARALLAX) {
            if (mHeader == null || mCoordinates == null || !isAttachmentPreviewsEnabled()) {
                return;
            }

            invalidate(mCoordinates.attachmentPreviewsX, mCoordinates.attachmentPreviewsY,
                    mCoordinates.attachmentPreviewsX + mCoordinates.attachmentPreviewsWidth,
                    mCoordinates.attachmentPreviewsY + mCoordinates.attachmentPreviewsHeight);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Utils.traceBeginSection("CIVC.draw");

        // Contact photo
        if (mGadgetMode == ConversationItemViewCoordinates.GADGET_CONTACT_PHOTO) {
            canvas.save();
            drawContactImageArea(canvas);
            canvas.restore();
        }

        // Senders.
        boolean isUnread = mHeader.unread;
        // Old style senders; apply text colors/ sizes/ styling.
        canvas.save();
        if (mHeader.sendersDisplayLayout != null) {
            sPaint.setTextSize(mCoordinates.sendersFontSize);
            sPaint.setTypeface(SendersView.getTypeface(isUnread));
            sPaint.setColor(isUnread ? sSendersTextColorUnread : sSendersTextColorRead);
            canvas.translate(mCoordinates.sendersX, mCoordinates.sendersY
                    + mHeader.sendersDisplayLayout.getTopPadding());
            mHeader.sendersDisplayLayout.draw(canvas);
        } else {
            drawSenders(canvas);
        }
        canvas.restore();


        // Subject.
        sPaint.setTypeface(Typeface.DEFAULT);
        canvas.save();
        drawSubject(canvas);
        canvas.restore();

        // Folders.
        if (mConfig.areFoldersVisible()) {
            mHeader.folderDisplayer.drawFolders(canvas, mCoordinates);
        }

        // If this folder has a color (combined view/Email), show it here
        if (mConfig.isColorBlockVisible()) {
            sFoldersPaint.setColor(mHeader.conversation.color);
            sFoldersPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(mCoordinates.colorBlockX, mCoordinates.colorBlockY,
                    mCoordinates.colorBlockX + mCoordinates.colorBlockWidth,
                    mCoordinates.colorBlockY + mCoordinates.colorBlockHeight, sFoldersPaint);
        }

        // Draw the reply state. Draw nothing if neither replied nor forwarded.
        if (mConfig.isReplyStateVisible()) {
            if (mHeader.hasBeenRepliedTo && mHeader.hasBeenForwarded) {
                canvas.drawBitmap(STATE_REPLIED_AND_FORWARDED, mCoordinates.replyStateX,
                        mCoordinates.replyStateY, null);
            } else if (mHeader.hasBeenRepliedTo) {
                canvas.drawBitmap(STATE_REPLIED, mCoordinates.replyStateX,
                        mCoordinates.replyStateY, null);
            } else if (mHeader.hasBeenForwarded) {
                canvas.drawBitmap(STATE_FORWARDED, mCoordinates.replyStateX,
                        mCoordinates.replyStateY, null);
            } else if (mHeader.isInvite) {
                canvas.drawBitmap(STATE_CALENDAR_INVITE, mCoordinates.replyStateX,
                        mCoordinates.replyStateY, null);
            }
        }

        if (mConfig.isPersonalIndicatorVisible()) {
            canvas.drawBitmap(mHeader.personalLevelBitmap, mCoordinates.personalIndicatorX,
                    mCoordinates.personalIndicatorY, null);
        }

        // Info icon
        if (mHeader.infoIcon != null) {
            canvas.drawBitmap(mHeader.infoIcon, mInfoIconX, mCoordinates.infoIconY, sPaint);
        }

        // Date.
        sPaint.setTextSize(mCoordinates.dateFontSize);
        sPaint.setTypeface(Typeface.DEFAULT);
        sPaint.setColor(sDateTextColor);
        drawText(canvas, mHeader.dateText, mDateX, mCoordinates.dateYBaseline,
                sPaint);

        // Paper clip icon.
        if (mHeader.paperclip != null) {
            canvas.drawBitmap(mHeader.paperclip, mPaperclipX, mCoordinates.paperclipY, sPaint);
        }

        if (mStarEnabled) {
            // Star.
            canvas.drawBitmap(getStarBitmap(), mCoordinates.starX, mCoordinates.starY, sPaint);
        }

        // Attachment previews
        if (isAttachmentPreviewsEnabled()) {
            canvas.save();
            drawAttachmentPreviews(canvas);
            canvas.restore();
        }

        // right-side edge effect when in tablet conversation mode and the list is not collapsed
        if (Utils.getDisplayListRightEdgeEffect(mTabletDevice, mListCollapsible,
                mConfig.getViewMode())) {
            RIGHT_EDGE_TABLET.setBounds(getWidth() - RIGHT_EDGE_TABLET.getIntrinsicWidth(), 0,
                    getWidth(), getHeight());
            RIGHT_EDGE_TABLET.draw(canvas);

            if (isActivated()) {
                // draw caret on the right, centered vertically
                final int x = getWidth() - VISIBLE_CONVERSATION_CARET.getWidth();
                final int y = (getHeight() - VISIBLE_CONVERSATION_CARET.getHeight()) / 2;
                canvas.drawBitmap(VISIBLE_CONVERSATION_CARET, x, y, null);
            }
        }
        Utils.traceEndSection();
    }

    /**
     * Draws the contact images or check, in the correct animated state.
     */
    private void drawContactImageArea(final Canvas canvas) {
        if (isSelected()) {
            mLastSelectedId = mHeader.conversation.id;

            // Since this is selected, we draw the checkbox if the animation is not running, or if
            // it's running, and is past the half-way point
            if (mPhotoFlipAnimator.getValue() > 1 || !mPhotoFlipAnimator.isStarted()) {
                // Flash in the check
                drawCheckbox(canvas);
            } else {
                // Flip out the contact photo
                drawContactImages(canvas);
            }
        } else {
            if ((mConversationListListener.isExitingSelectionMode()
                    && mLastSelectedId == mHeader.conversation.id)
                    || mPhotoFlipAnimator.isStarted()) {
                // Animate back to the photo
                if (!mPhotoFlipAnimator.isStarted()) {
                    mPhotoFlipAnimator.startAnimation(true /* reverse */);
                }

                if (mPhotoFlipAnimator.getValue() > 1) {
                    // Flash out the check
                    drawCheckbox(canvas);
                } else {
                    // Flip in the contact photo
                    drawContactImages(canvas);
                }
            } else {
                mLastSelectedId = -1; // We don't care anymore
                mPhotoFlipAnimator.stopAnimation(); // It's not running, but we want to reset state

                // Contact photos
                drawContactImages(canvas);
            }
        }
    }

    private void drawContactImages(final Canvas canvas) {
        // mPhotoFlipFraction goes from 0 to 1
        final float value = mPhotoFlipAnimator.getValue();

        final float scale = 1f - value;
        final float xOffset = mContactImagesHolder.getWidth() * value / 2;

        mPhotoFlipMatrix.reset();
        mPhotoFlipMatrix.postScale(scale, 1);

        final float x = mCoordinates.contactImagesX + xOffset;
        final float y = mCoordinates.contactImagesY;

        canvas.translate(x, y);

        if (mPhotoBitmap == null) {
            mContactImagesHolder.draw(canvas, mPhotoFlipMatrix);
        } else {
            canvas.drawBitmap(mPhotoBitmap, null, mPhotoRect, sPaint);
        }
    }

    private void drawCheckbox(final Canvas canvas) {
        // mPhotoFlipFraction goes from 1 to 2

        // Draw the background
        canvas.save();
        canvas.translate(mCoordinates.contactImagesX, mCoordinates.contactImagesY);
        canvas.drawRect(0, 0, mCoordinates.contactImagesWidth, mCoordinates.contactImagesHeight,
                sCheckBackgroundPaint);
        canvas.restore();

        final int x = mCoordinates.contactImagesX
                + (mCoordinates.contactImagesWidth - CHECK.getWidth()) / 2;
        final int y = mCoordinates.contactImagesY
                + (mCoordinates.contactImagesHeight - CHECK.getHeight()) / 2;

        final float value = mPhotoFlipAnimator.getValue();
        final float scale;

        if (!mPhotoFlipAnimator.isStarted()) {
            // We're not animating
            scale = 1;
        } else if (value < 1.9) {
            // 1.0 to 1.9 will scale 0 to 1
            scale = (value - 1f) / 0.9f;
        } else if (value < 1.95) {
            // 1.9 to 1.95 will scale 1 to 19/18
            scale = (value - 1f) / 0.9f;
        } else {
            // 1.95 to 2.0 will scale 19/18 to 1
            scale = (0.95f - (value - 1.95f)) / 0.9f;
        }

        final float xOffset = CHECK.getWidth() * (1f - scale) / 2f;
        final float yOffset = CHECK.getHeight() * (1f - scale) / 2f;

        mCheckMatrix.reset();
        mCheckMatrix.postScale(scale, scale);

        canvas.translate(x + xOffset, y + yOffset);

        canvas.drawBitmap(CHECK, mCheckMatrix, sPaint);
    }

    private void drawAttachmentPreviews(Canvas canvas) {
        canvas.translate(mCoordinates.attachmentPreviewsX, mCoordinates.attachmentPreviewsY);
        final float fraction;
        if (SwipeableListView.ENABLE_ATTACHMENT_PARALLAX) {
            final View listView = getListView();
            final View listItemView = unwrap();
            if (mParallaxDirectionAlternative) {
                fraction = 1 - (float) listItemView.getBottom()
                        / (listView.getHeight() + listItemView.getHeight());
            } else {
                fraction = (float) listItemView.getBottom()
                        / (listView.getHeight() + listItemView.getHeight());
            }
        } else {
            // Vertically center the preview crop, which has already been decoded at 1/3.
            fraction = 0.5f;
        }
        mAttachmentsView.setParallaxFraction(fraction);
        mAttachmentsView.draw(canvas);
    }

    private void drawSubject(Canvas canvas) {
        canvas.translate(mCoordinates.subjectX, mCoordinates.subjectY);
        mSubjectTextView.draw(canvas);
    }

    private void drawSenders(Canvas canvas) {
        canvas.translate(mCoordinates.sendersX, mCoordinates.sendersY);
        mSendersTextView.draw(canvas);
    }

    private Bitmap getStarBitmap() {
        return mHeader.conversation.starred ? STAR_ON : STAR_OFF;
    }

    private static void drawText(Canvas canvas, CharSequence s, int x, int y, TextPaint paint) {
        canvas.drawText(s, 0, s.length(), x, y, paint);
    }

    /**
     * Set the background for this item based on:
     * 1. Read / Unread (unread messages have a lighter background)
     * 2. Tablet / Phone
     * 3. Checkbox checked / Unchecked (controls CAB color for item)
     * 4. Activated / Not activated (controls the blue highlight on tablet)
     * @param isUnread
     */
    private void updateBackground(boolean isUnread) {
        final int background;
        if (mBackgroundOverrideResId > 0) {
            background = mBackgroundOverrideResId;
        } else if (isUnread) {
            background = R.drawable.conversation_unread_selector;
        } else {
            background = R.drawable.conversation_read_selector;
        }
        setBackgroundResource(background);
    }

    /**
     * Toggle the check mark on this view and update the conversation or begin
     * drag, if drag is enabled.
     */
    @Override
    public boolean toggleSelectedStateOrBeginDrag() {
        ViewMode mode = mActivity.getViewMode();
        if (mIsExpansiveTablet && mode.isListMode()) {
            return beginDragMode();
        } else {
            return toggleSelectedState("long_press");
        }
    }

    @Override
    public boolean toggleSelectedState() {
        return toggleSelectedState(null);
    }

    private boolean toggleSelectedState(String sourceOpt) {
        if (mHeader != null && mHeader.conversation != null && mSelectedConversationSet != null) {
            mSelected = !mSelected;
            setSelected(mSelected);
            Conversation conv = mHeader.conversation;
            // Set the list position of this item in the conversation
            SwipeableListView listView = getListView();

            try {
                conv.position = mSelected && listView != null ? listView.getPositionForView(this)
                        : Conversation.NO_POSITION;
            } catch (final NullPointerException e) {
                // TODO(skennedy) Remove this if we find the root cause b/9527863
            }

            if (mSelectedConversationSet.isEmpty()) {
                final String source = (sourceOpt != null) ? sourceOpt : "checkbox";
                Analytics.getInstance().sendEvent("enter_cab_mode", source, null, 0);
            }

            mSelectedConversationSet.toggle(conv);
            if (mSelectedConversationSet.isEmpty()) {
                listView.commitDestructiveActions(true);
            }

            final boolean reverse = !mSelected;
            mPhotoFlipAnimator.startAnimation(reverse);
            mPhotoFlipAnimator.invalidateArea();

            // We update the background after the checked state has changed
            // now that we have a selected background asset. Setting the background
            // usually waits for a layout pass, but we don't need a full layout,
            // just an update to the background.
            requestLayout();

            return true;
        }

        return false;
    }

    /**
     * Toggle the star on this view and update the conversation.
     */
    public void toggleStar() {
        mHeader.conversation.starred = !mHeader.conversation.starred;
        Bitmap starBitmap = getStarBitmap();
        postInvalidate(mCoordinates.starX, mCoordinates.starY, mCoordinates.starX
                + starBitmap.getWidth(),
                mCoordinates.starY + starBitmap.getHeight());
        ConversationCursor cursor = (ConversationCursor) mAdapter.getCursor();
        if (cursor != null) {
            // TODO(skennedy) What about ads?
            cursor.updateBoolean(mHeader.conversation, ConversationColumns.STARRED,
                    mHeader.conversation.starred);
        }
    }

    private boolean isTouchInContactPhoto(float x, float y) {
        // Everything before the right edge of contact photo

        final int threshold = mCoordinates.contactImagesX + mCoordinates.contactImagesWidth
                + sSenderImageTouchSlop;

        // Allow touching a little right of the contact photo when we're already in selection mode
        final float extra;
        if (mSelectedConversationSet == null || mSelectedConversationSet.isEmpty()) {
            extra = 0;
        } else {
            extra = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                    getResources().getDisplayMetrics());
        }

        return mHeader.gadgetMode == ConversationItemViewCoordinates.GADGET_CONTACT_PHOTO
                && x < (threshold + extra)
                && (!isAttachmentPreviewsEnabled() || y < mCoordinates.attachmentPreviewsY);
    }

    private boolean isTouchInInfoIcon(final float x, final float y) {
        if (mHeader.infoIcon == null) {
            // We have no info icon
            return false;
        }

        // Regardless of device, we always want to be right of the date's left touch slop
        if (x < mDateX - sStarTouchSlop) {
            return false;
        }

        if (mStarEnabled) {
            if (mIsExpansiveTablet) {
                // Just check that we're left of the star's touch area
                if (x >= mCoordinates.starX - sStarTouchSlop) {
                    return false;
                }
            } else {
                // We're on a phone or non-expansive tablet

                // We allow touches all the way to the right edge, so no x check is necessary

                // We need to be above the star's touch area, which ends at the top of the subject
                // text
                return y < mCoordinates.subjectY;
            }
        }

        // With no star below the info icon, we allow touches anywhere from the top edge to the
        // bottom edge, or to the top of the attachment previews, whichever is higher
        return !isAttachmentPreviewsEnabled() || y < mCoordinates.attachmentPreviewsY;
    }

    private boolean isTouchInStar(float x, float y) {
        if (mHeader.infoIcon != null && !mIsExpansiveTablet) {
            // We have an info icon, and it's above the star
            // We allow touches everywhere below the top of the subject text
            if (y < mCoordinates.subjectY) {
                return false;
            }
        }

        // Everything after the star and include a touch slop.
        return mStarEnabled
                && x > mCoordinates.starX - sStarTouchSlop
                && (!isAttachmentPreviewsEnabled() || y < mCoordinates.attachmentPreviewsY);
    }

    @Override
    public boolean canChildBeDismissed() {
        return true;
    }

    @Override
    public void dismiss() {
        SwipeableListView listView = getListView();
        if (listView != null) {
            getListView().dismissChild(this);
        }
    }

    private boolean onTouchEventNoSwipe(MotionEvent event) {
        Utils.traceBeginSection("on touch event no swipe");
        boolean handled = false;

        int x = (int) event.getX();
        int y = (int) event.getY();
        mLastTouchX = x;
        mLastTouchY = y;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isTouchInContactPhoto(x, y) || isTouchInInfoIcon(x, y) || isTouchInStar(x, y)) {
                    mDownEvent = true;
                    handled = true;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                mDownEvent = false;
                break;

            case MotionEvent.ACTION_UP:
                if (mDownEvent) {
                    if (isTouchInContactPhoto(x, y)) {
                        // Touch on the check mark
                        toggleSelectedState();
                    } else if (isTouchInInfoIcon(x, y)) {
                        if (mConversationItemAreaClickListener != null) {
                            mConversationItemAreaClickListener.onInfoIconClicked();
                        }
                    } else if (isTouchInStar(x, y)) {
                        // Touch on the star
                        if (mConversationItemAreaClickListener == null) {
                            toggleStar();
                        } else {
                            mConversationItemAreaClickListener.onStarClicked();
                        }
                    }
                    handled = true;
                }
                break;
        }

        if (!handled) {
            handled = super.onTouchEvent(event);
        }

        Utils.traceEndSection();
        return handled;
    }

    /**
     * ConversationItemView is given the first chance to handle touch events.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Utils.traceBeginSection("on touch event");
        int x = (int) event.getX();
        int y = (int) event.getY();
        mLastTouchX = x;
        mLastTouchY = y;
        if (!mSwipeEnabled) {
            Utils.traceEndSection();
            return onTouchEventNoSwipe(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isTouchInContactPhoto(x, y) || isTouchInInfoIcon(x, y) || isTouchInStar(x, y)) {
                    mDownEvent = true;
                    Utils.traceEndSection();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mDownEvent) {
                    if (isTouchInContactPhoto(x, y)) {
                        // Touch on the check mark
                        Utils.traceEndSection();
                        mDownEvent = false;
                        toggleSelectedState();
                        Utils.traceEndSection();
                        return true;
                    } else if (isTouchInInfoIcon(x, y)) {
                        // Touch on the info icon
                        mDownEvent = false;
                        if (mConversationItemAreaClickListener != null) {
                            mConversationItemAreaClickListener.onInfoIconClicked();
                        }
                        Utils.traceEndSection();
                        return true;
                    } else if (isTouchInStar(x, y)) {
                        // Touch on the star
                        mDownEvent = false;
                        if (mConversationItemAreaClickListener == null) {
                            toggleStar();
                        } else {
                            mConversationItemAreaClickListener.onStarClicked();
                        }
                        Utils.traceEndSection();
                        return true;
                    }
                }
                break;
        }
        // Let View try to handle it as well.
        boolean handled = super.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Utils.traceEndSection();
            return true;
        }
        Utils.traceEndSection();
        return handled;
    }

    @Override
    public boolean performClick() {
        final boolean handled = super.performClick();
        final SwipeableListView list = getListView();
        if (!handled && list != null && list.getAdapter() != null) {
            final int pos = list.findConversation(this, mHeader.conversation);
            list.performItemClick(this, pos, mHeader.conversation.id);
        }
        return handled;
    }

    private View unwrap() {
        final ViewParent vp = getParent();
        if (vp == null || !(vp instanceof View)) {
            return null;
        }
        return (View) vp;
    }

    private SwipeableListView getListView() {
        SwipeableListView v = null;
        final View wrapper = unwrap();
        if (wrapper != null && wrapper instanceof SwipeableConversationItemView) {
            v = (SwipeableListView) ((SwipeableConversationItemView) wrapper).getListView();
        }
        if (v == null) {
            v = mAdapter.getListView();
        }
        return v;
    }

    /**
     * Reset any state associated with this conversation item view so that it
     * can be reused.
     */
    public void reset() {
        Utils.traceBeginSection("reset");
        setAlpha(1f);
        setTranslationX(0f);
        mAnimatedHeightFraction = 1.0f;
        Utils.traceEndSection();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setTranslationX(float translationX) {
        super.setTranslationX(translationX);

        // When a list item is being swiped or animated, ensure that the hosting view has a
        // background color set. We only enable the background during the X-translation effect to
        // reduce overdraw during normal list scrolling.
        final View parent = (View) getParent();
        if (parent == null) {
            LogUtils.w(LOG_TAG, "CIV.setTranslationX null ConversationItemView parent x=%s",
                    translationX);
        }

        if (parent instanceof SwipeableConversationItemView) {
            if (translationX != 0f) {
                parent.setBackgroundResource(R.color.swiped_bg_color);
            } else {
                parent.setBackgroundDrawable(null);
            }
        }
    }

    /**
     * Grow the height of the item and fade it in when bringing a conversation
     * back from a destructive action.
     */
    public Animator createSwipeUndoAnimation() {
        ObjectAnimator undoAnimator = createTranslateXAnimation(true);
        return undoAnimator;
    }

    /**
     * Grow the height of the item and fade it in when bringing a conversation
     * back from a destructive action.
     */
    public Animator createUndoAnimation() {
        ObjectAnimator height = createHeightAnimation(true);
        Animator fade = ObjectAnimator.ofFloat(this, "alpha", 0, 1.0f);
        fade.setDuration(sShrinkAnimationDuration);
        fade.setInterpolator(new DecelerateInterpolator(2.0f));
        AnimatorSet transitionSet = new AnimatorSet();
        transitionSet.playTogether(height, fade);
        transitionSet.addListener(new HardwareLayerEnabler(this));
        return transitionSet;
    }

    /**
     * Grow the height of the item and fade it in when bringing a conversation
     * back from a destructive action.
     */
    public Animator createDestroyWithSwipeAnimation() {
        ObjectAnimator slide = createTranslateXAnimation(false);
        ObjectAnimator height = createHeightAnimation(false);
        AnimatorSet transitionSet = new AnimatorSet();
        transitionSet.playSequentially(slide, height);
        return transitionSet;
    }

    private ObjectAnimator createTranslateXAnimation(boolean show) {
        SwipeableListView parent = getListView();
        // If we can't get the parent...we have bigger problems.
        int width = parent != null ? parent.getMeasuredWidth() : 0;
        final float start = show ? width : 0f;
        final float end = show ? 0f : width;
        ObjectAnimator slide = ObjectAnimator.ofFloat(this, "translationX", start, end);
        slide.setInterpolator(new DecelerateInterpolator(2.0f));
        slide.setDuration(sSlideAnimationDuration);
        return slide;
    }

    public Animator createDestroyAnimation() {
        return createHeightAnimation(false);
    }

    private ObjectAnimator createHeightAnimation(boolean show) {
        final float start = show ? 0f : 1.0f;
        final float end = show ? 1.0f : 0f;
        ObjectAnimator height = ObjectAnimator.ofFloat(this, "animatedHeightFraction", start, end);
        height.setInterpolator(new DecelerateInterpolator(2.0f));
        height.setDuration(sShrinkAnimationDuration);
        return height;
    }

    // Used by animator
    public void setAnimatedHeightFraction(float height) {
        mAnimatedHeightFraction = height;
        requestLayout();
    }

    @Override
    public SwipeableView getSwipeableView() {
        return SwipeableView.from(this);
    }

    /**
     * Begin drag mode. Keep the conversation selected (NOT toggle selection) and start drag.
     */
    private boolean beginDragMode() {
        if (mLastTouchX < 0 || mLastTouchY < 0 ||  mSelectedConversationSet == null) {
            return false;
        }
        // If this is already checked, don't bother unchecking it!
        if (!mSelected) {
            toggleSelectedState();
        }

        // Clip data has form: [conversations_uri, conversationId1,
        // maxMessageId1, label1, conversationId2, maxMessageId2, label2, ...]
        final int count = mSelectedConversationSet.size();
        String description = Utils.formatPlural(mContext, R.plurals.move_conversation, count);

        final ClipData data = ClipData.newUri(mContext.getContentResolver(), description,
                Conversation.MOVE_CONVERSATIONS_URI);
        for (Conversation conversation : mSelectedConversationSet.values()) {
            data.addItem(new Item(String.valueOf(conversation.position)));
        }
        // Protect against non-existent views: only happens for monkeys
        final int width = this.getWidth();
        final int height = this.getHeight();
        final boolean isDimensionNegative = (width < 0) || (height < 0);
        if (isDimensionNegative) {
            LogUtils.e(LOG_TAG, "ConversationItemView: dimension is negative: "
                        + "width=%d, height=%d", width, height);
            return false;
        }
        mActivity.startDragMode();
        // Start drag mode
        startDrag(data, new ShadowBuilder(this, count, mLastTouchX, mLastTouchY), null, 0);

        return true;
    }

    /**
     * Handles the drag event.
     *
     * @param event the drag event to be handled
     */
    @Override
    public boolean onDragEvent(DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_ENDED:
                mActivity.stopDragMode();
                return true;
        }
        return false;
    }

    private class ShadowBuilder extends DragShadowBuilder {
        private final Drawable mBackground;

        private final View mView;
        private final String mDragDesc;
        private final int mTouchX;
        private final int mTouchY;
        private int mDragDescX;
        private int mDragDescY;

        public ShadowBuilder(View view, int count, int touchX, int touchY) {
            super(view);
            mView = view;
            mBackground = mView.getResources().getDrawable(R.drawable.list_pressed_holo);
            mDragDesc = Utils.formatPlural(mView.getContext(), R.plurals.move_conversation, count);
            mTouchX = touchX;
            mTouchY = touchY;
        }

        @Override
        public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
            final int width = mView.getWidth();
            final int height = mView.getHeight();

            sPaint.setTextSize(mCoordinates.subjectFontSize);
            mDragDescX = mCoordinates.sendersX;
            mDragDescY = (height - (int) mCoordinates.subjectFontSize) / 2 ;
            shadowSize.set(width, height);
            shadowTouchPoint.set(mTouchX, mTouchY);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            mBackground.setBounds(0, 0, mView.getWidth(), mView.getHeight());
            mBackground.draw(canvas);
            sPaint.setTextSize(mCoordinates.subjectFontSize);
            canvas.drawText(mDragDesc, mDragDescX, mDragDescY - sPaint.ascent(), sPaint);
        }
    }

    @Override
    public float getMinAllowScrollDistance() {
        return sScrollSlop;
    }

    private abstract class CabAnimator {
        private ObjectAnimator mAnimator = null;

        private final String mPropertyName;

        private float mValue;

        private final float mStartValue;
        private final float mEndValue;

        private final long mDuration;

        private boolean mReversing = false;

        public CabAnimator(final String propertyName, final float startValue, final float endValue,
                final long duration) {
            mPropertyName = propertyName;

            mStartValue = startValue;
            mEndValue = endValue;

            mDuration = duration;
        }

        private ObjectAnimator createAnimator() {
            final ObjectAnimator animator = ObjectAnimator.ofFloat(ConversationItemView.this,
                    mPropertyName, mStartValue, mEndValue);
            animator.setDuration(mDuration);
            animator.setInterpolator(new LinearInterpolator());
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animation) {
                    invalidateArea();
                }
            });
            animator.addListener(mAnimatorListener);
            return animator;
        }

        private final AnimatorListener mAnimatorListener = new AnimatorListener() {
            @Override
            public void onAnimationStart(final Animator animation) {
                // Do nothing
            }

            @Override
            public void onAnimationEnd(final Animator animation) {
                if (mReversing) {
                    mReversing = false;
                    // We no longer want to track whether we were last selected,
                    // since we no longer are selected
                    mLastSelectedId = -1;
                }
            }

            @Override
            public void onAnimationCancel(final Animator animation) {
                // Do nothing
            }

            @Override
            public void onAnimationRepeat(final Animator animation) {
                // Do nothing
            }
        };

        public abstract void invalidateArea();

        public void setValue(final float fraction) {
            if (mValue == fraction) {
                return;
            }
            mValue = fraction;
            invalidateArea();
        }

        public float getValue() {
            return mValue;
        }

        /**
         * @param reverse <code>true</code> to animate in reverse
         */
        public void startAnimation(final boolean reverse) {
            if (mAnimator != null) {
                mAnimator.cancel();
            }

            mAnimator = createAnimator();
            mReversing = reverse;

            if (reverse) {
                mAnimator.reverse();
            } else {
                mAnimator.start();
            }
        }

        public void stopAnimation() {
            if (mAnimator != null) {
                mAnimator.cancel();
                mAnimator = null;
            }

            mReversing = false;

            setValue(0);
        }

        public boolean isStarted() {
            return mAnimator != null && mAnimator.isStarted();
        }
    }

    public void setPhotoFlipFraction(final float fraction) {
        mPhotoFlipAnimator.setValue(fraction);
    }

    public String getAccount() {
        return mAccount;
    }
}
