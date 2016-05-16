/*
 * Copyright (C) 2011-2012 The Android Open Source Project
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

#include "rsContext.h"
#include "rsScriptC.h"
#include "rsMatrix4x4.h"
#include "rsMatrix3x3.h"
#include "rsMatrix2x2.h"
#include "rsRuntime.h"

#include "rsdCore.h"
#include "rsdBcc.h"

#include "rsdPath.h"
#include "rsdAllocation.h"
#include "rsdShaderCache.h"
#include "rsdVertexArray.h"

#include <time.h>

using namespace android;
using namespace android::renderscript;

typedef float float2 __attribute__((ext_vector_type(2)));
typedef float float3 __attribute__((ext_vector_type(3)));
typedef float float4 __attribute__((ext_vector_type(4)));
typedef double double2 __attribute__((ext_vector_type(2)));
typedef double double3 __attribute__((ext_vector_type(3)));
typedef double double4 __attribute__((ext_vector_type(4)));
typedef char char2 __attribute__((ext_vector_type(2)));
typedef char char3 __attribute__((ext_vector_type(3)));
typedef char char4 __attribute__((ext_vector_type(4)));
typedef unsigned char uchar2 __attribute__((ext_vector_type(2)));
typedef unsigned char uchar3 __attribute__((ext_vector_type(3)));
typedef unsigned char uchar4 __attribute__((ext_vector_type(4)));
typedef short short2 __attribute__((ext_vector_type(2)));
typedef short short3 __attribute__((ext_vector_type(3)));
typedef short short4 __attribute__((ext_vector_type(4)));
typedef unsigned short ushort2 __attribute__((ext_vector_type(2)));
typedef unsigned short ushort3 __attribute__((ext_vector_type(3)));
typedef unsigned short ushort4 __attribute__((ext_vector_type(4)));
typedef int32_t int2 __attribute__((ext_vector_type(2)));
typedef int32_t int3 __attribute__((ext_vector_type(3)));
typedef int32_t int4 __attribute__((ext_vector_type(4)));
typedef uint32_t uint2 __attribute__((ext_vector_type(2)));
typedef uint32_t uint3 __attribute__((ext_vector_type(3)));
typedef uint32_t uint4 __attribute__((ext_vector_type(4)));
typedef long long long2 __attribute__((ext_vector_type(2)));
typedef long long long3 __attribute__((ext_vector_type(3)));
typedef long long long4 __attribute__((ext_vector_type(4)));
typedef unsigned long long ulong2 __attribute__((ext_vector_type(2)));
typedef unsigned long long ulong3 __attribute__((ext_vector_type(3)));
typedef unsigned long long ulong4 __attribute__((ext_vector_type(4)));

typedef uint8_t uchar;
typedef uint16_t ushort;
typedef uint32_t uint;
#ifndef RS_SERVER
typedef uint64_t ulong;
#endif

#ifdef RS_COMPATIBILITY_LIB
#define OPAQUETYPE(t) \
    typedef struct { const int* const p; } __attribute__((packed, aligned(4))) t;

OPAQUETYPE(rs_element)
OPAQUETYPE(rs_type)
OPAQUETYPE(rs_allocation)
OPAQUETYPE(rs_sampler)
OPAQUETYPE(rs_script)
OPAQUETYPE(rs_script_call)
#undef OPAQUETYPE

typedef struct {
    int tm_sec;     ///< seconds
    int tm_min;     ///< minutes
    int tm_hour;    ///< hours
    int tm_mday;    ///< day of the month
    int tm_mon;     ///< month
    int tm_year;    ///< year
    int tm_wday;    ///< day of the week
    int tm_yday;    ///< day of the year
    int tm_isdst;   ///< daylight savings time
} rs_tm;
#endif

//////////////////////////////////////////////////////////////////////////////
// Allocation
//////////////////////////////////////////////////////////////////////////////


static void SC_AllocationSyncAll2(Allocation *a, RsAllocationUsageType source) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrAllocationSyncAll(rsc, a, source);
}

static void SC_AllocationSyncAll(Allocation *a) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrAllocationSyncAll(rsc, a, RS_ALLOCATION_USAGE_SCRIPT);
}

static void SC_AllocationCopy1DRange(Allocation *dstAlloc,
                                     uint32_t dstOff,
                                     uint32_t dstMip,
                                     uint32_t count,
                                     Allocation *srcAlloc,
                                     uint32_t srcOff, uint32_t srcMip) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrAllocationCopy1DRange(rsc, dstAlloc, dstOff, dstMip, count,
                             srcAlloc, srcOff, srcMip);
}

static void SC_AllocationCopy2DRange(Allocation *dstAlloc,
                                     uint32_t dstXoff, uint32_t dstYoff,
                                     uint32_t dstMip, uint32_t dstFace,
                                     uint32_t width, uint32_t height,
                                     Allocation *srcAlloc,
                                     uint32_t srcXoff, uint32_t srcYoff,
                                     uint32_t srcMip, uint32_t srcFace) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrAllocationCopy2DRange(rsc, dstAlloc,
                             dstXoff, dstYoff, dstMip, dstFace,
                             width, height,
                             srcAlloc,
                             srcXoff, srcYoff, srcMip, srcFace);
}

#ifndef RS_COMPATIBILITY_LIB
static void SC_AllocationIoSend(Allocation *alloc) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrAllocationIoSend(rsc, alloc);
}


static void SC_AllocationIoReceive(Allocation *alloc) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrAllocationIoReceive(rsc, alloc);
}



//////////////////////////////////////////////////////////////////////////////
// Context
//////////////////////////////////////////////////////////////////////////////

static void SC_BindTexture(ProgramFragment *pf, uint32_t slot, Allocation *a) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrBindTexture(rsc, pf, slot, a);
}

static void SC_BindVertexConstant(ProgramVertex *pv, uint32_t slot, Allocation *a) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrBindConstant(rsc, pv, slot, a);
}

static void SC_BindFragmentConstant(ProgramFragment *pf, uint32_t slot, Allocation *a) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrBindConstant(rsc, pf, slot, a);
}

static void SC_BindSampler(ProgramFragment *pf, uint32_t slot, Sampler *s) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrBindSampler(rsc, pf, slot, s);
}

static void SC_BindProgramStore(ProgramStore *ps) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrBindProgramStore(rsc, ps);
}

static void SC_BindProgramFragment(ProgramFragment *pf) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrBindProgramFragment(rsc, pf);
}

static void SC_BindProgramVertex(ProgramVertex *pv) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrBindProgramVertex(rsc, pv);
}

static void SC_BindProgramRaster(ProgramRaster *pr) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrBindProgramRaster(rsc, pr);
}

static void SC_BindFrameBufferObjectColorTarget(Allocation *a, uint32_t slot) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrBindFrameBufferObjectColorTarget(rsc, a, slot);
}

static void SC_BindFrameBufferObjectDepthTarget(Allocation *a) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrBindFrameBufferObjectDepthTarget(rsc, a);
}

static void SC_ClearFrameBufferObjectColorTarget(uint32_t slot) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrClearFrameBufferObjectColorTarget(rsc, slot);
}

static void SC_ClearFrameBufferObjectDepthTarget(Context *, Script *) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrClearFrameBufferObjectDepthTarget(rsc);
}

static void SC_ClearFrameBufferObjectTargets(Context *, Script *) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrClearFrameBufferObjectTargets(rsc);
}


//////////////////////////////////////////////////////////////////////////////
// VP
//////////////////////////////////////////////////////////////////////////////

static void SC_VpLoadProjectionMatrix(const rsc_Matrix *m) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrVpLoadProjectionMatrix(rsc, m);
}

static void SC_VpLoadModelMatrix(const rsc_Matrix *m) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrVpLoadModelMatrix(rsc, m);
}

static void SC_VpLoadTextureMatrix(const rsc_Matrix *m) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrVpLoadTextureMatrix(rsc, m);
}

static void SC_PfConstantColor(ProgramFragment *pf, float r, float g, float b, float a) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrPfConstantColor(rsc, pf, r, g, b, a);
}

static void SC_VpGetProjectionMatrix(rsc_Matrix *m) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrVpGetProjectionMatrix(rsc, m);
}


//////////////////////////////////////////////////////////////////////////////
// Drawing
//////////////////////////////////////////////////////////////////////////////

static void SC_DrawQuadTexCoords(float x1, float y1, float z1, float u1, float v1,
                                 float x2, float y2, float z2, float u2, float v2,
                                 float x3, float y3, float z3, float u3, float v3,
                                 float x4, float y4, float z4, float u4, float v4) {
    Context *rsc = RsdCpuReference::getTlsContext();

    if (!rsc->setupCheck()) {
        return;
    }

    RsdHal *dc = (RsdHal *)rsc->mHal.drv;
    if (!dc->gl.shaderCache->setup(rsc)) {
        return;
    }

    //ALOGE("Quad");
    //ALOGE("%4.2f, %4.2f, %4.2f", x1, y1, z1);
    //ALOGE("%4.2f, %4.2f, %4.2f", x2, y2, z2);
    //ALOGE("%4.2f, %4.2f, %4.2f", x3, y3, z3);
    //ALOGE("%4.2f, %4.2f, %4.2f", x4, y4, z4);

    float vtx[] = {x1,y1,z1, x2,y2,z2, x3,y3,z3, x4,y4,z4};
    const float tex[] = {u1,v1, u2,v2, u3,v3, u4,v4};

    RsdVertexArray::Attrib attribs[2];
    attribs[0].set(GL_FLOAT, 3, 12, false, (uint32_t)vtx, "ATTRIB_position");
    attribs[1].set(GL_FLOAT, 2, 8, false, (uint32_t)tex, "ATTRIB_texture0");

    RsdVertexArray va(attribs, 2);
    va.setup(rsc);

    RSD_CALL_GL(glDrawArrays, GL_TRIANGLE_FAN, 0, 4);
}

static void SC_DrawQuad(float x1, float y1, float z1,
                        float x2, float y2, float z2,
                        float x3, float y3, float z3,
                        float x4, float y4, float z4) {
    SC_DrawQuadTexCoords(x1, y1, z1, 0, 1,
                         x2, y2, z2, 1, 1,
                         x3, y3, z3, 1, 0,
                         x4, y4, z4, 0, 0);
}

static void SC_DrawSpriteScreenspace(float x, float y, float z, float w, float h) {
    Context *rsc = RsdCpuReference::getTlsContext();

    ObjectBaseRef<const ProgramVertex> tmp(rsc->getProgramVertex());
    rsc->setProgramVertex(rsc->getDefaultProgramVertex());
    //rsc->setupCheck();

    //GLint crop[4] = {0, h, w, -h};

    float sh = rsc->getHeight();

    SC_DrawQuad(x,   sh - y,     z,
                x+w, sh - y,     z,
                x+w, sh - (y+h), z,
                x,   sh - (y+h), z);
    rsc->setProgramVertex((ProgramVertex *)tmp.get());
}

static void SC_DrawRect(float x1, float y1, float x2, float y2, float z) {
    SC_DrawQuad(x1, y2, z, x2, y2, z, x2, y1, z, x1, y1, z);
}

static void SC_DrawPath(Path *p) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsdPathDraw(rsc, p);
}

static void SC_DrawMesh(Mesh *m) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrDrawMesh(rsc, m);
}

static void SC_DrawMeshPrimitive(Mesh *m, uint32_t primIndex) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrDrawMeshPrimitive(rsc, m, primIndex);
}

static void SC_DrawMeshPrimitiveRange(Mesh *m, uint32_t primIndex, uint32_t start, uint32_t len) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrDrawMeshPrimitiveRange(rsc, m, primIndex, start, len);
}

static void SC_MeshComputeBoundingBox(Mesh *m,
                               float *minX, float *minY, float *minZ,
                               float *maxX, float *maxY, float *maxZ) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrMeshComputeBoundingBox(rsc, m, minX, minY, minZ, maxX, maxY, maxZ);
}



//////////////////////////////////////////////////////////////////////////////
//
//////////////////////////////////////////////////////////////////////////////


static void SC_Color(float r, float g, float b, float a) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrColor(rsc, r, g, b, a);
}

static void SC_Finish() {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsdGLFinish(rsc);
}

static void SC_ClearColor(float r, float g, float b, float a) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrPrepareClear(rsc);
    rsdGLClearColor(rsc, r, g, b, a);
}

static void SC_ClearDepth(float v) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrPrepareClear(rsc);
    rsdGLClearDepth(rsc, v);
}

static uint32_t SC_GetWidth() {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrGetWidth(rsc);
}

static uint32_t SC_GetHeight() {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrGetHeight(rsc);
}

static void SC_DrawTextAlloc(Allocation *a, int x, int y) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrDrawTextAlloc(rsc, a, x, y);
}

static void SC_DrawText(const char *text, int x, int y) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrDrawText(rsc, text, x, y);
}

static void SC_MeasureTextAlloc(Allocation *a,
                         int32_t *left, int32_t *right, int32_t *top, int32_t *bottom) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrMeasureTextAlloc(rsc, a, left, right, top, bottom);
}

static void SC_MeasureText(const char *text,
                    int32_t *left, int32_t *right, int32_t *top, int32_t *bottom) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrMeasureText(rsc, text, left, right, top, bottom);
}

static void SC_BindFont(Font *f) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrBindFont(rsc, f);
}

static void SC_FontColor(float r, float g, float b, float a) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrFontColor(rsc, r, g, b, a);
}
#endif


//////////////////////////////////////////////////////////////////////////////
//
//////////////////////////////////////////////////////////////////////////////

static void SC_SetObject(ObjectBase **dst, ObjectBase * src) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrSetObject(rsc, dst, src);
}

static void SC_ClearObject(ObjectBase **dst) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrClearObject(rsc, dst);
}

static bool SC_IsObject(const ObjectBase *src) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrIsObject(rsc, src);
}




static const Allocation * SC_GetAllocation(const void *ptr) {
    Context *rsc = RsdCpuReference::getTlsContext();
    const Script *sc = RsdCpuReference::getTlsScript();
    return rsdScriptGetAllocationForPointer(rsc, sc, ptr);
}

static void SC_ForEach_SAA(Script *target,
                            Allocation *in,
                            Allocation *out) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrForEach(rsc, target, in, out, NULL, 0, NULL);
}

static void SC_ForEach_SAAU(Script *target,
                            Allocation *in,
                            Allocation *out,
                            const void *usr) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrForEach(rsc, target, in, out, usr, 0, NULL);
}

static void SC_ForEach_SAAUS(Script *target,
                             Allocation *in,
                             Allocation *out,
                             const void *usr,
                             const RsScriptCall *call) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrForEach(rsc, target, in, out, usr, 0, call);
}

static void SC_ForEach_SAAUL(Script *target,
                             Allocation *in,
                             Allocation *out,
                             const void *usr,
                             uint32_t usrLen) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrForEach(rsc, target, in, out, usr, usrLen, NULL);
}

static void SC_ForEach_SAAULS(Script *target,
                              Allocation *in,
                              Allocation *out,
                              const void *usr,
                              uint32_t usrLen,
                              const RsScriptCall *call) {
    Context *rsc = RsdCpuReference::getTlsContext();
    rsrForEach(rsc, target, in, out, usr, usrLen, call);
}



//////////////////////////////////////////////////////////////////////////////
// Time routines
//////////////////////////////////////////////////////////////////////////////

static float SC_GetDt() {
    Context *rsc = RsdCpuReference::getTlsContext();
    const Script *sc = RsdCpuReference::getTlsScript();
    return rsrGetDt(rsc, sc);
}

#ifndef RS_COMPATIBILITY_LIB
time_t SC_Time(time_t *timer) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrTime(rsc, timer);
}
#else
static int SC_Time(int *timer) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrTime(rsc, (long*)timer);
}
#endif

tm* SC_LocalTime(tm *local, time_t *timer) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrLocalTime(rsc, local, timer);
}

int64_t SC_UptimeMillis() {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrUptimeMillis(rsc);
}

int64_t SC_UptimeNanos() {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrUptimeNanos(rsc);
}

//////////////////////////////////////////////////////////////////////////////
// Message routines
//////////////////////////////////////////////////////////////////////////////

static uint32_t SC_ToClient2(int cmdID, void *data, int len) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrToClient(rsc, cmdID, data, len);
}

static uint32_t SC_ToClient(int cmdID) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrToClient(rsc, cmdID, NULL, 0);
}

static uint32_t SC_ToClientBlocking2(int cmdID, void *data, int len) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrToClientBlocking(rsc, cmdID, data, len);
}

static uint32_t SC_ToClientBlocking(int cmdID) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrToClientBlocking(rsc, cmdID, NULL, 0);
}


static void * ElementAt1D(Allocation *a, RsDataType dt, uint32_t vecSize, uint32_t x) {
    Context *rsc = RsdCpuReference::getTlsContext();
    const Type *t = a->getType();
    const Element *e = t->getElement();

    char buf[256];
    if (x >= t->getLODDimX(0)) {
        sprintf(buf, "Out range ElementAt X %i of %i", x, t->getLODDimX(0));
        rsc->setError(RS_ERROR_FATAL_DEBUG, buf);
        return NULL;
    }

    if (vecSize > 0) {
        if (vecSize != e->getVectorSize()) {
            sprintf(buf, "Vector size mismatch for ElementAt %i of %i", vecSize, e->getVectorSize());
            rsc->setError(RS_ERROR_FATAL_DEBUG, buf);
            return NULL;
        }

        if (dt != e->getType()) {
            sprintf(buf, "Data type mismatch for ElementAt %i of %i", dt, e->getType());
            rsc->setError(RS_ERROR_FATAL_DEBUG, buf);
            return NULL;
        }
    }

    uint8_t *p = (uint8_t *)a->mHal.drvState.lod[0].mallocPtr;
    const uint32_t eSize = e->getSizeBytes();
    return &p[(eSize * x)];
}

static void * ElementAt2D(Allocation *a, RsDataType dt, uint32_t vecSize, uint32_t x, uint32_t y) {
    Context *rsc = RsdCpuReference::getTlsContext();
    const Type *t = a->getType();
    const Element *e = t->getElement();

    char buf[256];
    if (x >= t->getLODDimX(0)) {
        sprintf(buf, "Out range ElementAt X %i of %i", x, t->getLODDimX(0));
        rsc->setError(RS_ERROR_FATAL_DEBUG, buf);
        return NULL;
    }

    if (y >= t->getLODDimY(0)) {
        sprintf(buf, "Out range ElementAt Y %i of %i", y, t->getLODDimY(0));
        rsc->setError(RS_ERROR_FATAL_DEBUG, buf);
        return NULL;
    }

    if (vecSize > 0) {
        if (vecSize != e->getVectorSize()) {
            sprintf(buf, "Vector size mismatch for ElementAt %i of %i", vecSize, e->getVectorSize());
            rsc->setError(RS_ERROR_FATAL_DEBUG, buf);
            return NULL;
        }

        if (dt != e->getType()) {
            sprintf(buf, "Data type mismatch for ElementAt %i of %i", dt, e->getType());
            rsc->setError(RS_ERROR_FATAL_DEBUG, buf);
            return NULL;
        }
    }

    uint8_t *p = (uint8_t *)a->mHal.drvState.lod[0].mallocPtr;
    const uint32_t eSize = e->getSizeBytes();
    const uint32_t stride = a->mHal.drvState.lod[0].stride;
    return &p[(eSize * x) + (y * stride)];
}

static void * ElementAt3D(Allocation *a, RsDataType dt, uint32_t vecSize, uint32_t x, uint32_t y, uint32_t z) {
    Context *rsc = RsdCpuReference::getTlsContext();
    const Type *t = a->getType();
    const Element *e = t->getElement();

    char buf[256];
    if (x >= t->getLODDimX(0)) {
        sprintf(buf, "Out range ElementAt X %i of %i", x, t->getLODDimX(0));
        rsc->setError(RS_ERROR_FATAL_DEBUG, buf);
        return NULL;
    }

    if (y >= t->getLODDimY(0)) {
        sprintf(buf, "Out range ElementAt Y %i of %i", y, t->getLODDimY(0));
        rsc->setError(RS_ERROR_FATAL_DEBUG, buf);
        return NULL;
    }

    if (z >= t->getLODDimZ(0)) {
        sprintf(buf, "Out range ElementAt Z %i of %i", z, t->getLODDimZ(0));
        rsc->setError(RS_ERROR_FATAL_DEBUG, buf);
        return NULL;
    }

    if (vecSize > 0) {
        if (vecSize != e->getVectorSize()) {
            sprintf(buf, "Vector size mismatch for ElementAt %i of %i", vecSize, e->getVectorSize());
            rsc->setError(RS_ERROR_FATAL_DEBUG, buf);
            return NULL;
        }

        if (dt != e->getType()) {
            sprintf(buf, "Data type mismatch for ElementAt %i of %i", dt, e->getType());
            rsc->setError(RS_ERROR_FATAL_DEBUG, buf);
            return NULL;
        }
    }

    uint8_t *p = (uint8_t *)a->mHal.drvState.lod[0].mallocPtr;
    const uint32_t eSize = e->getSizeBytes();
    const uint32_t stride = a->mHal.drvState.lod[0].stride;
    return &p[(eSize * x) + (y * stride)];
}

static const void * SC_GetElementAt1D(Allocation *a, uint32_t x) {
    return ElementAt1D(a, RS_TYPE_UNSIGNED_8, 0, x);
}
static const void * SC_GetElementAt2D(Allocation *a, uint32_t x, uint32_t y) {
    return ElementAt2D(a, RS_TYPE_UNSIGNED_8, 0, x, y);
}
static const void * SC_GetElementAt3D(Allocation *a, uint32_t x, uint32_t y, uint32_t z) {
    return ElementAt3D(a, RS_TYPE_UNSIGNED_8, 0, x, y, z);
}

static void SC_SetElementAt1D(Allocation *a, const void *ptr, uint32_t x) {
    const Type *t = a->getType();
    const Element *e = t->getElement();
    void *tmp = ElementAt1D(a, RS_TYPE_UNSIGNED_8, 0, x);
    if (tmp != NULL) {
        memcpy(tmp, ptr, e->getSizeBytes());
    }
}
static void SC_SetElementAt2D(Allocation *a, const void *ptr, uint32_t x, uint32_t y) {
    const Type *t = a->getType();
    const Element *e = t->getElement();
    void *tmp = ElementAt2D(a, RS_TYPE_UNSIGNED_8, 0, x, y);
    if (tmp != NULL) {
        memcpy(tmp, ptr, e->getSizeBytes());
    }
}
static void SC_SetElementAt3D(Allocation *a, const void *ptr, uint32_t x, uint32_t y, uint32_t z) {
    const Type *t = a->getType();
    const Element *e = t->getElement();
    void *tmp = ElementAt3D(a, RS_TYPE_UNSIGNED_8, 0, x, y, z);
    if (tmp != NULL) {
        memcpy(tmp, ptr, e->getSizeBytes());
    }
}

#define ELEMENT_AT(T, DT, VS)                                               \
    static void SC_SetElementAt1_##T(Allocation *a, const T *val, uint32_t x) {           \
        void *r = ElementAt1D(a, DT, VS, x);                            \
        if (r != NULL) ((T *)r)[0] = *val;                               \
        else ALOGE("Error from %s", __PRETTY_FUNCTION__);               \
    }                                                                   \
    static void SC_SetElementAt2_##T(Allocation * a, const T * val, uint32_t x, uint32_t y) { \
        void *r = ElementAt2D(a, DT, VS, x, y);            \
        if (r != NULL) ((T *)r)[0] = *val;                               \
        else ALOGE("Error from %s", __PRETTY_FUNCTION__);               \
    }                                                                   \
    static void SC_SetElementAt3_##T(Allocation * a, const T * val, uint32_t x, uint32_t y, uint32_t z) { \
        void *r = ElementAt3D(a, DT, VS, x, y, z);         \
        if (r != NULL) ((T *)r)[0] = *val;                               \
        else ALOGE("Error from %s", __PRETTY_FUNCTION__);               \
    }                                                                   \
    static void SC_GetElementAt1_##T(Allocation * a, T *val, uint32_t x) {                  \
        void *r = ElementAt1D(a, DT, VS, x);               \
        if (r != NULL) *val = ((T *)r)[0];                              \
        else ALOGE("Error from %s", __PRETTY_FUNCTION__);                    \
    }                                                                   \
    static void SC_GetElementAt2_##T(Allocation * a, T *val, uint32_t x, uint32_t y) {      \
        void *r = ElementAt2D(a, DT, VS, x, y);            \
        if (r != NULL) *val = ((T *)r)[0];                              \
        else ALOGE("Error from %s", __PRETTY_FUNCTION__);                    \
    }                                                                   \
    static void SC_GetElementAt3_##T(Allocation * a, T *val, uint32_t x, uint32_t y, uint32_t z) { \
        void *r = ElementAt3D(a, DT, VS, x, y, z);         \
        if (r != NULL) *val = ((T *)r)[0];                              \
        else ALOGE("Error from %s", __PRETTY_FUNCTION__);                    \
    }

ELEMENT_AT(char, RS_TYPE_SIGNED_8, 1)
ELEMENT_AT(char2, RS_TYPE_SIGNED_8, 2)
ELEMENT_AT(char3, RS_TYPE_SIGNED_8, 3)
ELEMENT_AT(char4, RS_TYPE_SIGNED_8, 4)
ELEMENT_AT(uchar, RS_TYPE_UNSIGNED_8, 1)
ELEMENT_AT(uchar2, RS_TYPE_UNSIGNED_8, 2)
ELEMENT_AT(uchar3, RS_TYPE_UNSIGNED_8, 3)
ELEMENT_AT(uchar4, RS_TYPE_UNSIGNED_8, 4)
ELEMENT_AT(short, RS_TYPE_SIGNED_16, 1)
ELEMENT_AT(short2, RS_TYPE_SIGNED_16, 2)
ELEMENT_AT(short3, RS_TYPE_SIGNED_16, 3)
ELEMENT_AT(short4, RS_TYPE_SIGNED_16, 4)
ELEMENT_AT(ushort, RS_TYPE_UNSIGNED_16, 1)
ELEMENT_AT(ushort2, RS_TYPE_UNSIGNED_16, 2)
ELEMENT_AT(ushort3, RS_TYPE_UNSIGNED_16, 3)
ELEMENT_AT(ushort4, RS_TYPE_UNSIGNED_16, 4)
ELEMENT_AT(int, RS_TYPE_SIGNED_32, 1)
ELEMENT_AT(int2, RS_TYPE_SIGNED_32, 2)
ELEMENT_AT(int3, RS_TYPE_SIGNED_32, 3)
ELEMENT_AT(int4, RS_TYPE_SIGNED_32, 4)
ELEMENT_AT(uint, RS_TYPE_UNSIGNED_32, 1)
ELEMENT_AT(uint2, RS_TYPE_UNSIGNED_32, 2)
ELEMENT_AT(uint3, RS_TYPE_UNSIGNED_32, 3)
ELEMENT_AT(uint4, RS_TYPE_UNSIGNED_32, 4)
ELEMENT_AT(long, RS_TYPE_SIGNED_64, 1)
ELEMENT_AT(long2, RS_TYPE_SIGNED_64, 2)
ELEMENT_AT(long3, RS_TYPE_SIGNED_64, 3)
ELEMENT_AT(long4, RS_TYPE_SIGNED_64, 4)
ELEMENT_AT(ulong, RS_TYPE_UNSIGNED_64, 1)
ELEMENT_AT(ulong2, RS_TYPE_UNSIGNED_64, 2)
ELEMENT_AT(ulong3, RS_TYPE_UNSIGNED_64, 3)
ELEMENT_AT(ulong4, RS_TYPE_UNSIGNED_64, 4)
ELEMENT_AT(float, RS_TYPE_FLOAT_32, 1)
ELEMENT_AT(float2, RS_TYPE_FLOAT_32, 2)
ELEMENT_AT(float3, RS_TYPE_FLOAT_32, 3)
ELEMENT_AT(float4, RS_TYPE_FLOAT_32, 4)
ELEMENT_AT(double, RS_TYPE_FLOAT_64, 1)
ELEMENT_AT(double2, RS_TYPE_FLOAT_64, 2)
ELEMENT_AT(double3, RS_TYPE_FLOAT_64, 3)
ELEMENT_AT(double4, RS_TYPE_FLOAT_64, 4)

#undef ELEMENT_AT

//////////////////////////////////////////////////////////////////////////////
// Stub implementation
//////////////////////////////////////////////////////////////////////////////

// llvm name mangling ref
//  <builtin-type> ::= v  # void
//                 ::= b  # bool
//                 ::= c  # char
//                 ::= a  # signed char
//                 ::= h  # unsigned char
//                 ::= s  # short
//                 ::= t  # unsigned short
//                 ::= i  # int
//                 ::= j  # unsigned int
//                 ::= l  # long
//                 ::= m  # unsigned long
//                 ::= x  # long long, __int64
//                 ::= y  # unsigned long long, __int64
//                 ::= f  # float
//                 ::= d  # double

static RsdCpuReference::CpuSymbol gSyms[] = {
    // Debug runtime
    { "_Z14rsGetElementAt13rs_allocationj", (void *)&SC_GetElementAt1D, true },
    { "_Z14rsGetElementAt13rs_allocationjj", (void *)&SC_GetElementAt2D, true },
    { "_Z14rsGetElementAt13rs_allocationjjj", (void *)&SC_GetElementAt3D, true },
    { "_Z14rsSetElementAt13rs_allocationPKvj", (void *)&SC_SetElementAt1D, true },
    { "_Z14rsSetElementAt13rs_allocationPKvjj", (void *)&SC_SetElementAt2D, true },
    { "_Z14rsSetElementAt13rs_allocationPKvjjj", (void *)&SC_SetElementAt3D, true },


    { "_Z20rsGetElementAt_uchar13rs_allocationPhj", (void *)&SC_GetElementAt1_uchar, true },
    { "_Z21rsGetElementAt_uchar213rs_allocationPDv2_hj", (void *)&SC_GetElementAt1_uchar2, true },
    { "_Z21rsGetElementAt_uchar313rs_allocationPDv3_hj", (void *)&SC_GetElementAt1_uchar3, true },
    { "_Z21rsGetElementAt_uchar413rs_allocationPDv4_hj", (void *)&SC_GetElementAt1_uchar4, true },
    { "_Z20rsGetElementAt_uchar13rs_allocationPhjj", (void *)&SC_GetElementAt2_uchar, true },
    { "_Z21rsGetElementAt_uchar213rs_allocationPDv2_hjj", (void *)&SC_GetElementAt2_uchar2, true },
    { "_Z21rsGetElementAt_uchar313rs_allocationPDv3_hjj", (void *)&SC_GetElementAt2_uchar3, true },
    { "_Z21rsGetElementAt_uchar413rs_allocationPDv4_hjj", (void *)&SC_GetElementAt2_uchar4, true },
    { "_Z20rsGetElementAt_uchar13rs_allocationPhjjj", (void *)&SC_GetElementAt3_uchar, true },
    { "_Z21rsGetElementAt_uchar213rs_allocationPDv2_hjjj", (void *)&SC_GetElementAt3_uchar2, true },
    { "_Z21rsGetElementAt_uchar313rs_allocationPDv3_hjjj", (void *)&SC_GetElementAt3_uchar3, true },
    { "_Z21rsGetElementAt_uchar413rs_allocationPDv4_hjjj", (void *)&SC_GetElementAt3_uchar4, true },

    { "_Z19rsGetElementAt_char13rs_allocationPcj", (void *)&SC_GetElementAt1_char, true },
    { "_Z20rsGetElementAt_char213rs_allocationPDv2_cj", (void *)&SC_GetElementAt1_char2, true },
    { "_Z20rsGetElementAt_char313rs_allocationPDv3_cj", (void *)&SC_GetElementAt1_char3, true },
    { "_Z20rsGetElementAt_char413rs_allocationPDv4_cj", (void *)&SC_GetElementAt1_char4, true },
    { "_Z19rsGetElementAt_char13rs_allocationPcjj", (void *)&SC_GetElementAt2_char, true },
    { "_Z20rsGetElementAt_char213rs_allocationPDv2_cjj", (void *)&SC_GetElementAt2_char2, true },
    { "_Z20rsGetElementAt_char313rs_allocationPDv3_cjj", (void *)&SC_GetElementAt2_char3, true },
    { "_Z20rsGetElementAt_char413rs_allocationPDv4_cjj", (void *)&SC_GetElementAt2_char4, true },
    { "_Z19rsGetElementAt_char13rs_allocationPcjjj", (void *)&SC_GetElementAt3_char, true },
    { "_Z20rsGetElementAt_char213rs_allocationPDv2_cjjj", (void *)&SC_GetElementAt3_char2, true },
    { "_Z20rsGetElementAt_char313rs_allocationPDv3_cjjj", (void *)&SC_GetElementAt3_char3, true },
    { "_Z20rsGetElementAt_char413rs_allocationPDv4_cjjj", (void *)&SC_GetElementAt3_char4, true },

    { "_Z21rsGetElementAt_ushort13rs_allocationPtj", (void *)&SC_GetElementAt1_ushort, true },
    { "_Z22rsGetElementAt_ushort213rs_allocationPDv2_tj", (void *)&SC_GetElementAt1_ushort2, true },
    { "_Z22rsGetElementAt_ushort313rs_allocationPDv3_tj", (void *)&SC_GetElementAt1_ushort3, true },
    { "_Z22rsGetElementAt_ushort413rs_allocationPDv4_tj", (void *)&SC_GetElementAt1_ushort4, true },
    { "_Z21rsGetElementAt_ushort13rs_allocationPtjj", (void *)&SC_GetElementAt2_ushort, true },
    { "_Z22rsGetElementAt_ushort213rs_allocationPDv2_tjj", (void *)&SC_GetElementAt2_ushort2, true },
    { "_Z22rsGetElementAt_ushort313rs_allocationPDv3_tjj", (void *)&SC_GetElementAt2_ushort3, true },
    { "_Z22rsGetElementAt_ushort413rs_allocationPDv4_tjj", (void *)&SC_GetElementAt2_ushort4, true },
    { "_Z21rsGetElementAt_ushort13rs_allocationPtjjj", (void *)&SC_GetElementAt3_ushort, true },
    { "_Z22rsGetElementAt_ushort213rs_allocationPDv2_tjjj", (void *)&SC_GetElementAt3_ushort2, true },
    { "_Z22rsGetElementAt_ushort313rs_allocationPDv3_tjjj", (void *)&SC_GetElementAt3_ushort3, true },
    { "_Z22rsGetElementAt_ushort413rs_allocationPDv4_tjjj", (void *)&SC_GetElementAt3_ushort4, true },

    { "_Z20rsGetElementAt_short13rs_allocationPsj", (void *)&SC_GetElementAt1_short, true },
    { "_Z21rsGetElementAt_short213rs_allocationPDv2_sj", (void *)&SC_GetElementAt1_short2, true },
    { "_Z21rsGetElementAt_short313rs_allocationPDv3_sj", (void *)&SC_GetElementAt1_short3, true },
    { "_Z21rsGetElementAt_short413rs_allocationPDv4_sj", (void *)&SC_GetElementAt1_short4, true },
    { "_Z20rsGetElementAt_short13rs_allocationPsjj", (void *)&SC_GetElementAt2_short, true },
    { "_Z21rsGetElementAt_short213rs_allocationPDv2_sjj", (void *)&SC_GetElementAt2_short2, true },
    { "_Z21rsGetElementAt_short313rs_allocationPDv3_sjj", (void *)&SC_GetElementAt2_short3, true },
    { "_Z21rsGetElementAt_short413rs_allocationPDv4_sjj", (void *)&SC_GetElementAt2_short4, true },
    { "_Z20rsGetElementAt_short13rs_allocationPsjjj", (void *)&SC_GetElementAt3_short, true },
    { "_Z21rsGetElementAt_short213rs_allocationPDv2_sjjj", (void *)&SC_GetElementAt3_short2, true },
    { "_Z21rsGetElementAt_short313rs_allocationPDv3_sjjj", (void *)&SC_GetElementAt3_short3, true },
    { "_Z21rsGetElementAt_short413rs_allocationPDv4_sjjj", (void *)&SC_GetElementAt3_short4, true },

    { "_Z19rsGetElementAt_uint13rs_allocationPjj", (void *)&SC_GetElementAt1_uint, true },
    { "_Z20rsGetElementAt_uint213rs_allocationPDv2_jj", (void *)&SC_GetElementAt1_uint2, true },
    { "_Z20rsGetElementAt_uint313rs_allocationPDv3_jj", (void *)&SC_GetElementAt1_uint3, true },
    { "_Z20rsGetElementAt_uint413rs_allocationPDv4_jj", (void *)&SC_GetElementAt1_uint4, true },
    { "_Z19rsGetElementAt_uint13rs_allocationPjjj", (void *)&SC_GetElementAt2_uint, true },
    { "_Z20rsGetElementAt_uint213rs_allocationPDv2_jjj", (void *)&SC_GetElementAt2_uint2, true },
    { "_Z20rsGetElementAt_uint313rs_allocationPDv3_jjj", (void *)&SC_GetElementAt2_uint3, true },
    { "_Z20rsGetElementAt_uint413rs_allocationPDv4_jjj", (void *)&SC_GetElementAt2_uint4, true },
    { "_Z19rsGetElementAt_uint13rs_allocationPjjjj", (void *)&SC_GetElementAt3_uint, true },
    { "_Z20rsGetElementAt_uint213rs_allocationPDv2_jjjj", (void *)&SC_GetElementAt3_uint2, true },
    { "_Z20rsGetElementAt_uint313rs_allocationPDv3_jjjj", (void *)&SC_GetElementAt3_uint3, true },
    { "_Z20rsGetElementAt_uint413rs_allocationPDv4_jjjj", (void *)&SC_GetElementAt3_uint4, true },

    { "_Z18rsGetElementAt_int13rs_allocationPij", (void *)&SC_GetElementAt1_int, true },
    { "_Z19rsGetElementAt_int213rs_allocationPDv2_ij", (void *)&SC_GetElementAt1_int2, true },
    { "_Z19rsGetElementAt_int313rs_allocationPDv3_ij", (void *)&SC_GetElementAt1_int3, true },
    { "_Z19rsGetElementAt_int413rs_allocationPDv4_ij", (void *)&SC_GetElementAt1_int4, true },
    { "_Z18rsGetElementAt_int13rs_allocationPijj", (void *)&SC_GetElementAt2_int, true },
    { "_Z19rsGetElementAt_int213rs_allocationPDv2_ijj", (void *)&SC_GetElementAt2_int2, true },
    { "_Z19rsGetElementAt_int313rs_allocationPDv3_ijj", (void *)&SC_GetElementAt2_int3, true },
    { "_Z19rsGetElementAt_int413rs_allocationPDv4_ijj", (void *)&SC_GetElementAt2_int4, true },
    { "_Z18rsGetElementAt_int13rs_allocationPijjj", (void *)&SC_GetElementAt3_int, true },
    { "_Z19rsGetElementAt_int213rs_allocationPDv2_ijjj", (void *)&SC_GetElementAt3_int2, true },
    { "_Z19rsGetElementAt_int313rs_allocationPDv3_ijjj", (void *)&SC_GetElementAt3_int3, true },
    { "_Z19rsGetElementAt_int413rs_allocationPDv4_ijjj", (void *)&SC_GetElementAt3_int4, true },

    { "_Z20rsGetElementAt_ulong13rs_allocationPmj", (void *)&SC_GetElementAt1_ulong, true },
    { "_Z21rsGetElementAt_ulong213rs_allocationPDv2_mj", (void *)&SC_GetElementAt1_ulong2, true },
    { "_Z21rsGetElementAt_ulong313rs_allocationPDv3_mj", (void *)&SC_GetElementAt1_ulong3, true },
    { "_Z21rsGetElementAt_ulong413rs_allocationPDv4_mj", (void *)&SC_GetElementAt1_ulong4, true },
    { "_Z20rsGetElementAt_ulong13rs_allocationPmjj", (void *)&SC_GetElementAt2_ulong, true },
    { "_Z21rsGetElementAt_ulong213rs_allocationPDv2_mjj", (void *)&SC_GetElementAt2_ulong2, true },
    { "_Z21rsGetElementAt_ulong313rs_allocationPDv3_mjj", (void *)&SC_GetElementAt2_ulong3, true },
    { "_Z21rsGetElementAt_ulong413rs_allocationPDv4_mjj", (void *)&SC_GetElementAt2_ulong4, true },
    { "_Z20rsGetElementAt_ulong13rs_allocationPmjjj", (void *)&SC_GetElementAt3_ulong, true },
    { "_Z21rsGetElementAt_ulong213rs_allocationPDv2_mjjj", (void *)&SC_GetElementAt3_ulong2, true },
    { "_Z21rsGetElementAt_ulong313rs_allocationPDv3_mjjj", (void *)&SC_GetElementAt3_ulong3, true },
    { "_Z21rsGetElementAt_ulong413rs_allocationPDv4_mjjj", (void *)&SC_GetElementAt3_ulong4, true },

    { "_Z19rsGetElementAt_long13rs_allocationPlj", (void *)&SC_GetElementAt1_long, true },
    { "_Z20rsGetElementAt_long213rs_allocationPDv2_lj", (void *)&SC_GetElementAt1_long2, true },
    { "_Z20rsGetElementAt_long313rs_allocationPDv3_lj", (void *)&SC_GetElementAt1_long3, true },
    { "_Z20rsGetElementAt_long413rs_allocationPDv4_lj", (void *)&SC_GetElementAt1_long4, true },
    { "_Z19rsGetElementAt_long13rs_allocationPljj", (void *)&SC_GetElementAt2_long, true },
    { "_Z20rsGetElementAt_long213rs_allocationPDv2_ljj", (void *)&SC_GetElementAt2_long2, true },
    { "_Z20rsGetElementAt_long313rs_allocationPDv3_ljj", (void *)&SC_GetElementAt2_long3, true },
    { "_Z20rsGetElementAt_long413rs_allocationPDv4_ljj", (void *)&SC_GetElementAt2_long4, true },
    { "_Z19rsGetElementAt_long13rs_allocationPljjj", (void *)&SC_GetElementAt3_long, true },
    { "_Z20rsGetElementAt_long213rs_allocationPDv2_ljjj", (void *)&SC_GetElementAt3_long2, true },
    { "_Z20rsGetElementAt_long313rs_allocationPDv3_ljjj", (void *)&SC_GetElementAt3_long3, true },
    { "_Z20rsGetElementAt_long413rs_allocationPDv4_ljjj", (void *)&SC_GetElementAt3_long4, true },

    { "_Z20rsGetElementAt_float13rs_allocationPfj", (void *)&SC_GetElementAt1_float, true },
    { "_Z21rsGetElementAt_float213rs_allocationPDv2_fj", (void *)&SC_GetElementAt1_float2, true },
    { "_Z21rsGetElementAt_float313rs_allocationPDv3_fj", (void *)&SC_GetElementAt1_float3, true },
    { "_Z21rsGetElementAt_float413rs_allocationPDv4_fj", (void *)&SC_GetElementAt1_float4, true },
    { "_Z20rsGetElementAt_float13rs_allocationPfjj", (void *)&SC_GetElementAt2_float, true },
    { "_Z21rsGetElementAt_float213rs_allocationPDv2_fjj", (void *)&SC_GetElementAt2_float2, true },
    { "_Z21rsGetElementAt_float313rs_allocationPDv3_fjj", (void *)&SC_GetElementAt2_float3, true },
    { "_Z21rsGetElementAt_float413rs_allocationPDv4_fjj", (void *)&SC_GetElementAt2_float4, true },
    { "_Z20rsGetElementAt_float13rs_allocationPfjjj", (void *)&SC_GetElementAt3_float, true },
    { "_Z21rsGetElementAt_float213rs_allocationPDv2_fjjj", (void *)&SC_GetElementAt3_float2, true },
    { "_Z21rsGetElementAt_float313rs_allocationPDv3_fjjj", (void *)&SC_GetElementAt3_float3, true },
    { "_Z21rsGetElementAt_float413rs_allocationPDv4_fjjj", (void *)&SC_GetElementAt3_float4, true },

    { "_Z21rsGetElementAt_double13rs_allocationPdj", (void *)&SC_GetElementAt1_double, true },
    { "_Z22rsGetElementAt_double213rs_allocationPDv2_dj", (void *)&SC_GetElementAt1_double2, true },
    { "_Z22rsGetElementAt_double313rs_allocationPDv3_dj", (void *)&SC_GetElementAt1_double3, true },
    { "_Z22rsGetElementAt_double413rs_allocationPDv4_dj", (void *)&SC_GetElementAt1_double4, true },
    { "_Z21rsGetElementAt_double13rs_allocationPdjj", (void *)&SC_GetElementAt2_double, true },
    { "_Z22rsGetElementAt_double213rs_allocationPDv2_djj", (void *)&SC_GetElementAt2_double2, true },
    { "_Z22rsGetElementAt_double313rs_allocationPDv3_djj", (void *)&SC_GetElementAt2_double3, true },
    { "_Z22rsGetElementAt_double413rs_allocationPDv4_djj", (void *)&SC_GetElementAt2_double4, true },
    { "_Z21rsGetElementAt_double13rs_allocationPdjjj", (void *)&SC_GetElementAt3_double, true },
    { "_Z22rsGetElementAt_double213rs_allocationPDv2_djjj", (void *)&SC_GetElementAt3_double2, true },
    { "_Z22rsGetElementAt_double313rs_allocationPDv3_djjj", (void *)&SC_GetElementAt3_double3, true },
    { "_Z22rsGetElementAt_double413rs_allocationPDv4_djjj", (void *)&SC_GetElementAt3_double4, true },



    { "_Z20rsSetElementAt_uchar13rs_allocationPKhj", (void *)&SC_SetElementAt1_uchar, true },
    { "_Z21rsSetElementAt_uchar213rs_allocationPKDv2_hj", (void *)&SC_SetElementAt1_uchar2, true },
    { "_Z21rsSetElementAt_uchar313rs_allocationPKDv3_hj", (void *)&SC_SetElementAt1_uchar3, true },
    { "_Z21rsSetElementAt_uchar413rs_allocationPKDv4_hj", (void *)&SC_SetElementAt1_uchar4, true },
    { "_Z20rsSetElementAt_uchar13rs_allocationPKhjj", (void *)&SC_SetElementAt2_uchar, true },
    { "_Z21rsSetElementAt_uchar213rs_allocationPKDv2_hjj", (void *)&SC_SetElementAt2_uchar2, true },
    { "_Z21rsSetElementAt_uchar313rs_allocationPKDv3_hjj", (void *)&SC_SetElementAt2_uchar3, true },
    { "_Z21rsSetElementAt_uchar413rs_allocationPKDv4_hjj", (void *)&SC_SetElementAt2_uchar4, true },
    { "_Z20rsSetElementAt_uchar13rs_allocationPKhjjj", (void *)&SC_SetElementAt3_uchar, true },
    { "_Z21rsSetElementAt_uchar213rs_allocationPKDv2_hjjj", (void *)&SC_SetElementAt3_uchar2, true },
    { "_Z21rsSetElementAt_uchar313rs_allocationPKDv3_hjjj", (void *)&SC_SetElementAt3_uchar3, true },
    { "_Z21rsSetElementAt_uchar413rs_allocationPKDv4_hjjj", (void *)&SC_SetElementAt3_uchar4, true },

    { "_Z19rsSetElementAt_char13rs_allocationPKcj", (void *)&SC_SetElementAt1_char, true },
    { "_Z20rsSetElementAt_char213rs_allocationPKDv2_cj", (void *)&SC_SetElementAt1_char2, true },
    { "_Z20rsSetElementAt_char313rs_allocationPKDv3_cj", (void *)&SC_SetElementAt1_char3, true },
    { "_Z20rsSetElementAt_char413rs_allocationPKDv4_cj", (void *)&SC_SetElementAt1_char4, true },
    { "_Z19rsSetElementAt_char13rs_allocationPKcjj", (void *)&SC_SetElementAt2_char, true },
    { "_Z20rsSetElementAt_char213rs_allocationPKDv2_cjj", (void *)&SC_SetElementAt2_char2, true },
    { "_Z20rsSetElementAt_char313rs_allocationPKDv3_cjj", (void *)&SC_SetElementAt2_char3, true },
    { "_Z20rsSetElementAt_char413rs_allocationPKDv4_cjj", (void *)&SC_SetElementAt2_char4, true },
    { "_Z19rsSetElementAt_char13rs_allocationPKcjjj", (void *)&SC_SetElementAt3_char, true },
    { "_Z20rsSetElementAt_char213rs_allocationPKDv2_cjjj", (void *)&SC_SetElementAt3_char2, true },
    { "_Z20rsSetElementAt_char313rs_allocationPKDv3_cjjj", (void *)&SC_SetElementAt3_char3, true },
    { "_Z20rsSetElementAt_char413rs_allocationPKDv4_cjjj", (void *)&SC_SetElementAt3_char4, true },

    { "_Z21rsSetElementAt_ushort13rs_allocationPKht", (void *)&SC_SetElementAt1_ushort, true },
    { "_Z22rsSetElementAt_ushort213rs_allocationPKDv2_tj", (void *)&SC_SetElementAt1_ushort2, true },
    { "_Z22rsSetElementAt_ushort313rs_allocationPKDv3_tj", (void *)&SC_SetElementAt1_ushort3, true },
    { "_Z22rsSetElementAt_ushort413rs_allocationPKDv4_tj", (void *)&SC_SetElementAt1_ushort4, true },
    { "_Z21rsSetElementAt_ushort13rs_allocationPKtjj", (void *)&SC_SetElementAt2_ushort, true },
    { "_Z22rsSetElementAt_ushort213rs_allocationPKDv2_tjj", (void *)&SC_SetElementAt2_ushort2, true },
    { "_Z22rsSetElementAt_ushort313rs_allocationPKDv3_tjj", (void *)&SC_SetElementAt2_ushort3, true },
    { "_Z22rsSetElementAt_ushort413rs_allocationPKDv4_tjj", (void *)&SC_SetElementAt2_ushort4, true },
    { "_Z21rsSetElementAt_ushort13rs_allocationPKtjjj", (void *)&SC_SetElementAt3_ushort, true },
    { "_Z22rsSetElementAt_ushort213rs_allocationPKDv2_tjjj", (void *)&SC_SetElementAt3_ushort2, true },
    { "_Z22rsSetElementAt_ushort313rs_allocationPKDv3_tjjj", (void *)&SC_SetElementAt3_ushort3, true },
    { "_Z22rsSetElementAt_ushort413rs_allocationPKDv4_tjjj", (void *)&SC_SetElementAt3_ushort4, true },

    { "_Z20rsSetElementAt_short13rs_allocationPKsj", (void *)&SC_SetElementAt1_short, true },
    { "_Z21rsSetElementAt_short213rs_allocationPKDv2_sj", (void *)&SC_SetElementAt1_short2, true },
    { "_Z21rsSetElementAt_short313rs_allocationPKDv3_sj", (void *)&SC_SetElementAt1_short3, true },
    { "_Z21rsSetElementAt_short413rs_allocationPKDv4_sj", (void *)&SC_SetElementAt1_short4, true },
    { "_Z20rsSetElementAt_short13rs_allocationPKsjj", (void *)&SC_SetElementAt2_short, true },
    { "_Z21rsSetElementAt_short213rs_allocationPKDv2_sjj", (void *)&SC_SetElementAt2_short2, true },
    { "_Z21rsSetElementAt_short313rs_allocationPKDv3_sjj", (void *)&SC_SetElementAt2_short3, true },
    { "_Z21rsSetElementAt_short413rs_allocationPKDv4_sjj", (void *)&SC_SetElementAt2_short4, true },
    { "_Z20rsSetElementAt_short13rs_allocationPKsjjj", (void *)&SC_SetElementAt3_short, true },
    { "_Z21rsSetElementAt_short213rs_allocationPKDv2_sjjj", (void *)&SC_SetElementAt3_short2, true },
    { "_Z21rsSetElementAt_short313rs_allocationPKDv3_sjjj", (void *)&SC_SetElementAt3_short3, true },
    { "_Z21rsSetElementAt_short413rs_allocationPKDv4_sjjj", (void *)&SC_SetElementAt3_short4, true },

    { "_Z19rsSetElementAt_uint13rs_allocationPKjj", (void *)&SC_SetElementAt1_uint, true },
    { "_Z20rsSetElementAt_uint213rs_allocationPKDv2_jj", (void *)&SC_SetElementAt1_uint2, true },
    { "_Z20rsSetElementAt_uint313rs_allocationPKDv3_jj", (void *)&SC_SetElementAt1_uint3, true },
    { "_Z20rsSetElementAt_uint413rs_allocationPKDv4_jj", (void *)&SC_SetElementAt1_uint4, true },
    { "_Z19rsSetElementAt_uint13rs_allocationPKjjj", (void *)&SC_SetElementAt2_uint, true },
    { "_Z20rsSetElementAt_uint213rs_allocationPKDv2_jjj", (void *)&SC_SetElementAt2_uint2, true },
    { "_Z20rsSetElementAt_uint313rs_allocationPKDv3_jjj", (void *)&SC_SetElementAt2_uint3, true },
    { "_Z20rsSetElementAt_uint413rs_allocationPKDv4_jjj", (void *)&SC_SetElementAt2_uint4, true },
    { "_Z19rsSetElementAt_uint13rs_allocationPKjjjj", (void *)&SC_SetElementAt3_uint, true },
    { "_Z20rsSetElementAt_uint213rs_allocationPKDv2_jjjj", (void *)&SC_SetElementAt3_uint2, true },
    { "_Z20rsSetElementAt_uint313rs_allocationPKDv3_jjjj", (void *)&SC_SetElementAt3_uint3, true },
    { "_Z20rsSetElementAt_uint413rs_allocationPKDv4_jjjj", (void *)&SC_SetElementAt3_uint4, true },

    { "_Z18rsSetElementAt_int13rs_allocationPKij", (void *)&SC_SetElementAt1_int, true },
    { "_Z19rsSetElementAt_int213rs_allocationPKDv2_ij", (void *)&SC_SetElementAt1_int2, true },
    { "_Z19rsSetElementAt_int313rs_allocationPKDv3_ij", (void *)&SC_SetElementAt1_int3, true },
    { "_Z19rsSetElementAt_int413rs_allocationPKDv4_ij", (void *)&SC_SetElementAt1_int4, true },
    { "_Z18rsSetElementAt_int13rs_allocationPKijj", (void *)&SC_SetElementAt2_int, true },
    { "_Z19rsSetElementAt_int213rs_allocationPKDv2_ijj", (void *)&SC_SetElementAt2_int2, true },
    { "_Z19rsSetElementAt_int313rs_allocationPKDv3_ijj", (void *)&SC_SetElementAt2_int3, true },
    { "_Z19rsSetElementAt_int413rs_allocationPKDv4_ijj", (void *)&SC_SetElementAt2_int4, true },
    { "_Z18rsSetElementAt_int13rs_allocationPKijjj", (void *)&SC_SetElementAt3_int, true },
    { "_Z19rsSetElementAt_int213rs_allocationPKDv2_ijjj", (void *)&SC_SetElementAt3_int2, true },
    { "_Z19rsSetElementAt_int313rs_allocationPKDv3_ijjj", (void *)&SC_SetElementAt3_int3, true },
    { "_Z19rsSetElementAt_int413rs_allocationPKDv4_ijjj", (void *)&SC_SetElementAt3_int4, true },

    { "_Z20rsSetElementAt_ulong13rs_allocationPKmt", (void *)&SC_SetElementAt1_ulong, true },
    { "_Z21rsSetElementAt_ulong213rs_allocationPKDv2_mj", (void *)&SC_SetElementAt1_ulong2, true },
    { "_Z21rsSetElementAt_ulong313rs_allocationPKDv3_mj", (void *)&SC_SetElementAt1_ulong3, true },
    { "_Z21rsSetElementAt_ulong413rs_allocationPKDv4_mj", (void *)&SC_SetElementAt1_ulong4, true },
    { "_Z20rsSetElementAt_ulong13rs_allocationPKmjj", (void *)&SC_SetElementAt2_ulong, true },
    { "_Z21rsSetElementAt_ulong213rs_allocationPKDv2_mjj", (void *)&SC_SetElementAt2_ulong2, true },
    { "_Z21rsSetElementAt_ulong313rs_allocationPKDv3_mjj", (void *)&SC_SetElementAt2_ulong3, true },
    { "_Z21rsSetElementAt_ulong413rs_allocationPKDv4_mjj", (void *)&SC_SetElementAt2_ulong4, true },
    { "_Z20rsSetElementAt_ulong13rs_allocationPKmjjj", (void *)&SC_SetElementAt3_ulong, true },
    { "_Z21rsSetElementAt_ulong213rs_allocationPKDv2_mjjj", (void *)&SC_SetElementAt3_ulong2, true },
    { "_Z21rsSetElementAt_ulong313rs_allocationPKDv3_mjjj", (void *)&SC_SetElementAt3_ulong3, true },
    { "_Z21rsSetElementAt_ulong413rs_allocationPKDv4_mjjj", (void *)&SC_SetElementAt3_ulong4, true },

    { "_Z19rsSetElementAt_long13rs_allocationPKlj", (void *)&SC_SetElementAt1_long, true },
    { "_Z20rsSetElementAt_long213rs_allocationPKDv2_lj", (void *)&SC_SetElementAt1_long2, true },
    { "_Z20rsSetElementAt_long313rs_allocationPKDv3_lj", (void *)&SC_SetElementAt1_long3, true },
    { "_Z20rsSetElementAt_long413rs_allocationPKDv4_lj", (void *)&SC_SetElementAt1_long4, true },
    { "_Z19rsSetElementAt_long13rs_allocationPKljj", (void *)&SC_SetElementAt2_long, true },
    { "_Z20rsSetElementAt_long213rs_allocationPKDv2_ljj", (void *)&SC_SetElementAt2_long2, true },
    { "_Z20rsSetElementAt_long313rs_allocationPKDv3_ljj", (void *)&SC_SetElementAt2_long3, true },
    { "_Z20rsSetElementAt_long413rs_allocationPKDv4_ljj", (void *)&SC_SetElementAt2_long4, true },
    { "_Z19rsSetElementAt_long13rs_allocationPKljjj", (void *)&SC_SetElementAt3_long, true },
    { "_Z20rsSetElementAt_long213rs_allocationPKDv2_ljjj", (void *)&SC_SetElementAt3_long2, true },
    { "_Z20rsSetElementAt_long313rs_allocationPKDv3_ljjj", (void *)&SC_SetElementAt3_long3, true },
    { "_Z20rsSetElementAt_long413rs_allocationPKDv4_ljjj", (void *)&SC_SetElementAt3_long4, true },

    { "_Z20rsSetElementAt_float13rs_allocationPKft", (void *)&SC_SetElementAt1_float, true },
    { "_Z21rsSetElementAt_float213rs_allocationPKDv2_fj", (void *)&SC_SetElementAt1_float2, true },
    { "_Z21rsSetElementAt_float313rs_allocationPKDv3_fj", (void *)&SC_SetElementAt1_float3, true },
    { "_Z21rsSetElementAt_float413rs_allocationPKDv4_fj", (void *)&SC_SetElementAt1_float4, true },
    { "_Z20rsSetElementAt_float13rs_allocationPKfjj", (void *)&SC_SetElementAt2_float, true },
    { "_Z21rsSetElementAt_float213rs_allocationPKDv2_fjj", (void *)&SC_SetElementAt2_float2, true },
    { "_Z21rsSetElementAt_float313rs_allocationPKDv3_fjj", (void *)&SC_SetElementAt2_float3, true },
    { "_Z21rsSetElementAt_float413rs_allocationPKDv4_fjj", (void *)&SC_SetElementAt2_float4, true },
    { "_Z20rsSetElementAt_float13rs_allocationPKfjjj", (void *)&SC_SetElementAt3_float, true },
    { "_Z21rsSetElementAt_float213rs_allocationPKDv2_fjjj", (void *)&SC_SetElementAt3_float2, true },
    { "_Z21rsSetElementAt_float313rs_allocationPKDv3_fjjj", (void *)&SC_SetElementAt3_float3, true },
    { "_Z21rsSetElementAt_float413rs_allocationPKDv4_fjjj", (void *)&SC_SetElementAt3_float4, true },

    { "_Z21rsSetElementAt_double13rs_allocationPKdt", (void *)&SC_SetElementAt1_double, true },
    { "_Z22rsSetElementAt_double213rs_allocationPKDv2_dj", (void *)&SC_SetElementAt1_double2, true },
    { "_Z22rsSetElementAt_double313rs_allocationPKDv3_dj", (void *)&SC_SetElementAt1_double3, true },
    { "_Z22rsSetElementAt_double413rs_allocationPKDv4_dj", (void *)&SC_SetElementAt1_double4, true },
    { "_Z21rsSetElementAt_double13rs_allocationPKdjj", (void *)&SC_SetElementAt2_double, true },
    { "_Z22rsSetElementAt_double213rs_allocationPKDv2_djj", (void *)&SC_SetElementAt2_double2, true },
    { "_Z22rsSetElementAt_double313rs_allocationPKDv3_djj", (void *)&SC_SetElementAt2_double3, true },
    { "_Z22rsSetElementAt_double413rs_allocationPKDv4_djj", (void *)&SC_SetElementAt2_double4, true },
    { "_Z21rsSetElementAt_double13rs_allocationPKdjjj", (void *)&SC_SetElementAt3_double, true },
    { "_Z22rsSetElementAt_double213rs_allocationPKDv2_djjj", (void *)&SC_SetElementAt3_double2, true },
    { "_Z22rsSetElementAt_double313rs_allocationPKDv3_djjj", (void *)&SC_SetElementAt3_double3, true },
    { "_Z22rsSetElementAt_double413rs_allocationPKDv4_djjj", (void *)&SC_SetElementAt3_double4, true },


    // Refcounting
    { "_Z11rsSetObjectP10rs_elementS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP10rs_element", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject10rs_element", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP7rs_typeS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP7rs_type", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject7rs_type", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP13rs_allocationS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP13rs_allocation", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject13rs_allocation", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP10rs_samplerS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP10rs_sampler", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject10rs_sampler", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP9rs_scriptS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP9rs_script", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject9rs_script", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP7rs_pathS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP7rs_path", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject7rs_path", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP7rs_meshS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP7rs_mesh", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject7rs_mesh", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP19rs_program_fragmentS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP19rs_program_fragment", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject19rs_program_fragment", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP17rs_program_vertexS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP17rs_program_vertex", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject17rs_program_vertex", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP17rs_program_rasterS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP17rs_program_raster", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject17rs_program_raster", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP16rs_program_storeS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP16rs_program_store", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject16rs_program_store", (void *)&SC_IsObject, true },

    { "_Z11rsSetObjectP7rs_fontS_", (void *)&SC_SetObject, true },
    { "_Z13rsClearObjectP7rs_font", (void *)&SC_ClearObject, true },
    { "_Z10rsIsObject7rs_font", (void *)&SC_IsObject, true },

    // Allocation ops
    { "_Z21rsAllocationMarkDirty13rs_allocation", (void *)&SC_AllocationSyncAll, true },
    { "_Z20rsgAllocationSyncAll13rs_allocation", (void *)&SC_AllocationSyncAll, false },
    { "_Z20rsgAllocationSyncAll13rs_allocationj", (void *)&SC_AllocationSyncAll2, false },
    { "_Z20rsgAllocationSyncAll13rs_allocation24rs_allocation_usage_type", (void *)&SC_AllocationSyncAll2, false },
    { "_Z15rsGetAllocationPKv", (void *)&SC_GetAllocation, true },
#ifndef RS_COMPATIBILITY_LIB
    { "_Z18rsAllocationIoSend13rs_allocation", (void *)&SC_AllocationIoSend, false },
    { "_Z21rsAllocationIoReceive13rs_allocation", (void *)&SC_AllocationIoReceive, false },
#endif
    { "_Z23rsAllocationCopy1DRange13rs_allocationjjjS_jj", (void *)&SC_AllocationCopy1DRange, false },
    { "_Z23rsAllocationCopy2DRange13rs_allocationjjj26rs_allocation_cubemap_facejjS_jjjS0_", (void *)&SC_AllocationCopy2DRange, false },

    // Messaging

    { "_Z14rsSendToClienti", (void *)&SC_ToClient, false },
    { "_Z14rsSendToClientiPKvj", (void *)&SC_ToClient2, false },
    { "_Z22rsSendToClientBlockingi", (void *)&SC_ToClientBlocking, false },
    { "_Z22rsSendToClientBlockingiPKvj", (void *)&SC_ToClientBlocking2, false },
#ifndef RS_COMPATIBILITY_LIB
    { "_Z22rsgBindProgramFragment19rs_program_fragment", (void *)&SC_BindProgramFragment, false },
    { "_Z19rsgBindProgramStore16rs_program_store", (void *)&SC_BindProgramStore, false },
    { "_Z20rsgBindProgramVertex17rs_program_vertex", (void *)&SC_BindProgramVertex, false },
    { "_Z20rsgBindProgramRaster17rs_program_raster", (void *)&SC_BindProgramRaster, false },
    { "_Z14rsgBindSampler19rs_program_fragmentj10rs_sampler", (void *)&SC_BindSampler, false },
    { "_Z14rsgBindTexture19rs_program_fragmentj13rs_allocation", (void *)&SC_BindTexture, false },
    { "_Z15rsgBindConstant19rs_program_fragmentj13rs_allocation", (void *)&SC_BindFragmentConstant, false },
    { "_Z15rsgBindConstant17rs_program_vertexj13rs_allocation", (void *)&SC_BindVertexConstant, false },

    { "_Z36rsgProgramVertexLoadProjectionMatrixPK12rs_matrix4x4", (void *)&SC_VpLoadProjectionMatrix, false },
    { "_Z31rsgProgramVertexLoadModelMatrixPK12rs_matrix4x4", (void *)&SC_VpLoadModelMatrix, false },
    { "_Z33rsgProgramVertexLoadTextureMatrixPK12rs_matrix4x4", (void *)&SC_VpLoadTextureMatrix, false },

    { "_Z35rsgProgramVertexGetProjectionMatrixP12rs_matrix4x4", (void *)&SC_VpGetProjectionMatrix, false },

    { "_Z31rsgProgramFragmentConstantColor19rs_program_fragmentffff", (void *)&SC_PfConstantColor, false },

    { "_Z11rsgGetWidthv", (void *)&SC_GetWidth, false },
    { "_Z12rsgGetHeightv", (void *)&SC_GetHeight, false },


    { "_Z11rsgDrawRectfffff", (void *)&SC_DrawRect, false },
    { "_Z11rsgDrawQuadffffffffffff", (void *)&SC_DrawQuad, false },
    { "_Z20rsgDrawQuadTexCoordsffffffffffffffffffff", (void *)&SC_DrawQuadTexCoords, false },
    { "_Z24rsgDrawSpriteScreenspacefffff", (void *)&SC_DrawSpriteScreenspace, false },

    { "_Z11rsgDrawMesh7rs_mesh", (void *)&SC_DrawMesh, false },
    { "_Z11rsgDrawMesh7rs_meshj", (void *)&SC_DrawMeshPrimitive, false },
    { "_Z11rsgDrawMesh7rs_meshjjj", (void *)&SC_DrawMeshPrimitiveRange, false },
    { "_Z25rsgMeshComputeBoundingBox7rs_meshPfS0_S0_S0_S0_S0_", (void *)&SC_MeshComputeBoundingBox, false },

    { "_Z11rsgDrawPath7rs_path", (void *)&SC_DrawPath, false },

    { "_Z13rsgClearColorffff", (void *)&SC_ClearColor, false },
    { "_Z13rsgClearDepthf", (void *)&SC_ClearDepth, false },

    { "_Z11rsgDrawTextPKcii", (void *)&SC_DrawText, false },
    { "_Z11rsgDrawText13rs_allocationii", (void *)&SC_DrawTextAlloc, false },
    { "_Z14rsgMeasureTextPKcPiS1_S1_S1_", (void *)&SC_MeasureText, false },
    { "_Z14rsgMeasureText13rs_allocationPiS0_S0_S0_", (void *)&SC_MeasureTextAlloc, false },

    { "_Z11rsgBindFont7rs_font", (void *)&SC_BindFont, false },
    { "_Z12rsgFontColorffff", (void *)&SC_FontColor, false },

    { "_Z18rsgBindColorTarget13rs_allocationj", (void *)&SC_BindFrameBufferObjectColorTarget, false },
    { "_Z18rsgBindDepthTarget13rs_allocation", (void *)&SC_BindFrameBufferObjectDepthTarget, false },
    { "_Z19rsgClearColorTargetj", (void *)&SC_ClearFrameBufferObjectColorTarget, false },
    { "_Z19rsgClearDepthTargetv", (void *)&SC_ClearFrameBufferObjectDepthTarget, false },
    { "_Z24rsgClearAllRenderTargetsv", (void *)&SC_ClearFrameBufferObjectTargets, false },
#endif // RS_COMPATIBILITY_LIB

    { "_Z9rsForEach9rs_script13rs_allocationS0_", (void *)&SC_ForEach_SAA, true },
    { "_Z9rsForEach9rs_script13rs_allocationS0_PKv", (void *)&SC_ForEach_SAAU, true },
    { "_Z9rsForEach9rs_script13rs_allocationS0_PKvPK14rs_script_call", (void *)&SC_ForEach_SAAUS, true },
    { "_Z9rsForEach9rs_script13rs_allocationS0_PKvj", (void *)&SC_ForEach_SAAUL, true },
    { "_Z9rsForEach9rs_script13rs_allocationS0_PKvjPK14rs_script_call", (void *)&SC_ForEach_SAAULS, true },

    // time
    { "_Z6rsTimePi", (void *)&SC_Time, true },
    { "_Z11rsLocaltimeP5rs_tmPKi", (void *)&SC_LocalTime, true },
    { "_Z14rsUptimeMillisv", (void*)&SC_UptimeMillis, true },
    { "_Z13rsUptimeNanosv", (void*)&SC_UptimeNanos, true },
    { "_Z7rsGetDtv", (void*)&SC_GetDt, false },

    // misc
#ifndef RS_COMPATIBILITY_LIB
    { "_Z5colorffff", (void *)&SC_Color, false },
    { "_Z9rsgFinishv", (void *)&SC_Finish, false },
#endif

    { NULL, NULL, false }
};

#ifdef RS_COMPATIBILITY_LIB

//////////////////////////////////////////////////////////////////////////////
// Compatibility Library entry points
//////////////////////////////////////////////////////////////////////////////

bool rsIsObject(rs_element src) {
    return SC_IsObject((ObjectBase*)src.p);
}

#define CLEAR_SET_OBJ(t) \
    void __attribute__((overloadable)) rsClearObject(t *dst) { \
    return SC_ClearObject((ObjectBase**) dst); \
    } \
    void __attribute__((overloadable)) rsSetObject(t *dst, t src) { \
    return SC_SetObject((ObjectBase**) dst, (ObjectBase*) src.p); \
    }

CLEAR_SET_OBJ(rs_element)
CLEAR_SET_OBJ(rs_type)
CLEAR_SET_OBJ(rs_allocation)
CLEAR_SET_OBJ(rs_sampler)
CLEAR_SET_OBJ(rs_script)
#undef CLEAR_SET_OBJ

const Allocation * rsGetAllocation(const void *ptr) {
    return SC_GetAllocation(ptr);
}

void __attribute__((overloadable)) rsForEach(rs_script script,
                                             rs_allocation in,
                                             rs_allocation out,
                                             const void *usr,
                                             const rs_script_call *call) {
    return SC_ForEach_SAAUS((Script *)script.p, (Allocation*)in.p, (Allocation*)out.p, usr, (RsScriptCall*)call);
}

void __attribute__((overloadable)) rsForEach(rs_script script,
                                             rs_allocation in,
                                             rs_allocation out,
                                             const void *usr,
                                             uint32_t usrLen,
                                             const rs_script_call *call) {
    return SC_ForEach_SAAULS((Script *)script.p, (Allocation*)in.p, (Allocation*)out.p, usr, usrLen, (RsScriptCall*)call);
}

int rsTime(int *timer) {
    return SC_Time(timer);
}

rs_tm* rsLocaltime(rs_tm* local, const int *timer) {
    return (rs_tm*)(SC_LocalTime((tm*)local, (long*)timer));
}

int64_t rsUptimeMillis() {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrUptimeMillis(rsc);
}

uint32_t rsSendToClientBlocking2(int cmdID, void *data, int len) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrToClientBlocking(rsc, cmdID, data, len);
}

uint32_t rsSendToClientBlocking(int cmdID) {
    Context *rsc = RsdCpuReference::getTlsContext();
    return rsrToClientBlocking(rsc, cmdID, NULL, 0);
}

static void SC_debugF(const char *s, float f) {
    ALOGD("%s %f, 0x%08x", s, f, *((int *) (&f)));
}
static void SC_debugFv2(const char *s, float f1, float f2) {
    ALOGD("%s {%f, %f}", s, f1, f2);
}
static void SC_debugFv3(const char *s, float f1, float f2, float f3) {
    ALOGD("%s {%f, %f, %f}", s, f1, f2, f3);
}
static void SC_debugFv4(const char *s, float f1, float f2, float f3, float f4) {
    ALOGD("%s {%f, %f, %f, %f}", s, f1, f2, f3, f4);
}
static void SC_debugF2(const char *s, float2 f) {
    ALOGD("%s {%f, %f}", s, f.x, f.y);
}
static void SC_debugF3(const char *s, float3 f) {
    ALOGD("%s {%f, %f, %f}", s, f.x, f.y, f.z);
}
static void SC_debugF4(const char *s, float4 f) {
    ALOGD("%s {%f, %f, %f, %f}", s, f.x, f.y, f.z, f.w);
}
static void SC_debugD(const char *s, double d) {
    ALOGD("%s %f, 0x%08llx", s, d, *((long long *) (&d)));
}
static void SC_debugFM4v4(const char *s, const float *f) {
    ALOGD("%s {%f, %f, %f, %f", s, f[0], f[4], f[8], f[12]);
    ALOGD("%s  %f, %f, %f, %f", s, f[1], f[5], f[9], f[13]);
    ALOGD("%s  %f, %f, %f, %f", s, f[2], f[6], f[10], f[14]);
    ALOGD("%s  %f, %f, %f, %f}", s, f[3], f[7], f[11], f[15]);
}
static void SC_debugFM3v3(const char *s, const float *f) {
    ALOGD("%s {%f, %f, %f", s, f[0], f[3], f[6]);
    ALOGD("%s  %f, %f, %f", s, f[1], f[4], f[7]);
    ALOGD("%s  %f, %f, %f}",s, f[2], f[5], f[8]);
}
static void SC_debugFM2v2(const char *s, const float *f) {
    ALOGD("%s {%f, %f", s, f[0], f[2]);
    ALOGD("%s  %f, %f}",s, f[1], f[3]);
}
static void SC_debugI8(const char *s, char c) {
    ALOGD("%s %hhd  0x%hhx", s, c, (unsigned char)c);
}
static void SC_debugC2(const char *s, char2 c) {
    ALOGD("%s {%hhd, %hhd}  0x%hhx 0x%hhx", s, c.x, c.y, (unsigned char)c.x, (unsigned char)c.y);
}
static void SC_debugC3(const char *s, char3 c) {
    ALOGD("%s {%hhd, %hhd, %hhd}  0x%hhx 0x%hhx 0x%hhx", s, c.x, c.y, c.z, (unsigned char)c.x, (unsigned char)c.y, (unsigned char)c.z);
}
static void SC_debugC4(const char *s, char4 c) {
    ALOGD("%s {%hhd, %hhd, %hhd, %hhd}  0x%hhx 0x%hhx 0x%hhx 0x%hhx", s, c.x, c.y, c.z, c.w, (unsigned char)c.x, (unsigned char)c.y, (unsigned char)c.z, (unsigned char)c.w);
}
static void SC_debugU8(const char *s, unsigned char c) {
    ALOGD("%s %hhu  0x%hhx", s, c, c);
}
static void SC_debugUC2(const char *s, uchar2 c) {
    ALOGD("%s {%hhu, %hhu}  0x%hhx 0x%hhx", s, c.x, c.y, c.x, c.y);
}
static void SC_debugUC3(const char *s, uchar3 c) {
    ALOGD("%s {%hhu, %hhu, %hhu}  0x%hhx 0x%hhx 0x%hhx", s, c.x, c.y, c.z, c.x, c.y, c.z);
}
static void SC_debugUC4(const char *s, uchar4 c) {
    ALOGD("%s {%hhu, %hhu, %hhu, %hhu}  0x%hhx 0x%hhx 0x%hhx 0x%hhx", s, c.x, c.y, c.z, c.w, c.x, c.y, c.z, c.w);
}
static void SC_debugI16(const char *s, short c) {
    ALOGD("%s %hd  0x%hx", s, c, c);
}
static void SC_debugS2(const char *s, short2 c) {
    ALOGD("%s {%hd, %hd}  0x%hx 0x%hx", s, c.x, c.y, c.x, c.y);
}
static void SC_debugS3(const char *s, short3 c) {
    ALOGD("%s {%hd, %hd, %hd}  0x%hx 0x%hx 0x%hx", s, c.x, c.y, c.z, c.x, c.y, c.z);
}
static void SC_debugS4(const char *s, short4 c) {
    ALOGD("%s {%hd, %hd, %hd, %hd}  0x%hx 0x%hx 0x%hx 0x%hx", s, c.x, c.y, c.z, c.w, c.x, c.y, c.z, c.w);
}
static void SC_debugU16(const char *s, unsigned short c) {
    ALOGD("%s %hu  0x%hx", s, c, c);
}
static void SC_debugUS2(const char *s, ushort2 c) {
    ALOGD("%s {%hu, %hu}  0x%hx 0x%hx", s, c.x, c.y, c.x, c.y);
}
static void SC_debugUS3(const char *s, ushort3 c) {
    ALOGD("%s {%hu, %hu, %hu}  0x%hx 0x%hx 0x%hx", s, c.x, c.y, c.z, c.x, c.y, c.z);
}
static void SC_debugUS4(const char *s, ushort4 c) {
    ALOGD("%s {%hu, %hu, %hu, %hu}  0x%hx 0x%hx 0x%hx 0x%hx", s, c.x, c.y, c.z, c.w, c.x, c.y, c.z, c.w);
}
static void SC_debugI32(const char *s, int32_t i) {
    ALOGD("%s %d  0x%x", s, i, i);
}
static void SC_debugI2(const char *s, int2 i) {
    ALOGD("%s {%d, %d}  0x%x 0x%x", s, i.x, i.y, i.x, i.y);
}
static void SC_debugI3(const char *s, int3 i) {
    ALOGD("%s {%d, %d, %d}  0x%x 0x%x 0x%x", s, i.x, i.y, i.z, i.x, i.y, i.z);
}
static void SC_debugI4(const char *s, int4 i) {
    ALOGD("%s {%d, %d, %d, %d}  0x%x 0x%x 0x%x 0x%x", s, i.x, i.y, i.z, i.w, i.x, i.y, i.z, i.w);
}
static void SC_debugU32(const char *s, uint32_t i) {
    ALOGD("%s %u  0x%x", s, i, i);
}
static void SC_debugUI2(const char *s, uint2 i) {
    ALOGD("%s {%u, %u}  0x%x 0x%x", s, i.x, i.y, i.x, i.y);
}
static void SC_debugUI3(const char *s, uint3 i) {
    ALOGD("%s {%u, %u, %u}  0x%x 0x%x 0x%x", s, i.x, i.y, i.z, i.x, i.y, i.z);
}
static void SC_debugUI4(const char *s, uint4 i) {
    ALOGD("%s {%u, %u, %u, %u}  0x%x 0x%x 0x%x 0x%x", s, i.x, i.y, i.z, i.w, i.x, i.y, i.z, i.w);
}
static void SC_debugLL64(const char *s, long long ll) {
    ALOGD("%s %lld  0x%llx", s, ll, ll);
}
static void SC_debugL2(const char *s, long2 ll) {
    ALOGD("%s {%lld, %lld}  0x%llx 0x%llx", s, ll.x, ll.y, ll.x, ll.y);
}
static void SC_debugL3(const char *s, long3 ll) {
    ALOGD("%s {%lld, %lld, %lld}  0x%llx 0x%llx 0x%llx", s, ll.x, ll.y, ll.z, ll.x, ll.y, ll.z);
}
static void SC_debugL4(const char *s, long4 ll) {
    ALOGD("%s {%lld, %lld, %lld, %lld}  0x%llx 0x%llx 0x%llx 0x%llx", s, ll.x, ll.y, ll.z, ll.w, ll.x, ll.y, ll.z, ll.w);
}
static void SC_debugULL64(const char *s, unsigned long long ll) {
    ALOGD("%s %llu  0x%llx", s, ll, ll);
}
static void SC_debugUL2(const char *s, ulong2 ll) {
    ALOGD("%s {%llu, %llu}  0x%llx 0x%llx", s, ll.x, ll.y, ll.x, ll.y);
}
static void SC_debugUL3(const char *s, ulong3 ll) {
    ALOGD("%s {%llu, %llu, %llu}  0x%llx 0x%llx 0x%llx", s, ll.x, ll.y, ll.z, ll.x, ll.y, ll.z);
}
static void SC_debugUL4(const char *s, ulong4 ll) {
    ALOGD("%s {%llu, %llu, %llu, %llu}  0x%llx 0x%llx 0x%llx 0x%llx", s, ll.x, ll.y, ll.z, ll.w, ll.x, ll.y, ll.z, ll.w);
}
static void SC_debugP(const char *s, const void *p) {
    ALOGD("%s %p", s, p);
}

// TODO: allocation ops, messaging, time

void rsDebug(const char *s, float f) {
    SC_debugF(s, f);
}

void rsDebug(const char *s, float f1, float f2) {
    SC_debugFv2(s, f1, f2);
}

void rsDebug(const char *s, float f1, float f2, float f3) {
    SC_debugFv3(s, f1, f2, f3);
}

void rsDebug(const char *s, float f1, float f2, float f3, float f4) {
    SC_debugFv4(s, f1, f2, f3, f4);
}

void rsDebug(const char *s, const float2 *f) {
    SC_debugF2(s, *f);
}

void rsDebug(const char *s, const float3 *f) {
    SC_debugF3(s, *f);
}

void rsDebug(const char *s, const float4 *f) {
    SC_debugF4(s, *f);
}

void rsDebug(const char *s, double d) {
    SC_debugD(s, d);
}

void rsDebug(const char *s, const rs_matrix4x4 *m) {
    SC_debugFM4v4(s, (float *) m);
}

void rsDebug(const char *s, const rs_matrix3x3 *m) {
    SC_debugFM3v3(s, (float *) m);
}

void rsDebug(const char *s, const rs_matrix2x2 *m) {
    SC_debugFM2v2(s, (float *) m);
}

void rsDebug(const char *s, char c) {
    SC_debugI8(s, c);
}

void rsDebug(const char *s, const char2 *c) {
    SC_debugC2(s, *c);
}

void rsDebug(const char *s, const char3 *c) {
    SC_debugC3(s, *c);
}

void rsDebug(const char *s, const char4 *c) {
    SC_debugC4(s, *c);
}

void rsDebug(const char *s, unsigned char c) {
    SC_debugU8(s, c);
}

void rsDebug(const char *s, const uchar2 *c) {
    SC_debugUC2(s, *c);
}

void rsDebug(const char *s, const uchar3 *c) {
    SC_debugUC3(s, *c);
}

void rsDebug(const char *s, const uchar4 *c) {
    SC_debugUC4(s, *c);
}

void rsDebug(const char *s, short c) {
    SC_debugI16(s, c);
}

void rsDebug(const char *s, const short2 *c) {
    SC_debugS2(s, *c);
}

void rsDebug(const char *s, const short3 *c) {
    SC_debugS3(s, *c);
}

void rsDebug(const char *s, const short4 *c) {
    SC_debugS4(s, *c);
}

void rsDebug(const char *s, unsigned short c) {
    SC_debugU16(s, c);
}

void rsDebug(const char *s, const ushort2 *c) {
    SC_debugUS2(s, *c);
}

void rsDebug(const char *s, const ushort3 *c) {
    SC_debugUS3(s, *c);
}

void rsDebug(const char *s, const ushort4 *c) {
    SC_debugUS4(s, *c);
}

void rsDebug(const char *s, int c) {
    SC_debugI32(s, c);
}

void rsDebug(const char *s, const int2 *c) {
    SC_debugI2(s, *c);
}

void rsDebug(const char *s, const int3 *c) {
    SC_debugI3(s, *c);
}

void rsDebug(const char *s, const int4 *c) {
    SC_debugI4(s, *c);
}

void rsDebug(const char *s, unsigned int c) {
    SC_debugU32(s, c);
}

void rsDebug(const char *s, const uint2 *c) {
    SC_debugUI2(s, *c);
}

void rsDebug(const char *s, const uint3 *c) {
    SC_debugUI3(s, *c);
}

void rsDebug(const char *s, const uint4 *c) {
    SC_debugUI4(s, *c);
}

void rsDebug(const char *s, long c) {
    SC_debugLL64(s, c);
}

void rsDebug(const char *s, long long c) {
    SC_debugLL64(s, c);
}

void rsDebug(const char *s, const long2 *c) {
    SC_debugL2(s, *c);
}

void rsDebug(const char *s, const long3 *c) {
    SC_debugL3(s, *c);
}

void rsDebug(const char *s, const long4 *c) {
    SC_debugL4(s, *c);
}

void rsDebug(const char *s, unsigned long c) {
    SC_debugULL64(s, c);
}

void rsDebug(const char *s, unsigned long long c) {
    SC_debugULL64(s, c);
}

void rsDebug(const char *s, const ulong2 *c) {
    SC_debugUL2(s, *c);
}

void rsDebug(const char *s, const ulong3 *c) {
    SC_debugUL3(s, *c);
}

void rsDebug(const char *s, const ulong4 *c) {
    SC_debugUL4(s, *c);
}

void rsDebug(const char *s, const void *p) {
    SC_debugP(s, p);
}
#endif // RS_COMPATIBILITY_LIB

extern const RsdCpuReference::CpuSymbol * rsdLookupRuntimeStub(Context * pContext, char const* name) {
    ScriptC *s = (ScriptC *)pContext;
    const RsdCpuReference::CpuSymbol *syms = gSyms;
    const RsdCpuReference::CpuSymbol *sym = NULL;

    if (!sym) {
        while (syms->fnPtr) {
            if (!strcmp(syms->name, name)) {
                return syms;
            }
            syms++;
        }
    }

    return NULL;
}


