#pragma version(1)
#pragma rs java_package_name(foo)

void foo() {
    float4 f4;
    (void)f4.xg;
    (void)f4.ry;
    f4.xr = f4.ba;
    return;
}

