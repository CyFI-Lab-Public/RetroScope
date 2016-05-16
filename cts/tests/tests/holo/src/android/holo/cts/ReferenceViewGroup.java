/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.holo.cts;

import com.android.cts.holo.R;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * {@link ViewGroup} that inflates to a reference width and height.
 */
public class ReferenceViewGroup extends ViewGroup {

    private final int mWidthDp;
    private final int mHeightDp;

    public ReferenceViewGroup(Context context) {
        this(context, null);
    }

    public ReferenceViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources resources = context.getResources();
        mWidthDp = resources.getDimensionPixelSize(R.dimen.reference_width);
        mHeightDp = resources.getDimensionPixelSize(R.dimen.reference_height);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        widthMeasureSpec = getMeasureSpec(LayoutParams.MATCH_PARENT, mWidthDp);
        heightMeasureSpec = getMeasureSpec(LayoutParams.MATCH_PARENT, mHeightDp);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            LayoutParams params = child.getLayoutParams();
            int width = getMeasureSpec(params.width, mWidthDp);
            int height = getMeasureSpec(params.height, mHeightDp);
            child.measure(width, height);
        }
    }

    private int getMeasureSpec(int value, int size) {
        if (value == LayoutParams.MATCH_PARENT) {
            return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        } else if (value == LayoutParams.WRAP_CONTENT) {
            return MeasureSpec.makeMeasureSpec(size, MeasureSpec.AT_MOST);
        } else {
            return value;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!changed) {
            return;
        }

        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
        }
    }
}
