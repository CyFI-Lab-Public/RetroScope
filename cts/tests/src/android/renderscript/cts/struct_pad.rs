#include "shared.rsh"

struct PadMe {
    int i;
    float4 f4;
    int j;
} s;

void verify() {
    bool failed = false;
    _RS_ASSERT(s.i == 7);
    _RS_ASSERT(s.f4.x == 1.0f);
    _RS_ASSERT(s.f4.y == 2.0f);
    _RS_ASSERT(s.f4.z == 3.0f);
    _RS_ASSERT(s.f4.w == 4.0f);
    _RS_ASSERT(s.j == 9);

    if (failed) {
        rsSendToClientBlocking(RS_MSG_TEST_FAILED);
    } else {
        rsSendToClientBlocking(RS_MSG_TEST_PASSED);
    }
}
