#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

void root (const uchar4* in, float4* out) {
    *out = rsUnpackColor8888(*in);
}
