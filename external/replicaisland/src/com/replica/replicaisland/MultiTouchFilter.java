package com.replica.replicaisland;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.MotionEvent;

public class MultiTouchFilter extends SingleTouchFilter {
	private boolean mCheckedForMultitouch = false;
	private boolean mSupportsMultitouch = false;
	
    @Override
    public void updateTouch(MotionEvent event) {
		ContextParameters params = sSystemRegistry.contextParameters;
    	final int pointerCount = event.getPointerCount();
    	for (int x = 0; x < pointerCount; x++) {
    		final int action = event.getAction();
    		final int actualEvent = action & MotionEvent.ACTION_MASK;
    		final int id = event.getPointerId(x);
    		if (actualEvent == MotionEvent.ACTION_POINTER_UP || 
    				actualEvent == MotionEvent.ACTION_UP || 
    				actualEvent == MotionEvent.ACTION_CANCEL) {
        		BaseObject.sSystemRegistry.inputSystem.touchUp(id, 
        				event.getX(x) * (1.0f / params.viewScaleX), 
        				event.getY(x) * (1.0f / params.viewScaleY));
        	} else {
        		BaseObject.sSystemRegistry.inputSystem.touchDown(id, 
        				event.getX(x) * (1.0f / params.viewScaleX),
        				event.getY(x) * (1.0f / params.viewScaleY));
        	}
    	}
    }
    
    @Override
    public boolean supportsMultitouch(Context context) {
    	if (!mCheckedForMultitouch) {
    		PackageManager packageManager = context.getPackageManager();
    		mSupportsMultitouch = packageManager.hasSystemFeature("android.hardware.touchscreen.multitouch");
    		mCheckedForMultitouch = true;
    	}
    	
    	return mSupportsMultitouch;
    }
}
