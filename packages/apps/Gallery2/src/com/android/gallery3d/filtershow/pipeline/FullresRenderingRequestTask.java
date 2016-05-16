package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;
import com.android.gallery3d.filtershow.filters.FiltersManager;

public class FullresRenderingRequestTask extends ProcessingTask {

    private CachingPipeline mFullresPipeline = null;
    private boolean mPipelineIsOn = false;

    public void setPreviewScaleFactor(float previewScale) {
        mFullresPipeline.setPreviewScaleFactor(previewScale);
    }

    static class Render implements Request {
        RenderingRequest request;
    }

    static class RenderResult implements Result {
        RenderingRequest request;
    }

    public FullresRenderingRequestTask() {
        mFullresPipeline = new CachingPipeline(
                FiltersManager.getHighresManager(), "Fullres");
    }

    public void setOriginal(Bitmap bitmap) {
        mFullresPipeline.setOriginal(bitmap);
        mPipelineIsOn = true;
    }

    public void stop() {
        mFullresPipeline.stop();
    }

    public void postRenderingRequest(RenderingRequest request) {
        if (!mPipelineIsOn) {
            return;
        }
        Render render = new Render();
        render.request = request;
        postRequest(render);
    }

    @Override
    public Result doInBackground(Request message) {
        RenderingRequest request = ((Render) message).request;
        RenderResult result = null;
        mFullresPipeline.render(request);
        result = new RenderResult();
        result.request = request;
        return result;
    }

    @Override
    public void onResult(Result message) {
        if (message == null) {
            return;
        }
        RenderingRequest request = ((RenderResult) message).request;
        request.markAvailable();
    }

    @Override
    public boolean isDelayedTask() { return true; }
}
