#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)
typedef struct _cross_f32_3_struct {
    float3 low;
    float3 high;
} cross_f32_3_struct;

void cross_f32_3(const cross_f32_3_struct *in, float3 *out) {
    *out = cross(in->low, in->high);
}

typedef struct _cross_f32_4_struct {
    float4 low;
    float4 high;
} cross_f32_4_struct;

void cross_f32_4(const cross_f32_4_struct *in, float4 *out) {
    *out = cross(in->low, in->high);
}
