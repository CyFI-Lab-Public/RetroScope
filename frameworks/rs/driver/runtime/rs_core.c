#include "rs_core.rsh"
#include "rs_graphics.rsh"
#include "rs_structs.h"

/* Function declarations from libRS */
extern float4 __attribute__((overloadable)) convert_float4(uchar4 c);

/* Implementation of Core Runtime */

extern float4 rsUnpackColor8888(uchar4 c)
{
    return convert_float4(c) * 0.003921569f;
}


extern int32_t __attribute__((overloadable)) rsAtomicCas(volatile int32_t *ptr, int32_t expectedValue, int32_t newValue) {
    return __sync_val_compare_and_swap(ptr, expectedValue, newValue);
}

extern uint32_t __attribute__((overloadable)) rsAtomicCas(volatile uint32_t *ptr, uint32_t expectedValue, uint32_t newValue) {
    return __sync_val_compare_and_swap((volatile int32_t *)ptr, (int32_t)expectedValue, (int32_t)newValue);
}

extern int32_t __attribute__((overloadable)) rsAtomicInc(volatile int32_t *ptr) {
    return __sync_fetch_and_add(ptr, 1);
}

extern int32_t __attribute__((overloadable)) rsAtomicDec(volatile int32_t *ptr) {
    return __sync_fetch_and_sub(ptr, 1);
}

extern int32_t __attribute__((overloadable)) rsAtomicAdd(volatile int32_t *ptr, int32_t value) {
    return __sync_fetch_and_add(ptr, value);
}

extern int32_t __attribute__((overloadable)) rsAtomicSub(volatile int32_t *ptr, int32_t value) {
    return __sync_fetch_and_sub(ptr, value);
}

extern int32_t __attribute__((overloadable)) rsAtomicAnd(volatile int32_t *ptr, int32_t value) {
    return __sync_fetch_and_and(ptr, value);
}

extern int32_t __attribute__((overloadable)) rsAtomicOr(volatile int32_t *ptr, int32_t value) {
    return __sync_fetch_and_or(ptr, value);
}

extern int32_t __attribute__((overloadable)) rsAtomicXor(volatile int32_t *ptr, int32_t value) {
    return __sync_fetch_and_xor(ptr, value);
}

extern uint32_t __attribute__((overloadable)) min(uint32_t, uint32_t);
extern int32_t __attribute__((overloadable)) min(int32_t, int32_t);
extern uint32_t __attribute__((overloadable)) max(uint32_t, uint32_t);
extern int32_t __attribute__((overloadable)) max(int32_t, int32_t);

extern uint32_t __attribute__((overloadable)) rsAtomicMin(volatile uint32_t *ptr, uint32_t value) {
    uint32_t prev, status;
    do {
        prev = *ptr;
        uint32_t n = min(value, prev);
        status = rsAtomicCas((volatile int32_t*) ptr, (int32_t) prev, (int32_t)n);
    } while (status != prev);
    return prev;
}

extern int32_t __attribute__((overloadable)) rsAtomicMin(volatile int32_t *ptr, int32_t value) {
    int32_t prev, status;
    do {
        prev = *ptr;
        int32_t n = min(value, prev);
        status = rsAtomicCas(ptr, prev, n);
    } while (status != prev);
    return prev;
}

extern uint32_t __attribute__((overloadable)) rsAtomicMax(volatile uint32_t *ptr, uint32_t value) {
    uint32_t prev, status;
    do {
        prev = *ptr;
        uint32_t n = max(value, prev);
        status = rsAtomicCas((volatile int32_t*) ptr, (int32_t) prev, (int32_t) n);
    } while (status != prev);
    return prev;
}

extern int32_t __attribute__((overloadable)) rsAtomicMax(volatile int32_t *ptr, int32_t value) {
    int32_t prev, status;
    do {
        prev = *ptr;
        int32_t n = max(value, prev);
        status = rsAtomicCas(ptr, prev, n);
    } while (status != prev);
    return prev;
}



extern int32_t rand();
#define RAND_MAX 0x7fffffff



extern float __attribute__((overloadable)) rsRand(float min, float max);/* {
    float r = (float)rand();
    r /= RAND_MAX;
    r = r * (max - min) + min;
    return r;
}
*/

extern float __attribute__((overloadable)) rsRand(float max) {
    return rsRand(0.f, max);
    //float r = (float)rand();
    //r *= max;
    //r /= RAND_MAX;
    //return r;
}

extern int __attribute__((overloadable)) rsRand(int max) {
    return (int)rsRand((float)max);
}

extern int __attribute__((overloadable)) rsRand(int min, int max) {
    return (int)rsRand((float)min, (float)max);
}

#define PRIM_DEBUG(T)                               \
extern void __attribute__((overloadable)) rsDebug(const char *, const T *);     \
void __attribute__((overloadable)) rsDebug(const char *txt, T val) {            \
    rsDebug(txt, &val);                                                         \
}

PRIM_DEBUG(char2)
PRIM_DEBUG(char3)
PRIM_DEBUG(char4)
PRIM_DEBUG(uchar2)
PRIM_DEBUG(uchar3)
PRIM_DEBUG(uchar4)
PRIM_DEBUG(short2)
PRIM_DEBUG(short3)
PRIM_DEBUG(short4)
PRIM_DEBUG(ushort2)
PRIM_DEBUG(ushort3)
PRIM_DEBUG(ushort4)
PRIM_DEBUG(int2)
PRIM_DEBUG(int3)
PRIM_DEBUG(int4)
PRIM_DEBUG(uint2)
PRIM_DEBUG(uint3)
PRIM_DEBUG(uint4)
PRIM_DEBUG(long2)
PRIM_DEBUG(long3)
PRIM_DEBUG(long4)
PRIM_DEBUG(ulong2)
PRIM_DEBUG(ulong3)
PRIM_DEBUG(ulong4)
PRIM_DEBUG(float2)
PRIM_DEBUG(float3)
PRIM_DEBUG(float4)
PRIM_DEBUG(double2)
PRIM_DEBUG(double3)
PRIM_DEBUG(double4)

#undef PRIM_DEBUG

