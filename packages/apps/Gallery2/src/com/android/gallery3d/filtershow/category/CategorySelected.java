package com.android.gallery3d.filtershow.category;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.android.gallery3d.R;

public class CategorySelected extends View {
    private Paint mPaint = new Paint();
    private int mMargin = 20;

    public CategorySelected(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMargin = getResources().getDimensionPixelSize(R.dimen.touch_circle_size);
    }

    public void onDraw(Canvas canvas) {
        mPaint.reset();
        mPaint.setStrokeWidth(mMargin);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.argb(128, 128, 128, 128));
        canvas.drawCircle(getWidth()/2, getHeight()/2,
                getWidth()/2 - mMargin, mPaint);
    }

}
