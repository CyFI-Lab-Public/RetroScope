package com.android.mail.bitmap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.animation.LinearInterpolator;

import com.android.bitmap.BitmapCache;
import com.android.bitmap.BitmapUtils;
import com.android.bitmap.DecodeAggregator;
import com.android.bitmap.DecodeTask;
import com.android.bitmap.DecodeTask.Request;
import com.android.bitmap.ReusableBitmap;
import com.android.bitmap.Trace;
import com.android.mail.R;
import com.android.mail.browse.ConversationItemViewCoordinates;
import com.android.mail.ui.SwipeableListView;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.RectUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class encapsulates all functionality needed to display a single image attachment thumbnail,
 * including request creation/cancelling, data unbinding and re-binding, and fancy animations
 * to draw upon state changes.
 * <p>
 * The actual bitmap decode work is handled by {@link DecodeTask}.
 */
public class AttachmentDrawable extends Drawable implements DecodeTask.BitmapView,
        Drawable.Callback, Runnable, Parallaxable, DecodeAggregator.Callback {

    private ImageAttachmentRequest mCurrKey;
    private ReusableBitmap mBitmap;
    private final BitmapCache mCache;
    private final DecodeAggregator mDecodeAggregator;
    private DecodeTask mTask;
    private int mDecodeWidth;
    private int mDecodeHeight;
    private int mLoadState = LOAD_STATE_UNINITIALIZED;
    private float mParallaxFraction = 0.5f;
    private float mParallaxSpeedMultiplier;

    // each attachment gets its own placeholder and progress indicator, to be shown, hidden,
    // and animated based on Drawable#setVisible() changes, which are in turn driven by
    // #setLoadState().
    private Placeholder mPlaceholder;
    private Progress mProgress;

    private static final Executor SMALL_POOL_EXECUTOR = new ThreadPoolExecutor(4, 4,
            1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    private static final Executor EXECUTOR = SMALL_POOL_EXECUTOR;

    private static final boolean LIMIT_BITMAP_DENSITY = true;

    private static final int MAX_BITMAP_DENSITY = DisplayMetrics.DENSITY_HIGH;

    private static final int LOAD_STATE_UNINITIALIZED = 0;
    private static final int LOAD_STATE_NOT_YET_LOADED = 1;
    private static final int LOAD_STATE_LOADING = 2;
    private static final int LOAD_STATE_LOADED = 3;
    private static final int LOAD_STATE_FAILED = 4;

    private final ConversationItemViewCoordinates mCoordinates;
    private final float mDensity;
    private final int mProgressDelayMs;
    private final Paint mPaint = new Paint();
    private final Rect mSrcRect = new Rect();
    private final Handler mHandler = new Handler();

    public final String LOG_TAG = "AttachPreview";

    public AttachmentDrawable(final Resources res, final BitmapCache cache,
            final DecodeAggregator decodeAggregator,
            final ConversationItemViewCoordinates coordinates, final Drawable placeholder,
            final Drawable progress) {
        mCoordinates = coordinates;
        mDensity = res.getDisplayMetrics().density;
        mCache = cache;
        this.mDecodeAggregator = decodeAggregator;
        mPaint.setFilterBitmap(true);

        final int fadeOutDurationMs = res.getInteger(R.integer.ap_fade_animation_duration);
        final int tileColor = res.getColor(R.color.ap_background_color);
        mProgressDelayMs = res.getInteger(R.integer.ap_progress_animation_delay);

        mPlaceholder = new Placeholder(placeholder.getConstantState().newDrawable(res), res,
                coordinates, fadeOutDurationMs, tileColor);
        mPlaceholder.setCallback(this);

        mProgress = new Progress(progress.getConstantState().newDrawable(res), res,
                coordinates, fadeOutDurationMs, tileColor);
        mProgress.setCallback(this);
    }

    public DecodeTask.Request getKey() {
        return mCurrKey;
    }

    public void setDecodeDimensions(int w, int h) {
        mDecodeWidth = w;
        mDecodeHeight = h;
    }

    public void setParallaxSpeedMultiplier(final float parallaxSpeedMultiplier) {
        mParallaxSpeedMultiplier = parallaxSpeedMultiplier;
    }

    public void showStaticPlaceholder() {
        setLoadState(LOAD_STATE_FAILED);
    }

    public void unbind() {
        setImage(null);
    }

    public void bind(Context context, String lookupUri, int rendition) {
        final Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            throw new IllegalStateException("AttachmentDrawable must have bounds set before bind");
        }
        setImage(new ImageAttachmentRequest(context, lookupUri, rendition, bounds.width()));
    }

    private void setImage(final ImageAttachmentRequest key) {
        if (mCurrKey != null && mCurrKey.equals(key)) {
            return;
        }

        Trace.beginSection("set image");
        // avoid visual state transitions when the existing request and the new one are just
        // requests for different renditions of the same attachment
        final boolean onlyRenditionChange = (mCurrKey != null && mCurrKey.matches(key));

        if (mBitmap != null && !onlyRenditionChange) {
            mBitmap.releaseReference();
//            System.out.println("view.bind() decremented ref to old bitmap: " + mBitmap);
            mBitmap = null;
        }
        if (mCurrKey != null && SwipeableListView.ENABLE_ATTACHMENT_DECODE_AGGREGATOR) {
            mDecodeAggregator.forget(mCurrKey);
        }
        mCurrKey = key;

        if (mTask != null) {
            mTask.cancel();
            mTask = null;
        }

        mHandler.removeCallbacks(this);
        // start from a clean slate on every bind
        // this allows the initial transition to be specially instantaneous, so e.g. a cache hit
        // doesn't unnecessarily trigger a fade-in
        setLoadState(LOAD_STATE_UNINITIALIZED);

        if (key == null) {
            Trace.endSection();
            return;
        }

        // find cached entry here and skip decode if found.
        final ReusableBitmap cached = mCache.get(key, true /* incrementRefCount */);
        if (cached != null) {
            setBitmap(cached);
            LogUtils.d(LOG_TAG, "CACHE HIT key=%s", mCurrKey);
        } else {
            decode(!onlyRenditionChange);
            if (LogUtils.isLoggable(LOG_TAG, LogUtils.DEBUG)) {
                LogUtils.d(LOG_TAG, "CACHE MISS key=%s\ncache=%s",
                        mCurrKey, mCache.toDebugString());
            }
        }
        Trace.endSection();
    }

    @Override
    public void setParallaxFraction(float fraction) {
        mParallaxFraction = fraction;
    }

    @Override
    public void draw(final Canvas canvas) {
        final Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }

        if (mBitmap != null) {
            BitmapUtils
                    .calculateCroppedSrcRect(mBitmap.getLogicalWidth(), mBitmap.getLogicalHeight(),
                            bounds.width(), bounds.height(),
                            mCoordinates.attachmentPreviewsDecodeHeight, Integer.MAX_VALUE,
                            mParallaxFraction, false /* absoluteFraction */,
                            mParallaxSpeedMultiplier, mSrcRect);

            final int orientation = mBitmap.getOrientation();
            // calculateCroppedSrcRect() gave us the source rectangle "as if" the orientation has
            // been corrected. We need to decode the uncorrected source rectangle. Calculate true
            // coordinates.
            RectUtils.rotateRectForOrientation(orientation,
                    new Rect(0, 0, mBitmap.getLogicalWidth(), mBitmap.getLogicalHeight()),
                    mSrcRect);

            // We may need to rotate the canvas, so we also have to rotate the bounds.
            final Rect rotatedBounds = new Rect(bounds);
            RectUtils.rotateRect(orientation, bounds.centerX(), bounds.centerY(), rotatedBounds);

            // Rotate the canvas.
            canvas.save();
            canvas.rotate(orientation, bounds.centerX(), bounds.centerY());
            canvas.drawBitmap(mBitmap.bmp, mSrcRect, rotatedBounds, mPaint);
            canvas.restore();
        }

        // Draw the two possible overlay layers in reverse-priority order.
        // (each layer will no-op the draw when appropriate)
        // This ordering means cross-fade transitions are just fade-outs of each layer.
        mProgress.draw(canvas);
        mPlaceholder.draw(canvas);
    }

    @Override
    public void setAlpha(int alpha) {
        final int old = mPaint.getAlpha();
        mPaint.setAlpha(alpha);
        mPlaceholder.setAlpha(alpha);
        mProgress.setAlpha(alpha);
        if (alpha != old) {
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
        mPlaceholder.setColorFilter(cf);
        mProgress.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return (mBitmap != null && (mBitmap.bmp.hasAlpha() || mPaint.getAlpha() < 255)) ?
                PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        mPlaceholder.setBounds(bounds);
        mProgress.setBounds(bounds);
    }

    @Override
    public void onDecodeBegin(final Request key) {
        if (SwipeableListView.ENABLE_ATTACHMENT_DECODE_AGGREGATOR) {
            mDecodeAggregator.expect(key, this);
        } else {
            onBecomeFirstExpected(key);
        }
    }

    @Override
    public void onBecomeFirstExpected(final Request key) {
        if (!key.equals(mCurrKey)) {
            return;
        }
        // normally, we'd transition to the LOADING state now, but we want to delay that a bit
        // to minimize excess occurrences of the rotating spinner
        mHandler.postDelayed(this, mProgressDelayMs);
    }

    @Override
    public void run() {
        if (mLoadState == LOAD_STATE_NOT_YET_LOADED) {
            setLoadState(LOAD_STATE_LOADING);
        }
    }

    @Override
    public void onDecodeComplete(final Request key, final ReusableBitmap result) {
        if (SwipeableListView.ENABLE_ATTACHMENT_DECODE_AGGREGATOR) {
            mDecodeAggregator.execute(key, new Runnable() {
                @Override
                public void run() {
                    onDecodeCompleteImpl(key, result);
                }

                @Override
                public String toString() {
                    return "DONE";
                }
            });
        } else {
            onDecodeCompleteImpl(key, result);
        }
    }

    private void onDecodeCompleteImpl(final Request key, final ReusableBitmap result) {
        if (key.equals(mCurrKey)) {
            setBitmap(result);
        } else {
            // if the requests don't match (i.e. this request is stale), decrement the
            // ref count to allow the bitmap to be pooled
            if (result != null) {
                result.releaseReference();
            }
        }
    }

    @Override
    public void onDecodeCancel(final Request key) {
        if (SwipeableListView.ENABLE_ATTACHMENT_DECODE_AGGREGATOR) {
            mDecodeAggregator.forget(key);
        }
    }

    private void setBitmap(ReusableBitmap bmp) {
        if (mBitmap != null && mBitmap != bmp) {
            mBitmap.releaseReference();
        }
        mBitmap = bmp;
        setLoadState((bmp != null) ? LOAD_STATE_LOADED : LOAD_STATE_FAILED);
        invalidateSelf();
    }

    private void decode(boolean executeStateChange) {
        final int w;
        final int bufferW;
        final int bufferH;

        if (mCurrKey == null) {
            return;
        }

        Trace.beginSection("decode");
        if (LIMIT_BITMAP_DENSITY) {
            final float scale =
                    Math.min(1f, (float) MAX_BITMAP_DENSITY / DisplayMetrics.DENSITY_DEFAULT
                            / mDensity);
            w = (int) (mCurrKey.mDestW * scale);
            bufferW = (int) (mDecodeWidth * scale);
            bufferH = (int) (mDecodeHeight * scale);
        } else {
            w = mCurrKey.mDestW;
            bufferW = mDecodeWidth;
            bufferH = mDecodeHeight;
        }

        if (w == 0 || bufferH == 0) {
            Trace.endSection();
            return;
        }
//        System.out.println("ITEM " + this + " w=" + w + " h=" + bufferH + " key=" + mCurrKey);
        if (mTask != null) {
            mTask.cancel();
        }
        if (executeStateChange) {
            setLoadState(LOAD_STATE_NOT_YET_LOADED);
        }
        mTask = new DecodeTask(mCurrKey, w, bufferH, bufferW, bufferH, this, mCache);
        mTask.executeOnExecutor(EXECUTOR);
        Trace.endSection();
    }

    private void setLoadState(int loadState) {
        LogUtils.v(LOG_TAG, "IN AD.setState. old=%s new=%s key=%s this=%s", mLoadState, loadState,
                mCurrKey, this);
        if (mLoadState == loadState) {
            LogUtils.v(LOG_TAG, "OUT no-op AD.setState");
            return;
        }

        Trace.beginSection("set load state");
        switch (loadState) {
            // This state differs from LOADED in that the subsequent state transition away from
            // UNINITIALIZED will not have a fancy transition. This allows list item binds to
            // cached data to take immediate effect without unnecessary whizzery.
            case LOAD_STATE_UNINITIALIZED:
                mPlaceholder.reset();
                mProgress.reset();
                break;
            case LOAD_STATE_NOT_YET_LOADED:
                mPlaceholder.setPulseEnabled(true);
                mPlaceholder.setVisible(true);
                mProgress.setVisible(false);
                break;
            case LOAD_STATE_LOADING:
                mPlaceholder.setVisible(false);
                mProgress.setVisible(true);
                break;
            case LOAD_STATE_LOADED:
                mPlaceholder.setVisible(false);
                mProgress.setVisible(false);
                break;
            case LOAD_STATE_FAILED:
                mPlaceholder.setPulseEnabled(false);
                mPlaceholder.setVisible(true);
                mProgress.setVisible(false);
                break;
        }
        Trace.endSection();

        mLoadState = loadState;
        LogUtils.v(LOG_TAG, "OUT stateful AD.setState. new=%s placeholder=%s progress=%s",
                loadState, mPlaceholder.isVisible(), mProgress.isVisible());
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

    private static class Placeholder extends TileDrawable {

        private final ValueAnimator mPulseAnimator;
        private boolean mPulseEnabled = true;
        private float mPulseAlphaFraction = 1f;

        public Placeholder(Drawable placeholder, Resources res,
                ConversationItemViewCoordinates coordinates, int fadeOutDurationMs,
                int tileColor) {
            super(placeholder, coordinates.placeholderWidth, coordinates.placeholderHeight,
                    tileColor, fadeOutDurationMs);
            mPulseAnimator = ValueAnimator.ofInt(55, 255)
                    .setDuration(res.getInteger(R.integer.ap_placeholder_animation_duration));
            mPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
            mPulseAnimator.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mPulseAlphaFraction = ((Integer) animation.getAnimatedValue()) / 255f;
                    setInnerAlpha(getCurrentAlpha());
                }
            });
            mFadeOutAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    stopPulsing();
                }
            });
        }

        @Override
        public void setInnerAlpha(final int alpha) {
            super.setInnerAlpha((int) (alpha * mPulseAlphaFraction));
        }

        public void setPulseEnabled(boolean enabled) {
            mPulseEnabled = enabled;
            if (!mPulseEnabled) {
                stopPulsing();
            }
        }

        private void stopPulsing() {
            if (mPulseAnimator != null) {
                mPulseAnimator.cancel();
                mPulseAlphaFraction = 1f;
                setInnerAlpha(getCurrentAlpha());
            }
        }

        @Override
        public boolean setVisible(boolean visible) {
            final boolean changed = super.setVisible(visible);
            if (changed) {
                if (isVisible()) {
                    // start
                    if (mPulseAnimator != null && mPulseEnabled) {
                        mPulseAnimator.start();
                    }
                } else {
                    // can't cancel the pulsing yet-- wait for the fade-out animation to end
                    // one exception: if alpha is already zero, there is no fade-out, so stop now
                    if (getCurrentAlpha() == 0) {
                        stopPulsing();
                    }
                }
            }
            return changed;
        }

    }

    private static class Progress extends TileDrawable {

        private final ValueAnimator mRotateAnimator;

        public Progress(Drawable progress, Resources res,
                ConversationItemViewCoordinates coordinates, int fadeOutDurationMs,
                int tileColor) {
            super(progress, coordinates.progressBarWidth, coordinates.progressBarHeight,
                    tileColor, fadeOutDurationMs);

            mRotateAnimator = ValueAnimator.ofInt(0, 10000)
                    .setDuration(res.getInteger(R.integer.ap_progress_animation_duration));
            mRotateAnimator.setInterpolator(new LinearInterpolator());
            mRotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mRotateAnimator.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setLevel((Integer) animation.getAnimatedValue());
                }
            });
            mFadeOutAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mRotateAnimator != null) {
                        mRotateAnimator.cancel();
                    }
                }
            });
        }

        @Override
        public boolean setVisible(boolean visible) {
            final boolean changed = super.setVisible(visible);
            if (changed) {
                if (isVisible()) {
                    if (mRotateAnimator != null) {
                        mRotateAnimator.start();
                    }
                } else {
                    // can't cancel the rotate yet-- wait for the fade-out animation to end
                    // one exception: if alpha is already zero, there is no fade-out, so stop now
                    if (getCurrentAlpha() == 0 && mRotateAnimator != null) {
                        mRotateAnimator.cancel();
                    }
                }
            }
            return changed;
        }

    }
}
