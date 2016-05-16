#include "rs_core.rsh"
#include "rs_graphics.rsh"
#include "rs_structs.h"

/* Function declarations from libRS */
extern float4 __attribute__((overloadable)) convert_float4(uchar4 c);

/* Implementation of Core Runtime */


/////////////////////////////////////////////////////
// Matrix ops
/////////////////////////////////////////////////////


extern void __attribute__((overloadable))
rsMatrixLoadIdentity(rs_matrix4x4 *m) {
    m->m[0] = 1.f;
    m->m[1] = 0.f;
    m->m[2] = 0.f;
    m->m[3] = 0.f;
    m->m[4] = 0.f;
    m->m[5] = 1.f;
    m->m[6] = 0.f;
    m->m[7] = 0.f;
    m->m[8] = 0.f;
    m->m[9] = 0.f;
    m->m[10] = 1.f;
    m->m[11] = 0.f;
    m->m[12] = 0.f;
    m->m[13] = 0.f;
    m->m[14] = 0.f;
    m->m[15] = 1.f;
}

extern void __attribute__((overloadable))
rsMatrixLoadIdentity(rs_matrix3x3 *m) {
    m->m[0] = 1.f;
    m->m[1] = 0.f;
    m->m[2] = 0.f;
    m->m[3] = 0.f;
    m->m[4] = 1.f;
    m->m[5] = 0.f;
    m->m[6] = 0.f;
    m->m[7] = 0.f;
    m->m[8] = 1.f;
}
extern void __attribute__((overloadable))
rsMatrixLoadIdentity(rs_matrix2x2 *m) {
    m->m[0] = 1.f;
    m->m[1] = 0.f;
    m->m[2] = 0.f;
    m->m[3] = 1.f;
}

extern void __attribute__((overloadable))
rsMatrixLoad(rs_matrix4x4 *m, const float *f) {
    m->m[0] = f[0];
    m->m[1] = f[1];
    m->m[2] = f[2];
    m->m[3] = f[3];
    m->m[4] = f[4];
    m->m[5] = f[5];
    m->m[6] = f[6];
    m->m[7] = f[7];
    m->m[8] = f[8];
    m->m[9] = f[9];
    m->m[10] = f[10];
    m->m[11] = f[11];
    m->m[12] = f[12];
    m->m[13] = f[13];
    m->m[14] = f[14];
    m->m[15] = f[15];
}
extern void __attribute__((overloadable))
rsMatrixLoad(rs_matrix3x3 *m, const float *f) {
    m->m[0] = f[0];
    m->m[1] = f[1];
    m->m[2] = f[2];
    m->m[3] = f[3];
    m->m[4] = f[4];
    m->m[5] = f[5];
    m->m[6] = f[6];
    m->m[7] = f[7];
    m->m[8] = f[8];
}
extern void __attribute__((overloadable))
rsMatrixLoad(rs_matrix2x2 *m, const float *f) {
    m->m[0] = f[0];
    m->m[1] = f[1];
    m->m[2] = f[2];
    m->m[3] = f[3];
}

extern void __attribute__((overloadable))
rsMatrixLoad(rs_matrix4x4 *m, const rs_matrix4x4 *s) {
    m->m[0] = s->m[0];
    m->m[1] = s->m[1];
    m->m[2] = s->m[2];
    m->m[3] = s->m[3];
    m->m[4] = s->m[4];
    m->m[5] = s->m[5];
    m->m[6] = s->m[6];
    m->m[7] = s->m[7];
    m->m[8] = s->m[8];
    m->m[9] = s->m[9];
    m->m[10] = s->m[10];
    m->m[11] = s->m[11];
    m->m[12] = s->m[12];
    m->m[13] = s->m[13];
    m->m[14] = s->m[14];
    m->m[15] = s->m[15];
}
extern void __attribute__((overloadable))
rsMatrixLoad(rs_matrix4x4 *m, const rs_matrix3x3 *v) {
    m->m[0] = v->m[0];
    m->m[1] = v->m[1];
    m->m[2] = v->m[2];
    m->m[3] = 0.f;
    m->m[4] = v->m[3];
    m->m[5] = v->m[4];
    m->m[6] = v->m[5];
    m->m[7] = 0.f;
    m->m[8] = v->m[6];
    m->m[9] = v->m[7];
    m->m[10] = v->m[8];
    m->m[11] = 0.f;
    m->m[12] = 0.f;
    m->m[13] = 0.f;
    m->m[14] = 0.f;
    m->m[15] = 1.f;
}
extern void __attribute__((overloadable))
rsMatrixLoad(rs_matrix4x4 *m, const rs_matrix2x2 *v) {
    m->m[0] = v->m[0];
    m->m[1] = v->m[1];
    m->m[2] = 0.f;
    m->m[3] = 0.f;
    m->m[4] = v->m[2];
    m->m[5] = v->m[3];
    m->m[6] = 0.f;
    m->m[7] = 0.f;
    m->m[8] = 0.f;
    m->m[9] = 0.f;
    m->m[10] = 1.f;
    m->m[11] = 0.f;
    m->m[12] = 0.f;
    m->m[13] = 0.f;
    m->m[14] = 0.f;
    m->m[15] = 1.f;
}
extern void __attribute__((overloadable))
rsMatrixLoad(rs_matrix3x3 *m, const rs_matrix3x3 *s) {
    m->m[0] = s->m[0];
    m->m[1] = s->m[1];
    m->m[2] = s->m[2];
    m->m[3] = s->m[3];
    m->m[4] = s->m[4];
    m->m[5] = s->m[5];
    m->m[6] = s->m[6];
    m->m[7] = s->m[7];
    m->m[8] = s->m[8];
}
extern void __attribute__((overloadable))
rsMatrixLoad(rs_matrix2x2 *m, const rs_matrix2x2 *s) {
    m->m[0] = s->m[0];
    m->m[1] = s->m[1];
    m->m[2] = s->m[2];
    m->m[3] = s->m[3];
}


extern void __attribute__((overloadable))
rsMatrixSet(rs_matrix4x4 *m, uint32_t row, uint32_t col, float v) {
    m->m[row * 4 + col] = v;
}

extern float __attribute__((overloadable))
rsMatrixGet(const rs_matrix4x4 *m, uint32_t row, uint32_t col) {
    return m->m[row * 4 + col];
}

extern void __attribute__((overloadable))
rsMatrixSet(rs_matrix3x3 *m, uint32_t row, uint32_t col, float v) {
    m->m[row * 3 + col] = v;
}

extern float __attribute__((overloadable))
rsMatrixGet(const rs_matrix3x3 *m, uint32_t row, uint32_t col) {
    return m->m[row * 3 + col];
}

extern void __attribute__((overloadable))
rsMatrixSet(rs_matrix2x2 *m, uint32_t row, uint32_t col, float v) {
    m->m[row * 2 + col] = v;
}

extern float __attribute__((overloadable))
rsMatrixGet(const rs_matrix2x2 *m, uint32_t row, uint32_t col) {
    return m->m[row * 2 + col];
}

extern float2 __attribute__((overloadable))
rsMatrixMultiply(const rs_matrix2x2 *m, float2 in) {
    float2 ret;
    ret.x = (m->m[0] * in.x) + (m->m[2] * in.y);
    ret.y = (m->m[1] * in.x) + (m->m[3] * in.y);
    return ret;
}
extern float2 __attribute__((overloadable))
rsMatrixMultiply(rs_matrix2x2 *m, float2 in) {
    return rsMatrixMultiply((const rs_matrix2x2 *)m, in);
}

extern float4 __attribute__((overloadable))
rsMatrixMultiply(rs_matrix4x4 *m, float4 in) {
    return rsMatrixMultiply((const rs_matrix4x4 *)m, in);
}

extern float4 __attribute__((overloadable))
rsMatrixMultiply(rs_matrix4x4 *m, float3 in) {
    return rsMatrixMultiply((const rs_matrix4x4 *)m, in);
}

extern float4 __attribute__((overloadable))
rsMatrixMultiply(rs_matrix4x4 *m, float2 in) {
    return rsMatrixMultiply((const rs_matrix4x4 *)m, in);
}

extern float3 __attribute__((overloadable))
rsMatrixMultiply(rs_matrix3x3 *m, float3 in) {
    return rsMatrixMultiply((const rs_matrix3x3 *)m, in);
}

extern float3 __attribute__((overloadable))
rsMatrixMultiply(rs_matrix3x3 *m, float2 in) {
    return rsMatrixMultiply((const rs_matrix3x3 *)m, in);
}

extern void __attribute__((overloadable))
rsMatrixLoadMultiply(rs_matrix4x4 *ret, const rs_matrix4x4 *lhs, const rs_matrix4x4 *rhs) {
    for (int i=0 ; i<4 ; i++) {
        float ri0 = 0;
        float ri1 = 0;
        float ri2 = 0;
        float ri3 = 0;
        for (int j=0 ; j<4 ; j++) {
            const float rhs_ij = rsMatrixGet(rhs, i, j);
            ri0 += rsMatrixGet(lhs, j, 0) * rhs_ij;
            ri1 += rsMatrixGet(lhs, j, 1) * rhs_ij;
            ri2 += rsMatrixGet(lhs, j, 2) * rhs_ij;
            ri3 += rsMatrixGet(lhs, j, 3) * rhs_ij;
        }
        rsMatrixSet(ret, i, 0, ri0);
        rsMatrixSet(ret, i, 1, ri1);
        rsMatrixSet(ret, i, 2, ri2);
        rsMatrixSet(ret, i, 3, ri3);
    }
}

extern void __attribute__((overloadable))
rsMatrixMultiply(rs_matrix4x4 *lhs, const rs_matrix4x4 *rhs) {
    rs_matrix4x4 r;
    rsMatrixLoadMultiply(&r, lhs, rhs);
    rsMatrixLoad(lhs, &r);
}

extern void __attribute__((overloadable))
rsMatrixLoadMultiply(rs_matrix3x3 *ret, const rs_matrix3x3 *lhs, const rs_matrix3x3 *rhs) {
    for (int i=0 ; i<3 ; i++) {
        float ri0 = 0;
        float ri1 = 0;
        float ri2 = 0;
        for (int j=0 ; j<3 ; j++) {
            const float rhs_ij = rsMatrixGet(rhs, i, j);
            ri0 += rsMatrixGet(lhs, j, 0) * rhs_ij;
            ri1 += rsMatrixGet(lhs, j, 1) * rhs_ij;
            ri2 += rsMatrixGet(lhs, j, 2) * rhs_ij;
        }
        rsMatrixSet(ret, i, 0, ri0);
        rsMatrixSet(ret, i, 1, ri1);
        rsMatrixSet(ret, i, 2, ri2);
    }
}

extern void __attribute__((overloadable))
rsMatrixMultiply(rs_matrix3x3 *lhs, const rs_matrix3x3 *rhs) {
    rs_matrix3x3 r;
    rsMatrixLoadMultiply(&r, lhs, rhs);
    rsMatrixLoad(lhs, &r);
}

extern void __attribute__((overloadable))
rsMatrixLoadMultiply(rs_matrix2x2 *ret, const rs_matrix2x2 *lhs, const rs_matrix2x2 *rhs) {
    for (int i=0 ; i<2 ; i++) {
        float ri0 = 0;
        float ri1 = 0;
        for (int j=0 ; j<2 ; j++) {
            const float rhs_ij = rsMatrixGet(rhs, i, j);
            ri0 += rsMatrixGet(lhs, j, 0) * rhs_ij;
            ri1 += rsMatrixGet(lhs, j, 1) * rhs_ij;
        }
        rsMatrixSet(ret, i, 0, ri0);
        rsMatrixSet(ret, i, 1, ri1);
    }
}

extern void __attribute__((overloadable))
rsMatrixMultiply(rs_matrix2x2 *lhs, const rs_matrix2x2 *rhs) {
    rs_matrix2x2 r;
    rsMatrixLoadMultiply(&r, lhs, rhs);
    rsMatrixLoad(lhs, &r);
}

