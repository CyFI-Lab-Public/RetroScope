#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

void Rsqrt_f32_1 (const float* in, float* out) {
    *out = rsqrt(*in);
}

void Rsqrt_f32_2 (const float2* in, float2* out) {
    *out = rsqrt(*in);
}

void Rsqrt_f32_3 (const float3* in, float3* out) {
    *out = rsqrt(*in);
}

void Rsqrt_f32_4 (const float4* in, float4* out) {
    *out = rsqrt(*in);
}
