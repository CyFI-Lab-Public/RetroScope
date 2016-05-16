#pragma version(1)
#pragma rs java_package_name(foo)

uchar uc;
uchar2 uc2;
uchar3 uc3;
uchar4 uc4;

ushort us;
ushort2 us2;
ushort3 us3;
ushort4 us4;

uint ui;
uint2 ui2;
uint3 ui3;
uint4 ui4;

char c;
char2 c2;
char3 c3;
char4 c4;

short s;
short2 s2;
short3 s3;
short4 s4;

int i;
int2 i2;
int3 i3;
int4 i4;

float f;
float2 f2;
float3 f3;
float4 f4;

#define TEST4_1(ret, typ, fnc)  \
    ret = fnc(typ);             \
    ret##2 = fnc(typ##2);       \
    ret##3 = fnc(typ##3);       \
    ret##4 = fnc(typ##4);

#define TEST4_2(typ, fnc)           \
    typ = fnc(typ, typ);            \
    typ##2 = fnc(typ##2, typ##2);   \
    typ##3 = fnc(typ##3, typ##3);   \
    typ##4 = fnc(typ##4, typ##4);

#define TEST4_2S(typ, fnc)          \
    typ = fnc(typ, typ);            \
    typ##2 = fnc(typ##2, typ);      \
    typ##3 = fnc(typ##3, typ);      \
    typ##4 = fnc(typ##4, typ);

#define TEST_UIFUNC_1(fnc)  \
    TEST4_1(uc, c, fnc);    \
    TEST4_1(us, s, fnc);    \
    TEST4_1(ui, i, fnc);

#define TEST_IFUNC_1(fnc)   \
    TEST4_1(uc, uc, fnc);   \
    TEST4_1(c, c, fnc);     \
    TEST4_1(us, us, fnc);   \
    TEST4_1(s, s, fnc);     \
    TEST4_1(ui, ui, fnc);   \
    TEST4_1(i, i, fnc);

#define TEST_IFUNC_2(fnc)   \
    TEST4_2(uc, fnc);       \
    TEST4_2(c, fnc);        \
    TEST4_2(us, fnc);       \
    TEST4_2(s, fnc);        \
    TEST4_2(ui, fnc);       \
    TEST4_2(f, fnc);        \


void compile_all_math_int_ops() {
    TEST_UIFUNC_1(abs);
    TEST_IFUNC_1(clz);
    TEST_IFUNC_2(min);
    TEST_IFUNC_2(max);
    TEST4_2S(f, min);
    TEST4_2S(f, max);

    return;
}

