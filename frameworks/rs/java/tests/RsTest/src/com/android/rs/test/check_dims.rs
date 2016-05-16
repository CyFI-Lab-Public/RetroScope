#include "shared.rsh"

int pattern;

rs_allocation aFailed;

// This test checks to see that we only work on the cells specified for the
// input allocation (i.e. don't affect anything between dimX and stride for
// each row). If we don't see the pattern that we wrote, we know that we
// are definitely working outside our proper bounds.
void root(const int *o, uint32_t x, uint32_t y) {
    if (*o != pattern) {
        rsSetElementAt_uchar(aFailed, 1, 0);
    }
}

void check_dims_test() {
    bool failed = rsGetElementAt_uchar(aFailed, 0);
    if (failed) {
        rsSendToClientBlocking(RS_MSG_TEST_FAILED);
    }
    else {
        rsSendToClientBlocking(RS_MSG_TEST_PASSED);
    }
}

