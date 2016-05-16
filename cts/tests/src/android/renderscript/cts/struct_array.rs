#include "shared.rsh"

typedef struct ArrayMe {
    int i[5];
} ArrayMe_t;

ArrayMe_t *s;

void verify() {
    bool failed = false;
    for (uint32_t index = 0; index < 5; index++) {
        _RS_ASSERT(s->i[index] == index);
    }

    if (failed) {
        rsSendToClientBlocking(RS_MSG_TEST_FAILED);
    } else {
        rsSendToClientBlocking(RS_MSG_TEST_PASSED);
    }
}
