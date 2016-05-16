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

import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Adapter;

public interface PanelTrack {
    public int getOrientation();
    public void onTouch(MotionEvent event, StateView view);
    public StateView getCurrentView();
    public void setCurrentView(View view);
    public Point getTouchPoint();
    public View findChildAt(int x, int y);
    public int findChild(View view);
    public Adapter getAdapter();
    public void fillContent(boolean value);
    public View getChildAt(int pos);
    public void setExited(boolean value);
    public void checkEndState();
}
