#include "rs_core.rsh"
#include "rs_graphics.rsh"
#include "rs_structs.h"

// Opaque Allocation type operations
extern uint32_t __attribute__((overloadable))
    rsAllocationGetDimX(rs_allocation a) {
    Allocation_t *alloc = (Allocation_t *)a.p;
    return alloc->mHal.drvState.lod[0].dimX;
}

extern uint32_t __attribute__((overloadable))
        rsAllocationGetDimY(rs_allocation a) {
    Allocation_t *alloc = (Allocation_t *)a.p;
    return alloc->mHal.drvState.lod[0].dimY;
}

extern uint32_t __attribute__((overloadable))
        rsAllocationGetDimZ(rs_allocation a) {
    Allocation_t *alloc = (Allocation_t *)a.p;
    return alloc->mHal.drvState.lod[0].dimZ;
}

extern uint32_t __attribute__((overloadable))
        rsAllocationGetDimLOD(rs_allocation a) {
    Allocation_t *alloc = (Allocation_t *)a.p;
    return alloc->mHal.state.hasMipmaps;
}

extern uint32_t __attribute__((overloadable))
        rsAllocationGetDimFaces(rs_allocation a) {
    Allocation_t *alloc = (Allocation_t *)a.p;
    return alloc->mHal.state.hasFaces;
}


extern rs_element __attribute__((overloadable))
        rsAllocationGetElement(rs_allocation a) {
    Allocation_t *alloc = (Allocation_t *)a.p;
    if (alloc == NULL) {
        rs_element nullElem = {0};
        return nullElem;
    }
    Type_t *type = (Type_t *)alloc->mHal.state.type;
    rs_element returnElem = {type->mHal.state.element};
    return returnElem;
}

// TODO: this needs to be optimized, obviously
static void memcpy(void* dst, void* src, size_t size) {
    char* dst_c = (char*) dst, *src_c = (char*) src;
    for (; size > 0; size--) {
        *dst_c++ = *src_c++;
    }
}

#ifdef RS_DEBUG_RUNTIME
#define ELEMENT_AT(T)                                                   \
    extern void __attribute__((overloadable))                           \
        rsSetElementAt_##T(rs_allocation a, const T *val, uint32_t x);  \
    extern void __attribute__((overloadable))                           \
        rsSetElementAt_##T(rs_allocation a, const T *val, uint32_t x, uint32_t y); \
    extern void __attribute__((overloadable))                           \
        rsSetElementAt_##T(rs_allocation a, const T *val, uint32_t x, uint32_t y, uint32_t z); \
    extern void __attribute__((overloadable))                           \
        rsGetElementAt_##T(rs_allocation a, T *val, uint32_t x);  \
    extern void __attribute__((overloadable))                           \
        rsGetElementAt_##T(rs_allocation a, T *val, uint32_t x, uint32_t y); \
    extern void __attribute__((overloadable))                           \
        rsGetElementAt_##T(rs_allocation a, T *val, uint32_t x, uint32_t y, uint32_t z); \
                                                                        \
    extern void __attribute__((overloadable))                           \
    rsSetElementAt_##T(rs_allocation a, T val, uint32_t x) {            \
        rsSetElementAt_##T(a, &val, x);                                 \
    }                                                                   \
    extern void __attribute__((overloadable))                           \
    rsSetElementAt_##T(rs_allocation a, T val, uint32_t x, uint32_t y) { \
        rsSetElementAt_##T(a, &val, x, y);                              \
    }                                                                   \
    extern void __attribute__((overloadable))                           \
    rsSetElementAt_##T(rs_allocation a, T val, uint32_t x, uint32_t y, uint32_t z) { \
        rsSetElementAt_##T(a, &val, x, y, z);                           \
    }                                                                   \
    extern T __attribute__((overloadable))                              \
    rsGetElementAt_##T(rs_allocation a, uint32_t x) {                   \
        T tmp;                                                          \
        rsGetElementAt_##T(a, &tmp, x);                                 \
        return tmp;                                                     \
    }                                                                   \
    extern T __attribute__((overloadable))                              \
    rsGetElementAt_##T(rs_allocation a, uint32_t x, uint32_t y) {       \
        T tmp;                                                          \
        rsGetElementAt_##T(a, &tmp, x, y);                              \
        return tmp;                                                     \
    }                                                                   \
    extern T __attribute__((overloadable))                              \
    rsGetElementAt_##T(rs_allocation a, uint32_t x, uint32_t y, uint32_t z) { \
        T tmp;                                                          \
        rsGetElementAt_##T(a, &tmp, x, y, z);                           \
        return tmp;                                                     \
    }

#else

uint8_t*
rsOffset(rs_allocation a, uint32_t sizeOf, uint32_t x, uint32_t y,
         uint32_t z) {
    Allocation_t *alloc = (Allocation_t *)a.p;
    uint8_t *p = (uint8_t *)alloc->mHal.drvState.lod[0].mallocPtr;
    const uint32_t stride = alloc->mHal.drvState.lod[0].stride;
    const uint32_t dimY = alloc->mHal.drvState.lod[0].dimY;
    uint8_t *dp = &p[(sizeOf * x) + (y * stride) +
                     (z * stride * dimY)];
    return dp;
}

#define ELEMENT_AT(T)                                                   \
                                                                        \
    void                                                                \
    rsSetElementAtImpl_##T(rs_allocation a, T val, uint32_t x,          \
                           uint32_t y, uint32_t z);                     \
                                                                        \
    extern void __attribute__((overloadable))                           \
    rsSetElementAt_##T(rs_allocation a, T val, uint32_t x) {            \
        rsSetElementAtImpl_##T(a, val, x, 0, 0);                        \
    }                                                                   \
                                                                        \
    extern void __attribute__((overloadable))                           \
    rsSetElementAt_##T(rs_allocation a, T val, uint32_t x,              \
                       uint32_t y) {                                    \
        rsSetElementAtImpl_##T(a, val, x, y, 0);                        \
    }                                                                   \
                                                                        \
    extern void __attribute__((overloadable))                           \
    rsSetElementAt_##T(rs_allocation a, T val, uint32_t x, uint32_t y,  \
                       uint32_t z) {                                    \
        rsSetElementAtImpl_##T(a, val, x, y, z);                        \
    }                                                                   \
                                                                        \
    T                                                                   \
    rsGetElementAtImpl_##T(rs_allocation a, uint32_t x, uint32_t y,     \
                       uint32_t z);                                     \
                                                                        \
    extern T __attribute__((overloadable))                              \
    rsGetElementAt_##T(rs_allocation a, uint32_t x) {                   \
        return rsGetElementAtImpl_##T(a, x, 0, 0);                      \
    }                                                                   \
                                                                        \
    extern T __attribute__((overloadable))                              \
    rsGetElementAt_##T(rs_allocation a, uint32_t x, uint32_t y) {       \
        return rsGetElementAtImpl_##T(a, x, y, 0);                      \
    }                                                                   \
                                                                        \
    extern T __attribute__((overloadable))                              \
    rsGetElementAt_##T(rs_allocation a, uint32_t x, uint32_t y,         \
                       uint32_t z) {                                    \
        return rsGetElementAtImpl_##T(a, x, y, z);                      \
    }



extern const void * __attribute__((overloadable))
        rsGetElementAt(rs_allocation a, uint32_t x) {
    Allocation_t *alloc = (Allocation_t *)a.p;
    const uint8_t *p = (const uint8_t *)alloc->mHal.drvState.lod[0].mallocPtr;
    const uint32_t eSize = alloc->mHal.state.elementSizeBytes;
    return &p[eSize * x];
}

extern const void * __attribute__((overloadable))
        rsGetElementAt(rs_allocation a, uint32_t x, uint32_t y) {
    Allocation_t *alloc = (Allocation_t *)a.p;
    const uint8_t *p = (const uint8_t *)alloc->mHal.drvState.lod[0].mallocPtr;
    const uint32_t eSize = alloc->mHal.state.elementSizeBytes;
    const uint32_t stride = alloc->mHal.drvState.lod[0].stride;
    return &p[(eSize * x) + (y * stride)];
}

extern const void * __attribute__((overloadable))
        rsGetElementAt(rs_allocation a, uint32_t x, uint32_t y, uint32_t z) {
    Allocation_t *alloc = (Allocation_t *)a.p;
    const uint8_t *p = (const uint8_t *)alloc->mHal.drvState.lod[0].mallocPtr;
    const uint32_t eSize = alloc->mHal.state.elementSizeBytes;
    const uint32_t stride = alloc->mHal.drvState.lod[0].stride;
    const uint32_t dimY = alloc->mHal.drvState.lod[0].dimY;
    return &p[(eSize * x) + (y * stride) + (z * stride * dimY)];
}
extern void __attribute__((overloadable))
        rsSetElementAt(rs_allocation a, void* ptr, uint32_t x) {
    Allocation_t *alloc = (Allocation_t *)a.p;
    const uint8_t *p = (const uint8_t *)alloc->mHal.drvState.lod[0].mallocPtr;
    const uint32_t eSize = alloc->mHal.state.elementSizeBytes;
    memcpy((void*)&p[eSize * x], ptr, eSize);
}

extern void __attribute__((overloadable))
        rsSetElementAt(rs_allocation a, void* ptr, uint32_t x, uint32_t y) {
    Allocation_t *alloc = (Allocation_t *)a.p;
    const uint8_t *p = (const uint8_t *)alloc->mHal.drvState.lod[0].mallocPtr;
    const uint32_t eSize = alloc->mHal.state.elementSizeBytes;
    const uint32_t stride = alloc->mHal.drvState.lod[0].stride;
    memcpy((void*)&p[(eSize * x) + (y * stride)], ptr, eSize);
}

extern void __attribute__((overloadable))
        rsSetElementAt(rs_allocation a, void* ptr, uint32_t x, uint32_t y, uint32_t z) {
    Allocation_t *alloc = (Allocation_t *)a.p;
    const uint8_t *p = (const uint8_t *)alloc->mHal.drvState.lod[0].mallocPtr;
    const uint32_t eSize = alloc->mHal.state.elementSizeBytes;
    const uint32_t stride = alloc->mHal.drvState.lod[0].stride;
    const uint32_t dimY = alloc->mHal.drvState.lod[0].dimY;
    memcpy((void*)&p[(eSize * x) + (y * stride) + (z * stride * dimY)], ptr, eSize);
}
#endif

ELEMENT_AT(char)
ELEMENT_AT(char2)
ELEMENT_AT(char3)
ELEMENT_AT(char4)
ELEMENT_AT(uchar)
ELEMENT_AT(uchar2)
ELEMENT_AT(uchar3)
ELEMENT_AT(uchar4)
ELEMENT_AT(short)
ELEMENT_AT(short2)
ELEMENT_AT(short3)
ELEMENT_AT(short4)
ELEMENT_AT(ushort)
ELEMENT_AT(ushort2)
ELEMENT_AT(ushort3)
ELEMENT_AT(ushort4)
ELEMENT_AT(int)
ELEMENT_AT(int2)
ELEMENT_AT(int3)
ELEMENT_AT(int4)
ELEMENT_AT(uint)
ELEMENT_AT(uint2)
ELEMENT_AT(uint3)
ELEMENT_AT(uint4)
ELEMENT_AT(long)
ELEMENT_AT(long2)
ELEMENT_AT(long3)
ELEMENT_AT(long4)
ELEMENT_AT(ulong)
ELEMENT_AT(ulong2)
ELEMENT_AT(ulong3)
ELEMENT_AT(ulong4)
ELEMENT_AT(float)
ELEMENT_AT(float2)
ELEMENT_AT(float3)
ELEMENT_AT(float4)
ELEMENT_AT(double)
ELEMENT_AT(double2)
ELEMENT_AT(double3)
ELEMENT_AT(double4)

#undef ELEMENT_AT


extern const uchar __attribute__((overloadable))
        rsGetElementAtYuv_uchar_Y(rs_allocation a, uint32_t x, uint32_t y) {
    return rsGetElementAt_uchar(a, x, y);
}

extern const uchar __attribute__((overloadable))
        rsGetElementAtYuv_uchar_U(rs_allocation a, uint32_t x, uint32_t y) {

    Allocation_t *alloc = (Allocation_t *)a.p;

    const size_t cstep = alloc->mHal.drvState.yuv.step;
    const size_t shift = alloc->mHal.drvState.yuv.shift;
    const size_t stride = alloc->mHal.drvState.lod[1].stride;

    const uchar *pin = (const uchar *)alloc->mHal.drvState.lod[1].mallocPtr;

    return pin[((x >> shift) * cstep) + ((y >> shift) * stride)];
}

extern const uchar __attribute__((overloadable))
        rsGetElementAtYuv_uchar_V(rs_allocation a, uint32_t x, uint32_t y) {

    Allocation_t *alloc = (Allocation_t *)a.p;

    const size_t cstep = alloc->mHal.drvState.yuv.step;
    const size_t shift = alloc->mHal.drvState.yuv.shift;
    const size_t stride = alloc->mHal.drvState.lod[2].stride;

    const uchar *pin = (const uchar *)alloc->mHal.drvState.lod[2].mallocPtr;

    return pin[((x >> shift) * cstep) + ((y >> shift) * stride)];
}

