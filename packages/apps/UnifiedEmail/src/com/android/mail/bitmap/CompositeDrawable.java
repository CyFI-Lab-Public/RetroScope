package com.android.mail.bitmap;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.android.bitmap.Trace;

import java.util.ArrayList;
import java.util.List;

/**
 * A drawable that contains up to 4 other smaller drawables in a regular grid. This class attempts
 * to reuse inner drawables when feasible to promote object reuse. This design goal makes the API
 * to reuse an instance a little awkward: you must first zero out the count, then set it to a new
 * value, then populate the entries with {@link #getOrCreateDrawable(int)} and a drawable subclass
 * bind() method of your design.
 */
public abstract class CompositeDrawable<T extends Drawable> extends Drawable
        implements Drawable.Callback {

    protected final List<T> mDrawables;
    protected int mCount;

    public CompositeDrawable(int maxDivisions) {
        if (maxDivisions >= 4) {
            throw new IllegalArgumentException("CompositeDrawable only supports 4 divisions");
        }
        mDrawables = new ArrayList<T>(maxDivisions);
        for (int i = 0; i < maxDivisions; i++) {
            mDrawables.add(i, null);
        }
        mCount = 0;
    }

    protected abstract T createDivisionDrawable();

    public void setCount(int count) {
        // zero out the composite bounds, which will propagate to the division drawables
        // this invalidates any old division bounds, which may change with the count
        setBounds(0, 0, 0, 0);
        mCount = count;
    }

    public int getCount() {
        return mCount;
    }

    public T getOrCreateDrawable(int i) {
        if (i >= mCount) {
            throw new IllegalArgumentException("bad index: " + i);
        }

        T result = mDrawables.get(i);
        if (result == null) {
            Trace.beginSection("create division drawable");
            result = createDivisionDrawable();
            mDrawables.set(i, result);
            result.setCallback(this);
            // Make sure drawables created after the bounds were already set have their bounds
            // set initially (the other unaffected drawables basically de-bounce this).
            onBoundsChange(getBounds());
            Trace.endSection();
        }
        return result;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        final int w = bounds.width();
        final int h = bounds.height();
        final int mw = w / 2;
        final int mh = h / 2;
        switch (mCount) {
            case 1:
                // 1 bitmap: passthrough
                applyBounds(0, 0, 0, w, h);
                break;
            case 2:
                // 2 bitmaps split vertically
                applyBounds(0, 0, 0, mw, h);
                applyBounds(1, mw, 0, w, h);
                break;
            case 3:
                // 1st is tall on the left, 2nd/3rd stacked vertically on the right
                applyBounds(0, 0, 0, mw, h);
                applyBounds(1, mw, 0, w, mh);
                applyBounds(2, mw, mh, w, h);
                break;
            case 4:
                // 4 bitmaps in a 2x2 grid
                applyBounds(0, 0, 0, mw, mh);
                applyBounds(1, mw, 0, w, mh);
                applyBounds(2, 0, mh, mw, h);
                applyBounds(3, mw, mh, w, h);
                break;
        }
    }

    private void applyBounds(int index, int left, int top, int right, int bottom) {
        final T d = mDrawables.get(index);
        if (d == null) {
            return;
        }
        d.setBounds(left, top, right, bottom);
    }

    @Override
    public void draw(Canvas canvas) {
        for (int i = 0; i < mCount; i++) {
            mDrawables.get(i).draw(canvas);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        for (int i = 0; i < mCount; i++) {
            mDrawables.get(i).setAlpha(alpha);
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        for (int i = 0; i < mCount; i++) {
            mDrawables.get(i).setColorFilter(cf);
        }
    }

    @Override
    public int getOpacity() {
        int opacity = PixelFormat.OPAQUE;
        for (int i = 0; i < mCount; i++) {
            if (mDrawables.get(i).getOpacity() != PixelFormat.OPAQUE) {
                opacity = PixelFormat.TRANSLUCENT;
                break;
            }
        }
        return opacity;
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
