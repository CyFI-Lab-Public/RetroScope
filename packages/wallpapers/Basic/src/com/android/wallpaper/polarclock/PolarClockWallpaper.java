/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.wallpaper.polarclock;

import android.service.wallpaper.WallpaperService;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.RectF;
import android.view.SurfaceHolder;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;

import android.os.Handler;
import android.os.SystemClock;
import android.text.format.Time;
import android.util.MathUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.TimeZone;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;
import static org.xmlpull.v1.XmlPullParser.*;

import com.android.wallpaper.R;

public class PolarClockWallpaper extends WallpaperService {
    private static final String LOG_TAG = "PolarClock";
    
    static final String SHARED_PREFS_NAME = "polar_clock_settings";

    static final String PREF_SHOW_SECONDS = "show_seconds";
    static final String PREF_VARIABLE_LINE_WIDTH = "variable_line_width";
    static final String PREF_PALETTE = "palette";

    static final int BACKGROUND_COLOR = 0xffffffff;

    static abstract class ClockPalette {
        public static ClockPalette parseXmlPaletteTag(XmlResourceParser xrp) {
            String kind = xrp.getAttributeValue(null, "kind");
            if ("cycling".equals(kind)) {
                return CyclingClockPalette.parseXmlPaletteTag(xrp);
            } else {
                return FixedClockPalette.parseXmlPaletteTag(xrp);
            }
        }

        public abstract int getBackgroundColor();

        // forAngle should be on [0.0,1.0) but 1.0 must be tolerated
        public abstract int getSecondColor(float forAngle);

        public abstract int getMinuteColor(float forAngle);

        public abstract int getHourColor(float forAngle);

        public abstract int getDayColor(float forAngle);

        public abstract int getMonthColor(float forAngle);

        public abstract String getId();

    }

    static class FixedClockPalette extends ClockPalette {
        protected String mId;
        protected int mBackgroundColor;
        protected int mSecondColor;
        protected int mMinuteColor;
        protected int mHourColor;
        protected int mDayColor;
        protected int mMonthColor;

        private static FixedClockPalette sFallbackPalette = null;

        public static FixedClockPalette getFallback() {
            if (sFallbackPalette == null) {
                sFallbackPalette = new FixedClockPalette();
                sFallbackPalette.mId = "default";
                sFallbackPalette.mBackgroundColor = Color.WHITE;
                sFallbackPalette.mSecondColor =
                    sFallbackPalette.mMinuteColor =
                    sFallbackPalette.mHourColor =
                    sFallbackPalette.mDayColor =
                    sFallbackPalette.mMonthColor =
                    Color.BLACK;
            }
            return sFallbackPalette;
        }

        private FixedClockPalette() { }

        public static ClockPalette parseXmlPaletteTag(XmlResourceParser xrp) {
            final FixedClockPalette pal = new FixedClockPalette();
            pal.mId = xrp.getAttributeValue(null, "id");
            String val;
            if ((val = xrp.getAttributeValue(null, "background")) != null)
                pal.mBackgroundColor = Color.parseColor(val);
            if ((val = xrp.getAttributeValue(null, "second")) != null)
                pal.mSecondColor = Color.parseColor(val);
            if ((val = xrp.getAttributeValue(null, "minute")) != null)
                pal.mMinuteColor = Color.parseColor(val);
            if ((val = xrp.getAttributeValue(null, "hour")) != null)
                pal.mHourColor = Color.parseColor(val);
            if ((val = xrp.getAttributeValue(null, "day")) != null)
                pal.mDayColor = Color.parseColor(val);
            if ((val = xrp.getAttributeValue(null, "month")) != null)
                pal.mMonthColor = Color.parseColor(val);
            return (pal.mId == null) ? null : pal;
        }

        @Override
        public int getBackgroundColor() {
            return mBackgroundColor;
        }

        @Override
        public int getSecondColor(float forAngle) {
            return mSecondColor;
        }

        @Override
        public int getMinuteColor(float forAngle) {
            return mMinuteColor;
        }

        @Override
        public int getHourColor(float forAngle) {
            return mHourColor;
        }

        @Override
        public int getDayColor(float forAngle) {
            return mDayColor;
        }

        @Override
        public int getMonthColor(float forAngle) {
            return mMonthColor;
        }

        @Override
        public String getId() {
            return mId;
        }

    }

    static class CyclingClockPalette extends ClockPalette {
        protected String mId;
        protected int mBackgroundColor;
        protected float mSaturation;
        protected float mBrightness;

        private static final int COLORS_CACHE_COUNT = 720;
        private final int[] mColors = new int[COLORS_CACHE_COUNT];

        private static CyclingClockPalette sFallbackPalette = null;

        public static CyclingClockPalette getFallback() {
            if (sFallbackPalette == null) {
                sFallbackPalette = new CyclingClockPalette();
                sFallbackPalette.mId = "default_c";
                sFallbackPalette.mBackgroundColor = Color.WHITE;
                sFallbackPalette.mSaturation = 0.8f;
                sFallbackPalette.mBrightness = 0.9f;
                sFallbackPalette.computeIntermediateColors();
            }
            return sFallbackPalette;
        }

        private CyclingClockPalette() { }

        private void computeIntermediateColors() {
            final int[] colors = mColors;
            final int count = colors.length;
            float invCount = 1.0f / (float) COLORS_CACHE_COUNT;
            for (int i = 0; i < count; i++) {
                colors[i] = Color.HSBtoColor(i * invCount, mSaturation, mBrightness);
            }
        }

        public static ClockPalette parseXmlPaletteTag(XmlResourceParser xrp) {
            final CyclingClockPalette pal = new CyclingClockPalette();
            pal.mId = xrp.getAttributeValue(null, "id");
            String val;
            if ((val = xrp.getAttributeValue(null, "background")) != null)
                pal.mBackgroundColor = Color.parseColor(val);
            if ((val = xrp.getAttributeValue(null, "saturation")) != null)
                pal.mSaturation = Float.parseFloat(val);
            if ((val = xrp.getAttributeValue(null, "brightness")) != null)
                pal.mBrightness = Float.parseFloat(val);
            if (pal.mId == null) {
                return null;
            } else {
                pal.computeIntermediateColors();
                return pal;
            }
        }
        @Override
        public int getBackgroundColor() {
            return mBackgroundColor;
        }

        @Override
        public int getSecondColor(float forAngle) {
            if (forAngle >= 1.0f || forAngle < 0.0f) forAngle = 0.0f;
            return mColors[((int) (forAngle * COLORS_CACHE_COUNT))];
        }

        @Override
        public int getMinuteColor(float forAngle) {
            if (forAngle >= 1.0f || forAngle < 0.0f) forAngle = 0.0f;
            return mColors[((int) (forAngle * COLORS_CACHE_COUNT))];
        }

        @Override
        public int getHourColor(float forAngle) {
            if (forAngle >= 1.0f || forAngle < 0.0f) forAngle = 0.0f;
            return mColors[((int) (forAngle * COLORS_CACHE_COUNT))];
        }

        @Override
        public int getDayColor(float forAngle) {
            if (forAngle >= 1.0f || forAngle < 0.0f) forAngle = 0.0f;
            return mColors[((int) (forAngle * COLORS_CACHE_COUNT))];
        }

        @Override
        public int getMonthColor(float forAngle) {
            if (forAngle >= 1.0f || forAngle < 0.0f) forAngle = 0.0f;
            return mColors[((int) (forAngle * COLORS_CACHE_COUNT))];
        }

        @Override
        public String getId() {
            return mId;
        }
    }

    private final Handler mHandler = new Handler();

    private IntentFilter mFilter;

    @Override
    public void onCreate() {
        super.onCreate();

        mFilter = new IntentFilter();
        mFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public Engine onCreateEngine() {
        return new ClockEngine();
    }

    class ClockEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
        private static final float SMALL_RING_THICKNESS = 8.0f;
        private static final float MEDIUM_RING_THICKNESS = 16.0f;
        private static final float LARGE_RING_THICKNESS = 32.0f;

        private static final float DEFAULT_RING_THICKNESS = 24.0f;

        private static final float SMALL_GAP = 14.0f;
        private static final float LARGE_GAP = 38.0f;

        private final HashMap<String, ClockPalette> mPalettes = new HashMap<String, ClockPalette>();
        private ClockPalette mPalette;

        private SharedPreferences mPrefs;
        private boolean mShowSeconds;
        private boolean mVariableLineWidth;

        private boolean mWatcherRegistered;
        private Time mCalendar;

        private final Paint mPaint = new Paint();
        private final RectF mRect = new RectF();

        private float mOffsetX;

        private final BroadcastReceiver mWatcher = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                final String timeZone = intent.getStringExtra("time-zone");
                mCalendar = new Time(TimeZone.getTimeZone(timeZone).getID());
                drawFrame();
            }
        };

        private final Runnable mDrawClock = new Runnable() {
            public void run() {
                drawFrame();
            }
        };
        private boolean mVisible;

        ClockEngine() {
            XmlResourceParser xrp = getResources().getXml(R.xml.polar_clock_palettes);
            try {
                int what = xrp.getEventType();
                while (what != END_DOCUMENT) {
                    if (what == START_TAG) {
                        if ("palette".equals(xrp.getName())) {
                            ClockPalette pal = ClockPalette.parseXmlPaletteTag(xrp);
                            if (pal.getId() != null) {
                                mPalettes.put(pal.getId(), pal);
                            }
                        }
                    }
                    what = xrp.next();
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "An error occured during wallpaper configuration:", e);
            } catch (XmlPullParserException e) {
                Log.e(LOG_TAG, "An error occured during wallpaper configuration:", e);
            } finally {
                xrp.close();
            }

            mPalette = CyclingClockPalette.getFallback();
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            mPrefs = PolarClockWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
            mPrefs.registerOnSharedPreferenceChangeListener(this);

            // load up user's settings
            onSharedPreferenceChanged(mPrefs, null);

            mCalendar = new Time();
            mCalendar.setToNow();

            final Paint paint = mPaint;
            paint.setAntiAlias(true);
            paint.setStrokeWidth(DEFAULT_RING_THICKNESS);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);

            if (isPreview()) {
                mOffsetX = 0.5f;            
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (mWatcherRegistered) {
                mWatcherRegistered = false;
                unregisterReceiver(mWatcher);
            }
            mHandler.removeCallbacks(mDrawClock);
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                String key) {

            boolean changed = false;
            if (key == null || PREF_SHOW_SECONDS.equals(key)) {
                mShowSeconds = sharedPreferences.getBoolean(
                    PREF_SHOW_SECONDS, true);
                changed = true;
            }
            if (key == null || PREF_VARIABLE_LINE_WIDTH.equals(key)) {
                mVariableLineWidth = sharedPreferences.getBoolean(
                    PREF_VARIABLE_LINE_WIDTH, true);
                changed = true;
            }
            if (key == null || PREF_PALETTE.equals(key)) {
                String paletteId = sharedPreferences.getString(
                    PREF_PALETTE, "");
                ClockPalette pal = mPalettes.get(paletteId);
                if (pal != null) {
                    mPalette = pal;
                    changed = true;
                }
            }

            if (mVisible && changed) {
                drawFrame();
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                if (!mWatcherRegistered) {
                    mWatcherRegistered = true;
                    registerReceiver(mWatcher, mFilter, null, mHandler);
                }
                mCalendar = new Time();
                mCalendar.setToNow();
            } else {
                if (mWatcherRegistered) {
                    mWatcherRegistered = false;
                    unregisterReceiver(mWatcher);
                }
                mHandler.removeCallbacks(mDrawClock);
            }
            drawFrame();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mDrawClock);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xStep, float yStep, int xPixels, int yPixels) {
            if (isPreview()) return;

            mOffsetX = xOffset;
            drawFrame();
        }

        void drawFrame() {
            if (mPalette == null) {
                Log.w("PolarClockWallpaper", "no palette?!");
                return;
            }

            final SurfaceHolder holder = getSurfaceHolder();
            final Rect frame = holder.getSurfaceFrame();
            final int width = frame.width();
            final int height = frame.height();

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    final Time calendar = mCalendar;
                    final Paint paint = mPaint;

                    final long millis = System.currentTimeMillis();
                    calendar.set(millis);
                    calendar.normalize(false);

                    int s = width / 2;
                    int t = height / 2;

                    c.drawColor(mPalette.getBackgroundColor());

                    c.translate(s + MathUtils.lerp(s, -s, mOffsetX), t);
                    c.rotate(-90.0f);
                    if (height < width) {
                        c.scale(0.9f, 0.9f);
                    }

                    float size = Math.min(width, height) * 0.5f - DEFAULT_RING_THICKNESS;
                    final RectF rect = mRect;
                    rect.set(-size, -size, size, size);
                    float angle;

                    float lastRingThickness = DEFAULT_RING_THICKNESS;

                    if (mShowSeconds) {
                        // Draw seconds  
                        angle = (float) (millis % 60000) / 60000.0f;
                        //Log.d("PolarClock", "millis=" + millis + ", angle=" + angle);
                        paint.setColor(mPalette.getSecondColor(angle));

                        if (mVariableLineWidth) {
                            lastRingThickness = SMALL_RING_THICKNESS;
                            paint.setStrokeWidth(lastRingThickness);
                        }
                        c.drawArc(rect, 0.0f, angle * 360.0f, false, paint);
                    }

                    // Draw minutes
                    size -= (SMALL_GAP + lastRingThickness);
                    rect.set(-size, -size, size, size);

                    angle = ((calendar.minute * 60.0f + calendar.second) % 3600) / 3600.0f;
                    paint.setColor(mPalette.getMinuteColor(angle));

                    if (mVariableLineWidth) {
                        lastRingThickness = MEDIUM_RING_THICKNESS;
                        paint.setStrokeWidth(lastRingThickness);
                    }
                    c.drawArc(rect, 0.0f, angle * 360.0f, false, paint);

                    // Draw hours
                    size -= (SMALL_GAP + lastRingThickness);
                    rect.set(-size, -size, size, size);

                    angle = ((calendar.hour * 60.0f + calendar.minute) % 1440) / 1440.0f;
                    paint.setColor(mPalette.getHourColor(angle));

                    if (mVariableLineWidth) {
                        lastRingThickness = LARGE_RING_THICKNESS;
                        paint.setStrokeWidth(lastRingThickness);
                    }
                    c.drawArc(rect, 0.0f, angle * 360.0f, false, paint);

                    // Draw day
                    size -= (LARGE_GAP + lastRingThickness);
                    rect.set(-size, -size, size, size);

                    angle = (calendar.monthDay - 1) /
                            (float) (calendar.getActualMaximum(Time.MONTH_DAY) - 1);
                    paint.setColor(mPalette.getDayColor(angle));

                    if (mVariableLineWidth) {
                        lastRingThickness = MEDIUM_RING_THICKNESS;
                        paint.setStrokeWidth(lastRingThickness);
                    }
                    c.drawArc(rect, 0.0f, angle * 360.0f, false, paint);

                    // Draw month
                    size -= (SMALL_GAP + lastRingThickness);
                    rect.set(-size, -size, size, size);

                    angle = (calendar.month) / 11.0f; // NB: month is already on [0..11]

                    paint.setColor(mPalette.getMonthColor(angle));

                    if (mVariableLineWidth) {
                        lastRingThickness = LARGE_RING_THICKNESS;
                        paint.setStrokeWidth(lastRingThickness);
                    }
                    c.drawArc(rect, 0.0f, angle * 360.0f, false, paint);
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }

            mHandler.removeCallbacks(mDrawClock);
            if (mVisible) {
                if (mShowSeconds) {
                    mHandler.postDelayed(mDrawClock, 1000 / 25);
                } else {
                    // If we aren't showing seconds, we don't need to update
                    // nearly as often.
                    mHandler.postDelayed(mDrawClock, 2000);
                }
            }
        }
    }
}
