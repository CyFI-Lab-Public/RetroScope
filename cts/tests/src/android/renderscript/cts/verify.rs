/*
 * Copyright (C) 2013 The Android Open Source Project
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

#include "shared.rsh"

int gAllowedIntError = 0;
static bool hadError = false;
static int2 errorLoc = {0,0};


static bool compare_float(float f1, float f2) {
    if (fabs(f1-f2) > 0.0001f) {
        hadError = true;
        return false;
    }
    return true;
}

static bool verify_float4(rs_allocation in1, rs_allocation in2)
{
    uint32_t w = rsAllocationGetDimX(in1);
    uint32_t h = rsAllocationGetDimY(in1);
    for (uint32_t y=0; y < h; y++) {
        for (uint32_t x=0; x < w; x++) {
            float4 pref = rsGetElementAt_float4(in1, x, y);
            float4 ptst = rsGetElementAt_float4(in2, x, y);
            bool e = !compare_float(pref.x, ptst.x);
            e |= !compare_float(pref.y, ptst.y);
            e |= !compare_float(pref.z, ptst.z);
            e |= !compare_float(pref.w, ptst.w);
            if (e) {
                errorLoc.x = x;
                errorLoc.y = y;
                return false;
            }
        }
    }
    return true;
}

static bool verify_float3(rs_allocation in1, rs_allocation in2)
{
    uint32_t w = rsAllocationGetDimX(in1);
    uint32_t h = rsAllocationGetDimY(in1);
    for (uint32_t y=0; y < h; y++) {
        for (uint32_t x=0; x < w; x++) {
            float3 pref = rsGetElementAt_float3(in1, x, y);
            float3 ptst = rsGetElementAt_float3(in2, x, y);
            bool e = !compare_float(pref.x, ptst.x);
            e |= !compare_float(pref.y, ptst.y);
            e |= !compare_float(pref.z, ptst.z);
            if (e) {
                errorLoc.x = x;
                errorLoc.y = y;
                return false;
            }
        }
    }
    return true;
}

static bool verify_float2(rs_allocation in1, rs_allocation in2)
{
    uint32_t w = rsAllocationGetDimX(in1);
    uint32_t h = rsAllocationGetDimY(in1);
    for (uint32_t y=0; y < h; y++) {
        for (uint32_t x=0; x < w; x++) {
            float2 pref = rsGetElementAt_float2(in1, x, y);
            float2 ptst = rsGetElementAt_float2(in2, x, y);
            bool e = !compare_float(pref.x, ptst.x);
            e |= !compare_float(pref.y, ptst.y);
            if (e) {
                errorLoc.x = x;
                errorLoc.y = y;
                return false;
            }
        }
    }
    return true;
}

static bool verify_float(rs_allocation in1, rs_allocation in2)
{
    uint32_t w = rsAllocationGetDimX(in1);
    uint32_t h = rsAllocationGetDimY(in1);
    for (uint32_t y=0; y < h; y++) {
        for (uint32_t x=0; x < w; x++) {
            float pref = rsGetElementAt_float(in1, x, y);
            float ptst = rsGetElementAt_float(in2, x, y);
            bool e = !compare_float(pref, ptst);
            if (e) {
                errorLoc.x = x;
                errorLoc.y = y;
                return false;
            }
        }
    }
    return true;
}

static bool verify_uchar4(rs_allocation in1, rs_allocation in2)
{
    int merr = 0;
    uint32_t w = rsAllocationGetDimX(in1);
    uint32_t h = rsAllocationGetDimY(in1);
    for (uint32_t y=0; y < h; y++) {
        for (uint32_t x=0; x < w; x++) {
            int4 pref = convert_int4(rsGetElementAt_uchar4(in1, x, y));
            int4 ptst = convert_int4(rsGetElementAt_uchar4(in2, x, y));
            int4 d = convert_int4(abs(pref - ptst));
            int e = 0;
            e = max(e, d.x);
            e = max(e, d.y);
            e = max(e, d.z);
            e = max(e, d.w);
            if (e > gAllowedIntError) {
                errorLoc.x = x;
                errorLoc.y = y;
                hadError = true;
                return false;
            }
            merr = max(e, merr);
        }
    }
    return true;
}

static bool verify_uchar3(rs_allocation in1, rs_allocation in2)
{
    int merr = 0;
    uint32_t w = rsAllocationGetDimX(in1);
    uint32_t h = rsAllocationGetDimY(in1);
    for (uint32_t y=0; y < h; y++) {
        for (uint32_t x=0; x < w; x++) {
            int3 pref = convert_int3(rsGetElementAt_uchar3(in1, x, y));
            int3 ptst = convert_int3(rsGetElementAt_uchar3(in2, x, y));
            int3 d = convert_int3(abs(pref - ptst));
            int e = 0;
            e = max(e, d.x);
            e = max(e, d.y);
            e = max(e, d.z);
            if (e > gAllowedIntError) {
                errorLoc.x = x;
                errorLoc.y = y;
                hadError = true;
                return false;
            }
            merr = max(e, merr);
        }
    }
    return true;
}

static bool verify_uchar2(rs_allocation in1, rs_allocation in2)
{
    int merr = 0;
    uint32_t w = rsAllocationGetDimX(in1);
    uint32_t h = rsAllocationGetDimY(in1);
    for (uint32_t y=0; y < h; y++) {
        for (uint32_t x=0; x < w; x++) {
            int2 pref = convert_int2(rsGetElementAt_uchar2(in1, x, y));
            int2 ptst = convert_int2(rsGetElementAt_uchar2(in2, x, y));
            int2 d = convert_int2(abs(pref - ptst));
            int e = 0;
            e = max(e, d.x);
            e = max(e, d.y);
            if (e > gAllowedIntError) {
                errorLoc.x = x;
                errorLoc.y = y;
                hadError = true;
                return false;
            }
            merr = max(e, merr);
        }
    }
    return true;
}

static bool verify_uchar(rs_allocation in1, rs_allocation in2)
{
    int merr = 0;
    uint32_t w = rsAllocationGetDimX(in1);
    uint32_t h = rsAllocationGetDimY(in1);
    for (uint32_t y=0; y < h; y++) {
        for (uint32_t x=0; x < w; x++) {
            int pref = rsGetElementAt_uchar(in1, x, y);
            int ptst = rsGetElementAt_uchar(in2, x, y);
            int e = abs(pref - ptst);
            if (e > gAllowedIntError) {
                errorLoc.x = x;
                errorLoc.y = y;
                hadError = true;
                return false;
            }
            merr = max(e, merr);
        }
    }
    return true;
}

#define printCell(txt, a, xy) \
{                       \
    rs_element e = rsAllocationGetElement(a); \
    rs_data_type dt = rsElementGetDataType(e); \
    uint32_t vs = rsElementGetVectorSize(e); \
 \
    if (dt == RS_TYPE_UNSIGNED_8) { \
        switch(vs) { \
        case 4: \
            rsDebug(txt, rsGetElementAt_uchar4(a, xy.x, xy.y)); \
            break; \
        case 3: \
            rsDebug(txt, rsGetElementAt_uchar3(a, xy.x, xy.y)); \
            break; \
        case 2: \
            rsDebug(txt, rsGetElementAt_uchar2(a, xy.x, xy.y)); \
            break; \
        case 1: \
            rsDebug(txt, rsGetElementAt_uchar(a, xy.x, xy.y)); \
            break; \
        } \
    } else { \
        switch(vs) { \
        case 4: \
            rsDebug(txt, rsGetElementAt_float4(a, xy.x, xy.y)); \
            break; \
        case 3: \
            rsDebug(txt, rsGetElementAt_float3(a, xy.x, xy.y)); \
            break; \
        case 2: \
            rsDebug(txt, rsGetElementAt_float2(a, xy.x, xy.y)); \
            break; \
        case 1: \
            rsDebug(txt, rsGetElementAt_float(a, xy.x, xy.y)); \
            break; \
        } \
    } \
}

void verify(rs_allocation ref_in, rs_allocation tst_in, rs_allocation src_in)
{
    rs_element e = rsAllocationGetElement(ref_in);
    rs_data_type dt = rsElementGetDataType(e);
    uint32_t vs = rsElementGetVectorSize(e);
    bool valid = false;

    if (dt == RS_TYPE_UNSIGNED_8) {
        switch(vs) {
        case 4:
            valid = verify_uchar4(ref_in, tst_in);
            break;
        case 3:
            valid = verify_uchar3(ref_in, tst_in);
            break;
        case 2:
            valid = verify_uchar2(ref_in, tst_in);
            break;
        case 1:
            valid = verify_uchar(ref_in, tst_in);
            break;
        }
    } else {
        switch(vs) {
        case 4:
            valid = verify_float4(ref_in, tst_in);
            break;
        case 3:
            valid = verify_float3(ref_in, tst_in);
            break;
        case 2:
            valid = verify_float2(ref_in, tst_in);
            break;
        case 1:
            valid = verify_float(ref_in, tst_in);
            break;
        }
    }
    if (!valid) {
        rsDebug("verify failure at xy", errorLoc);
        printCell("start value     ", src_in, errorLoc);
        printCell("reference value ", ref_in, errorLoc);
        printCell("test value      ", tst_in, errorLoc);
    }
}

void checkError()
{
    if (hadError) {
        rsSendToClientBlocking(RS_MSG_TEST_FAILED);
    } else {
        rsSendToClientBlocking(RS_MSG_TEST_PASSED);
    }
}
