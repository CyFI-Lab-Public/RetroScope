// -target-api 15
#pragma version(1)
#pragma rs java_package_name(foo)

rs_allocation aFail[2];

struct rsStruct {
    rs_allocation a;
} sFail;

static rs_allocation aOk[2];

static struct noExport {
    rs_allocation a;
} sOk;

struct onlyPtr {
    rs_allocation a;
} *ptrOk;

