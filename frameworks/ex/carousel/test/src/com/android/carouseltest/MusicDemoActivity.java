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

import com.android.ex.carousel.CarouselView;
import com.android.ex.carousel.CarouselViewHelper;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

public class MusicDemoActivity extends Activity {
    private static final String TAG = "MusicDemoActivity";
    private static final int CD_GEOMETRY = R.raw.book;
    private static final int VISIBLE_SLOTS = 7;
    private static final int CARD_SLOTS = 56;
    private static final int TOTAL_CARDS = 10000;
    private CarouselView mView;
    private int mImageResources[] = {
        R.drawable.emo_im_angel,
        R.drawable.emo_im_cool,
        R.drawable.emo_im_crying,
        R.drawable.emo_im_foot_in_mouth,
        R.drawable.emo_im_happy,
        R.drawable.emo_im_kissing,
        R.drawable.emo_im_laughing,
        R.drawable.emo_im_lips_are_sealed,
        R.drawable.emo_im_money_mouth,
        R.drawable.emo_im_sad,
        R.drawable.emo_im_surprised,
        R.drawable.emo_im_tongue_sticking_out,
        R.drawable.emo_im_undecided,
        R.drawable.emo_im_winking,
        R.drawable.emo_im_wtf,
        R.drawable.emo_im_yelling
    };

    private LocalCarouselViewHelper mHelper;

    class LocalCarouselViewHelper extends CarouselViewHelper {

        LocalCarouselViewHelper(Context context) {
            super(context);
        }

        @Override
        public void onCardSelected(int id) {
            Log.v(TAG, "Yay, item " + id + " was selected!");
        }

        @Override
        public Bitmap getTexture(int n) {
            return BitmapFactory.decodeResource(getResources(),
                    mImageResources[n % mImageResources.length]);
        }

        @Override
        public Bitmap getDetailTexture(int n) {
            return null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Resources res = getResources();
        setContentView(R.layout.music_demo);
        mView = (CarouselView) findViewById(R.id.carousel);
        mHelper = new LocalCarouselViewHelper(this);
        mHelper.setCarouselView(mView);
        mView.setSlotCount(CARD_SLOTS);
        mView.createCards(TOTAL_CARDS);
        mView.setVisibleSlots(VISIBLE_SLOTS);
        mView.setStartAngle((float) -(2.0f*Math.PI * 5 / CARD_SLOTS));
        mView.setDefaultBitmap(BitmapFactory.decodeResource(res, R.drawable.wait));
        mView.setLoadingBitmap(BitmapFactory.decodeResource(res, R.drawable.blank_album));
        mView.setBackgroundBitmap(BitmapFactory.decodeResource(res, R.drawable.background));
        mView.setDefaultGeometry(CD_GEOMETRY);
        mView.setFadeInDuration(250);
        mView.setRezInCardCount(3.0f);
        mView.setForceBlendCardsWithZ(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHelper.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHelper.onPause();
    }
}
