#pragma version(1)
#pragma rs java_package_name(foo)

void old_kernel(const uint32_t *ain, uint32_t x, uint32_t y) {
}

uint32_t* bad_kernel(const uint32_t *ain, uint32_t x, uint32_t y) {
    return 0;
}

