package com.android.mail.bitmap;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.android.bitmap.BitmapCache;
import com.android.bitmap.DecodeAggregator;
import com.android.mail.R;
import com.android.mail.browse.ConversationItemViewCoordinates;


/**
 * A 2x1 grid of attachment drawables. Supports showing a small "+N" badge in the corner.
 */
public class AttachmentGridDrawable extends CompositeDrawable<AttachmentDrawable>
        implements Parallaxable {

    public static final int MAX_VISIBLE_ATTACHMENT_COUNT = 2;

    private BitmapCache mCache;
    private DecodeAggregator mDecodeAggregator;
    private String mOverflowText;
    private ConversationItemViewCoordinates mCoordinates;
    private float mParallaxFraction = 0.5f;

    private final Resources mResources;
    private final Drawable mPlaceholder;
    private final Drawable mProgress;
    private final int mOverflowTextColor;
    private final int mOverflowBadgeColor;
    private final Paint mPaint = new Paint();
    private final Rect mRect = new Rect();

    public AttachmentGridDrawable(Resources res, Drawable placeholder, Drawable progress) {
        super(MAX_VISIBLE_ATTACHMENT_COUNT);
        mResources = res;
        mPlaceholder = placeholder;
        mProgress = progress;
        mOverflowTextColor = res.getColor(R.color.ap_overflow_text_color);
        mOverflowBadgeColor = res.getColor(R.color.ap_overflow_badge_color);

        mPaint.setAntiAlias(true);
    }

    @Override
    protected AttachmentDrawable createDivisionDrawable() {
        final AttachmentDrawable result = new AttachmentDrawable(mResources, mCache,
                mDecodeAggregator, mCoordinates, mPlaceholder, mProgress);
        return result;
    }

    public void setBitmapCache(BitmapCache cache) {
        mCache = cache;
    }

    public void setDecodeAggregator(final DecodeAggregator decodeAggregator) {
        this.mDecodeAggregator = decodeAggregator;
    }

    public void setOverflowText(String text) {
        mOverflowText = text;
        layoutOverflowBadge();
    }

    public void setCoordinates(ConversationItemViewCoordinates coordinates) {
        mCoordinates = coordinates;
        layoutOverflowBadge();
    }

    private void layoutOverflowBadge() {
        if (mCoordinates == null || mOverflowText == null) {
            return;
        }
        mPaint.setTextSize(mCoordinates.overflowFontSize);
        mPaint.setTypeface(mCoordinates.overflowTypeface);
        mPaint.setTextAlign(Align.CENTER);
        mPaint.getTextBounds(mOverflowText, 0, mOverflowText.length(), mRect);
    }

    @Override
    public void draw(Canvas canvas) {
        for (int i = 0; i < mCount; i++) {
            mDrawables.get(i).setParallaxFraction(mParallaxFraction);
        }

        super.draw(canvas);

        // Overflow badge and count
        if (mOverflowText != null && mCoordinates != null) {
            final float radius = mCoordinates.overflowDiameter / 2f;
            // transform item-level coordinates into local drawable coordinate space
            final float x = mCoordinates.overflowXEnd - mCoordinates.attachmentPreviewsX - radius;
            final float y = mCoordinates.overflowYEnd - mCoordinates.attachmentPreviewsY - radius;

            mPaint.setColor(mOverflowBadgeColor);
            canvas.drawCircle(x, y, radius, mPaint);

            mPaint.setColor(mOverflowTextColor);
            canvas.drawText(mOverflowText, x, y + (mRect.height() / 2f), mPaint);
        }
    }

    @Override
    public void setParallaxFraction(float fraction) {
        mParallaxFraction = fraction;
    }

}
