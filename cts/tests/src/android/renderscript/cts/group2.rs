#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

#include "shared.rsh"

rs_allocation aSharedInt;
rs_allocation aFailed;

static bool failed[2] = { false, false };

void __attribute__((kernel)) getSharedInt(uint32_t in, uint32_t x) {
    int v = rsGetElementAt_int(aSharedInt, 0);
    if (in != x) {
        rsDebug("Failed to read in on iteration: ", x);
        rsDebug("Read: ", in);
        failed[x] = true;
    }
    if (v != -5) {
        rsDebug("Failed to read -5 on iteration: ", x);
        rsDebug("Read: ", v);
        failed[x] = true;
    }
}

// Write out aFailed if either of our kernel instances read old data.
void verify() {
    for (int i = 0; i < 2; i++) {
        if (failed[i]) {
            rsSetElementAt_int(aFailed, 1, 0);
        }
    }
}
