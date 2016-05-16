#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

typedef struct InputData {
    float a;
    float b;
} InputData;

void nextafter_f32_1(const InputData *in, float *out) {
    *out = nextafter (in->a, in->b);
}

typedef struct InputData_2 {
    float2 a;
    float2 b;
} InputData_2;

void nextafter_f32_2(const InputData_2 *in, float2 *out) {
    *out = nextafter (in->a, in->b);
}

typedef struct InputData_3 {
    float3 a;
    float3 b;
} InputData_3;

void nextafter_f32_3(const InputData_3 *in, float3 *out) {
    *out = nextafter (in->a, in->b);
}

typedef struct InputData_4 {
    float4 a;
    float4 b;
} InputData_4;

void nextafter_f32_4(const InputData_4 *in, float4 *out) {
    *out = nextafter (in->a, in->b);
}
