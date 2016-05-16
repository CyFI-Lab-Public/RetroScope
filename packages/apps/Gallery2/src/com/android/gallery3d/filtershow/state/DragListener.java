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

package com.android.gallery3d.filtershow.state;

import android.view.DragEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

class DragListener implements View.OnDragListener {

    private static final String LOGTAG = "DragListener";
    private PanelTrack mStatePanelTrack;
    private static float sSlope = 0.2f;

    public DragListener(PanelTrack statePanelTrack) {
        mStatePanelTrack = statePanelTrack;
    }

    private void setState(DragEvent event) {
        float translation = event.getY() - mStatePanelTrack.getTouchPoint().y;
        float alpha = 1.0f - (Math.abs(translation)
                / mStatePanelTrack.getCurrentView().getHeight());
        if (mStatePanelTrack.getOrientation() == LinearLayout.VERTICAL) {
            translation = event.getX() - mStatePanelTrack.getTouchPoint().x;
            alpha = 1.0f - (Math.abs(translation)
                    / mStatePanelTrack.getCurrentView().getWidth());
            mStatePanelTrack.getCurrentView().setTranslationX(translation);
        } else {
            mStatePanelTrack.getCurrentView().setTranslationY(translation);
        }
        mStatePanelTrack.getCurrentView().setBackgroundAlpha(alpha);
    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED: {
                break;
            }
            case DragEvent.ACTION_DRAG_LOCATION: {
                if (mStatePanelTrack.getCurrentView() != null) {
                    setState(event);
                    View over = mStatePanelTrack.findChildAt((int) event.getX(),
                                                             (int) event.getY());
                    if (over != null && over != mStatePanelTrack.getCurrentView()) {
                        StateView stateView = (StateView) over;
                        if (stateView != mStatePanelTrack.getCurrentView()) {
                            int pos = mStatePanelTrack.findChild(over);
                            int origin = mStatePanelTrack.findChild(
                                    mStatePanelTrack.getCurrentView());
                            ArrayAdapter array = (ArrayAdapter) mStatePanelTrack.getAdapter();
                            if (origin != -1 && pos != -1) {
                                State current = (State) array.getItem(origin);
                                array.remove(current);
                                array.insert(current, pos);
                                mStatePanelTrack.fillContent(false);
                                mStatePanelTrack.setCurrentView(mStatePanelTrack.getChildAt(pos));
                            }
                        }
                    }
                }
                break;
            }
            case DragEvent.ACTION_DRAG_ENTERED: {
                mStatePanelTrack.setExited(false);
                if (mStatePanelTrack.getCurrentView() != null) {
                    mStatePanelTrack.getCurrentView().setVisibility(View.VISIBLE);
                }
                return true;
            }
            case DragEvent.ACTION_DRAG_EXITED: {
                if (mStatePanelTrack.getCurrentView() != null) {
                    setState(event);
                    mStatePanelTrack.getCurrentView().setVisibility(View.INVISIBLE);
                }
                mStatePanelTrack.setExited(true);
                break;
            }
            case DragEvent.ACTION_DROP: {
                break;
            }
            case DragEvent.ACTION_DRAG_ENDED: {
                if (mStatePanelTrack.getCurrentView() != null
                        && mStatePanelTrack.getCurrentView().getAlpha() > sSlope) {
                    setState(event);
                }
                mStatePanelTrack.checkEndState();
                break;
            }
            default:
                break;
        }
        return true;
    }
}
