package com.android.mail.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mail.ConversationListContext;
import com.android.mail.R;
import com.android.mail.analytics.Analytics;
import com.android.mail.preferences.AccountPreferences;
import com.android.mail.preferences.MailPrefs;
import com.android.mail.providers.UIProvider.FolderCapabilities;
import com.android.mail.providers.UIProvider.FolderType;
import com.android.mail.ui.ConversationSyncDisabledTipView.ReasonSyncOff;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;

/**
 * Conversation list view contains a {@link SwipeableListView} and a sync status bar above it.
 */
public class ConversationListView extends FrameLayout implements SwipeableListView.SwipeListener {

    private static final int MIN_DISTANCE_TO_TRIGGER_SYNC = 150; // dp
    private static final int MAX_DISTANCE_TO_TRIGGER_SYNC = 300; // dp

    private static final int DISTANCE_TO_IGNORE = 15; // dp
    private static final int DISTANCE_TO_TRIGGER_CANCEL = 10; // dp
    private static final int SHOW_CHECKING_FOR_MAIL_DURATION_IN_MILLIS = 1 * 1000; // 1 seconds

    private static final int SWIPE_TEXT_APPEAR_DURATION_IN_MILLIS = 200;
    private static final int SYNC_STATUS_BAR_FADE_DURATION_IN_MILLIS = 150;
    private static final int SYNC_TRIGGER_SHRINK_DURATION_IN_MILLIS = 250;

    // Max number of times we display the same sync turned off warning message in a toast.
    // After we reach this max, and device/account still has sync off, we assume user has
    // intentionally disabled sync and no longer warn.
    private static final int MAX_NUM_OF_SYNC_TOASTS = 5;

    private static final String LOG_TAG = LogTag.getLogTag();

    private View mSyncTriggerBar;
    private View mSyncProgressBar;
    private final AnimatorListenerAdapter mSyncDismissListener;
    private SwipeableListView mListView;

    // Whether to ignore events in {#dispatchTouchEvent}.
    private boolean mIgnoreTouchEvents = false;

    private boolean mTrackingScrollMovement = false;
    // Y coordinate of where scroll started
    private float mTrackingScrollStartY;
    // Max Y coordinate reached since starting scroll, this is used to know whether
    // user moved back up which should cancel the current tracking state and hide the
    // sync trigger bar.
    private float mTrackingScrollMaxY;
    private boolean mIsSyncing = false;

    private final Interpolator mAccelerateInterpolator = new AccelerateInterpolator(1.5f);
    private final Interpolator mDecelerateInterpolator = new DecelerateInterpolator(1.5f);

    private float mDensity;

    private ControllableActivity mActivity;
    private final WindowManager mWindowManager;
    private final HintText mHintText;
    private boolean mHasHintTextViewBeenAdded = false;

    // Minimum vertical distance (in dips) of swipe to trigger a sync.
    // This value can be different based on the device.
    private float mDistanceToTriggerSyncDp = MIN_DISTANCE_TO_TRIGGER_SYNC;

    private ConversationListContext mConvListContext;

    private final MailPrefs mMailPrefs;
    private AccountPreferences mAccountPreferences;

    // Instantiated through view inflation
    @SuppressWarnings("unused")
    public ConversationListView(Context context) {
        this(context, null);
    }

    public ConversationListView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public ConversationListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mHintText = new ConversationListView.HintText(context);

        mSyncDismissListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator arg0) {
                mSyncProgressBar.setVisibility(GONE);
                mSyncTriggerBar.setVisibility(GONE);
            }
        };

        mMailPrefs = MailPrefs.get(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mListView = (SwipeableListView) findViewById(android.R.id.list);
        mListView.setSwipeListener(this);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        mDensity = displayMetrics.density;

        // Calculate distance threshold for triggering a sync based on
        // screen height.  Apply a min and max cutoff.
        float threshold = (displayMetrics.heightPixels) / mDensity / 2.5f;
        mDistanceToTriggerSyncDp = Math.max(
                Math.min(threshold, MAX_DISTANCE_TO_TRIGGER_SYNC),
                MIN_DISTANCE_TO_TRIGGER_SYNC);
    }

    protected void setActivity(ControllableActivity activity) {
        mActivity = activity;
    }

    protected void setConversationContext(ConversationListContext convListContext) {
        mConvListContext = convListContext;
        mAccountPreferences = AccountPreferences.get(getContext(),
                convListContext.account.getEmailAddress());
    }

    @Override
    public void onBeginSwipe() {
        mIgnoreTouchEvents = true;
        if (mTrackingScrollMovement) {
            cancelMovementTracking();
        }
    }

    private void addHintTextViewIfNecessary() {
        if (!mHasHintTextViewBeenAdded) {
            mWindowManager.addView(mHintText, getRefreshHintTextLayoutParams());
            mHasHintTextViewBeenAdded = true;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // Delayed to this step because activity has to be running in order for view to be
        // successfully added to the window manager.
        addHintTextViewIfNecessary();

        // First check for any events that can trigger end of a swipe, so we can reset
        // mIgnoreTouchEvents back to false (it can only be set to true at beginning of swipe)
        // via {#onBeginSwipe()} callback.
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIgnoreTouchEvents = false;
        }

        if (mIgnoreTouchEvents) {
            return super.dispatchTouchEvent(event);
        }

        float y = event.getY(0);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mIsSyncing) {
                    break;
                }
                // Disable swipe to refresh in search results page
                if (ConversationListContext.isSearchResult(mConvListContext)) {
                    break;
                }
                // Disable swipe to refresh in CAB mode
                if (mActivity.getSelectedSet() != null &&
                        mActivity.getSelectedSet().size() > 0) {
                    break;
                }
                // Only if we have reached the top of the list, any further scrolling
                // can potentially trigger a sync.
                if (mListView.getChildCount() == 0 || mListView.getChildAt(0).getTop() == 0) {
                    startMovementTracking(y);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTrackingScrollMovement) {
                    if (mActivity.getFolderController().getFolder().isDraft()) {
                        // Don't allow refreshing of DRAFT folders. See b/11158759
                        LogUtils.d(LOG_TAG, "ignoring swipe to refresh on DRAFT folder");
                        break;
                    }
                    if (mActivity.getFolderController().getFolder().supportsCapability(
                            FolderCapabilities.IS_VIRTUAL)) {
                        // Don't allow refreshing of virtual folders.
                        LogUtils.d(LOG_TAG, "ignoring swipe to refresh on virtual folder");
                        break;
                    }
                    // Sync is triggered when tap and drag distance goes over a certain threshold
                    float verticalDistancePx = y - mTrackingScrollStartY;
                    float verticalDistanceDp = verticalDistancePx / mDensity;
                    if (verticalDistanceDp > mDistanceToTriggerSyncDp) {
                        LogUtils.i(LOG_TAG, "Sync triggered from distance");
                        triggerSync();
                        break;
                    }

                    // Moving back up vertically should be handled the same as CANCEL / UP:
                    float verticalDistanceFromMaxPx = mTrackingScrollMaxY - y;
                    float verticalDistanceFromMaxDp = verticalDistanceFromMaxPx / mDensity;
                    if (verticalDistanceFromMaxDp > DISTANCE_TO_TRIGGER_CANCEL) {
                        cancelMovementTracking();
                        break;
                    }

                    // Otherwise hint how much further user needs to drag to trigger sync by
                    // expanding the sync status bar proportional to how far they have dragged.
                    if (verticalDistanceDp < DISTANCE_TO_IGNORE) {
                        // Ignore small movements such as tap
                        verticalDistanceDp = 0;
                    } else {
                        mHintText.displaySwipeToRefresh();
                    }
                    setTriggerScale(mAccelerateInterpolator.getInterpolation(
                            verticalDistanceDp/mDistanceToTriggerSyncDp));

                    if (y > mTrackingScrollMaxY) {
                        mTrackingScrollMaxY = y;
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mTrackingScrollMovement) {
                    cancelMovementTracking();
                }
                break;
        }

        return super.dispatchTouchEvent(event);
    }

    private void startMovementTracking(float y) {
        LogUtils.d(LOG_TAG, "Start swipe to refresh tracking");
        mTrackingScrollMovement = true;
        mTrackingScrollStartY = y;
        mTrackingScrollMaxY = mTrackingScrollStartY;
    }

    private void cancelMovementTracking() {
        if (mTrackingScrollMovement) {
            // Shrink the status bar when user lifts finger and no sync has happened yet
            if (mSyncTriggerBar != null) {
                mSyncTriggerBar.animate()
                        .scaleX(0f)
                        .setInterpolator(mDecelerateInterpolator)
                        .setDuration(SYNC_TRIGGER_SHRINK_DURATION_IN_MILLIS)
                        .setListener(mSyncDismissListener)
                        .start();
            }
            mTrackingScrollMovement = false;
        }
        mHintText.hide();
    }

    private void setTriggerScale(float scale) {
        if (scale == 0f && mSyncTriggerBar == null) {
            // No-op. A null trigger means it's uninitialized, and setting it to zero-scale
            // means we're trying to reset state, so there's nothing to reset in this case.
            return;
        } else if (mSyncTriggerBar != null) {
            // reset any leftover trigger visual state
            mSyncTriggerBar.animate().cancel();
            mSyncTriggerBar.setVisibility(VISIBLE);
        }
        ensureProgressBars();
        mSyncTriggerBar.setScaleX(scale);
    }

    private void ensureProgressBars() {
        if (mSyncTriggerBar == null || mSyncProgressBar == null) {
            final LayoutInflater inflater = LayoutInflater.from(getContext());
            inflater.inflate(R.layout.conversation_list_progress, this, true /* attachToRoot */);
            mSyncTriggerBar = findViewById(R.id.sync_trigger);
            mSyncProgressBar = findViewById(R.id.progress);
        }
    }

    private void triggerSync() {
        ensureProgressBars();
        mSyncTriggerBar.setVisibility(View.GONE);

        Analytics.getInstance().sendEvent(Analytics.EVENT_CATEGORY_MENU_ITEM, "swipe_refresh", null,
                0);

        // This will call back to showSyncStatusBar():
        mActivity.getFolderController().requestFolderRefresh();

        // Any continued dragging after this should have no effect
        mTrackingScrollMovement = false;

        mHintText.displayCheckingForMailAndHideAfterDelay();
    }

    protected void showSyncStatusBar() {
        if (!mIsSyncing) {
            mIsSyncing = true;

            LogUtils.i(LOG_TAG, "ConversationListView show sync status bar");
            ensureProgressBars();
            mSyncTriggerBar.setVisibility(GONE);
            mSyncProgressBar.setVisibility(VISIBLE);
            mSyncProgressBar.setAlpha(1f);

            showToastIfSyncIsOff();
        }
    }

    // If sync is turned off on this device or account, remind the user with a toast.
    private void showToastIfSyncIsOff() {
        final int reasonSyncOff = ConversationSyncDisabledTipView.calculateReasonSyncOff(
                mMailPrefs, mConvListContext.account, mAccountPreferences);
        switch (reasonSyncOff) {
            case ReasonSyncOff.AUTO_SYNC_OFF:
                // TODO: make this an actionable toast, tapping on it goes to Settings
                int num = mMailPrefs.getNumOfDismissesForAutoSyncOff();
                if (num > 0 && num <= MAX_NUM_OF_SYNC_TOASTS) {
                    Toast.makeText(getContext(), R.string.auto_sync_off, Toast.LENGTH_SHORT)
                            .show();
                    mMailPrefs.incNumOfDismissesForAutoSyncOff();
                }
                break;
            case ReasonSyncOff.ACCOUNT_SYNC_OFF:
                // TODO: make this an actionable toast, tapping on it goes to Settings
                num = mAccountPreferences.getNumOfDismissesForAccountSyncOff();
                if (num > 0 && num <= MAX_NUM_OF_SYNC_TOASTS) {
                    Toast.makeText(getContext(), R.string.account_sync_off, Toast.LENGTH_SHORT)
                            .show();
                    mAccountPreferences.incNumOfDismissesForAccountSyncOff();
                }
                break;
        }
    }

    protected void onSyncFinished() {
        // onSyncFinished() can get called several times as result of folder updates that maybe
        // or may not be related to sync.
        if (mIsSyncing) {
            LogUtils.i(LOG_TAG, "ConversationListView hide sync status bar");
            // Hide both the sync progress bar and sync trigger bar
            mSyncProgressBar.animate().alpha(0f)
                    .setDuration(SYNC_STATUS_BAR_FADE_DURATION_IN_MILLIS)
                    .setListener(mSyncDismissListener);
            mSyncTriggerBar.setVisibility(GONE);
            // Hide the "Checking for mail" text in action bar if it isn't hidden already:
            mHintText.hide();
            mIsSyncing = false;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mHasHintTextViewBeenAdded) {
            try {
                mWindowManager.removeView(mHintText);
            } catch (IllegalArgumentException e) {
                // Have seen this happen on occasion during orientation change.
            }
        }
    }

    private WindowManager.LayoutParams getRefreshHintTextLayoutParams() {
        // Create the "Swipe down to refresh" text view that covers the action bar.
        Rect rect= new Rect();
        Window window = mActivity.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);
        int statusBarHeight = rect.top;

        final TypedArray actionBarSize = ((Activity) mActivity).obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize});
        int actionBarHeight = actionBarSize.getDimensionPixelSize(0, 0);
        actionBarSize.recycle();

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                actionBarHeight,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP;
        params.x = 0;
        params.y = statusBarHeight;
        return params;
    }

    /**
     * A text view that covers the entire action bar, used for displaying
     * "Swipe down to refresh" hint text if user has initiated a downward swipe.
     */
    protected static class HintText extends FrameLayout {

        private static final int NONE = 0;
        private static final int SWIPE_TO_REFRESH = 1;
        private static final int CHECKING_FOR_MAIL = 2;

        // Can be one of NONE, SWIPE_TO_REFRESH, CHECKING_FOR_MAIL
        private int mDisplay;

        private final TextView mTextView;

        private final Interpolator mDecelerateInterpolator = new DecelerateInterpolator(1.5f);
        private final Interpolator mAccelerateInterpolator = new AccelerateInterpolator(1.5f);

        private final Runnable mHideHintTextRunnable = new Runnable() {
            @Override
            public void run() {
                hide();
            }
        };
        private final Runnable mSetVisibilityGoneRunnable = new Runnable() {
            @Override
            public void run() {
                setVisibility(View.GONE);
            }
        };

        public HintText(final Context context) {
            this(context, null);
        }

        public HintText(final Context context, final AttributeSet attrs) {
            this(context, attrs, -1);
        }

        public HintText(final Context context, final AttributeSet attrs, final int defStyle) {
            super(context, attrs, defStyle);

            final LayoutInflater factory = LayoutInflater.from(context);
            factory.inflate(R.layout.swipe_to_refresh, this);

            mTextView = (TextView) findViewById(R.id.swipe_text);

            mDisplay = NONE;
            setVisibility(View.GONE);

            // Set background color to be same as action bar color
            final int actionBarRes = Utils.getActionBarBackgroundResource(context);
            setBackgroundResource(actionBarRes);
        }

        private void displaySwipeToRefresh() {
            if (mDisplay != SWIPE_TO_REFRESH) {
                mTextView.setText(getResources().getText(R.string.swipe_down_to_refresh));
                // Covers the current action bar:
                setVisibility(View.VISIBLE);
                setAlpha(1f);
                // Animate text sliding down onto action bar:
                mTextView.setY(-mTextView.getHeight());
                mTextView.animate().y(0)
                        .setInterpolator(mDecelerateInterpolator)
                        .setDuration(SWIPE_TEXT_APPEAR_DURATION_IN_MILLIS);
                mDisplay = SWIPE_TO_REFRESH;
            }
        }

        private void displayCheckingForMailAndHideAfterDelay() {
            mTextView.setText(getResources().getText(R.string.checking_for_mail));
            setVisibility(View.VISIBLE);
            mDisplay = CHECKING_FOR_MAIL;
            postDelayed(mHideHintTextRunnable, SHOW_CHECKING_FOR_MAIL_DURATION_IN_MILLIS);
        }

        private void hide() {
            if (mDisplay != NONE) {
                // Animate text sliding up leaving behind a blank action bar
                mTextView.animate().y(-mTextView.getHeight())
                        .setInterpolator(mAccelerateInterpolator)
                        .setDuration(SWIPE_TEXT_APPEAR_DURATION_IN_MILLIS)
                        .start();
                animate().alpha(0f)
                        .setDuration(SWIPE_TEXT_APPEAR_DURATION_IN_MILLIS);
                postDelayed(mSetVisibilityGoneRunnable, SWIPE_TEXT_APPEAR_DURATION_IN_MILLIS);
                mDisplay = NONE;
            }
        }
    }
}
