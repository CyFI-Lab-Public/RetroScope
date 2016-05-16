/*
 * Copyright (C) 2008,2009  OMRON SOFTWARE Co., Ltd.
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

package jp.co.omronsoft.openwnn;

import android.widget.Button;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.content.Context;

/** 
 * The button for the candidate-view
 * @author Copyright (C) 2009, OMRON SOFTWARE CO., LTD.  All Rights Reserved.
 */
public class CandidateViewButton extends Button {

    /** The state of up */
    private int[] mUpState;

    /** Constructor */
    public CandidateViewButton(Context context) {
        super(context);
    }

    /** Constructor */
    public CandidateViewButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** @see android.view.View#onTouchEvent */
    public boolean onTouchEvent(MotionEvent me) {
        /* for changing the button on CandidateView when it is pressed. */

        boolean ret = super.onTouchEvent(me);
        Drawable d = getBackground();

        switch (me.getAction()) {
        case MotionEvent.ACTION_DOWN:
            mUpState = d.getState();
            d.setState(View.PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET);
            break;
        case MotionEvent.ACTION_UP:
        default:
            d.setState(mUpState);
            break;
        }

        return ret;
    }
}
