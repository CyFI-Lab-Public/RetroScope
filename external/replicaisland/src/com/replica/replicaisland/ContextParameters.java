/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.replica.replicaisland;

import android.content.Context;

/** Contains global (but typically constant) parameters about the current operating context */
public class ContextParameters extends BaseObject {
    public int viewWidth;
    public int viewHeight;
    public Context context;
	public int gameWidth;
	public int gameHeight;
	public float viewScaleX;
	public float viewScaleY;
	public boolean supportsDrawTexture;
	public boolean supportsVBOs;
	public int difficulty;
    
    public ContextParameters() {
        super();
    }
   
    @Override
    public void reset() {
        
    }
}
