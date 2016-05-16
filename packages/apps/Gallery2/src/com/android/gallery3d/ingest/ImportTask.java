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

import android.content.Context;
import android.mtp.MtpDevice;
import android.mtp.MtpObjectInfo;
import android.os.Environment;
import android.os.PowerManager;

import com.android.gallery3d.util.GalleryUtils;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ImportTask implements Runnable {

    public interface Listener {
        void onImportProgress(int visitedCount, int totalCount, String pathIfSuccessful);

        void onImportFinish(Collection<MtpObjectInfo> objectsNotImported, int visitedCount);
    }

    static private final String WAKELOCK_LABEL = "MTP Import Task";

    private Listener mListener;
    private String mDestAlbumName;
    private Collection<MtpObjectInfo> mObjectsToImport;
    private MtpDevice mDevice;
    private PowerManager.WakeLock mWakeLock;

    public ImportTask(MtpDevice device, Collection<MtpObjectInfo> objectsToImport,
            String destAlbumName, Context context) {
        mDestAlbumName = destAlbumName;
        mObjectsToImport = objectsToImport;
        mDevice = device;
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, WAKELOCK_LABEL);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public void run() {
        mWakeLock.acquire();
        try {
            List<MtpObjectInfo> objectsNotImported = new LinkedList<MtpObjectInfo>();
            int visited = 0;
            int total = mObjectsToImport.size();
            mListener.onImportProgress(visited, total, null);
            File dest = new File(Environment.getExternalStorageDirectory(), mDestAlbumName);
            dest.mkdirs();
            for (MtpObjectInfo object : mObjectsToImport) {
                visited++;
                String importedPath = null;
                if (GalleryUtils.hasSpaceForSize(object.getCompressedSize())) {
                    importedPath = new File(dest, object.getName()).getAbsolutePath();
                    if (!mDevice.importFile(object.getObjectHandle(), importedPath)) {
                        importedPath = null;
                    }
                }
                if (importedPath == null) {
                    objectsNotImported.add(object);
                }
                if (mListener != null) {
                    mListener.onImportProgress(visited, total, importedPath);
                }
            }
            if (mListener != null) {
                mListener.onImportFinish(objectsNotImported, visited);
            }
        } finally {
            mListener = null;
            mWakeLock.release();
        }
    }
}
