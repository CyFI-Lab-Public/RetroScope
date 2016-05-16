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

#include "filters.h"

void JNIFUNCF(ImageFilter, nativeApplyGradientFilter, jobject bitmap, jint width, jint height,
        jintArray redGradient, jintArray greenGradient, jintArray blueGradient)
{
    char* destination = 0;
    jint* redGradientArray = 0;
    jint* greenGradientArray = 0;
    jint* blueGradientArray = 0;
    if (redGradient)
        redGradientArray = (*env)->GetIntArrayElements(env, redGradient, NULL);
    if (greenGradient)
        greenGradientArray = (*env)->GetIntArrayElements(env, greenGradient, NULL);
    if (blueGradient)
        blueGradientArray = (*env)->GetIntArrayElements(env, blueGradient, NULL);

    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);
    int i;
    int len = width * height * 4;
    for (i = 0; i < len; i+=4)
    {
        if (redGradient)
        {
            int r = destination[RED];
            r = redGradientArray[r];
            destination[RED] = r;
        }
        if (greenGradient)
        {
            int g = destination[GREEN];
            g = greenGradientArray[g];
            destination[GREEN] = g;
        }
        if (blueGradient)
        {
            int b = destination[BLUE];
            b = blueGradientArray[b];
            destination[BLUE] = b;
        }
    }
    if (redGradient)
        (*env)->ReleaseIntArrayElements(env, redGradient, redGradientArray, 0);
    if (greenGradient)
        (*env)->ReleaseIntArrayElements(env, greenGradient, greenGradientArray, 0);
    if (blueGradient)
        (*env)->ReleaseIntArrayElements(env, blueGradient, blueGradientArray, 0);
    AndroidBitmap_unlockPixels(env, bitmap);
}

