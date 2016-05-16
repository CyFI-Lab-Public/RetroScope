#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

rs_allocation aInt;

void write_i(int value, uint32_t x) {
    rsSetElementAt_int(aInt, value, x);
}

void __attribute__((kernel)) write_k(int unused) {
    rsSetElementAt_int(aInt, 1, 1);
}
