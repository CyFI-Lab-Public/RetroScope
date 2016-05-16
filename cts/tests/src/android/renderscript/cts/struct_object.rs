#include "shared.rsh"

struct objects_rs {
    rs_element e;
    int i;
};

struct objects_rs myStruct;

void struct_object_test() {
    bool failed = false;

    _RS_ASSERT(myStruct.i == 1);

    if (failed) {
        rsDebug("struct_object_test FAILED", 0);
        rsSendToClientBlocking(RS_MSG_TEST_FAILED);
    }
    else {
        rsDebug("struct_object_test PASSED", 0);
        rsSendToClientBlocking(RS_MSG_TEST_PASSED);
    }
}

