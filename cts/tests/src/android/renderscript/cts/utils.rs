#include "shared.rsh"

static bool test_color_pack_unpack() {
    bool failed = false;
    start();

    for(uint i = 0; i < 256; i ++) {
        uchar4 v = (uchar)i;
        float4 f = rsUnpackColor8888(v);
        uchar4 res = rsPackColorTo8888(f);
        _RS_ASSERT(v.x == res.x);
        _RS_ASSERT(v.y == res.y);
        _RS_ASSERT(v.z == res.z);
        _RS_ASSERT(v.w == res.w);
    }

    float time = end();

    if (failed) {
        rsDebug("test_color_pack_unpack FAILED", time);
    }
    else {
        rsDebug("test_color_pack_unpack PASSED", time);
    }

    return failed;
}
void test() {
    bool failed = false;
    failed |= test_color_pack_unpack();

    if (failed) {
        rsSendToClientBlocking(RS_MSG_TEST_FAILED);
    }
    else {
        rsSendToClientBlocking(RS_MSG_TEST_PASSED);
    }
}

