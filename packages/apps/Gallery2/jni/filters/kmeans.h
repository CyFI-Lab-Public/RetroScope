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

#ifndef KMEANS_H
#define KMEANS_H

#include <cstdlib>
#include <math.h>

// Helper functions

template <typename T, typename N>
inline void sum(T values[], int len, int dimension, int stride, N dst[]) {
    int x, y;
    // zero out dst vector
    for (x = 0; x < dimension; x++) {
        dst[x] = 0;
    }
    for (x = 0; x < len; x+= stride) {
        for (y = 0; y < dimension; y++) {
            dst[y] += values[x + y];
        }
    }
}

template <typename T, typename N>
inline void set(T val1[], N val2[], int dimension) {
    int x;
    for (x = 0; x < dimension; x++) {
        val1[x] = val2[x];
    }
}

template <typename T, typename N>
inline void add(T val[], N dst[], int dimension) {
    int x;
    for (x = 0; x < dimension; x++) {
        dst[x] += val[x];
    }
}

template <typename T, typename N>
inline void divide(T dst[], N divisor, int dimension) {
   int x;
   if (divisor == 0) {
       return;
   }
   for (x = 0; x < dimension; x++) {
       dst[x] /= divisor;
   }
}

/**
 * Calculates euclidean distance.
 */

template <typename T, typename N>
inline N euclideanDist(T val1[], T val2[], int dimension) {
    int x;
    N sum = 0;
    for (x = 0; x < dimension; x++) {
        N diff = (N) val1[x] - (N) val2[x];
        sum += diff * diff;
    }
    return sqrt(sum);
}

// K-Means


/**
 * Picks k random starting points from the data set.
 */
template <typename T>
void initialPickHeuristicRandom(int k, T values[], int len, int dimension, int stride, T dst[],
        unsigned int seed) {
    int x, z, num_vals, cntr;
    num_vals = len / stride;
    cntr = 0;
    srand(seed);
    unsigned int r_vals[k];
    unsigned int r;

    for (x = 0; x < k; x++) {

        // ensure randomly chosen value is unique
        int r_check = 0;
        while (r_check == 0) {
            r = (unsigned int) rand() % num_vals;
            r_check = 1;
            for (z = 0; z < x; z++) {
                if (r == r_vals[z]) {
                    r_check = 0;
                }
            }
        }
        r_vals[x] = r;
        r *= stride;

        // set dst to be randomly chosen value
        set<T,T>(dst + cntr, values + r, dimension);
        cntr += stride;
    }
}

/**
 * Finds index of closet centroid to a value
 */
template <typename T, typename N>
inline int findClosest(T values[], T oldCenters[], int dimension, int stride, int pop_size) {
    int best_ind = 0;
    N best_len = euclideanDist <T, N>(values, oldCenters, dimension);
    int y;
    for (y = stride; y < pop_size; y+=stride) {
        N l = euclideanDist <T, N>(values, oldCenters + y, dimension);
        if (l < best_len) {
            best_len = l;
            best_ind = y;
        }
    }
    return best_ind;
}

/**
 * Calculates new centroids by averaging value clusters for old centroids.
 */
template <typename T, typename N>
int calculateNewCentroids(int k, T values[], int len, int dimension, int stride, T oldCenters[],
        T dst[]) {
    int x, pop_size;
    pop_size = k * stride;
    int popularities[k];
    N tmp[pop_size];

    //zero popularities
    memset(popularities, 0, sizeof(int) * k);
    // zero dst, and tmp
    for (x = 0; x < pop_size; x++) {
        tmp[x] = 0;
    }

    // put summation for each k in tmp
    for (x = 0; x < len; x+=stride) {
        int best = findClosest<T, N>(values + x, oldCenters, dimension, stride, pop_size);
        add<T, N>(values + x, tmp + best, dimension);
        popularities[best / stride]++;

    }

    int ret = 0;
    int y;
    // divide to get centroid and set dst to result
    for (x = 0; x < pop_size; x+=stride) {
        divide<N, int>(tmp + x, popularities[x / stride], dimension);
        for (y = 0; y < dimension; y++) {
            if ((dst + x)[y] != (T) ((tmp + x)[y])) {
                ret = 1;
            }
        }
        set(dst + x, tmp + x, dimension);
    }
    return ret;
}

template <typename T, typename N>
void runKMeansWithPicks(int k, T finalCentroids[], T values[], int len, int dimension, int stride,
        int iterations, T initialPicks[]){
        int k_len = k * stride;
        int x;

        // zero newCenters
        for (x = 0; x < k_len; x++) {
            finalCentroids[x] = 0;
        }

        T * c1 = initialPicks;
        T * c2 = finalCentroids;
        T * temp;
        int ret = 1;
        for (x = 0; x < iterations; x++) {
            ret = calculateNewCentroids<T, N>(k, values, len, dimension, stride, c1, c2);
            temp = c1;
            c1 = c2;
            c2 = temp;
            if (ret == 0) {
                x = iterations;
            }
        }
        set<T, T>(finalCentroids, c1, dimension);
}

/**
 * Runs the k-means algorithm on dataset values with some initial centroids.
 */
template <typename T, typename N>
void runKMeans(int k, T finalCentroids[], T values[], int len, int dimension, int stride,
        int iterations, unsigned int seed){
    int k_len = k * stride;
    T initialPicks [k_len];
    initialPickHeuristicRandom<T>(k, values, len, dimension, stride, initialPicks, seed);

    runKMeansWithPicks<T, N>(k, finalCentroids, values, len, dimension, stride,
        iterations, initialPicks);
}

/**
 * Sets each value in values to the closest centroid.
 */
template <typename T, typename N>
void applyCentroids(int k, T centroids[], T values[], int len, int dimension, int stride) {
    int x, pop_size;
    pop_size = k * stride;
    for (x = 0; x < len; x+= stride) {
        int best = findClosest<T, N>(values + x, centroids, dimension, stride, pop_size);
        set<T, T>(values + x, centroids + best, dimension);
    }
}

#endif // KMEANS_H
