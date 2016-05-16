#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct Floats {
    float fa;
    float fb;
    float fc;
} Floats;

void fma_f32_1(const Floats *in, float *out) {
    *out = fma(in->fa, in->fb, in->fc);
}

typedef struct Floats2 {
    float2 fa;
    float2 fb;
    float2 fc;
} Floats2;

void fma_f32_2(const Floats2 *in, float2 *out) {
    *out = fma(in->fa, in->fb, in->fc);
}

typedef struct Floats3 {
    float3 fa;
    float3 fb;
    float3 fc;
} Floats3;

void fma_f32_3(const Floats3 *in, float3 *out) {
    *out = fma(in->fa, in->fb, in->fc);
}

typedef struct Floats4 {
    float4 fa;
    float4 fb;
    float4 fc;
} Floats4;

void fma_f32_4(const Floats4 *in, float4 *out) {
    *out = fma(in->fa, in->fb, in->fc);
}
