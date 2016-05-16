
package com.android.gallery3d.filtershow.imageshow;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.EditorDraw;
import com.android.gallery3d.filtershow.filters.FilterDrawRepresentation;
import com.android.gallery3d.filtershow.filters.ImageFilterDraw;

public class ImageDraw extends ImageShow {

    private static final String LOGTAG = "ImageDraw";
    private int mCurrentColor = Color.RED;
    final static float INITAL_STROKE_RADIUS = 40;
    private float mCurrentSize = INITAL_STROKE_RADIUS;
    private byte mType = 0;
    private FilterDrawRepresentation mFRep;
    private EditorDraw mEditorDraw;
    private long mTimeout;
    private Paint mCheckerdPaint = makeCheckedPaint();
    private Paint mShadowPaint = new Paint();
    private Paint mIconPaint = new Paint();
    private Paint mBorderPaint = new Paint();
    private Handler mHandler;
    private FilterDrawRepresentation.StrokeData mTmpStrokData =
            new FilterDrawRepresentation.StrokeData();
    private Bitmap mBitmap;
    private float mDisplayRound;
    private float mDisplayBorder;
    private int DISPLAY_TIME = 500;
    private Matrix mRotateToScreen = new Matrix();
    private Matrix mToOrig;
    private int mBorderColor;
    private int mBorderShadowSize;
    private NinePatchDrawable mShadow;

    Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
           invalidate();
        }
    };


    public ImageDraw(Context context, AttributeSet attrs) {
        super(context, attrs);
        resetParameter();
        setupConstants(context);
        setupTimer();
    }

    public ImageDraw(Context context) {
        super(context);
        resetParameter();
        setupConstants(context);
        setupTimer();
    }

    private void setupConstants(Context context){
        Resources res = context.getResources();
        mDisplayRound = res.getDimensionPixelSize(R.dimen.draw_rect_round);
        mDisplayBorder = res.getDimensionPixelSize(R.dimen.draw_rect_border);
        mBorderShadowSize = res.getDimensionPixelSize(R.dimen.draw_rect_shadow);
        float edge = res.getDimensionPixelSize(R.dimen.draw_rect_border_edge);

        mBorderColor = res.getColor(R.color.draw_rect_border);
        mBorderPaint.setColor(mBorderColor);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(edge);
        mShadowPaint.setStyle(Paint.Style.FILL);
        mShadowPaint.setColor(Color.BLACK);
        mShadowPaint.setShadowLayer(mBorderShadowSize,mBorderShadowSize,
                mBorderShadowSize,Color.BLACK);
        mShadow = (NinePatchDrawable) res.getDrawable(R.drawable.geometry_shadow);
    }

    public void setEditor(EditorDraw editorDraw) {
        mEditorDraw = editorDraw;
    }

    public void setFilterDrawRepresentation(FilterDrawRepresentation fr) {
        mFRep = fr;
        mTmpStrokData =
                new FilterDrawRepresentation.StrokeData();
    }

    public Drawable getIcon(Context context) {

        return null;
    }

    @Override
    public void resetParameter() {
        if (mFRep != null) {
            mFRep.clear();
        }
    }

    public void setColor(int color) {
        mCurrentColor = color;
    }

    public void setSize(int size) {
        mCurrentSize = size;
    }

    public void setStyle(byte style) {
        mType = (byte) (style % ImageFilterDraw.NUMBER_OF_STYLES);
    }

    public int getStyle() {
        return mType;
    }

    public int getSize() {
        return (int) mCurrentSize;
    }

    float[] mTmpPoint = new float[2]; // so we do not malloc
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1) {
            boolean ret = super.onTouchEvent(event);
            if (mFRep.getCurrentDrawing() != null) {
                mFRep.clearCurrentSection();
                mEditorDraw.commitLocalRepresentation();
            }
            return ret;
        }
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            if (mFRep.getCurrentDrawing() == null) {
                return super.onTouchEvent(event);
            }
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            calcScreenMapping();
            mTmpPoint[0] = event.getX();
            mTmpPoint[1] = event.getY();
            mToOrig.mapPoints(mTmpPoint);
            mFRep.startNewSection( mTmpPoint[0], mTmpPoint[1]);
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {

            int historySize = event.getHistorySize();
            for (int h = 0; h < historySize; h++) {
                int p = 0;
                {
                    mTmpPoint[0] = event.getHistoricalX(p, h);
                    mTmpPoint[1] = event.getHistoricalY(p, h);
                    mToOrig.mapPoints(mTmpPoint);
                    mFRep.addPoint(mTmpPoint[0], mTmpPoint[1]);
                }
            }
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            mTmpPoint[0] = event.getX();
            mTmpPoint[1] = event.getY();
            mToOrig.mapPoints(mTmpPoint);
            mFRep.endSection(mTmpPoint[0], mTmpPoint[1]);
        }
        mEditorDraw.commitLocalRepresentation();
        invalidate();
        return true;
    }

    private void calcScreenMapping() {
        mToOrig = getScreenToImageMatrix(true);
        mToOrig.invert(mRotateToScreen);
    }

    private static Paint makeCheckedPaint(){
        int[] colors = new int[16 * 16];
        for (int i = 0; i < colors.length; i++) {
            int y = i / (16 * 8);
            int x = (i / 8) % 2;
            colors[i] = (x == y) ? 0xFF777777 : 0xFF222222;
        }
        Bitmap bitmap = Bitmap.createBitmap(colors, 16, 16, Bitmap.Config.ARGB_8888);
        BitmapShader bs = new BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Paint p = new Paint();
        p.setShader(bs);
        return p;
    }

    private void setupTimer() {
        mHandler = new Handler(getActivity().getMainLooper());
    }

    private void scheduleWakeup(int delay) {
        mHandler.removeCallbacks(mUpdateRunnable);
        mHandler.postDelayed(mUpdateRunnable, delay);
    }

    public Bitmap getBrush(int brushid) {
        Bitmap bitmap;
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.ALPHA_8;
        bitmap = BitmapFactory.decodeResource(getActivity().getResources(), brushid, opt);
        bitmap = bitmap.extractAlpha();

        return bitmap;
    }

    public Bitmap createScaledBitmap(Bitmap src, int dstWidth, int dstHeight, boolean filter) {
        Matrix m = new Matrix();
        m.setScale(dstWidth / (float) src.getWidth(), dstHeight / (float) src.getHeight());
        Bitmap result = Bitmap.createBitmap(dstWidth, dstHeight, src.getConfig());
        Canvas canvas = new Canvas(result);

        Paint paint = new Paint();
        paint.setFilterBitmap(filter);
        canvas.drawBitmap(src, m, paint);

        return result;

    }

    public void displayDrawLook() {
        if (mFRep == null) {
            return;
        }
        int color = mTmpStrokData.mColor;
        byte type = mTmpStrokData.mType;
        float radius = mTmpStrokData.mRadius;
        mFRep.fillStrokeParameters(mTmpStrokData);

        if (radius != mTmpStrokData.mRadius) {
            mTimeout = DISPLAY_TIME + System.currentTimeMillis();
            scheduleWakeup(DISPLAY_TIME);
        }
    }

    public void drawLook(Canvas canvas) {
        if (mFRep == null) {
            return;
        }
        int cw = canvas.getWidth();
        int ch = canvas.getHeight();
        int centerx = cw / 2;
        int centery = ch / 2;

//        mFRep.fillStrokeParameters(mTmpStrokData);
        mIconPaint.setAntiAlias(true);
        mIconPaint.setStyle(Paint.Style.STROKE);
        float rad = mRotateToScreen.mapRadius(mTmpStrokData.mRadius);

        RectF rec = new RectF();
        rec.set(centerx - rad,
                centery - rad,
                centerx + rad,
                centery + rad);
        mIconPaint.setColor(Color.BLACK);
        mIconPaint.setStrokeWidth(5);
        canvas.drawArc(rec, 0, 360, true, mIconPaint);
        mIconPaint.setColor(Color.WHITE);
        mIconPaint.setStrokeWidth(3);
        canvas.drawArc(rec, 0, 360, true, mIconPaint);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        calcScreenMapping();
        if (System.currentTimeMillis() < mTimeout) {
            drawLook(canvas);
        }
    }

}
