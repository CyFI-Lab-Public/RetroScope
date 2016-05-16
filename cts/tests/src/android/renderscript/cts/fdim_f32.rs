#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct fdim_f32_input {
    float x;
    float y;
};

void fdim_f32_1(const struct fdim_f32_input *in, float *out) {
    *out = fdim(in->x, in->y);
}

struct fdim_f32_2_input {
    float2 x;
    float2 y;
};

void fdim_f32_2(const struct fdim_f32_2_input *in, float2 *out) {
    *out = fdim(in->x, in->y);
}

struct fdim_f32_3_input {
    float3 x;
    float3 y;
};

void fdim_f32_3(const struct fdim_f32_3_input *in, float3 *out) {
    *out = fdim(in->x, in->y);
}

struct fdim_f32_4_input {
    float4 x;
    float4 y;
};

void fdim_f32_4(const struct fdim_f32_4_input *in, float4 *out) {
    *out = fdim(in->x, in->y);
}
