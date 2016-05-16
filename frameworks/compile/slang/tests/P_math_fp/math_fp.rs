#pragma version(1)
#pragma rs java_package_name(foo)

int i;
int2 i2;
int3 i3;
int4 i4;

float f;
float2 f2;
float3 f3;
float4 f4;

#define TEST_FUNC_1(fnc)    \
    f = fnc(f);             \
    f2 = fnc(f2);           \
    f3 = fnc(f3);           \
    f4 = fnc(f4);

#define TEST_FUNC_1_RI(fnc) \
    i = fnc(f);             \
    i2 = fnc(f2);           \
    i3 = fnc(f3);           \
    i4 = fnc(f4);

#define TEST_FUNC_2(fnc)    \
    f = fnc(f, f);          \
    f2 = fnc(f2, f2);       \
    f3 = fnc(f3, f3);       \
    f4 = fnc(f4, f4);

#define TEST_FUNC_2P(fnc)   \
    f = fnc(f, &f);         \
    f2 = fnc(f2, &f2);      \
    f3 = fnc(f3, &f3);      \
    f4 = fnc(f4, &f4);

#define TEST_FUNC_2PI(fnc)  \
    f = fnc(f, &i);         \
    f2 = fnc(f2, &i2);      \
    f3 = fnc(f3, &i3);      \
    f4 = fnc(f4, &i4);

#define TEST_FUNC_2F(fnc)   \
    f = fnc(f, f);          \
    f2 = fnc(f2, f2);       \
    f3 = fnc(f3, f3);       \
    f4 = fnc(f4, f4);

#define TEST_FUNC_2I(fnc)   \
    f = fnc(f, i);          \
    f2 = fnc(f2, i);        \
    f3 = fnc(f3, i);        \
    f4 = fnc(f4, i);

#define TEST_FUNC_2IN(fnc)  \
    f = fnc(f, i);          \
    f2 = fnc(f2, i2);       \
    f3 = fnc(f3, i3);       \
    f4 = fnc(f4, i4);

#define TEST_FUNC_3(fnc)    \
    f = fnc(f, f, f);       \
    f2 = fnc(f2, f2, f2);   \
    f3 = fnc(f3, f3, f3);   \
    f4 = fnc(f4, f4, f4);

#define TEST_FUNC_3PI(fnc)  \
    f = fnc(f, f, &i);      \
    f2 = fnc(f2, f2, &i2);  \
    f3 = fnc(f3, f3, &i3);  \
    f4 = fnc(f4, f4, &i4);

void compile_all_math_fp_ops() {
    TEST_FUNC_1(acos);
    TEST_FUNC_1(acosh);
    TEST_FUNC_1(acospi);
    TEST_FUNC_1(asin);
    TEST_FUNC_1(asinh);
    TEST_FUNC_1(asinpi);
    TEST_FUNC_1(atan);
    TEST_FUNC_2(atan2);
    TEST_FUNC_1(atanh);
    TEST_FUNC_1(atanpi);
    TEST_FUNC_2(atan2pi);
    TEST_FUNC_1(cbrt);
    TEST_FUNC_1(ceil);
    TEST_FUNC_2(copysign);
    TEST_FUNC_1(cos);
    TEST_FUNC_1(cosh);
    TEST_FUNC_1(cospi);
    TEST_FUNC_1(erfc);
    TEST_FUNC_1(erf);
    TEST_FUNC_1(exp);
    TEST_FUNC_1(exp2);
    TEST_FUNC_1(exp10);
    TEST_FUNC_1(expm1);
    TEST_FUNC_1(fabs);
    TEST_FUNC_2(fdim);
    TEST_FUNC_1(floor);
    TEST_FUNC_3(fma);
    TEST_FUNC_2(fmax);
    TEST_FUNC_2F(fmax);
    TEST_FUNC_2(fmin);
    TEST_FUNC_2F(fmin);
    TEST_FUNC_2(fmod);
    TEST_FUNC_2P(fract);
    TEST_FUNC_2PI(frexp);
    TEST_FUNC_2(hypot);
    TEST_FUNC_1_RI(ilogb);
    TEST_FUNC_2IN(ldexp);
    TEST_FUNC_2I(ldexp);
    TEST_FUNC_1(lgamma);
    TEST_FUNC_2PI(lgamma);
    TEST_FUNC_1(log);
    TEST_FUNC_1(log2);
    TEST_FUNC_1(log10);
    TEST_FUNC_1(log1p);
    TEST_FUNC_1(logb);
    TEST_FUNC_3(mad);
    TEST_FUNC_2P(modf);
    //TEST_FUNC_1(nan);
    TEST_FUNC_2(nextafter);
    TEST_FUNC_2(pow);
    TEST_FUNC_2I(pown);
    TEST_FUNC_2(powr);
    TEST_FUNC_2(remainder);
    TEST_FUNC_3PI(remquo);
    TEST_FUNC_1(rint);
    TEST_FUNC_2I(rootn);
    TEST_FUNC_1(round);
    TEST_FUNC_1(rsqrt);
    TEST_FUNC_1(sin);
    TEST_FUNC_2P(sincos);
    TEST_FUNC_1(sinh);
    TEST_FUNC_1(sinpi);
    TEST_FUNC_1(sqrt);
    TEST_FUNC_1(tan);
    TEST_FUNC_1(tanh);
    TEST_FUNC_1(tanpi);
    TEST_FUNC_1(tgamma);
    TEST_FUNC_1(trunc);

    return;
}

