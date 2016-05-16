#pragma version(1)
#pragma rs java_package_name(foo)

bool init = false;

void foo() {
    if (!init)
        return;
    rs_allocation a, b;
    rsAllocationGetDimX(a);
    return;
}
