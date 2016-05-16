#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct mad_input_f32 {
    float x;
    float y;
    float z;
};

void mad_f32_1(const struct mad_input_f32 *param, float *out) {
    *out = mad(param->x, param->y, param->z);
}

struct mad_input_f32_2 {
    float2 x;
    float2 y;
    float2 z;
};

void mad_f32_2(const struct mad_input_f32_2 *param, float2 *out) {
    *out = mad(param->x, param->y, param->z);
}

struct mad_input_f32_3 {
    float3 x;
    float3 y;
    float3 z;
};

void mad_f32_3(const struct mad_input_f32_3 *param, float3 *out) {
    *out = mad(param->x, param->y, param->z);
}

struct mad_input_f32_4 {
    float4 x;
    float4 y;
    float4 z;
};

void mad_f32_4(const struct mad_input_f32_4 *param, float4 *out) {
    *out = mad(param->x, param->y, param->z);
}
