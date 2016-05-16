#include "rs_types.rsh"

extern float2 __attribute__((overloadable)) convert_float2(int2 c);
extern float3 __attribute__((overloadable)) convert_float3(int3 c);
extern float4 __attribute__((overloadable)) convert_float4(int4 c);

extern int2 __attribute__((overloadable)) convert_int2(float2 c);
extern int3 __attribute__((overloadable)) convert_int3(float3 c);
extern int4 __attribute__((overloadable)) convert_int4(float4 c);


extern float __attribute__((overloadable)) fmin(float v, float v2);
extern float2 __attribute__((overloadable)) fmin(float2 v, float v2);
extern float3 __attribute__((overloadable)) fmin(float3 v, float v2);
extern float4 __attribute__((overloadable)) fmin(float4 v, float v2);

extern float __attribute__((overloadable)) fmax(float v, float v2);
extern float2 __attribute__((overloadable)) fmax(float2 v, float v2);
extern float3 __attribute__((overloadable)) fmax(float3 v, float v2);
extern float4 __attribute__((overloadable)) fmax(float4 v, float v2);

// Float ops, 6.11.2

#define FN_FUNC_FN(fnc)                                         \
extern float2 __attribute__((overloadable)) fnc(float2 v) { \
    float2 r;                                                   \
    r.x = fnc(v.x);                                             \
    r.y = fnc(v.y);                                             \
    return r;                                                   \
}                                                               \
extern float3 __attribute__((overloadable)) fnc(float3 v) { \
    float3 r;                                                   \
    r.x = fnc(v.x);                                             \
    r.y = fnc(v.y);                                             \
    r.z = fnc(v.z);                                             \
    return r;                                                   \
}                                                               \
extern float4 __attribute__((overloadable)) fnc(float4 v) { \
    float4 r;                                                   \
    r.x = fnc(v.x);                                             \
    r.y = fnc(v.y);                                             \
    r.z = fnc(v.z);                                             \
    r.w = fnc(v.w);                                             \
    return r;                                                   \
}

#define IN_FUNC_FN(fnc)                                         \
extern int2 __attribute__((overloadable)) fnc(float2 v) {   \
    int2 r;                                                     \
    r.x = fnc(v.x);                                             \
    r.y = fnc(v.y);                                             \
    return r;                                                   \
}                                                               \
extern int3 __attribute__((overloadable)) fnc(float3 v) {   \
    int3 r;                                                     \
    r.x = fnc(v.x);                                             \
    r.y = fnc(v.y);                                             \
    r.z = fnc(v.z);                                             \
    return r;                                                   \
}                                                               \
extern int4 __attribute__((overloadable)) fnc(float4 v) {   \
    int4 r;                                                     \
    r.x = fnc(v.x);                                             \
    r.y = fnc(v.y);                                             \
    r.z = fnc(v.z);                                             \
    r.w = fnc(v.w);                                             \
    return r;                                                   \
}

#define FN_FUNC_FN_FN(fnc)                                                  \
extern float2 __attribute__((overloadable)) fnc(float2 v1, float2 v2) { \
    float2 r;                                                               \
    r.x = fnc(v1.x, v2.x);                                                  \
    r.y = fnc(v1.y, v2.y);                                                  \
    return r;                                                               \
}                                                                           \
extern float3 __attribute__((overloadable)) fnc(float3 v1, float3 v2) { \
    float3 r;                                                               \
    r.x = fnc(v1.x, v2.x);                                                  \
    r.y = fnc(v1.y, v2.y);                                                  \
    r.z = fnc(v1.z, v2.z);                                                  \
    return r;                                                               \
}                                                                           \
extern float4 __attribute__((overloadable)) fnc(float4 v1, float4 v2) { \
    float4 r;                                                               \
    r.x = fnc(v1.x, v2.x);                                                  \
    r.y = fnc(v1.y, v2.y);                                                  \
    r.z = fnc(v1.z, v2.z);                                                  \
    r.w = fnc(v1.w, v2.w);                                                  \
    return r;                                                               \
}

#define FN_FUNC_FN_F(fnc)                                                   \
extern float2 __attribute__((overloadable)) fnc(float2 v1, float v2) {  \
    float2 r;                                                               \
    r.x = fnc(v1.x, v2);                                                    \
    r.y = fnc(v1.y, v2);                                                    \
    return r;                                                               \
}                                                                           \
extern float3 __attribute__((overloadable)) fnc(float3 v1, float v2) {  \
    float3 r;                                                               \
    r.x = fnc(v1.x, v2);                                                    \
    r.y = fnc(v1.y, v2);                                                    \
    r.z = fnc(v1.z, v2);                                                    \
    return r;                                                               \
}                                                                           \
extern float4 __attribute__((overloadable)) fnc(float4 v1, float v2) {  \
    float4 r;                                                               \
    r.x = fnc(v1.x, v2);                                                    \
    r.y = fnc(v1.y, v2);                                                    \
    r.z = fnc(v1.z, v2);                                                    \
    r.w = fnc(v1.w, v2);                                                    \
    return r;                                                               \
}

#define FN_FUNC_FN_IN(fnc)                                                  \
extern float2 __attribute__((overloadable)) fnc(float2 v1, int2 v2) {   \
    float2 r;                                                               \
    r.x = fnc(v1.x, v2.x);                                                  \
    r.y = fnc(v1.y, v2.y);                                                  \
    return r;                                                               \
}                                                                           \
extern float3 __attribute__((overloadable)) fnc(float3 v1, int3 v2) {   \
    float3 r;                                                               \
    r.x = fnc(v1.x, v2.x);                                                  \
    r.y = fnc(v1.y, v2.y);                                                  \
    r.z = fnc(v1.z, v2.z);                                                  \
    return r;                                                               \
}                                                                           \
extern float4 __attribute__((overloadable)) fnc(float4 v1, int4 v2) {   \
    float4 r;                                                               \
    r.x = fnc(v1.x, v2.x);                                                  \
    r.y = fnc(v1.y, v2.y);                                                  \
    r.z = fnc(v1.z, v2.z);                                                  \
    r.w = fnc(v1.w, v2.w);                                                  \
    return r;                                                               \
}

#define FN_FUNC_FN_I(fnc)                                                   \
extern float2 __attribute__((overloadable)) fnc(float2 v1, int v2) {    \
    float2 r;                                                               \
    r.x = fnc(v1.x, v2);                                                    \
    r.y = fnc(v1.y, v2);                                                    \
    return r;                                                               \
}                                                                           \
extern float3 __attribute__((overloadable)) fnc(float3 v1, int v2) {    \
    float3 r;                                                               \
    r.x = fnc(v1.x, v2);                                                    \
    r.y = fnc(v1.y, v2);                                                    \
    r.z = fnc(v1.z, v2);                                                    \
    return r;                                                               \
}                                                                           \
extern float4 __attribute__((overloadable)) fnc(float4 v1, int v2) {    \
    float4 r;                                                               \
    r.x = fnc(v1.x, v2);                                                    \
    r.y = fnc(v1.y, v2);                                                    \
    r.z = fnc(v1.z, v2);                                                    \
    r.w = fnc(v1.w, v2);                                                    \
    return r;                                                               \
}

#define FN_FUNC_FN_PFN(fnc)                     \
extern float2 __attribute__((overloadable)) \
        fnc(float2 v1, float2 *v2) {            \
    float2 r;                                   \
    float t[2];                                 \
    r.x = fnc(v1.x, &t[0]);                     \
    r.y = fnc(v1.y, &t[1]);                     \
    v2->x = t[0];                               \
    v2->y = t[1];                               \
    return r;                                   \
}                                               \
extern float3 __attribute__((overloadable)) \
        fnc(float3 v1, float3 *v2) {            \
    float3 r;                                   \
    float t[3];                                 \
    r.x = fnc(v1.x, &t[0]);                     \
    r.y = fnc(v1.y, &t[1]);                     \
    r.z = fnc(v1.z, &t[2]);                     \
    v2->x = t[0];                               \
    v2->y = t[1];                               \
    v2->z = t[2];                               \
    return r;                                   \
}                                               \
extern float4 __attribute__((overloadable)) \
        fnc(float4 v1, float4 *v2) {            \
    float4 r;                                   \
    float t[4];                                 \
    r.x = fnc(v1.x, &t[0]);                     \
    r.y = fnc(v1.y, &t[1]);                     \
    r.z = fnc(v1.z, &t[2]);                     \
    r.w = fnc(v1.w, &t[3]);                     \
    v2->x = t[0];                               \
    v2->y = t[1];                               \
    v2->z = t[2];                               \
    v2->w = t[3];                               \
    return r;                                   \
}

#define FN_FUNC_FN_PIN(fnc)                                                 \
extern float2 __attribute__((overloadable)) fnc(float2 v1, int2 *v2) {  \
    float2 r;                                                               \
    int t[2];                                                               \
    r.x = fnc(v1.x, &t[0]);                                                 \
    r.y = fnc(v1.y, &t[1]);                                                 \
    v2->x = t[0];                                                           \
    v2->y = t[1];                                                           \
    return r;                                                               \
}                                                                           \
extern float3 __attribute__((overloadable)) fnc(float3 v1, int3 *v2) {  \
    float3 r;                                                               \
    int t[3];                                                               \
    r.x = fnc(v1.x, &t[0]);                                                 \
    r.y = fnc(v1.y, &t[1]);                                                 \
    r.z = fnc(v1.z, &t[2]);                                                 \
    v2->x = t[0];                                                           \
    v2->y = t[1];                                                           \
    v2->z = t[2];                                                           \
    return r;                                                               \
}                                                                           \
extern float4 __attribute__((overloadable)) fnc(float4 v1, int4 *v2) {  \
    float4 r;                                                               \
    int t[4];                                                               \
    r.x = fnc(v1.x, &t[0]);                                                 \
    r.y = fnc(v1.y, &t[1]);                                                 \
    r.z = fnc(v1.z, &t[2]);                                                 \
    r.w = fnc(v1.w, &t[3]);                                                 \
    v2->x = t[0];                                                           \
    v2->y = t[1];                                                           \
    v2->z = t[2];                                                           \
    v2->w = t[3];                                                           \
    return r;                                                               \
}

#define FN_FUNC_FN_FN_FN(fnc)                   \
extern float2 __attribute__((overloadable)) \
        fnc(float2 v1, float2 v2, float2 v3) {  \
    float2 r;                                   \
    r.x = fnc(v1.x, v2.x, v3.x);                \
    r.y = fnc(v1.y, v2.y, v3.y);                \
    return r;                                   \
}                                               \
extern float3 __attribute__((overloadable)) \
        fnc(float3 v1, float3 v2, float3 v3) {  \
    float3 r;                                   \
    r.x = fnc(v1.x, v2.x, v3.x);                \
    r.y = fnc(v1.y, v2.y, v3.y);                \
    r.z = fnc(v1.z, v2.z, v3.z);                \
    return r;                                   \
}                                               \
extern float4 __attribute__((overloadable)) \
        fnc(float4 v1, float4 v2, float4 v3) {  \
    float4 r;                                   \
    r.x = fnc(v1.x, v2.x, v3.x);                \
    r.y = fnc(v1.y, v2.y, v3.y);                \
    r.z = fnc(v1.z, v2.z, v3.z);                \
    r.w = fnc(v1.w, v2.w, v3.w);                \
    return r;                                   \
}

#define FN_FUNC_FN_FN_PIN(fnc)                  \
extern float2 __attribute__((overloadable)) \
        fnc(float2 v1, float2 v2, int2 *v3) {   \
    float2 r;                                   \
    int t[2];                                   \
    r.x = fnc(v1.x, v2.x, &t[0]);               \
    r.y = fnc(v1.y, v2.y, &t[1]);               \
    v3->x = t[0];                               \
    v3->y = t[1];                               \
    return r;                                   \
}                                               \
extern float3 __attribute__((overloadable)) \
        fnc(float3 v1, float3 v2, int3 *v3) {   \
    float3 r;                                   \
    int t[3];                                   \
    r.x = fnc(v1.x, v2.x, &t[0]);               \
    r.y = fnc(v1.y, v2.y, &t[1]);               \
    r.z = fnc(v1.z, v2.z, &t[2]);               \
    v3->x = t[0];                               \
    v3->y = t[1];                               \
    v3->z = t[2];                               \
    return r;                                   \
}                                               \
extern float4 __attribute__((overloadable)) \
        fnc(float4 v1, float4 v2, int4 *v3) {   \
    float4 r;                                   \
    int t[4];                                   \
    r.x = fnc(v1.x, v2.x, &t[0]);               \
    r.y = fnc(v1.y, v2.y, &t[1]);               \
    r.z = fnc(v1.z, v2.z, &t[2]);               \
    r.w = fnc(v1.w, v2.w, &t[3]);               \
    v3->x = t[0];                               \
    v3->y = t[1];                               \
    v3->z = t[2];                               \
    v3->w = t[3];                               \
    return r;                                   \
}

static const int iposinf = 0x7f800000;
static const int ineginf = 0xff800000;

static const float posinf() {
    float f = *((float*)&iposinf);
    return f;
}

static const float neginf() {
    float f = *((float*)&ineginf);
    return f;
}

static bool isinf(float f) {
    int i = *((int*)(void*)&f);
    return (i == iposinf) || (i == ineginf);
}

static bool isnan(float f) {
    int i = *((int*)(void*)&f);
    return (((i & 0x7f800000) == 0x7f800000) && (i & 0x007fffff));
}

static bool isposzero(float f) {
    int i = *((int*)(void*)&f);
    return (i == 0x00000000);
}

static bool isnegzero(float f) {
    int i = *((int*)(void*)&f);
    return (i == 0x80000000);
}

static bool iszero(float f) {
    return isposzero(f) || isnegzero(f);
}


extern float __attribute__((overloadable)) acos(float);
FN_FUNC_FN(acos)

extern float __attribute__((overloadable)) acosh(float);
FN_FUNC_FN(acosh)


extern float __attribute__((overloadable)) acospi(float v) {
    return acos(v) / M_PI;
}
FN_FUNC_FN(acospi)

extern float __attribute__((overloadable)) asin(float);
FN_FUNC_FN(asin)

extern float __attribute__((overloadable)) asinh(float);
FN_FUNC_FN(asinh)

extern float __attribute__((overloadable)) asinpi(float v) {
    return asin(v) / M_PI;
}
FN_FUNC_FN(asinpi)

extern float __attribute__((overloadable)) atan(float);
FN_FUNC_FN(atan)

extern float __attribute__((overloadable)) atan2(float, float);
FN_FUNC_FN_FN(atan2)

extern float __attribute__((overloadable)) atanh(float);
FN_FUNC_FN(atanh)

extern float __attribute__((overloadable)) atanpi(float v) {
    return atan(v) / M_PI;
}
FN_FUNC_FN(atanpi)


extern float __attribute__((overloadable)) atan2pi(float y, float x) {
    return atan2(y, x) / M_PI;
}
FN_FUNC_FN_FN(atan2pi)

extern float __attribute__((overloadable)) cbrt(float);
FN_FUNC_FN(cbrt)

extern float __attribute__((overloadable)) ceil(float);
FN_FUNC_FN(ceil)

extern float __attribute__((overloadable)) copysign(float, float);
FN_FUNC_FN_FN(copysign)

extern float __attribute__((overloadable)) cos(float);
FN_FUNC_FN(cos)

extern float __attribute__((overloadable)) cosh(float);
FN_FUNC_FN(cosh)

extern float __attribute__((overloadable)) cospi(float v) {
    return cos(v * M_PI);
}
FN_FUNC_FN(cospi)

extern float __attribute__((overloadable)) erfc(float);
FN_FUNC_FN(erfc)

extern float __attribute__((overloadable)) erf(float);
FN_FUNC_FN(erf)

extern float __attribute__((overloadable)) exp(float);
FN_FUNC_FN(exp)

extern float __attribute__((overloadable)) exp2(float);
FN_FUNC_FN(exp2)

extern float __attribute__((overloadable)) pow(float, float);

extern float __attribute__((overloadable)) exp10(float v) {
    return exp2(v * 3.321928095f);
}
FN_FUNC_FN(exp10)

extern float __attribute__((overloadable)) expm1(float);
FN_FUNC_FN(expm1)

extern float __attribute__((overloadable)) fabs(float v) {
    int i = *((int*)(void*)&v) & 0x7fffffff;
    return  *((float*)(void*)&i);
}
FN_FUNC_FN(fabs)

extern float __attribute__((overloadable)) fdim(float, float);
FN_FUNC_FN_FN(fdim)

extern float __attribute__((overloadable)) floor(float);
FN_FUNC_FN(floor)

extern float __attribute__((overloadable)) fma(float, float, float);
FN_FUNC_FN_FN_FN(fma)

extern float __attribute__((overloadable)) fmin(float, float);

extern float __attribute__((overloadable)) fmod(float, float);
FN_FUNC_FN_FN(fmod)

extern float __attribute__((overloadable)) fract(float v, float *iptr) {
    int i = (int)floor(v);
    if (iptr) {
        iptr[0] = i;
    }
    return fmin(v - i, 0x1.fffffep-1f);
}
FN_FUNC_FN_PFN(fract)

extern float __attribute__((overloadable)) frexp(float, int *);
FN_FUNC_FN_PIN(frexp)

extern float __attribute__((overloadable)) hypot(float, float);
FN_FUNC_FN_FN(hypot)

extern int __attribute__((overloadable)) ilogb(float);
IN_FUNC_FN(ilogb)

extern float __attribute__((overloadable)) ldexp(float, int);
FN_FUNC_FN_IN(ldexp)
FN_FUNC_FN_I(ldexp)

extern float __attribute__((overloadable)) lgamma(float);
FN_FUNC_FN(lgamma)
extern float __attribute__((overloadable)) lgamma(float, int*);
FN_FUNC_FN_PIN(lgamma)

extern float __attribute__((overloadable)) log(float);
FN_FUNC_FN(log)

extern float __attribute__((overloadable)) log10(float);
FN_FUNC_FN(log10)


extern float __attribute__((overloadable)) log2(float v) {
    return log10(v) * 3.321928095f;
}
FN_FUNC_FN(log2)

extern float __attribute__((overloadable)) log1p(float);
FN_FUNC_FN(log1p)

extern float __attribute__((overloadable)) logb(float);
FN_FUNC_FN(logb)

extern float __attribute__((overloadable)) mad(float a, float b, float c) {
    return a * b + c;
}
extern float2 __attribute__((overloadable)) mad(float2 a, float2 b, float2 c) {
    return a * b + c;
}
extern float3 __attribute__((overloadable)) mad(float3 a, float3 b, float3 c) {
    return a * b + c;
}
extern float4 __attribute__((overloadable)) mad(float4 a, float4 b, float4 c) {
    return a * b + c;
}

extern float __attribute__((overloadable)) modf(float, float *);
FN_FUNC_FN_PFN(modf);

extern float __attribute__((overloadable)) nan(uint v) {
    float f[1];
    uint32_t *ip = (uint32_t *)f;
    *ip = v | 0x7fc00000;
    return f[0];
}

extern float __attribute__((overloadable)) nextafter(float, float);
FN_FUNC_FN_FN(nextafter)

FN_FUNC_FN_FN(pow)

extern float __attribute__((overloadable)) pown(float v, int p) {
    return pow(v, (float)p);
}
extern float2 __attribute__((overloadable)) pown(float2 v, int2 p) {
    float2 f2 = convert_float2(p);
    return pow(v, f2);
}
extern float3 __attribute__((overloadable)) pown(float3 v, int3 p) {
    float3 f3 = convert_float3(p);
    return pow(v, f3);
}
extern float4 __attribute__((overloadable)) pown(float4 v, int4 p) {
    float4 f4 = convert_float4(p);
    return pow(v, f4);
}

extern float __attribute__((overloadable)) powr(float v, float p) {
    return pow(v, p);
}
extern float2 __attribute__((overloadable)) powr(float2 v, float2 p) {
    return pow(v, p);
}
extern float3 __attribute__((overloadable)) powr(float3 v, float3 p) {
    return pow(v, p);
}
extern float4 __attribute__((overloadable)) powr(float4 v, float4 p) {
    return pow(v, p);
}

extern float __attribute__((overloadable)) remainder(float, float);
FN_FUNC_FN_FN(remainder)

extern float __attribute__((overloadable)) remquo(float, float, int *);
FN_FUNC_FN_FN_PIN(remquo)

extern float __attribute__((overloadable)) rint(float);
FN_FUNC_FN(rint)

extern float __attribute__((overloadable)) rootn(float v, int r) {
    if (r == 0) {
        return nan(0);
    }

    if (iszero(v)) {
        if (r < 0) {
            if (r & 1) {
                return copysign(posinf(), v);
            } else {
                return posinf();
            }
        } else {
            if (r & 1) {
                return copysign(0.f, v);
            } else {
                return 0.f;
            }
        }
    }

    if (!isinf(v) && !isnan(v) && (v < 0.f)) {
        if (r & 1) {
            return (-1.f * pow(-1.f * v, 1.f / r));
        } else {
            return nan(0);
        }
    }

    return pow(v, 1.f / r);
}
FN_FUNC_FN_IN(rootn);

extern float __attribute__((overloadable)) round(float);
FN_FUNC_FN(round)


extern float __attribute__((overloadable)) sqrt(float);
extern float __attribute__((overloadable)) rsqrt(float v) {
    return 1.f / sqrt(v);
}

#if !defined(ARCH_X86_HAVE_SSE2) && !defined(ARCH_X86_HAVE_SSE3)
FN_FUNC_FN(sqrt)
#endif // !defined(ARCH_X86_HAVE_SSE2) && !defined(ARCH_X86_HAVE_SSE3)

FN_FUNC_FN(rsqrt)

extern float __attribute__((overloadable)) sin(float);
FN_FUNC_FN(sin)

extern float __attribute__((overloadable)) sincos(float v, float *cosptr) {
    *cosptr = cos(v);
    return sin(v);
}
extern float2 __attribute__((overloadable)) sincos(float2 v, float2 *cosptr) {
    *cosptr = cos(v);
    return sin(v);
}
extern float3 __attribute__((overloadable)) sincos(float3 v, float3 *cosptr) {
    *cosptr = cos(v);
    return sin(v);
}
extern float4 __attribute__((overloadable)) sincos(float4 v, float4 *cosptr) {
    *cosptr = cos(v);
    return sin(v);
}

extern float __attribute__((overloadable)) sinh(float);
FN_FUNC_FN(sinh)

extern float __attribute__((overloadable)) sinpi(float v) {
    return sin(v * M_PI);
}
FN_FUNC_FN(sinpi)

extern float __attribute__((overloadable)) tan(float);
FN_FUNC_FN(tan)

extern float __attribute__((overloadable)) tanh(float);
FN_FUNC_FN(tanh)

extern float __attribute__((overloadable)) tanpi(float v) {
    return tan(v * M_PI);
}
FN_FUNC_FN(tanpi)


extern float __attribute__((overloadable)) tgamma(float);
FN_FUNC_FN(tgamma)

extern float __attribute__((overloadable)) trunc(float);
FN_FUNC_FN(trunc)

// Int ops (partial), 6.11.3

#define XN_FUNC_YN(typeout, fnc, typein)                                \
extern typeout __attribute__((overloadable)) fnc(typein);               \
extern typeout##2 __attribute__((overloadable)) fnc(typein##2 v) {  \
    typeout##2 r;                                                       \
    r.x = fnc(v.x);                                                     \
    r.y = fnc(v.y);                                                     \
    return r;                                                           \
}                                                                       \
extern typeout##3 __attribute__((overloadable)) fnc(typein##3 v) {  \
    typeout##3 r;                                                       \
    r.x = fnc(v.x);                                                     \
    r.y = fnc(v.y);                                                     \
    r.z = fnc(v.z);                                                     \
    return r;                                                           \
}                                                                       \
extern typeout##4 __attribute__((overloadable)) fnc(typein##4 v) {  \
    typeout##4 r;                                                       \
    r.x = fnc(v.x);                                                     \
    r.y = fnc(v.y);                                                     \
    r.z = fnc(v.z);                                                     \
    r.w = fnc(v.w);                                                     \
    return r;                                                           \
}


#define UIN_FUNC_IN(fnc)          \
XN_FUNC_YN(uchar, fnc, char)      \
XN_FUNC_YN(ushort, fnc, short)    \
XN_FUNC_YN(uint, fnc, int)

#define IN_FUNC_IN(fnc)           \
XN_FUNC_YN(uchar, fnc, uchar)     \
XN_FUNC_YN(char, fnc, char)       \
XN_FUNC_YN(ushort, fnc, ushort)   \
XN_FUNC_YN(short, fnc, short)     \
XN_FUNC_YN(uint, fnc, uint)       \
XN_FUNC_YN(int, fnc, int)


#define XN_FUNC_XN_XN_BODY(type, fnc, body)         \
extern type __attribute__((overloadable))       \
        fnc(type v1, type v2) {                     \
    return body;                                    \
}                                                   \
extern type##2 __attribute__((overloadable))    \
        fnc(type##2 v1, type##2 v2) {               \
    type##2 r;                                      \
    r.x = fnc(v1.x, v2.x);                          \
    r.y = fnc(v1.y, v2.y);                          \
    return r;                                       \
}                                                   \
extern type##3 __attribute__((overloadable))    \
        fnc(type##3 v1, type##3 v2) {               \
    type##3 r;                                      \
    r.x = fnc(v1.x, v2.x);                          \
    r.y = fnc(v1.y, v2.y);                          \
    r.z = fnc(v1.z, v2.z);                          \
    return r;                                       \
}                                                   \
extern type##4 __attribute__((overloadable))    \
        fnc(type##4 v1, type##4 v2) {               \
    type##4 r;                                      \
    r.x = fnc(v1.x, v2.x);                          \
    r.y = fnc(v1.y, v2.y);                          \
    r.z = fnc(v1.z, v2.z);                          \
    r.w = fnc(v1.w, v2.w);                          \
    return r;                                       \
}

#define IN_FUNC_IN_IN_BODY(fnc, body) \
XN_FUNC_XN_XN_BODY(uchar, fnc, body)  \
XN_FUNC_XN_XN_BODY(char, fnc, body)   \
XN_FUNC_XN_XN_BODY(ushort, fnc, body) \
XN_FUNC_XN_XN_BODY(short, fnc, body)  \
XN_FUNC_XN_XN_BODY(uint, fnc, body)   \
XN_FUNC_XN_XN_BODY(int, fnc, body)    \
XN_FUNC_XN_XN_BODY(float, fnc, body)


/**
 * abs
 */
extern uint32_t __attribute__((overloadable)) abs(int32_t v) {
    if (v < 0)
        return -v;
    return v;
}
extern uint16_t __attribute__((overloadable)) abs(int16_t v) {
    if (v < 0)
        return -v;
    return v;
}
extern uint8_t __attribute__((overloadable)) abs(int8_t v) {
    if (v < 0)
        return -v;
    return v;
}

/**
 * clz
 */
extern uint32_t __attribute__((overloadable)) clz(uint32_t v) {
    return __builtin_clz(v);
}
extern uint16_t __attribute__((overloadable)) clz(uint16_t v) {
    return (uint16_t)__builtin_clz(v);
}
extern uint8_t __attribute__((overloadable)) clz(uint8_t v) {
    return (uint8_t)__builtin_clz(v);
}
extern int32_t __attribute__((overloadable)) clz(int32_t v) {
    return (int32_t)__builtin_clz((uint32_t)v);
}
extern int16_t __attribute__((overloadable)) clz(int16_t v) {
    return (int16_t)__builtin_clz(v);
}
extern int8_t __attribute__((overloadable)) clz(int8_t v) {
    return (int8_t)__builtin_clz(v);
}


UIN_FUNC_IN(abs)
IN_FUNC_IN(clz)


// 6.11.4


extern float __attribute__((overloadable)) degrees(float radians) {
    return radians * (180.f / M_PI);
}
extern float2 __attribute__((overloadable)) degrees(float2 radians) {
    return radians * (180.f / M_PI);
}
extern float3 __attribute__((overloadable)) degrees(float3 radians) {
    return radians * (180.f / M_PI);
}
extern float4 __attribute__((overloadable)) degrees(float4 radians) {
    return radians * (180.f / M_PI);
}

extern float __attribute__((overloadable)) mix(float start, float stop, float amount) {
    return start + (stop - start) * amount;
}
extern float2 __attribute__((overloadable)) mix(float2 start, float2 stop, float2 amount) {
    return start + (stop - start) * amount;
}
extern float3 __attribute__((overloadable)) mix(float3 start, float3 stop, float3 amount) {
    return start + (stop - start) * amount;
}
extern float4 __attribute__((overloadable)) mix(float4 start, float4 stop, float4 amount) {
    return start + (stop - start) * amount;
}
extern float2 __attribute__((overloadable)) mix(float2 start, float2 stop, float amount) {
    return start + (stop - start) * amount;
}
extern float3 __attribute__((overloadable)) mix(float3 start, float3 stop, float amount) {
    return start + (stop - start) * amount;
}
extern float4 __attribute__((overloadable)) mix(float4 start, float4 stop, float amount) {
    return start + (stop - start) * amount;
}

extern float __attribute__((overloadable)) radians(float degrees) {
    return degrees * (M_PI / 180.f);
}
extern float2 __attribute__((overloadable)) radians(float2 degrees) {
    return degrees * (M_PI / 180.f);
}
extern float3 __attribute__((overloadable)) radians(float3 degrees) {
    return degrees * (M_PI / 180.f);
}
extern float4 __attribute__((overloadable)) radians(float4 degrees) {
    return degrees * (M_PI / 180.f);
}

extern float __attribute__((overloadable)) step(float edge, float v) {
    return (v < edge) ? 0.f : 1.f;
}
extern float2 __attribute__((overloadable)) step(float2 edge, float2 v) {
    float2 r;
    r.x = (v.x < edge.x) ? 0.f : 1.f;
    r.y = (v.y < edge.y) ? 0.f : 1.f;
    return r;
}
extern float3 __attribute__((overloadable)) step(float3 edge, float3 v) {
    float3 r;
    r.x = (v.x < edge.x) ? 0.f : 1.f;
    r.y = (v.y < edge.y) ? 0.f : 1.f;
    r.z = (v.z < edge.z) ? 0.f : 1.f;
    return r;
}
extern float4 __attribute__((overloadable)) step(float4 edge, float4 v) {
    float4 r;
    r.x = (v.x < edge.x) ? 0.f : 1.f;
    r.y = (v.y < edge.y) ? 0.f : 1.f;
    r.z = (v.z < edge.z) ? 0.f : 1.f;
    r.w = (v.w < edge.w) ? 0.f : 1.f;
    return r;
}
extern float2 __attribute__((overloadable)) step(float2 edge, float v) {
    float2 r;
    r.x = (v < edge.x) ? 0.f : 1.f;
    r.y = (v < edge.y) ? 0.f : 1.f;
    return r;
}
extern float3 __attribute__((overloadable)) step(float3 edge, float v) {
    float3 r;
    r.x = (v < edge.x) ? 0.f : 1.f;
    r.y = (v < edge.y) ? 0.f : 1.f;
    r.z = (v < edge.z) ? 0.f : 1.f;
    return r;
}
extern float4 __attribute__((overloadable)) step(float4 edge, float v) {
    float4 r;
    r.x = (v < edge.x) ? 0.f : 1.f;
    r.y = (v < edge.y) ? 0.f : 1.f;
    r.z = (v < edge.z) ? 0.f : 1.f;
    r.w = (v < edge.w) ? 0.f : 1.f;
    return r;
}

extern float __attribute__((overloadable)) smoothstep(float, float, float);
extern float2 __attribute__((overloadable)) smoothstep(float2, float2, float2);
extern float3 __attribute__((overloadable)) smoothstep(float3, float3, float3);
extern float4 __attribute__((overloadable)) smoothstep(float4, float4, float4);
extern float2 __attribute__((overloadable)) smoothstep(float, float, float2);
extern float3 __attribute__((overloadable)) smoothstep(float, float, float3);
extern float4 __attribute__((overloadable)) smoothstep(float, float, float4);

extern float __attribute__((overloadable)) sign(float v) {
    if (v > 0) return 1.f;
    if (v < 0) return -1.f;
    return v;
}
FN_FUNC_FN(sign)


// 6.11.5
extern float3 __attribute__((overloadable)) cross(float3 lhs, float3 rhs) {
    float3 r;
    r.x = lhs.y * rhs.z  - lhs.z * rhs.y;
    r.y = lhs.z * rhs.x  - lhs.x * rhs.z;
    r.z = lhs.x * rhs.y  - lhs.y * rhs.x;
    return r;
}

extern float4 __attribute__((overloadable)) cross(float4 lhs, float4 rhs) {
    float4 r;
    r.x = lhs.y * rhs.z  - lhs.z * rhs.y;
    r.y = lhs.z * rhs.x  - lhs.x * rhs.z;
    r.z = lhs.x * rhs.y  - lhs.y * rhs.x;
    r.w = 0.f;
    return r;
}

#if !defined(ARCH_X86_HAVE_SSE3)

extern float __attribute__((overloadable)) dot(float lhs, float rhs) {
    return lhs * rhs;
}
extern float __attribute__((overloadable)) dot(float2 lhs, float2 rhs) {
    return lhs.x*rhs.x + lhs.y*rhs.y;
}
extern float __attribute__((overloadable)) dot(float3 lhs, float3 rhs) {
    return lhs.x*rhs.x + lhs.y*rhs.y + lhs.z*rhs.z;
}
extern float __attribute__((overloadable)) dot(float4 lhs, float4 rhs) {
    return lhs.x*rhs.x + lhs.y*rhs.y + lhs.z*rhs.z + lhs.w*rhs.w;
}

extern float __attribute__((overloadable)) length(float v) {
    return fabs(v);
}
extern float __attribute__((overloadable)) length(float2 v) {
    return sqrt(v.x*v.x + v.y*v.y);
}
extern float __attribute__((overloadable)) length(float3 v) {
    return sqrt(v.x*v.x + v.y*v.y + v.z*v.z);
}
extern float __attribute__((overloadable)) length(float4 v) {
    return sqrt(v.x*v.x + v.y*v.y + v.z*v.z + v.w*v.w);
}

#else

extern float __attribute__((overloadable)) length(float v);
extern float __attribute__((overloadable)) length(float2 v);
extern float __attribute__((overloadable)) length(float3 v);
extern float __attribute__((overloadable)) length(float4 v);

#endif

extern float __attribute__((overloadable)) distance(float lhs, float rhs) {
    return length(lhs - rhs);
}
extern float __attribute__((overloadable)) distance(float2 lhs, float2 rhs) {
    return length(lhs - rhs);
}
extern float __attribute__((overloadable)) distance(float3 lhs, float3 rhs) {
    return length(lhs - rhs);
}
extern float __attribute__((overloadable)) distance(float4 lhs, float4 rhs) {
    return length(lhs - rhs);
}

extern float __attribute__((overloadable)) normalize(float v) {
    return 1.f;
}
extern float2 __attribute__((overloadable)) normalize(float2 v) {
    return v / length(v);
}
extern float3 __attribute__((overloadable)) normalize(float3 v) {
    return v / length(v);
}
extern float4 __attribute__((overloadable)) normalize(float4 v) {
    return v / length(v);
}

extern float __attribute__((overloadable)) half_sqrt(float);

extern float __attribute__((overloadable)) fast_length(float v) {
    return fabs(v);
}
extern float __attribute__((overloadable)) fast_length(float2 v) {
    return half_sqrt(v.x*v.x + v.y*v.y);
}
extern float __attribute__((overloadable)) fast_length(float3 v) {
    return half_sqrt(v.x*v.x + v.y*v.y + v.z*v.z);
}
extern float __attribute__((overloadable)) fast_length(float4 v) {
    return half_sqrt(v.x*v.x + v.y*v.y + v.z*v.z + v.w*v.w);
}

extern float __attribute__((overloadable)) fast_distance(float lhs, float rhs) {
    return fast_length(lhs - rhs);
}
extern float __attribute__((overloadable)) fast_distance(float2 lhs, float2 rhs) {
    return fast_length(lhs - rhs);
}
extern float __attribute__((overloadable)) fast_distance(float3 lhs, float3 rhs) {
    return fast_length(lhs - rhs);
}
extern float __attribute__((overloadable)) fast_distance(float4 lhs, float4 rhs) {
    return fast_length(lhs - rhs);
}

extern float __attribute__((overloadable)) half_rsqrt(float);

extern float __attribute__((overloadable)) fast_normalize(float v) {
    return 1.f;
}
extern float2 __attribute__((overloadable)) fast_normalize(float2 v) {
    return v * half_rsqrt(v.x*v.x + v.y*v.y);
}
extern float3 __attribute__((overloadable)) fast_normalize(float3 v) {
    return v * half_rsqrt(v.x*v.x + v.y*v.y + v.z*v.z);
}
extern float4 __attribute__((overloadable)) fast_normalize(float4 v) {
    return v * half_rsqrt(v.x*v.x + v.y*v.y + v.z*v.z + v.w*v.w);
}

extern float __attribute__((overloadable)) half_recip(float);

/*
extern float __attribute__((overloadable)) approx_atan(float x) {
    if (x == 0.f)
        return 0.f;
    if (x < 0.f)
        return -1.f * approx_atan(-1.f * x);
    if (x > 1.f)
        return M_PI_2 - approx_atan(approx_recip(x));
    return x * approx_recip(1.f + 0.28f * x*x);
}
FN_FUNC_FN(approx_atan)
*/

typedef union
{
  float fv;
  int32_t iv;
} ieee_float_shape_type;

/* Get a 32 bit int from a float.  */

#define GET_FLOAT_WORD(i,d)                 \
do {                                \
  ieee_float_shape_type gf_u;                   \
  gf_u.fv = (d);                     \
  (i) = gf_u.iv;                      \
} while (0)

/* Set a float from a 32 bit int.  */

#define SET_FLOAT_WORD(d,i)                 \
do {                                \
  ieee_float_shape_type sf_u;                   \
  sf_u.iv = (i);                      \
  (d) = sf_u.fv;                     \
} while (0)



// Valid -125 to 125
extern float __attribute__((overloadable)) native_exp2(float v) {
    int32_t iv = (int)v;
    int32_t x = iv + (iv >> 31); // ~floor(v)
    float r = (v - x);

    float fo;
    SET_FLOAT_WORD(fo, (x + 127) << 23);

    r *= 0.694f; // ~ log(e) / log(2)
    float r2 = r*r;
    float adj = 1.f + r + (r2 * 0.5f) + (r2*r * 0.166666f) + (r2*r2 * 0.0416666f);
    return fo * adj;
}

extern float2 __attribute__((overloadable)) native_exp2(float2 v) {
    int2 iv = convert_int2(v);
    int2 x = iv + (iv >> (int2)31);//floor(v);
    float2 r = (v - convert_float2(x));

    x += 127;

    float2 fo = (float2)(x << (int2)23);

    r *= 0.694f; // ~ log(e) / log(2)
    float2 r2 = r*r;
    float2 adj = 1.f + r + (r2 * 0.5f) + (r2*r * 0.166666f) + (r2*r2 * 0.0416666f);
    return fo * adj;
}

extern float4 __attribute__((overloadable)) native_exp2(float4 v) {
    int4 iv = convert_int4(v);
    int4 x = iv + (iv >> (int4)31);//floor(v);
    float4 r = (v - convert_float4(x));

    x += 127;

    float4 fo = (float4)(x << (int4)23);

    r *= 0.694f; // ~ log(e) / log(2)
    float4 r2 = r*r;
    float4 adj = 1.f + r + (r2 * 0.5f) + (r2*r * 0.166666f) + (r2*r2 * 0.0416666f);
    return fo * adj;
}

extern float3 __attribute__((overloadable)) native_exp2(float3 v) {
    float4 t = 1.f;
    t.xyz = v;
    return native_exp2(t).xyz;
}


extern float __attribute__((overloadable)) native_exp(float v) {
    return native_exp2(v * 1.442695041f);
}
extern float2 __attribute__((overloadable)) native_exp(float2 v) {
    return native_exp2(v * 1.442695041f);
}
extern float3 __attribute__((overloadable)) native_exp(float3 v) {
    return native_exp2(v * 1.442695041f);
}
extern float4 __attribute__((overloadable)) native_exp(float4 v) {
    return native_exp2(v * 1.442695041f);
}

extern float __attribute__((overloadable)) native_exp10(float v) {
    return native_exp2(v * 3.321928095f);
}
extern float2 __attribute__((overloadable)) native_exp10(float2 v) {
    return native_exp2(v * 3.321928095f);
}
extern float3 __attribute__((overloadable)) native_exp10(float3 v) {
    return native_exp2(v * 3.321928095f);
}
extern float4 __attribute__((overloadable)) native_exp10(float4 v) {
    return native_exp2(v * 3.321928095f);
}

extern float __attribute__((overloadable)) native_log2(float v) {
    int32_t ibits;
    GET_FLOAT_WORD(ibits, v);

    int32_t e = (ibits >> 23) & 0xff;

    ibits &= 0x7fffff;
    ibits |= 127 << 23;

    float ir;
    SET_FLOAT_WORD(ir, ibits);

    ir -= 1.5f;
    float ir2 = ir*ir;
    float adj2 = 0.405465108f + // -0.00009f +
                 (0.666666667f * ir) -
                 (0.222222222f * ir2) +
                 (0.098765432f * ir*ir2) -
                 (0.049382716f * ir2*ir2) +
                 (0.026337449f * ir*ir2*ir2) -
                 (0.014631916f * ir2*ir2*ir2);
    adj2 *= (1.f / 0.693147181f);

    return (float)(e - 127) + adj2;
}
extern float2 __attribute__((overloadable)) native_log2(float2 v) {
    float2 v2 = {native_log2(v.x), native_log2(v.y)};
    return v2;
}
extern float3 __attribute__((overloadable)) native_log2(float3 v) {
    float3 v2 = {native_log2(v.x), native_log2(v.y), native_log2(v.z)};
    return v2;
}
extern float4 __attribute__((overloadable)) native_log2(float4 v) {
    float4 v2 = {native_log2(v.x), native_log2(v.y), native_log2(v.z), native_log2(v.w)};
    return v2;
}

extern float __attribute__((overloadable)) native_log(float v) {
    return native_log2(v) * (1.f / 1.442695041f);
}
extern float2 __attribute__((overloadable)) native_log(float2 v) {
    return native_log2(v) * (1.f / 1.442695041f);
}
extern float3 __attribute__((overloadable)) native_log(float3 v) {
    return native_log2(v) * (1.f / 1.442695041f);
}
extern float4 __attribute__((overloadable)) native_log(float4 v) {
    return native_log2(v) * (1.f / 1.442695041f);
}

extern float __attribute__((overloadable)) native_log10(float v) {
    return native_log2(v) * (1.f / 3.321928095f);
}
extern float2 __attribute__((overloadable)) native_log10(float2 v) {
    return native_log2(v) * (1.f / 3.321928095f);
}
extern float3 __attribute__((overloadable)) native_log10(float3 v) {
    return native_log2(v) * (1.f / 3.321928095f);
}
extern float4 __attribute__((overloadable)) native_log10(float4 v) {
    return native_log2(v) * (1.f / 3.321928095f);
}


extern float __attribute__((overloadable)) native_powr(float v, float y) {
    float v2 = native_log2(v);
    v2 = fmax(v2, -125.f);
    return native_exp2(v2 * y);
}
extern float2 __attribute__((overloadable)) native_powr(float2 v, float2 y) {
    float2 v2 = native_log2(v);
    v2 = fmax(v2, -125.f);
    return native_exp2(v2 * y);
}
extern float3 __attribute__((overloadable)) native_powr(float3 v, float3 y) {
    float3 v2 = native_log2(v);
    v2 = fmax(v2, -125.f);
    return native_exp2(v2 * y);
}
extern float4 __attribute__((overloadable)) native_powr(float4 v, float4 y) {
    float4 v2 = native_log2(v);
    v2 = fmax(v2, -125.f);
    return native_exp2(v2 * y);
}


#undef FN_FUNC_FN
#undef IN_FUNC_FN
#undef FN_FUNC_FN_FN
#undef FN_FUNC_FN_F
#undef FN_FUNC_FN_IN
#undef FN_FUNC_FN_I
#undef FN_FUNC_FN_PFN
#undef FN_FUNC_FN_PIN
#undef FN_FUNC_FN_FN_FN
#undef FN_FUNC_FN_FN_PIN
#undef XN_FUNC_YN
#undef UIN_FUNC_IN
#undef IN_FUNC_IN
#undef XN_FUNC_XN_XN_BODY
#undef IN_FUNC_IN_IN_BODY
