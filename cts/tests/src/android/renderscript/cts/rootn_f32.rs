#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

rs_allocation n1;

void rootn_f32_1(const float *in, float *out, uint32_t x) {
    *out = rootn(*in, *(int *)rsGetElementAt(n1,x));
}

rs_allocation n2;

void rootn_f32_2(const float2 *in, float2 *out, uint32_t x) {
    *out = rootn(*in, *(int2 *)rsGetElementAt(n2,x));
}

rs_allocation n3;

void rootn_f32_3(const float3 *in, float3 *out, uint32_t x) {
    *out = rootn(*in, *(int3 *)rsGetElementAt(n3,x));
}

rs_allocation n4;

void rootn_f32_4(const float4 *in, float4 *out, uint32_t x) {
    *out = rootn(*in, *(int4 *)rsGetElementAt(n4,x));
}
