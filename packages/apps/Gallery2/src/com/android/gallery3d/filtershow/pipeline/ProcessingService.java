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

package com.android.gallery3d.filtershow.pipeline;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.filters.ImageFilter;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.tools.SaveImage;

import java.io.File;

public class ProcessingService extends Service {
    private static final String LOGTAG = "ProcessingService";
    private static final boolean SHOW_IMAGE = false;
    private int mNotificationId;
    private NotificationManager mNotifyMgr = null;
    private Notification.Builder mBuilder = null;

    private static final String PRESET = "preset";
    private static final String QUALITY = "quality";
    private static final String SOURCE_URI = "sourceUri";
    private static final String SELECTED_URI = "selectedUri";
    private static final String DESTINATION_FILE = "destinationFile";
    private static final String SAVING = "saving";
    private static final String FLATTEN = "flatten";
    private static final String SIZE_FACTOR = "sizeFactor";
    private static final String EXIT = "exit";

    private ProcessingTaskController mProcessingTaskController;
    private ImageSavingTask mImageSavingTask;
    private UpdatePreviewTask mUpdatePreviewTask;
    private HighresRenderingRequestTask mHighresRenderingRequestTask;
    private FullresRenderingRequestTask mFullresRenderingRequestTask;
    private RenderingRequestTask mRenderingRequestTask;

    private final IBinder mBinder = new LocalBinder();
    private FilterShowActivity mFiltershowActivity;

    private boolean mSaving = false;
    private boolean mNeedsAlive = false;

    public void setFiltershowActivity(FilterShowActivity filtershowActivity) {
        mFiltershowActivity = filtershowActivity;
    }

    public void setOriginalBitmap(Bitmap originalBitmap) {
        if (mUpdatePreviewTask == null) {
            return;
        }
        mUpdatePreviewTask.setOriginal(originalBitmap);
        mHighresRenderingRequestTask.setOriginal(originalBitmap);
        mFullresRenderingRequestTask.setOriginal(originalBitmap);
        mRenderingRequestTask.setOriginal(originalBitmap);
    }

    public void updatePreviewBuffer() {
        mHighresRenderingRequestTask.stop();
        mFullresRenderingRequestTask.stop();
        mUpdatePreviewTask.updatePreview();
    }

    public void postRenderingRequest(RenderingRequest request) {
        mRenderingRequestTask.postRenderingRequest(request);
    }

    public void postHighresRenderingRequest(ImagePreset preset, float scaleFactor,
                                            RenderingRequestCaller caller) {
        RenderingRequest request = new RenderingRequest();
        // TODO: use the triple buffer preset as UpdatePreviewTask does instead of creating a copy
        ImagePreset passedPreset = new ImagePreset(preset);
        request.setOriginalImagePreset(preset);
        request.setScaleFactor(scaleFactor);
        request.setImagePreset(passedPreset);
        request.setType(RenderingRequest.HIGHRES_RENDERING);
        request.setCaller(caller);
        mHighresRenderingRequestTask.postRenderingRequest(request);
    }

    public void postFullresRenderingRequest(ImagePreset preset, float scaleFactor,
                                            Rect bounds, Rect destination,
                                            RenderingRequestCaller caller) {
        RenderingRequest request = new RenderingRequest();
        ImagePreset passedPreset = new ImagePreset(preset);
        request.setOriginalImagePreset(preset);
        request.setScaleFactor(scaleFactor);
        request.setImagePreset(passedPreset);
        request.setType(RenderingRequest.PARTIAL_RENDERING);
        request.setCaller(caller);
        request.setBounds(bounds);
        request.setDestination(destination);
        passedPreset.setPartialRendering(true, bounds);
        mFullresRenderingRequestTask.postRenderingRequest(request);
    }

    public void setHighresPreviewScaleFactor(float highResPreviewScale) {
        mHighresRenderingRequestTask.setHighresPreviewScaleFactor(highResPreviewScale);
    }

    public void setPreviewScaleFactor(float previewScale) {
        mHighresRenderingRequestTask.setPreviewScaleFactor(previewScale);
        mFullresRenderingRequestTask.setPreviewScaleFactor(previewScale);
        mRenderingRequestTask.setPreviewScaleFactor(previewScale);
    }

    public void setOriginalBitmapHighres(Bitmap originalHires) {
        mHighresRenderingRequestTask.setOriginalBitmapHighres(originalHires);
    }

    public class LocalBinder extends Binder {
        public ProcessingService getService() {
            return ProcessingService.this;
        }
    }

    public static Intent getSaveIntent(Context context, ImagePreset preset, File destination,
            Uri selectedImageUri, Uri sourceImageUri, boolean doFlatten, int quality,
            float sizeFactor, boolean needsExit) {
        Intent processIntent = new Intent(context, ProcessingService.class);
        processIntent.putExtra(ProcessingService.SOURCE_URI,
                sourceImageUri.toString());
        processIntent.putExtra(ProcessingService.SELECTED_URI,
                selectedImageUri.toString());
        processIntent.putExtra(ProcessingService.QUALITY, quality);
        processIntent.putExtra(ProcessingService.SIZE_FACTOR, sizeFactor);
        if (destination != null) {
            processIntent.putExtra(ProcessingService.DESTINATION_FILE, destination.toString());
        }
        processIntent.putExtra(ProcessingService.PRESET,
                preset.getJsonString(ImagePreset.JASON_SAVED));
        processIntent.putExtra(ProcessingService.SAVING, true);
        processIntent.putExtra(ProcessingService.EXIT, needsExit);
        if (doFlatten) {
            processIntent.putExtra(ProcessingService.FLATTEN, true);
        }
        return processIntent;
    }


    @Override
    public void onCreate() {
        mProcessingTaskController = new ProcessingTaskController(this);
        mImageSavingTask = new ImageSavingTask(this);
        mUpdatePreviewTask = new UpdatePreviewTask();
        mHighresRenderingRequestTask = new HighresRenderingRequestTask();
        mFullresRenderingRequestTask = new FullresRenderingRequestTask();
        mRenderingRequestTask = new RenderingRequestTask();
        mProcessingTaskController.add(mImageSavingTask);
        mProcessingTaskController.add(mUpdatePreviewTask);
        mProcessingTaskController.add(mHighresRenderingRequestTask);
        mProcessingTaskController.add(mFullresRenderingRequestTask);
        mProcessingTaskController.add(mRenderingRequestTask);
        setupPipeline();
    }

    @Override
    public void onDestroy() {
        tearDownPipeline();
        mProcessingTaskController.quit();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mNeedsAlive = true;
        if (intent != null && intent.getBooleanExtra(SAVING, false)) {
            // we save using an intent to keep the service around after the
            // activity has been destroyed.
            String presetJson = intent.getStringExtra(PRESET);
            String source = intent.getStringExtra(SOURCE_URI);
            String selected = intent.getStringExtra(SELECTED_URI);
            String destination = intent.getStringExtra(DESTINATION_FILE);
            int quality = intent.getIntExtra(QUALITY, 100);
            float sizeFactor = intent.getFloatExtra(SIZE_FACTOR, 1);
            boolean flatten = intent.getBooleanExtra(FLATTEN, false);
            boolean exit = intent.getBooleanExtra(EXIT, false);
            Uri sourceUri = Uri.parse(source);
            Uri selectedUri = null;
            if (selected != null) {
                selectedUri = Uri.parse(selected);
            }
            File destinationFile = null;
            if (destination != null) {
                destinationFile = new File(destination);
            }
            ImagePreset preset = new ImagePreset();
            preset.readJsonFromString(presetJson);
            mNeedsAlive = false;
            mSaving = true;
            handleSaveRequest(sourceUri, selectedUri, destinationFile, preset,
                    MasterImage.getImage().getHighresImage(),
                    flatten, quality, sizeFactor, exit);
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void onStart() {
        mNeedsAlive = true;
        if (!mSaving && mFiltershowActivity != null) {
            mFiltershowActivity.updateUIAfterServiceStarted();
        }
    }

    public void handleSaveRequest(Uri sourceUri, Uri selectedUri,
            File destinationFile, ImagePreset preset, Bitmap previewImage,
            boolean flatten, int quality, float sizeFactor, boolean exit) {
        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancelAll();

        mBuilder =
                new Notification.Builder(this)
                        .setSmallIcon(R.drawable.filtershow_button_fx)
                        .setContentTitle(getString(R.string.filtershow_notification_label))
                        .setContentText(getString(R.string.filtershow_notification_message));

        startForeground(mNotificationId, mBuilder.build());

        updateProgress(SaveImage.MAX_PROCESSING_STEPS, 0);

        // Process the image

        mImageSavingTask.saveImage(sourceUri, selectedUri, destinationFile,
                preset, previewImage, flatten, quality, sizeFactor, exit);
    }

    public void updateNotificationWithBitmap(Bitmap bitmap) {
        mBuilder.setLargeIcon(bitmap);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    public void updateProgress(int max, int current) {
        mBuilder.setProgress(max, current, false);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    public void completePreviewSaveImage(Uri result, boolean exit) {
        if (exit && !mNeedsAlive && !mFiltershowActivity.isSimpleEditAction()) {
            mFiltershowActivity.completeSaveImage(result);
        }
    }

    public void completeSaveImage(Uri result, boolean exit) {
        if (SHOW_IMAGE) {
            // TODO: we should update the existing image in Gallery instead
            Intent viewImage = new Intent(Intent.ACTION_VIEW, result);
            viewImage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(viewImage);
        }
        mNotifyMgr.cancel(mNotificationId);
        if (!exit) {
            stopForeground(true);
            stopSelf();
            return;
        }
        stopForeground(true);
        stopSelf();
        if (mNeedsAlive) {
            // If the app has been restarted while we were saving...
            mFiltershowActivity.updateUIAfterServiceStarted();
        } else if (exit || mFiltershowActivity.isSimpleEditAction()) {
            // terminate now
            mFiltershowActivity.completeSaveImage(result);
        }
    }

    private void setupPipeline() {
        Resources res = getResources();
        FiltersManager.setResources(res);
        CachingPipeline.createRenderscriptContext(this);

        FiltersManager filtersManager = FiltersManager.getManager();
        filtersManager.addLooks(this);
        filtersManager.addBorders(this);
        filtersManager.addTools(this);
        filtersManager.addEffects();

        FiltersManager highresFiltersManager = FiltersManager.getHighresManager();
        highresFiltersManager.addLooks(this);
        highresFiltersManager.addBorders(this);
        highresFiltersManager.addTools(this);
        highresFiltersManager.addEffects();
    }

    private void tearDownPipeline() {
        ImageFilter.resetStatics();
        FiltersManager.getPreviewManager().freeRSFilterScripts();
        FiltersManager.getManager().freeRSFilterScripts();
        FiltersManager.getHighresManager().freeRSFilterScripts();
        FiltersManager.reset();
        CachingPipeline.destroyRenderScriptContext();
    }

    static {
        System.loadLibrary("jni_filtershow_filters");
    }
}
