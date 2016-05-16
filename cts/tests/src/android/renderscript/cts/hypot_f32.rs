#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct hypot_f32_in {
    float x;
    float y;
} hypot_input_f32;

void hypot_f32_1(const hypot_input_f32 *in, float *out) {
    *out = hypot(in->x, in->y);
}

typedef struct hypot_f32_2_in {
    float2 x;
    float2 y;
} hypot_input_f32_2;

void hypot_f32_2(const hypot_input_f32_2 *in, float2 *out) {
    *out = hypot(in->x, in->y);
}

typedef struct hypot_f32_3_in {
    float3 x;
    float3 y;
} hypot_input_f32_3;

void hypot_f32_3(const hypot_input_f32_3 *in, float3 *out) {
    *out = hypot(in->x, in->y);
}

typedef struct hypot_f32_4_in {
    float4 x;
    float4 y;
} hypot_input_f32_4;

void hypot_f32_4(const hypot_input_f32_4 *in, float4 *out) {
    *out = hypot(in->x, in->y);
}
