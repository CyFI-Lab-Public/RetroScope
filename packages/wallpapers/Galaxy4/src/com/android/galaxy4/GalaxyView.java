package com.android.galaxy4;

import android.content.Context;
import android.graphics.PixelFormat;
import android.renderscript.RSSurfaceView;
import android.renderscript.RenderScriptGL;
import android.renderscript.RenderScriptGL.SurfaceConfig;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.app.Service;
import android.util.Log;
import android.util.DisplayMetrics;

public class GalaxyView extends RSSurfaceView {

    private RenderScriptGL mRS;
    private GalaxyRS mRender;

    public GalaxyView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        super.surfaceChanged(holder, format, w, h);
        if (mRS == null) {
            RenderScriptGL.SurfaceConfig sc = new RenderScriptGL.SurfaceConfig();
            mRS = createRenderScriptGL(sc);
            mRS.setSurface(holder, w, h);

            DisplayMetrics metrics = new DisplayMetrics();
            ((WindowManager) getContext()
                    .getSystemService(Service.WINDOW_SERVICE))
                    .getDefaultDisplay().getMetrics(metrics);

            mRender = new GalaxyRS();
            mRender.init(metrics.densityDpi, mRS, getResources(), w, h);
        } else {
            mRender.createProgramVertex();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mRS != null) {
            mRS.setSurface(null, 0, 0);
            mRS = null;
            destroyRenderScriptGL();
        }
    }

}
