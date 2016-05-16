package com.android.mail.ui;

import android.animation.ObjectAnimator;
import android.app.LoaderManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.mail.R;
import com.android.mail.analytics.Analytics;
import com.android.mail.browse.ConversationCursor;
import com.android.mail.preferences.MailPrefs;
import com.android.mail.providers.Folder;
import com.android.mail.utils.Utils;

/**
 * A teaser to introduce people to the contact photo check boxes
 */
public class ConversationPhotoTeaserView extends FrameLayout
        implements ConversationSpecialItemView, SwipeableItemView {
    private static int sScrollSlop = 0;
    private static int sShrinkAnimationDuration;

    private final MailPrefs mMailPrefs;
    private AnimatedAdapter mAdapter;

    private View mSwipeableContent;

    private boolean mShown;
    private int mAnimatedHeight = -1;
    private boolean mNeedLayout;
    private int mTextTop;

    private View mTeaserRightEdge;
    /** Whether we are on a tablet device or not */
    private final boolean mTabletDevice;
    /** When in conversation mode, true if the list is hidden */
    private final boolean mListCollapsible;

    public ConversationPhotoTeaserView(final Context context) {
        this(context, null);
    }

    public ConversationPhotoTeaserView(final Context context, final AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public ConversationPhotoTeaserView(
            final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        final Resources resources = context.getResources();

        synchronized (ConversationPhotoTeaserView.class) {
            if (sScrollSlop == 0) {
                sScrollSlop = resources.getInteger(R.integer.swipeScrollSlop);
                sShrinkAnimationDuration = resources.getInteger(
                        R.integer.shrink_animation_duration);
            }
        }

        mMailPrefs = MailPrefs.get(context);

        mNeedLayout = true;

        mTabletDevice = Utils.useTabletUI(resources);
        mListCollapsible = resources.getBoolean(R.bool.list_collapsible);
    }

    @Override
    protected void onFinishInflate() {
        mSwipeableContent = findViewById(R.id.swipeable_content);

        findViewById(R.id.dismiss_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mTeaserRightEdge = findViewById(R.id.teaser_right_edge);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        final TextView text = (TextView) findViewById(R.id.text);
        final ImageView arrow = (ImageView) findViewById(R.id.arrow);

        // We post to avoid calling layout within layout
        arrow.post(new Runnable() {
            @Override
            public void run() {

                // The text top is changed when we move the arrow, so we need to
                // do multiple passes
                int textTop = text.getTop();
                if (mNeedLayout || textTop != mTextTop) {
                    mNeedLayout = false;
                    mTextTop = textTop;

                    final int lineHeight = text.getLineHeight();
                    final LinearLayout.LayoutParams arrowParams = (LinearLayout.LayoutParams) arrow
                            .getLayoutParams();
                    arrowParams.topMargin = mTextTop + lineHeight / 2;
                    arrow.setLayoutParams(arrowParams);
                }
                arrow.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onUpdate(Folder folder, ConversationCursor cursor) {
        // Do nothing
    }

    @Override
    public void onGetView() {
        // Do nothing
    }

    @Override
    public boolean getShouldDisplayInList() {
        // show if 1) sender images are enabled 2) there are items
        mShown = shouldShowSenderImage() && !mAdapter.isEmpty()
                && !mMailPrefs.isConversationPhotoTeaserAlreadyShown();
        return mShown;
    }

    @Override
    public int getPosition() {
        return 0;
    }

    @Override
    public void setAdapter(AnimatedAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public void bindFragment(final LoaderManager loaderManager, final Bundle savedInstanceState) {
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void onConversationSelected() {
        // DO NOTHING
    }

    @Override
    public void onCabModeEntered() {
        if (mShown) {
            dismiss();
        }
    }

    @Override
    public void onCabModeExited() {
        // Do nothing
    }

    @Override
    public void onConversationListVisibilityChanged(final boolean visible) {
        // Do nothing
    }

    @Override
    public void saveInstanceState(final Bundle outState) {
        // Do nothing
    }

    @Override
    public boolean acceptsUserTaps() {
        // No, we don't allow user taps.
        return false;
    }

    @Override
    public void dismiss() {
        setDismissed();
        startDestroyAnimation();
    }

    private void setDismissed() {
        if (mShown) {
            mMailPrefs.setConversationPhotoTeaserAlreadyShown();
            mShown = false;
            Analytics.getInstance().sendEvent("list_swipe", "photo_teaser", null, 0);
        }
    }

    protected boolean shouldShowSenderImage() {
        return mMailPrefs.getShowSenderImages();
    }

    @Override
    public SwipeableView getSwipeableView() {
        return SwipeableView.from(mSwipeableContent);
    }

    @Override
    public boolean canChildBeDismissed() {
        return true;
    }

    @Override
    public float getMinAllowScrollDistance() {
        return sScrollSlop;
    }

    private void startDestroyAnimation() {
        final int start = getHeight();
        final int end = 0;
        mAnimatedHeight = start;
        final ObjectAnimator heightAnimator =
                ObjectAnimator.ofInt(this, "animatedHeight", start, end);
        heightAnimator.setInterpolator(new DecelerateInterpolator(2.0f));
        heightAnimator.setDuration(sShrinkAnimationDuration);
        heightAnimator.start();

        /*
         * Ideally, we would like to call mAdapter.notifyDataSetChanged() in a listener's
         * onAnimationEnd(), but we are in the middle of a touch event, and this will cause all the
         * views to get recycled, which will cause problems.
         *
         * Instead, we'll just leave the item in the list with a height of 0, and the next
         * notifyDatasetChanged() will remove it from the adapter.
         */
    }

    /**
     * This method is used by the animator.  It is explicitly kept in proguard.flags to prevent it
     * from being removed, inlined, or obfuscated.
     * Edit ./packages/apps/UnifiedEmail/proguard.flags
     * In the future, we want to use @Keep
     */
    public void setAnimatedHeight(final int height) {
        mAnimatedHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (Utils.getDisplayListRightEdgeEffect(mTabletDevice, mListCollapsible,
                mAdapter.getViewMode())) {
            mTeaserRightEdge.setVisibility(VISIBLE);
        } else {
            mTeaserRightEdge.setVisibility(GONE);
        }

        if (mAnimatedHeight == -1) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mAnimatedHeight);
        }
    }
}
