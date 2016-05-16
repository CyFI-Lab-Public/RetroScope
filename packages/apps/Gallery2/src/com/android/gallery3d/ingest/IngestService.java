/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.gallery3d.ingest;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.mtp.MtpDevice;
import android.mtp.MtpDeviceInfo;
import android.mtp.MtpObjectInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.SparseBooleanArray;
import android.widget.Adapter;

import com.android.gallery3d.R;
import com.android.gallery3d.app.NotificationIds;
import com.android.gallery3d.data.MtpClient;
import com.android.gallery3d.util.BucketNames;
import com.android.gallery3d.util.UsageStatistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IngestService extends Service implements ImportTask.Listener,
        MtpDeviceIndex.ProgressListener, MtpClient.Listener {

    public class LocalBinder extends Binder {
        IngestService getService() {
            return IngestService.this;
        }
    }

    private static final int PROGRESS_UPDATE_INTERVAL_MS = 180;

    private static MtpClient sClient;

    private final IBinder mBinder = new LocalBinder();
    private ScannerClient mScannerClient;
    private MtpDevice mDevice;
    private String mDevicePrettyName;
    private MtpDeviceIndex mIndex;
    private IngestActivity mClientActivity;
    private boolean mRedeliverImportFinish = false;
    private int mRedeliverImportFinishCount = 0;
    private Collection<MtpObjectInfo> mRedeliverObjectsNotImported;
    private boolean mRedeliverNotifyIndexChanged = false;
    private boolean mRedeliverIndexFinish = false;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    private long mLastProgressIndexTime = 0;
    private boolean mNeedRelaunchNotification = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mScannerClient = new ScannerClient(this);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationBuilder = new NotificationCompat.Builder(this);
        mNotificationBuilder.setSmallIcon(android.R.drawable.stat_notify_sync) // TODO drawable
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, IngestActivity.class), 0));
        mIndex = MtpDeviceIndex.getInstance();
        mIndex.setProgressListener(this);

        if (sClient == null) {
            sClient = new MtpClient(getApplicationContext());
        }
        List<MtpDevice> devices = sClient.getDeviceList();
        if (devices.size() > 0) {
            setDevice(devices.get(0));
        }
        sClient.addListener(this);
    }

    @Override
    public void onDestroy() {
        sClient.removeListener(this);
        mIndex.unsetProgressListener(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void setDevice(MtpDevice device) {
        if (mDevice == device) return;
        mRedeliverImportFinish = false;
        mRedeliverObjectsNotImported = null;
        mRedeliverNotifyIndexChanged = false;
        mRedeliverIndexFinish = false;
        mDevice = device;
        mIndex.setDevice(mDevice);
        if (mDevice != null) {
            MtpDeviceInfo deviceInfo = mDevice.getDeviceInfo();
            if (deviceInfo == null) {
                setDevice(null);
                return;
            } else {
                mDevicePrettyName = deviceInfo.getModel();
                mNotificationBuilder.setContentTitle(mDevicePrettyName);
                new Thread(mIndex.getIndexRunnable()).start();
            }
        } else {
            mDevicePrettyName = null;
        }
        if (mClientActivity != null) {
            mClientActivity.notifyIndexChanged();
        } else {
            mRedeliverNotifyIndexChanged = true;
        }
    }

    protected MtpDeviceIndex getIndex() {
        return mIndex;
    }

    protected void setClientActivity(IngestActivity activity) {
        if (mClientActivity == activity) return;
        mClientActivity = activity;
        if (mClientActivity == null) {
            if (mNeedRelaunchNotification) {
                mNotificationBuilder.setProgress(0, 0, false)
                    .setContentText(getResources().getText(R.string.ingest_scanning_done));
                mNotificationManager.notify(NotificationIds.INGEST_NOTIFICATION_SCANNING,
                    mNotificationBuilder.build());
            }
            return;
        }
        mNotificationManager.cancel(NotificationIds.INGEST_NOTIFICATION_IMPORTING);
        mNotificationManager.cancel(NotificationIds.INGEST_NOTIFICATION_SCANNING);
        if (mRedeliverImportFinish) {
            mClientActivity.onImportFinish(mRedeliverObjectsNotImported,
                    mRedeliverImportFinishCount);
            mRedeliverImportFinish = false;
            mRedeliverObjectsNotImported = null;
        }
        if (mRedeliverNotifyIndexChanged) {
            mClientActivity.notifyIndexChanged();
            mRedeliverNotifyIndexChanged = false;
        }
        if (mRedeliverIndexFinish) {
            mClientActivity.onIndexFinish();
            mRedeliverIndexFinish = false;
        }
        if (mDevice != null) {
            mNeedRelaunchNotification = true;
        }
    }

    protected void importSelectedItems(SparseBooleanArray selected, Adapter adapter) {
        List<MtpObjectInfo> importHandles = new ArrayList<MtpObjectInfo>();
        for (int i = 0; i < selected.size(); i++) {
            if (selected.valueAt(i)) {
                Object item = adapter.getItem(selected.keyAt(i));
                if (item instanceof MtpObjectInfo) {
                    importHandles.add(((MtpObjectInfo) item));
                }
            }
        }
        ImportTask task = new ImportTask(mDevice, importHandles, BucketNames.IMPORTED, this);
        task.setListener(this);
        mNotificationBuilder.setProgress(0, 0, true)
            .setContentText(getResources().getText(R.string.ingest_importing));
        startForeground(NotificationIds.INGEST_NOTIFICATION_IMPORTING,
                    mNotificationBuilder.build());
        new Thread(task).start();
    }

    @Override
    public void deviceAdded(MtpDevice device) {
        if (mDevice == null) {
            setDevice(device);
            UsageStatistics.onEvent(UsageStatistics.COMPONENT_IMPORTER,
                    "DeviceConnected", null);
        }
    }

    @Override
    public void deviceRemoved(MtpDevice device) {
        if (device == mDevice) {
            setDevice(null);
            mNeedRelaunchNotification = false;
            mNotificationManager.cancel(NotificationIds.INGEST_NOTIFICATION_SCANNING);
        }
    }

    @Override
    public void onImportProgress(int visitedCount, int totalCount,
            String pathIfSuccessful) {
        if (pathIfSuccessful != null) {
            mScannerClient.scanPath(pathIfSuccessful);
        }
        mNeedRelaunchNotification = false;
        if (mClientActivity != null) {
            mClientActivity.onImportProgress(visitedCount, totalCount, pathIfSuccessful);
        }
        mNotificationBuilder.setProgress(totalCount, visitedCount, false)
            .setContentText(getResources().getText(R.string.ingest_importing));
        mNotificationManager.notify(NotificationIds.INGEST_NOTIFICATION_IMPORTING,
                mNotificationBuilder.build());
    }

    @Override
    public void onImportFinish(Collection<MtpObjectInfo> objectsNotImported,
            int visitedCount) {
        stopForeground(true);
        mNeedRelaunchNotification = true;
        if (mClientActivity != null) {
            mClientActivity.onImportFinish(objectsNotImported, visitedCount);
        } else {
            mRedeliverImportFinish = true;
            mRedeliverObjectsNotImported = objectsNotImported;
            mRedeliverImportFinishCount = visitedCount;
            mNotificationBuilder.setProgress(0, 0, false)
                .setContentText(getResources().getText(R.string.import_complete));
            mNotificationManager.notify(NotificationIds.INGEST_NOTIFICATION_IMPORTING,
                    mNotificationBuilder.build());
        }
        UsageStatistics.onEvent(UsageStatistics.COMPONENT_IMPORTER,
                "ImportFinished", null, visitedCount);
    }

    @Override
    public void onObjectIndexed(MtpObjectInfo object, int numVisited) {
        mNeedRelaunchNotification = false;
        if (mClientActivity != null) {
            mClientActivity.onObjectIndexed(object, numVisited);
        } else {
            // Throttle the updates to one every PROGRESS_UPDATE_INTERVAL_MS milliseconds
            long currentTime = SystemClock.uptimeMillis();
            if (currentTime > mLastProgressIndexTime + PROGRESS_UPDATE_INTERVAL_MS) {
                mLastProgressIndexTime = currentTime;
                mNotificationBuilder.setProgress(0, numVisited, true)
                        .setContentText(getResources().getText(R.string.ingest_scanning));
                mNotificationManager.notify(NotificationIds.INGEST_NOTIFICATION_SCANNING,
                        mNotificationBuilder.build());
            }
        }
    }

    @Override
    public void onSorting() {
        if (mClientActivity != null) mClientActivity.onSorting();
    }

    @Override
    public void onIndexFinish() {
        mNeedRelaunchNotification = true;
        if (mClientActivity != null) {
            mClientActivity.onIndexFinish();
        } else {
            mNotificationBuilder.setProgress(0, 0, false)
                .setContentText(getResources().getText(R.string.ingest_scanning_done));
            mNotificationManager.notify(NotificationIds.INGEST_NOTIFICATION_SCANNING,
                    mNotificationBuilder.build());
            mRedeliverIndexFinish = true;
        }
    }

    // Copied from old Gallery3d code
    private static final class ScannerClient implements MediaScannerConnectionClient {
        ArrayList<String> mPaths = new ArrayList<String>();
        MediaScannerConnection mScannerConnection;
        boolean mConnected;
        Object mLock = new Object();

        public ScannerClient(Context context) {
            mScannerConnection = new MediaScannerConnection(context, this);
        }

        public void scanPath(String path) {
            synchronized (mLock) {
                if (mConnected) {
                    mScannerConnection.scanFile(path, null);
                } else {
                    mPaths.add(path);
                    mScannerConnection.connect();
                }
            }
        }

        @Override
        public void onMediaScannerConnected() {
            synchronized (mLock) {
                mConnected = true;
                if (!mPaths.isEmpty()) {
                    for (String path : mPaths) {
                        mScannerConnection.scanFile(path, null);
                    }
                    mPaths.clear();
                }
            }
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
        }
    }
}
