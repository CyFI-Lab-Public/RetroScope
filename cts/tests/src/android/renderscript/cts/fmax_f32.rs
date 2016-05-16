#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct fmax_f32_in {
    float first;
    float second;
} input1;

void fmax_f32_1(const input1* in, float* out){
    *out = fmax(in->first, in->second);
}

typedef struct fmax_f32_2_in {
    float2 first;
    float2 second;
} input2;

void fmax_f32_2(const input2* in, float2* out){
    *out = fmax(in->first, in->second);
}

typedef struct fmax_f32_3_in {
    float3 first;
    float3 second;
} input3;

void fmax_f32_3(const input3* in, float3* out){
    *out = fmax(in->first, in->second);
}

typedef struct fmax_f32_4_in {
    float4 first;
    float4 second;
} input4;

void fmax_f32_4(const input4* in, float4* out){
    *out = fmax(in->first, in->second);
}
