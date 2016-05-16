#include "shared.rsh"

static bool b = false;

void root(const int *o, uint32_t x, uint32_t y) {
    b = true;
}

void static_globals_test() {
    if (!b) {
        rsSendToClientBlocking(RS_MSG_TEST_FAILED);
    }
    else {
        rsSendToClientBlocking(RS_MSG_TEST_PASSED);
    }
}

