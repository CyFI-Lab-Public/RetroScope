#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

float a;

void init()
{
    a = 2.f;
}

void root(const float *in, float *out) {
    *out = a;
}
