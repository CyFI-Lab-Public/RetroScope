#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

void root(const float *in, float *out) {
    *out = rsFrac(*in);
}
