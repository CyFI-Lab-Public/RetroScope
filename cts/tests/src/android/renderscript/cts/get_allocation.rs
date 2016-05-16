#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

const int* pointer;
rs_script script;
rs_allocation alloc_in;
rs_allocation alloc_out;

void root(const int* in, int *out) {
    *out = *in;
}

void start() {
    alloc_in = rsGetAllocation(pointer);
    rsForEach(script, alloc_in, alloc_out);
}
