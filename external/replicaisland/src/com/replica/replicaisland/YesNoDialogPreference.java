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
import android.preference.DialogPreference;
import android.util.AttributeSet;

public class YesNoDialogPreference extends DialogPreference {
	private YesNoDialogListener mListener;
	
	public abstract interface YesNoDialogListener {
		public abstract void onDialogClosed(boolean positiveResult);
	}
	
	public YesNoDialogPreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.yesNoPreferenceStyle);
		// TODO Auto-generated constructor stub
	}

	public YesNoDialogPreference(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}
	
	public YesNoDialogPreference(Context context) {
        this(context, null);
    }

	public void setListener(YesNoDialogListener listener) {
		mListener = listener;
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (mListener != null) {
			mListener.onDialogClosed(positiveResult);
		}
    }
}
