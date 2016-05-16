#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct remainder_f32 {
    float num;
    float den;
};

void remainder_f32_1 (const struct remainder_f32* in, float* out) {
    *out = remainder(in->num, in->den);
}

struct remainder_f32_2 {
    float2 num;
    float2 den;
};

void remainder_f32_2 (const struct remainder_f32_2* in, float2* out) {
    *out = remainder(in->num, in->den);
}

struct remainder_f32_3 {
    float3 num;
    float3 den;
};

void remainder_f32_3 (const struct remainder_f32_3* in, float3* out) {
    *out = remainder(in->num, in->den);
}

struct remainder_f32_4 {
    float4 num;
    float4 den;
};

void remainder_f32_4 (const struct remainder_f32_4* in, float4* out) {
    *out = remainder(in->num, in->den);
}
