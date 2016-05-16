/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.cts.verifier.location;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.util.List;
import java.util.ArrayList;

public class LocationVerifier implements Handler.Callback {

    public static final String TAG = "CtsVerifierLocation";

    private static final int MSG_TIMEOUT = 1;

    /** Timing failures on first NUM_IGNORED_UPDATES updates are ignored. */
    private static final int NUM_IGNORED_UPDATES = 2;

    /* In active mode, the mean computed for the deltas should not be smaller
     * than mInterval * ACTIVE_MIN_MEAN_RATIO */
    private static final double ACTIVE_MIN_MEAN_RATIO = 0.75;

    /* In passive mode, the mean computed for the deltas should not be smaller
     * than mInterval * PASSIVE_MIN_MEAN_RATIO */
    private static final double PASSIVE_MIN_MEAN_RATIO = 0.1;

    /**
     * The standard deviation computed for the deltas should not be bigger
     * than mInterval * ALLOWED_STDEV_ERROR_RATIO
     * or MIN_STDEV_MS, whichever is higher.
     */
    private static final double ALLOWED_STDEV_ERROR_RATIO = 0.50;
    private static final long MIN_STDEV_MS = 1000;

    private final LocationManager mLocationManager;
    private final PassFailLog mCb;
    private final String mProvider;
    private final long mInterval;
    private final long mTimeout;
    private final Handler mHandler;
    private final int mRequestedUpdates;
    private final ActiveListener mActiveListener;
    private final PassiveListener mPassiveListener;

    private boolean isTestOutcomeSet = false;
    private long mLastActiveTimestamp = -1;
    private long mLastPassiveTimestamp = -1;
    private int mNumActiveUpdates = 0;
    private int mNumPassiveUpdates = 0;
    private boolean mIsMockProvider = false;
    private boolean mRunning = false;
    private boolean mActiveLocationArrive = false;

    private List<Long> mActiveDeltas = new ArrayList();
    private List<Long> mPassiveDeltas = new ArrayList();

    private class ActiveListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            if (!mRunning) return;

            mActiveLocationArrive = true;
            mNumActiveUpdates++;
            scheduleTimeout();

            long timestamp = location.getTime();
            long delta = timestamp - mLastActiveTimestamp;
            mLastActiveTimestamp = timestamp;

            if (mNumActiveUpdates <= NUM_IGNORED_UPDATES ) {
                mCb.log("(ignored) active " + mProvider + " update (" + delta + "ms)");
                return;
            }
            if (location.isFromMockProvider() != mIsMockProvider) {
                fail("location coming from \"" + mProvider +
                        "\" provider reports isFromMockProvider() to be " +
                        location.isFromMockProvider());
            }

            mActiveDeltas.add(delta);
            mCb.log("active " + mProvider + " update (" + delta + "ms)");

            if (mNumActiveUpdates >= mRequestedUpdates) {
                assertMeanAndStdev(mProvider, mActiveDeltas, ACTIVE_MIN_MEAN_RATIO);
                assertMeanAndStdev(LocationManager.PASSIVE_PROVIDER, mPassiveDeltas, PASSIVE_MIN_MEAN_RATIO);
                pass();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }
        @Override
        public void onProviderEnabled(String provider) { }
        @Override
        public void onProviderDisabled(String provider) { }
    }

    private void assertMeanAndStdev(String provider, List<Long> deltas, double minMeanRatio) {
        double mean = computeMean(deltas);
        double stdev = computeStdev(mean, deltas);

        double minMean = mInterval * minMeanRatio;
        if (mean < minMean) {
            fail(provider + " provider mean too small: " + mean
                 + " (min: " + minMean + ")");
            return;
        }

        double maxStdev = Math.max(MIN_STDEV_MS, mInterval * ALLOWED_STDEV_ERROR_RATIO);
        if (stdev > maxStdev) {
            fail (provider + " provider stdev too big: "
                  + stdev + " (max: " + maxStdev + ")");
            return;
        }

        mCb.log(provider + " provider mean: " + mean);
        mCb.log(provider + " provider stdev: " + stdev);
    }

    private double computeMean(List<Long> deltas) {
        long accumulator = 0;
        for (long d : deltas) {
            accumulator += d;
        }
        return accumulator / deltas.size();
    }

    private double computeStdev(double mean, List<Long> deltas) {
        double accumulator = 0;
        for (long d : deltas) {
            double diff = d - mean;
            accumulator += diff * diff;
        }
        return Math.sqrt(accumulator / (deltas.size() - 1));
    }

    private class PassiveListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            if (!mRunning) return;
            if (!location.getProvider().equals(mProvider)) return;

            // When a test round start, passive listener shouldn't recevice location before active listener.
            // If this situation occurs, we treat this location as overdue location.
            // (The overdue location comes from previous test round, it occurs occasionally)
            // We have to skip it to prevent wrong calculation of time interval.
            if (!mActiveLocationArrive) {
                mCb.log("ignoring passive " + mProvider + " update");
                return;
            }

            mNumPassiveUpdates++;
            long timestamp = location.getTime();
            long delta = timestamp - mLastPassiveTimestamp;
            mLastPassiveTimestamp = timestamp;

            if (mNumPassiveUpdates <= NUM_IGNORED_UPDATES) {
                mCb.log("(ignored) passive " + mProvider + " update (" + delta + "ms)");
                return;
            }
            if (location.isFromMockProvider() != mIsMockProvider) {
                fail("location coming from \"" + mProvider +
                        "\" provider reports isFromMockProvider() to be " +
                        location.isFromMockProvider());
            }

            mPassiveDeltas.add(delta);
            mCb.log("passive " + mProvider + " update (" + delta + "ms)");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }
        @Override
        public void onProviderEnabled(String provider) { }
        @Override
        public void onProviderDisabled(String provider) { }
    }

    public LocationVerifier(PassFailLog cb, LocationManager locationManager,
            String provider, long requestedInterval, int numUpdates, boolean isMockProvider) {
        mProvider = provider;
        mInterval = requestedInterval;
        // timeout at 60 seconds after interval time
        mTimeout = requestedInterval + 60 * 1000;
        mRequestedUpdates = numUpdates + NUM_IGNORED_UPDATES;
        mLocationManager = locationManager;
        mCb = cb;
        mHandler = new Handler(this);
        mActiveListener = new ActiveListener();
        mPassiveListener = new PassiveListener();
        mIsMockProvider = isMockProvider;
    }

    public void start() {
        mRunning = true;
        scheduleTimeout();
        mLastActiveTimestamp = System.currentTimeMillis();
        mLastPassiveTimestamp = mLastActiveTimestamp;
        mCb.log("enabling passive listener");
        mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0,
                mPassiveListener);
        mCb.log("enabling " + mProvider + " (minTime=" + mInterval + "ms)");
        mLocationManager.requestLocationUpdates(mProvider, mInterval, 0,
                mActiveListener);
    }

    public void stop() {
        mRunning = false;
        mCb.log("disabling " + mProvider);
        mLocationManager.removeUpdates(mActiveListener);
        mCb.log("disabling passive listener");
        mLocationManager.removeUpdates(mPassiveListener);
        mHandler.removeMessages(MSG_TIMEOUT);
    }

    private void pass() {
        if (!isTestOutcomeSet) {
            stop();
            mCb.pass();
            isTestOutcomeSet = true;
        }
    }

    private void fail(String s) {
        if (!isTestOutcomeSet) {
            stop();
            mCb.fail(s);
            isTestOutcomeSet = true;
        }
    }

    private void scheduleTimeout() {
        mHandler.removeMessages(MSG_TIMEOUT);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_TIMEOUT), mTimeout);
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (!mRunning) return true;
        fail("timeout (" + mTimeout + "ms) waiting for " +
                mProvider + " location change");
        return true;
    }
}
