/* //device/content/providers/media/src/com/android/providers/media/MediaScannerService.java
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package com.android.providers.media;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.IMediaScannerListener;
import android.media.IMediaScannerService;
import android.media.MediaScanner;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;

public class MediaScannerService extends Service implements Runnable
{
    private static final String TAG = "MediaScannerService";

    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private PowerManager.WakeLock mWakeLock;
    private String[] mExternalStoragePaths;
    
    private void openDatabase(String volumeName) {
        try {
            ContentValues values = new ContentValues();
            values.put("name", volumeName);
            getContentResolver().insert(Uri.parse("content://media/"), values);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "failed to open media database");
        }         
    }

    private MediaScanner createMediaScanner() {
        MediaScanner scanner = new MediaScanner(this);
        Locale locale = getResources().getConfiguration().locale;
        if (locale != null) {
            String language = locale.getLanguage();
            String country = locale.getCountry();
            String localeString = null;
            if (language != null) {
                if (country != null) {
                    scanner.setLocale(language + "_" + country);
                } else {
                    scanner.setLocale(language);
                }
            }    
        }
        
        return scanner;
    }

    private void scan(String[] directories, String volumeName) {
        Uri uri = Uri.parse("file://" + directories[0]);
        // don't sleep while scanning
        mWakeLock.acquire();

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MEDIA_SCANNER_VOLUME, volumeName);
            Uri scanUri = getContentResolver().insert(MediaStore.getMediaScannerUri(), values);

            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_STARTED, uri));

            try {
                if (volumeName.equals(MediaProvider.EXTERNAL_VOLUME)) {
                    openDatabase(volumeName);
                }

                MediaScanner scanner = createMediaScanner();
                scanner.scanDirectories(directories, volumeName);
            } catch (Exception e) {
                Log.e(TAG, "exception in MediaScanner.scan()", e);
            }

            getContentResolver().delete(scanUri, null, null);

        } finally {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_FINISHED, uri));
            mWakeLock.release();
        }
    }
    
    @Override
    public void onCreate()
    {
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        StorageManager storageManager = (StorageManager)getSystemService(Context.STORAGE_SERVICE);
        mExternalStoragePaths = storageManager.getVolumePaths();

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        Thread thr = new Thread(null, this, "MediaScannerService");
        thr.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        while (mServiceHandler == null) {
            synchronized (this) {
                try {
                    wait(100);
                } catch (InterruptedException e) {
                }
            }
        }

        if (intent == null) {
            Log.e(TAG, "Intent is null in onStartCommand: ",
                new NullPointerException());
            return Service.START_NOT_STICKY;
        }

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent.getExtras();
        mServiceHandler.sendMessage(msg);

        // Try again later if we are killed before we can finish scanning.
        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy()
    {
        // Make sure thread has started before telling it to quit.
        while (mServiceLooper == null) {
            synchronized (this) {
                try {
                    wait(100);
                } catch (InterruptedException e) {
                }
            }
        }
        mServiceLooper.quit();
    }

    public void run()
    {
        // reduce priority below other background threads to avoid interfering
        // with other services at boot time.
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND +
                Process.THREAD_PRIORITY_LESS_FAVORABLE);
        Looper.prepare();

        mServiceLooper = Looper.myLooper();
        mServiceHandler = new ServiceHandler();

        Looper.loop();
    }
   
    private Uri scanFile(String path, String mimeType) {
        String volumeName = MediaProvider.EXTERNAL_VOLUME;
        openDatabase(volumeName);
        MediaScanner scanner = createMediaScanner();
        try {
            // make sure the file path is in canonical form
            String canonicalPath = new File(path).getCanonicalPath();
            return scanner.scanSingleFile(canonicalPath, volumeName, mimeType);
        } catch (Exception e) {
            Log.e(TAG, "bad path " + path + " in scanFile()", e);
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }
    
    private final IMediaScannerService.Stub mBinder = 
            new IMediaScannerService.Stub() {
        public void requestScanFile(String path, String mimeType, IMediaScannerListener listener)
        {
            if (false) {
                Log.d(TAG, "IMediaScannerService.scanFile: " + path + " mimeType: " + mimeType);
            }
            Bundle args = new Bundle();
            args.putString("filepath", path);
            args.putString("mimetype", mimeType);
            if (listener != null) {
                args.putIBinder("listener", listener.asBinder());
            }
            startService(new Intent(MediaScannerService.this,
                    MediaScannerService.class).putExtras(args));
        }

        public void scanFile(String path, String mimeType) {
            requestScanFile(path, mimeType, null);
        }
    };

    private final class ServiceHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg)
        {
            Bundle arguments = (Bundle) msg.obj;
            String filePath = arguments.getString("filepath");
            
            try {
                if (filePath != null) {
                    IBinder binder = arguments.getIBinder("listener");
                    IMediaScannerListener listener = 
                            (binder == null ? null : IMediaScannerListener.Stub.asInterface(binder));
                    Uri uri = null;
                    try {
                        uri = scanFile(filePath, arguments.getString("mimetype"));
                    } catch (Exception e) {
                        Log.e(TAG, "Exception scanning file", e);
                    }
                    if (listener != null) {
                        listener.scanCompleted(filePath, uri);
                    }
                } else {
                    String volume = arguments.getString("volume");
                    String[] directories = null;
                    
                    if (MediaProvider.INTERNAL_VOLUME.equals(volume)) {
                        // scan internal media storage
                        directories = new String[] {
                                Environment.getRootDirectory() + "/media",
                        };
                    }
                    else if (MediaProvider.EXTERNAL_VOLUME.equals(volume)) {
                        // scan external storage volumes
                        directories = mExternalStoragePaths;
                    }

                    if (directories != null) {
                        if (false) Log.d(TAG, "start scanning volume " + volume + ": "
                                + Arrays.toString(directories));
                        scan(directories, volume);
                        if (false) Log.d(TAG, "done scanning volume " + volume);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in handleMessage", e);
            }

            stopSelf(msg.arg1);
        }
    };
}

