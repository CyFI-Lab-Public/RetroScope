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

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.mail.R;
import com.android.mail.analytics.Analytics;
import com.android.mail.providers.Attachment;
import com.android.mail.providers.UIProvider.AttachmentDestination;
import com.android.mail.providers.UIProvider.AttachmentState;
import com.android.mail.utils.AttachmentUtils;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.MimeType;
import com.android.mail.utils.Utils;

/**
 * View for a single attachment in conversation view. Shows download status and allows launching
 * intents to act on an attachment.
 *
 */
public class MessageAttachmentBar extends FrameLayout implements OnClickListener,
        OnMenuItemClickListener, AttachmentViewInterface {

    private Attachment mAttachment;
    private TextView mTitle;
    private TextView mSubTitle;
    private String mAttachmentSizeText;
    private String mDisplayType;
    private ProgressBar mProgress;
    private ImageButton mCancelButton;
    private PopupMenu mPopup;
    private ImageView mOverflowButton;

    private final AttachmentActionHandler mActionHandler;
    private boolean mSaveClicked;
    private Uri mAccountUri;

    private final Runnable mUpdateRunnable = new Runnable() {
            @Override
        public void run() {
            updateActionsInternal();
        }
    };

    private static final String LOG_TAG = LogTag.getLogTag();


    public MessageAttachmentBar(Context context) {
        this(context, null);
    }

    public MessageAttachmentBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        mActionHandler = new AttachmentActionHandler(context, this);
    }

    public void initialize(FragmentManager fragmentManager) {
        mActionHandler.initialize(fragmentManager);
    }

    public static MessageAttachmentBar inflate(LayoutInflater inflater, ViewGroup parent) {
        MessageAttachmentBar view = (MessageAttachmentBar) inflater.inflate(
                R.layout.conversation_message_attachment_bar, parent, false);
        return view;
    }

    /**
     * Render or update an attachment's view. This happens immediately upon instantiation, and
     * repeatedly as status updates stream in, so only properties with new or changed values will
     * cause sub-views to update.
     */
    public void render(Attachment attachment, Uri accountUri, boolean loaderResult) {
        // get account uri for potential eml viewer usage
        mAccountUri = accountUri;

        final Attachment prevAttachment = mAttachment;
        mAttachment = attachment;
        mActionHandler.setAttachment(mAttachment);

        // reset mSaveClicked if we are not currently downloading
        // So if the download fails or the download completes, we stop
        // showing progress, etc
        mSaveClicked = !attachment.isDownloading() ? false : mSaveClicked;

        LogUtils.d(LOG_TAG, "got attachment list row: name=%s state/dest=%d/%d dled=%d" +
                " contentUri=%s MIME=%s flags=%d", attachment.getName(), attachment.state,
                attachment.destination, attachment.downloadedSize, attachment.contentUri,
                attachment.getContentType(), attachment.flags);

        if ((attachment.flags & Attachment.FLAG_DUMMY_ATTACHMENT) != 0) {
            mTitle.setText(R.string.load_attachment);
        } else if (prevAttachment == null
                || !TextUtils.equals(attachment.getName(), prevAttachment.getName())) {
            mTitle.setText(attachment.getName());
        }

        if (prevAttachment == null || attachment.size != prevAttachment.size) {
            mAttachmentSizeText = AttachmentUtils.convertToHumanReadableSize(getContext(),
                    attachment.size);
            mDisplayType = AttachmentUtils.getDisplayType(getContext(), attachment);
            updateSubtitleText();
        }

        updateActions();
        mActionHandler.updateStatus(loaderResult);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTitle = (TextView) findViewById(R.id.attachment_title);
        mSubTitle = (TextView) findViewById(R.id.attachment_subtitle);
        mProgress = (ProgressBar) findViewById(R.id.attachment_progress);
        mOverflowButton = (ImageView) findViewById(R.id.overflow);
        mCancelButton = (ImageButton) findViewById(R.id.cancel_attachment);

        setOnClickListener(this);
        mOverflowButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        onClick(v.getId(), v);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        mPopup.dismiss();
        return onClick(item.getItemId(), null);
    }

    private boolean onClick(final int res, final View v) {
        if (res == R.id.preview_attachment) {
            previewAttachment();
        } else if (res == R.id.save_attachment) {
            if (mAttachment.canSave()) {
                mActionHandler.startDownloadingAttachment(AttachmentDestination.EXTERNAL);
                mSaveClicked = true;

                Analytics.getInstance().sendEvent(
                        "save_attachment", Utils.normalizeMimeType(mAttachment.getContentType()),
                        "attachment_bar", mAttachment.size);
            }
        } else if (res == R.id.download_again) {
            if (mAttachment.isPresentLocally()) {
                mActionHandler.showDownloadingDialog();
                mActionHandler.startRedownloadingAttachment(mAttachment);

                Analytics.getInstance().sendEvent("redownload_attachment",
                        Utils.normalizeMimeType(mAttachment.getContentType()), "attachment_bar",
                        mAttachment.size);
            }
        } else if (res == R.id.cancel_attachment) {
            mActionHandler.cancelAttachment();
            mSaveClicked = false;

            Analytics.getInstance().sendEvent(
                    "cancel_attachment", Utils.normalizeMimeType(mAttachment.getContentType()),
                    "attachment_bar", mAttachment.size);
        } else if (res == R.id.overflow) {
            // If no overflow items are visible, just bail out.
            // We shouldn't be able to get here anyhow since the overflow
            // button should be hidden.
            if (shouldShowOverflow()) {
                if (mPopup == null) {
                    mPopup = new PopupMenu(getContext(), v);
                    mPopup.getMenuInflater().inflate(R.menu.message_footer_overflow_menu,
                            mPopup.getMenu());
                    mPopup.setOnMenuItemClickListener(this);
                }

                final Menu menu = mPopup.getMenu();
                menu.findItem(R.id.preview_attachment).setVisible(shouldShowPreview());
                menu.findItem(R.id.save_attachment).setVisible(shouldShowSave());
                menu.findItem(R.id.download_again).setVisible(shouldShowDownloadAgain());

                mPopup.show();
            }
        } else {
            // Handles clicking the attachment
            // in any area that is not the overflow
            // button or cancel button or one of the
            // overflow items.
            final String mime = Utils.normalizeMimeType(mAttachment.getContentType());
            final String action;

            if ((mAttachment.flags & Attachment.FLAG_DUMMY_ATTACHMENT) != 0) {
                // This is a dummy. We need to download it, but not attempt to open or preview.
                mActionHandler.showDownloadingDialog();
                mActionHandler.setViewOnFinish(false);
                mActionHandler.startDownloadingAttachment(AttachmentDestination.CACHE);

                action = null;
            }
            // If the mimetype is blocked, show the info dialog
            else if (MimeType.isBlocked(mAttachment.getContentType())) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                int dialogMessage = R.string.attachment_type_blocked;
                builder.setTitle(R.string.more_info_attachment)
                       .setMessage(dialogMessage)
                       .show();

                action = "attachment_bar_blocked";
            }
            // If we can install, install.
            else if (MimeType.isInstallable(mAttachment.getContentType())) {
                // Save to external because the package manager only handles
                // file:// uris not content:// uris. We do the same
                // workaround in
                // UiProvider#getUiAttachmentsCursorForUIAttachments()
                mActionHandler.showAttachment(AttachmentDestination.EXTERNAL);

                action = "attachment_bar_install";
            }
            // If we can view or play with an on-device app,
            // view or play.
            else if (MimeType.isViewable(
                    getContext(), mAttachment.contentUri, mAttachment.getContentType())) {
                mActionHandler.showAttachment(AttachmentDestination.CACHE);

                action = "attachment_bar";
            }
            // If we can only preview the attachment, preview.
            else if (mAttachment.canPreview()) {
                previewAttachment();

                action = null;
            }
            // Otherwise, if we cannot do anything, show the info dialog.
            else {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                int dialogMessage = R.string.no_application_found;
                builder.setTitle(R.string.more_info_attachment)
                       .setMessage(dialogMessage)
                       .show();

                action = "attachment_bar_no_viewer";
            }

            if (action != null) {
                Analytics.getInstance()
                        .sendEvent("view_attachment", mime, action, mAttachment.size);
            }
        }

        return true;
    }

    private boolean shouldShowPreview() {
        // state could be anything
        return mAttachment.canPreview();
    }

    private boolean shouldShowSave() {
        return mAttachment.canSave() && !mSaveClicked;
    }

    private boolean shouldShowDownloadAgain() {
        // implies state == SAVED || state == FAILED
        // and the attachment supports re-download
        return mAttachment.supportsDownloadAgain() && mAttachment.isDownloadFinishedOrFailed();
    }

    private boolean shouldShowOverflow() {
        return (shouldShowPreview() || shouldShowSave() || shouldShowDownloadAgain())
                && !shouldShowCancel();
    }

    private boolean shouldShowCancel() {
        return mAttachment.isDownloading() && mSaveClicked;
    }

    @Override
    public void viewAttachment() {
        if (mAttachment.contentUri == null) {
            LogUtils.e(LOG_TAG, "viewAttachment with null content uri");
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        final String contentType = mAttachment.getContentType();
        Utils.setIntentDataAndTypeAndNormalize(
                intent, mAttachment.contentUri, contentType);

        // For EML files, we want to open our dedicated
        // viewer rather than let any activity open it.
        if (MimeType.isEmlMimeType(contentType)) {
            intent.setClass(getContext(), EmlViewerActivity.class);
            intent.putExtra(EmlViewerActivity.EXTRA_ACCOUNT_URI, mAccountUri);
        }

        try {
            getContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // couldn't find activity for View intent
            LogUtils.e(LOG_TAG, e, "Couldn't find Activity for intent");
        }
    }

    private void previewAttachment() {
        if (mAttachment.canPreview()) {
            final Intent previewIntent =
                    new Intent(Intent.ACTION_VIEW, mAttachment.previewIntentUri);
            getContext().startActivity(previewIntent);

            Analytics.getInstance().sendEvent(
                    "preview_attachment", Utils.normalizeMimeType(mAttachment.getContentType()),
                    null, mAttachment.size);
        }
    }

    private static void setButtonVisible(View button, boolean visible) {
        button.setVisibility(visible ? VISIBLE : GONE);
    }

    /**
     * Update all actions based on current downloading state.
     */
    private void updateActions() {
        removeCallbacks(mUpdateRunnable);
        post(mUpdateRunnable);
    }

    private void updateActionsInternal() {
        // If the progress dialog is visible, skip any of the updating
        if (mActionHandler.isProgressDialogVisible()) {
            return;
        }

        // To avoid visibility state transition bugs, every button's visibility should be touched
        // once by this routine.
        setButtonVisible(mCancelButton, shouldShowCancel());
        setButtonVisible(mOverflowButton, shouldShowOverflow());
    }

    @Override
    public void onUpdateStatus() {
        updateSubtitleText();
    }

    @Override
    public void updateProgress(boolean showProgress) {
        if (mAttachment.isDownloading()) {
            mProgress.setMax(mAttachment.size);
            mProgress.setProgress(mAttachment.downloadedSize);
            mProgress.setIndeterminate(!showProgress);
            mProgress.setVisibility(VISIBLE);
            mSubTitle.setVisibility(INVISIBLE);
        } else {
            mProgress.setVisibility(INVISIBLE);
            mSubTitle.setVisibility(VISIBLE);
        }
    }

    private void updateSubtitleText() {
        // TODO: make this a formatted resource when we have a UX design.
        // not worth translation right now.
        final StringBuilder sb = new StringBuilder();
        if (mAttachment.state == AttachmentState.FAILED) {
            sb.append(getResources().getString(R.string.download_failed));
        } else {
            if (mAttachment.isSavedToExternal()) {
                sb.append(getResources().getString(R.string.saved, mAttachmentSizeText));
            } else {
                sb.append(mAttachmentSizeText);
            }
            if (mDisplayType != null) {
                sb.append(' ');
                sb.append(mDisplayType);
            }
        }
        mSubTitle.setText(sb.toString());
    }
}
