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

#include <sys/mman.h>
#include <unistd.h>

#include "rsCpuIntrinsic.h"
#include "rsCpuIntrinsicInlines.h"
#include "linkloader/include/MemChunk.h"

#include <sys/mman.h>
#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>
//#include <utils/StopWatch.h>


/*  uint kernel
 *  Q0  D0:  Load slot for R
 *      D1:  Load slot for G
 *  Q1  D2:  Load slot for B
 *      D3:  Load slot for A
 *  Q2  D4:  Matrix
 *      D5:  =
 *  Q3  D6:  =
 *      D7:  =
 *  Q4  D8:  Add R
 *      D9:
 *  Q5  D10: Add G
 *      D11:
 *  Q6  D12: Add B
 *      D13:
 *  Q7  D14: Add A
 *      D15:
 *  Q8  D16:  I32: R Sum
 *      D17:
 *  Q9  D18:  I32: G Sum
 *      D19:
 *  Q10 D20:  I32: B Sum
 *      D21:
 *  Q11 D22:  I32: A Sum
 *      D23:
 *  Q12 D24:  U16: expanded R
 *      D25:
 *  Q13 D26:  U16: expanded G
 *      D27:
 *  Q14 D28:  U16: expanded B
 *      D29:
 *  Q15 D30:  U16: expanded A
 *      D31:
 *
 */

/*  float kernel
 *  Q0  D0:  Load slot for R
 *      D1:  =
 *  Q1  D2:  Load slot for G
 *      D3:  =
 *  Q2  D4:  Load slot for B
 *      D5:  =
 *  Q3  D6:  Load slot for A
 *      D7:  =
 *  Q4  D8:  Matrix
 *      D9:  =
 *  Q5  D10: =
 *      D11: =
 *  Q6  D12: =
 *      D13: =
 *  Q7  D14: =
 *      D15: =
 *  Q8  D16: Add R
 *      D17: =
 *  Q9  D18: Add G
 *      D19: =
 *  Q10 D20: Add B
 *      D21: =
 *  Q11 D22: Add A
 *      D23: =
 *  Q12 D24: Sum R
 *      D25: =
 *  Q13 D26: Sum G
 *      D27: =
 *  Q14 D28: Sum B
 *      D29: =
 *  Q15 D30: Sum A
 *      D31: =
 *
 */



using namespace android;
using namespace android::renderscript;

namespace android {
namespace renderscript {

typedef union {
    uint64_t key;
    struct {
        uint32_t inVecSize          :2;  // [0 - 1]
        uint32_t outVecSize         :2;  // [2 - 3]
        uint32_t inType             :4;  // [4 - 7]
        uint32_t outType            :4;  // [8 - 11]
        uint32_t dot                :1;  // [12]
        uint32_t _unused1           :1;  // [13]
        uint32_t copyAlpha          :1;  // [14]
        uint32_t _unused2           :1;  // [15]
        uint32_t coeffMask          :16; // [16-31]
        uint32_t addMask            :4;  // [32-35]
    } u;
} Key_t;

class RsdCpuScriptIntrinsicColorMatrix : public RsdCpuScriptIntrinsic {
public:
    virtual void populateScript(Script *);

    virtual void setGlobalVar(uint32_t slot, const void *data, size_t dataLength);

    virtual ~RsdCpuScriptIntrinsicColorMatrix();
    RsdCpuScriptIntrinsicColorMatrix(RsdCpuReferenceImpl *ctx, const Script *s, const Element *e);

    virtual void preLaunch(uint32_t slot, const Allocation * ain, Allocation * aout,
                           const void * usr, uint32_t usrLen, const RsScriptCall *sc);
    virtual void postLaunch(uint32_t slot, const Allocation * ain, Allocation * aout,
                            const void * usr, uint32_t usrLen, const RsScriptCall *sc);

protected:
    float fp[16];
    float fpa[4];

    // The following four fields are read as constants
    // by the SIMD assembly code.
    short ip[16];
    int ipa[16];
    float tmpFp[16];
    float tmpFpa[16];

    static void kernel(const RsForEachStubParamStruct *p,
                       uint32_t xstart, uint32_t xend,
                       uint32_t instep, uint32_t outstep);
    void updateCoeffCache(float fpMul, float addMul);

    Key_t mLastKey;
    unsigned char *mBuf;
    size_t mBufSize;

    Key_t computeKey(const Element *ein, const Element *eout);

    bool build(Key_t key);

    void (*mOptKernel)(void *dst, const void *src, const short *coef, uint32_t count);

};

}
}


Key_t RsdCpuScriptIntrinsicColorMatrix::computeKey(
        const Element *ein, const Element *eout) {

    Key_t key;
    key.key = 0;

    // Compute a unique code key for this operation

    // Add to the key the input and output types
    bool hasFloat = false;
    if (ein->getType() == RS_TYPE_FLOAT_32) {
        hasFloat = true;
        key.u.inType = RS_TYPE_FLOAT_32;
        rsAssert(key.u.inType == RS_TYPE_FLOAT_32);
    }
    if (eout->getType() == RS_TYPE_FLOAT_32) {
        hasFloat = true;
        key.u.outType = RS_TYPE_FLOAT_32;
        rsAssert(key.u.outType == RS_TYPE_FLOAT_32);
    }

    // Mask in the bits indicating which coefficients in the
    // color matrix are needed.
    if (hasFloat) {
        for (uint32_t i=0; i < 16; i++) {
            if (fabs(fp[i]) != 0.f) {
                key.u.coeffMask |= 1 << i;
            }
        }
        if (fabs(fpa[0]) != 0.f) key.u.addMask |= 0x1;
        if (fabs(fpa[1]) != 0.f) key.u.addMask |= 0x2;
        if (fabs(fpa[2]) != 0.f) key.u.addMask |= 0x4;
        if (fabs(fpa[3]) != 0.f) key.u.addMask |= 0x8;

    } else {
        for (uint32_t i=0; i < 16; i++) {
            if (ip[i] != 0) {
                key.u.coeffMask |= 1 << i;
            }
        }
        if (ipa[0] != 0) key.u.addMask |= 0x1;
        if (ipa[4] != 0) key.u.addMask |= 0x2;
        if (ipa[8] != 0) key.u.addMask |= 0x4;
        if (ipa[12] != 0) key.u.addMask |= 0x8;
    }

    // Look for a dot product where the r,g,b colums are the same
    if ((ip[0] == ip[1]) && (ip[0] == ip[2]) &&
        (ip[4] == ip[5]) && (ip[4] == ip[6]) &&
        (ip[8] == ip[9]) && (ip[8] == ip[10]) &&
        (ip[12] == ip[13]) && (ip[12] == ip[14])) {

        if (!key.u.addMask) key.u.dot = 1;
    }

    // Is alpha a simple copy
    if (!(key.u.coeffMask & 0x0888) && (ip[15] == 256) && !(key.u.addMask & 0x8)) {
        key.u.copyAlpha = !(key.u.inType || key.u.outType);
    }

    //ALOGE("build key %08x, %08x", (int32_t)(key.key >> 32), (int32_t)key.key);

    switch (ein->getVectorSize()) {
    case 4:
        key.u.inVecSize = 3;
        break;
    case 3:
        key.u.inVecSize = 2;
        key.u.coeffMask &= ~0xF000;
        break;
    case 2:
        key.u.inVecSize = 1;
        key.u.coeffMask &= ~0xFF00;
        break;
    default:
        key.u.coeffMask &= ~0xFFF0;
        break;
    }

    switch (eout->getVectorSize()) {
    case 4:
        key.u.outVecSize = 3;
        break;
    case 3:
        key.u.outVecSize = 2;
        key.u.coeffMask &= ~0x8888;
        break;
    case 2:
        key.u.outVecSize = 1;
        key.u.coeffMask &= ~0xCCCC;
        break;
    default:
        key.u.coeffMask &= ~0xEEEE;
        break;
    }

    if (key.u.inType && !key.u.outType) {
        key.u.addMask |= 1;
        if (key.u.outVecSize > 0) key.u.addMask |= 2;
        if (key.u.outVecSize > 1) key.u.addMask |= 4;
        if (key.u.outVecSize > 2) key.u.addMask |= 8;
    }

    //ALOGE("build key %08x, %08x", (int32_t)(key.key >> 32), (int32_t)key.key);
    return key;
}

#if defined(ARCH_ARM_HAVE_NEON)

#define DEF_SYM(x)                                  \
    extern "C" uint32_t _N_ColorMatrix_##x;      \
    extern "C" uint32_t _N_ColorMatrix_##x##_end;  \
    extern "C" uint32_t _N_ColorMatrix_##x##_len;

DEF_SYM(prefix_i)
DEF_SYM(prefix_f)
DEF_SYM(postfix1)
DEF_SYM(postfix2)

DEF_SYM(load_u8_4)
DEF_SYM(load_u8_3)
DEF_SYM(load_u8_2)
DEF_SYM(load_u8_1)
DEF_SYM(load_u8f_4)
DEF_SYM(load_u8f_3)
DEF_SYM(load_u8f_2)
DEF_SYM(load_u8f_1)
DEF_SYM(load_f32_4)
DEF_SYM(load_f32_3)
DEF_SYM(load_f32_2)
DEF_SYM(load_f32_1)

DEF_SYM(store_u8_4)
DEF_SYM(store_u8_2)
DEF_SYM(store_u8_1)
DEF_SYM(store_f32_4)
DEF_SYM(store_f32_3)
DEF_SYM(store_f32_2)
DEF_SYM(store_f32_1)
DEF_SYM(store_f32u_4)
DEF_SYM(store_f32u_2)
DEF_SYM(store_f32u_1)

DEF_SYM(unpack_u8_4)
DEF_SYM(unpack_u8_3)
DEF_SYM(unpack_u8_2)
DEF_SYM(unpack_u8_1)
DEF_SYM(pack_u8_4)
DEF_SYM(pack_u8_3)
DEF_SYM(pack_u8_2)
DEF_SYM(pack_u8_1)
DEF_SYM(dot)
DEF_SYM(add_0_u8)
DEF_SYM(add_1_u8)
DEF_SYM(add_2_u8)
DEF_SYM(add_3_u8)

#define ADD_CHUNK(x) \
    memcpy(buf, &_N_ColorMatrix_##x, _N_ColorMatrix_##x##_len); \
    buf += _N_ColorMatrix_##x##_len


static uint8_t * addBranch(uint8_t *buf, const uint8_t *target, uint32_t condition) {
    size_t off = (target - buf - 8) >> 2;
    rsAssert(((off & 0xff000000) == 0) ||
           ((off & 0xff000000) == 0xff000000));

    uint32_t op = (condition << 28);
    op |= 0xa << 24;  // branch
    op |= 0xffffff & off;
    ((uint32_t *)buf)[0] = op;
    return buf + 4;
}

static uint32_t encodeSIMDRegs(uint32_t vd, uint32_t vn, uint32_t vm) {
    rsAssert(vd < 32);
    rsAssert(vm < 32);
    rsAssert(vn < 32);

    uint32_t op = ((vd & 0xf) << 12) | (((vd & 0x10) >> 4) << 22);
    op |= (vm & 0xf) | (((vm & 0x10) >> 4) << 5);
    op |= ((vn & 0xf) << 16) | (((vn & 0x10) >> 4) << 7);
    return op;
}

static uint8_t * addVMLAL_S16(uint8_t *buf, uint32_t dest_q, uint32_t src_d1, uint32_t src_d2, uint32_t src_d2_s) {
    //vmlal.s16 Q#1, D#1, D#2[#]
    uint32_t op = 0xf2900240 | encodeSIMDRegs(dest_q << 1, src_d1, src_d2 | (src_d2_s << 3));
    ((uint32_t *)buf)[0] = op;
    return buf + 4;
}

static uint8_t * addVMULL_S16(uint8_t *buf, uint32_t dest_q, uint32_t src_d1, uint32_t src_d2, uint32_t src_d2_s) {
    //vmull.s16 Q#1, D#1, D#2[#]
    uint32_t op = 0xf2900A40 | encodeSIMDRegs(dest_q << 1, src_d1, src_d2 | (src_d2_s << 3));
    ((uint32_t *)buf)[0] = op;
    return buf + 4;
}

static uint8_t * addVQADD_S32(uint8_t *buf, uint32_t dest_q, uint32_t src_q1, uint32_t src_q2) {
    //vqadd.s32 Q#1, D#1, D#2
    uint32_t op = 0xf2200050 | encodeSIMDRegs(dest_q << 1, src_q1 << 1, src_q2 << 1);
    ((uint32_t *)buf)[0] = op;
    return buf + 4;
}

static uint8_t * addVMLAL_F32(uint8_t *buf, uint32_t dest_q, uint32_t src_d1, uint32_t src_d2, uint32_t src_d2_s) {
    //vmlal.f32 Q#1, D#1, D#2[#]
    uint32_t op = 0xf3a00140 | encodeSIMDRegs(dest_q << 1, src_d1, src_d2 | (src_d2_s << 4));
    ((uint32_t *)buf)[0] = op;
    return buf + 4;
}

static uint8_t * addVMULL_F32(uint8_t *buf, uint32_t dest_q, uint32_t src_d1, uint32_t src_d2, uint32_t src_d2_s) {
    //vmull.f32 Q#1, D#1, D#2[#]
    uint32_t op = 0xf3a00940 | encodeSIMDRegs(dest_q << 1, src_d1, src_d2 | (src_d2_s << 4));
    ((uint32_t *)buf)[0] = op;
    return buf + 4;
}

static uint8_t * addVORR_32(uint8_t *buf, uint32_t dest_q, uint32_t src_q1, uint32_t src_q2) {
    //vadd.f32 Q#1, D#1, D#2
    uint32_t op = 0xf2200150 | encodeSIMDRegs(dest_q << 1, src_q1 << 1, src_q2 << 1);
    ((uint32_t *)buf)[0] = op;
    return buf + 4;
}

static uint8_t * addVADD_F32(uint8_t *buf, uint32_t dest_q, uint32_t src_q1, uint32_t src_q2) {
    //vadd.f32 Q#1, D#1, D#2
    uint32_t op = 0xf2000d40 | encodeSIMDRegs(dest_q << 1, src_q1 << 1, src_q2 << 1);
    ((uint32_t *)buf)[0] = op;
    return buf + 4;
}
#endif


bool RsdCpuScriptIntrinsicColorMatrix::build(Key_t key) {
#if defined(ARCH_ARM_HAVE_NEON)
    mBufSize = 4096;
    //StopWatch build_time("rs cm: build time");
    mBuf = (uint8_t *)mmap(0, mBufSize, PROT_READ | PROT_WRITE,
                                  MAP_PRIVATE | MAP_ANON, -1, 0);
    if (!mBuf) {
        return false;
    }

    uint8_t *buf = mBuf;
    uint8_t *buf2 = NULL;

    int ops[5][4];  // 0=unused, 1 = set, 2 = accumulate, 3 = final
    int opInit[4] = {0, 0, 0, 0};

    memset(ops, 0, sizeof(ops));
    for (int i=0; i < 4; i++) {
        if (key.u.coeffMask & (1 << (i*4))) {
            ops[i][0] = 0x2 | opInit[0];
            opInit[0] = 1;
        }
        if (!key.u.dot) {
            if (key.u.coeffMask & (1 << (1 + i*4))) {
                ops[i][1] = 0x2 | opInit[1];
                opInit[1] = 1;
            }
            if (key.u.coeffMask & (1 << (2 + i*4))) {
                ops[i][2] = 0x2 | opInit[2];
                opInit[2] = 1;
            }
        }
        if (!key.u.copyAlpha) {
            if (key.u.coeffMask & (1 << (3 + i*4))) {
                ops[i][3] = 0x2 | opInit[3];
                opInit[3] = 1;
            }
        }
    }

    if (key.u.inType || key.u.outType) {
        key.u.copyAlpha = 0;
        ADD_CHUNK(prefix_f);
        buf2 = buf;

        // Load the incoming r,g,b,a as needed
        if (key.u.inType) {
            switch(key.u.inVecSize) {
            case 3:
                ADD_CHUNK(load_f32_4);
                break;
            case 2:
                ADD_CHUNK(load_f32_3);
                break;
            case 1:
                ADD_CHUNK(load_f32_2);
                break;
            case 0:
                ADD_CHUNK(load_f32_1);
                break;
            }
        } else {
            switch(key.u.inVecSize) {
            case 3:
                ADD_CHUNK(load_u8f_4);
                break;
            case 2:
                ADD_CHUNK(load_u8f_3);
                break;
            case 1:
                ADD_CHUNK(load_u8f_2);
                break;
            case 0:
                ADD_CHUNK(load_u8f_1);
                break;
            }
        }

        for (int i=0; i < 4; i++) {
            for (int j=0; j < 4; j++) {
                switch(ops[i][j]) {
                case 0:
                    break;
                case 2:
                    buf = addVMULL_F32(buf, 12+j, i*2, 8+i*2 + (j >> 1), j & 1);
                    break;
                case 3:
                    buf = addVMLAL_F32(buf, 12+j, i*2, 8+i*2 + (j >> 1), j & 1);
                    break;
                }
            }
        }
        for (int j=0; j < 4; j++) {
            if (opInit[j]) {
                if (key.u.addMask & (1 << j)) {
                    buf = addVADD_F32(buf, j, 12+j, 8+j);
                } else {
                    buf = addVORR_32(buf, j, 12+j, 12+j);
                }
            } else {
                if (key.u.addMask & (1 << j)) {
                    buf = addVADD_F32(buf, j, j, 8+j);
                }
            }
        }

        if (key.u.outType) {
            switch(key.u.outVecSize) {
            case 3:
                ADD_CHUNK(store_f32_4);
                break;
            case 2:
                ADD_CHUNK(store_f32_3);
                break;
            case 1:
                ADD_CHUNK(store_f32_2);
                break;
            case 0:
                ADD_CHUNK(store_f32_1);
                break;
            }
        } else {
            switch(key.u.outVecSize) {
            case 3:
            case 2:
                ADD_CHUNK(store_f32u_4);
                break;
            case 1:
                ADD_CHUNK(store_f32u_2);
                break;
            case 0:
                ADD_CHUNK(store_f32u_1);
                break;
            }
        }


    } else {
        // Add the function prefix
        // Store the address for the loop return
        ADD_CHUNK(prefix_i);
        buf2 = buf;

        // Load the incoming r,g,b,a as needed
        switch(key.u.inVecSize) {
        case 3:
            ADD_CHUNK(load_u8_4);
            if (key.u.copyAlpha) {
                ADD_CHUNK(unpack_u8_3);
            } else {
                ADD_CHUNK(unpack_u8_4);
            }
            break;
        case 2:
            ADD_CHUNK(load_u8_3);
            ADD_CHUNK(unpack_u8_3);
            break;
        case 1:
            ADD_CHUNK(load_u8_2);
            ADD_CHUNK(unpack_u8_2);
            break;
        case 0:
            ADD_CHUNK(load_u8_1);
            ADD_CHUNK(unpack_u8_1);
            break;
        }

        // Add multiply and accumulate
        // use MULL to init the output register,
        // use MLAL from there
        for (int i=0; i < 4; i++) {
            for (int j=0; j < 4; j++) {
                switch(ops[i][j]) {
                case 0:
                    break;
                case 2:
                    buf = addVMULL_S16(buf, 8+j, 24+i*2, 4+i, j);
                    break;
                case 3:
                    buf = addVMLAL_S16(buf, 8+j, 24+i*2, 4+i, j);
                    break;
                }
            }
        }
        for (int j=0; j < 4; j++) {
            if (opInit[j]) {
                if (key.u.addMask & (1 << j)) {
                    buf = addVQADD_S32(buf, 8+j, 8+j, 4+j);
                }
            } else {
                if (key.u.addMask & (1 << j)) {
                    buf = addVQADD_S32(buf, 8+j, 12+j, 4+j);
                }
            }
        }

        // If we have a dot product, perform the special pack.
        if (key.u.dot) {
            ADD_CHUNK(pack_u8_1);
            ADD_CHUNK(dot);
        } else {
            switch(key.u.outVecSize) {
            case 3:
                if (key.u.copyAlpha) {
                    ADD_CHUNK(pack_u8_3);
                } else {
                    ADD_CHUNK(pack_u8_4);
                }
                break;
            case 2:
                ADD_CHUNK(pack_u8_3);
                break;
            case 1:
                ADD_CHUNK(pack_u8_2);
                break;
            case 0:
                ADD_CHUNK(pack_u8_1);
                break;
            }
        }

        // Write out result
        switch(key.u.outVecSize) {
        case 3:
        case 2:
            ADD_CHUNK(store_u8_4);
            break;
        case 1:
            ADD_CHUNK(store_u8_2);
            break;
        case 0:
            ADD_CHUNK(store_u8_1);
            break;
        }
    }

    if (key.u.inType != key.u.outType) {
        key.u.copyAlpha = 0;
        key.u.dot = 0;
    }

    // Loop, branch, and cleanup
    ADD_CHUNK(postfix1);
    buf = addBranch(buf, buf2, 0x01);
    ADD_CHUNK(postfix2);

    int ret = mprotect(mBuf, mBufSize, PROT_READ | PROT_EXEC);
    if (ret == -1) {
        ALOGE("mprotect error %i", ret);
        return false;
    }

    cacheflush((long)mBuf, (long)mBuf + mBufSize, 0);
    return true;
#else
    return false;
#endif
}

void RsdCpuScriptIntrinsicColorMatrix::updateCoeffCache(float fpMul, float addMul) {
    for(int ct=0; ct < 16; ct++) {
        ip[ct] = (short)(fp[ct] * 256.f + 0.5f);
        tmpFp[ct] = fp[ct] * fpMul;
        //ALOGE("mat %i %f  %f", ct, fp[ct], tmpFp[ct]);
    }

    float add = 0.f;
    if (fpMul > 254.f) add = 0.5f;
    for(int ct=0; ct < 4; ct++) {
        tmpFpa[ct * 4 + 0] = fpa[ct] * addMul + add;
        //ALOGE("fpa %i %f  %f", ct, fpa[ct], tmpFpa[ct * 4 + 0]);
        tmpFpa[ct * 4 + 1] = tmpFpa[ct * 4];
        tmpFpa[ct * 4 + 2] = tmpFpa[ct * 4];
        tmpFpa[ct * 4 + 3] = tmpFpa[ct * 4];
    }

    for(int ct=0; ct < 4; ct++) {
        ipa[ct * 4 + 0] = (int)(fpa[ct] * 65536.f + 0.5f);
        ipa[ct * 4 + 1] = ipa[ct * 4];
        ipa[ct * 4 + 2] = ipa[ct * 4];
        ipa[ct * 4 + 3] = ipa[ct * 4];
    }
}

void RsdCpuScriptIntrinsicColorMatrix::setGlobalVar(uint32_t slot, const void *data,
                                                    size_t dataLength) {
    switch(slot) {
    case 0:
        memcpy (fp, data, sizeof(fp));
        break;
    case 1:
        memcpy (fpa, data, sizeof(fpa));
        break;
    default:
        rsAssert(0);
        break;
    }
    mRootPtr = &kernel;
}


static void One(const RsForEachStubParamStruct *p, void *out,
                const void *py, const float* coeff, const float *add,
                uint32_t vsin, uint32_t vsout, bool fin, bool fout) {

    float4 f = 0.f;
    if (fin) {
        switch(vsin) {
        case 3:
            f = ((const float4 *)py)[0];
            break;
        case 2:
            f = ((const float4 *)py)[0];
            f.w = 0.f;
            break;
        case 1:
            f.xy = ((const float2 *)py)[0];
            break;
        case 0:
            f.x = ((const float *)py)[0];
            break;
        }
    } else {
        switch(vsin) {
        case 3:
            f = convert_float4(((const uchar4 *)py)[0]);
            break;
        case 2:
            f = convert_float4(((const uchar4 *)py)[0]);
            f.w = 0.f;
            break;
        case 1:
            f.xy = convert_float2(((const uchar2 *)py)[0]);
            break;
        case 0:
            f.x = (float)(((const uchar *)py)[0]);
            break;
        }
    }
    //ALOGE("f1  %f %f %f %f", f.x, f.y, f.z, f.w);

    float4 sum;
    sum.x = f.x * coeff[0] +
            f.y * coeff[4] +
            f.z * coeff[8] +
            f.w * coeff[12];
    sum.y = f.x * coeff[1] +
            f.y * coeff[5] +
            f.z * coeff[9] +
            f.w * coeff[13];
    sum.z = f.x * coeff[2] +
            f.y * coeff[6] +
            f.z * coeff[10] +
            f.w * coeff[14];
    sum.w = f.x * coeff[3] +
            f.y * coeff[7] +
            f.z * coeff[11] +
            f.w * coeff[15];
    //ALOGE("f2  %f %f %f %f", sum.x, sum.y, sum.z, sum.w);

    sum.x += add[0];
    sum.y += add[4];
    sum.z += add[8];
    sum.w += add[12];


    //ALOGE("fout %i vs %i, sum %f %f %f %f", fout, vsout, sum.x, sum.y, sum.z, sum.w);
    if (fout) {
        switch(vsout) {
        case 3:
        case 2:
            ((float4 *)out)[0] = sum;
            break;
        case 1:
            ((float2 *)out)[0] = sum.xy;
            break;
        case 0:
            ((float *)out)[0] = sum.x;
            break;
        }
    } else {
        sum.x = sum.x < 0 ? 0 : (sum.x > 255.5 ? 255.5 : sum.x);
        sum.y = sum.y < 0 ? 0 : (sum.y > 255.5 ? 255.5 : sum.y);
        sum.z = sum.z < 0 ? 0 : (sum.z > 255.5 ? 255.5 : sum.z);
        sum.w = sum.w < 0 ? 0 : (sum.w > 255.5 ? 255.5 : sum.w);

        switch(vsout) {
        case 3:
        case 2:
            ((uchar4 *)out)[0] = convert_uchar4(sum);
            break;
        case 1:
            ((uchar2 *)out)[0] = convert_uchar2(sum.xy);
            break;
        case 0:
            ((uchar *)out)[0] = sum.x;
            break;
        }
    }
    //ALOGE("out %p %f %f %f %f", out, ((float *)out)[0], ((float *)out)[1], ((float *)out)[2], ((float *)out)[3]);
}

void RsdCpuScriptIntrinsicColorMatrix::kernel(const RsForEachStubParamStruct *p,
                                              uint32_t xstart, uint32_t xend,
                                              uint32_t instep, uint32_t outstep) {
    RsdCpuScriptIntrinsicColorMatrix *cp = (RsdCpuScriptIntrinsicColorMatrix *)p->usr;
    uchar *out = (uchar *)p->out;
    uchar *in = (uchar *)p->in;
    uint32_t x1 = xstart;
    uint32_t x2 = xend;

    uint32_t vsin = cp->mLastKey.u.inVecSize;
    uint32_t vsout = cp->mLastKey.u.outVecSize;
    bool floatIn = !!cp->mLastKey.u.inType;
    bool floatOut = !!cp->mLastKey.u.outType;

    //if (!p->y) ALOGE("steps %i %i   %i %i", instep, outstep, vsin, vsout);

    if(x2 > x1) {
        int32_t len = (x2 - x1) >> 2;
        if((cp->mOptKernel != NULL) && (len > 0)) {
            cp->mOptKernel(out, in, cp->ip, len);
            x1 += len << 2;
            out += outstep * (len << 2);
            in += instep * (len << 2);
        }

        while(x1 != x2) {
            One(p, out, in, cp->tmpFp, cp->tmpFpa, vsin, vsout, floatIn, floatOut);
            out += outstep;
            in += instep;
            x1++;
        }
    }
}

void RsdCpuScriptIntrinsicColorMatrix::preLaunch(
        uint32_t slot, const Allocation * ain, Allocation * aout,
        const void * usr, uint32_t usrLen, const RsScriptCall *sc) {

    const Element *ein = ain->mHal.state.type->getElement();
    const Element *eout = aout->mHal.state.type->getElement();

    if (ein->getType() == eout->getType()) {
        if (eout->getType() == RS_TYPE_UNSIGNED_8) {
            updateCoeffCache(1.f, 255.f);
        } else {
            updateCoeffCache(1.f, 1.f);
        }
    } else {
        if (eout->getType() == RS_TYPE_UNSIGNED_8) {
            updateCoeffCache(255.f, 255.f);
        } else {
            updateCoeffCache(1.f / 255.f, 1.f);
        }
    }

    Key_t key = computeKey(ain->mHal.state.type->getElement(),
                           aout->mHal.state.type->getElement());
    if ((mOptKernel == NULL) || (mLastKey.key != key.key)) {
        if (mBuf) munmap(mBuf, mBufSize);
        mBuf = NULL;
        mOptKernel = NULL;
        if (build(key)) {
            mOptKernel = (void (*)(void *, const void *, const short *, uint32_t)) mBuf;
            mLastKey = key;
        }
    }
}

void RsdCpuScriptIntrinsicColorMatrix::postLaunch(
        uint32_t slot, const Allocation * ain, Allocation * aout,
        const void * usr, uint32_t usrLen, const RsScriptCall *sc) {

}

RsdCpuScriptIntrinsicColorMatrix::RsdCpuScriptIntrinsicColorMatrix(
            RsdCpuReferenceImpl *ctx, const Script *s, const Element *e)
            : RsdCpuScriptIntrinsic(ctx, s, e, RS_SCRIPT_INTRINSIC_ID_COLOR_MATRIX) {

    mLastKey.key = 0;
    mBuf = NULL;
    mBufSize = 0;
    mOptKernel = NULL;
    const static float defaultMatrix[] = {
        1.f, 0.f, 0.f, 0.f,
        0.f, 1.f, 0.f, 0.f,
        0.f, 0.f, 1.f, 0.f,
        0.f, 0.f, 0.f, 1.f
    };
    const static float defaultAdd[] = {0.f, 0.f, 0.f, 0.f};
    setGlobalVar(0, defaultMatrix, sizeof(defaultMatrix));
    setGlobalVar(1, defaultAdd, sizeof(defaultAdd));
}

RsdCpuScriptIntrinsicColorMatrix::~RsdCpuScriptIntrinsicColorMatrix() {
    if (mBuf) munmap(mBuf, mBufSize);
    mBuf = NULL;
    mOptKernel = NULL;
}

void RsdCpuScriptIntrinsicColorMatrix::populateScript(Script *s) {
    s->mHal.info.exportedVariableCount = 2;
}

RsdCpuScriptImpl * rsdIntrinsic_ColorMatrix(RsdCpuReferenceImpl *ctx,
                                            const Script *s, const Element *e) {

    return new RsdCpuScriptIntrinsicColorMatrix(ctx, s, e);
}



