#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

struct step_input {
    float x;
    float y;
};

void step_f32_1(const struct step_input *in, float *out) {
    *out = step(in->x, in->y);
}

struct step_2_input {
    float2 x;
    float2 y;
};

void step_f32_2(const struct step_2_input *in, float2 *out) {
    *out = step(in->x, in->y);
}

struct step_3_input {
    float3 x;
    float3 y;
};

void step_f32_3(const struct step_3_input *in, float3 *out) {
    *out = step(in->x, in->y);
}

struct step_4_input {
    float4 x;
    float4 y;
};

void step_f32_4(const struct step_4_input *in, float4 *out) {
    *out = step(in->x, in->y);
}
