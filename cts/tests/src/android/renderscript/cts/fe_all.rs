#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

void test_i8(const char *ain, uchar *aout) {
    aout[0] = ain[0] + 1;
    return;
}

void test_i8_2(const char2 *ain, uchar2 *aout) {
    aout[0].x = ain[0].x + 1;
    aout[0].y = ain[0].y + 1;
    return;
}

void test_i8_3(const char3 *ain, uchar3 *aout) {
    aout[0].x = ain[0].x + 1;
    aout[0].y = ain[0].y + 1;
    aout[0].z = ain[0].z + 1;
    return;
}

void test_i8_4(const char4 *ain, uchar4 *aout) {
    aout[0].x = ain[0].x + 1;
    aout[0].y = ain[0].y + 1;
    aout[0].z = ain[0].z + 1;
    aout[0].w = ain[0].w + 1;
    return;
}

void test_i16(const short *ain, ushort *aout) {
    aout[0] = ain[0] + 1;
    return;
}

void test_i16_2(const short2 *ain, ushort2 *aout) {
    aout[0].x = ain[0].x + 1;
    aout[0].y = ain[0].y + 1;
    return;
}

void test_i16_3(const short3 *ain, ushort3 *aout) {
    aout[0].x = ain[0].x + 1;
    aout[0].y = ain[0].y + 1;
    aout[0].z = ain[0].z + 1;
    return;
}

void test_i16_4(const short4 *ain, ushort4 *aout) {
    aout[0].x = ain[0].x + 1;
    aout[0].y = ain[0].y + 1;
    aout[0].z = ain[0].z + 1;
    aout[0].w = ain[0].w + 1;
    return;
}

void test_i32(const int *ain, uint *aout) {
    aout[0] = ain[0] + 1;
    return;
}

void test_i32_2(const int2 *ain, uint2 *aout) {
    aout[0].x = ain[0].x + 1;
    aout[0].y = ain[0].y + 1;
    return;
}

void test_i32_3(const int3 *ain, uint3 *aout) {
    aout[0].x = ain[0].x + 1;
    aout[0].y = ain[0].y + 1;
    aout[0].z = ain[0].z + 1;
    return;
}

void test_i32_4(const int4 *ain, uint4 *aout) {
    aout[0].x = ain[0].x + 1;
    aout[0].y = ain[0].y + 1;
    aout[0].z = ain[0].z + 1;
    aout[0].w = ain[0].w + 1;
    return;
}

void test_i64(const long *ain, ulong *aout) {
    aout[0] = ain[0] + 1;
    return;
}

void test_i64_2(const long2 *ain, ulong2 *aout) {
    aout[0].x = ain[0].x + 1;
    aout[0].y = ain[0].y + 1;
    return;
}

void test_i64_3(const long3 *ain, ulong3 *aout) {
    aout[0].x = ain[0].x + 1;
    aout[0].y = ain[0].y + 1;
    aout[0].z = ain[0].z + 1;
    return;
}

void test_i64_4(const long4 *ain, ulong4 *aout) {
    aout[0].x = ain[0].x + 1;
    aout[0].y = ain[0].y + 1;
    aout[0].z = ain[0].z + 1;
    aout[0].w = ain[0].w + 1;
    return;
}

void test_f32(const float *ain, float *aout) {
    aout[0] = ain[0] + 1.0f;
    return;
}

void test_f32_2(const float2 *ain, float2 *aout) {
    aout[0].x = ain[0].x + 1.0f;
    aout[0].y = ain[0].y + 1.0f;
    return;
}

void test_f32_3(const float3 *ain, float3 *aout) {
    aout[0].x = ain[0].x + 1.0f;
    aout[0].y = ain[0].y + 1.0f;
    aout[0].z = ain[0].z + 1.0f;
    return;
}

void test_f32_4(const float4 *ain, float4 *aout) {
    aout[0].x = ain[0].x + 1.0f;
    aout[0].y = ain[0].y + 1.0f;
    aout[0].z = ain[0].z + 1.0f;
    aout[0].w = ain[0].w + 1.0f;
    return;
}

void test_f64(const double *ain, double *aout) {
    aout[0] = ain[0] + 1.0;
    return;
}

void test_f64_2(const double2 *ain, double2 *aout) {
    aout[0].x = ain[0].x + 1.0;
    aout[0].y = ain[0].y + 1.0;
    return;
}

void test_f64_3(const double3 *ain, double3 *aout) {
    aout[0].x = ain[0].x + 1.0;
    aout[0].y = ain[0].y + 1.0;
    aout[0].z = ain[0].z + 1.0;
    return;
}

void test_f64_4(const double4 *ain, double4 *aout) {
    aout[0].x = ain[0].x + 1.0;
    aout[0].y = ain[0].y + 1.0;
    aout[0].z = ain[0].z + 1.0;
    aout[0].w = ain[0].w + 1.0;
    return;
}

struct fe_test {
    int i;
    float f;
};

void test_struct(const struct fe_test *ain, struct fe_test *aout) {
    aout[0].i = ain[0].i + 1;
    aout[0].f = ain[0].f + 1.0f;
    return;
}

void test_bool(const bool *ain, bool *aout) {
    aout[0] = !ain[0];
    return;
}
