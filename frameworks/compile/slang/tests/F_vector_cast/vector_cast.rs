#pragma version(1)
#pragma rs java_package_name(foo)

void i2f() {
    int2 i;
    float2 f;
    f = (float2)i;
    f = f * i;
}

void f2i() {
    int3 i;
    float3 f;
    i = (int3)f;
    i = i * f;
}

void c2uc() {
    char4 c;
    uchar4 u;
    u = (uchar4) c;
}

uchar4 bar(uchar4 u) {
    return u;
}

void c2uc_bar() {
    char4 c;
    uchar4 u;
    u = bar((uchar4) c);
}

