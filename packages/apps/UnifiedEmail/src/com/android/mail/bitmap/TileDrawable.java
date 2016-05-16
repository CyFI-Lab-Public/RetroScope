package com.android.mail.bitmap;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * A drawable that wraps another drawable and places it in the center of this space. This drawable
 * allows a background color for the "tile", and has a fade-out transition when
 * {@link #setVisible(boolean, boolean)} indicates that it is no longer visible.
 */
public class TileDrawable extends Drawable implements Drawable.Callback {

    private final Paint mPaint = new Paint();
    private final Drawable mInner;
    private final int mInnerWidth;
    private final int mInnerHeight;

    protected final ValueAnimator mFadeOutAnimator;

    public TileDrawable(Drawable inner, int innerWidth, int innerHeight,
            int backgroundColor, int fadeOutDurationMs) {
        mInner = inner.mutate();
        mInnerWidth = innerWidth;
        mInnerHeight = innerHeight;
        mPaint.setColor(backgroundColor);
        mInner.setCallback(this);

        mFadeOutAnimator = ValueAnimator.ofInt(255, 0)
                .setDuration(fadeOutDurationMs);
        mFadeOutAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setAlpha((Integer) animation.getAnimatedValue());
            }
        });

        reset();
    }

    public void reset() {
        setAlpha(0);
        setVisible(false);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        if (bounds.isEmpty()) {
            mInner.setBounds(0, 0, 0, 0);
        } else {
            final int l = bounds.left + (bounds.width() / 2) - (mInnerWidth / 2);
            final int t = bounds.top + (bounds.height() / 2) - (mInnerHeight / 2);
            mInner.setBounds(l, t, l + mInnerWidth, t + mInnerHeight);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (!isVisible() && mPaint.getAlpha() == 0) {
            return;
        }
        canvas.drawRect(getBounds(), mPaint);
        mInner.draw(canvas);
    }

    @Override
    public void setAlpha(int alpha) {
        final int old = mPaint.getAlpha();
        mPaint.setAlpha(alpha);
        setInnerAlpha(alpha);
        if (alpha != old) {
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
        mInner.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    protected int getCurrentAlpha() {
        return mPaint.getAlpha();
    }

    public boolean setVisible(boolean visible) {
        return setVisible(visible, true /* dontcare */);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        mInner.setVisible(visible, restart);
        final boolean changed = super.setVisible(visible, restart);
        if (changed) {
            if (isVisible()) {
                // pop in (no-op)
                // the transition will still be smooth if the previous state's layer fades out
                mFadeOutAnimator.cancel();
                setAlpha(255);
            } else {
                // fade out
                if (mPaint.getAlpha() == 255 && !getBounds().isEmpty()) {
                    mFadeOutAnimator.start();
                }
            }
        }
        return changed;
    }

    @Override
    protected boolean onLevelChange(int level) {
        return mInner.setLevel(level);
    }

    /**
     * Changes the alpha on just the inner wrapped drawable.
     */
    public void setInnerAlpha(int alpha) {
        mInner.setAlpha(alpha);
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        invalidateSelf();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }

}
