package com.android.dialer.list;

import android.animation.Animator;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.dialer.DialtactsActivity;
import com.android.dialer.R;

/**
 * A teaser to introduce people to the contact photo check boxes
 */
public class TileInteractionTeaserView extends FrameLayout {
    private static int sShrinkAnimationDuration;

    private static final String KEY_TILE_INTERACTION_TEASER_SHOWN =
            "key_tile_interaction_teaser_shown";

    private boolean mNeedLayout;
    private int mTextTop;
    private int mAnimatedHeight = -1;

    private PhoneFavoriteMergedAdapter mAdapter;

    public TileInteractionTeaserView(final Context context) {
        this(context, null);
    }

    public TileInteractionTeaserView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        final Resources resources = context.getResources();

        mNeedLayout = true;
        sShrinkAnimationDuration = resources.getInteger(R.integer.escape_animation_duration);
    }

    @Override
    protected void onFinishInflate() {
        findViewById(R.id.dismiss_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startDestroyAnimation();
            }
        });
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

    public boolean getShouldDisplayInList() {
        final SharedPreferences prefs = getContext().getSharedPreferences(
                DialtactsActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_TILE_INTERACTION_TEASER_SHOWN, true);
    }

    public void setAdapter(PhoneFavoriteMergedAdapter adapter) {
        mAdapter = adapter;
    }

    private void startDestroyAnimation() {
        final int start = getHeight();
        final int end = 0;
        mAnimatedHeight = start;
        Log.v("Interaction", "Start from" + start);

        ValueAnimator heightAnimator = ValueAnimator.ofInt(start, end);
        heightAnimator.setDuration(sShrinkAnimationDuration);
        heightAnimator.setInterpolator(new DecelerateInterpolator(2.0f));
        heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                mAnimatedHeight = (Integer) animation.getAnimatedValue();
                requestLayout();
            }
        });
        heightAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                setVisibility(GONE);
                setDismissed();
                if (mAdapter != null) {
                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });

        heightAnimator.start();
    }

    private void setDismissed() {
        final SharedPreferences prefs = getContext().getSharedPreferences(
                DialtactsActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_TILE_INTERACTION_TEASER_SHOWN, false).apply();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (mAnimatedHeight == -1) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mAnimatedHeight);
        }
    }
}
