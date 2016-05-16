#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct atan2pi_float_input {
    float x;
    float y;
};

void atan2pi_f32_1 (const struct atan2pi_float_input* in, float* out) {
    *out = atan2pi(in->x, in->y);
}

struct atan2pi_float2_input {
    float2 x;
    float2 y;
};

void atan2pi_f32_2 (const struct atan2pi_float2_input* in, float2* out) {
    *out = atan2pi(in->x, in->y);
}

struct atan2pi_float3_input {
    float3 x;
    float3 y;
};

void atan2pi_f32_3 (const struct atan2pi_float3_input* in, float3* out) {
    *out = atan2pi(in->x, in->y);
}

struct atan2pi_float4_input {
    float4 x;
    float4 y;
};

void atan2pi_f32_4 (const struct atan2pi_float4_input* in, float4* out) {
    *out = atan2pi(in->x, in->y);
}
