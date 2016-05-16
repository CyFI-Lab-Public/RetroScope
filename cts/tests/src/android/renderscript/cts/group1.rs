#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

#include "shared.rsh"

rs_allocation aSharedInt;

uint32_t  __attribute__((kernel)) setSharedInt(uint32_t x) {
    if (x == 1) {
        rsSetElementAt_int(aSharedInt, -5, 0);
    }
    return x;
}
