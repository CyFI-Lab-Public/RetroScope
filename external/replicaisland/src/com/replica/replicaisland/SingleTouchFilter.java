package com.replica.replicaisland;

import android.view.MotionEvent;

public class SingleTouchFilter extends TouchFilter {

	public void updateTouch(MotionEvent event) {
		ContextParameters params = sSystemRegistry.contextParameters;
    	if (event.getAction() == MotionEvent.ACTION_UP) {
    		sSystemRegistry.inputSystem.touchUp(0, event.getRawX() * (1.0f / params.viewScaleX), 
    				event.getRawY() * (1.0f / params.viewScaleY));
    	} else {
    		sSystemRegistry.inputSystem.touchDown(0, event.getRawX() * (1.0f / params.viewScaleX),
    				event.getRawY() * (1.0f / params.viewScaleY));
    	}
    }
	@Override
	public void reset() {
	}

}
