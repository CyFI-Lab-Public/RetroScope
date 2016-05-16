/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.phone;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * OnTouchListener used to shrink the "hit target" of some onscreen buttons.
 *
 * We do this for a few specific buttons which are vulnerable to
 * "false touches" because either (1) they're near the edge of the
 * screen and might be unintentionally touched while holding the
 * device in your hand, (2) they're in the upper corners and might
 * be touched by the user's ear before the prox sensor has a chance to
 * kick in, or (3) they are close to other buttons.
 */
public class SmallerHitTargetTouchListener implements View.OnTouchListener {
    private static final String TAG = "SmallerHitTargetTouchListener";

    /**
     * Edge dimensions where a touch does not register an action (in DIP).
     */
    private static final int HIT_TARGET_EDGE_IGNORE_DP_X = 30;
    private static final int HIT_TARGET_EDGE_IGNORE_DP_Y = 10;
    private static final int HIT_TARGET_MIN_SIZE_DP_X = HIT_TARGET_EDGE_IGNORE_DP_X * 3;
    private static final int HIT_TARGET_MIN_SIZE_DP_Y = HIT_TARGET_EDGE_IGNORE_DP_Y * 3;

    // True if the most recent DOWN event was a "hit".
    boolean mDownEventHit;

    /**
     * Called when a touch event is dispatched to a view. This allows listeners to
     * get a chance to respond before the target view.
     *
     * @return True if the listener has consumed the event, false otherwise.
     *         (In other words, we return true when the touch is *outside*
     *         the "smaller hit target", which will prevent the actual
     *         button from handling these events.)
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // if (DBG) log("SmallerHitTargetTouchListener: " + v + ", event " + event);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Note that event.getX() and event.getY() are already
            // translated into the View's coordinates.  (In other words,
            // "0,0" is a touch on the upper-left-most corner of the view.)
            final int touchX = (int) event.getX();
            final int touchY = (int) event.getY();

            final int viewWidth = v.getWidth();
            final int viewHeight = v.getHeight();

            final float pixelDensity = v.getResources().getDisplayMetrics().density;
            final int targetMinSizeX = (int) (HIT_TARGET_MIN_SIZE_DP_X * pixelDensity);
            final int targetMinSizeY = (int) (HIT_TARGET_MIN_SIZE_DP_Y * pixelDensity);

            int edgeIgnoreX = (int) (HIT_TARGET_EDGE_IGNORE_DP_X * pixelDensity);
            int edgeIgnoreY = (int) (HIT_TARGET_EDGE_IGNORE_DP_Y * pixelDensity);

            // If we are dealing with smaller buttons where the dead zone defined by
            // HIT_TARGET_EDGE_IGNORE_DP_[X|Y] is too large.
            if (viewWidth < targetMinSizeX || viewHeight < targetMinSizeY) {
                // This really should not happen given our two use cases (as of this writing)
                // in the call edge button and secondary calling card. However, we leave
                // this is as a precautionary measure.
                Log.w(TAG, "onTouch: view is too small for SmallerHitTargetTouchListener");
                edgeIgnoreX = 0;
                edgeIgnoreY = 0;
            }

            final int minTouchX = edgeIgnoreX;
            final int maxTouchX = viewWidth - edgeIgnoreX;
            final int minTouchY = edgeIgnoreY;
            final int maxTouchY = viewHeight - edgeIgnoreY;

            if (touchX < minTouchX || touchX > maxTouchX ||
                    touchY < minTouchY || touchY > maxTouchY) {
                // Missed!
                // if (DBG) log("  -> MISSED!");
                mDownEventHit = false;
                return true;  // Consume this event; don't let the button see it
            } else {
                // Hit!
                // if (DBG) log("  -> HIT!");
                mDownEventHit = true;
                return false;  // Let this event through to the actual button
            }
        } else {
            // This is a MOVE, UP or CANCEL event.
            //
            // We only do the "smaller hit target" check on DOWN events.
            // For the subsequent MOVE/UP/CANCEL events, we let them
            // through to the actual button IFF the previous DOWN event
            // got through to the actual button (i.e. it was a "hit".)
            return !mDownEventHit;
        }
    }
}
