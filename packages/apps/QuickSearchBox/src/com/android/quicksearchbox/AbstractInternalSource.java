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
package com.android.quicksearchbox;

import com.android.quicksearchbox.util.NamedTaskExecutor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;

/**
 * Abstract implementation of a source that is not backed by a searchable activity.
 */
public abstract class AbstractInternalSource extends AbstractSource {

    public AbstractInternalSource(Context context, Handler uiThread, NamedTaskExecutor iconLoader) {
        super(context, uiThread, iconLoader);
    }

    @Override
    public String getSuggestUri() {
        return null;
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public String getDefaultIntentData() {
        return null;
    }

    @Override
    protected String getIconPackage() {
        return getContext().getPackageName();
    }

    @Override
    public int getQueryThreshold() {
        return 0;
    }

    @Override
    public Drawable getSourceIcon() {
        return getContext().getResources().getDrawable(getSourceIconResource());
    }

    @Override
    public Uri getSourceIconUri() {
        return Uri.parse("android.resource://" + getContext().getPackageName()
                + "/" +  getSourceIconResource());
    }

    protected abstract int getSourceIconResource();

    @Override
    public boolean queryAfterZeroResults() {
        return true;
    }

}
