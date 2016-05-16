#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct fmod_input_f32 {
    float param1;
    float param2;
};

void fmod_f32_1(const struct fmod_input_f32 *in, float *out) {
    *out = fmod(in->param1, in->param2);
}

struct fmod_input_f32_2 {
    float2 param1;
    float2 param2;
};

void fmod_f32_2(const struct fmod_input_f32_2 *in, float2 *out) {
    *out = fmod(in->param1, in->param2);
}

struct fmod_input_f32_3 {
    float3 param1;
    float3 param2;
};

void fmod_f32_3(const struct fmod_input_f32_3 *in, float3 *out) {
    *out = fmod(in->param1, in->param2);
}

struct fmod_input_f32_4 {
    float4 param1;
    float4 param2;
};

void fmod_f32_4(const struct fmod_input_f32_4 *in, float4 *out) {
    *out = fmod(in->param1, in->param2);
}
