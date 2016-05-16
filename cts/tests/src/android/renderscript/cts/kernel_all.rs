#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

uchar __attribute__((kernel)) test_i8(char ain) {
    return ain + 1;
}

uchar2 __attribute__((kernel)) test_i8_2(char2 ain) {
    uchar2 r;
    r.x = ain.x + 1;
    r.y = ain.y + 1;
    return r;
}

uchar3 __attribute__((kernel)) test_i8_3(char3 ain) {
    uchar3 r;
    r.x = ain.x + 1;
    r.y = ain.y + 1;
    r.z = ain.z + 1;
    return r;
}

uchar4 __attribute__((kernel)) test_i8_4(char4 ain) {
    uchar4 r;
    r.x = ain.x + 1;
    r.y = ain.y + 1;
    r.z = ain.z + 1;
    r.w = ain.w + 1;
    return r;
}

ushort __attribute__((kernel)) test_i16(short ain) {
    return ain + 1;
}

ushort2 __attribute__((kernel)) test_i16_2(short2 ain) {
    ushort2 r;
    r.x = ain.x + 1;
    r.y = ain.y + 1;
    return r;
}

ushort3 __attribute__((kernel)) test_i16_3(short3 ain) {
    ushort3 r;
    r.x = ain.x + 1;
    r.y = ain.y + 1;
    r.z = ain.z + 1;
    return r;
}

ushort4 __attribute__((kernel)) test_i16_4(short4 ain) {
    ushort4 r;
    r.x = ain.x + 1;
    r.y = ain.y + 1;
    r.z = ain.z + 1;
    r.w = ain.w + 1;
    return r;
}

uint __attribute__((kernel)) test_i32(int ain) {
    return ain + 1;
}

uint2 __attribute__((kernel)) test_i32_2(int2 ain) {
    uint2 r;
    r.x = ain.x + 1;
    r.y = ain.y + 1;
    return r;
}

uint3 __attribute__((kernel)) test_i32_3(int3 ain) {
    uint3 r;
    r.x = ain.x + 1;
    r.y = ain.y + 1;
    r.z = ain.z + 1;
    return r;
}

uint4 __attribute__((kernel)) test_i32_4(int4 ain) {
    uint4 r;
    r.x = ain.x + 1;
    r.y = ain.y + 1;
    r.z = ain.z + 1;
    r.w = ain.w + 1;
    return r;
}

ulong __attribute__((kernel)) test_i64(long ain) {
    return ain + 1;
}

ulong2 __attribute__((kernel)) test_i64_2(long2 ain) {
    ulong2 r;
    r.x = ain.x + 1;
    r.y = ain.y + 1;
    return r;
}

ulong3 __attribute__((kernel)) test_i64_3(long3 ain) {
    ulong3 r;
    r.x = ain.x + 1;
    r.y = ain.y + 1;
    r.z = ain.z + 1;
    return r;
}

ulong4 __attribute__((kernel)) test_i64_4(long4 ain) {
    ulong4 r;
    r.x = ain.x + 1;
    r.y = ain.y + 1;
    r.z = ain.z + 1;
    r.w = ain.w + 1;
    return r;
}

float __attribute__((kernel)) test_f32(float ain) {
    return ain + 1.0f;
}

float2 __attribute__((kernel)) test_f32_2(float2 ain) {
    float2 r;
    r.x = ain.x + 1.0f;
    r.y = ain.y + 1.0f;
    return r;
}

float3 __attribute__((kernel)) test_f32_3(float3 ain) {
    float3 r;
    r.x = ain.x + 1.0f;
    r.y = ain.y + 1.0f;
    r.z = ain.z + 1.0f;
    return r;
}

float4 __attribute__((kernel)) test_f32_4(float4 ain) {
    float4 r;
    r.x = ain.x + 1.0f;
    r.y = ain.y + 1.0f;
    r.z = ain.z + 1.0f;
    r.w = ain.w + 1.0f;
    return r;
}

double __attribute__((kernel)) test_f64(double ain) {
    return ain + 1.0;
}

double2 __attribute__((kernel)) test_f64_2(double2 ain) {
    double2 r;
    r.x = ain.x + 1.0;
    r.y = ain.y + 1.0;
    return r;
}

double3 __attribute__((kernel)) test_f64_3(double3 ain) {
    double3 r;
    r.x = ain.x + 1.0;
    r.y = ain.y + 1.0;
    r.z = ain.z + 1.0;
    return r;
}

double4 __attribute__((kernel)) test_f64_4(double4 ain) {
    double4 r;
    r.x = ain.x + 1.0;
    r.y = ain.y + 1.0;
    r.z = ain.z + 1.0;
    r.w = ain.w + 1.0;
    return r;
}

struct kernel_test {
    int i;
    char ignored1;
    float f;
};

struct kernel_test __attribute__((kernel)) test_struct(struct kernel_test ain) {
    struct kernel_test r;
    r.i = ain.i + 1;
    r.f = ain.f + 1.0f;
    return r;
}

bool __attribute__((kernel)) test_bool(bool ain) {
    return !ain;
}
