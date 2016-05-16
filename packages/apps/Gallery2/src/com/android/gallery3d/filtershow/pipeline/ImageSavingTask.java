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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.tools.SaveImage;

import java.io.File;

public class ImageSavingTask extends ProcessingTask {
    private ProcessingService mProcessingService;

    static class SaveRequest implements Request {
        Uri sourceUri;
        Uri selectedUri;
        File destinationFile;
        ImagePreset preset;
        boolean flatten;
        int quality;
        float sizeFactor;
        Bitmap previewImage;
        boolean exit;
    }

    static class UpdateBitmap implements Update {
        Bitmap bitmap;
    }

    static class UpdateProgress implements Update {
        int max;
        int current;
    }

    static class UpdatePreviewSaved implements Update {
        Uri uri;
        boolean exit;
    }

    static class URIResult implements Result {
        Uri uri;
        boolean exit;
    }

    public ImageSavingTask(ProcessingService service) {
        mProcessingService = service;
    }

    public void saveImage(Uri sourceUri, Uri selectedUri,
                          File destinationFile, ImagePreset preset,
                          Bitmap previewImage, boolean flatten,
                          int quality, float sizeFactor, boolean exit) {
        SaveRequest request = new SaveRequest();
        request.sourceUri = sourceUri;
        request.selectedUri = selectedUri;
        request.destinationFile = destinationFile;
        request.preset = preset;
        request.flatten = flatten;
        request.quality = quality;
        request.sizeFactor = sizeFactor;
        request.previewImage = previewImage;
        request.exit = exit;
        postRequest(request);
    }

    public Result doInBackground(Request message) {
        SaveRequest request = (SaveRequest) message;
        Uri sourceUri = request.sourceUri;
        Uri selectedUri = request.selectedUri;
        File destinationFile = request.destinationFile;
        Bitmap previewImage = request.previewImage;
        ImagePreset preset = request.preset;
        boolean flatten = request.flatten;
        final boolean exit = request.exit;
        // We create a small bitmap showing the result that we can
        // give to the notification
        UpdateBitmap updateBitmap = new UpdateBitmap();
        updateBitmap.bitmap = createNotificationBitmap(previewImage, sourceUri, preset);
        postUpdate(updateBitmap);
        SaveImage saveImage = new SaveImage(mProcessingService, sourceUri,
                selectedUri, destinationFile, previewImage,
                new SaveImage.Callback() {
                    @Override
                    public void onPreviewSaved(Uri uri){
                        UpdatePreviewSaved previewSaved = new UpdatePreviewSaved();
                        previewSaved.uri = uri;
                        previewSaved.exit = exit;
                        postUpdate(previewSaved);
                    }

                    @Override
                    public void onProgress(int max, int current) {
                        UpdateProgress updateProgress = new UpdateProgress();
                        updateProgress.max = max;
                        updateProgress.current = current;
                        postUpdate(updateProgress);
                    }
                });
        Uri uri = saveImage.processAndSaveImage(preset, flatten,
                request.quality, request.sizeFactor, request.exit);
        URIResult result = new URIResult();
        result.uri = uri;
        result.exit = request.exit;
        return result;
    }

    @Override
    public void onResult(Result message) {
        URIResult result = (URIResult) message;
        mProcessingService.completeSaveImage(result.uri, result.exit);
    }

    @Override
    public void onUpdate(Update message) {
        if (message instanceof UpdatePreviewSaved){
            Uri uri = ((UpdatePreviewSaved) message).uri;
            boolean exit = ((UpdatePreviewSaved) message).exit;
            mProcessingService.completePreviewSaveImage(uri, exit);
        }
        if (message instanceof UpdateBitmap) {
            Bitmap bitmap = ((UpdateBitmap) message).bitmap;
            mProcessingService.updateNotificationWithBitmap(bitmap);
        }
        if (message instanceof UpdateProgress) {
            UpdateProgress progress = (UpdateProgress) message;
            mProcessingService.updateProgress(progress.max, progress.current);
        }
    }

    private Bitmap createNotificationBitmap(Bitmap preview, Uri sourceUri, ImagePreset preset) {
        int notificationBitmapSize = Resources.getSystem().getDimensionPixelSize(
                android.R.dimen.notification_large_icon_width);
        if (preview != null) {
            return Bitmap.createScaledBitmap(preview,
                    notificationBitmapSize, notificationBitmapSize, true);
        }
        Bitmap bitmap = ImageLoader.loadConstrainedBitmap(sourceUri, getContext(),
                notificationBitmapSize, null, true);
        CachingPipeline pipeline = new CachingPipeline(FiltersManager.getManager(), "Thumb");
        return pipeline.renderFinalImage(bitmap, preset);
    }

}
