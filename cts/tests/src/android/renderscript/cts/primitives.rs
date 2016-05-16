/*****************************************************************************
 *****
 *****
 *****
 ***** DANGER
 ***** DO NOT ALTER THIS TEST WITHOUT VERIFYING ScriptTest.java STILL WORKS
 ***** WE DEPEND ON VARIABLE ORDER FOR VERIFICATION
 *****
 *****
 *****
 *****************************************************************************/
#include "shared.rsh"
#include "structs.rsh"

// Testing primitive types
float floatTest = 1.99f;
double doubleTest = 2.05;
char charTest = -8;
short shortTest = -16;
int intTest = -32;
long longTest = 17179869184l; // 1 << 34
long long longlongTest = 68719476736l; // 1 << 36
bool boolTest = false;

struct myStruct {
    int i;
} structTest;

rs_allocation allocationTest;
int *intPtrTest;

uchar ucharTest = 8;
ushort ushortTest = 16;
uint uintTest = 32;
ulong ulongTest = 4611686018427387904L;
int64_t int64_tTest = -17179869184l; // - 1 << 34
uint64_t uint64_tTest = 117179869184l;

static bool test_primitive_types() {
    bool failed = false;
    start();

    _RS_ASSERT(floatTest == 2.99f);
    _RS_ASSERT(doubleTest == 3.05);
    _RS_ASSERT(charTest == -16);
    _RS_ASSERT(shortTest == -32);
    _RS_ASSERT(intTest == -64);
    _RS_ASSERT(longTest == 17179869185l);
    _RS_ASSERT(longlongTest == 68719476735l);

    _RS_ASSERT(ucharTest == 8);
    _RS_ASSERT(ushortTest == 16);
    _RS_ASSERT(uintTest == 32);
    _RS_ASSERT(ulongTest == 4611686018427387903L);
    _RS_ASSERT(int64_tTest == -17179869184l);
    _RS_ASSERT(uint64_tTest == 117179869185l);

    float time = end();

    if (failed) {
        rsDebug("test_primitive_types FAILED", time);
    }
    else {
        rsDebug("test_primitive_types PASSED", time);
    }

    return failed;
}

static bool test_vector_types() {
    bool failed = false;
    start();
    _RS_ASSERT(avt->b2.x == 1);
    _RS_ASSERT(avt->b2.y == 2);
    _RS_ASSERT(avt->b3.x == 1);
    _RS_ASSERT(avt->b3.y == 2);
    _RS_ASSERT(avt->b3.z == 3);
    _RS_ASSERT(avt->b4.x == 1);
    _RS_ASSERT(avt->b4.y == 2);
    _RS_ASSERT(avt->b4.z == 3);
    _RS_ASSERT(avt->b4.w == 4);

    _RS_ASSERT(avt->s2.x == 1);
    _RS_ASSERT(avt->s2.y == 2);
    _RS_ASSERT(avt->s3.x == 1);
    _RS_ASSERT(avt->s3.y == 2);
    _RS_ASSERT(avt->s3.z == 3);
    _RS_ASSERT(avt->s4.x == 1);
    _RS_ASSERT(avt->s4.y == 2);
    _RS_ASSERT(avt->s4.z == 3);
    _RS_ASSERT(avt->s4.w == 4);

    _RS_ASSERT(avt->i2.x == 1);
    _RS_ASSERT(avt->i2.y == 2);
    _RS_ASSERT(avt->i3.x == 1);
    _RS_ASSERT(avt->i3.y == 2);
    _RS_ASSERT(avt->i3.z == 3);
    _RS_ASSERT(avt->i4.x == 1);
    _RS_ASSERT(avt->i4.y == 2);
    _RS_ASSERT(avt->i4.z == 3);
    _RS_ASSERT(avt->i4.w == 4);

    _RS_ASSERT(avt->f2.x == 1.0f);
    _RS_ASSERT(avt->f2.y == 2.0f);
    _RS_ASSERT(avt->f3.x == 1.0f);
    _RS_ASSERT(avt->f3.y == 2.0f);
    _RS_ASSERT(avt->f3.z == 3.0f);
    _RS_ASSERT(avt->f4.x == 1.0f);
    _RS_ASSERT(avt->f4.y == 2.0f);
    _RS_ASSERT(avt->f4.z == 3.0f);
    _RS_ASSERT(avt->f4.w == 4.0f);

    float time = end();

    if (failed) {
        rsDebug("test_vector_types FAILED", time);
    }
    else {
        rsDebug("test_vector_types PASSED", time);
    }

    return failed;
}

void test() {
    bool failed = false;
    failed |= test_primitive_types();
    failed |= test_vector_types();

    if (failed) {
        rsSendToClientBlocking(RS_MSG_TEST_FAILED);
    }
    else {
        rsSendToClientBlocking(RS_MSG_TEST_PASSED);
    }
}

