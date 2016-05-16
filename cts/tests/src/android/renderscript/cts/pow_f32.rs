#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct PowInputData {
    float base;
    float expo;
} PowInputData;

void pow_f32_1(const PowInputData *in, float *out) {
    *out = pow(in->base, in->expo);
}

typedef struct PowInputData_2 {
    float2 base;
    float2 expo;
} PowInputData_2;

void pow_f32_2(const PowInputData_2 *in, float2 *out) {
    *out = pow(in->base, in->expo);
}

typedef struct PowInputData_3 {
    float3 base;
    float3 expo;
} PowInputData_3;

void pow_f32_3(const PowInputData_3 *in, float3 *out) {
    *out = pow(in->base, in->expo);
}

typedef struct PowInputData_4 {
    float4 base;
    float4 expo;
} PowInputData_4;

void pow_f32_4(const PowInputData_4 *in, float4 *out) {
    *out = pow(in->base, in->expo);
}
