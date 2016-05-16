package com.android.mail.utils;

import android.content.Context;
import android.os.SystemClock;

import com.google.common.collect.Lists;

import java.util.Deque;

/**
 * Utility class to calculate a velocity using a moving average filter of recent input positions.
 * Intended to smooth out touch input events.
 */
public class InputSmoother {

    /**
     * Some devices have significant sampling noise: it could be that samples come in too late,
     * or that the reported position doesn't quite match up with the time. Instantaneous velocity
     * on these devices is too jittery to be useful in deciding whether to instantly snap, so smooth
     * out the data using a moving average over this window size. A sample window size n will
     * effectively average the velocity over n-1 points, so n=2 is the minimum valid value (no
     * averaging at all).
     */
    private static final int SAMPLING_WINDOW_SIZE = 5;

    /**
     * The maximum elapsed time (in millis) between samples that we would consider "consecutive".
     * Only consecutive samples will factor into the rolling average sample window.
     * Any samples that are older than this maximum are continually purged from the sample window,
     * so as to avoid skewing the average with irrelevant older values.
     */
    private static final long MAX_SAMPLE_INTERVAL_MS = 200;

    /**
     * Sampling window to calculate rolling average of scroll velocity.
     */
    private final Deque<Sample> mRecentSamples = Lists.newLinkedList();
    private final float mDensity;

    private static class Sample {
        int pos;
        long millis;
    }

    public InputSmoother(Context context) {
        mDensity = context.getResources().getDisplayMetrics().density;
    }

    public void onInput(int pos) {
        Sample sample;
        final long nowMs = SystemClock.uptimeMillis();

        final Sample last = mRecentSamples.peekLast();
        if (last != null && nowMs - last.millis > MAX_SAMPLE_INTERVAL_MS) {
            mRecentSamples.clear();
        }

        if (mRecentSamples.size() == SAMPLING_WINDOW_SIZE) {
            sample = mRecentSamples.removeFirst();
        } else {
            sample = new Sample();
        }
        sample.pos = pos;
        sample.millis = nowMs;

        mRecentSamples.add(sample);
    }

    /**
     * Calculates velocity based on recent inputs from {@link #onInput(int)}, averaged together to
     * smooth out jitter.
     *
     * @return returns velocity in dp/s, or null if not enough samples have been collected
     */
    public Float getSmoothedVelocity() {
        if (mRecentSamples.size() < 2) {
            // need at least 2 position samples to determine a velocity
            return null;
        }

        // calculate moving average over current window
        int totalDistancePx = 0;
        int prevPos = mRecentSamples.getFirst().pos;
        final long totalTimeMs = mRecentSamples.getLast().millis - mRecentSamples.getFirst().millis;

        if (totalTimeMs <= 0) {
            // samples are really fast or bad. no answer.
            return null;
        }

        for (Sample s : mRecentSamples) {
            totalDistancePx += Math.abs(s.pos - prevPos);
            prevPos = s.pos;
        }
        final float distanceDp = totalDistancePx / mDensity;
        // velocity in dp per second
        return distanceDp * 1000 / totalTimeMs;
    }

}
