// -target-api 17
#pragma version(1)
#pragma rs java_package_name(android.renderscript.cts)

#if RS_VERSION != 17
#error Invalid RS_VERSION
#endif

#define RS_MSG_TEST_PASSED 100
#define RS_MSG_TEST_FAILED 101

void check(int version) {
    if (version != RS_VERSION) {
        rsDebug("version: ", version);
        rsDebug("RS_VERSION: ", RS_VERSION);
        rsSendToClient(RS_MSG_TEST_FAILED);
    } else {
        rsSendToClientBlocking(RS_MSG_TEST_PASSED);
    }
}

