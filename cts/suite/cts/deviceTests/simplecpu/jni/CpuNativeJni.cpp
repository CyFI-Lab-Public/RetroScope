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

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>

/* Code from now to qsort_local all copied from bionic source.
 * The code is duplicated here to remove dependency on optimized bionic
 */
static __inline char    *med3(char *, char *, char *, int (*)(const void *, const void *));
static __inline void     swapfunc(char *, char *, int, int);

#define min(a, b)   (a) < (b) ? a : b

/*
 * Qsort routine from Bentley & McIlroy's "Engineering a Sort Function".
 */
#define swapcode(TYPE, parmi, parmj, n) {       \
    long i = (n) / sizeof (TYPE);           \
    TYPE *pi = (TYPE *) (parmi);            \
    TYPE *pj = (TYPE *) (parmj);            \
    do {                        \
        TYPE    t = *pi;            \
        *pi++ = *pj;                \
        *pj++ = t;              \
        } while (--i > 0);              \
}

#define SWAPINIT(a, es) swaptype = ((char *)a - (char *)0) % sizeof(long) || \
    es % sizeof(long) ? 2 : es == sizeof(long)? 0 : 1;

static __inline void
swapfunc(char *a, char *b, int n, int swaptype)
{
    if (swaptype <= 1)
        swapcode(long, a, b, n)
    else
        swapcode(char, a, b, n)
}

#define swap(a, b)                  \
    if (swaptype == 0) {                \
        long t = *(long *)(a);          \
        *(long *)(a) = *(long *)(b);        \
        *(long *)(b) = t;           \
    } else                      \
        swapfunc(a, b, es, swaptype)

#define vecswap(a, b, n)    if ((n) > 0) swapfunc(a, b, n, swaptype)

static __inline char *
med3(char *a, char *b, char *c, int (*cmp)(const void *, const void *))
{
    return cmp(a, b) < 0 ?
           (cmp(b, c) < 0 ? b : (cmp(a, c) < 0 ? c : a ))
              :(cmp(b, c) > 0 ? b : (cmp(a, c) < 0 ? a : c ));
}

void
qsort_local(void *aa, size_t n, size_t es, int (*cmp)(const void *, const void *))
{
    char *pa, *pb, *pc, *pd, *pl, *pm, *pn;
    int d, r, swaptype, swap_cnt;
    char *a = (char*)aa;

loop:   SWAPINIT(a, es);
    swap_cnt = 0;
    if (n < 7) {
        for (pm = (char *)a + es; pm < (char *) a + n * es; pm += es)
            for (pl = pm; pl > (char *) a && cmp(pl - es, pl) > 0;
                 pl -= es)
                swap(pl, pl - es);
        return;
    }
    pm = (char *)a + (n / 2) * es;
    if (n > 7) {
        pl = (char *)a;
        pn = (char *)a + (n - 1) * es;
        if (n > 40) {
            d = (n / 8) * es;
            pl = med3(pl, pl + d, pl + 2 * d, cmp);
            pm = med3(pm - d, pm, pm + d, cmp);
            pn = med3(pn - 2 * d, pn - d, pn, cmp);
        }
        pm = med3(pl, pm, pn, cmp);
    }
    swap(a, pm);
    pa = pb = (char *)a + es;

    pc = pd = (char *)a + (n - 1) * es;
    for (;;) {
        while (pb <= pc && (r = cmp(pb, a)) <= 0) {
            if (r == 0) {
                swap_cnt = 1;
                swap(pa, pb);
                pa += es;
            }
            pb += es;
        }
        while (pb <= pc && (r = cmp(pc, a)) >= 0) {
            if (r == 0) {
                swap_cnt = 1;
                swap(pc, pd);
                pd -= es;
            }
            pc -= es;
        }
        if (pb > pc)
            break;
        swap(pb, pc);
        swap_cnt = 1;
        pb += es;
        pc -= es;
    }
    if (swap_cnt == 0) {  /* Switch to insertion sort */
        for (pm = (char *) a + es; pm < (char *) a + n * es; pm += es)
            for (pl = pm; pl > (char *) a && cmp(pl - es, pl) > 0;
                 pl -= es)
                swap(pl, pl - es);
        return;
    }

    pn = (char *)a + n * es;
    r = min(pa - (char *)a, pb - pa);
    vecswap(a, pb - r, r);
    r = min(pd - pc, pn - pd - (int)es);
    vecswap(pb, pn - r, r);
    if ((r = pb - pa) > (int)es)
        qsort_local(a, r / es, es, cmp);
    if ((r = pd - pc) > (int)es) {
        /* Iterate rather than recurse to save stack space */
        a = pn - r;
        n = r / es;
        goto loop;
    }
    /* qsort(pn - r, r / es, es, cmp); */
}

/* code duplication ends here */

/**
 * Util for getting time stamp
 */
double currentTimeMillis()
{
    struct timeval tv;
    gettimeofday(&tv, (struct timezone *) NULL);
    return tv.tv_sec * 1000.0 + tv.tv_usec / 1000.0;
}

/**
 * Initialize given array randomly for the given seed
 */
template <typename T> void randomInitArray(T* array, int len, unsigned int seed)
{
    srand(seed);
    for (int i = 0; i < len; i++) {
        array[i] = (T) rand();
    }
}

/**
 * comparison function for int, for qsort
 */
int cmpint(const void* p1, const void* p2)
{
    return *(int*)p1 - *(int*)p2;
}

extern "C" JNIEXPORT jdouble JNICALL Java_com_android_cts_simplecpu_CpuNative_runSort(JNIEnv* env,
        jclass clazz, jint numberElements, jint repetition)
{
    int* data = new int[numberElements];
    if (data == NULL) {
        env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"), "No memory");
        return -1;
    }
    double totalTime = 0;
    for (int i = 0; i < repetition; i++) {
        randomInitArray<int>(data, numberElements, 0);
        double start = currentTimeMillis();
        qsort_local(data, numberElements, sizeof(int), cmpint);
        double end = currentTimeMillis();
        totalTime += (end - start);
    }
    delete[] data;
    return totalTime;
}


/**
 * Do matrix multiplication, C = A x B with all matrices having dimension of n x n
 * The implementation is not in the most efficient, but it is good enough for benchmarking purpose.
 * @param n should be multiple of 8
 */
void doMatrixMultiplication(float* A, float* B, float* C, int n)
{
    // batch size
    const int M = 8;
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j += M) {
            float sum[M];
            for (int k = 0; k < M; k++) {
                sum[k] = 0;
            }
            // re-use the whole cache line for accessing B.
            // otherwise, the whole line will be read and only one value will be used.

            for (int k = 0; k < n; k++) {
                float a = A[i * n + k];
                sum[0] += a * B[k * n + j];
                sum[1] += a * B[k * n + j + 1];
                sum[2] += a * B[k * n + j + 2];
                sum[3] += a * B[k * n + j + 3];
                sum[4] += a * B[k * n + j + 4];
                sum[5] += a * B[k * n + j + 5];
                sum[6] += a * B[k * n + j + 6];
                sum[7] += a * B[k * n + j + 7];
            }
            for (int k = 0; k < M; k++) {
                C[i * n + j + k] = sum[k];
            }
        }
    }
}

extern "C" JNIEXPORT jdouble JNICALL Java_com_android_cts_simplecpu_CpuNative_runMatrixMultiplication(
        JNIEnv* env, jclass clazz, jint n, jint repetition)
{
    // C = A x B
    float* A = new float[n * n];
    float* B = new float[n * n];
    float* C = new float[n * n];
    if ((A == NULL) || (B == NULL) || (C == NULL)) {
        delete[] A;
        delete[] B;
        delete[] C;
        env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"), "No memory");
        return -1;
    }
    double totalTime = 0;
    for (int i = 0; i < repetition; i++) {
        randomInitArray<float>(A, n * n, 0);
        randomInitArray<float>(B, n * n, 1);
        double start = currentTimeMillis();
        doMatrixMultiplication(A, B, C, n);
        double end = currentTimeMillis();
        totalTime += (end - start);
    }
    delete[] A;
    delete[] B;
    delete[] C;
    return totalTime;
}

