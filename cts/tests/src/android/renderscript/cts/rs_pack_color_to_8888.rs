#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

void pack_color_to_8888_rgb (const float3* in, uchar4* out) {
    *out = rsPackColorTo8888(in->r, in->g, in->b);
}

void pack_color_to_8888_rgba (const float4* in, uchar4* out) {
    *out = rsPackColorTo8888(in->r, in->g, in->b, in->a);
}
void pack_color_to_8888_f32_3 (const float3* in, uchar4* out) {
    *out = rsPackColorTo8888(*in);
}

void pack_color_to_8888_f32_4 (const float4* in, uchar4* out) {
    *out = rsPackColorTo8888(*in);
}
