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
#include "kmeans.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
 * For reasonable speeds:
 * k < 30
 * small_ds_bitmap width/height < 64 pixels.
 * large_ds_bitmap width/height < 512 pixels
 *
 * bad for high-frequency image noise
 */

void JNIFUNCF(ImageFilterKMeans, nativeApplyFilter, jobject bitmap, jint width, jint height,
        jobject large_ds_bitmap, jint lwidth, jint lheight, jobject small_ds_bitmap,
        jint swidth, jint sheight, jint p, jint seed)
{
    char* destination = 0;
    char* larger_ds_dst = 0;
    char* smaller_ds_dst = 0;
    AndroidBitmap_lockPixels(env, bitmap, (void**) &destination);
    AndroidBitmap_lockPixels(env, large_ds_bitmap, (void**) &larger_ds_dst);
    AndroidBitmap_lockPixels(env, small_ds_bitmap, (void**) &smaller_ds_dst);
    unsigned char * dst = (unsigned char *) destination;

    unsigned char * small_ds = (unsigned char *) smaller_ds_dst;
    unsigned char * large_ds = (unsigned char *) larger_ds_dst;

    // setting for small bitmap
    int len = swidth * sheight * 4;
    int dimension = 3;
    int stride = 4;
    int iterations = 20;
    int k = p;
    unsigned int s = seed;
    unsigned char finalCentroids[k * stride];

    // get initial picks from small downsampled image
    runKMeans<unsigned char, int>(k, finalCentroids, small_ds, len, dimension,
            stride, iterations, s);


    len = lwidth * lheight * 4;
    iterations = 8;
    unsigned char nextCentroids[k * stride];

    // run kmeans on large downsampled image
    runKMeansWithPicks<unsigned char, int>(k, nextCentroids, large_ds, len,
            dimension, stride, iterations, finalCentroids);

    len = width * height * 4;

    // apply to final image
    applyCentroids<unsigned char, int>(k, nextCentroids, dst, len, dimension, stride);

    AndroidBitmap_unlockPixels(env, small_ds_bitmap);
    AndroidBitmap_unlockPixels(env, large_ds_bitmap);
    AndroidBitmap_unlockPixels(env, bitmap);
}
#ifdef __cplusplus
}
#endif
