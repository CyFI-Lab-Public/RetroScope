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

package com.android.carouseltest;

import android.content.Context;
import android.util.AttributeSet;

import com.android.ex.carousel.CarouselController;
import com.android.ex.carousel.CarouselView;
import com.android.ex.carousel.CarouselView.Info;

public class MyCarouselView extends CarouselView {

    public MyCarouselView(Context context, CarouselController controller) {
        this(context, null, controller);
    }

    public MyCarouselView(Context context, AttributeSet attrs) {
        this(context, attrs, new CarouselController());
    }

    public MyCarouselView(Context context, AttributeSet attrs, CarouselController controller) {
        super(context, attrs, controller);
    }

    public Info getRenderScriptInfo() {
        return new Info(R.raw.carousel);
    }

    @Override
    public boolean interpretLongPressEvents() {
        return true;
    }

}
