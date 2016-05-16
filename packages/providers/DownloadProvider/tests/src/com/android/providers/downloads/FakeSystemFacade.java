package com.android.providers.downloads;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;

import java.util.ArrayList;
import java.util.List;
public class FakeSystemFacade implements SystemFacade {
    long mTimeMillis = 0;
    Integer mActiveNetworkType = ConnectivityManager.TYPE_WIFI;
    boolean mIsRoaming = false;
    boolean mIsMetered = false;
    Long mMaxBytesOverMobile = null;
    Long mRecommendedMaxBytesOverMobile = null;
    List<Intent> mBroadcastsSent = new ArrayList<Intent>();
    private boolean mReturnActualTime = false;

    public void setUp() {
        mTimeMillis = 0;
        mActiveNetworkType = ConnectivityManager.TYPE_WIFI;
        mIsRoaming = false;
        mIsMetered = false;
        mMaxBytesOverMobile = null;
        mRecommendedMaxBytesOverMobile = null;
        mBroadcastsSent.clear();
        mReturnActualTime = false;
    }

    void incrementTimeMillis(long delta) {
        mTimeMillis += delta;
    }

    @Override
    public long currentTimeMillis() {
        if (mReturnActualTime) {
            return System.currentTimeMillis();
        }
        return mTimeMillis;
    }

    @Override
    public NetworkInfo getActiveNetworkInfo(int uid) {
        if (mActiveNetworkType == null) {
            return null;
        } else {
            final NetworkInfo info = new NetworkInfo(mActiveNetworkType, 0, null, null);
            info.setDetailedState(DetailedState.CONNECTED, null, null);
            return info;
        }
    }

    @Override
    public boolean isActiveNetworkMetered() {
        return mIsMetered;
    }

    @Override
    public boolean isNetworkRoaming() {
        return mIsRoaming;
    }

    @Override
    public Long getMaxBytesOverMobile() {
        return mMaxBytesOverMobile ;
    }

    @Override
    public Long getRecommendedMaxBytesOverMobile() {
        return mRecommendedMaxBytesOverMobile ;
    }

    @Override
    public void sendBroadcast(Intent intent) {
        mBroadcastsSent.add(intent);
    }

    @Override
    public boolean userOwnsPackage(int uid, String pckg) throws NameNotFoundException {
        return true;
    }

    public void setReturnActualTime(boolean flag) {
        mReturnActualTime = flag;
    }
}
